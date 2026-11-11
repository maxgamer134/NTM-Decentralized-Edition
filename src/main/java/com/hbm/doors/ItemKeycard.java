// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from susy-ntmdoors (Kyron04): https://github.com/Kyron04/susy-ntmdoors

package com.hbm.doors;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/**
 * A keycard. Each card gets its own id the first time a scanner reads it, so a scanner trusts that
 * card and not others of the same kind. The creative keycard opens every scanner.
 */
public class ItemKeycard extends Item {

    private static final String KEY = "hbm_keycard";
    private final boolean creative;

    public ItemKeycard(Properties props, boolean creative) {
        super(props);
        this.creative = creative;
    }

    public static boolean isCreative(ItemStack stack) {
        return stack.getItem() instanceof ItemKeycard card && card.creative;
    }

    /** The card's id, given to it now if it has none. */
    public static String id(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String id = tag.getString(KEY);
        if (!id.isEmpty()) return id;
        String fresh = UUID.randomUUID().toString();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putString(KEY, fresh));
        return fresh;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (creative) tooltip.add(Component.translatable("hbm.door_access.card_creative").withStyle(ChatFormatting.RED));
        else if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(KEY))
            tooltip.add(Component.translatable("hbm.door_access.card_programmed").withStyle(ChatFormatting.GRAY));
        else tooltip.add(Component.translatable("hbm.door_access.card_blank").withStyle(ChatFormatting.GRAY));
    }
}
