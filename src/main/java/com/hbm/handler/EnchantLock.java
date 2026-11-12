// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.config.RuntimeConfig;
import java.util.Arrays;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * With ntm_enchanting off, NTM items take from the table and the anvil only the enchantments
 * ntm_enchanting_efficiency and ntm_enchanting_allowed let through.
 */
public final class EnchantLock {

    private EnchantLock() {}

    /** True for an NTM item while ntm_enchanting is off. */
    public static boolean locked(ItemStack stack) {
        return !stack.isEmpty()
                && !NeoForgeHbmConfig.runtime().ntmEnchanting()
                && "hbm".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    public static boolean allowed(Holder<Enchantment> enchantment) {
        RuntimeConfig config = NeoForgeHbmConfig.runtime();
        if (config.ntmEnchantingEfficiency() && enchantment.is(Enchantments.EFFICIENCY)) return true;
        String id = enchantment.unwrapKey().map(k -> k.location().toString()).orElse("");
        return Arrays.stream(config.ntmEnchantingAllowed().split(","))
                .map(String::trim)
                .anyMatch(id::equals);
    }

    /** Whether anything may be enchanted onto it at all, for the enchanting table. */
    public static boolean anyAllowed() {
        RuntimeConfig config = NeoForgeHbmConfig.runtime();
        return config.ntmEnchantingEfficiency() || !config.ntmEnchantingAllowed().isBlank();
    }

    public static void onAnvil(AnvilUpdateEvent event) {
        if (!locked(event.getLeft())) return;
        ItemEnchantments added = EnchantmentHelper.getEnchantmentsForCrafting(event.getRight());
        for (Holder<Enchantment> enchantment : added.keySet()) {
            if (!allowed(enchantment)) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
