// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.client.ClientEffects;
import com.hbm.handler.UnstableFuses;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.Satellite;
import java.util.List;
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

/** Main's lambda rocket: climbs like the Soyuz and puts its one satellite into orbit at y 600. */
public class EntityRocketLambda extends EntityCompat implements StoredItems {

    private double acceleration;
    private ItemStack payload = ItemStack.EMPTY;

    public EntityRocketLambda(EntityType<? extends EntityRocketLambda> type, Level level) {
        super(type, level);
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (visitor.visit(payload) && payload.isEmpty()) payload = ItemStack.EMPTY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        Vec3 motion = getDeltaMovement();
        if (motion.y < 2.0D) {
            acceleration += 0.00025D;
            motion = new Vec3(motion.x, motion.y + acceleration, motion.z);
            setDeltaMovement(motion);
        }

        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (level().isClientSide()) spawnExhaust(getX(), getY() + 1, getZ());

        if (getY() > 600 && !level().isClientSide()) {
            deployPayload();
            discard();
        }
    }

    private void spawnExhaust(double x, double y, double z) {
        ClientEffects.spawnRocketFlame(
                level(),
                x,
                y,
                z,
                1.5F,
                0.0D,
                -1D + random.nextDouble() * 0.25D,
                0.0D,
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
        List<ItemStack> list =
                input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        payload = list.isEmpty() ? ItemStack.EMPTY : list.get(0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putDouble("acceleration", acceleration);
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), List.of(payload));
    }
}
