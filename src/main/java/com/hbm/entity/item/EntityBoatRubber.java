// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import net.minecraft.core.particles.ParticleTypes;
import com.hbm.items.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.hbm.backport.compat.BoatCompat;

public class EntityBoatRubber extends BoatCompat {

    private double speedMultiplier = 0.07D;

    public EntityBoatRubber(EntityType<? extends EntityBoatRubber> type, Level level) {
        super(type, level);
    }

    // backport: 26.x passes the drop item to the Boat constructor; 1.21.1 asks getDropItem()
    @Override
    public net.minecraft.world.item.Item getDropItem() {
        return ModItems.BOAT_RUBBER.get();
    }

    @Override
    public void controlBoat() {
        if (!isVehicle()) return;

        int forward = inputUp == inputDown ? 0 : inputUp ? 1 : -1;
        int strafe = inputLeft == inputRight ? 0 : inputLeft ? 1 : -1;
        Vec3 motion = getDeltaMovement();
        double previousSpeed = motion.horizontalDistance();

        if (forward != 0 || strafe != 0) {
            float heading = (float) (-(getYRot() + 90F) * Math.PI / 180D);
            double push = speedMultiplier * forward * 0.05D;
            motion = motion.add(Mth.sin(heading) * push, 0D, Mth.cos(heading) * push);

            float rotated = -strafe * 3F;
            setYRot(getYRot() + rotated);
            float turn = (float) (-rotated * Math.PI / 180D);
            float cos = Mth.cos(turn);
            float sin = Mth.sin(turn);
            motion =
                    new Vec3(
                            motion.x * cos + motion.z * sin,
                            motion.y,
                            motion.z * cos - motion.x * sin);
        }

        double speed = motion.horizontalDistance();
        if (speed > 0.5D) {
            motion = new Vec3(motion.x * (0.5D / speed), motion.y, motion.z * (0.5D / speed));
            speed = 0.5D;
        }

        if (speed > previousSpeed && speedMultiplier < 0.5D) {
            speedMultiplier = Math.min(0.5D, speedMultiplier + (0.5D - speedMultiplier) / 50D);
        } else {
            speedMultiplier = Math.max(0.07D, speedMultiplier - (speedMultiplier - 0.07D) / 35D);
        }

        setDeltaMovement(motion);
        setPaddleState(inputRight && !inputLeft || inputUp, inputLeft && !inputRight || inputUp);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (passenger instanceof Player player)
            player.setYBodyRot(Mth.wrapDegrees(getYRot() + 90F));
    }

    // main's rubber boat kicks up splashes along both sides once it moves faster than 0.2625/tick
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) return;
        double moX = xo - getX();
        double moZ = zo - getZ();
        double speed = Math.sqrt(moX * moX + moZ * moZ);
        if (speed <= 0.2625D) return;
        double cosYaw = Math.cos(getYRot() * Math.PI / 180.0D);
        double sinYaw = Math.sin(getYRot() * Math.PI / 180.0D);
        for (double j = 0; j < 1.0D + speed * 60.0D; ++j) {
            double offset = random.nextFloat() * 2.0F - 1.0F;
            double side = (random.nextInt(2) * 2 - 1) * 0.7D;
            double magX, magZ;
            if (random.nextBoolean()) {
                magX = getX() - cosYaw * offset * 0.8D + sinYaw * side;
                magZ = getZ() - sinYaw * offset * 0.8D - cosYaw * side;
            } else {
                magX = getX() + cosYaw + sinYaw * offset * 0.7D;
                magZ = getZ() + sinYaw - cosYaw * offset * 0.7D;
            }
            level().addParticle(ParticleTypes.SPLASH, magX, getY() - 0.125D, magZ, moX, 0.1D, moZ);
        }
    }
}
