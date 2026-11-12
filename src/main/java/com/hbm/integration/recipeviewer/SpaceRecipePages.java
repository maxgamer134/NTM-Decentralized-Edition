// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.integration.recipeviewer;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.rocket.ItemVOTVdrive;
import com.hbm.space.rocket.SpaceRockets;
import com.hbm.space.trait.CBT_Water;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * NTM:Space's per-body recipe viewer pages (its NEI fluid table and oil extraction handlers):
 * what a pump draws and what oil drills strike on each body. Added only with Space on.
 */
public final class SpaceRecipePages {

    private SpaceRecipePages() {}

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        RecipePages.addon(new VacuumCircuitPage());
        RecipePages.addon(new AlkylationPage());
        RecipePages.addon(new AtmospherePage());
        RecipePages.addon(new BodyPage("space_fluid_table", "Fluid Table", List.of(
                new ItemStack(ModBlocks.PUMP_ELECTRIC.get()), new ItemStack(ModBlocks.PUMP_STEAM.get()))) {
            @Override
            List<Out> outputs(SolarSystem.Body type, CelestialBody body) {
                CBT_Water table = body.getDefaultTrait(CBT_Water.class);
                if (table == null || table.fluid == null) return List.of();
                return List.of(new Out(table.fluid, table.fluid == NTMFluids.WATER ? 10_000 : 1_000));
            }
        });
        RecipePages.addon(new BodyPage("space_oil_extraction", "Oil Extraction", List.of(
                new ItemStack(ModBlocks.MACHINE_WELL.get()), new ItemStack(ModBlocks.MACHINE_PUMPJACK.get()),
                new ItemStack(ModBlocks.MACHINE_FRACKING_TOWER.get()))) {
            @Override
            List<Out> outputs(SolarSystem.Body type, CelestialBody body) {
                return switch (type) {
                    case KERBIN, DUNA -> List.of(out("OIL", 1000), out("GAS", 200));
                    case EVE -> List.of(out("GAS", 1000), out("PETROLEUM", 200));
                    case LAYTHE -> List.of(out("OIL_DS", 1000), out("GAS", 200));
                    case TEKTO -> List.of(out("TCRUDE", 1000), out("HGAS", 200));
                    case MUN, MINMUS, IKE -> List.of(out("BRINE", 1000));
                    default -> List.of();
                };
            }
        });
    }

    public record AlkylationRow(ResourceLocation id, com.hbm.space.machine.SpaceAlkylation.Recipe recipe) implements RecipePage.Row {}

    /** NTM:Space's alkylation handler: the input (and reagent) per 1,000 mB, and what comes out. */
    static final class AlkylationPage extends RecipePage<AlkylationRow> {
        AlkylationPage() {
            super(PageIds.page("alkylation"), AlkylationRow.class);
        }

        @Override
        public Component title() {
            return Component.translatable("block.hbm.machine_alkylation");
        }

        @Override
        public ItemStack icon() {
            return new ItemStack(com.hbm.space.machine.SpaceAlkylation.BLOCK.get());
        }

        @Override
        public List<ItemStack> catalysts() {
            return List.of(icon());
        }

        @Override
        public List<AlkylationRow> rows() {
            List<AlkylationRow> rows = new ArrayList<>();
            for (var recipe : com.hbm.space.machine.SpaceAlkylation.recipes())
                rows.add(new AlkylationRow(PageIds.derived(id(), recipe.input().toLowerCase(java.util.Locale.ROOT)), recipe));
            return rows;
        }

        @Override
        public void layout(AlkylationRow row, PageLayout page) {
            var r = row.recipe();
            page.input(30, 24).background().fluid(NTMFluids.byName(r.input()), 1000);
            if (r.reagent() != null) page.input(48, 24).background().fluid(NTMFluids.byName(r.reagent()), r.reagentAmount() * 10);
            page.output(102, 24).background().fluid(NTMFluids.byName(r.product()), r.productAmount() * 10);
            page.output(120, 24).background().fluid(NTMFluids.byName(r.byproduct()), r.byproductAmount() * 10);
            page.catalyst(75, 31).item(com.hbm.space.machine.SpaceAlkylation.BLOCK.get());
        }

        @Override
        public void draw(AlkylationRow row, GuiGraphicsExtractor graphics) {
            RecipePanel.universalPage(graphics);
        }
    }

    public record AtmoRow(ResourceLocation id, String output, int outAmount, String inA, int amountA, String inB, int amountB) implements RecipePage.Row {}

    /** NTM:Space's "Atmospheric Chemistry" handler (AtmosphereRecipes): what the atmospheric emitter's chemistry turns into. */
    static final class AtmospherePage extends RecipePage<AtmoRow> {
        AtmospherePage() {
            super(PageIds.page("atmospheric_chemistry"), AtmoRow.class);
        }

        @Override
        public Component title() {
            return Component.translatable("jei.hbm.atmospheric_chemistry");
        }

        @Override
        public ItemStack icon() {
            return new ItemStack(com.hbm.space.machine.SpaceAtmoMachines.EMITTER.get());
        }

        @Override
        public List<ItemStack> catalysts() {
            return List.of(icon());
        }

        @Override
        public List<AtmoRow> rows() {
            // upstream processes about 64x this per operation
            return List.of(new AtmoRow(PageIds.derived(id(), "lithcarbonate"), "LITHCARBONATE", 32_000, "LITHYDRO", 16_000, "DUNAAIR", 256_000));
        }

        @Override
        public void layout(AtmoRow row, PageLayout page) {
            page.input(30, 24).background().fluid(NTMFluids.byName(row.inA()), row.amountA());
            page.input(48, 24).background().fluid(NTMFluids.byName(row.inB()), row.amountB());
            page.output(102, 24).background().fluid(NTMFluids.byName(row.output()), row.outAmount());
            page.catalyst(75, 31).item(com.hbm.space.machine.SpaceAtmoMachines.EMITTER.get());
        }

        @Override
        public void draw(AtmoRow row, GuiGraphicsExtractor graphics) {
            RecipePanel.universalPage(graphics);
        }
    }

    public record VacuumRow(ResourceLocation id, com.hbm.space.machine.SpaceVacuumCircuit.Recipe recipe) implements RecipePage.Row {}

    /** The vacuum solderer's recipes, laid out like the soldering station's. */
    static final class VacuumCircuitPage extends RecipePage<VacuumRow> {
        VacuumCircuitPage() {
            super(PageIds.page("vacuum_circuit"), VacuumRow.class);
        }

        @Override
        public Component title() {
            return Component.translatable("block.hbm.machine_vacuum_circuit");
        }

        @Override
        public ItemStack icon() {
            return new ItemStack(com.hbm.space.machine.SpaceVacuumCircuit.BLOCK.get());
        }

        @Override
        public List<ItemStack> catalysts() {
            return List.of(icon());
        }

        @Override
        public List<VacuumRow> rows() {
            List<VacuumRow> rows = new ArrayList<>();
            for (var recipe : com.hbm.space.machine.SpaceVacuumCircuit.recipes()) {
                ResourceLocation out = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(recipe.output().get().getItem());
                rows.add(new VacuumRow(PageIds.derived(id(), out.getPath()), recipe));
            }
            return rows;
        }

        @Override
        public void layout(VacuumRow row, PageLayout page) {
            var recipe = row.recipe();
            int[][] positions = GenericMachinePage.universalInputPositions(recipe.wafer().length + recipe.pcb().length);
            int slot = 0;
            for (var group : List.of(recipe.wafer(), recipe.pcb()))
                for (var input : group) {
                    page.input(positions[slot][0], positions[slot][1]).background().items(input.displayStacks());
                    slot++;
                }
            page.output(102, 24).background().item(recipe.output().get());
            page.catalyst(75, 31).item(com.hbm.space.machine.SpaceVacuumCircuit.BLOCK.get());
        }

        @Override
        public void draw(VacuumRow row, GuiGraphicsExtractor graphics) {
            RecipePanel.universalPage(graphics);
            RecipePanel.rightAligned(graphics, Component.translatable("jei.hbm.duration",
                    String.format(java.util.Locale.US, "%,d", row.recipe().duration())), 160, 43, RecipePanel.TEXT);
            RecipePanel.rightAligned(graphics, Component.translatable("jei.hbm.consumption_padded",
                    String.format(java.util.Locale.US, "%,d", row.recipe().consumption())), 160, 55, RecipePanel.TEXT);
        }
    }

    record Out(@org.jspecify.annotations.Nullable Fluid fluid, int amount) {}

    private static Out out(String fluid, int amount) {
        try {
            return new Out(NTMFluids.byName(fluid), amount);
        } catch (IllegalStateException missing) {
            return new Out(null, amount); // left out of the page
        }
    }

    public record BodyRow(ResourceLocation id, CelestialBody body, ItemStack drive, List<Out> outputs) implements RecipePage.Row {}

    abstract static class BodyPage extends RecipePage<BodyRow> {
        private final Component title;
        private final List<ItemStack> machines;

        BodyPage(String id, String title, List<ItemStack> machines) {
            super(PageIds.page(id), BodyRow.class);
            this.title = Component.literal(title);
            this.machines = machines;
        }

        abstract List<Out> outputs(SolarSystem.Body type, CelestialBody body);

        @Override
        public Component title() {
            return title;
        }

        @Override
        public ItemStack icon() {
            return machines.getFirst();
        }

        @Override
        public List<ItemStack> catalysts() {
            return machines;
        }

        @Override
        public List<BodyRow> rows() {
            List<BodyRow> rows = new ArrayList<>();
            for (SolarSystem.Body type : SolarSystem.Body.values()) {
                if (type == SolarSystem.Body.ORBIT) continue;
                CelestialBody body = type.getBody();
                if (body == null) continue;
                List<Out> outputs = new ArrayList<>();
                for (Out out : outputs(type, body)) if (out.fluid() != null && out.fluid() != NTMFluids.NONE) outputs.add(out);
                if (outputs.isEmpty()) continue;
                rows.add(new BodyRow(PageIds.derived(id(), PageIds.segment(type)), body,
                        ItemVOTVdrive.processed(SpaceRockets.DRIVES.get(type).get()), outputs));
            }
            return rows;
        }

        @Override
        public void layout(BodyRow row, PageLayout page) {
            page.input(48, 24).background().item(row.drive());
            for (int i = 0; i < row.outputs().size(); i++) {
                Out out = row.outputs().get(i);
                page.output(102 + i * 18, 24).background().fluid(out.fluid(), out.amount());
            }
        }

        @Override
        public void draw(BodyRow row, GuiGraphicsExtractor graphics) {
            RecipePanel.universalPage(graphics);
            Component name = Component.translatable("body." + row.body().name);
            RecipePanel.leftAligned(graphics, name, 57 - RecipePanel.font().width(name) / 2, 46, RecipePanel.TEXT);
        }
    }
}
