// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.almostunified;

import com.almostreliable.unified.api.plugin.AlmostUnifiedNeoPlugin;
import com.almostreliable.unified.api.plugin.AlmostUnifiedPlugin;
import com.almostreliable.unified.api.unification.recipe.CustomIngredientUnifierRegistry;
import com.almostreliable.unified.api.unification.recipe.RecipeUnifierRegistry;
import com.google.gson.JsonObject;
import com.hbm.NuclearTech;
import com.hbm.inventory.recipes.ingredient.HbmRecipeIngredient;
import com.hbm.lib.Library;
import net.minecraft.resources.ResourceLocation;

/**
 * backport-fix: BF-128 AlmostUnified compat (an addition: 1.7.10 has no AlmostUnified). AU hides an
 * item from recipe viewers when another mod's item of the same unify tag is preferred, and EMI (and
 * TMRV) then drops every recipe showing a hidden item. AU swaps such items for the preferred one in
 * recipe JSON, but only in the vanilla shapes it knows; NTM's recipes name items as plain strings
 * ({@code "hbm:plate_iron"}) under its own keys ({@code input_items}, {@code output_items}), so they
 * kept the hidden items and vanished from EMI/TMRV in packs that hide (alumina electrolysis, most
 * centrifuge, shredder and SILEX rows, ...). This teaches AU NTM's recipe format. AU finds the class
 * by its annotation and only loads it when AU is installed.
 */
@AlmostUnifiedNeoPlugin
public final class NTMAlmostUnifiedPlugin implements AlmostUnifiedPlugin {

    @Override
    public ResourceLocation getPluginId() {
        return Library.id("almostunified");
    }

    @Override
    public void registerRecipeUnifiers(RecipeUnifierRegistry registry) {
        // every hbm:* recipe type: machines, anvils, hbm:crafting_shaped/shapeless, ...
        registry.registerForModId(NuclearTech.MOD_ID, new NTMRecipeUnifier());
    }

    @Override
    public void registerCustomIngredientUnifiers(CustomIngredientUnifierRegistry registry) {
        // hbm:recipe_input inside vanilla-typed recipes (smelting, blasting): AU's own unifier
        // handles the rest of those recipes and hands this ingredient over
        registry.registerForType(
                HbmRecipeIngredient.ID,
                (helper, ingredient) -> unifyBase(new NTMRecipeUnifier.Pass(helper), ingredient));
    }

    private static boolean unifyBase(NTMRecipeUnifier.Pass pass, JsonObject ingredient) {
        if (!ingredient.has("base")) return false;
        ingredient.add("base", pass.input(ingredient.get("base")));
        return pass.changed;
    }
}
