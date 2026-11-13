// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.EntityUFOBase;
import com.hbm.entity.projectile.EntitySiegeLaser;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.special.ItemSpawnEgg;
import com.hbm.items.weapon.legacy.LegacyFx;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.packet.toclient.PlasmaBlastPayload;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import com.hbm.space.SpaceInvaders.SiegeTier;
import com.hbm.space.trait.CBT_Invasion;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

/** The invasion: the siege craft (big siege UFO), the kill hook that counts the waves and the wave boss bar. */
public final class SpaceSiege {

    private SpaceSiege() {}

    public static final RegistryHandle<SoundEvent> ALARM_PING = NeoForgeRegistrar.registerSound("alarm.ping");

    public static final RegistryHandle<EntityType<SiegeCraft>> SIEGE_CRAFT = Reg.entity("entity_micro_ufo",
            () -> EntityType.Builder.<SiegeCraft>of(SiegeCraft::new, MobCategory.MONSTER).sized(7F, 1F).fireImmune()
                    .clientTrackingRange(16).build(key("entity_micro_ufo")));

    static {
        Reg.item("entity_micro_ufo_spawn_egg", p -> new ItemSpawnEgg(() -> SIEGE_CRAFT, 0x5B963E, 0xC0B286, p), Item.Properties::new);
        NeoForgeRegistrar.registerLivingAttributes(SIEGE_CRAFT, SiegeCraft::createAttributes);
    }

    private static final Map<String, ServerBossEvent> BARS = new HashMap<>();

    private static String key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Library.id(name)).location().toString();
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(SpaceSiege::onDeath);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> tickBars(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> BARS.clear());
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        CelestialBody body = CelestialBody.getBody(entity.level());
        CBT_Invasion alien = body.getTrait(CBT_Invasion.class);
        if (alien != null) alien.onKill(entity, body);
    }

    /** Space showed the wave as the client's boss bar on the body being invaded; here it is a server boss bar. */
    private static void tickBars(MinecraftServer server) {
        for (CelestialBody body : CelestialBody.getAllBodies()) {
            CBT_Invasion inv = body.getTrait(CBT_Invasion.class);
            ServerLevel level = body.dimension == null ? null : server.getLevel(body.dimension);
            if (inv == null || !inv.isInvading || inv.wave >= 4 || level == null) {
                ServerBossEvent bar = BARS.remove(body.name);
                if (bar != null) bar.removeAllPlayers();
                continue;
            }
            ServerBossEvent bar = BARS.computeIfAbsent(body.name,
                    k -> new ServerBossEvent(inv.getBarName(), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS));
            bar.setName(inv.getBarName());
            bar.setProgress(inv.getMaxHealth() <= 0 ? 1F : Mth.clamp(inv.getHealth() / inv.getMaxHealth(), 0F, 1F));
            for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) if (p.level() != level) bar.removePlayer(p);
            for (ServerPlayer p : level.players()) bar.addPlayer(p);
        }
    }

    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(SiegeCraft.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LOCK_X = SynchedEntityData.defineId(SiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOCK_Y = SynchedEntityData.defineId(SiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOCK_Z = SynchedEntityData.defineId(SiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> BEAM = SynchedEntityData.defineId(SiegeCraft.class, EntityDataSerializers.BOOLEAN);

    /**
     * The siege craft: a big spinning saucer that fans seven lasers at its target and every fifteen seconds locks on,
     * marks the spot with red dust and blasts it from above. Leaves after four minutes.
     */
    public static class SiegeCraft extends EntityUFOBase {
        private static final int MAX_LIFETIME = 60 * 4 * 20;

        private int attackCooldown;
        private int beamCountdown;
        private int lifetime = 0;
        private boolean isRetreating = false;

        public SiegeCraft(EntityType<? extends SiegeCraft> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.0D);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(TIER, 0);
            builder.define(LOCK_X, 0F);
            builder.define(LOCK_Y, 0F);
            builder.define(LOCK_Z, 0F);
            builder.define(BEAM, false);
        }

        @Override
        public boolean shouldRenderAtSqrDistance(double distance) {
            return true;
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
            if (isInvulnerableTo(source)) return false;
            SiegeTier tier = getTier();
            if (tier.fireProof && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                clearFire();
                return false;
            }
            if (tier.noFriendlyFire && source.getEntity() != null && !(source.getEntity() instanceof Player)) return false;
            damage -= tier.dt;
            if (damage < 0) {
                level.playSound(null, this, SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 5F, 1.0F + random.nextFloat() * 0.5F);
                return false;
            }
            damage *= (1F - tier.dr);
            return super.hurtServer(level, source, damage);
        }

        @Override
        protected void tickDeath() {
            beamCountdown = 200;
            setBeam(false);
            setDeltaMovement(getDeltaMovement().subtract(0, 0.05D, 0));
            if (deathTime == 19 && level() instanceof ServerLevel server) {
                PacketWire.sendToAllAround(new MukePayload(getX(), getY() + 0.5, getZ(), true, false),
                        new TargetPoint(server, getX(), getY(), getZ(), 250));
                server.playSound(null, getX(), getY(), getZ(), ModSounds.GUN_MINI_NUKE_EXPLOSION.get(), SoundSource.HOSTILE, 15F, 1F);
            }
            super.tickDeath();
        }

        public void setTier(SiegeTier tier) {
            applyTier(tier);
            setHealth(getMaxHealth());
        }

        // MixinRangedAttribute lifts vanilla's 1024 max health cap with Space on, as Space had none
        private void applyTier(SiegeTier tier) {
            entityData.set(TIER, tier.id);
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(tier.speedMod);
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(tier.health * 25);
        }

        public SiegeTier getTier() {
            return SiegeTier.byId(entityData.get(TIER));
        }

        public void setBeam(boolean beam) {
            entityData.set(BEAM, beam);
        }

        public boolean getBeam() {
            return entityData.get(BEAM);
        }

        public void setLockon(double x, double y, double z) {
            entityData.set(LOCK_X, (float) x);
            entityData.set(LOCK_Y, (float) y);
            entityData.set(LOCK_Z, (float) z);
        }

        public Vec3 getLockon() {
            return new Vec3(entityData.get(LOCK_X), entityData.get(LOCK_Y), entityData.get(LOCK_Z));
        }

        @Override
        protected int getScanRange() {
            return 100;
        }

        @Override
        protected int targetHeightOffset() {
            return 7 + random.nextInt(5);
        }

        @Override
        protected int wanderHeightOffset() {
            return 10 + random.nextInt(2);
        }

        @Override
        protected void setCourseWithoutTarget() {
            int x = (int) Math.floor(getX() + random.nextGaussian() * 15);
            int z = (int) Math.floor(getZ() + random.nextGaussian() * 15);
            setWaypoint(x, surfaceAt(x, z) + 5 + random.nextInt(6), z);
        }

        @Override
        protected void customServerAiStep(ServerLevel level) {
            super.customServerAiStep(level);
            if (isRemoved()) return;

            if (courseChangeCooldown > 0) courseChangeCooldown--;
            if (scanCooldown > 0) scanCooldown--;

            lifetime++;
            if (lifetime > MAX_LIFETIME) isRetreating = true;
            if (isRetreating) {
                target = null;
                setWaypoint((int) getX(), level.getMaxBuildHeight() - 1, (int) getZ());
                approachPosition(20);
                if (getY() > level.getMaxBuildHeight() - 6 || lifetime > MAX_LIFETIME + 300) {
                    discard();
                    return;
                }
            }

            if (attackCooldown > 0) attackCooldown--;
            if (beamCountdown > 0) beamCountdown--;

            if (random.nextInt(50) == 0) {
                // Space's "tau" burst
                ParticleCreators.sparks(level, getX() + random.nextGaussian() * 2, getY() + random.nextGaussian(),
                        getZ() + random.nextGaussian() * 2, 2 + random.nextInt(3), false, 50);
            }

            boolean beam = false;
            if (target == null || beamCountdown <= 0) {
                beamCountdown = 300;
            } else {
                if (beamCountdown >= 60 && beamCountdown < 120) {
                    setLockon(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
                    if (beamCountdown == 110)
                        level.playSound(null, target, SpaceInvaders.STINGER_LOCK_ON.get(), SoundSource.HOSTILE, 2F, 0.75F);
                }
                if (beamCountdown >= 40 && beamCountdown < 100) {
                    Vec3 lockon = getLockon();
                    LegacyFx.vanillaBurst(level, lockon.x, lockon.y, lockon.z, DustParticleOptions.REDSTONE, 5, 0.2D);
                }
                if (beamCountdown < 40) {
                    Vec3 lockon = getLockon();
                    if (beamCountdown == 39)
                        level.playSound(null, lockon.x, lockon.y, lockon.z, ModSounds.UFO_BLAST.get(), SoundSource.HOSTILE, 5.0F,
                                0.9F + random.nextFloat() * 0.2F);
                    for (Entity e : level.getEntities(this, new AABB(lockon, lockon).inflate(2))) {
                        e.hurt(level.damageSources().source(ModDamageTypes.COMBINE, this, e), 1000F);
                        e.igniteForSeconds(5);
                        if (e instanceof LivingEntity living)
                            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
                    }
                    PacketWire.sendToAllAround(new PlasmaBlastPayload(lockon.x, lockon.y, lockon.z, 0.0F, 0.75F, 1.0F,
                                    -90 + random.nextFloat() * 180, random.nextFloat() * 180F, 5F),
                            new TargetPoint(level, lockon.x, lockon.y, lockon.z, 150));
                    beam = true;
                }
            }
            setBeam(beam);

            if (attackCooldown == 0 && target != null) {
                attackCooldown = 30 + random.nextInt(10);
                Vec3 vec = new Vec3(target.getX() - getX(), target.getY() + target.getBbHeight() * 0.5 - getY(), target.getZ() - getZ()).normalize();
                SiegeTier tier = getTier();
                float health = getHealth() / getMaxHealth();
                int color = ((int) (0xff * (1 - health)) << 16) | ((int) (0xff * health) << 8);
                for (int i = 0; i < 7; i++) {
                    Vec3 copy = vec.yRot((float) Math.PI / 180F * (i - 3) * 5F);
                    EntitySiegeLaser laser = new EntitySiegeLaser(ModEntities.SIEGE_LASER.get(), level, this);
                    laser.setPos(getX(), getY(), getZ());
                    laser.setThrowableHeading(copy.x, copy.y, copy.z, 1F, 0.0F);
                    laser.setColor(color);
                    laser.setDamage(tier.damageMod);
                    laser.setBreakChance(tier.laserBreak * 2);
                    if (tier.laserIncendiary) laser.setIncendiary();
                    level.addFreshEntity(laser);
                }
                playSound(ModSounds.BALLS_LASER.get(), 2.0F, 1.0F);
            }

            if (courseChangeCooldown > 0) {
                double speed = target == null ? 0.5D : 1.0D + getAttributeValue(Attributes.MOVEMENT_SPEED) * 3.0D;
                if (isRetreating) speed *= 2.0D;
                approachPosition(speed);
            }
        }

        @Override
        public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                @Nullable SpawnGroupData data) {
            setTier(SiegeTier.tiers[random.nextInt(SiegeTier.getLength())]);
            return super.finalizeSpawn(level, difficulty, reason, data);
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {
            super.addAdditionalSaveData(output);
            output.putInt("siegeTier", getTier().id);
            output.putInt("lifetime", lifetime);
            output.putBoolean("isRetreating", isRetreating);
        }

        // as for the mini UFO: Space reset the tier (and so the health) on load
        @Override
        protected void readAdditionalSaveData(ValueInput input) {
            super.readAdditionalSaveData(input);
            float health = getHealth();
            applyTier(SiegeTier.byId(input.getIntOr("siegeTier", 0)));
            setHealth(health);
            lifetime = input.getIntOr("lifetime", 0);
            isRetreating = input.getBooleanOr("isRetreating", false);
        }

        @Override
        protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
            super.dropCustomDeathLoot(level, source, recentlyHit);
            if (recentlyHit) for (Supplier<ItemStack> drop : getTier().dropItem) spawnAtLocation(drop.get(), 0F);
        }
    }
}
