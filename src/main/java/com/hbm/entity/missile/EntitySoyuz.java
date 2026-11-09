// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.client.ClientEffects;
import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.handler.UnstableFuses;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.ISatChip;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.sound.ModSounds;
import com.hbm.util.ChunkUtil;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.hbm.backport.compat.EntityCompat;
import com.hbm.backport.LaunchAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class EntitySoyuz extends EntityCompat implements StoredItems {
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(EntitySoyuz.class, EntityDataSerializers.INT);
    // backport-fix: BF-113 the pad's up axis in the world (straight up unless it sits tilted on a build)
    private static final EntityDataAccessor<Vector3f> AXIS =
            SynchedEntityData.defineId(EntitySoyuz.class, EntityDataSerializers.VECTOR3);

    double acceleration = 0.00D;
    // backport: the heading is kept for the last two ticks, so the model turns smoothly between them
    private int steerTicks;
    private Vec3 nosePrev, noseLast;

    /** The nose, blended between the last two ticks for drawing. */
    public Vec3 flightDirection(float partialTick) {
        Vec3 now = flightDirection();
        if (nosePrev == null || noseLast == null) return now;
        Vec3 d = nosePrev.lerp(noseLast, partialTick);
        return d.lengthSqr() < 1.0e-8D ? now : d.normalize();
    }

    private void trackNose() {
        nosePrev = noseLast == null ? flightDirection() : noseLast;
        noseLast = flightDirection();
    }

    public int mode;
    public int targetX;
    public int targetZ;
    boolean memed = false;
    private final ItemStack[] payload = new ItemStack[18];

    @Override
    public void visitStoredItems(Visitor visitor) {
        for (int i = 0; i < payload.length; i++) {
            if (visitor.visit(payload[i]) && payload[i].isEmpty()) payload[i] = ItemStack.EMPTY;
        }
    }

    public EntitySoyuz(EntityType<? extends EntitySoyuz> type, Level level) {
        super(type, level);
        Arrays.fill(this.payload, ItemStack.EMPTY);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SKIN, 0);
        builder.define(AXIS, new Vector3f(0F, 1F, 0F));
    }

    public void setLaunchAxis(Vec3 axis) {
        entityData.set(AXIS, new Vector3f((float) axis.x, (float) axis.y, (float) axis.z));
    }

    public Vec3 launchAxis() {
        Vector3f v = entityData.get(AXIS);
        return new Vec3(v.x(), v.y(), v.z());
    }

    /** Where the nose points: the flight direction, or the pad axis before it moves. */
    public Vec3 flightDirection() {
        Vec3 axis = launchAxis();
        if (LaunchAxis.upright(axis)) return LaunchAxis.UP;
        Vec3 motion = getDeltaMovement();
        return motion.lengthSqr() > 1.0e-10 ? motion.normalize() : axis;
    }

    @Override
    public void tick() {
        Vec3 motion = getDeltaMovement();
        Vec3 axis = launchAxis();
        boolean upright = LaunchAxis.upright(axis);
        if (upright) {
            if (motion.y < 2.0D) {
                acceleration += LaunchAxis.ROCKET_ACCEL_STEP;
                motion = new Vec3(motion.x, motion.y + acceleration, motion.z);
                setDeltaMovement(motion);
            }
        } else {
            // same speed ramp as main, along the pad axis bending to straight up
            double speed = motion.length();
            if (speed < 2.0D) {
                acceleration += LaunchAxis.ROCKET_ACCEL_STEP;
                speed += acceleration;
            }
            motion = LaunchAxis.steer(axis, LaunchAxis.rocketSteerEased(steerTicks++)).scale(speed);
            setDeltaMovement(motion);
        }
        Vec3 nose = flightDirection();

        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        trackNose();

        if (level() instanceof ServerLevel server) {

            AABB exhaust =
                    upright
                            ? new AABB(
                                    getX() - 5,
                                    getY() - 15,
                                    getZ() - 5,
                                    getX() + 5,
                                    getY(),
                                    getZ() + 5)
                            : AABB.ofSize(position().subtract(nose.scale(7.5D)), 15D, 15D, 15D);
            List<Entity> list = level().getEntities(this, exhaust);

            for (Entity e : list) {
                e.igniteForSeconds(15);

                e.hurt(level().damageSources().source(ModDamageTypes.EXHAUST), 100.0F);

                if (e instanceof Player) {
                    if (!memed) {
                        memed = true;
                        level().playSound(
                                        null,
                                        getX(),
                                        getY(),
                                        getZ(),
                                        ModSounds.ALARM_SOYUZED.get(),
                                        SoundSource.NEUTRAL,
                                        100.0F,
                                        1.0F);
                    }

                    if (e instanceof ServerPlayer serverPlayer) {
                        HbmCriteria.detonation(serverPlayer, DetonationTrigger.Kind.SOYUZ_EXHAUST);
                    }
                }
            }
        }

        if (level().isClientSide()) {
            if (upright) {
                spawnExhaust(getX(), getY(), getZ(), nose);
                spawnExhaust(getX() + 2.75, getY(), getZ(), nose);
                spawnExhaust(getX() - 2.75, getY(), getZ(), nose);
                spawnExhaust(getX(), getY(), getZ() + 2.75, nose);
                spawnExhaust(getX(), getY(), getZ() - 2.75, nose);
            } else {
                Quaternionf q = LaunchAxis.rotation(nose);
                float[][] nozzles = {{0F, 0F}, {2.75F, 0F}, {-2.75F, 0F}, {0F, 2.75F}, {0F, -2.75F}};
                for (float[] n : nozzles) {
                    Vector3f o = q.transform(new Vector3f(n[0], 0F, n[1]));
                    spawnExhaust(getX() + o.x, getY() + o.y, getZ() + o.z, nose);
                }
            }
        }

        if (getY() > 600 && !level().isClientSide()) {
            deployPayload();
        }
    }

    private void spawnExhaust(double x, double y, double z, Vec3 d) {
        double width = random.nextDouble() * 0.25 - 0.5;
        double jx = random.nextGaussian() * width;
        double jz = random.nextGaussian() * width;
        double push = -0.75D + random.nextDouble() * 0.5D;
        ClientEffects.spawnRocketFlame(
                level(),
                x + jx,
                y,
                z + jz,
                1.0F,
                d.x * push,
                d.y * push,
                d.z * push,
                300 + random.nextInt(50));
    }

    private void deployPayload() {

        if (mode == 0) {

            ItemStack load = this.payload[0];

            if (!load.isEmpty()) {

                if (load.is(ModItems.FLAME_PONY.get())) {
                    ExplosionLarge.spawnTracers(level(), getX(), getY(), getZ(), 25);
                    for (Player p : level().players()) {
                        if (p instanceof ServerPlayer serverPlayer)
                            HbmCriteria.orbit(serverPlayer, load);
                    }
                }

                if (load.getItem() instanceof ISatChip
                        && level() instanceof ServerLevel orbitLevel) {
                    Satellite.orbit(
                            orbitLevel, load, ISatChip.getFreqS(load), getX(), getY(), getZ());
                }
            }
        }

        if (mode == 1) {

            EntitySoyuzCapsule capsule =
                    new EntitySoyuzCapsule(ModEntities.SOYUZ_CAPSULE.get(), level());
            capsule.payload = this.payload;
            capsule.soyuz = this.getSkin();
            capsule.setPos(targetX + 0.5, 600, targetZ + 0.5);

            if (level() instanceof ServerLevel server) {
                ChunkUtil.loadForEntity(server, new ChunkPos(targetX >> 4, targetZ >> 4));
            }

            level().addFreshEntity(capsule);
        }

        this.discard();
    }

    public void setSat(ItemStack stack) {
        this.payload[0] = stack == null ? ItemStack.EMPTY : stack;
        UnstableFuses.changed(this);
    }

    public void setPayload(List<ItemStack> payload) {
        for (int i = 0; i < payload.size() && i < this.payload.length; i++) {
            ItemStack stack = payload.get(i);
            this.payload[i] = stack == null ? ItemStack.EMPTY : stack;
        }
        UnstableFuses.changed(this);
    }

    public void setSkin(int i) {
        entityData.set(SKIN, i);
    }

    public int getSkin() {
        return entityData.get(SKIN);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setSkin(input.getIntOr("skin", 0));
        setLaunchAxis(
                new Vec3(
                        input.getDoubleOr("axisX", 0D),
                        input.getDoubleOr("axisY", 1D),
                        input.getDoubleOr("axisZ", 0D)));
        targetX = input.getIntOr("targetX", targetX);
        targetZ = input.getIntOr("targetZ", targetZ);
        mode = input.getIntOr("mode", mode);
        steerTicks = input.getIntOr("steerTicks", 0);

        List<ItemStack> list =
                input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < list.size() && i < payload.length; i++) {
            payload[i] = list.get(i);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("skin", getSkin());
        Vec3 axis = launchAxis();
        output.putDouble("axisX", axis.x);
        output.putDouble("axisY", axis.y);
        output.putDouble("axisZ", axis.z);
        output.putInt("targetX", targetX);
        output.putInt("targetZ", targetZ);
        output.putInt("mode", mode);
        output.putInt("steerTicks", steerTicks);

        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), List.of(payload));
    }
}
