// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.RadiationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CoalGasHandler {

    private CoalGasHandler() {}

    public static void onBlockBroken(Level level, BlockPos pos, BlockState broken) {
        if (level.isClientSide() || RadiationConfig.coalDustDisabled()) return;
        if (!broken.is(net.neoforged.neoforge.common.Tags.Blocks.ORES_COAL)
                && !broken.is(Blocks.COAL_BLOCK)
                && !broken.is(ModBlocks.ORE_LIGNITE.get())) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            if (level.getRandom().nextInt(2) == 0 && level.getBlockState(at).isAir()) {
                level.setBlockAndUpdate(at, ModBlocks.GAS_COAL.get().defaultBlockState());
            }
        }
    }
}
