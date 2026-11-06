// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import net.minecraft.ChatFormatting;
import com.hbm.blockentity.machine.BlockEntityCondenserPowered;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.BobMathUtil;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineCondenserPowered extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {
    public MachineCondenserPowered(Properties props) {
        super(props);
    }

    // backport-fix: BF-137 main colours only the arrow; the fluid or item name after it stays white
    private static void line(ILookOverlay.LookInfo info, boolean input, FluidTankNTM tank) {
        info.line(
                (input ? ChatFormatting.GREEN + "-> " : ChatFormatting.RED + "<- ")
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(tank.getFluid())
                        + ": "
                        + String.format(Locale.US, "%,d", tank.getFill())
                        + "/"
                        + String.format(Locale.US, "%,d", tank.getMaxFill())
                        + "mB");
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCondenserPowered(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCondenserPowered be)) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        info.line(
                BobMathUtil.getShortNumber(be.power)
                        + "HE / "
                        + BobMathUtil.getShortNumber(be.getMaxPower())
                        + "HE"); // backport-fix: BF-137 white like main's
        line(info, true, be.input);
        line(info, false, be.output);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().fluidIn().fluidOut().fe();
    }
}
