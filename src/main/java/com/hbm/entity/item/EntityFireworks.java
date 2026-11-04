// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.entity.ModEntities;
import com.hbm.particle.helper.ParticleCreators;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.joml.Vector3f;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import com.hbm.backport.compat.EntityCompat;

public class EntityFireworks extends EntityCompat {
    // backport-fix: BF-113 the launcher's up axis in the world (straight up unless it sits tilted on a build)
    private static final EntityDataAccessor<Vector3f> AXIS =
            SynchedEntityData.defineId(EntityFireworks.class, EntityDataSerializers.VECTOR3);

    private int color;
    private int character;

    public EntityFireworks(EntityType<? extends EntityFireworks> type, Level level) {
        super(type, level);
    }

    public EntityFireworks(Level level, double x, double y, double z, int color, int character) {
        this(ModEntities.FIREWORK_BALL.get(), level);
        this.moveTo(x, y, z, 0.0F, 0.0F);
        this.color = color;
        this.character = character;
    }

    public int getColor() {
        return color;
    }

    public int getCharacter() {
        return character;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(AXIS, new Vector3f(0F, 1F, 0F));
    }

    public void setLaunchAxis(Vec3 axis) {
        entityData.set(AXIS, new Vector3f((float) axis.x, (float) axis.y, (float) axis.z));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        Vector3f axis = entityData.get(AXIS);
        move(MoverType.SELF, new Vec3(axis.x() * 3.0D, axis.y() * 3.0D, axis.z() * 3.0D));

        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, -0.3D, 0.0D);
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, -0.2D, 0.0D);
            return;
        }

        if (tickCount < 16) return;

        level().playSound(
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        SoundEvents.FIREWORK_ROCKET_BLAST,
                        SoundSource.AMBIENT,
                        20F,
                        1F + random.nextFloat() * 0.2F);

        ParticleCreators.fireworkLetter(level(), getX(), getY(), getZ(), color, (char) character);

        discard();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        character = input.getIntOr("char", 0);
        color = input.getIntOr("color", 0);
        setLaunchAxis(
                new Vec3(
                        input.getDoubleOr("axisX", 0D),
                        input.getDoubleOr("axisY", 1D),
                        input.getDoubleOr("axisZ", 0D)));
        tickCount = input.getIntOr("ticksExisted", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("char", character);
        output.putInt("color", color);
        Vector3f axis = entityData.get(AXIS);
        output.putDouble("axisX", axis.x());
        output.putDouble("axisY", axis.y());
        output.putDouble("axisZ", axis.z());
        output.putInt("ticksExisted", tickCount);
    }
}
