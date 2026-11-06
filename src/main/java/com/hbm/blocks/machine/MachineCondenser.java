// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import net.minecraft.ChatFormatting;
import com.hbm.api.environment.Environment;
import com.hbm.blockentity.machine.BlockEntityCondenser;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.BobMathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.BlockCompat;

public class MachineCondenser extends BlockCompat
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    public MachineCondenser(Properties props) {
        super(props);
    }

    // backport-fix: BF-137 main colours only the arrow; the fluid or item name after it stays white
    private static void line(ILookOverlay.LookInfo info, boolean input, FluidTankNTM tank) {
        info.line(
                Component.empty()
                        .append(
                                Component.literal(input ? "-> " : "<- ")
                                        .withStyle(input ? ChatFormatting.GREEN : ChatFormatting.RED))
                        .append(NTMFluidProperties.getDisplayName(tank.getFluid()))
                        .append(": ")
                        .append(
                                Component.translatable(
                                        "unit.millibuckets",
                                        tank.getFill() + "/" + tank.getMaxFill())));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCondenser(pos, state);
    }

    static void vacuumWarning(Level level, ILookOverlay.LookInfo info) {
        if (!Environment.vacuum(level)) return;
        info.line(
                Component.literal("! ! ! ")
                        .append(Component.translatable("atmosphere.noVacuum"))
                        .append(" ! ! !"),
                BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCondenser be)) return;
        info.title(getName(), 0xFFFF00, 0x404000);
        vacuumWarning(level, info);
        line(info, true, be.input);
        line(info, false, be.output);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.CONDENSER).fluidIn().fluidOut();
    }
}
