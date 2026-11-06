// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.blocks.bomb.BlockDetonatable;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.effect.EntityEMPBlast;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm.explosion.vanillant.standard.BlockProcessorNoDamage;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.weapon.legacy.BulletConfigSyncingUtil;
import com.hbm.items.weapon.legacy.BulletConfiguration;
import com.hbm.items.weapon.legacy.GunConfiguration;
import com.hbm.items.weapon.legacy.ItemGunBase;
import com.hbm.items.weapon.legacy.LegacySounds;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.potion.HbmPotion;
import com.hbm.util.BobMathUtil;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's pre-rework bullet (entity/projectile/EntityBulletBaseNT):
 * a throwable with main's ricochet, headshot and impact effects, driven by a BulletConfiguration.
 */
public class EntityBulletBaseNT extends EntityThrowableInterp {

    private static final EntityDataAccessor<Byte> STYLE =
            SynchedEntityData.defineId(EntityBulletBaseNT.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> TRAIL =
            SynchedEntityData.defineId(EntityBulletBaseNT.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> CONFIG =
            SynchedEntityData.defineId(EntityBulletBaseNT.class, EntityDataSerializers.INT);

    private BulletConfiguration config;
    public float overrideDamage;
    /** main kept this in getEntityData() for the homing bullets */
    public int homingTarget;

    public EntityBulletBaseNT(EntityType<? extends EntityBulletBaseNT> type, Level level) {
        super(type, level);
    }

    public BulletConfiguration getConfig() {
        return config;
    }

    public int getStyle() {
        return this.entityData.get(STYLE);
    }

    public int getTrail() {
        return this.entityData.get(TRAIL);
    }

    private void setConfig(int id) {
        this.config = BulletConfigSyncingUtil.pullConfig(id);
        this.entityData.set(CONFIG, id);
        if (this.config == null) {
            this.discard();
            return;
        }
        this.entityData.set(STYLE, (byte) this.config.style);
        this.entityData.set(TRAIL, (byte) this.config.trail);
    }

    public EntityBulletBaseNT(Level world, int config) {
        this(ModEntities.BULLET_NT.get(), world);
        setConfig(config);
    }

    public EntityBulletBaseNT(Level world, int config, LivingEntity entity) {
        this(ModEntities.BULLET_NT.get(), world);
        setConfig(config);
        if (this.config == null) return;
        setThrower(entity);

        ItemStack gun = entity.getMainHandItem();
        boolean offsetShot = true;
        boolean accuracyBoost = false;

        if (gun.getItem() instanceof ItemGunBase base) {
            GunConfiguration cfg = base.mainConfig;

            if (cfg != null) {
                if (cfg.hasSights && entity.isShiftKeyDown()) {
                    offsetShot = false;
                    accuracyBoost = true;
                }

                if (cfg.isCentered) offsetShot = false;
            }
        }

        this.moveTo(
                entity.getX(),
                entity.getY() + entity.getEyeHeight(),
                entity.getZ(),
                entity.getYRot(),
                entity.getXRot());

        double x = this.getX(), y = this.getY() - 0.1D, z = this.getZ();
        if (offsetShot) {
            double sideOffset = 0.16D;
            x -= Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * sideOffset;
            z -= Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * sideOffset;
        }
        this.setPos(x, y, z);

        double motionX =
                -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionZ =
                Mth.cos(this.getYRot() / 180.0F * (float) Math.PI)
                        * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI);
        double motionY = -Mth.sin(this.getXRot() / 180.0F * (float) Math.PI);
        this.setDeltaMovement(motionX, motionY, motionZ);

        this.setThrowableHeading(
                motionX, motionY, motionZ, 1.0F, this.config.spread * (accuracyBoost ? 0.25F : 1F));
    }

    public EntityBulletBaseNT(
            Level world, int config, LivingEntity entity, LivingEntity target, float motion, float deviation) {
        this(ModEntities.BULLET_NT.get(), world);
        setConfig(config);
        if (this.config == null) return;
        setThrower(entity);

        double y = entity.getY() + entity.getEyeHeight() - 0.10000000149011612D;
        double d0 = target.getX() - entity.getX();
        double d1 = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - y;
        double d2 = target.getZ() - entity.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        if (d3 >= 1.0E-7D) {
            float f2 = (float) (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
            float f3 = (float) (-(Math.atan2(d1, d3) * 180.0D / Math.PI));
            double d4 = d0 / d3;
            double d5 = d2 / d3;
            this.moveTo(entity.getX() + d4, y, entity.getZ() + d5, f2, f3);
            this.setThrowableHeading(d0, d1, d2, motion, deviation);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STYLE, (byte) 0);
        builder.define(TRAIL, (byte) 0);
        builder.define(CONFIG, 0);
    }

    @Override
    public void tick() {

        if (config == null) config = BulletConfigSyncingUtil.pullConfig(this.entityData.get(CONFIG));

        if (config == null) {
            this.discard();
            return;
        }

        Level world = this.level();

        if (world.isClientSide() && this.config.blackPowder && this.tickCount == 1) {
            Vec3 m = this.getDeltaMovement();
            for (int i = 0; i < 15; i++) {
                double mod = random.nextDouble();
                world.addParticle(
                        ParticleTypes.SMOKE,
                        getX(),
                        getY(),
                        getZ(),
                        (m.x + random.nextGaussian() * 0.05) * mod,
                        (m.y + random.nextGaussian() * 0.05) * mod,
                        (m.z + random.nextGaussian() * 0.05) * mod);
            }
            double mod = 0.5;
            world.addParticle(
                    ParticleTypes.FLAME, getX() + m.x * mod, getY() + m.y * mod, getZ() + m.z * mod, 0, 0, 0);
        }

        if (!world.isClientSide()) {

            if (config.maxAge == 0) {
                if (this.config.bntUpdate != null) this.config.bntUpdate.behaveUpdate(this);
                this.discard();
                return;
            }

            if (this.tickCount > config.maxAge) this.discard();
        }

        if (this.config.bntUpdate != null) this.config.bntUpdate.behaveUpdate(this);

        super.tick();
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (mop instanceof BlockHitResult hit && mop.getType() == HitResult.Type.BLOCK) {

            boolean hRic = random.nextInt(100) < config.HBRC;
            boolean doesRic = config.doesRicochet && hRic;

            if (!config.isSpectral && !doesRic) {
                this.setPos(mop.getLocation());
                this.onBlockImpact(hit.getBlockPos(), hit.getDirection());
            }

            if (doesRic) {

                Direction side = hit.getDirection();
                Vec3 face = new Vec3(side.getStepX(), side.getStepY(), side.getStepZ());
                Vec3 vel = this.getDeltaMovement();

                boolean lRic = random.nextInt(100) < config.LBRC;
                double angle = Math.abs(BobMathUtil.getCrossAngle(vel, face) - 90);

                if (hRic || (angle <= config.ricochetAngle && lRic)) {
                    switch (side.getAxis()) {
                        case Y -> this.setDeltaMovement(vel.x, -vel.y, vel.z);
                        case Z -> this.setDeltaMovement(vel.x, vel.y, -vel.z);
                        case X -> this.setDeltaMovement(-vel.x, vel.y, vel.z);
                    }

                    if (config.plink == 1) LegacySounds.play(this, "hbm:weapon.ricochet", 0.25F, 1.0F);
                    if (config.plink == 2) LegacySounds.play(this, "hbm:weapon.gBounce", 1.0F, 1.0F);

                    this.setPos(mop.getLocation());
                    onRicochet(hit.getBlockPos());

                } else {
                    if (!level().isClientSide()) {
                        this.setPos(mop.getLocation());
                        onBlockImpact(hit.getBlockPos(), side);
                    }
                }

                this.setDeltaMovement(this.getDeltaMovement().scale(config.bounceMod));
            }
        }

        if (mop instanceof EntityHitResult entityHit && !level().isClientSide()) {

            Entity victim = entityHit.getEntity();
            DamageSource damagesource = this.config.getDamage(this, this.getThrower());

            if (!config.doesPenetrate) {
                this.setPos(mop.getLocation());
                onEntityImpact(victim);
            } else {
                onEntityHurt(victim);
            }

            float damage = random.nextFloat() * (config.dmgMax - config.dmgMin) + config.dmgMin;

            if (overrideDamage != 0) damage = overrideDamage;

            boolean headshot = false;

            if (victim instanceof LivingEntity living && this.config.headshotMult > 1F) {
                double head = living.getBbHeight() - living.getEyeHeight();

                if (living.isAlive()
                        && mop.getLocation().y > (living.getY() + living.getBbHeight() - head * 2)) {
                    damage *= this.config.headshotMult;
                    headshot = true;
                }
            }

            // main's lastDamage trick: a hit inside the victim's i-frames still lands in full
            if (!EntityDamageUtil.attackEntityFromIgnoreIFrame(victim, damagesource, damage))
                headshot = false;

            if (headshot && victim instanceof LivingEntity living && level() instanceof ServerLevel server) {
                double head = living.getBbHeight() - living.getEyeHeight();
                server.sendParticles(
                        new BlockParticleOption(
                                ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                        living.getX(),
                        living.getY() + living.getBbHeight() - head,
                        living.getZ(),
                        15,
                        0,
                        0,
                        0,
                        0.1D);
                LegacySounds.play(
                        server,
                        victim.getX(),
                        victim.getY(),
                        victim.getZ(),
                        "mob.zombie.woodbreak",
                        1.0F,
                        0.95F + random.nextFloat() * 0.2F);
            }
        }
    }

    // for when a bullet dies by hitting a block
    private void onBlockImpact(BlockPos pos, Direction sideHit) {

        Level world = this.level();
        BlockState block = pos == null ? Blocks.AIR.defaultBlockState() : world.getBlockState(pos);

        if (config.bntImpact != null) config.bntImpact.behaveBlockHit(this, pos, sideHit);

        if (!world.isClientSide()) {
            if (!config.liveAfterImpact && !config.isSpectral && pos != null && !this.inGround)
                this.discard();
            if (!config.doesPenetrate && pos == null) this.discard();
        }

        if (world.isClientSide()) return;

        if (config.incendiary > 0) {
            BlockPos here = this.blockPosition();
            for (BlockPos at :
                    new BlockPos[] {
                        here, here.east(), here.west(), here.above(), here.below(), here.south(), here.north()
                    }) {
                if (random.nextInt(3) == 0 && world.getBlockState(at).isAir())
                    world.setBlockAndUpdate(at, Blocks.FIRE.defaultBlockState());
            }
        }

        if (config.emp > 0)
            ExplosionNukeGeneric.empBlast(
                    world, BlockPos.containing(getX() + 0.5D, getY() + 0.5D, getZ() + 0.5D), config.emp);

        if (config.emp > 3) {
            EntityEMPBlast cloud = new EntityEMPBlast(ModEntities.EMP_BLAST.get(), world);
            cloud.setMaxAge(config.emp);
            cloud.setPos(getX(), getY() + 0.5F, getZ());
            world.addFreshEntity(cloud);
        }

        if (config.jolt > 0) ExplosionLarge.jolt(world, getX(), getY(), getZ(), (int) config.jolt, 150, 0.25);

        if (config.explosive > 0) {
            ExplosionVNT vnt = new ExplosionVNT(world, getX(), getY(), getZ(), config.explosive, this.getThrower());
            vnt.setBlockAllocator(new BlockAllocatorStandard());
            if (config.blockDamage)
                vnt.setBlockProcessor(
                        new BlockProcessorStandard()
                                .withBlockEffect(config.incendiary > 0 ? new BlockMutatorFire() : null));
            else
                vnt.setBlockProcessor(
                        new BlockProcessorNoDamage()
                                .withBlockEffect(config.incendiary > 0 ? new BlockMutatorFire() : null));
            vnt.setEntityProcessor(new EntityProcessorStandard().allowSelfDamage());
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectStandard());
            vnt.explode();
        }

        if (config.shrapnel > 0) ExplosionLarge.spawnShrapnels(world, getX(), getY(), getZ(), config.shrapnel);

        if (config.rainbow > 0) {
            EntityNukeExplosionMK3 ex =
                    EntityNukeExplosionMK3.statFacFleija(world, getX(), getY(), getZ(), config.rainbow);
            if (!ex.isRemoved()) {
                LegacySounds.play(
                        world, getX(), getY(), getZ(), "random.explode", 100.0f, random.nextFloat() * 0.1F + 0.9F);
                world.addFreshEntity(ex);
                world.addFreshEntity(
                        EntityCloudFleijaRainbow.statFac(world, config.rainbow, getX(), getY(), getZ()));
            }
        }

        if (config.nuke > 0 && world instanceof ServerLevel server) {
            world.addFreshEntity(EntityNukeExplosionMK5.statFac(world, config.nuke, getX(), getY(), getZ()));
            PacketWire.sendToAllAround(
                    new MukePayload(getX(), getY() + 0.5, getZ(), false, random.nextInt(100) == 0),
                    new TargetPoint(server, getX(), getY(), getZ(), 250));
            LegacySounds.play(world, getX(), getY(), getZ(), "hbm:weapon.mukeExplosion", 15.0F, 1.0F);
        }

        if (pos == null) return;

        if (config.destroysBlocks) {
            if (block.getDestroySpeed(world, pos) <= 120) world.destroyBlock(pos, false);
        } else if (config.doesBreakGlass) {
            if (block.is(Tags.Blocks.GLASS_BLOCKS) || block.is(Tags.Blocks.GLASS_PANES))
                world.destroyBlock(pos, false);

            if (block.getBlock() instanceof BlockDetonatable detonatable) detonatable.onShot(world, pos);
        }
    }

    // for when a bullet dies by hitting a block
    private void onRicochet(BlockPos pos) {
        if (config.bntRicochet != null) config.bntRicochet.behaveBlockRicochet(this, pos);
    }

    // for when a bullet dies by hitting an entity
    private void onEntityImpact(Entity e) {
        onEntityHurt(e);
        onBlockImpact(null, null);

        if (config.bntHit != null) config.bntHit.behaveEntityHit(this, e);
    }

    // for when a bullet hurts an entity, not necessarily dying
    private void onEntityHurt(Entity e) {

        if (config.bntHurt != null) config.bntHurt.behaveEntityHurt(this, e);

        if (level().isClientSide()) return;

        if (config.incendiary > 0) e.igniteForSeconds(config.incendiary);

        if (config.leadChance > 0 && random.nextInt(100) < config.leadChance && e instanceof LivingEntity living)
            living.addEffect(
                    new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HbmPotion.lead.get()), 10 * 20, 0));

        if (e instanceof LivingEntity living && config.effects != null) {
            for (MobEffectInstance effect : config.effects) living.addEffect(new MobEffectInstance(effect));
        }

        if (config.instakill && e instanceof LivingEntity living) {
            if (!(e instanceof Player player && player.isCreative())) living.setHealth(0.0F);
        }

        if (config.caustic > 0 && e instanceof Player player) {
            ArmorUtil.damageSuit(player, EquipmentSlot.HEAD, config.caustic);
            ArmorUtil.damageSuit(player, EquipmentSlot.CHEST, config.caustic);
            ArmorUtil.damageSuit(player, EquipmentSlot.LEGS, config.caustic);
            ArmorUtil.damageSuit(player, EquipmentSlot.FEET, config.caustic);
        }
    }

    @Override
    public boolean doesPenetrate() {
        return this.config.doesPenetrate;
    }

    @Override
    public boolean isSpectral() {
        return this.config.isSpectral;
    }

    @Override
    public int selfDamageDelay() {
        return this.config.selfDamageDelay;
    }

    @Override
    protected double headingForceMult() {
        return 1D;
    }

    @Override
    public double getGravityVelocity() {
        return this.config.gravity;
    }

    @Override
    protected double motionMult() {
        return this.config.velocity;
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected float getWaterDrag() {
        return 1F;
    }

    public interface IBulletHurtBehaviorNT {
        void behaveEntityHurt(EntityBulletBaseNT bullet, Entity hit);
    }

    public interface IBulletHitBehaviorNT {
        void behaveEntityHit(EntityBulletBaseNT bullet, Entity hit);
    }

    public interface IBulletRicochetBehaviorNT {
        void behaveBlockRicochet(EntityBulletBaseNT bullet, BlockPos pos);
    }

    public interface IBulletImpactBehaviorNT {
        /** pos and sideHit are null when the bullet died in an entity */
        void behaveBlockHit(EntityBulletBaseNT bullet, BlockPos pos, Direction sideHit);
    }

    public interface IBulletUpdateBehaviorNT {
        void behaveUpdate(EntityBulletBaseNT bullet);
    }
}
