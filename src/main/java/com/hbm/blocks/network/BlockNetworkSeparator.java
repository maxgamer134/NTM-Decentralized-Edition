// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.backport.InteractionResult;
import com.hbm.blockentity.network.BlockEntityNetworkSeparator;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 3x2 power bridge from main: the input end takes power from one network, the output end feeds
 * another, limited to a configured throughput. The ports are the two end cells of the baked layout.
 */
public class BlockNetworkSeparator extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    public static Consumer<BlockEntityNetworkSeparator> OPEN_SCREEN = separator -> {};

    public BlockNetworkSeparator(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityNetworkSeparator(pos, state);
    }

    /** Only the input port takes power; neighbours of the other cells and the core do not. */
    @Override
    public int passiveCoreDomains(int declaredCaps) {
        return PASSIVE_NONE;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(core) instanceof BlockEntityNetworkSeparator separator))
            return InteractionResult.PASS;
        if (level.isClientSide()) OPEN_SCREEN.accept(separator);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityNetworkSeparator separator)) return;
        info.title(getName().getString(), 0xffff00, 0x404000)
                .input(Component.literal(BobMathUtil.getShortNumber(separator.deltaTick[19]) + "HE/t").append(Component.literal(" ->").withStyle(ChatFormatting.RED)))
                .input(Component.literal(BobMathUtil.getShortNumber(separator.deltaSecond) + "HE/s").append(Component.literal(" ->").withStyle(ChatFormatting.RED)))
                .line(
                        "Configured: "
                                + BobMathUtil.getShortNumber(separator.configuredLimit)
                                + "HE/t")
                .line(Component.literal("Priority: ").append(Component.literal(separator.priority.name()).withStyle(ChatFormatting.YELLOW)));
        if (separator.redstone) info.line(Component.literal("Disabled by redstone").withStyle(ChatFormatting.RED));
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.NETWORK_SEPARATOR).powerIn().powerOut();
    }
}
