package com.almostreliable.unified.api.unification.recipe;

/** Compile-only stand-in for AlmostUnified's RecipeUnifier. */
public interface RecipeUnifier {

    void unify(UnificationHelper helper, RecipeJson recipe);
}
