// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.hbm.backport.BlockEntityCompat;

public final class BlockEntityStatueElbF extends BlockEntityCompat {

    private static final double SEARCH_RADIUS = 9.0D;
    private static final double FEET_RADIUS_SQ = 16.0D;
    private static final double EYE_RADIUS_SQ = 64.0D;

    public BlockEntityStatueElbF(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATUE_ELB_F.get(), pos, state);
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server)) return;
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        // backport-fix: BF-117 measure from where the statue is in the world (also on a build)
        net.minecraft.world.phys.Vec3 o = com.hbm.backport.SubLevelSpace.toWorld(com.hbm.backport.SubLevelSpace.frame(server, worldPosition), new net.minecraft.world.phys.Vec3(x, y, z));
        AABB search =
                new AABB(
                        o.x - SEARCH_RADIUS,
                        o.y - SEARCH_RADIUS,
                        o.z - SEARCH_RADIUS,
                        o.x + SEARCH_RADIUS,
                        o.y + SEARCH_RADIUS,
                        o.z + SEARCH_RADIUS);
        for (Player player : server.getEntitiesOfClass(Player.class, search)) {
            if (player.distanceToSqr(o.x, o.y, o.z) > FEET_RADIUS_SQ) continue;
            double dx = player.getX() - o.x;
            double dy = player.getEyeY() - o.y;
            double dz = player.getZ() - o.z;
            if (dx * dx + dy * dy + dz * dz >= EYE_RADIUS_SQ) continue;
            player.addEffect(new MobEffectInstance(MobEffects.HEAL, 5, 99));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 5, 99));
        }
    }
}
