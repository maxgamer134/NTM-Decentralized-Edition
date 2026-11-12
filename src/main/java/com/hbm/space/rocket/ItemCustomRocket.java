// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.rocket;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import com.hbm.backport.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/** A finished rocket out of the assembly building, waiting for a launch pad. */
public class ItemCustomRocket extends com.hbm.backport.compat.ItemCompat {

    public ItemCustomRocket(Properties props) {
        super(props);
    }

    public static ItemStack build(RocketStruct rocket) {
        ItemStack stack = new ItemStack(SpaceRockets.ROCKET.get());
        CompoundTag saved = rocket.save();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.put("rocket", saved));
        return stack;
    }

    public static ItemStack build(RocketStruct rocket, boolean hasFuel) {
        ItemStack stack = build(rocket);
        setFuel(stack, hasFuel);
        return stack;
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static @Nullable RocketStruct get(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemCustomRocket)) return null;
        CompoundTag tag = tag(stack);
        return tag.contains("rocket") ? RocketStruct.load(tag.getCompound("rocket")) : null;
    }

    public static boolean hasFuel(ItemStack stack) {
        return tag(stack).getBoolean("hasFuel");
    }

    public static void setFuel(ItemStack stack, boolean hasFuel) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean("hasFuel", hasFuel));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> adder, TooltipFlag flag) {
        RocketStruct rocket = get(stack);
        if (rocket == null || rocket.capsule == null) return;
        adder.accept(Component.literal("Payload: ").withStyle(ChatFormatting.BOLD)
                .append(rocket.capsule.getDescription().copy().withStyle(ChatFormatting.GRAY)));
        adder.accept(Component.literal("Stages: ").withStyle(ChatFormatting.BOLD)
                .append(Component.literal(String.valueOf(rocket.stages.size())).withStyle(ChatFormatting.GRAY)));
        if (hasFuel(stack)) adder.accept(Component.literal("Is fully fueled").withStyle(ChatFormatting.GRAY));
    }
}
