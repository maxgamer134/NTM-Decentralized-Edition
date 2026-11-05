// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.almostunified;

import com.almostreliable.unified.api.unification.UnificationEntry;
import com.almostreliable.unified.api.unification.UnificationLookup;
import com.almostreliable.unified.api.unification.recipe.RecipeJson;
import com.almostreliable.unified.api.unification.recipe.RecipeUnifier;
import com.almostreliable.unified.api.unification.recipe.UnificationHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * backport-fix: BF-128 unifies NTM's recipe JSON the way AU unifies vanilla recipes: an input item
 * that AU hides becomes its unify tag (so every variant fits), an output item that AU hides becomes
 * the preferred item. Ingredients are NTM's string forms ({@code "ns:item"}, {@code "#tag"}, lists)
 * or 1.21.1 objects, wrapped in {@code input_items[].ingredient}, {@code key}, {@code ingredients},
 * {@code pcb}/{@code solder}, {@code base} (hbm:recipe_input) and compound {@code children}; outputs
 * are id strings or {@code {"id": ..}} objects in {@code output_items} (weighted pools nest them in
 * {@code data}), {@code result} and {@code dead_end_items}. Left alone: stacks with components (they
 * are NTM-specific), fluids, icons, and an output whose preferred item the recipe's own inputs take
 * (it would loop: "shredder: diamond ore to diamond gravel" would become diamond ore to diamond ore).
 */
final class NTMRecipeUnifier implements RecipeUnifier {

    private static final List<String> INPUT_KEYS =
            List.of("input_items", "ingredients", "ingredient", "pcb", "solder");
    private static final List<String> OUTPUT_KEYS = List.of("output_items", "result", "dead_end_items");

    @Override
    public void unify(UnificationHelper helper, RecipeJson recipe) {
        Pass pass = new Pass(helper);
        for (String key : INPUT_KEYS) {
            JsonElement value = recipe.getProperty(key);
            if (value != null) recipe.setProperty(key, pass.input(value));
        }
        // shaped crafting and ammo press patterns: one ingredient per symbol
        if (recipe.getProperty("key") instanceof JsonObject symbols)
            for (Map.Entry<String, JsonElement> symbol : symbols.entrySet())
                symbol.setValue(pass.input(symbol.getValue()));
        for (String key : OUTPUT_KEYS) {
            JsonElement value = recipe.getProperty(key);
            if (value != null) recipe.setProperty(key, pass.output(value));
        }
    }

    static final class Pass {

        private final UnificationLookup lookup;
        // the recipe's inputs after unifying: item ids and "#tags", for the loop check on outputs
        private final Set<String> consumed = new HashSet<>();
        boolean changed;

        Pass(UnificationHelper helper) {
            this.lookup = helper.getUnificationLookup();
        }

        JsonElement input(JsonElement value) {
            if (value instanceof JsonPrimitive primitive && primitive.isString()) {
                String tag = inputTag(primitive.getAsString());
                JsonElement unified = tag == null ? value : changed(new JsonPrimitive("#" + tag));
                consumed.add(unified.getAsString());
                return unified;
            }
            if (value instanceof JsonArray array) {
                for (int i = 0; i < array.size(); i++) array.set(i, input(array.get(i)));
                return value;
            }
            if (!(value instanceof JsonObject object) || object.has("fluid")) return value;
            // 1.21.1 item object: {"item": ..} becomes {"tag": ..}, as AU does for vanilla recipes
            if (object.get("item") instanceof JsonPrimitive item && item.isString()) {
                String tag = inputTag(item.getAsString());
                if (tag != null) {
                    object.remove("item");
                    object.addProperty("tag", tag);
                    changed = true;
                }
            }
            if (object.get("item") instanceof JsonPrimitive item && item.isString())
                consumed.add(item.getAsString());
            if (object.get("tag") instanceof JsonPrimitive tag && tag.isString())
                consumed.add("#" + tag.getAsString());
            // entries ({"ingredient": .., "count": ..}), hbm:recipe_input and compound ingredients;
            // a difference ingredient's "subtracted" part stays as it is
            for (String key : List.of("ingredient", "base", "children", "ingredients"))
                if (object.has(key)) object.add(key, input(object.get(key)));
            return value;
        }

        JsonElement output(JsonElement value) {
            if (value instanceof JsonPrimitive primitive && primitive.isString()) {
                String target = outputTarget(primitive.getAsString());
                return target == null ? value : changed(new JsonPrimitive(target));
            }
            if (value instanceof JsonArray array) {
                for (int i = 0; i < array.size(); i++) array.set(i, output(array.get(i)));
                return value;
            }
            if (!(value instanceof JsonObject object) || object.has("components")) return value;
            if (object.has("data")) object.add("data", output(object.get("data")));
            for (String key : List.of("id", "item")) {
                if (object.get(key) instanceof JsonPrimitive id && id.isString()) {
                    String target = outputTarget(id.getAsString());
                    if (target != null) {
                        object.addProperty(key, target);
                        changed = true;
                    }
                }
            }
            return value;
        }

        // the unify tag of an item that AU hides (another mod's item of that tag is preferred)
        private @Nullable String inputTag(String id) {
            ResourceLocation item = hiddenItem(id);
            if (item == null) return null;
            TagKey<Item> tag = lookup.getRelevantItemTag(item);
            return tag == null ? null : tag.location().toString();
        }

        // the preferred item for an item that AU hides
        private @Nullable String outputTarget(String id) {
            ResourceLocation item = hiddenItem(id);
            if (item == null) return null;
            UnificationEntry<Item> target = lookup.getVariantItemTarget(item);
            if (target == null) return null;
            TagKey<Item> tag = lookup.getRelevantItemTag(item);
            // a recipe whose input takes the preferred item would turn it into more of itself
            if (consumed.contains(target.id().toString())
                    || tag != null && consumed.contains("#" + tag.location())) return null;
            return target.id().toString();
        }

        private @Nullable ResourceLocation hiddenItem(String id) {
            if (id.startsWith("#")) return null;
            ResourceLocation item = ResourceLocation.tryParse(id);
            if (item == null) return null;
            UnificationEntry<Item> target = lookup.getVariantItemTarget(item);
            return target == null || target.id().equals(item) ? null : item;
        }

        private JsonElement changed(JsonElement value) {
            changed = true;
            return value;
        }
    }
}
