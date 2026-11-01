// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from HBM's Nuclear Tech Mod (ItemRTTYDesignator).

package com.hbm.items.tool;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.backport.InteractionResult;
import com.hbm.backport.TooltipDisplay;
import com.hbm.backport.compat.ItemCompat;
import com.hbm.blockentity.network.RTTYSystem;
import com.hbm.items.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Redstone-over-Radio designator: sneak-use sets the channel (same screen as the pager), use
 * broadcasts the coordinates of the block face looked at (up to 300 blocks) as "x:y:z".
 */
public class ItemRTTYDesignator extends ItemCompat {

    public ItemRTTYDesignator(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) ItemRTTYPager.OPEN_SCREEN.accept(player);
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!stack.has(ModDataComponents.RTTY_PAGER_CHANNEL.get())) return InteractionResult.SUCCESS;

        HitResult hit = player.pick(300.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK)
            return InteractionResult.SUCCESS;
        BlockPos pos = bhr.getBlockPos().relative(bhr.getDirection());
        String coords =
                pos.getX()
                        + IRORInteractive.PARAM_SEPARATOR
                        + pos.getY()
                        + IRORInteractive.PARAM_SEPARATOR
                        + pos.getZ();
        RTTYSystem.broadcast(level, ItemRTTYPager.channel(stack), coords);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        String channel = ItemRTTYPager.channel(stack);
        if (!stack.has(ModDataComponents.RTTY_PAGER_CHANNEL.get()) || channel.isEmpty()) {
            adder.accept(
                    Component.translatable("desc.item.rttyPager.noChannelSet")
                            .withStyle(ChatFormatting.RED));
        } else {
            adder.accept(
                    Component.translatable("desc.item.rttyPager.channel", channel)
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
