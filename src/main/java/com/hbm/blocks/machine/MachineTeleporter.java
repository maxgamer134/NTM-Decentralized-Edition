// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineTeleporter;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.BlockCompat;

public class MachineTeleporter extends BlockCompat
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    public MachineTeleporter(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineTeleporter(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineTeleporter tele)) return;

        info.title(getName().getString(), 0xFFFF00, 0x404000);
        GlobalPos target = tele.target;
        if (target == null) {
            info.line(Component.literal("No destination set!").withStyle(ChatFormatting.RED));
            return;
        }
        info.line(Component.literal(String.format(Locale.US, "%,d", tele.power) + " / " + String.format(Locale.US, "%,d", BlockEntityMachineTeleporter.MAX_POWER)).withStyle(tele.power >= BlockEntityMachineTeleporter.CONSUMPTION ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (tele.crossesDimension() && BlockEntityMachineTeleporter.needsFluid()) {
            // NTM:Space: the N-MASS in the tank, shown when the jump leaves this dimension
            info.line(Component.empty()
                    .append(tele.tank.getTankType().getFluidType().getDescription())
                    .append(": " + tele.tank.getFill() + "/" + tele.tank.getMaxFill() + "mB"));
        }
        info.line(
                "Destination: "
                        + target.pos().getX()
                        + " / "
                        + target.pos().getY()
                        + " / "
                        + target.pos().getZ()
                        + " (D: "
                        + target.dimension().location()
                        + ")");
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.TELEBLOCK).powerIn().fluidIn().fe();
    }
}
