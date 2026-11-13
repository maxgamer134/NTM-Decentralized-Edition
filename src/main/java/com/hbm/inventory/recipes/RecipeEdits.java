// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

/**
 * The datapack form of the KubeJS plugin's recipe edits, for packs without KubeJS:
 * {@code data/<namespace>/hbm_recipe_edits/<name>.json}.
 *
 * <pre>{
 *   "remove": [{"type": "hbm:shredder", "input": "minecraft:cobblestone"}],
 *   "replace_input": [{"filter": {"type": "hbm:assembly_machine"}, "from": "hbm:motor", "to": "minecraft:stick"}],
 *   "replace_output": [{"filter": {"type": "hbm:shredder"}, "from": "minecraft:gravel", "to": "minecraft:sand", "count": 2}]
 * }</pre>
 *
 * A filter takes any of {@code id}, {@code type}, {@code mod}, {@code input} and {@code output}, all of which must
 * match; an empty filter matches every recipe. Items are ids or {@code #tags}; {@code "fluid": true} on a
 * replacement swaps fluids instead (their amount and pressure stay). Replaced items keep their count unless
 * {@code count} is given. Edits run on the recipe json before it is read, so they reach every recipe type, crafting
 * included, and reload with /reload. Files apply in id order.
 */
public final class RecipeEdits {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FOLDER = "hbm_recipe_edits";

    private RecipeEdits() {}

    public static void apply(Map<ResourceLocation, JsonElement> recipes, ResourceManager manager) {
        Map<ResourceLocation, Resource> files =
                new TreeMap<>(manager.listResources(FOLDER, path -> path.getPath().endsWith(".json")));
        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet()) {
            try (Reader reader = file.getValue().openAsReader()) {
                JsonObject edits = JsonParser.parseReader(reader).getAsJsonObject();
                int changed = applyFile(recipes, edits);
                LOGGER.info("Recipe edits {} changed {} recipes", file.getKey(), changed);
            } catch (Exception e) {
                LOGGER.error("Bad recipe edit file {}: {}", file.getKey(), e.getMessage());
            }
        }
    }

    private static int applyFile(Map<ResourceLocation, JsonElement> recipes, JsonObject edits) {
        int changed = 0;
        if (edits.has("remove")) {
            for (JsonElement f : edits.getAsJsonArray("remove")) {
                JsonObject filter = f.getAsJsonObject();
                Iterator<Map.Entry<ResourceLocation, JsonElement>> it = recipes.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<ResourceLocation, JsonElement> e = it.next();
                    if (matches(filter, e.getKey(), e.getValue())) {
                        it.remove();
                        changed++;
                    }
                }
            }
        }
        changed += replaceAll(recipes, edits, "replace_input", false);
        changed += replaceAll(recipes, edits, "replace_output", true);
        return changed;
    }

    private static int replaceAll(
            Map<ResourceLocation, JsonElement> recipes, JsonObject edits, String key, boolean output) {
        if (!edits.has(key)) return 0;
        int changed = 0;
        for (JsonElement r : edits.getAsJsonArray(key)) {
            JsonObject rep = r.getAsJsonObject();
            JsonObject filter = rep.has("filter") ? rep.getAsJsonObject("filter") : new JsonObject();
            Replacement replacement =
                    new Replacement(
                            rep.get("from").getAsString(),
                            rep.get("to").getAsString(),
                            rep.has("count") ? rep.get("count").getAsInt() : -1,
                            rep.has("fluid") && rep.get("fluid").getAsBoolean());
            for (Map.Entry<ResourceLocation, JsonElement> e : recipes.entrySet()) {
                if (!e.getValue().isJsonObject() || !matches(filter, e.getKey(), e.getValue())) continue;
                if (walk(e.getValue(), null, false, false, output, replacement)) changed++;
            }
        }
        return changed;
    }

    private record Replacement(String from, String to, int count, boolean fluid) {}

    private static boolean matches(JsonObject filter, ResourceLocation id, JsonElement recipe) {
        if (!recipe.isJsonObject()) return false;
        JsonObject o = recipe.getAsJsonObject();
        if (filter.has("id") && !id.toString().equals(filter.get("id").getAsString())) return false;
        if (filter.has("mod") && !id.getNamespace().equals(filter.get("mod").getAsString())) return false;
        if (filter.has("type")
                && !(o.has("type") && o.get("type").getAsString().equals(filter.get("type").getAsString())))
            return false;
        if (filter.has("input") && !references(o, filter.get("input").getAsString(), false)) return false;
        if (filter.has("output") && !references(o, filter.get("output").getAsString(), true)) return false;
        return true;
    }

    private static boolean references(JsonObject recipe, String target, boolean output) {
        List<String> found = new ArrayList<>();
        collect(recipe, null, false, false, output, found);
        return found.contains(target);
    }

    private static boolean isOutputKey(String key) {
        return key.startsWith("output") || key.equals("result") || key.equals("results");
    }

    /** Every item and fluid reference on one side of a recipe. */
    private static void collect(
            JsonElement el, String key, boolean inFluids, boolean inOutput, boolean wantOutput, List<String> out) {
        if (el.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
                String k = e.getKey();
                if (key == null && k.equals("type")) continue;
                boolean side = inOutput || isOutputKey(k);
                collect(e.getValue(), k, inFluids || k.endsWith("_fluids"), side, wantOutput, out);
            }
        } else if (el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) collect(e, key, inFluids, inOutput, wantOutput, out);
        } else if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString() && inOutput == wantOutput) {
            String v = el.getAsString();
            if (inFluids) {
                if ("type".equals(key)) out.add(v);
            } else if ("tag".equals(key)) out.add("#" + v);
            else if (!"type".equals(key)) out.add(v);
        }
    }

    /** Swaps one item or fluid on one side; true if anything changed. */
    private static boolean walk(
            JsonElement el, String key, boolean inFluids, boolean inOutput, boolean wantOutput, Replacement r) {
        boolean changed = false;
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : new ArrayList<>(o.entrySet())) {
                String k = e.getKey();
                if (key == null && k.equals("type")) continue;
                boolean side = inOutput || isOutputKey(k);
                boolean fluids = inFluids || k.endsWith("_fluids");
                JsonElement v = e.getValue();
                if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString() && side == wantOutput) {
                    if (swapField(o, k, v.getAsString(), fluids, r)) changed = true;
                } else if (walk(v, k, fluids, side, wantOutput, r)) changed = true;
            }
        } else if (el.isJsonArray()) {
            JsonArray a = el.getAsJsonArray();
            for (int i = 0; i < a.size(); i++) {
                JsonElement v = a.get(i);
                if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString() && inOutput == wantOutput) {
                    if (!r.fluid && !inFluids && v.getAsString().equals(r.from)) {
                        a.set(i, r.count > 0 && wantOutput ? stack(r) : new JsonPrimitive(r.to));
                        changed = true;
                    }
                } else if (walk(v, key, inFluids, inOutput, wantOutput, r)) changed = true;
            }
        }
        return changed;
    }

    private static boolean swapField(JsonObject o, String k, String value, boolean fluids, Replacement r) {
        if (r.fluid) {
            if (!fluids || !k.equals("type") || !value.equals(r.from)) return false;
            o.addProperty("type", r.to);
            return true;
        }
        if (fluids || k.equals("type")) return false;
        boolean tag = k.equals("tag");
        if (!(tag ? ("#" + value).equals(r.from) : value.equals(r.from))) return false;
        if (tag) {
            o.remove("tag");
            if (r.to.startsWith("#")) o.addProperty("tag", r.to.substring(1));
            else o.addProperty("item", r.to);
        } else {
            o.addProperty(k, r.to);
        }
        if (r.count > 0 && o.has("count")) o.addProperty("count", r.count);
        else if (r.count > 0 && (k.equals("id") || k.equals("ingredient"))) o.addProperty("count", r.count);
        return true;
    }

    private static JsonObject stack(Replacement r) {
        JsonObject o = new JsonObject();
        o.addProperty("count", r.count);
        o.addProperty("id", r.to);
        return o;
    }
}
