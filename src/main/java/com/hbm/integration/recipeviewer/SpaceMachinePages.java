// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.integration.recipeviewer;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.blocks.ModBlocks;
import com.hbm.blockentity.machine.BlockEntityDeuteriumExtractor;
import com.hbm.integration.recipeviewer.GenericMachinePage.Extras;
import com.hbm.integration.recipeviewer.GenericMachinePage.FluidView;
import com.hbm.integration.recipeviewer.GenericMachinePage.Layout;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Rocket;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.CryoDistillRecipe;
import com.hbm.inventory.recipes.CryoDistillRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemScraps;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.client.ScreenCryoDistiller;
import com.hbm.space.client.ScreenHydroponic;
import com.hbm.space.client.ScreenMilkReformer;
import com.hbm.space.machine.SpaceAtmoMachines;
import com.hbm.space.machine.SpaceAtmoTower;
import com.hbm.space.machine.SpaceCryoDistiller;
import com.hbm.space.machine.SpaceGasDock;
import com.hbm.space.machine.SpaceHTR3;
import com.hbm.space.machine.SpaceHydroponic;
import com.hbm.space.machine.SpaceMagma;
import com.hbm.space.machine.SpaceMilkReformer;
import com.hbm.space.machine.SpaceXenonThruster;
import com.hbm.space.rocket.ItemVOTVdrive;
import com.hbm.space.rocket.SpaceRockets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * Recipe viewer pages for NTM:Space's machines that upstream had no NEI handler for (and its
 * cryogenic distillation and dairy handlers), with the click areas on their screens. Everything
 * that touches a registry is read when the pages are built or their rows asked for, never at
 * class init.
 */
public final class SpaceMachinePages {

    private SpaceMachinePages() {}

    static void register() {
        // NTM:Space's CryoHandler: 1,000 mB of gas in, each fraction times ten out, as upstream showed it
        RecipePage<?> cryo = GenericMachinePage.builder("cryo_distiller", CryoDistillRecipe.class, CryoDistillRecipes.INSTANCE,
                        "jei.hbm.space.cryo_distiller", SpaceCryoDistiller.BLOCK.get(), Layout.UNIVERSAL)
                .extras(Extras.NONE)
                .fluidView(FluidView.batch(1000, 10))
                .build();
        RecipePages.addon(cryo);
        RecipePages.addonClickArea(ScreenCryoDistiller.class, 80, 72, 70, 16, cryo);

        // NTM:Space's DairyHandler
        SimplePage dairy = new SimplePage("dairy", "jei.hbm.space.dairy", () -> List.of(new ItemStack(SpaceMilkReformer.BLOCK.get())),
                page -> List.of(new SimpleRow(PageIds.derived(page, "milk"),
                        List.of(Ing.fluid("MILK", 100)),
                        List.of(Ing.fluid("EMILK", 50), Ing.fluid("CMILK", 35), Ing.fluid("CREAM", 15)),
                        List.of(), List.of(perOperation(10_000)))));
        RecipePages.addon(dairy);
        RecipePages.addonClickArea(ScreenMilkReformer.class, 72, 28, 86, 6, dairy);

        // the atmosphere tower: the deuterium tower's process on air
        RecipePages.addon(new SimplePage("atmosphere_tower", "block.hbm.machine_atmo_tower",
                () -> List.of(new ItemStack(SpaceAtmoTower.BLOCK.get())),
                page -> List.of(new SimpleRow(PageIds.derived(page, "earthair"),
                        List.of(Ing.fluid("EARTHAIR", 1_000)),
                        List.of(Ing.fluid("NITROGEN", 1_000 / BlockEntityDeuteriumExtractor.RATIO)),
                        List.of(), List.of()))));

        // the algae film: carbon dioxide into oxygen, a millibucket at a time, only in orbit
        RecipePages.addon(new SimplePage("algae_film", "block.hbm.algae_film",
                () -> List.of(new ItemStack(SpaceAtmoMachines.ALGAE.get())),
                page -> List.of(new SimpleRow(PageIds.derived(page, "carbondioxide"),
                        List.of(Ing.fluid("CARBONDIOXIDE", 1)),
                        List.of(Ing.fluid("OXYGEN", 1)),
                        List.of(), List.of(Component.translatable("jei.hbm.space.orbit_only"))))));

        // the hydroponic bay: each growth stage of a crop turns CO2 into as much oxygen
        SimplePage hydroponic = new SimplePage("hydroponic_bay", "block.hbm.hydrobay",
                () -> List.of(new ItemStack(SpaceHydroponic.BLOCK.get())),
                page -> List.of(new SimpleRow(PageIds.derived(page, "growth"),
                        List.of(Ing.items(hydroponicSeeds()),
                                Ing.items(List.of(new ItemStack(Items.BONE_MEAL), new ItemStack(ModItems.POWDER_FERTILIZER.get()))),
                                Ing.fluid("CARBONDIOXIDE", com.hbm.space.atmosphere.SpaceAtmosphere.CROP_GROWTH_CONVERSION)),
                        List.of(Ing.fluid("OXYGEN", com.hbm.space.atmosphere.SpaceAtmosphere.CROP_GROWTH_CONVERSION)),
                        List.of(), List.of(Component.translatable("jei.hbm.space.per_growth_stage"), perTick(25)))));
        RecipePages.addon(hydroponic);
        RecipePages.addonClickArea(ScreenHydroponic.class, 86, 21, 22, 8, hydroponic);

        // the gas dock: a rocket flown on half a bucket each of hydrogen and oxygen brings back 8 B of the giant's gas
        RecipePages.addon(new SimplePage("gas_dock", "block.hbm.gas_dock",
                () -> List.of(new ItemStack(SpaceGasDock.BLOCK.get())),
                page -> {
                    List<SimpleRow> rows = new ArrayList<>();
                    for (SolarSystem.Body type : SolarSystem.Body.values()) {
                        CelestialBody body = type.getBody();
                        if (body == null || body.gas == null || body.gas == NTMFluids.NONE) continue;
                        List<Ing> hidden = new ArrayList<>();
                        ItemStack drive = drive(type);
                        if (!drive.isEmpty()) hidden.add(Ing.item(drive));
                        rows.add(new SimpleRow(PageIds.derived(page, PageIds.segment(type)),
                                List.of(Ing.fluid("HYDROGEN", 500), Ing.fluid("OXYGEN", 500)),
                                List.of(new Ing(List.of(), body.gas, 8_000)),
                                hidden, List.of(Component.translatable("body." + body.name))));
                    }
                    return rows;
                }));

        // the magma drill: over lava on Moho, a slag ingot and four quanta of rich magma every ten ticks
        RecipePages.addon(new SimplePage("magma_drill", "block.hbm.machine_magma",
                () -> List.of(new ItemStack(SpaceMagma.BLOCK.get())),
                page -> {
                    List<Ing> hidden = new ArrayList<>();
                    ItemStack drive = drive(SolarSystem.Body.MOHO);
                    if (!drive.isEmpty()) hidden.add(Ing.item(drive));
                    return List.of(new SimpleRow(PageIds.derived(page, "moho"),
                            List.of(new Ing(List.of(), NTMFluids.LAVA, 0)),
                            List.of(Ing.item(ItemScraps.create(new MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.q(1)), true)),
                                    Ing.item(ItemScraps.create(new MaterialStack(SpaceMagma.MAT_RICH_MAGMA, MaterialShapes.QUANTUM.q(4)), true))),
                            hidden,
                            List.of(Component.translatable("jei.hbm.space.only_on", Component.translatable("body.moho")),
                                    perTick(SpaceMagma.CONSUMPTION))));
                }));

        // the station thrusters and the propellants they burn, with the ISP the burn's fuel cost is worked out from
        RecipePages.addon(new ThrusterPage());
        RecipePages.addon(new PWRHeatingPage());
        RecipePages.addon(new FusionPlasmaPage());
    }

    private static Component perOperation(long he) {
        return Component.translatable("jei.hbm.space.he_per_operation", String.format(Locale.US, "%,d", he));
    }

    private static Component perTick(long he) {
        return Component.translatable("jei.hbm.consumption_padded", String.format(Locale.US, "%,d", he));
    }

    private static ItemStack drive(SolarSystem.Body type) {
        var drive = SpaceRockets.DRIVES.get(type);
        return drive == null ? ItemStack.EMPTY : ItemVOTVdrive.processed(drive.get());
    }

    /** What the hydroponic bay takes in its seed slot: any crop's item but a stem's, and of Space's crops only the hydroponic ones. */
    private static List<ItemStack> hydroponicSeeds() {
        List<ItemStack> seeds = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem blockItem)) continue;
            var block = blockItem.getBlock();
            if (!(block instanceof CropBlock) || block instanceof StemBlock) continue;
            if (block instanceof com.hbm.space.SpaceCrops.Crop crop && !crop.hydroponic) continue;
            seeds.add(new ItemStack(item));
        }
        return seeds;
    }

    /** An ingredient: items to cycle through, or a fluid (an amount of 0 shows the fluid alone). */
    record Ing(List<ItemStack> items, @Nullable Fluid fluid, int amount) {
        static Ing item(ItemStack stack) {
            return new Ing(List.of(stack), null, 0);
        }

        static Ing items(List<ItemStack> stacks) {
            return new Ing(stacks, null, 0);
        }

        static Ing fluid(String name, int amount) {
            return new Ing(List.of(), NTMFluids.byName(name), amount);
        }

        void fill(PageSlot slot) {
            if (fluid != null) {
                if (amount > 0) slot.fluid(fluid, amount);
                else slot.fluid(fluid);
            } else {
                slot.items(items);
            }
        }
    }

    public record SimpleRow(ResourceLocation id, List<Ing> inputs, List<Ing> outputs, List<Ing> hidden, List<Component> lines)
            implements RecipePage.Row {}

    /** A universal page for a machine's hardcoded process: inputs, the machine, outputs and a few lines of text. */
    static final class SimplePage extends RecipePage<SimpleRow> {
        private final Component title;
        private final Supplier<List<ItemStack>> machines;
        private final java.util.function.Function<ResourceLocation, List<SimpleRow>> rows;

        SimplePage(String id, String titleKey, Supplier<List<ItemStack>> machines,
                java.util.function.Function<ResourceLocation, List<SimpleRow>> rows) {
            super(PageIds.page(id), SimpleRow.class);
            this.title = Component.translatable(titleKey);
            this.machines = machines;
            this.rows = rows;
        }

        @Override
        public Component title() {
            return title;
        }

        @Override
        public ItemStack icon() {
            return machines.get().getFirst();
        }

        @Override
        public List<ItemStack> catalysts() {
            return machines.get();
        }

        @Override
        public List<SimpleRow> rows() {
            return rows.apply(id());
        }

        @Override
        public void layout(SimpleRow row, PageLayout page) {
            int[][] in = GenericMachinePage.universalInputPositions(row.inputs().size());
            for (int i = 0; i < row.inputs().size(); i++) row.inputs().get(i).fill(page.input(in[i][0], in[i][1]).background());
            int[][] out = GenericMachinePage.outputPositions(row.outputs().size());
            for (int i = 0; i < row.outputs().size(); i++) row.outputs().get(i).fill(page.output(out[i][0], out[i][1]).background());
            for (Ing hidden : row.hidden()) hidden.fill(page.hiddenInput());
            page.catalyst(75, 31).items(machines.get());
        }

        @Override
        public void draw(SimpleRow row, GuiGraphicsExtractor graphics) {
            RecipePanel.universalPage(graphics);
            drawLines(graphics, row.lines());
        }
    }

    /** Lines right-aligned at the bottom of the page, the last on the bottom line, as upstream's duration and consumption were. */
    static void drawLines(GuiGraphicsExtractor graphics, List<Component> lines) {
        for (int i = 0; i < lines.size(); i++)
            RecipePanel.rightAligned(graphics, lines.get(i), 160, 55 - 12 * (lines.size() - 1 - i), RecipePanel.TEXT);
    }

    /** {@code source} is a machine feeding the thruster (the HTR-F4's fusion reactor), {@code hePerBurn} its charge. */
    public record ThrusterRow(ResourceLocation id, ItemStack thruster, List<Fluid> propellants, int isp, long hePerMb,
            ItemStack source, long hePerBurn) implements RecipePage.Row {
        ThrusterRow(ResourceLocation id, ItemStack thruster, List<Fluid> propellants, int isp, long hePerMb) {
            this(id, thruster, propellants, isp, hePerMb, ItemStack.EMPTY, 0L);
        }
    }

    /**
     * The station thrusters' propellants: the xenon thruster's xenon (and 5,000 HE for every
     * millibucket), the LPW-2's reformed kerosene and oxygen, and every heated fluid the LPW-3N can
     * be set to burn. A burn's fuel cost is worked out from the propellant's ISP (300 without one).
     */
    static final class ThrusterPage extends RecipePage<ThrusterRow> {
        private static final int DEFAULT_ISP = 300;

        ThrusterPage() {
            super(PageIds.page("station_thrusters"), ThrusterRow.class);
        }

        @Override
        public Component title() {
            return Component.translatable("jei.hbm.space.station_thrusters");
        }

        @Override
        public ItemStack icon() {
            return new ItemStack(SpaceXenonThruster.BLOCK.get());
        }

        @Override
        public List<ItemStack> catalysts() {
            return List.of(new ItemStack(SpaceXenonThruster.BLOCK.get()), new ItemStack(ModBlocks.MACHINE_LPW2.get()),
                    new ItemStack(SpaceHTR3.BLOCK.get()), new ItemStack(com.hbm.space.machine.SpaceHTRF.BLOCK.get()));
        }

        private static @Nullable Fluid firstStepProduct(FT_Heatable heatable) {
            try {
                return heatable.getFirstStep().typeProduced();
            } catch (IndexOutOfBoundsException noSteps) {
                return null;
            }
        }

        private static int isp(@Nullable Fluid fluid) {
            FT_Rocket rocket = NTMFluidProperties.getTrait(fluid, FT_Rocket.class);
            return rocket != null ? rocket.getISP() : DEFAULT_ISP;
        }

        @Override
        public List<ThrusterRow> rows() {
            List<ThrusterRow> rows = new ArrayList<>();
            Fluid xenon = NTMFluids.byName("XENON");
            rows.add(new ThrusterRow(PageIds.derived(id(), "xenon_thruster"), new ItemStack(SpaceXenonThruster.BLOCK.get()),
                    List.of(xenon), isp(xenon), 5_000L));
            Fluid kerosene = NTMFluids.byName("KEROSENE_REFORM");
            rows.add(new ThrusterRow(PageIds.derived(id(), "lpw2"), new ItemStack(ModBlocks.MACHINE_LPW2.get()),
                    List.of(kerosene, NTMFluids.byName("OXYGEN")), isp(kerosene), 0L));
            // the LPW-3N burns what a PWR heats a fluid into, when that can fly (the PWR Heating page
            // shows the heating): found from the cold side (a PWR-heatable fluid) and from the hot side
            // (a rocket fluid that cools back down)
            Set<Fluid> heated = new LinkedHashSet<>();
            for (PWRHeatingRow heating : PWRHeatingPage.heatings(null))
                if (NTMFluidProperties.hasTrait(heating.hot(), FT_Rocket.class)) heated.add(heating.hot());
            for (Fluid fluid : NTMFluids.displayOrder()) {
                FT_Coolable coolable = NTMFluidProperties.getTrait(fluid, FT_Coolable.class);
                if (coolable != null && coolable.coolsTo() != null && NTMFluidProperties.hasTrait(fluid, FT_Rocket.class))
                    heated.add(fluid);
            }
            for (Fluid hot : heated) {
                rows.add(new ThrusterRow(PageIds.derived(id(), "htr3", PageIds.segment(BuiltInRegistries.FLUID.getKey(hot))),
                        new ItemStack(SpaceHTR3.BLOCK.get()), List.of(hot), isp(hot), 0L));
            }
            // the HTR-F4 NEO burns a fusion reactor's plasma at that plasma's ISP, charged with 200M HE
            // and cooled by perfluoromethyl
            ItemStack neo = new ItemStack(com.hbm.space.machine.SpaceHTRF.BLOCK.get());
            for (Fluid plasma : com.hbm.space.FusionPlasmas.all()) {
                rows.add(new ThrusterRow(PageIds.derived(id(), "htrf4", PageIds.segment(BuiltInRegistries.FLUID.getKey(plasma))),
                        neo, List.of(plasma, NTMFluids.PERFLUOROMETHYL_COLD), isp(plasma), 0L, ItemStack.EMPTY,
                        com.hbm.space.machine.SpaceHTRF.HTRFBE.MAX_POWER));
            }
            return rows;
        }

        @Override
        public void layout(ThrusterRow row, PageLayout page) {
            int count = row.propellants().size() + (row.source().isEmpty() ? 0 : 1);
            int[][] in = GenericMachinePage.universalInputPositions(count);
            for (int i = 0; i < row.propellants().size(); i++)
                page.input(in[i][0], in[i][1]).background().fluid(row.propellants().get(i));
            if (!row.source().isEmpty()) page.input(in[count - 1][0], in[count - 1][1]).background().item(row.source());
            page.display(102, 24).background().item(row.thruster());
        }

        @Override
        public void draw(ThrusterRow row, GuiGraphicsExtractor graphics) {
            RecipePanel.universalPage(graphics);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("jei.hbm.space.isp", row.isp()));
            if (row.hePerMb() > 0) lines.add(Component.translatable("jei.hbm.space.he_per_mb", String.format(Locale.US, "%,d", row.hePerMb())));
            if (row.hePerBurn() > 0) lines.add(Component.translatable("jei.hbm.space.he_per_burn", String.format(Locale.US, "%,d", row.hePerBurn())));
            drawLines(graphics, lines);
        }
    }

    public record PWRHeatingRow(ResourceLocation id, Fluid cold, int amountReq, Fluid hot, int amountProduced, int heatReq)
            implements RecipePage.Row {}

    /**
     * What a PWR heats its coolant into: every fluid a PWR can heat (hydrogen into superheated
     * hydrogen for the LPW-3N, coolant into hot coolant), with a greyed-out fuel rod for the reactor
     * doing the heating.
     */
    static final class PWRHeatingPage extends RecipePage<PWRHeatingRow> {
        private static final int ROD_X = 75, ROD_Y = 31;

        PWRHeatingPage() {
            super(PageIds.page("pwr_heating"), PWRHeatingRow.class);
        }

        static List<PWRHeatingRow> heatings(@Nullable ResourceLocation page) {
            List<PWRHeatingRow> rows = new ArrayList<>();
            for (Fluid cold : NTMFluids.displayOrder()) {
                FT_Heatable heatable = NTMFluidProperties.getTrait(cold, FT_Heatable.class);
                if (heatable == null || heatable.getEfficiency(FT_Heatable.HeatingType.PWR) <= 0) continue;
                FT_Heatable.HeatingStep step;
                try {
                    step = heatable.getFirstStep();
                } catch (IndexOutOfBoundsException noSteps) {
                    continue;
                }
                Fluid hot = step.typeProduced();
                if (hot == null) continue;
                ResourceLocation id = page == null ? null
                        : PageIds.derived(page, PageIds.segment(BuiltInRegistries.FLUID.getKey(cold)));
                rows.add(new PWRHeatingRow(id, cold, step.amountReq, hot, step.amountProduced, step.heatReq));
            }
            return rows;
        }

        @Override
        public Component title() {
            return Component.translatable("jei.hbm.space.pwr_heating");
        }

        @Override
        public ItemStack icon() {
            return new ItemStack(ModBlocks.PWR_CONTROLLER.get());
        }

        @Override
        public List<ItemStack> catalysts() {
            return List.of(new ItemStack(ModBlocks.PWR_CONTROLLER.get()));
        }

        @Override
        public List<PWRHeatingRow> rows() {
            return heatings(id());
        }

        @Override
        public void layout(PWRHeatingRow row, PageLayout page) {
            page.input(48, 24).background().fluid(row.cold());
            page.output(102, 24).background().fluid(row.hot());
        }

        @Override
        public void draw(PWRHeatingRow row, GuiGraphicsExtractor graphics) {
            RecipePanel.universalPage(graphics);
            // the rod greyed out under a translucent wash: the reactor is the process, not an ingredient
            graphics.fakeItem(new ItemStack(ModItems.cold(com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel.VALUES[0])),
                    ROD_X, ROD_Y);
            graphics.pose().pushPose();
            graphics.pose().translate(0F, 0F, 300F);
            graphics.fill(ROD_X, ROD_Y, ROD_X + 16, ROD_Y + 16, 0xB0C6C6C6);
            graphics.pose().popPose();
            drawLines(graphics, List.of(Component.translatable("jei.hbm.space.pwr_heat",
                    String.format(Locale.US, "%,d", row.heatReq()), row.amountReq(), row.amountProduced())));
        }
    }

    /** {@code plasma} is null for a recipe making plasma energy only, drawn as a swatch in the recipe's colour. */
    public record FusionPlasmaRow(ResourceLocation id, List<com.hbm.inventory.fluid.FluidStackNTM> fuels,
            @Nullable Fluid plasma, long tuPerTick, int color) implements RecipePage.Row {}

    /**
     * What each fusion recipe's plasma is: its fuels in, the plasma fluid out (or the plasma energy
     * alone), with the TU it puts out each tick. The fusion reactor makes it, the HTR-F4 NEO burns it.
     */
    static final class FusionPlasmaPage extends RecipePage<FusionPlasmaRow> {
        FusionPlasmaPage() {
            super(PageIds.page("fusion_plasma"), FusionPlasmaRow.class);
        }

        @Override
        public Component title() {
            return Component.translatable("jei.hbm.space.fusion_plasma");
        }

        @Override
        public ItemStack icon() {
            return new ItemStack(ModBlocks.FUSION_TORUS.get());
        }

        @Override
        public List<ItemStack> catalysts() {
            return List.of(new ItemStack(ModBlocks.FUSION_TORUS.get()), new ItemStack(com.hbm.space.machine.SpaceHTRF.BLOCK.get()));
        }

        @Override
        public List<FusionPlasmaRow> rows() {
            List<FusionPlasmaRow> rows = new ArrayList<>();
            for (com.hbm.inventory.recipes.FusionRecipe recipe : com.hbm.inventory.recipes.FusionRecipes.INSTANCE.recipes()) {
                if (recipe.inputFluid.length == 0) continue;
                int color = 0xFF000000 | Math.round(recipe.r * 255) << 16 | Math.round(recipe.g * 255) << 8 | Math.round(recipe.b * 255);
                rows.add(new FusionPlasmaRow(PageIds.derived(id(), recipe.getInternalName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_")),
                        List.of(recipe.inputFluid), com.hbm.space.FusionPlasmas.of(recipe), recipe.outputTemp, color));
            }
            return rows;
        }

        @Override
        public void layout(FusionPlasmaRow row, PageLayout page) {
            int[][] in = GenericMachinePage.universalInputPositions(row.fuels().size());
            for (int i = 0; i < row.fuels().size(); i++) page.input(in[i][0], in[i][1]).background().fluid(row.fuels().get(i));
            if (row.plasma() != null) page.output(102, 24).background().fluid(row.plasma());
            page.catalyst(75, 31).item(ModBlocks.FUSION_TORUS.get());
        }

        @Override
        public void draw(FusionPlasmaRow row, GuiGraphicsExtractor graphics) {
            RecipePanel.universalPage(graphics);
            if (row.plasma() == null) {
                // plasma energy with no fluid of its own: a swatch in the recipe's plasma colour
                graphics.fill(102, 24, 120, 42, 0xFF373737);
                graphics.fill(103, 25, 119, 41, row.color());
            }
            drawLines(graphics, List.of(Component.translatable("jei.hbm.space.plasma_tu",
                    com.hbm.util.BobMathUtil.getShortNumber(row.tuPerTick()))));
        }
    }
}
