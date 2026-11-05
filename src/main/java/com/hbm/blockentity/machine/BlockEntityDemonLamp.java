// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.hbm.backport.BlockEntityCompat;

public class BlockEntityDemonLamp extends BlockEntityCompat {

    public static final float RADS = 100000F;
    public static final double RANGE = 25D;
    public static final double BURN_RANGE = 2D;
    public static final float BURN_DAMAGE = 100F;

    public BlockEntityDemonLamp(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEMON_LAMP.get(), pos, state);
    }

    public static void tickServer(BlockEntityDemonLamp lamp) {
        radiate((ServerLevel) lamp.level, lamp.worldPosition);
    }

    public static void radiate(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5D, cy = pos.getY() + 0.5D, cz = pos.getZ() + 0.5D;
        // backport-fix: BF-116 like BF-028's ContaminationUtil.radiate: on a Sable build the lamp's
        // plot position is millions of blocks from the entities Sable's lookup returns (they are
        // around where the build is drawn), and the shielding ray walked (and loaded) that whole
        // way, hanging the server. Measure from the world position, and only read loaded chunks.
        Vec3 world = com.hbm.backport.SubLevelSpace.toWorld(level, cx, cy, cz);
        cx = world.x;
        cy = world.y;
        cz = world.z;
        BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
        for (LivingEntity e :
                level.getEntitiesOfClass(
                        LivingEntity.class, new AABB(cx, cy, cz, cx, cy, cz).inflate(RANGE))) {
            Vec3 vec = new Vec3(e.getX() - cx, e.getEyeY() - cy, e.getZ() - cz);
            double len = vec.length();
            if (len > RANGE * 1.7321D) continue; // the lookup box's corner: never true off a build
            vec = vec.normalize();

            float res = 0F;
            for (int i = 1; i < len; i++) {
                cell.set(
                        Mth.floor(cx + vec.x * i),
                        Mth.floor(cy + vec.y * i),
                        Mth.floor(cz + vec.z * i));
                if (!level.isLoaded(cell)) continue;
                res += level.getBlockState(cell).getBlock().getExplosionResistance();
            }
            if (res < 1F) res = 1F;

            float eRads = RADS;
            eRads /= res;
            eRads /= (float) (len * len);
            ContaminationUtil.contaminate(
                    e, HazardType.RADIATION, ContaminationType.CREATIVE, eRads);

            if (len < BURN_RANGE) e.hurt(level.damageSources().inFire(), BURN_DAMAGE);
        }
    }
}
