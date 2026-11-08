// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.kubejs;

import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * Villager trades paid in bottle caps. Exposed to KubeJS as {@code HbmTrades}, so a startup script
 * can write:
 *
 * <pre>
 * HbmTrades.add('minecraft:armorer', 1, 'cap_nuka', 8, 'minecraft:iron_ingot', 4, 12, 5)
 * HbmTrades.swapEmeralds('minecraft:farmer', 'cap_nuka', 2)
 * </pre>
 *
 * Cap ids are item paths in the hbm namespace (cap_nuka, cap_korl, cap_fritz, ...). Trades are
 * applied when the game collects villager trades, so register them in a startup script.
 */
@EventBusSubscriber(modid = "hbm")
public final class BottlecapTrades {

    private record Added(
            int level, String cap, int capCount, ItemStack result, int maxUses, int xp) {}

    private record Swap(String cap, int capsPerEmerald) {}

    private static final Map<ResourceLocation, List<Added>> ADDED = new HashMap<>();
    private static final Map<ResourceLocation, Swap> SWAPS = new HashMap<>();

    private BottlecapTrades() {}

    /** Add one trade: {@code capCount} caps of {@code cap} for {@code result}. */
    public static void add(
            String profession,
            int level,
            String cap,
            int capCount,
            ItemStack result,
            int maxUses,
            int xp) {
        ADDED.computeIfAbsent(ResourceLocation.parse(profession), k -> new ArrayList<>())
                .add(new Added(level, cap, capCount, result.copy(), maxUses, xp));
    }

    /** Make every existing emerald price of a profession cost caps instead. */
    public static void swapEmeralds(String profession, String cap, int capsPerEmerald) {
        SWAPS.put(ResourceLocation.parse(profession), new Swap(cap, Math.max(1, capsPerEmerald)));
    }

    private static Item capItem(String cap) {
        ResourceLocation id =
                cap.contains(":") ? ResourceLocation.parse(cap) : Library.id(cap);
        return BuiltInRegistries.ITEM.get(id);
    }

    private static ItemCost swap(ItemCost cost, Swap swap) {
        if (!cost.itemStack().is(Items.EMERALD)) return cost;
        Item cap = capItem(swap.cap());
        if (cap == Items.AIR) return cost;
        return new ItemCost(cap, Math.min(64, cost.count() * swap.capsPerEmerald()));
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        VillagerProfession type = event.getType();
        ResourceLocation id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(type);

        Swap swap = SWAPS.get(id);
        if (swap != null) {
            event.getTrades()
                    .int2ObjectEntrySet()
                    .forEach(
                            entry -> {
                                List<ItemListing> wrapped = new ArrayList<>();
                                for (ItemListing listing : entry.getValue()) {
                                    wrapped.add(
                                            (trader, random) -> {
                                                MerchantOffer offer =
                                                        listing.getOffer(trader, random);
                                                if (offer == null) return null;
                                                return new MerchantOffer(
                                                        swap(offer.getItemCostA(), swap),
                                                        offer.getItemCostB().map(c -> swap(c, swap)),
                                                        offer.getResult(),
                                                        offer.getUses(),
                                                        offer.getMaxUses(),
                                                        offer.getXp(),
                                                        offer.getPriceMultiplier(),
                                                        offer.getDemand());
                                            });
                                }
                                entry.setValue(wrapped);
                            });
        }

        List<Added> added = ADDED.get(id);
        if (added == null) return;
        for (Added a : added) {
            Item cap = capItem(a.cap());
            if (cap == Items.AIR) continue;
            event.getTrades()
                    .computeIfAbsent(a.level(), k -> new ArrayList<>())
                    .add(
                            (trader, random) ->
                                    new MerchantOffer(
                                            new ItemCost(cap, a.capCount()),
                                            a.result().copy(),
                                            a.maxUses(),
                                            a.xp(),
                                            0.05F));
        }
    }
}
