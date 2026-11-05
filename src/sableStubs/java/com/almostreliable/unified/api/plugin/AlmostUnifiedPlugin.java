package com.almostreliable.unified.api.plugin;

import com.almostreliable.unified.api.unification.recipe.CustomIngredientUnifierRegistry;
import com.almostreliable.unified.api.unification.recipe.RecipeUnifierRegistry;
import net.minecraft.resources.ResourceLocation;

/** Compile-only stand-in for AlmostUnified's plugin API (AU 1.4, 1.21.1). */
public interface AlmostUnifiedPlugin {

    ResourceLocation getPluginId();

    default void registerRecipeUnifiers(RecipeUnifierRegistry registry) {}

    default void registerCustomIngredientUnifiers(CustomIngredientUnifierRegistry registry) {}
}
