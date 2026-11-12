// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class CryoDistillRecipes extends SimpleGenericRecipes<CryoDistillRecipe> {

    public static final CryoDistillRecipes INSTANCE = new CryoDistillRecipes();

    public static final long POWER_PER_OP = 20_000L;
    public static final int FILL_PER_OP = 100;

    @Override
    protected String registryName() {
        return "cryo_distiller";
    }

    @Override
    protected RecipeSerializer<CryoDistillRecipe> serializer() {
        return CryoDistillRecipe.SERIALIZER;
    }

    public @Nullable CryoDistillRecipe getRecipeForInput(@Nullable Fluid type) {
        return byInputFluid().findLast(type);
    }
}
