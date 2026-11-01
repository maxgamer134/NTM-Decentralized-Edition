// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

// Almost Unified hides our copies of common items (sulfur, ingots, dusts), and EMI/TMRV then drop every
// recipe showing one. Each slot also lists the other members of the item's c:<kind>/<material> tags,
// so the recipe stays visible through whichever copy is left.
final class UnifiedStacks {

    private static final boolean UNIFYING = ModList.get().isLoaded("almostunified");

    private UnifiedStacks() {}

    static List<ItemStack> expand(ItemStack stack) {
        return expand(List.of(stack));
    }

    static List<ItemStack> expand(List<ItemStack> stacks) {
        if (!UNIFYING) return stacks;
        List<ItemStack> out = new ArrayList<>(stacks);
        List<Item> seen = new ArrayList<>();
        for (ItemStack stack : stacks) seen.add(stack.getItem());
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            stack.getTags()
                    .filter(tag -> tag.location().getNamespace().equals("c"))
                    .filter(tag -> tag.location().getPath().indexOf('/') > 0)
                    .forEach(
                            tag -> {
                                for (Holder<Item> other : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                                    if (seen.contains(other.value())) continue;
                                    seen.add(other.value());
                                    out.add(stack.transmuteCopy(other.value(), stack.getCount()));
                                }
                            });
        }
        return out;
    }
}
