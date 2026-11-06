// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineChemicalFactory;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineChemicalFactory extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {
    public MachineChemicalFactory(Properties props) {
        super(props);
    }


    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineChemicalFactory(pos, state);
    }

    // backport-fix: BF-137 like main, the readout only shows on a port: the coolant port names
    // its water and spent steam, an item port its recipe field; only the arrow is coloured
    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineChemicalFactory be)) return;
        int port = be.lookPort(info.getHitPos() != null ? info.getHitPos() : pos);
        if (port < 0) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        if (port == 0) {
            info.line(
                    ChatFormatting.GREEN
                            + "-> "
                            + ChatFormatting.RESET
                            + NTMFluidProperties.clientName(be.water().getFluid()));
            info.line(
                    ChatFormatting.RED
                            + "<- "
                            + ChatFormatting.RESET
                            + NTMFluidProperties.clientName(be.lps().getFluid()));
        } else {
            info.line(
                    ChatFormatting.YELLOW
                            + "-> "
                            + ChatFormatting.RESET
                            + "Recipe field ["
                            + port
                            + "]");
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().fluidIn().fluidOut().itemsAtCells().fe();
    }
}
