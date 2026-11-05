// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.recipeviewer;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * backport-fix: BF-129 the meteorite sword's upgrades that happen inside a machine (an addition:
 * main 1.7.10 does them in the tile entities only and has no NEI page, so the progression stopped
 * at the alloyed sword). The assembly machine and the chemical plant upgrade a sword in their
 * battery slot while they run a recipe, the research reactor and ZIRNOX one in a fuel slot while
 * running, and the fusion breeder breeds one from its input slot. Each row is that machine's own
 * GUI with the sword in the slot: a window onto the GUI where the slot sits on the page's input,
 * then the usual arrow and output, or the fusion breeder's GUI as it is (it has an output slot).
 */
public final class MeteoriteSwordPage extends RecipePage<MeteoriteSwordPage.Recipe> {

    // the page's input slot frame, where the sword's slot in the GUI lands
    private static final int SLOT_X = 47;
    private static final int SLOT_Y = 23;

    private static final ResourceLocation ASSEMBLER =
            Library.id("textures/gui/processing/gui_assembler.png");
    private static final ResourceLocation CHEMPLANT =
            Library.id("textures/gui/processing/gui_chemplant.png");
    private static final ResourceLocation RESEARCH_REACTOR =
            Library.id("textures/gui/reactors/gui_research_reactor.png");
    private static final ResourceLocation ZIRNOX =
            Library.id("textures/gui/reactors/gui_zirnox.png");
    private static final ResourceLocation FUSION_BREEDER =
            Library.id("textures/gui/reactors/gui_fusion_breeder.png");

    MeteoriteSwordPage() {
        super(PageIds.page("meteorite_sword"), Recipe.class);
    }

    @Override
    public Component title() {
        return Component.translatable("item.hbm.meteorite_sword");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(ModItems.METEORITE_SWORD.get());
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of(
                new ItemStack(ModBlocks.MACHINE_ASSEMBLY_MACHINE.get()),
                new ItemStack(ModBlocks.MACHINE_CHEMICAL_PLANT.get()),
                new ItemStack(ModBlocks.REACTOR_RESEARCH.get()),
                new ItemStack(ModBlocks.REACTOR_ZIRNOX.get()),
                new ItemStack(ModBlocks.FUSION_BREEDER.get()));
    }

    @Override
    public List<Recipe> rows() {
        // slot frames from the menus: battery 152,81, fuel 113,40 and 44,34, breeder input 48,45
        return List.of(
                row(
                        "assembly_machine",
                        ModItems.METEORITE_SWORD_ALLOYED.get(),
                        ModItems.METEORITE_SWORD_MACHINED.get(),
                        ModBlocks.MACHINE_ASSEMBLY_MACHINE.get(),
                        ASSEMBLER,
                        151,
                        80,
                        8,
                        33,
                        false),
                row(
                        "chemical_plant",
                        ModItems.METEORITE_SWORD_MACHINED.get(),
                        ModItems.METEORITE_SWORD_TREATED.get(),
                        ModBlocks.MACHINE_CHEMICAL_PLANT.get(),
                        CHEMPLANT,
                        151,
                        80,
                        8,
                        33,
                        false),
                row(
                        "research_reactor",
                        ModItems.METEORITE_SWORD_BRED.get(),
                        ModItems.METEORITE_SWORD_IRRADIATED.get(),
                        ModBlocks.REACTOR_RESEARCH.get(),
                        RESEARCH_REACTOR,
                        112,
                        39,
                        26,
                        51,
                        false),
                row(
                        "zirnox",
                        ModItems.METEORITE_SWORD_BRED.get(),
                        ModItems.METEORITE_SWORD_IRRADIATED.get(),
                        ModBlocks.REACTOR_ZIRNOX.get(),
                        ZIRNOX,
                        43,
                        33,
                        26,
                        51,
                        false),
                row(
                        "fusion_breeder",
                        ModItems.METEORITE_SWORD_IRRADIATED.get(),
                        ModItems.METEORITE_SWORD_FUSED.get(),
                        ModBlocks.FUSION_BREEDER.get(),
                        FUSION_BREEDER,
                        47,
                        44,
                        SLOT_X,
                        RecipePanel.WIDTH,
                        true));
    }

    // slotX/slotY: the sword's slot frame in the GUI; left/width: the window's span around it
    private Recipe row(
            String name,
            ItemLike from,
            ItemLike to,
            ItemLike machine,
            ResourceLocation gui,
            int slotX,
            int slotY,
            int left,
            int width,
            boolean guiOutput) {
        return new Recipe(
                PageIds.derived(id(), name),
                new ItemStack(from),
                new ItemStack(to),
                new ItemStack(machine),
                gui,
                SLOT_X - left,
                slotX - left,
                slotY - SLOT_Y,
                width,
                guiOutput);
    }

    @Override
    public void layout(Recipe recipe, PageLayout page) {
        page.input(SLOT_X + 1, SLOT_Y + 1).item(recipe.from());
        if (recipe.guiOutput()) {
            // the fusion breeder's output slot, 64 to the right of its input
            page.output(SLOT_X + 65, SLOT_Y + 1).item(recipe.to());
            return;
        }
        page.output(102, 24).background().item(recipe.to());
        page.catalyst(75, 31).item(recipe.machine());
    }

    @Override
    public void draw(Recipe recipe, GuiGraphicsExtractor graphics) {
        RecipePanel.region(
                graphics,
                recipe.gui(),
                recipe.x(),
                0,
                recipe.u(),
                recipe.v(),
                recipe.width(),
                RecipePanel.HEIGHT);
        if (!recipe.guiOutput()) RecipePanel.universalPage(graphics);
    }

    public record Recipe(
            ResourceLocation id,
            ItemStack from,
            ItemStack to,
            ItemStack machine,
            ResourceLocation gui,
            int x,
            int u,
            int v,
            int width,
            boolean guiOutput)
            implements RecipePage.Row {}
}
