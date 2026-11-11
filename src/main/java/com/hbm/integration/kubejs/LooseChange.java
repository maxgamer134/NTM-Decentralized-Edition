// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.kubejs;

import com.hbm.config.NeoForgeHbmConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

/**
 * Loose change for trades ({@code [trades] loose_change}): a price in one configured currency can
 * be paid in any other, and the trader hands back the difference in the largest coins that make
 * it exactly. With a quantum cap worth four nuka caps, a two-cap trade takes one quantum cap and
 * returns two nuka caps. A payment that leaves change no set of coins can make is refused.
 */
public final class LooseChange {

    private static final ThreadLocal<List<ItemStack>> PENDING = ThreadLocal.withInitial(ArrayList::new);
    private static String parsedFrom;
    private static Map<Item, Integer> values = Map.of();

    private LooseChange() {}

    /** The configured currencies and their values, re-read whenever the config text changes. */
    private static synchronized Map<Item, Integer> currencies() {
        String text = NeoForgeHbmConfig.runtime().tradeCurrencies();
        if (text.equals(parsedFrom)) return values;
        Map<Item, Integer> map = new LinkedHashMap<>();
        for (String part : text.split(",")) {
            String[] kv = part.trim().split("=");
            if (kv.length != 2) continue;
            ResourceLocation id = ResourceLocation.tryParse(kv[0].trim());
            if (id == null) continue;
            Item item = BuiltInRegistries.ITEM.get(id);
            try {
                int value = Integer.parseInt(kv[1].trim());
                if (item != Items.AIR && value > 0) map.put(item, value);
            } catch (NumberFormatException ignored) {
            }
        }
        parsedFrom = text;
        values = map;
        return map;
    }

    /** How one payment slot settles one price. */
    private record Pay(int take, int change) {
        static final Pay NONE = new Pay(-1, 0);
    }

    private static Pay pay(ItemStack slot, @Nullable ItemCost cost, ItemStack price) {
        if (cost == null) return slot.isEmpty() ? new Pay(0, 0) : Pay.NONE;
        if (cost.test(slot) && slot.getCount() >= price.getCount()) return new Pay(price.getCount(), 0);
        if (slot.isEmpty() || !cost.components().equals(net.minecraft.core.component.DataComponentPredicate.EMPTY)) return Pay.NONE;
        Map<Item, Integer> money = currencies();
        Integer priceValue = money.get(price.getItem());
        Integer coinValue = money.get(slot.getItem());
        if (priceValue == null || coinValue == null) return Pay.NONE;
        int owed = priceValue * price.getCount();
        int take = (owed + coinValue - 1) / coinValue;
        if (take > slot.getCount()) return Pay.NONE;
        int change = take * coinValue - owed;
        return makeChange(change, money) == null ? Pay.NONE : new Pay(take, change);
    }

    /** The change in the largest coins first, or null when no coins make it exactly. */
    private static @Nullable List<ItemStack> makeChange(int amount, Map<Item, Integer> money) {
        List<ItemStack> coins = new ArrayList<>();
        List<Map.Entry<Item, Integer>> sorted = new ArrayList<>(money.entrySet());
        sorted.sort(Map.Entry.<Item, Integer>comparingByValue(Comparator.reverseOrder()));
        for (var coin : sorted) {
            int n = amount / coin.getValue();
            amount -= n * coin.getValue();
            int max = coin.getKey().getDefaultMaxStackSize();
            for (; n > 0; n -= max) coins.add(new ItemStack(coin.getKey(), Math.min(n, max)));
        }
        return amount == 0 ? coins : null;
    }

    private static boolean enabled() {
        return NeoForgeHbmConfig.runtime().looseChange();
    }

    /** Whether the slots pay the offer, counting change; null when vanilla's check should decide. */
    public static @Nullable Boolean satisfiedBy(MerchantOffer offer, ItemStack a, ItemStack b) {
        if (!enabled()) return null;
        Pay payA = pay(a, offer.getItemCostA(), offer.getCostA());
        Pay payB = pay(b, offer.getItemCostB().orElse(null), offer.getCostB());
        if (payA.take() < 0 || payB.take() < 0 || payA.change() + payB.change() == 0) return null;
        return true;
    }

    /** Takes the payment and holds the change for the result slot; null when vanilla should take it. */
    public static @Nullable Boolean take(MerchantOffer offer, ItemStack a, ItemStack b) {
        if (satisfiedBy(offer, a, b) == null) return null;
        Pay payA = pay(a, offer.getItemCostA(), offer.getCostA());
        Pay payB = pay(b, offer.getItemCostB().orElse(null), offer.getCostB());
        a.shrink(payA.take());
        b.shrink(payB.take());
        List<ItemStack> change = makeChange(payA.change() + payB.change(), currencies());
        if (change != null) PENDING.get().addAll(change);
        return true;
    }

    /** Before a trade is taken: drops change a previous, unfinished take left behind. */
    public static void beginTake() {
        PENDING.get().clear();
    }

    /** After a trade is taken: gives the player the change, dropping what does not fit. */
    public static void giveChange(Player player) {
        List<ItemStack> change = PENDING.get();
        if (!player.level().isClientSide())
            for (ItemStack coin : change) player.getInventory().placeItemBackInInventory(coin);
        change.clear();
    }
}
