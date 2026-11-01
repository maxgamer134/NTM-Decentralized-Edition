// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.GasCentrifugeRecipe;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBatterySC;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.hbm.backport.random.Weighted;
import com.hbm.backport.random.WeightedList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class GasCentrifugePage extends RecipePage<GasCentrifugePage.Cascade> {
    private static final int MAX_STEPS = 32;

    GasCentrifugePage() {
        super(
                PageIds.page("gas_centrifuge"),
                Cascade.class,
                ModBlocks.MACHINE_GASCENT,
                "gui.jei.category.gasCentrifuge");
    }

    @Override
    public List<Cascade> rows() {
        GasCentrifugeRecipes table = GasCentrifugeRecipes.INSTANCE;
        List<Cascade> cascades = new ArrayList<>();

        List<GasCentrifugeRecipe> feeds = new ArrayList<>();
        for (GasCentrifugeRecipe recipe : table.allStages()) {
            if (recipe.feed != null) feeds.add(recipe);
        }
        feeds.sort(
                Comparator.comparingInt((GasCentrifugeRecipe r) -> r.order)
                        .thenComparing((GasCentrifugeRecipe r) -> r.stage));

        for (GasCentrifugeRecipe feed : feeds) {
            List<GasCentrifugeRecipe> chain = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            GasCentrifugeRecipe recipe = feed;
            while (recipe != null && seen.add(recipe.stage) && chain.size() < MAX_STEPS) {
                chain.add(recipe);
                Cascade cascade = build(feed.feed, chain);
                if (cascade != null) cascades.add(cascade);
                recipe = recipe.produced > 0 ? table.byStage(recipe.next) : null;
            }
        }

        return cascades;
    }

    private static @Nullable Cascade build(Fluid feed, List<GasCentrifugeRecipe> chain) {
        int size = chain.size();
        GasCentrifugeRecipe last = chain.get(size - 1);

        for (GasCentrifugeRecipe recipe : chain) if (recipe.consumed <= 0) return null;

        boolean handsOver = last.produced > 0 && last.next != null;
        GasCentrifugeRecipe leftover =
                handsOver ? GasCentrifugeRecipes.INSTANCE.byStage(last.next) : null;
        if (handsOver && (leftover == null || leftover.deadEndVolume <= 0)) return null;

        long[] num = new long[size];
        long[] den = new long[size];
        num[0] = 1;
        den[0] = 1;
        for (int k = 1; k < size; k++) {
            long n = num[k - 1] * chain.get(k - 1).produced;
            long d = den[k - 1] * chain.get(k).consumed;
            long g = gcd(n, d);
            num[k] = n / g;
            den[k] = d / g;
        }

        long batches = 1;
        for (int k = 0; k < size; k++) batches = lcm(batches, den[k]);
        if (leftover != null) {
            long n = num[size - 1] * last.produced;
            long d = den[size - 1] * (long) leftover.deadEndVolume;
            batches = lcm(batches, d / gcd(n, d));
        }

        List<Step> steps = new ArrayList<>(size);
        List<ItemStack> outputs = new ArrayList<>();
        List<WeightedList<ItemStack>> alternatives = new ArrayList<>();
        boolean requiresUpgrade = false;

        for (int k = 0; k < size; k++) {
            long operations = batches * num[k] / den[k];
            GasCentrifugeRecipe recipe = chain.get(k);
            steps.add(new Step(recipe, operations));
            requiresUpgrade |= recipe.requiresUpgrade;
            collect(recipe.outputItems(), operations, outputs, alternatives);
        }

        Step deadEnd = null;
        if (leftover != null) {
            long runs =
                    (batches * num[size - 1] * last.produced / den[size - 1])
                            / leftover.deadEndVolume;
            deadEnd = new Step(leftover, runs);
            for (ItemStack stack : leftover.deadEndItems()) merge(outputs, stack, runs);
        }

        int volume = (int) (batches * chain.get(0).consumed);
        return new Cascade(
                chain.get(0).recipeId().location().withSuffix("/" + size),
                feed,
                volume,
                List.copyOf(steps),
                deadEnd,
                List.copyOf(outputs),
                List.copyOf(alternatives),
                requiresUpgrade);
    }

    private static void collect(
            WeightedList<ItemStack> @Nullable [] slots,
            long operations,
            List<ItemStack> outputs,
            List<WeightedList<ItemStack>> alternatives) {
        if (slots == null) return;
        for (WeightedList<ItemStack> slot : slots) {
            List<ItemStack> entries = new ArrayList<>();
            for (Weighted<ItemStack> entry : slot.unwrap()) {
                if (!entry.value().isEmpty()) entries.add(entry.value());
            }
            if (entries.isEmpty()) continue;
            if (entries.size() == 1) {
                merge(outputs, entries.get(0), operations);
            } else {
                alternatives.add(slot);
            }
        }
    }

    private static void merge(List<ItemStack> outputs, ItemStack stack, long runs) {
        int count = (int) Math.min(Integer.MAX_VALUE, stack.getCount() * runs);
        for (ItemStack held : outputs) {
            if (ItemStack.isSameItemSameComponents(held, stack)) {
                held.setCount(held.getCount() + count);
                return;
            }
        }
        ItemStack copy = stack.copy();
        copy.setCount(count);
        outputs.add(copy);
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a == 0 ? 1 : a;
    }

    private static long lcm(long a, long b) {
        return a / gcd(a, b) * b;
    }

    // main: battery packs, then single-cell batteries, then the creative one
    private static List<ItemStack> batteries() {
        List<ItemStack> batteries = new ArrayList<>();
        for (var pack : ModItems.BATTERY_PACKS) batteries.add(new ItemStack(pack));
        for (var battery : ModItems.BATTERIES)
            if (battery.get() instanceof ItemBatterySC) batteries.add(new ItemStack(battery));
        batteries.add(new ItemStack(ModItems.BATTERY_CREATIVE));
        return batteries;
    }

    // main: GasCentrifugeRecipeHandler, fluid left, two output columns right, battery bottom left
    @Override
    public void layout(Cascade cascade, PageLayout page) {
        page.input(47, 24).fluid(cascade.feed(), cascade.volume());
        // main: an "other stack" that only cycles, so batteries do not list the centrifuge as a use
        page.display(3, 42).items(batteries());

        int slot = 0;
        for (ItemStack output : cascade.outputs()) {
            page.output(129 + slot % 2 * 18, 15 + slot / 2 * 18).item(output);
            slot++;
        }
        for (WeightedList<ItemStack> alternative : cascade.alternatives()) {
            for (GenericMachinePage.Outcome outcome : GenericMachinePage.outcomes(alternative)) {
                GenericMachinePage.outcomeSlot(
                        page, 129 + slot % 2 * 18, 15 + slot / 2 * 18, outcome, slot >= 4);
                slot++;
            }
        }

    }

    @Override
    public void draw(Cascade cascade, GuiGraphicsExtractor graphics) {
        ResourceLocation sheet = RecipePanel.GAS_CENTRIFUGE;
        RecipePanel.page(graphics, sheet);
        if (cascade.requiresUpgrade()) RecipePanel.region(graphics, sheet, 25, 24, 192, 0, 16, 16);
        RecipePanel.progress(graphics, sheet, 3, 6, 176, 0, 16, 34, 480, 7);
        RecipePanel.progress(
                graphics, sheet, 74, 17, 208, 0, 44, 37, cascade.requiresUpgrade() ? 80 : 150, 0);
        RecipePanel.centred(
                graphics,
                Component.translatable("jei.hbm.gas_centrifuge.cascade", cascade.steps().size()),
                50,
                10,
                0xFF00FF00);
    }

    public record Step(GasCentrifugeRecipe recipe, long operations) {}

    public record Cascade(
            ResourceLocation id,
            Fluid feed,
            int volume,
            List<Step> steps,
            @Nullable Step deadEnd,
            List<ItemStack> outputs,
            List<WeightedList<ItemStack>> alternatives,
            boolean requiresUpgrade) implements RecipePage.Row {}
}
