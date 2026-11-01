// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.SolidificationRecipe;
import com.hbm.inventory.recipes.SolidificationRecipes;
import java.util.List;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.ResourceLocation;
import com.hbm.backport.random.WeightedList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public final class SolidificationPage extends RecipePage<SolidificationPage.Recipe> {

    SolidificationPage() {
        super(
                PageIds.page("solidification"),
                Recipe.class,
                ModBlocks.MACHINE_SOLIDIFIER,
                "block.hbm.machine_solidifier");
    }

    @Override
    public List<Recipe> rows() {
        return SolidificationRecipes.INSTANCE.reachable().stream().map(Recipe::new).toList();
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(48, 24).background().fluid(recipe.input(), recipe.amount());
        GenericMachinePage.outputSlots(
                page,
                GenericMachinePage.outputPositions(GenericMachinePage.outcomes(recipe.output()).size()),
                0,
                0,
                recipe.output(),
                true);
        page.catalyst(75, 31).item(ModBlocks.MACHINE_SOLIDIFIER.get());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }

    public record Recipe(
            ResourceLocation id,
            Fluid input,
            int amount,
            WeightedList<ItemStack> output)
            implements RecipePage.Row {
        public Recipe(SolidificationRecipe recipe) {
            this(
                    recipe.recipeId().location(),
                    recipe.inputFluid[0].type(),
                    recipe.fillReq(),
                    unwrap(recipe.outputItems()));
        }

        private static WeightedList<ItemStack> unwrap(WeightedList<ItemStack>[] outputs) {
            return outputs.length > 0 ? outputs[0] : WeightedList.of();
        }
    }
}
