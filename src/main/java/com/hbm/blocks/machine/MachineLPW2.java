// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.machine.BlockEntityMachineLPW2;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.space.SpaceToggle;
import com.hbm.space.machine.SpaceLPW2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Main's decorative engine; with NTM:Space on, {@link SpaceLPW2} makes it a station thruster. */
public class MachineLPW2 extends BlockMultiblockCore
        implements EntityBlock, ICapabilityBlock, ILookOverlay {
    public MachineLPW2(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityMachineLPW2.create(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return SpaceToggle.enabled() ? SpaceLPW2.caps() : MachineCaps.NONE;
    }

    @Override
    public Direction getDirModified(Direction dir) {
        return SpaceToggle.enabled() ? dir.getCounterClockWise() : dir;
    }

    @Override
    protected @Nullable MultiblockLayout runtimeLayout(int declaredCaps) {
        return SpaceToggle.enabled() ? SpaceLPW2.layout(this, declaredCaps) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (!SpaceToggle.enabled()) return null;
        boolean client = level.isClientSide();
        return (l, pos, s, be) -> {
            if (!(be instanceof BlockEntityMachineBase machine)) return;
            if (client) machine.tickClient();
            else machine.tickServer();
        };
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (SpaceToggle.enabled()) SpaceLPW2.buildLookOverlay(this, level, pos, info);
    }
}
