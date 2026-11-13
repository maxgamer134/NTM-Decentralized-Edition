// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.items.armor;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The V1 chestplate mod: half again as fast, three dashes, and blood is fuel. Hits heal the wearer
 * by half the damage dealt and kills by a quarter of the victim's health ({@link V1}).
 */
public class ItemModV1 extends ItemModCloud {

    public ItemModV1(Properties properties) {
        super(properties);
    }

    @Override
    protected double speed() {
        return 0.5D;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(Component.translatable("desc.item.armorMod.v1").withStyle(ChatFormatting.RED));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(Component.translatable("desc.item.armorMod.v1.installed", stack.getHoverName())
                .withStyle(ChatFormatting.RED));
    }
}
