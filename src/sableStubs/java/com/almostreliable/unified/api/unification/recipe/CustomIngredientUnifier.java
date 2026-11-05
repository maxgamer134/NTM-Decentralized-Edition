package com.almostreliable.unified.api.unification.recipe;

import com.google.gson.JsonObject;

/** Compile-only stand-in for AlmostUnified's CustomIngredientUnifier. */
public interface CustomIngredientUnifier {

    boolean unify(UnificationHelper helper, JsonObject ingredient);
}
