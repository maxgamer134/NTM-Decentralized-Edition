// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityLaunchpadLambda;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Main's 15x15 lambda silo. Power, fluids and items go in at the five ports on its front edge,
 * as main's proxy cells there; the core is buried in the middle of the pad.
 */
public class LaunchpadLambda extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {

    public LaunchpadLambda(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityLaunchpadLambda(pos, state);
    }

    @Override
    public int passiveCoreDomains(int declaredCaps) {
        return PASSIVE_NONE;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.LAUNCHPAD_LAMBDA)
                .powerIn()
                .fluidIn()
                .items()
                .itemsAtCells()
                .fe();
    }
}
