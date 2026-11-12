// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine;

import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.space.SpaceToggle;
import com.hbm.space.machine.SpaceLPW2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityMachineLPW2 extends BlockEntityMachineBase {

    public BlockEntityMachineLPW2(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LPW2.get(), pos, state, 0);
    }

    /** With NTM:Space on, the LPW-2 is a station engine ({@link SpaceLPW2.LPW2BE}). */
    public static BlockEntityMachineLPW2 create(BlockPos pos, BlockState state) {
        return SpaceToggle.enabled()
                ? new SpaceLPW2.LPW2BE(pos, state)
                : new BlockEntityMachineLPW2(pos, state);
    }

    /** The clock the engine's animation runs on: the world's, unless Space's engine drives it. */
    public double animationTime(double worldTime, float partialTicks) {
        return worldTime;
    }
}
