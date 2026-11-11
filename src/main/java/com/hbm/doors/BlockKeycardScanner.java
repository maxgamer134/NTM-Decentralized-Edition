// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from susy-ntmdoors (Kyron04): https://github.com/Kyron04/susy-ntmdoors

package com.hbm.doors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A card reader that makes the door it touches a security door: no hands, no redstone, only the
 * cards its owner programmed (sneak + click with a card) and the creative keycard.
 */
public class BlockKeycardScanner extends BlockDoorDevice {

    // susy-ntmdoors' scanner: the model spans 0.75 x 1 x 0.30, scaled 0.36 x 0.55 x 0.30
    public BlockKeycardScanner(Properties props) {
        super(props, -0.135, 0.22, -0.5, 0.135, 0.77, -0.41);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityKeycardScanner(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof ItemKeycard)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BlockEntityKeycardScanner scanner)
            scanner.swipe(player, stack);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BlockEntityKeycardScanner scanner)
            scanner.toggleRedstone(player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
