// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.capability.FluidContainerRows;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidCell;
import com.hbm.items.machine.ItemFluidTank;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

// main: FluidRecipeHandler ("Fluid Containers"), how every canister, barrel, tank and cell is filled
public final class FluidContainerPage extends RecipePage<FluidContainerPage.Recipe> {

    FluidContainerPage() {
        super(PageIds.page("fluid_containers"), Recipe.class);
    }

    private static List<ItemFluidTank> families() {
        return List.of(
                ModItems.FLUID_TANK.get(),
                ModItems.FLUID_BARREL.get(),
                ModItems.FLUID_TANK_LEAD.get(),
                ModItems.CANISTER.get(),
                ModItems.GAS_TANK.get(),
                ModItems.DISPERSER_CANISTER.get(),
                ModItems.GLYPHID_GLAND.get());
    }

    private static ItemStack emptyOf(ItemFluidTank family) {
        if (family == ModItems.DISPERSER_CANISTER.get())
            return new ItemStack(ModItems.DISPERSER_CANISTER_EMPTY);
        if (family == ModItems.GLYPHID_GLAND.get())
            return new ItemStack(ModItems.GLYPHID_GLAND_EMPTY);
        return new ItemStack(family);
    }

    @Override
    public List<Recipe> rows() {
        List<Recipe> rows = new ArrayList<>();
        for (Fluid fluid : NTMFluids.displayOrder()) {
            for (ItemFluidTank family : families()) {
                if (!family.canStore(fluid)) continue;
                rows.add(row(fluid, family.capacity, emptyOf(family), family.make(fluid, family.capacity)));
            }
        }
        for (ItemFluidCell cell : ItemFluidCell.filledCells()) {
            Fluid fluid = cell.fluid();
            if (fluid != null) rows.add(row(fluid, ItemFluidCell.CAPACITY, cell.emptyStack(), new ItemStack(cell)));
        }
        for (FluidContainerRows.Row row : FluidContainerRows.rows()) {
            rows.add(row(row.fluid().get(), row.amount().getAsInt(), row.emptyStack(), row.fullStack()));
        }
        return rows;
    }

    private Recipe row(Fluid fluid, int amount, ItemStack empty, ItemStack full) {
        ResourceLocation id =
                PageIds.derived(
                        id(),
                        PageIds.segment(BuiltInRegistries.ITEM.getKey(full.getItem())),
                        PageIds.segment(BuiltInRegistries.FLUID.getKey(fluid)));
        return new Recipe(id, fluid, amount, empty, full);
    }

    @Override
    public Component title() {
        return Component.translatable("jei.hbm.fluid_containers");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModItems.CANISTER.get());
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of();
    }

    // main: FluidRecipeHandler finds a row from the fluid, the empty or the full container on either
    // side, matching its two-way arrow, so each side is also a hidden ingredient of the other
    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(30, 24).fluid(recipe.fluid(), recipe.amount());
        if (!recipe.empty().isEmpty()) page.input(48, 24).item(recipe.empty());
        page.output(120, 24).item(recipe.full());

        page.hiddenInput().item(recipe.full());
        page.hiddenOutput().fluid(recipe.fluid(), recipe.amount());
        if (!recipe.empty().isEmpty()) page.hiddenOutput().item(recipe.empty());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.page(graphics, RecipePanel.FLUID);
    }

    public record Recipe(
            ResourceLocation id,
            Fluid fluid,
            int amount,
            ItemStack empty,
            ItemStack full) implements RecipePage.Row {}
}
