// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.special.ItemHot;
import java.util.List;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class AnvilSmithingPage extends TablePage<AnvilSmithingRecipe> {

    AnvilSmithingPage() {
        super(PageIds.page("anvil_smithing"), AnvilSmithingRecipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("desc.misc.anvilConstructionCategory.anvil");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModBlocks.ANVIL_IRON);
    }

    @Override
    public List<AnvilSmithingRecipe> rows() {
        return AnvilSmithingRecipes.INSTANCE.recipes();
    }

    @Override
    public List<ItemStack> catalysts() {
        return AnvilConstructionPage.anvils();
    }

    @Override
    public void layout(AnvilSmithingRecipe recipe, PageLayout page) {
        boolean hot = recipe.kind == AnvilSmithingRecipe.Kind.HOT;
        page.input(39, 24).items(heated(recipe.left().displayStacks(), hot));
        page.input(75, 24).items(heated(recipe.right().displayStacks(), hot));
        page.output(111, 24).item(heated(recipe.getSimpleOutput(), hot));
    }

    // main: AnvilSmithingHotRecipe shows its ingots (and result) freshly heated
    private static List<ItemStack> heated(List<ItemStack> stacks, boolean hot) {
        return hot ? stacks.stream().map(stack -> heated(stack, true)).toList() : stacks;
    }

    private static ItemStack heated(ItemStack stack, boolean hot) {
        if (!hot || !(stack.getItem() instanceof ItemHot)) return stack;
        ItemStack copy = stack.copy();
        copy.set(ModDataComponents.HOT_ITEM_HEAT.get(), ItemHot.getMaxHeat(copy));
        return copy;
    }

    @Override
    public void draw(AnvilSmithingRecipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.SMITHING);
        RecipePanel.leftAligned(
                graphics,
                Component.translatable("jei.hbm.anvil_tier", recipe.tier),
                52,
                43,
                RecipePanel.TEXT);
    }
}
