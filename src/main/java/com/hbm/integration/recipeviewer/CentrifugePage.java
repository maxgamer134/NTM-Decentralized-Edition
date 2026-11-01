// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.CentrifugeRecipe;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import java.util.List;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.random.WeightedList;
import net.minecraft.world.item.ItemStack;

public final class CentrifugePage extends TablePage<CentrifugeRecipe> {

    CentrifugePage() {
        super(
                PageIds.page("centrifuge"),
                CentrifugeRecipe.class,
                ModBlocks.MACHINE_CENTRIFUGE,
                "block.hbm.machine_centrifuge");
    }

    private static int[][] outputPositions(int count) {
        return switch (count) {
            case 1 -> new int[][] {{102, 24}};
            case 2 -> new int[][] {{102, 24}, {120, 24}};
            case 3 -> new int[][] {{102, 24}, {120, 24}, {138, 24}};
            case 4 -> new int[][] {{102, 15}, {120, 15}, {102, 33}, {120, 33}};
            // datapack rows past the machine's four slots still show, on the generic grid
            default -> GenericMachinePage.outputPositions(count);
        };
    }

    @Override
    public List<CentrifugeRecipe> rows() {
        return CentrifugeRecipes.INSTANCE.recipes();
    }

    @Override
    public void layout(CentrifugeRecipe recipe, PageLayout page) {
        page.input(48, 24).background().items(recipe.inputItem[0].displayStacks());

        WeightedList<ItemStack>[] outputs = recipe.outputItems();
        int[][] positions = outputPositions(GenericMachinePage.outcomeCount(outputs));
        int slot = 0;
        for (WeightedList<ItemStack> output : outputs)
            slot = GenericMachinePage.outputSlots(page, positions, slot, 0, output, true);

        page.catalyst(75, 31).item(ModBlocks.MACHINE_CENTRIFUGE.get());
    }

    @Override
    public void draw(CentrifugeRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.universalPage(graphics);
    }
}
