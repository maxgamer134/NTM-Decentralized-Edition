// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.client.ClientEffects;
import com.hbm.handler.UnstableFuses;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.Satellite;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import com.hbm.backport.compat.EntityCompat;
import com.hbm.backport.LaunchAxis;
import org.joml.Vector3f;

/** Main's lambda rocket: climbs like the Soyuz and puts its one satellite into orbit at y 600. */
public class EntityRocketLambda extends EntityCompat implements StoredItems {

    // backport-fix: BF-113 the pad's up axis in the world (straight up unless it sits tilted on a build)
    private static final EntityDataAccessor<Vector3f> AXIS =
            SynchedEntityData.defineId(EntityRocketLambda.class, EntityDataSerializers.VECTOR3);

    private double acceleration;
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

    private ItemStack payload = ItemStack.EMPTY;

    public EntityRocketLambda(EntityType<? extends EntityRocketLambda> type, Level level) {
        super(type, level);
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (visitor.visit(payload) && payload.isEmpty()) payload = ItemStack.EMPTY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
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
        if (LaunchAxis.upright(axis)) {
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

        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        trackNose();

        if (level().isClientSide()) {
            Vec3 d = flightDirection();
            spawnExhaust(getX() + d.x, getY() + d.y, getZ() + d.z, d);
        }

        if (getY() > 600 && !level().isClientSide()) {
            deployPayload();
            discard();
        }
    }

    private void spawnExhaust(double x, double y, double z, Vec3 d) {
        double push = -1D + random.nextDouble() * 0.25D;
        ClientEffects.spawnRocketFlame(
                level(),
                x,
                y,
                z,
                1.5F,
                d.x * push,
                d.y * push,
                d.z * push,
                300 + random.nextInt(50));

        // main's "missileContrail" is a rocket flame thrown sideways, not a smoke trail
        if (tickCount % 10 == 0) {
            double angle = Math.toRadians(45 * random.nextInt(8));
            double vx = Math.cos(angle);
            double vz = -Math.sin(angle);
            double j = 0.5;
            ClientEffects.spawnRocketFlame(
                    level(),
                    getX() - vx * j,
                    getY(),
                    getZ() - vz * j,
                    1F,
                    vx,
                    getDeltaMovement().y - 0.5D,
                    vz,
                    60 + random.nextInt(20));
        }
    }

    private void deployPayload() {
        if (!payload.isEmpty()
                && payload.getItem() instanceof ISatChip
                && level() instanceof ServerLevel orbitLevel) {
            Satellite.orbit(
                    orbitLevel, payload, ISatChip.getFreqS(payload), getX(), getY(), getZ());
        }
    }

    public void setSat(ItemStack stack) {
        this.payload = stack == null ? ItemStack.EMPTY : stack;
        UnstableFuses.changed(this);
    }

    public void setPayload(List<ItemStack> payload) {
        setSat(payload.isEmpty() ? ItemStack.EMPTY : payload.get(0));
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
        acceleration = input.getDoubleOr("acceleration", 0D);
        steerTicks = input.getIntOr("steerTicks", 0);
        setLaunchAxis(
                new Vec3(
                        input.getDoubleOr("axisX", 0D),
                        input.getDoubleOr("axisY", 1D),
                        input.getDoubleOr("axisZ", 0D)));
        List<ItemStack> list =
                input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        payload = list.isEmpty() ? ItemStack.EMPTY : list.get(0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putDouble("acceleration", acceleration);
        output.putInt("steerTicks", steerTicks);
        Vec3 axis = launchAxis();
        output.putDouble("axisX", axis.x);
        output.putDouble("axisY", axis.y);
        output.putDouble("axisZ", axis.z);
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), List.of(payload));
    }
}
