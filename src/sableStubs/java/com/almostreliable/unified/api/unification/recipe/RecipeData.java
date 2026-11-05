package com.almostreliable.unified.api.unification.recipe;

import net.minecraft.resources.ResourceLocation;

/** Compile-only stand-in for AlmostUnified's RecipeData. */
public interface RecipeData {

    ResourceLocation getId();

    ResourceLocation getType();

    boolean hasProperty(String property);
}
