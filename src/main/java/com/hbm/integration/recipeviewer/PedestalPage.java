// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.PedestalRecipe;
import com.hbm.inventory.recipes.PedestalRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import java.util.List;
import java.util.Locale;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

// main has no NEI page for the pedestal ritual; drawn as its universal pages are: the 3x3 grid
// is the ritual circle seen from above, the centre being the pedestal that holds the result
public final class PedestalPage extends TablePage<PedestalRecipe> {

    PedestalPage() {
        super(PageIds.page("pedestal"), PedestalRecipe.class, ModBlocks.PEDESTAL.get(), "block.hbm.pedestal");
    }

    @Override
    public List<PedestalRecipe> rows() {
        return PedestalRecipes.INSTANCE.recipes();
    }

    @Override
    public void layout(PedestalRecipe recipe, PageLayout page) {
        CountIngredient[] grid = recipe.input();
        for (int i = 0; i < PedestalRecipe.GRID_SIZE; i++) {
            PageSlot slot = page.input(12 + i % 3 * 18, 6 + i / 3 * 18).background();
            if (grid[i] != null) slot.items(grid[i].displayStacks());
        }
        page.output(102, 24).background().item(recipe.output());
        page.catalyst(75, 31).item(ModBlocks.PEDESTAL.get());
    }

    @Override
    public void draw(PedestalRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
        if (recipe.extra() != PedestalRecipes.ExtraCondition.NONE) {
            RecipePanel.rightAligned(
                    graphics,
                    Component.translatable(
                            "jei.hbm.pedestal." + recipe.extra().name().toLowerCase(Locale.ROOT)),
                    52,
                    RecipePanel.TEXT);
        }
    }
}
