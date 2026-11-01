// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.ExposureChamberRecipe;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.blockentity.machine.BlockEntityMachineExposureChamber;
import java.util.List;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;

public final class ExposureChamberPage extends TablePage<ExposureChamberRecipe> {

    private static final int PARTICLE_X = 30, PARTICLE_Y = 24;
    private static final int INGREDIENT_X = 48, INGREDIENT_Y = 24;
    private static final int OUTPUT_X = 102, OUTPUT_Y = 24;

    private static final int BATCH = BlockEntityMachineExposureChamber.MAX_PARTICLES;

    ExposureChamberPage() {
        super(
                PageIds.page("exposure_chamber"),
                ExposureChamberRecipe.class,
                ModBlocks.MACHINE_EXPOSURE_CHAMBER,
                "block.hbm.machine_exposure_chamber");
    }

    @Override
    public List<ExposureChamberRecipe> rows() {
        return ExposureChamberRecipes.INSTANCE.recipes();
    }

    @Override
    public void layout(ExposureChamberRecipe recipe, PageLayout page) {
        page.input(PARTICLE_X, PARTICLE_Y).background().items(recipe.particle().displayStacks());
        page.input(INGREDIENT_X, INGREDIENT_Y)
                .background()
                .items(CountIngredient.displayStacks(recipe.ingredient().ingredient(), BATCH));
        page.output(OUTPUT_X, OUTPUT_Y).background().item(recipe.output().copyWithCount(BATCH));
        page.catalyst(75, 31).item(ModBlocks.MACHINE_EXPOSURE_CHAMBER.get());
    }

    @Override
    public void draw(ExposureChamberRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }
}
