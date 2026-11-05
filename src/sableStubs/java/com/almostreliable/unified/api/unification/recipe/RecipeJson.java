package com.almostreliable.unified.api.unification.recipe;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;

/** Compile-only stand-in for AlmostUnified's RecipeJson. */
public interface RecipeJson extends RecipeData {

    @Nullable
    JsonElement getProperty(String property);

    void setProperty(String property, JsonElement value);
}
