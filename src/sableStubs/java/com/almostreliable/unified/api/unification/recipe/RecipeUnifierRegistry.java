package com.almostreliable.unified.api.unification.recipe;

/** Compile-only stand-in for AlmostUnified's RecipeUnifierRegistry (only the members NTM uses). */
public interface RecipeUnifierRegistry {

    void registerForModId(String modId, RecipeUnifier unifier);
}
