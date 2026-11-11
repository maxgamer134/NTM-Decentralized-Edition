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

    /**
     * Datapack form of this API: data/<namespace>/hbm_trades/<name>.json, for example
     * <pre>{"profession": "minecraft:farmer",
     *  "swap": {"cap": "cap_nuka", "caps_per_emerald": 2},
     *  "trades": [{"level": 1, "cap": "cap_nuka", "count": 5,
     *              "result": {"id": "minecraft:bread", "count": 3}, "max_uses": 12, "xp": 2}]}</pre>
     * Both "swap" and "trades" are optional. Reloads with /reload, alongside script entries.
     */
    private static final Map<ResourceLocation, List<Added>> JSON_ADDED = new HashMap<>();
    private static final Map<ResourceLocation, Swap> JSON_SWAPS = new HashMap<>();

    @SubscribeEvent
    public static void onReloadListeners(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(new net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener(
                new com.google.gson.Gson(), "hbm_trades") {
            @Override
            protected void apply(
                    Map<ResourceLocation, com.google.gson.JsonElement> entries,
                    net.minecraft.server.packs.resources.ResourceManager manager,
                    net.minecraft.util.profiling.ProfilerFiller profiler) {
                JSON_ADDED.clear();
                JSON_SWAPS.clear();
                for (var entry : entries.entrySet()) {
                    try {
                        var o = entry.getValue().getAsJsonObject();
                        ResourceLocation prof = ResourceLocation.parse(o.get("profession").getAsString());
                        if (o.has("swap")) {
                            var sw = o.getAsJsonObject("swap");
                            JSON_SWAPS.put(prof, new Swap(sw.get("cap").getAsString(),
                                    Math.max(1, sw.get("caps_per_emerald").getAsInt())));
                        }
                        if (o.has("trades")) {
                            for (var t : o.getAsJsonArray("trades")) {
                                var to = t.getAsJsonObject();
                                ItemStack result = ItemStack.CODEC
                                        .parse(com.mojang.serialization.JsonOps.INSTANCE, to.get("result"))
                                        .getOrThrow();
                                JSON_ADDED.computeIfAbsent(prof, k -> new ArrayList<>()).add(new Added(
                                        to.has("level") ? to.get("level").getAsInt() : 1,
                                        to.get("cap").getAsString(),
                                        to.has("count") ? to.get("count").getAsInt() : 1,
                                        result,
                                        to.has("max_uses") ? to.get("max_uses").getAsInt() : 12,
                                        to.has("xp") ? to.get("xp").getAsInt() : 2));
                            }
                        }
                    } catch (RuntimeException e) {
                        com.mojang.logging.LogUtils.getLogger()
                                .error("Bad bottlecap trade file {}: {}", entry.getKey(), e.getMessage());
                    }
                }
            }
        });
    }

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

    /**
     * The swap for a profession: one a script or data pack set for it, else the config's swap
     * for every profession while {@code [trades] bottlecap_trades} is on, else none. Read when
     * an offer is made, so the config applies without a restart.
     */
    private static @org.jetbrains.annotations.Nullable Swap swapFor(@org.jetbrains.annotations.Nullable ResourceLocation profession) {
        if (profession != null) {
            Swap explicit = SWAPS.getOrDefault(profession, JSON_SWAPS.get(profession));
            if (explicit != null) return explicit;
        }
        var config = com.hbm.config.NeoForgeHbmConfig.runtime();
        return config.bottlecapTrades() ? new Swap(config.bottlecapItem(), config.capsPerEmerald()) : null;
    }

    private static ItemCost swap(ItemCost cost, Swap swap, Item cap) {
        if (!cost.itemStack().is(Items.EMERALD)) return cost;
        return new ItemCost(cap, Math.min(cap.getDefaultMaxStackSize(), cost.count() * swap.capsPerEmerald()));
    }

    /** The offer with emeralds turned into caps, in its prices and its payout; itself if it has none. */
    static MerchantOffer convert(MerchantOffer offer, @org.jetbrains.annotations.Nullable Swap swap) {
        if (swap == null) return offer;
        Item cap = capItem(swap.cap());
        if (cap == Items.AIR) return offer;
        boolean costs = offer.getItemCostA().itemStack().is(Items.EMERALD)
                || offer.getItemCostB().map(c -> c.itemStack().is(Items.EMERALD)).orElse(false);
        boolean pays = offer.getResult().is(Items.EMERALD);
        if (!costs && !pays) return offer;
        ItemStack result = offer.getResult();
        if (pays) result = new ItemStack(cap, Math.min(cap.getDefaultMaxStackSize(), result.getCount() * swap.capsPerEmerald()));
        return new MerchantOffer(
                swap(offer.getItemCostA(), swap, cap),
                offer.getItemCostB().map(c -> swap(c, swap, cap)),
                result,
                offer.getUses(),
                offer.getMaxUses(),
                offer.getXp(),
                offer.getPriceMultiplier(),
                offer.getDemand());
    }

    private static List<ItemListing> wrap(List<ItemListing> listings, @org.jetbrains.annotations.Nullable ResourceLocation profession) {
        List<ItemListing> wrapped = new ArrayList<>(listings.size());
        for (ItemListing listing : listings) {
            wrapped.add((trader, random) -> {
                MerchantOffer offer = listing.getOffer(trader, random);
                return offer == null ? null : convert(offer, swapFor(profession));
            });
        }
        return wrapped;
    }

    /** Converts the offers a trader already has: villagers made before the toggle was on. */
    private static void convertExisting(net.minecraft.world.entity.npc.AbstractVillager trader) {
        ResourceLocation profession = trader instanceof net.minecraft.world.entity.npc.Villager villager
                ? BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession())
                : null;
        Swap swap = swapFor(profession);
        if (swap == null) return;
        var offers = trader.getOffers();
        for (int i = 0; i < offers.size(); i++) offers.set(i, convert(offers.get(i), swap));
    }

    @SubscribeEvent
    public static void onInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide()
                && event.getTarget() instanceof net.minecraft.world.entity.npc.AbstractVillager trader)
            convertExisting(trader);
    }

    @SubscribeEvent
    public static void onWandererTrades(net.neoforged.neoforge.event.village.WandererTradesEvent event) {
        List<ItemListing> generic = wrap(event.getGenericTrades(), null);
        event.getGenericTrades().clear();
        event.getGenericTrades().addAll(generic);
        List<ItemListing> rare = wrap(event.getRareTrades(), null);
        event.getRareTrades().clear();
        event.getRareTrades().addAll(rare);
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        VillagerProfession type = event.getType();
        ResourceLocation id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(type);

        // every profession's listings are wrapped, so the swap is decided per offer at runtime
        event.getTrades().int2ObjectEntrySet().forEach(entry -> entry.setValue(wrap(entry.getValue(), id)));

        List<Added> added = new ArrayList<>(ADDED.getOrDefault(id, List.of()));
        added.addAll(JSON_ADDED.getOrDefault(id, List.of()));
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
