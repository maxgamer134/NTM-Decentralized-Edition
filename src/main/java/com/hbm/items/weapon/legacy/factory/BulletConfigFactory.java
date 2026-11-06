// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy.factory;

import com.hbm.items.weapon.legacy.BulletConfiguration;
import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.hbm.entity.projectile.EntityBulletBaseNT.IBulletHurtBehaviorNT;
import com.hbm.entity.projectile.EntityBulletBaseNT.IBulletImpactBehaviorNT;
import com.hbm.entity.projectile.EntityBulletBaseNT.IBulletUpdateBehaviorNT;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.explosion.ExplosionNukeSmall.MukeParams;
import com.hbm.hazard.HazardClass;
import com.hbm.util.ArmorRegistry;
import com.hbm.items.weapon.legacy.BulletConfigSyncingUtil;
import com.hbm.items.weapon.legacy.LegacyFx;
import com.hbm.potion.HbmPotion;
import com.hbm.util.BobMathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/BulletConfigFactory: the base bullets. */
public final class BulletConfigFactory {

    public static final float defaultSpread = 0.005F;

    private BulletConfigFactory() {}

    public static BulletConfiguration standardBulletConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 5.0F;
        bullet.spread = defaultSpread;
        bullet.wear = 10;
        bullet.bulletsMin = 1;
        bullet.bulletsMax = 1;
        bullet.gravity = 0D;
        bullet.maxAge = 100;
        bullet.doesRicochet = true;
        bullet.ricochetAngle = 5;
        bullet.HBRC = 2;
        bullet.LBRC = 95;
        bullet.bounceMod = 0.8;
        bullet.doesPenetrate = true;
        bullet.doesBreakGlass = true;
        bullet.destroysBlocks = false;
        bullet.style = BulletConfiguration.STYLE_NORMAL;
        bullet.plink = BulletConfiguration.PLINK_BULLET;
        bullet.leadChance = 5;
        return bullet;
    }

    public static BulletConfiguration standardPistolConfig() {
        BulletConfiguration bullet = standardBulletConfig();
        bullet.style = BulletConfiguration.STYLE_PISTOL;
        bullet.plink = BulletConfiguration.PLINK_BULLET;
        return bullet;
    }

    public static BulletConfiguration standardBuckshotConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 5.0F;
        bullet.spread = defaultSpread * 10F;
        bullet.wear = 10;
        bullet.bulletsMin = 6;
        bullet.bulletsMax = 8;
        bullet.gravity = 0D;
        bullet.maxAge = 100;
        bullet.doesRicochet = true;
        bullet.ricochetAngle = 5;
        bullet.HBRC = 10;
        bullet.LBRC = 95;
        bullet.bounceMod = 0.8;
        bullet.doesPenetrate = false;
        bullet.doesBreakGlass = true;
        bullet.style = BulletConfiguration.STYLE_PELLET;
        bullet.plink = BulletConfiguration.PLINK_BULLET;
        bullet.leadChance = 10;
        return bullet;
    }

    public static BulletConfiguration standardAirstrikeConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 5.0F;
        bullet.spread = 0.0F;
        bullet.wear = 50;
        bullet.bulletsMin = 1;
        bullet.bulletsMax = 1;
        bullet.gravity = 0D;
        bullet.maxAge = 100;
        bullet.doesRicochet = false;
        bullet.doesPenetrate = false;
        bullet.doesBreakGlass = false;
        bullet.style = BulletConfiguration.STYLE_BOLT;
        bullet.leadChance = 0;
        bullet.vPFX = "reddust";
        bullet.bntImpact = (b, pos, side) -> {
            Level level = b.level();
            if (level.isClientSide()) return;
            RandomSource rand = level.random;
            int count = rand.nextInt(11) + 95;
            for (int i = 0; i < count; i++) {
                double dx = b.getX() + rand.nextGaussian() * 4;
                double dy = b.getY() + 25 + rand.nextGaussian() * 5;
                double dz = b.getZ() + rand.nextGaussian() * 4;
                Vec3 motion = new Vec3(b.getX() - dx, b.getY() - dy, b.getZ() - dz).normalize();
                EntityBulletBaseNT bolt = new EntityBulletBaseNT(level, BulletConfigSyncingUtil.R556_FLECHETTE_DU);
                bolt.setPos(dx, dy, dz);
                bolt.setThrowableHeading(motion.x, motion.y, motion.z, 0.5F, 0.1F);
                if (b.getThrower() != null) bolt.initThrower(b.getThrower());
                level.addFreshEntity(bolt);
                // backport: main spawned EntityBSmokeFX (a client effect entity) for the first 30
                if (i < 30) LegacyFx.vanillaBurst(level, dx, dy, dz, ParticleTypes.LARGE_SMOKE, 1, 0);
            }
        };
        return bullet;
    }

    public static BulletConfiguration standardRocketConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 2.0F;
        bullet.spread = defaultSpread;
        bullet.wear = 10;
        bullet.bulletsMin = 1;
        bullet.bulletsMax = 1;
        bullet.gravity = 0.005D;
        bullet.maxAge = 300;
        bullet.doesRicochet = true;
        bullet.ricochetAngle = 10;
        bullet.HBRC = 2;
        bullet.LBRC = 100;
        bullet.bounceMod = 0.8;
        bullet.doesPenetrate = false;
        bullet.doesBreakGlass = false;
        bullet.explosive = 5.0F;
        bullet.style = BulletConfiguration.STYLE_ROCKET;
        bullet.plink = BulletConfiguration.PLINK_GRENADE;
        bullet.vPFX = "smoke";
        return bullet;
    }

    public static BulletConfiguration standardGrenadeConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 2.0F;
        bullet.spread = defaultSpread;
        bullet.wear = 10;
        bullet.bulletsMin = 1;
        bullet.bulletsMax = 1;
        bullet.gravity = 0.035D;
        bullet.maxAge = 300;
        bullet.doesRicochet = false;
        bullet.ricochetAngle = 0;
        bullet.HBRC = 0;
        bullet.LBRC = 0;
        bullet.bounceMod = 1.0;
        bullet.doesPenetrate = false;
        bullet.doesBreakGlass = false;
        bullet.explosive = 2.5F;
        bullet.style = BulletConfiguration.STYLE_GRENADE;
        bullet.plink = BulletConfiguration.PLINK_GRENADE;
        bullet.vPFX = "smoke";
        return bullet;
    }

    public static BulletConfiguration standardShellConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 3.0F;
        bullet.spread = defaultSpread;
        bullet.wear = 10;
        bullet.bulletsMin = 1;
        bullet.bulletsMax = 1;
        bullet.gravity = 0.005D;
        bullet.maxAge = 300;
        bullet.doesRicochet = true;
        bullet.ricochetAngle = 10;
        bullet.HBRC = 2;
        bullet.LBRC = 100;
        bullet.bounceMod = 0.8;
        bullet.doesPenetrate = false;
        bullet.doesBreakGlass = false;
        bullet.style = BulletConfiguration.STYLE_GRENADE;
        bullet.plink = BulletConfiguration.PLINK_GRENADE;
        bullet.vPFX = "smoke";
        return bullet;
    }

    public static BulletConfiguration standardNukeConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 3.0F;
        bullet.spread = defaultSpread;
        bullet.wear = 10;
        bullet.bulletsMin = 1;
        bullet.bulletsMax = 1;
        bullet.dmgMin = 1000;
        bullet.dmgMax = 1000;
        bullet.gravity = 0.025D;
        bullet.maxAge = 300;
        bullet.doesRicochet = false;
        bullet.ricochetAngle = 0;
        bullet.HBRC = 0;
        bullet.LBRC = 0;
        bullet.bounceMod = 1.0;
        bullet.doesPenetrate = true;
        bullet.doesBreakGlass = false;
        bullet.style = BulletConfiguration.STYLE_NUKE;
        bullet.plink = BulletConfiguration.PLINK_GRENADE;
        return bullet;
    }

    /** sizes: 0 safe, 1 tot, 2 small, 3 medium, 4 big; a null pos means the bullet's own position */
    public static void nuclearExplosion(Entity entity, @Nullable BlockPos pos, MukeParams params) {
        if (entity.level().isClientSide()) return;
        double posX = entity.getX();
        double posY = entity.getY() + 0.5;
        double posZ = entity.getZ();
        if (pos != null && pos.getY() >= entity.level().getMinBuildHeight()) {
            posX = pos.getX() + 0.5;
            posY = pos.getY() + 1.5;
            posZ = pos.getZ() + 0.5;
        }
        ExplosionNukeSmall.explode(entity.level(), posX, posY, posZ, params);
    }

    public static void makeFlechette(BulletConfiguration bullet) {
        bullet.bntImpact = (b, pos, side) -> {
            if (pos != null && side != null) b.getStuck(pos, side.get3DDataValue());
        };
    }

    public static IBulletImpactBehaviorNT getPhosphorousEffect(
            final int radius, final int duration, final int count, final double motion, float hazeChance) {
        return (b, pos, side) -> {
            Level level = b.level();
            for (Entity e : level.getEntities(b, b.getBoundingBox().inflate(radius))) {
                if (!LegacyFx.isObstructed(level, b, b.getX(), b.getY(), b.getZ(), e.getX(), e.getEyeY(), e.getZ())) {
                    e.igniteForSeconds(5);
                    if (e instanceof LivingEntity living)
                        living.addEffect(new MobEffectInstance(
                                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HbmPotion.phosphorus.get()), duration, 0, true, true));
                }
            }
            LegacyFx.vanillaBurst(level, b.getX(), b.getY(), b.getZ(), ParticleTypes.FLAME, count, motion);
            if (level.random.nextFloat() < hazeChance) LegacyFx.haze(level, b.getX(), b.getY(), b.getZ());
        };
    }

    public static IBulletImpactBehaviorNT getGasEffect(final int radius, final int duration) {
        return (b, pos, side) -> {
            Level level = b.level();
            for (Entity e : level.getEntities(b, b.getBoundingBox().inflate(radius))) {
                if (LegacyFx.isObstructed(level, b, b.getX(), b.getY(), b.getZ(), e.getX(), e.getEyeY(), e.getZ())) continue;
                if (!(e instanceof LivingEntity entity)) continue;
                if (ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, HazardClass.GAS_LUNG)) continue;
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 2, true, true));
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2, true, true));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 4, true, true));
                entity.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) Math.ceil(duration * 0.1), 0, true, true));
            }
            LegacyFx.vanillaBurst(level, b.getX(), b.getY(), b.getZ(), ParticleTypes.CLOUD, 15, 0.1D);
        };
    }

    public static IBulletUpdateBehaviorNT getLaserSteering() {
        return b -> {
            if (!(b.getThrower() instanceof Player player)) return;
            if (b.position().distanceTo(player.position()) > 100) return;
            HitResult mop = player.pick(200, 1F, false);
            if (mop == null || mop.getType() == HitResult.Type.MISS) return;
            Vec3 vec = mop.getLocation().subtract(b.position());
            if (vec.length() < 3) return;
            b.setDeltaMovement(vec.normalize().scale(b.getDeltaMovement().length()));
        };
    }

    public static IBulletUpdateBehaviorNT getHomingBehavior(final double range, final double angle) {
        return b -> {
            Level level = b.level();
            if (level.isClientSide()) return;
            if (level.getEntity(b.homingTarget) == null) chooseTarget(b, range, angle);
            Entity target = level.getEntity(b.homingTarget);
            if (target != null) {
                Vec3 delta = new Vec3(
                                target.getX() - b.getX(),
                                target.getY() + target.getBbHeight() / 2 - b.getY(),
                                target.getZ() - b.getZ())
                        .normalize();
                b.setDeltaMovement(delta.scale(b.getDeltaMovement().length()));
            }
        };
    }

    private static void chooseTarget(EntityBulletBaseNT b, double range, double angle) {
        Level level = b.level();
        Vec3 mot = b.getDeltaMovement();
        LivingEntity target = null;
        double targetAngle = angle;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, b.getBoundingBox().inflate(range))) {
            if (!e.isAlive() || e == b.getThrower()) continue;
            Vec3 delta = new Vec3(e.getX() - b.getX(), e.getY() + e.getBbHeight() / 2 - b.getY(), e.getZ() - b.getZ());
            if (LegacyFx.isObstructed(level, b, b.getX(), b.getY(), b.getZ(), e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ()))
                continue;
            if (e.distanceToSqr(b) < range * range) {
                double deltaAngle = BobMathUtil.getCrossAngle(mot, delta);
                // main's penetrating-bullet check compared a list against null, so it never skipped
                if (deltaAngle < targetAngle) {
                    target = e;
                    targetAngle = deltaAngle;
                }
            }
        }
        if (target != null) b.homingTarget = target.getId();
    }

    /** resets the bullet's target */
    public static IBulletHurtBehaviorNT getPenHomingBehavior() {
        return (b, hit) -> b.homingTarget = 0;
    }
}
