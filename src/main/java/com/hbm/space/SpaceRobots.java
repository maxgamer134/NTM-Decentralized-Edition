// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.api.environment.ISuffocationImmune;
import com.hbm.backport.compat.MonsterCompat;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.special.ItemSpawnEgg;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.GasFlamePayload;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Thatmo's war machines: the tankbot and the war behemoth. */
public final class SpaceRobots {

    private SpaceRobots() {}

    public static final RegistryHandle<SoundEvent> BEEP = NeoForgeRegistrar.registerSound("entity.beep");

    public static final RegistryHandle<EntityType<Tankbot>> TANKBOT = Reg.entity("entity_tankbot",
            () -> EntityType.Builder.<Tankbot>of(Tankbot::new, MobCategory.MONSTER).sized(2.45F, 3.25F).fireImmune()
                    .clientTrackingRange(24).build(key("entity_tankbot")));
    public static final RegistryHandle<EntityType<WarBehemoth>> WAR_BEHEMOTH = Reg.entity("entity_war_behemoth",
            () -> EntityType.Builder.<WarBehemoth>of(WarBehemoth::new, MobCategory.MONSTER).sized(1.75F, 6.35F)
                    .clientTrackingRange(16).build(key("entity_war_behemoth")));

    static {
        Reg.item("entity_tankbot_spawn_egg", p -> new ItemSpawnEgg(() -> TANKBOT, 0x204131, 0x75CE00, p), Item.Properties::new);
        Reg.item("entity_war_behemoth_spawn_egg", p -> new ItemSpawnEgg(() -> WAR_BEHEMOTH, 0x204131, 0x75CE00, p), Item.Properties::new);
        NeoForgeRegistrar.registerLivingAttributes(TANKBOT, Tankbot::createAttributes);
        NeoForgeRegistrar.registerLivingAttributes(WAR_BEHEMOTH, WarBehemoth::createAttributes);
        NeoForgeRegistrar.registerSpawnPlacement(TANKBOT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
        NeoForgeRegistrar.registerSpawnPlacement(WAR_BEHEMOTH, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules);
    }

    private static String key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Library.id(name)).location().toString();
    }

    public static void init() {}

    private static @Nullable SpentCasing tankCasing;

    private static SpentCasing tankCasing() {
        if (tankCasing == null) {
            tankCasing = new SpentCasing(SpentCasing.CasingType.SHOTGUN).setColor(0xE5DD00, SpentCasing.COLOR_CASE_12GA).setScale(10F)
                    .register("TankGa").setupSmoke(0.02F, 0.5D, 60, 20).setMaxAge(60);
        }
        return tankCasing;
    }

    /** Both robots: eggs sometimes short them out, fire and magic don't hurt, bullets and blasts barely do. */
    abstract static class Robot extends MonsterCompat implements ISuffocationImmune {
        private final EntityDataAccessor<Integer> targetId;

        Robot(EntityType<? extends Robot> type, Level level, EntityDataAccessor<Integer> targetId) {
            super(type, level);
            this.targetId = targetId;
        }

        protected void robotGoals(Class<? extends Robot> self) {
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 0, true, true, e -> !self.isInstance(e)));
            targetSelector.addGoal(3, new HurtByTargetGoal(this));
        }

        public @Nullable LivingEntity syncedTarget() {
            return level().getEntity(entityData.get(targetId)) instanceof LivingEntity l ? l : null;
        }

        @Override
        public void setTarget(@Nullable LivingEntity target) {
            super.setTarget(target);
            entityData.set(targetId, target != null ? target.getId() : 0);
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
            if (source.getDirectEntity() instanceof ThrownEgg && random.nextInt(10) == 0) {
                xpReward = 0;
                setHealth(0);
                return true;
            }
            if (source.is(DamageTypeTags.IS_FIRE)) amount = 0;
            if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) amount = 0;
            if (source.is(DamageTypeTags.IS_PROJECTILE)) amount *= 0.25F;
            if (source.is(DamageTypeTags.IS_EXPLOSION)) amount *= 0.5F;
            if (amount > 50) amount = 50 + (amount - 50) * 0.25F;
            return super.hurtServer(level, source, amount);
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return ModSounds.ENTITY_CYBERCRAB.get();
        }

        @Override
        public boolean removeWhenFarAway(double distance) {
            return false;
        }
    }

    private static final EntityDataAccessor<Integer> TANKBOT_TARGET = SynchedEntityData.defineId(Tankbot.class, EntityDataSerializers.INT);

    /** Walks in steps toward its target, shelling it from afar and blasting buckshot up close. Burns and blows up when it dies. */
    public static class Tankbot extends Robot {
        /** Client only: where the turret has swung to, chasing the target. */
        public double headTargetYaw;

        public Tankbot(EntityType<? extends Tankbot> type, Level level) {
            super(type, level, TANKBOT_TARGET);
            deathTime = -50;
            robotGoals(Tankbot.class);
            goalSelector.addGoal(5, new RandomStrollGoal(this, 0.2D));
            goalSelector.addGoal(3, new StepTowardsTarget(this, 50, 0.2D, 200, 20, 0.6));
            goalSelector.addGoal(4, new Tankshell(this, 2, 10, 30));
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 60.0).add(Attributes.ATTACK_DAMAGE, 15.0)
                    .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MAX_HEALTH, 180.0);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(TANKBOT_TARGET, 0);
        }

        @Override
        protected void tickDeath() {
            if (deathTime == -30) playSound(ModSounds.CHOPPER_DAMAGE.get(), 10.0F, 1.0F);
            if (deathTime >= -30 && level() instanceof ServerLevel server) {
                PacketWire.sendToAllAround(new GasFlamePayload(getX(), getY() + 1, getZ(), random.nextGaussian(), 0.4, random.nextGaussian(),
                        GasFlamePayload.DEFAULT_SCALE), new TargetPoint(server, getX(), getY(), getZ(), 100));
            }
            if (deathTime == -5 && !level().isClientSide()) {
                level().explode(this, getX(), getY(), getZ(), 5F, true, Level.ExplosionInteraction.NONE);
                ExplosionCreator.composeEffectSmall(level(), getX(), getY(), getZ());
                discard();
                return;
            }
            super.tickDeath();
        }

        @Override
        public void tick() {
            super.tick();
            // the body turns slowly after the turret
            float diff = Mth.clamp(Mth.wrapDegrees(getYHeadRot() - getYRot()), -1F, 1F);
            setYRot(getYRot() + diff);
        }
    }

    private static final EntityDataAccessor<Integer> BEHEMOTH_TARGET = SynchedEntityData.defineId(WarBehemoth.class, EntityDataSerializers.INT);

    /** A two-legged gun platform: a gatling up close, artillery shells from afar. Heavy, and steps over five blocks. */
    public static class WarBehemoth extends Robot {
        /** Client only: how far the hull has tipped to aim at the target. */
        public float aimPitch;

        public WarBehemoth(EntityType<? extends WarBehemoth> type, Level level) {
            super(type, level, BEHEMOTH_TARGET);
            robotGoals(WarBehemoth.class);
            goalSelector.addGoal(3, new StepTowardsTarget(this, 50, 0.3D, 50, 10, 0.6));
            goalSelector.addGoal(4, new BehemothGun(this, 3, 35, 30));
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 50.0).add(Attributes.ATTACK_DAMAGE, 15.0)
                    .add(Attributes.KNOCKBACK_RESISTANCE, 100.0).add(Attributes.MAX_HEALTH, 200.0).add(Attributes.STEP_HEIGHT, 5.0);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(BEHEMOTH_TARGET, 0);
        }

        @Override
        public void tick() {
            super.tick();
            // heavier than gravity alone
            if (!level().isClientSide()) setDeltaMovement(getDeltaMovement().add(0, -0.10D, 0));
        }
    }

    /** Beeps and strides toward its target in long steps, or wanders a few blocks when it has none. */
    static class StepTowardsTarget extends Goal {
        private final PathfinderMob entity;
        private final double stepDistance, speed, minDistance;
        private final int stepDuration, stepCooldown;
        private int stepTimer;

        StepTowardsTarget(PathfinderMob entity, double stepDistance, double speed, int stepDuration, int stepCooldown, double minDistance) {
            this.entity = entity;
            this.stepDistance = stepDistance;
            this.speed = speed;
            this.stepDuration = stepDuration;
            this.stepCooldown = stepCooldown;
            this.minDistance = minDistance;
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void start() {
            stepTimer = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (stepTimer > 0) {
                stepTimer--;
                return;
            }
            LivingEntity target = entity.getTarget();
            if (target != null && target.isAlive()) {
                double dx = target.getX() - entity.getX();
                double dz = target.getZ() - entity.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                entity.playSound(BEEP.get(), 1.0F, 1.0F);
                if (distance > minDistance) {
                    entity.getNavigation().moveTo(entity.getX() + dx / distance * stepDistance, entity.getY(),
                            entity.getZ() + dz / distance * stepDistance, speed);
                }
            } else {
                int x = Mth.floor(entity.getX()) + entity.getRandom().nextInt(10) - 5;
                int z = Mth.floor(entity.getZ()) + entity.getRandom().nextInt(10) - 5;
                entity.getNavigation().moveTo(x, Mth.floor(entity.getY()), z, speed);
            }
            stepTimer = stepDuration + stepCooldown;
        }
    }

    /** Shared by both guns: aims while the target is between 2 and 50 blocks away. */
    abstract static class RobotGun extends Goal {
        protected final PathfinderMob owner;
        protected LivingEntity target;
        private final int delay, switchDistance;
        private int timer;
        protected boolean artilleryMode;
        protected int reloadTimer;
        protected final int reloadDelay;

        RobotGun(PathfinderMob owner, int delay, int switchDistance, int reloadDelay) {
            this.owner = owner;
            this.delay = delay;
            this.timer = delay;
            this.switchDistance = switchDistance;
            this.reloadTimer = reloadDelay;
            this.reloadDelay = reloadDelay;
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            LivingEntity entity = owner.getTarget();
            if (entity == null) return false;
            target = entity;
            double dist = entity.position().subtract(owner.position()).length();
            artilleryMode = dist > switchDistance;
            return dist > 2 && dist < 50;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || !owner.getNavigation().isDone();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) return;
            if (--timer <= 0) {
                fire();
                timer = delay;
            }
            owner.setYRot(owner.getYHeadRot());
        }

        protected abstract void fire();

        protected Vec3 aim(double xOffset, double yOffset, double zOffset) {
            return new Vec3(target.getX() - (owner.getX() + xOffset), target.getY() + target.getEyeHeight() - (owner.getY() + yOffset),
                    target.getZ() - (owner.getZ() + zOffset)).normalize();
        }

        protected void bullet(BulletConfig config, float damage, double x, double y, double z, Vec3 motion) {
            EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(owner.level(), owner, config, damage * config.damageMult, 0.01F,
                    x, y, z, motion.x, motion.y, motion.z);
            bullet.setPos(x, y, z);
            bullet.setDeltaMovement(motion);
            owner.level().addFreshEntity(bullet);
        }
    }

    /** The tankbot's gun: rockets past 30 blocks (stronger ones once it's badly hurt), eight explosive buckshot closer in. */
    static class Tankshell extends RobotGun {
        Tankshell(PathfinderMob owner, int delay, int switchDistance, int reloadDelay) {
            super(owner, delay, switchDistance, reloadDelay);
        }

        @Override
        protected void fire() {
            if (!artilleryMode) return;
            if (reloadTimer <= 0) {
                double zOffset = Math.sin(Math.toRadians(owner.getYHeadRot())) * 1.5;
                double distance = new Vec3(target.getX() - owner.getX(), target.getY() + target.getEyeHeight() - (owner.getY() + 10),
                        target.getZ() - (owner.getZ() + zOffset)).length();
                Vec3 dir = aim(0, 10, zOffset);
                CasingCreator.composeEffect(owner.level(), owner, 0, 0, 0, -1, 0.6, 0, 0.1, tankCasing().getName());
                double x = owner.getX(), y = owner.getY() + 3, z = owner.getZ();
                if (distance > 30) {
                    BulletConfig rocket = XFactoryRocket.rocket_qd[owner.getHealth() < 30 ? 3 : 1];
                    bullet(rocket, 4, x, y, z, new Vec3(dir.x * 0.5D, dir.y * 0.1D, dir.z * 0.5D));
                } else {
                    for (int i = 0; i < 8; i++) {
                        bullet(XFactory12ga.g12_explosive, 4, x, y, z, new Vec3(dir.x * (0.2 + 0.5 * Math.random()), dir.y * 0.1,
                                dir.z * (0.2 + 0.5 * Math.random())));
                    }
                }
                owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.TURRET_JEREMY_FIRE.get(), SoundSource.HOSTILE, 25.0F, 1.0F);
                reloadTimer = reloadDelay;
            } else {
                reloadTimer--;
            }
            if (reloadTimer == 20) {
                owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.TURRET_JEREMY_RELOAD.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
            }
        }
    }

    /** The behemoth's guns: armour-piercing 7.62 up close, an artillery shell lobbed high past 35 blocks. */
    static class BehemothGun extends RobotGun {
        BehemothGun(PathfinderMob owner, int delay, int switchDistance, int reloadDelay) {
            super(owner, delay, switchDistance, reloadDelay);
        }

        @Override
        protected void fire() {
            if (artilleryMode) artillery();
            else gatling();
        }

        private void gatling() {
            double xOffset = Math.cos(Math.toRadians(owner.getYRot())) * 1.5;
            double zOffset = Math.sin(Math.toRadians(owner.getYRot())) * 1.5;
            double yOffset = 10;
            Vec3 dir = aim(xOffset, yOffset, zOffset);
            bullet(XFactory762mm.r762_ap, 0.5F, owner.getX() + xOffset, owner.getY() + yOffset, owner.getZ() + zOffset, dir.scale(0.5D));
            owner.playSound(ModSounds.GUN_MINIGUN_FIRE.get(), 15.0F, 1.0F);
        }

        private void artillery() {
            if (reloadTimer <= 0) {
                EntityArtilleryShell shell = new EntityArtilleryShell(ModEntities.ARTILLERY_SHELL.get(), owner.level());
                shell.setType(9);
                shell.setPos(owner.getX(), owner.getY() + 14, owner.getZ());
                double mx = (target.getX() - owner.getX()) * 0.05D;
                double my = 0.5D + owner.getRandom().nextDouble() * 0.5D;
                double mz = (target.getZ() - owner.getZ()) * 0.05D;
                shell.setDeltaMovement(mx, my, mz);
                shell.setThrowableHeading(mx, my * 6, mz, 5F, 0);
                shell.setTarget(target.getX(), target.getY(), target.getZ());
                owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.TURRET_JEREMY_FIRE.get(), SoundSource.HOSTILE, 25.0F, 1.0F);
                owner.level().addFreshEntity(shell);
                reloadTimer = reloadDelay;
            } else {
                reloadTimer--;
            }
        }
    }
}
