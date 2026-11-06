// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.google.gson.JsonElement;
import com.hbm.NuclearTech;
import com.hbm.config.BalanceConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * backport: the gun content switches (disable_all_guns, disable_post_rework_guns,
 * disable_pre_rework_guns in the content config). A disabled item stays registered, so worlds and
 * servers keep loading, but it leaves the creative tabs and recipe viewers, every recipe naming it
 * is dropped, and a disabled gun does not fire.
 */
public final class GunContent {

    /** main's current guns and what only they use: parts, casings, ammo, mods and the ammo press. */
    private static final Pattern POST_REWORK =
            Pattern.compile(
                    "gun_b92(_ammo)?|ammo_(standard|secret)_.+|ammo_bag(_infinite)?|casing_.+|"
                            + "weapon_mod_.+|part_(barrel|receiver|mechanism|stock|grip)_.+|machine_ammo_press");

    /** Items from main's guns before the rework; a port of them adds its items here. */
    private static final Set<ResourceLocation> PRE_REWORK = new HashSet<>();

    private static volatile Set<Item> disabled;

    private GunContent() {}

    public static synchronized void markPreRework(ResourceLocation item) {
        PRE_REWORK.add(item);
        disabled = null;
    }

    /** Called when the content config is (re)loaded. */
    public static void invalidate() {
        disabled = null;
    }

    public static boolean isDisabled(Item item) {
        return disabled().contains(item);
    }

    public static boolean isDisabled(ItemStack stack) {
        return !stack.isEmpty() && isDisabled(stack.getItem());
    }

    public static Set<Item> disabled() {
        Set<Item> set = disabled;
        if (set == null) disabled = set = compute();
        return set;
    }

    private static Set<Item> compute() {
        boolean post = BalanceConfig.disableAllGuns || BalanceConfig.disablePostReworkGuns;
        boolean pre = BalanceConfig.disableAllGuns || BalanceConfig.disablePreReworkGuns;
        if (!post && (!pre || PRE_REWORK.isEmpty())) return Set.of();
        Set<Item> out = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Map.Entry<net.minecraft.resources.ResourceKey<Item>, Item> e :
                BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation id = e.getKey().location();
            if (pre && PRE_REWORK.contains(id)) out.add(e.getValue());
            else if (post
                    && (e.getValue() instanceof ItemGunBaseNT
                            || (id.getNamespace().equals(NuclearTech.MOD_ID)
                                    && POST_REWORK.matcher(id.getPath()).matches())))
                out.add(e.getValue());
        }
        return Collections.unmodifiableSet(out);
    }

    /** Ids of the recipes whose json names a disabled item anywhere (output, input, catalyst). */
    public static Set<ResourceLocation> recipesToDrop(Map<ResourceLocation, JsonElement> recipes) {
        Set<Item> items = disabled();
        if (items.isEmpty()) return Set.of();
        Set<String> ids = new HashSet<>();
        for (Item item : items) ids.add(BuiltInRegistries.ITEM.getKey(item).toString());
        Set<ResourceLocation> drop = new HashSet<>();
        recipes.forEach((id, json) -> {
            if (names(json, ids)) drop.add(id);
        });
        return drop;
    }

    private static boolean names(JsonElement json, Set<String> ids) {
        if (json.isJsonPrimitive()) {
            return json.getAsJsonPrimitive().isString() && ids.contains(json.getAsString());
        }
        if (json.isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray()) if (names(e, ids)) return true;
        } else if (json.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject().entrySet())
                if (names(e.getValue(), ids)) return true;
        }
        return false;
    }
}
