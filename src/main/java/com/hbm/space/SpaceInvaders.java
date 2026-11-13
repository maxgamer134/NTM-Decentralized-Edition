// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.api.environment.ISuffocationImmune;
import com.hbm.backport.BlockEntityCompat;
import com.hbm.backport.compat.EntityCompat;
import com.hbm.backport.compat.MobCompat;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blocks.ITickingBlock;
import com.hbm.config.ConfigSchema;
import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.entity.mob.EntityUFOBase;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.material.Mats;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemChopper;
import com.hbm.items.special.ItemSpawnEgg;
import com.hbm.items.weapon.legacy.LegacyFx;
import com.hbm.items.weapon.sedna.factory.XFactoryEnergy;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.EffectNTPayload;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.particle.HbmEffectNT;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.serialization.MapCodec;
import org.jspecify.annotations.Nullable;

/** The invaders: the Radioactive Angel boss, the siege tier mini UFOs and the combat drop pod that lands glyphids. */
public final class SpaceInvaders {

    private SpaceInvaders() {}

    public static final RegistryHandle<SoundEvent> BFA_TALK = NeoForgeRegistrar.registerSound("entity.bfa_talk");
    public static final RegistryHandle<SoundEvent> BFA_SHOOT = NeoForgeRegistrar.registerSound("entity.bfa_shoot");
    public static final RegistryHandle<SoundEvent> STINGER_LOCK_ON = NeoForgeRegistrar.registerSound("weapon.stinger_lock_on");
    public static final RegistryHandle<SoundEvent> HATCH_OPEN = NeoForgeRegistrar.registerSound("block.hatch_open");
    public static final RegistryHandle<SoundEvent> HATCH_IMPACT = NeoForgeRegistrar.registerSound("block.hatch_impact");

    public static final RegistryHandle<EntityType<BFAngel>> BF_ANGEL = Reg.entity("entity_ntm_bfa",
            () -> EntityType.Builder.<BFAngel>of(BFAngel::new, MobCategory.MONSTER).sized(5F, 4F).fireImmune()
                    .clientTrackingRange(20).build(key("entity_ntm_bfa")));
    public static final RegistryHandle<EntityType<SiegeUFO>> SIEGE_UFO = Reg.entity("entity_meme_ufo",
            () -> EntityType.Builder.<SiegeUFO>of(SiegeUFO::new, MobCategory.MONSTER).sized(1.5F, 1F)
                    .clientTrackingRange(10).build(key("entity_meme_ufo")));
    public static final RegistryHandle<EntityType<CombatDropPod>> COMBAT_POD = Reg.entity("entity_combat_pod",
            () -> EntityType.Builder.<CombatDropPod>of(CombatDropPod::new, MobCategory.MISC).sized(0.25F, 0.25F).fireImmune()
                    .clientTrackingRange(32).updateInterval(1).build(key("entity_combat_pod")));

    public static final RegistryHandle<PodBlock> COMBAT_DROP = Reg.blockItem("combat_drop", PodBlock::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).sound(SoundType.METAL).noOcclusion());
    public static final Reg.BeHandle<PodBE> COMBAT_DROP_BE = Reg.be("combat_drop",
                    () -> new BlockEntityType<>(PodBE::new, Set.of(COMBAT_DROP.get()), null))
            .ticks(PodBE::tick);

    public static final RegistryHandle<ItemChopper> SPAWN_ANGEL = Reg.item("spawn_angel",
            props -> new ItemChopper(props, () -> BF_ANGEL.get(), 25, mob -> {}), () -> new Item.Properties().stacksTo(1));

    static {
        Reg.item("entity_ntm_bfa_spawn_egg", p -> new ItemSpawnEgg(() -> BF_ANGEL, 0x204131, 0x75CE00, p), Item.Properties::new);
        Reg.item("entity_meme_ufo_spawn_egg", p -> new ItemSpawnEgg(() -> SIEGE_UFO, 0x303030, 0x800000, p), Item.Properties::new);
        NeoForgeRegistrar.registerLivingAttributes(BF_ANGEL, BFAngel::createAttributes);
        NeoForgeRegistrar.registerLivingAttributes(SIEGE_UFO, SiegeUFO::createAttributes);
    }

    private static String key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Library.id(name)).location().toString();
    }

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.CONSUMABLES, Library.id("spawn_ufo"), () -> new ItemStack(SPAWN_ANGEL.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("soyuz_capsule"), () -> new ItemStack(COMBAT_DROP.get()));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Radioactive Angel

    private static final EntityDataAccessor<Boolean> BFA_BEAM = SynchedEntityData.defineId(BFAngel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BFA_WX = SynchedEntityData.defineId(BFAngel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BFA_WY = SynchedEntityData.defineId(BFAngel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BFA_WZ = SynchedEntityData.defineId(BFAngel.class, EntityDataSerializers.INT);

    /**
     * A flying boss: charges and fires a tesla beam at its target every few seconds, then spits balefire for a while,
     * and calls three beams down from the sky around the target every ten seconds. Dashes between waypoints around
     * its target. Drops an angelic core when it dies.
     */
    public static class BFAngel extends MobCompat implements Enemy, ISuffocationImmune {
        private final ServerBossEvent bossEvent =
                new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);

        public int courseChangeCooldown;
        public int scanCooldown;
        public int hurtCooldown;
        public int beamTimer;
        private @Nullable Entity target;
        private final List<Entity> secondaries = new ArrayList<>();

        private double lockedX, lockedY, lockedZ;
        private int aoeCooldown = 0;
        private int aoeStage = 0;
        private final Vec3[] strikePositions = new Vec3[3];
        private final int[] strikeTimers = new int[3];
        private final boolean[] strikeArmed = new boolean[3];

        public int gatlingCharge;
        public boolean isChargingGatling;

        public BFAngel(EntityType<? extends BFAngel> type, Level level) {
            super(type, level);
            setNoGravity(true);
            this.xpReward = 500;
            this.deathTime = -30;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10000.0D).add(Attributes.FOLLOW_RANGE, 150.0D);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(BFA_BEAM, false);
            builder.define(BFA_WX, 0);
            builder.define(BFA_WY, 0);
            builder.define(BFA_WZ, 0);
        }

        @Override
        protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {}

        @Override
        public boolean removeWhenFarAway(double distance) {
            return false;
        }

        @Override
        protected boolean shouldDespawnInPeaceful() {
            return true;
        }

        @Override
        public boolean shouldRenderAtSqrDistance(double distance) {
            return distance < 500000D;
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
            if (hurtCooldown > 0) return false;
            boolean hit = super.hurtServer(level, source, amount);
            if (hit) hurtCooldown = 5;
            return hit;
        }

        /** Space aimed at anything but its own kind; main's projectile check never matched, so it shot its own bullets too. */
        private boolean canTarget(Entity e) {
            return !(e instanceof BFAngel) && !(e instanceof Projectile) && !(e instanceof EntityBulletBaseMK4)
                    && !(e instanceof EntityBulletBeamBase);
        }

        @Override
        protected void customServerAiStep(ServerLevel level) {
            if (level.getDifficulty() == Difficulty.PEACEFUL) {
                discard();
                return;
            }
            if (hurtCooldown > 0) hurtCooldown--;
            if (courseChangeCooldown > 0) courseChangeCooldown--;
            if (scanCooldown > 0) scanCooldown--;

            if (target != null && !target.isAlive()) target = null;

            if (scanCooldown <= 0) scan();

            if (target != null && courseChangeCooldown <= 0) {
                Vec3 vec = new Vec3(getX() - target.getX(), 0, getZ() - target.getZ());
                if (random.nextInt(3) > 0) vec = vec.yRot((float) Math.PI * 2 * random.nextFloat());
                double length = vec.length();
                double overshoot = 35;
                int wX = (int) Math.floor(target.getX() - vec.x / length * overshoot);
                int wZ = (int) Math.floor(target.getZ() - vec.z / length * overshoot);
                setWaypoint(wX, Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, wX, wZ) + random.nextInt(15),
                        (int) target.getY() - 5), wZ);
                courseChangeCooldown = 40 + random.nextInt(20);
            }

            tickBeam(level);
            tickWeapons(level);
            tickStrikes(level);
            tickMovement();
        }

        private void scan() {
            secondaries.clear();
            target = null;
            for (Entity entity : level().getEntities(this, getBoundingBox().inflate(100, 50, 100))) {
                if (!entity.isAlive() || !canTarget(entity)) continue;
                if (entity instanceof Player player) {
                    if (player.isCreative() || player.isSpectator()) continue;
                    if (player.hasEffect(MobEffects.INVISIBILITY)) continue;
                    if (target == null || distanceToSqr(entity) < distanceToSqr(target)) target = entity;
                }
                if (entity instanceof LivingEntity && distanceToSqr(entity) < 100 * 100 && getSensing().hasLineOfSight(entity)
                        && entity != target) secondaries.add(entity);
            }
            if (target == null && !secondaries.isEmpty()) target = secondaries.get(random.nextInt(secondaries.size()));
            scanCooldown = 50;
        }

        /** The UFO's death ray, kept as Space has it; nothing on the angel arms it yet. */
        private void tickBeam(ServerLevel level) {
            if (beamTimer <= 0 && getBeam()) setBeam(false);
            if (beamTimer <= 0) return;
            beamTimer--;
            int iy = level.getMinBuildHeight();
            if (iy >= getY()) return;
            AABB column = new AABB(getX(), iy, getZ(), getX(), getY(), getZ()).inflate(5, 0, 5);
            for (Entity e : level.getEntities(this, column)) {
                if (!canTarget(e)) continue;
                e.hurt(level.damageSources().source(ModDamageTypes.COMBINE, this, e), 1000F);
                e.igniteForSeconds(5);
                if (e instanceof LivingEntity living)
                    ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
            }
            Vec3 motion = getDeltaMovement();
            for (double ahead : new double[] {0D, 0.5D}) {
                double px = getX() + motion.x * ahead, pz = getZ() + motion.z * ahead;
                PacketWire.sendToAllAround(new EffectNTPayload(HbmEffectNT.UfoCloud, px, iy + 0.5, pz), new TargetPoint(level, px, iy + 0.5, pz, 150));
            }
        }

        private void tickWeapons(ServerLevel level) {
            if (tickCount % 300 < 200) {
                if (!secondaries.isEmpty()) {
                    Entity e = secondaries.get(random.nextInt(secondaries.size()));
                    if (!e.isAlive()) secondaries.remove(e);
                }
                if (target != null) {
                    if (!isChargingGatling) {
                        isChargingGatling = true;
                        gatlingCharge = 70;
                    } else {
                        if (gatlingCharge > 10) {
                            lockedX = target.getX();
                            lockedY = target.getY();
                            lockedZ = target.getZ();
                        }
                        gatlingCharge--;
                        if (gatlingCharge == 30) level.playSound(null, this, BFA_TALK.get(), SoundSource.HOSTILE, 10F, 1F);
                        if (gatlingCharge <= 0) {
                            fireBeamAt(lockedX, lockedY, lockedZ);
                            isChargingGatling = false;
                            gatlingCharge = 0;
                        }
                    }
                }
            } else if (tickCount % 20 == 0) {
                if (!secondaries.isEmpty()) {
                    Entity e = secondaries.get(random.nextInt(secondaries.size()));
                    if (e.isAlive()) rocketAttack(e);
                    else secondaries.remove(e);
                } else if (target != null) {
                    rocketAttack(target);
                }
            } else if (tickCount % 4 == 2 && target != null) {
                rocketAttack(target);
            }
        }

        /** Every ten seconds three spots around the target are marked with red dust and beeps, then struck from above. */
        private void tickStrikes(ServerLevel level) {
            if (target == null) return;
            if (aoeCooldown <= 0 && aoeStage == 0) {
                aoeStage = 1;
                for (int i = 0; i < 3; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 12;
                    double offsetZ = (random.nextDouble() - 0.5) * 12;
                    strikePositions[i] = new Vec3(target.getX() + offsetX, target.getY(), target.getZ() + offsetZ);
                    strikeTimers[i] = -i * 4;
                    strikeArmed[i] = true;
                }
                aoeCooldown = 200;
            }
            for (int i = 0; i < 3; i++) {
                if (!strikeArmed[i]) continue;
                strikeTimers[i]++;
                if (strikeTimers[i] < 0) continue;
                Vec3 pos = strikePositions[i];
                if (strikeTimers[i] < 60 && strikeTimers[i] % 5 == 0) {
                    level.playSound(null, pos.x, pos.y, pos.z, STINGER_LOCK_ON.get(), SoundSource.HOSTILE, 1F, 1F);
                    LegacyFx.vanillaBurst(level, pos.x, pos.y, pos.z, DustParticleOptions.REDSTONE, 35, 0.2D);
                }
                if (strikeTimers[i] >= 60) {
                    fireDownwardBeamAt(pos.x, pos.y, pos.z);
                    strikeArmed[i] = false;
                }
            }
            boolean allDone = true;
            for (int i = 0; i < 3; i++) {
                if (strikeArmed[i]) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) aoeStage = 0;
            if (aoeCooldown > 0) aoeCooldown--;
        }

        /** Hovers still, then dashes at its waypoint in the last ten ticks before the next one; turns to face the target. */
        private void tickMovement() {
            setDeltaMovement(Vec3.ZERO);
            if (target == null) return;
            if (courseChangeCooldown <= 0) {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 15 + random.nextInt(10);
                setWaypoint((int) (target.getX() + Math.cos(angle) * radius), (int) (target.getY() + 5 + random.nextInt(5)),
                        (int) (target.getZ() + Math.sin(angle) * radius));
                courseChangeCooldown = 85;
            }
            if (courseChangeCooldown <= 10) {
                Vec3 dir = new Vec3(getWaypointX() - getX(), getWaypointY() - getY(), getWaypointZ() - getZ()).normalize();
                double dashSpeed = 2.0D * (courseChangeCooldown / 15.0);
                setDeltaMovement(dir.scale(dashSpeed));
            }
            courseChangeCooldown--;

            double dx = target.getX() - getX();
            double dy = target.getY() + target.getBbHeight() * 0.5 - (getY() + 1);
            double dz = target.getZ() - getZ();
            float targetYaw = (float) (Math.atan2(dz, dx) * 180D / Math.PI) + 90F;
            setYRot(getYRot() + Mth.wrapDegrees(targetYaw - getYRot()) * 0.15F);
            double dist = Math.sqrt(dx * dx + dz * dz);
            float targetPitch = (float) (-(Math.atan2(dy, dist) * -180D / Math.PI));
            setXRot(getXRot() + (targetPitch - getXRot()) * 0.1F);
        }

        private void fireBeamAt(double x, double y, double z) {
            double spawnX = getX() - 1, spawnY = getY() + 4, spawnZ = getZ();
            beam(spawnX, spawnY, spawnZ, new Vec3(x - spawnX, y - spawnY, z - spawnZ), 8F);
        }

        private void fireDownwardBeamAt(double x, double y, double z) {
            beam(x, y + 40, z, new Vec3(0, -1, 0), 6F);
        }

        // Space set the tesla overcharge's knockback to 0 on the shared config, taking the knockback off the player's gun too
        private void beam(double x, double y, double z, Vec3 delta, float damage) {
            EntityBulletBeamBase bullet = new EntityBulletBeamBase(this, XFactoryEnergy.energy_tesla_overcharge, damage);
            bullet.setPos(x, y, z);
            bullet.setRotationsFromVector(delta);
            bullet.performHitscanExternal(250D);
            level().addFreshEntity(bullet);
            playSound(BFA_SHOOT.get(), 5F, 1F);
        }

        private void rocketAttack(Entity e) {
            Vec3 heading = new Vec3(e.getX() - getX(), e.getY() + e.getBbHeight() / 2.0 - getY() - 0.5D, e.getZ() - getZ()).normalize();
            EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(this, XFactoryFlamer.flame_balefire, 0.5F, 1, 0, 0, 0);
            bullet.setDeltaMovement(heading);
            level().addFreshEntity(bullet);
            playSound(ModSounds.RICHARD_FIRE.get(), 5F, 1F);
        }

        @Override
        public void tick() {
            super.tick();
            if (!level().isClientSide()) bossEvent.setProgress(getHealth() / getMaxHealth());
        }

        @Override
        public void startSeenByPlayer(ServerPlayer player) {
            super.startSeenByPlayer(player);
            bossEvent.addPlayer(player);
        }

        @Override
        public void stopSeenByPlayer(ServerPlayer player) {
            super.stopSeenByPlayer(player);
            bossEvent.removePlayer(player);
        }

        @Override
        protected void tickDeath() {
            if (getBeam()) setBeam(false);
            setDeltaMovement(getDeltaMovement().subtract(0, 0.05D, 0));
            if (deathTime == -10) level().playSound(null, this, ModSounds.CHOPPER_DAMAGE.get(), SoundSource.HOSTILE, 10F, 1F);
            if (deathTime == 19 && level() instanceof ServerLevel server) {
                PacketWire.sendToAllAround(new MukePayload(getX(), getY() + 0.5, getZ(), true, false),
                        new TargetPoint(server, getX(), getY(), getZ(), 250));
                server.playSound(null, getX(), getY(), getZ(), ModSounds.GUN_MINI_NUKE_EXPLOSION.get(), SoundSource.HOSTILE, 15F, 1F);
                spawnAtLocation(new ItemStack(SpaceFuels.CORE_ANGEL.get()), 1);
                for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(200, 200, 200)))
                    player.sendSystemMessage(Component.literal("Stars are starting to flicker...").withStyle(ChatFormatting.RED));
                CelestialBody body = CelestialBody.getBody(server);
                if (body.getTrait(com.hbm.space.trait.CBT_Invasion.class) == null)
                    body.modifyTraits(new com.hbm.space.trait.CBT_Invasion(0, 122, false));
            }
            super.tickDeath();
        }

        @Override
        protected float getSoundVolume() {
            return 10F;
        }

        @Override
        protected @Nullable SoundEvent getHurtSound(DamageSource source) {
            return SoundEvents.BLAZE_HURT;
        }

        @Override
        protected @Nullable SoundEvent getDeathSound() {
            return null;
        }

        public void setBeam(boolean b) {
            entityData.set(BFA_BEAM, b);
        }

        public boolean getBeam() {
            return entityData.get(BFA_BEAM);
        }

        public void setWaypoint(int x, int y, int z) {
            entityData.set(BFA_WX, x);
            entityData.set(BFA_WY, y);
            entityData.set(BFA_WZ, z);
        }

        public int getWaypointX() {
            return entityData.get(BFA_WX);
        }

        public int getWaypointY() {
            return entityData.get(BFA_WY);
        }

        public int getWaypointZ() {
            return entityData.get(BFA_WZ);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Siege tiers and the mini UFO

    /** NTM's siege tiers: how tough a siege mob is, what it drops. The mini UFO is the only siege mob ported so far. */
    public static final class SiegeTier {
        public static final SiegeTier[] tiers = new SiegeTier[100];
        private static int nextID = 0;

        public static int getLength() {
            return nextID;
        }

        private static Supplier<ItemStack> ingot(com.hbm.inventory.material.NTMMaterial mat, int count) {
            return () -> new ItemStack(ModItems.ingot(mat), count);
        }

        private static Supplier<ItemStack> item(Supplier<? extends RegistryHandle<Item>> item, int count) {
            return () -> new ItemStack(item.get().get(), count);
        }

        public static final SiegeTier DEFAULT_BUFF = new SiegeTier(20, "buff").addDrop(ingot(Mats.MAT_STEEL, 1)).setDR(0.2F).setDMG(2F).setLaser(0F, 0F, false);
        public static final SiegeTier CLAY = new SiegeTier(30, "clay").addDrop(ingot(Mats.MAT_STEEL, 1)).setDR(0.2F).setDMG(3F).setLaser(0F, 0F, false);
        public static final SiegeTier STONE = new SiegeTier(40, "stone").addDrop(ingot(Mats.MAT_STEEL, 4)).setDR(0.3F).setDT(1F).setFP().setDMG(5F).setLaser(0F, 0F, true);
        public static final SiegeTier IRON = new SiegeTier(50, "iron").addDrop(ingot(Mats.MAT_STEEL, 6)).setDR(0.3F).setDT(2F).setFP().setDMG(7.5F).setFF().setLaser(0F, 1F, true);
        public static final SiegeTier SILVER = new SiegeTier(70, "silver").addDrop(ingot(Mats.MAT_STEEL, 8)).setDR(0.5F).setDT(3F).setNF().setFP().setDMG(10F).setSP(0.5F).setFF().setLaser(0.01F, 1F, true);
        public static final SiegeTier GOLD = new SiegeTier(100, "gold").addDrop(ingot(Mats.MAT_STEEL, 8)).addDrop(item(() -> ModItems.CIRCUIT_CAPACITOR, 2)).setDR(0.5F).setDT(5F).setNF().setFP().setDMG(15F).setSP(0.5F).setFF().setLaser(0.02F, 1.5F, true);
        public static final SiegeTier DESH = new SiegeTier(150, "desh").addDrop(ingot(Mats.MAT_STEEL, 16)).addDrop(item(() -> ModItems.CIRCUIT_CAPACITOR, 3)).setDR(0.7F).setDT(7F).setNF().setFP().setDMG(25F).setSP(0.5F).setFF().setLaser(0.05F, 1.5F, true);
        public static final SiegeTier SCHRAB = new SiegeTier(250, "schrab").addDrop(ingot(Mats.MAT_STEEL, 32)).addDrop(ingot(Mats.MAT_COBALT, 2)).addDrop(item(() -> ModItems.CIRCUIT_ADVANCED, 3)).setDR(0.7F).setDT(10F).setNF().setFP().setDMG(50F).setSP(1F).setFF().setLaser(0.1F, 2F, true);
        public static final SiegeTier DNT = new SiegeTier(500, "dnt").addDrop(ingot(Mats.MAT_STAR, 1)).setDR(0.9F).setDT(20F).setNF().setFP().setDMG(100F).setSP(1F).setFF().setLaser(0.2F, 2F, true);

        public final int id;
        public float dt = 0F;
        public float dr = 0F;
        public final float health;
        public final String name;
        public float damageMod = 1F;
        public float speedMod = 0F;
        public boolean fireProof = false;
        public boolean noFall = false;
        public boolean noFriendlyFire = false;
        public final List<Supplier<ItemStack>> dropItem = new ArrayList<>();
        public float laserBreak = 0F;
        public float laserExplosive = 0F;
        public boolean laserIncendiary = false;

        private SiegeTier(float baseHealth, String name) {
            this.id = nextID;
            tiers[id] = this;
            nextID++;
            this.health = baseHealth;
            this.name = name;
        }

        private SiegeTier setDT(float dt) { this.dt = dt; return this; }
        private SiegeTier setDR(float dr) { this.dr = dr; return this; }
        private SiegeTier setDMG(float dmg) { this.damageMod = dmg; return this; }
        private SiegeTier setSP(float sp) { this.speedMod = sp; return this; }
        private SiegeTier setFP() { this.fireProof = true; return this; }
        private SiegeTier setNF() { this.noFall = true; return this; }
        private SiegeTier setFF() { this.noFriendlyFire = true; return this; }

        private SiegeTier setLaser(float breaking, float explosive, boolean incendiary) {
            this.laserBreak = breaking;
            this.laserExplosive = explosive;
            this.laserIncendiary = incendiary;
            return this;
        }

        private SiegeTier addDrop(Supplier<ItemStack> drop) {
            dropItem.add(drop);
            return this;
        }

        public static SiegeTier byId(int id) {
            SiegeTier tier = id >= 0 && id < tiers.length ? tiers[id] : null;
            return tier != null ? tier : CLAY;
        }
    }

    private static final EntityDataAccessor<Integer> UFO_TIER = SynchedEntityData.defineId(SiegeUFO.class, EntityDataSerializers.INT);

    /** The invasion's mini UFO: circles its player, zapping emerald beams at where it last saw them; leaves after two minutes. */
    public static class SiegeUFO extends EntityUFOBase {
        private static final int MAX_LIFETIME = 60 * 2 * 20;

        private int attackCooldown;
        private double lastTargetX, lastTargetY, lastTargetZ;
        private int lifetime = 0;
        private boolean isRetreating = false;

        public SiegeUFO(EntityType<? extends SiegeUFO> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.0D);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(UFO_TIER, 0);
        }

        public void setTier(SiegeTier tier) {
            applyTier(tier);
            setHealth(getMaxHealth());
        }

        private void applyTier(SiegeTier tier) {
            entityData.set(UFO_TIER, tier.id);
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(tier.speedMod);
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(tier.health * 0.25);
        }

        public SiegeTier getTier() {
            return SiegeTier.byId(entityData.get(UFO_TIER));
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
        protected void customServerAiStep(ServerLevel level) {
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

            super.customServerAiStep(level);
            if (isRemoved()) return;

            if (courseChangeCooldown > 0) courseChangeCooldown--;
            if (scanCooldown > 0) scanCooldown--;
            if (attackCooldown > 0) attackCooldown--;

            if (!isRetreating && target != null) {
                if ((lastTargetX == 0 && lastTargetY == 0 && lastTargetZ == 0) || random.nextInt(10) == 0) {
                    lastTargetX = target.getX();
                    lastTargetY = target.getY() + target.getEyeHeight();
                    lastTargetZ = target.getZ();
                }
                if (attackCooldown == 0) {
                    attackCooldown = 20 + random.nextInt(25);
                    EntityBulletBeamBase bullet = new EntityBulletBeamBase(this, XFactoryEnergy.energy_emerald_overcharge, 8F);
                    bullet.setPos(getX(), getY(), getZ());
                    bullet.setRotationsFromVector(new Vec3(lastTargetX - getX(), lastTargetY - getY(), lastTargetZ - getZ()));
                    bullet.performHitscanExternal(250D);
                    level.addFreshEntity(bullet);
                    playSound(BFA_SHOOT.get(), 2F, 1F);
                }
            }

            if (courseChangeCooldown > 0) {
                double speed = target == null ? 0.5D : 1.0D + getAttributeValue(Attributes.MOVEMENT_SPEED);
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

        // Space reset the tier on load, which healed the UFO fully every time its chunk loaded
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
            for (Supplier<ItemStack> drop : getTier().dropItem) spawnAtLocation(drop.get(), 0F);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Combat drop pod

    private static final EntityDataAccessor<Integer> POD_COLOR = SynchedEntityData.defineId(CombatDropPod.class, EntityDataSerializers.INT);

    /** Falls four blocks a tick, buries itself in the first solid block it hits and becomes a pod that lets its cargo out. */
    public static class CombatDropPod extends EntityCompat {
        private @Nullable CompoundTag entityType;
        private int amount;
        private int color;

        public CombatDropPod(EntityType<? extends CombatDropPod> type, Level level) {
            super(type, level);
            noPhysics = true;
        }

        /** Cargo: {@code amount} copies of {@code entity} as it is now. */
        public void setEntity(Entity entity, int amount, int color) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", EntityType.getKey(entity.getType()).toString());
            entity.saveWithoutId(tag);
            setPayload(tag, amount, color);
        }

        public void setPayload(CompoundTag entityData, int amount, int color) {
            this.entityType = entityData;
            this.amount = amount;
            setColor(color);
        }

        public void setColor(int color) {
            this.color = color;
            entityData.set(POD_COLOR, color);
        }

        public int getColor() {
            return entityData.get(POD_COLOR);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            builder.define(POD_COLOR, 0);
        }

        @Override
        public boolean fireImmune() {
            return true;
        }

        @Override
        public boolean shouldRenderAtSqrDistance(double distance) {
            return distance < 500000D;
        }

        @Override
        public void tick() {
            super.tick();
            setDeltaMovement(0, -1, 0);
            xo = getX();
            yo = getY();
            zo = getZ();
            Level level = level();
            for (int i = 0; i < 4; i++) {
                BlockPos pos = BlockPos.containing(getX(), getY() + 1, getZ());
                if (!level.getBlockState(pos).isAir()) {
                    if (!level.isClientSide()) land(level, pos);
                    break;
                }
                setPos(getX(), getY() - 1, getZ());
            }
            if (getY() < level.getMinBuildHeight() - 64) discard();
        }

        private void land(Level level, BlockPos pos) {
            ExplosionLarge.spawnParticles(level, getX(), getY() + 1, getZ(), 50);
            level.playSound(null, getX(), getY(), getZ(), HATCH_IMPACT.get(), SoundSource.BLOCKS, 10F, 0.5F + random.nextFloat() * 0.5F);
            level.setBlockAndUpdate(pos, COMBAT_DROP.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof PodBE capsule && entityType != null) {
                capsule.setPayload(entityType, amount, color);
                capsule.setChanged();
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 3);
            }
            discard();
        }

        @Override
        protected void readAdditionalSaveData(ValueInput input) {
            entityType = input.read("entityType", CompoundTag.CODEC).orElse(null);
            amount = input.getIntOr("amount", 0);
            setColor(input.getIntOr("color", 0));
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {
            output.putInt("color", color);
            output.putInt("amount", amount);
            if (entityType != null) output.store("entityType", CompoundTag.CODEC, entityType);
        }
    }

    /** The landed pod: drops at most 16 steel ingots when broken, as in Space. */
    public static class PodBlock extends BaseEntityBlock implements ITickingBlock {
        public PodBlock(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(PodBlock::new);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new PodBE(pos, state);
        }
    }

    /** Waits two seconds, swings its hatches open and lets its cargo out; with combat_pod_despawn on it vanishes later. */
    public static class PodBE extends BlockEntityCompat {
        public @Nullable CompoundTag entityType;
        public int amount;
        public int color;
        public int delay = 40;
        public double hatchopen, hatchopen2, prevHatchopen, prevHatchopen2;

        public PodBE(BlockPos pos, BlockState state) {
            super(COMBAT_DROP_BE.get(), pos, state);
        }

        public void setPayload(CompoundTag entityType, int amount, int color) {
            this.entityType = entityType;
            this.amount = amount;
            this.color = color;
        }

        public static void tick(PodBE be) {
            be.update();
        }

        private void update() {
            Level level = getLevel();
            if (level == null) return;
            prevHatchopen = hatchopen;
            prevHatchopen2 = hatchopen2;
            if (delay > 0) {
                delay--;
                return;
            }
            hatchopen += (90 - hatchopen) * 0.2;
            hatchopen2 += (-90 - hatchopen2) * 0.2;

            BlockPos pos = getBlockPos();
            if (entityType != null && amount > 0 && !level.isClientSide()) {
                for (int i = 0; i < amount; i++) {
                    // a fresh UUID for every copy, or all but the first would be refused
                    CompoundTag tag = entityType.copy();
                    tag.remove("UUID");
                    EntityType.create(tag, level).ifPresent(entity -> {
                        entity.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, entity.getYRot(), entity.getXRot());
                        level.addFreshEntity(entity);
                    });
                }
                amount = 0;
                setChanged();
                level.playSound(null, pos, HATCH_OPEN.get(), SoundSource.BLOCKS, 10F, 0.9F);
                return;
            }
            if (!level.isClientSide() && level.getGameTime() % 1000 == 0
                    && NeoForgeHbmConfig.runtime().store().get(ConfigSchema.SPACE_COMBAT_POD_DESPAWN)) {
                level.removeBlock(pos, false);
            }
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (entityType != null) output.store("EntityType", CompoundTag.CODEC, entityType);
            output.putInt("amount", amount);
            output.putInt("color", color);
            output.putInt("delay", delay);
            output.putDouble("hatchopen", hatchopen);
            output.putDouble("hatchopen2", hatchopen2);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            entityType = input.read("EntityType", CompoundTag.CODEC).orElse(null);
            amount = input.getIntOr("amount", 0);
            color = input.getIntOr("color", 0);
            delay = input.getIntOr("delay", 40);
            hatchopen = input.getDoubleOr("hatchopen", 0);
            hatchopen2 = input.getDoubleOr("hatchopen2", 0);
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveWithoutMetadata(registries);
        }

        @Override
        public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
