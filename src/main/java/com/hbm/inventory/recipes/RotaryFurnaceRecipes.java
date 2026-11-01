// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class RotaryFurnaceRecipes extends SimpleGenericRecipes<RotaryFurnaceRecipe> {

    public static final RotaryFurnaceRecipes INSTANCE = new RotaryFurnaceRecipes();

    public static @Nullable RotaryFurnaceRecipe getRecipe(
            @Nullable Fluid fluid, ItemStack... inputs) {
        outer:
        for (RotaryFurnaceRecipe recipe : INSTANCE.recipes()) {

            // never try to match if a fluid is required and our tank doesn't match it
            FluidStackNTM required = recipe.fluid();
            if (required != null && required.type() != fluid) continue;

            List<CountIngredient> recipeList = new ArrayList<>(List.of(recipe.inputItem));

            for (ItemStack inputStack : inputs) {
                if (inputStack.isEmpty()) continue;

                boolean hasMatch = false;
                Iterator<CountIngredient> iterator = recipeList.iterator();

                while (iterator.hasNext()) {
                    CountIngredient recipeStack = iterator.next();
                    if (recipeStack.test(inputStack)) {
                        hasMatch = true;
                        iterator.remove();
                        break;
                    }
                }

                if (!hasMatch) continue outer;
            }

            if (recipeList.isEmpty()) return recipe;
        }

        return null;
    }

    @Override
    protected String registryName() {
        return "rotary_furnace";
    }

    @Override
    protected RecipeSerializer<RotaryFurnaceRecipe> serializer() {
        return RotaryFurnaceRecipe.SERIALIZER;
    }
}
