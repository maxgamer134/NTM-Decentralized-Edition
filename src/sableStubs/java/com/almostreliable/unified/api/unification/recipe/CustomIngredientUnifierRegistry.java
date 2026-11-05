package com.almostreliable.unified.api.unification.recipe;

import net.minecraft.resources.ResourceLocation;

/** Compile-only stand-in for AlmostUnified's CustomIngredientUnifierRegistry (only the members NTM uses). */
public interface CustomIngredientUnifierRegistry {

    void registerForType(ResourceLocation type, CustomIngredientUnifier unifier);
}
