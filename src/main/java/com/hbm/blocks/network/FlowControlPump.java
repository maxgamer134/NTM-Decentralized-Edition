// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blockentity.network.BlockEntityFlowControlPump;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 3x1 pump from main that replaced the fluid pump: fluid enters at one end cell and leaves at the
 * other, at most the configured buffer per tick.
 */
public class FlowControlPump extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    public static Consumer<BlockEntityFlowControlPump> OPEN_SCREEN = pump -> {};

    public FlowControlPump(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFlowControlPump(pos, state);
    }

    /** Only the input port takes fluid; neighbours of the core do not. */
    @Override
    public int passiveCoreDomains(int declaredCaps) {
        return PASSIVE_NONE;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack stack,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof BlockEntityFlowControlPump pump) {
            FluidIdentifierData data =
                    stack.getOrDefault(
                            ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
            Fluid type = data.primary();
            if (type != null && type != Fluids.EMPTY) {
                pump.tank.setTankTypeByIdentifier(type);
                pump.setChanged();
                player.sendSystemMessage(
                        Component.translatable(
                                "hbm.message.pumpSet", NTMFluidProperties.getDisplayName(type)));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(core) instanceof BlockEntityFlowControlPump pump))
            return InteractionResult.PASS;
        if (level.isClientSide()) OPEN_SCREEN.accept(pump);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityFlowControlPump pump)) return;
        Fluid type = pump.tank.getTankType();
        String typeName =
                type == null
                        ? Component.translatable("hbmfluid.none").getString()
                        : NTMFluidProperties.clientName(type);
        info.title(getName().getString(), 0xffff00, 0x404000)
                .input(Component.literal(typeName + " (" + pump.tank.getPressure() + " PU): " + String.format(Locale.US, "%,d", pump.rate) + "mB/t").append(Component.literal(" ->").withStyle(ChatFormatting.RED)))
                .line(Component.literal("Priority: ").append(Component.literal(pump.priority.name()).withStyle(ChatFormatting.YELLOW)));
        if (pump.redstone) info.line(Component.literal("Disabled by redstone").withStyle(ChatFormatting.RED));
        if (pump.tank.getFill() > 0)
            info.line(String.format(Locale.US, "%,d", pump.tank.getFill()) + "mB buffered");
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FLOW_CONTROL_PUMP).fluidIn().fluidOut();
    }
}
