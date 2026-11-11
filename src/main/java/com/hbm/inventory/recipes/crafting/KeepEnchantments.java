// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * NTM's crafts keep what the player put on an item. Upgrading a tool, weapon or armor piece
 * into another one (a starmetal pickaxe into a bismuth one, say) carries over its NBT-like data,
 * such as its name, tool ability presets and NTM settings, and every craft passes enchantments
 * on: each at the highest level an ingredient has it, when the result can take it and it fits
 * with what the result already has.
 */
public final class KeepEnchantments {

    private KeepEnchantments() {}

    public static ItemStack apply(ItemStack result, CraftingInput input) {
        if (result.isEmpty()) return result;
        carryData(result, input);
        ItemEnchantments.Mutable merged =
                new ItemEnchantments.Mutable(result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        boolean changed = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemEnchantments enchantments = input.getItem(slot).getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var entry : enchantments.entrySet()) {
                Holder<Enchantment> enchantment = entry.getKey();
                int level = entry.getIntValue();
                if (level <= merged.getLevel(enchantment)) continue;
                if (!result.supportsEnchantment(enchantment)) continue;
                if (merged.getLevel(enchantment) == 0
                        && !EnchantmentHelper.isEnchantmentCompatible(merged.keySet(), enchantment)) continue;
                merged.set(enchantment, level);
                changed = true;
            }
        }
        if (changed) result.set(DataComponents.ENCHANTMENTS, merged.toImmutable());
        return result;
    }

    private static boolean durable(ItemStack stack) {
        return stack.getItem().components().has(DataComponents.MAX_DAMAGE);
    }

    /** The first durable ingredient's NBT-like data, onto a durable result that does not set it itself. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void carryData(ItemStack result, CraftingInput input) {
        if (result.getCount() != 1 || !durable(result)) return;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack source = input.getItem(slot);
            if (source.isEmpty() || !durable(source)) continue;
            var own = result.getComponentsPatch();
            for (var change : source.getComponentsPatch().entrySet()) {
                DataComponentType type = change.getKey();
                if (change.getValue().isEmpty() || type == DataComponents.ENCHANTMENTS) continue;
                if (!RecipeInputState.ignoredByRecipes(type) || own.get(type) != null) continue;
                result.set(type, change.getValue().get());
            }
            return;
        }
    }
}
