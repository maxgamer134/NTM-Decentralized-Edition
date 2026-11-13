// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.backport.InteractionResult;
import com.hbm.backport.TooltipDisplay;
import com.hbm.backport.compat.ItemCompat;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BlockBobble.Bobble;
import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.handler.ArmorModHandler;
import com.hbm.interfaces.HalfLifeType;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModDataComponents;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.block.ItemBlockTrinket;
import com.hbm.items.machine.DepletedRTGMaterial;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.items.machine.ItemRTGPelletDepleted;
import com.hbm.items.machine.ItemWatzPellet;
import com.hbm.items.machine.ItemWatzPellet.WatzFuel;
import com.hbm.items.special.ItemWasteShort;
import com.hbm.lib.Library;
import com.hbm.registration.ItemFamily;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.space.atmosphere.SpaceEnvironment;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.util.RTGUtil;
import com.hbm.util.function.Function;
import com.hbm.util.function.Function.FunctionLinear;
import com.hbm.util.function.Function.FunctionSqrt;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space's remaining crafting materials: the americium, berkelium and curium wastes, the transuranic
 * Watz pellets, the berkelium RTG pellet, the PLSS oxygen tank and its refill, conglomerate and Gwen's
 * bobblehead.
 */
public final class SpaceExtras {

    private SpaceExtras() {}

    /** Space's extra short-lived waste classes (upstream ItemWasteShort.WasteClass). */
    public enum SpaceWaste implements ItemWasteShort.Kind {
        AMERICIUM241("Americium-241"),
        BERKELIUM247("Berkelium-247"),
        CURIUM244("Curium-244"),
        CURIUM245("Curium-245");

        public final String label;

        SpaceWaste(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }
    }

    public static final ItemFamily<SpaceWaste, ItemWasteShort> WASTE_SHORT = waste("nuclear_waste_short");
    public static final ItemFamily<SpaceWaste, ItemWasteShort> WASTE_SHORT_TINY = waste("nuclear_waste_short_tiny");
    public static final ItemFamily<SpaceWaste, ItemWasteShort> WASTE_SHORT_DEPLETED = waste("nuclear_waste_short_depleted");
    public static final ItemFamily<SpaceWaste, ItemWasteShort> WASTE_SHORT_DEPLETED_TINY = waste("nuclear_waste_short_depleted_tiny");

    private static ItemFamily<SpaceWaste, ItemWasteShort> waste(String prefix) {
        return Reg.family(SpaceWaste.values(), type -> prefix + "_" + type.name().toLowerCase(Locale.ROOT),
                (props, type) -> new ItemWasteShort(props, type), Item.Properties::new);
    }

    /** Space's transuranic Watz fuels (upstream EnumWatzType PU241 to ES253), at upstream Space's 1e9 yield. */
    public enum SpaceWatz implements WatzFuel {
        PU241(0x78817E, 394240, 1_950, 25, 0.0025D, new FunctionLinear(1.30D), new FunctionSqrt(2.66D / 18D).withOff(24D * 24D)),
        AMRG(0x93767B, 0x66474D, 2_888, 48, 0.0035D, new FunctionLinear(1.33D), new FunctionSqrt(4.33D / 25.5D).withOff(28D * 28D)),
        AMF(0x93767B, 0x66474D, 2_333, 44, 0.003D, new FunctionLinear(1.33D), new FunctionSqrt(4.11D / 22.2D).withOff(27D * 27D)),
        CMRG(0xD8C2C4, 0xAD9799, 2_999, 50, 0.005D, new FunctionLinear(1.5D), new FunctionSqrt(5.5D / 25.5D).withOff(30D * 28D)),
        CMF(0xD8C2C4, 0xAD9799, 2_444, 48, 0.0045D, new FunctionLinear(1.8D), new FunctionSqrt(5.0D / 20D).withOff(26D * 24D)),
        BK247(0xC2C9C7, 0x8D9592, 3_000, 55, 0.012D, new FunctionLinear(1.5D), new FunctionSqrt(6.0D / 23.5D).withOff(10D * 10D)),
        CF251(0x7879B4, 0x4D4E89, 1_250, 60, 0.001D, new FunctionLinear(1.7D), new FunctionSqrt(6.65D / 23.5D).withOff(10D * 10D)),
        CF252(0x7879B4, 0x4D4E89, 1_050, 120, 0.0015D, new FunctionLinear(1.8D), new FunctionSqrt(8.85D / 28.8D).withOff(10D * 10D)),
        ES253(0xB9BFB2, 0x594E44, 3_750, 70, 0.0001D, new FunctionLinear(1.3D), new FunctionSqrt(7.0D / 27.7D).withOff(10D * 10D));

        private final int colorLight;
        private final int colorDark;
        private final double passive;
        private final double heatEmission;
        private final double mudContent;
        private final Function burnFunc;
        private final Function heatDiv;

        SpaceWatz(int colorLight, int colorDark, double passive, double heatEmission, double mudContent, Function burnFunc, Function heatDiv) {
            this.colorLight = colorLight;
            this.colorDark = colorDark;
            this.passive = passive;
            this.heatEmission = heatEmission;
            this.mudContent = mudContent / 2D;
            this.burnFunc = burnFunc;
            this.heatDiv = heatDiv;
        }

        @Override public double yield() { return 1_000_000_000; }
        @Override public int colorLight() { return colorLight; }
        @Override public int colorDark() { return colorDark; }
        @Override public double passive() { return passive; }
        @Override public double heatEmission() { return heatEmission; }
        @Override public double mudContent() { return mudContent; }
        @Override public @Nullable Function burnFunc() { return burnFunc; }
        @Override public @Nullable Function heatDiv() { return heatDiv; }
        @Override public @Nullable Function absorbFunc() { return null; }

        @Override
        public ItemStack depletedStack() {
            return WATZ_PELLET_DEPLETED.stack(this);
        }
    }

    public static final ItemFamily<SpaceWatz, ItemWatzPellet> WATZ_PELLET = watz("watz_pellet", false);
    public static final ItemFamily<SpaceWatz, ItemWatzPellet> WATZ_PELLET_DEPLETED = watz("watz_pellet_depleted", true);

    private static ItemFamily<SpaceWatz, ItemWatzPellet> watz(String prefix, boolean depleted) {
        return Reg.family(SpaceWatz.values(), type -> prefix + "_" + type.name().toLowerCase(Locale.ROOT),
                (props, type) -> new ItemWatzPellet(props, depleted, type), () -> new Item.Properties().stacksTo(16));
    }

    /** The decayed americium pellet the berkelium pellet turns into (upstream DepletedRTGMaterial.AMERICIUM). */
    public static final RegistryHandle<ItemRTGPelletDepleted> RTG_DEPLETED_AMERICIUM = Reg.item("pellet_rtg_depleted_americium",
            ItemRTGPelletDepleted::new, () -> new Item.Properties().craftRemainder(com.hbm.items.ModItems.plate(com.hbm.inventory.material.Mats.MAT_IRON)));

    public static final RegistryHandle<ItemRTGPellet> RTG_BERKELIUM = Reg.item("pellet_rtg_berkelium",
            props -> new BerkeliumPellet(props).setDecays(DepletedRTGMaterial.NEPTUNIUM, (long) (RTGUtil.getLifespan(13.8F, HalfLifeType.LONG, false) * 1.5), 1D),
            () -> new Item.Properties().stacksTo(1));

    /** Upstream sets the decay with its default count of one half-life, then decays into americium. */
    public static class BerkeliumPellet extends ItemRTGPellet {
        public BerkeliumPellet(Item.Properties props) {
            super(props, 20);
        }

        @Override
        public @Nullable ItemStack getDecayItem() {
            return new ItemStack(RTG_DEPLETED_AMERICIUM.get());
        }
    }

    public static final RegistryHandle<OxyTank> OXY_PLSS = Reg.item("oxy_plss", props -> new OxyTank(props, 16_000, 10, 1), Item.Properties::new);
    public static final RegistryHandle<LoxTank> LOX_TANK = Reg.item("lox_tank", LoxTank::new, () -> new Item.Properties().stacksTo(16));

    /** NTM:Space's ItemModOxy: a helmet plate that feeds a sealed suit with oxygen. */
    public static class OxyTank extends ItemArmorMod implements IFluidContainerItem, SpaceEnvironment.OxygenTank {
        private final int capacity;
        private final int rate;
        private final int consumption;

        public OxyTank(Item.Properties props, int capacity, int rate, int consumption) {
            super(props, ArmorModHandler.PLATE_ONLY, true, false, false, false);
            this.capacity = capacity;
            this.rate = rate;
            this.consumption = consumption;
        }

        @Override
        public boolean attemptBreathing(LivingEntity entity, ItemStack tank, @Nullable CBT_Atmosphere atmosphere, boolean force) {
            if (!force && com.hbm.space.atmosphere.SpaceAtmosphere.canBreathe(atmosphere)) {
                setInUse(tank, false);
                return true;
            }
            if (entity.tickCount % rate == 0) setContent(tank, new FluidStackNTM(NTMFluids.OXYGEN, Math.max(getFuel(tank) - consumption, 0)));
            boolean hasFuel = getFuel(tank) > 0;
            setInUse(tank, hasFuel);
            return hasFuel;
        }

        private static final net.minecraft.sounds.SoundEvent BREATHING =
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Library.id("player.plss_breathing"));
        private static com.hbm.sound.@Nullable AudioWrapper audioBreathing;

        private static boolean inUse(ItemStack stack) {
            net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            return data != null && data.copyTag().getBoolean("inUse");
        }

        private static void setInUse(ItemStack stack, boolean inUse) {
            if (inUse(stack) == inUse) return;
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("inUse", inUse));
        }

        @Override
        public boolean ticksClient() {
            return true;
        }

        /** Upstream ItemModOxy: the wearer hears the PLSS breathe while it feeds them. */
        @Override
        public void modUpdateClient(LivingEntity entity, ItemStack armor) {
            if (!(entity instanceof net.minecraft.world.entity.player.Player player) || !player.isLocalPlayer()) return;
            ItemStack tank = ArmorModHandler.pryMod(armor, ArmorModHandler.PLATE_ONLY);
            if (inUse(tank) && !player.getAbilities().instabuild && com.hbm.config.RenderConfig.spacePlssBreathing) {
                if (audioBreathing == null || !audioBreathing.isPlaying()) {
                    audioBreathing = com.hbm.sound.AudioSystem.getLoopedSound(BREATHING, net.minecraft.sounds.SoundSource.PLAYERS,
                            (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), 0.1F, 5.0F, 1.0F, 10);
                    if (audioBreathing != null) {
                        audioBreathing.attachTo(entity);
                        audioBreathing.startSound();
                    }
                }
                if (audioBreathing != null) audioBreathing.keepAlive();
            } else if (audioBreathing != null) {
                audioBreathing.stopSound();
                audioBreathing = null;
            }
        }

        public int getFuel(ItemStack stack) {
            FluidStackNTM held = getContent(stack);
            return held.type() == NTMFluids.OXYGEN ? (int) held.amount() : 0;
        }

        @Override
        public boolean canStore(Fluid type) {
            return false;
        }

        @Override
        public boolean machineFillable() {
            return true;
        }

        @Override
        public int capacity(ItemStack stack) {
            return capacity;
        }

        @Override
        public FluidStackNTM getContent(ItemStack stack) {
            return stack.getOrDefault(ModDataComponents.FLUID_CONTENT.get(), EMPTY_CONTENT);
        }

        @Override
        public void setContent(ItemStack stack, @Nullable FluidStackNTM content) {
            if (content == null || content.amount() <= 0) {
                stack.remove(ModDataComponents.FLUID_CONTENT.get());
                return;
            }
            stack.set(ModDataComponents.FLUID_CONTENT.get(), new FluidStackNTM(content.type(), Math.min(content.amount(), capacity), content.pressure()));
        }

        @Override
        public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
            if (type != NTMFluids.OXYGEN || amount <= 0) return 0;
            int have = getFuel(stack);
            int accepted = Math.min(amount, capacity - have);
            if (accepted <= 0) return 0;
            setContent(stack, new FluidStackNTM(type, have + accepted, pressure));
            return accepted;
        }

        @Override
        public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
            return 0;
        }

        @Override
        protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
            adder.accept(Component.translatable("item.hbm.jetpack.fuel", com.hbm.inventory.fluid.NTMFluidProperties.getDisplayName(NTMFluids.OXYGEN), getFuel(stack), capacity)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            adder.accept(Component.empty());
            adder.accept(Component.translatable("armor.mustSeal").withStyle(ChatFormatting.GOLD));
        }
    }

    /** NTM:Space's lox tank: fills a worn PLSS with 1000mB of oxygen. */
    public static class LoxTank extends ItemCompat {
        public LoxTank(Item.Properties props) {
            super(props);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
            adder.accept(Component.translatable("item.hbm.lox_tank.desc"));
        }

        @Override
        public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
            if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (helmet.isEmpty() || !ArmorModHandler.hasMods(helmet)) return InteractionResult.PASS;
            ItemStack tank = ArmorModHandler.pryMod(helmet, ArmorModHandler.PLATE_ONLY);
            if (!(tank.getItem() instanceof OxyTank oxy)) return InteractionResult.PASS;
            if (oxy.fill(tank, NTMFluids.OXYGEN, 1000, 0) > 0) {
                player.getItemInHand(hand).consume(1, player);
                EntitySounds.play(player, player.getX(), player.getY(), player.getZ(), ModSounds.JETPACK_TANK.get(), SoundSource.PLAYERS, 1F, 1F);
            }
            ArmorModHandler.applyMod(helmet, tank, ArmorModHandler.PLATE_ONLY);
            return InteractionResult.SUCCESS;
        }
    }

    /** Upstream MAT_CONGLOMERATE: an additive the crucible takes from conglomerate ore (4 ingots each). */
    public static final com.hbm.inventory.material.NTMMaterial MAT_CONGLOMERATE =
            com.hbm.inventory.material.Mats.makeAdditive(24_019, com.hbm.inventory.material.Mats.df("conglomerate"), 0x797979, 0x797979, 0x797979).m();

    /** Conglomerate, the moons' resource stone (upstream stone_resource meta CONGLOMERATE). */
    public static final RegistryHandle<Block> CONGLOMERATE = Reg.blockItem("stone_resource_conglomerate", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(5.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());

    /** NTM:Space's bobbleheads: Gwen (meta 21, made on the magic pedestal) and three contributors. */
    public enum SpaceBobble implements Bobble {
        GWEN("Gwen", "Gwen", "Numero Uno Homie",
                "HELP ME I'M TRAPPED IN THIS FUCKING POLYRESIN PRISON YOU NEED TO LET ME OUT PLEASE SMASH IT OPEN DO IT NOW CRACK IT", true),
        JUICE("Juicy_Lad", "Juicy_Lad", "The Mojave Testing for this funny fork",
                "\"What should the Inscription be?\",$ \"Uhh.. I'll think about it.\"", true),
        JAMESH_2("JamesH_2", "JamesH_2", "The fork itself", "COME ON AND SLAM", true),
        MRKIMKIMORA("MrKimkimora", "MrKimkimora", "Part of NTM:Space models,$textures and russian localization",
                "I know your IP. Your IP is 127.0.0.1.", false);

        private final String fullName;
        private final String label;
        private final String contribution;
        private final String inscription;
        private final boolean layers;

        SpaceBobble(String fullName, String label, String contribution, String inscription, boolean layers) {
            this.fullName = fullName;
            this.label = label;
            this.contribution = contribution;
            this.inscription = inscription;
            this.layers = layers;
        }

        @Override public String fullName() { return fullName; }
        @Override public String label() { return label; }
        @Override public @Nullable String contribution() { return contribution; }
        @Override public @Nullable String inscription() { return inscription; }
        @Override public boolean skinLayers() { return layers; }
        @Override public ItemStack stack() {
            return new ItemStack(switch (this) {
                case GWEN -> BOBBLEHEAD_GWEN.get();
                case JUICE -> BOBBLEHEAD_JUICE.get();
                case JAMESH_2 -> BOBBLEHEAD_JAMESH_2.get();
                case MRKIMKIMORA -> BOBBLEHEAD_MRKIMKIMORA.get();
            });
        }
    }

    public static final RegistryHandle<GwenBobblehead> BOBBLEHEAD_GWEN = Reg.item("bobblehead_gwen",
            GwenBobblehead::new, Item.Properties::new);

    /** NTM:Space's contributor bobbleheads besides Gwen; creative-only, as upstream. */
    public static final RegistryHandle<ItemBlockTrinket<SpaceBobble>> BOBBLEHEAD_JUICE = bobble("bobblehead_juice", SpaceBobble.JUICE);
    public static final RegistryHandle<ItemBlockTrinket<SpaceBobble>> BOBBLEHEAD_JAMESH_2 = bobble("bobblehead_jamesh_2", SpaceBobble.JAMESH_2);
    public static final RegistryHandle<ItemBlockTrinket<SpaceBobble>> BOBBLEHEAD_MRKIMKIMORA = bobble("bobblehead_mrkimkimora", SpaceBobble.MRKIMKIMORA);

    private static RegistryHandle<ItemBlockTrinket<SpaceBobble>> bobble(String id, SpaceBobble type) {
        return Reg.item(id, props -> new ItemBlockTrinket<>(ModBlocks.BOBBLEHEAD.get(), props, type), Item.Properties::new);
    }

    /** Upstream's shredder breaks and blows up when Gwen goes into it (TileEntityMachineShredder.canProcess). */
    public static class GwenBobblehead extends ItemBlockTrinket<SpaceBobble>
            implements com.hbm.blockentity.machine.BlockEntityMachineShredder.Wrecker {

        public GwenBobblehead(Item.Properties props) {
            super(ModBlocks.BOBBLEHEAD.get(), props, SpaceBobble.GWEN);
        }

        @Override
        public void wreck(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
            level.destroyBlock(pos, false);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5F, true,
                    net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        }
    }

    static {
        BlockBobble.TYPES.addAfter(BobbleType.CIRNO, SpaceBobble.GWEN);
        BlockBobble.TYPES.addAfter(SpaceBobble.GWEN, SpaceBobble.JUICE);
        BlockBobble.TYPES.addAfter(SpaceBobble.JUICE, SpaceBobble.JAMESH_2);
        BlockBobble.TYPES.addAfter(BobbleType.MELLOW, SpaceBobble.MRKIMKIMORA);
    }

    private static void after(CreativeContents.Tab tab, String anchor, Iterable<? extends RegistryHandle<? extends net.minecraft.world.level.ItemLike>> items) {
        for (RegistryHandle<? extends net.minecraft.world.level.ItemLike> item : items) {
            CreativeContents.insertAfter(tab, Library.id(anchor), () -> new ItemStack(item.get()));
            anchor = item.id().getPath();
        }
    }

    /**
     * Upstream's OreLayer3D of conglomerate on the Mun, Ike and Minmus (scale 0.04 by 0.25,
     * threshold 220), through the port's 3D stratum, replacing the planet's stone.
     */
    public static void conglomerate(com.hbm.space.dim.legacy.World world, String body, int x, int z) {
        layer(world, "conglomerate_" + body, CONGLOMERATE.get().defaultBlockState(), 0.04D, 0.25D, 220D, x, z);
    }

    /** Upstream's OreLayer3D on a Space body, through the port's 3D stratum, replacing its stone. */
    public static void layer(com.hbm.space.dim.legacy.World world, String key, net.minecraft.world.level.block.state.BlockState block,
            double scaleH, double scaleV, double threshold, int x, int z) {
        if (com.hbm.space.dim.legacy.Features.recorded(block.getBlock(), 0, world.stone(), -1, -1,
                com.hbm.world.feature.Stratum3DFeature.MIN_Y, com.hbm.world.feature.Stratum3DFeature.MAX_Y, "stratum_3d")) return;
        com.hbm.world.feature.Stratum3DFeature.place(
                world.level(),
                net.minecraft.util.RandomSource.create(0L),
                new com.hbm.world.feature.Stratum3DConfig(
                        Library.id("stratum_" + key + "/x"),
                        Library.id("stratum_" + key + "/y"),
                        Library.id("stratum_" + key + "/z"),
                        block,
                        new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(world.stone()),
                        scaleH,
                        scaleV,
                        threshold),
                x,
                z);
    }

    /**
     * Upstream's OreCave on a Space body, through the port's ore cave feature: ore lines the
     * cave walls of {@code replace} (the planet's stone when null) around {@code yLevel}.
     */
    public static void oreCave(com.hbm.space.dim.legacy.World world, int x, int z, String key,
            net.minecraft.world.level.block.state.BlockState ore,
            net.minecraft.world.level.block.state.@org.jspecify.annotations.Nullable BlockState fluid,
            @org.jspecify.annotations.Nullable String spikes, @org.jspecify.annotations.Nullable Block replace,
            double threshold, int rangeMult, int yLevel, int maxRange, boolean ignoreWater) {
        Block target = replace != null ? replace : world.stone();
        if (com.hbm.space.dim.legacy.Features.recorded(ore.getBlock(), 0, target, -1, -1,
                yLevel - maxRange, yLevel + maxRange, "ore_cave")) return;
        java.util.Optional<net.minecraft.world.level.block.state.BlockState> stalactite = java.util.Optional.empty(), stalagmite = java.util.Optional.empty();
        if (spikes != null) {
            stalactite = java.util.Optional.of(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(Library.id("stalactite_" + spikes)).defaultBlockState());
            stalagmite = java.util.Optional.of(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(Library.id("stalagmite_" + spikes)).defaultBlockState());
        }
        com.hbm.world.feature.OreCaveConfig config = new com.hbm.world.feature.OreCaveConfig(ore, java.util.Optional.ofNullable(fluid),
                stalactite, stalagmite, new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(target),
                threshold, rangeMult, maxRange, yLevel, Library.id("noise/ore_cave_" + key), ignoreWater);
        net.minecraft.world.level.WorldGenLevel level = world.level();
        com.hbm.world.HbmFeatures.ORE_CAVE.get().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(
                java.util.Optional.empty(), level, null, net.minecraft.util.RandomSource.create(level.getSeed() ^ ((long) x * 341873128712L + (long) z * 132897987541L)),
                new net.minecraft.core.BlockPos(x, yLevel, z), config));
    }

    /** NTM:Space's sulfur caves, on every body that has them. */
    public static void sulfurCave(com.hbm.space.dim.legacy.World world, int x, int z, boolean acid) {
        oreCave(world, x, z, "sulfur", com.hbm.space.dim.legacy.LegacyBlocks.b("stone_resource_sulfur").defaultBlockState(),
                acid ? com.hbm.space.dim.legacy.LegacyBlocks.b("sulfuric_acid_block").defaultBlockState() : null,
                "sulfur", null, 1.5D, 20, 30, 20, false);
    }

    /** NTM:Space's Oxygen Pinwheel, an endless trickle of oxygen like the chlorine one. */
    public static final RegistryHandle<com.hbm.items.machine.ItemFluidContainerInfinite> OXY_PINWHEEL = Reg.item("oxy_pinwheel",
            p -> new com.hbm.items.machine.ItemFluidContainerInfinite(p, () -> NTMFluids.byName("OXYGEN"), 1),
            Item.Properties::new);
    /**
     * NTM:Space: the infinite water barrels draw on the body's water table, so they only run
     * where it is water. The verdict is sampled while carried and kept on the stack.
     */
    private static final class WaterTableCondition implements com.hbm.items.machine.ItemFluidContainerInfinite.Condition {
        private static boolean needsTable(com.hbm.items.machine.ItemFluidContainerInfinite item) {
            return item == com.hbm.items.ModItems.INF_WATER.get() || item == com.hbm.items.ModItems.INF_WATER_MK2.get();
        }

        private static net.minecraft.nbt.CompoundTag tag(ItemStack stack) {
            return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        }

        @Override
        public boolean operates(com.hbm.items.machine.ItemFluidContainerInfinite item, ItemStack stack) {
            return !needsTable(item) || !tag(stack).getBoolean("noAtmo");
        }

        @Override
        public void sample(com.hbm.items.machine.ItemFluidContainerInfinite item, ItemStack stack, net.minecraft.server.level.ServerLevel level) {
            if (!needsTable(item)) return;
            net.minecraft.world.level.material.Fluid table =
                    com.hbm.space.CelestialBody.inOrbit(level) ? null : com.hbm.api.environment.Environment.groundwater(level);
            boolean blocked = table != item.fluid();
            String fluid = table == null ? "" : net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(table).toString();
            net.minecraft.nbt.CompoundTag tag = tag(stack);
            if (tag.getBoolean("noAtmo") == blocked && tag.getString("table").equals(fluid)) return;
            tag.putBoolean("noAtmo", blocked);
            tag.putString("table", fluid);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }

        @Override
        public void describe(com.hbm.items.machine.ItemFluidContainerInfinite item, ItemStack stack, java.util.function.Consumer<net.minecraft.network.chat.Component> adder) {
            if (!needsTable(item) || operates(item, stack)) return;
            String table = tag(stack).getString("table");
            net.minecraft.ChatFormatting red = net.minecraft.ChatFormatting.RED;
            net.minecraft.network.chat.Component needs = net.minecraft.network.chat.Component.translatable(
                    "desc.item.infFluid.needs", item.fluid().getFluidType().getDescription()).withStyle(red);
            if (table.isEmpty()) {
                adder.accept(net.minecraft.network.chat.Component.translatable("desc.item.infFluid.noVapor").withStyle(red));
                adder.accept(needs);
                adder.accept(net.minecraft.network.chat.Component.translatable("desc.item.infFluid.addVapor").withStyle(red));
            } else {
                net.minecraft.world.level.material.Fluid current = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
                        net.minecraft.resources.ResourceLocation.parse(table));
                adder.accept(net.minecraft.network.chat.Component.translatable("desc.item.infFluid.wrongTable").withStyle(red));
                adder.accept(needs);
                adder.accept(net.minecraft.network.chat.Component.translatable("desc.item.infFluid.requires",
                        current.getFluidType().getDescription()).withStyle(red));
            }
        }
    }

    /** NTM:Space's civilian airliner caller and its jet loop. */
    public static final RegistryHandle<com.hbm.items.tool.ItemBombCaller> BOMB_CALLER_AIRLINER = Reg.item(
            "bomb_caller_civilian_airliner",
            props -> new com.hbm.items.tool.ItemBombCaller(props, com.hbm.items.tool.ItemBombCaller.EnumCallerType.CIVILIAN_AIRLINER),
            () -> new Item.Properties().stacksTo(1));
    /** NTM:Space's music off Earth and its station hum ({@code SpaceAmbience}). */
    public static final RegistryHandle<net.minecraft.sounds.SoundEvent> MUSIC_SPACE =
            com.hbm.registration.NeoForgeRegistrar.registerSound("music.game.space");
    public static final RegistryHandle<net.minecraft.sounds.SoundEvent> STATION_HUM =
            com.hbm.registration.NeoForgeRegistrar.registerSound("misc.stationhum");
    public static final RegistryHandle<net.minecraft.sounds.SoundEvent> JET_LOOP_BOMB =
            com.hbm.registration.NeoForgeRegistrar.registerSound("entity.jet_loop_bomb");

    public static void init() {
        after(CreativeContents.Tab.CONSUMABLES, "bomb_caller_atomic_bomb", java.util.List.of(BOMB_CALLER_AIRLINER));
        after(CreativeContents.Tab.PARTS, "chlorine_pinwheel", java.util.List.of(OXY_PINWHEEL));
        if (com.hbm.space.SpaceToggle.changes())
            com.hbm.items.machine.ItemFluidContainerInfinite.CONDITION.install("hbm_space", new WaterTableCondition());
        com.hbm.entity.logic.EntityBomber.addonLoop(9, JET_LOOP_BOMB::get);
        // NTM:Space's survey scanner reports natural gas and brine deposits too
        com.hbm.items.tool.ItemSurveyScanner.addLine("chat.surveyScanner.foundGas", net.minecraft.ChatFormatting.WHITE,
                java.util.List.of(SpaceBlocks.ORE_GAS));
        com.hbm.items.tool.ItemSurveyScanner.addLine("chat.surveyScanner.foundBrine", net.minecraft.ChatFormatting.WHITE,
                java.util.List.of(SpaceBlocks.ORE_BRINE));
        // NTM:Space's pumps draw groundwater through the planets' soils too
        for (String ground : new String[] {"eve_silt", "eve_rock", "ike_regolith", "ike_stone", "duna_sands", "moon_turf",
                "laythe_silt", "moho_regolith", "minmus_smooth", "vinyl_sand", "rubber_silt", "rubber_grass"})
            com.hbm.blockentity.machine.BlockEntityMachinePumpBase.addGround(() -> com.hbm.space.dim.legacy.LegacyBlocks.b(ground));
        after(CreativeContents.Tab.PARTS, "nuclear_waste_short_americium242", WASTE_SHORT);
        after(CreativeContents.Tab.PARTS, "nuclear_waste_short_tiny_americium242", WASTE_SHORT_TINY);
        after(CreativeContents.Tab.PARTS, "nuclear_waste_short_depleted_americium242", WASTE_SHORT_DEPLETED);
        after(CreativeContents.Tab.PARTS, "nuclear_waste_short_depleted_tiny_americium242", WASTE_SHORT_DEPLETED_TINY);
        after(CreativeContents.Tab.CONTROL, "watz_pellet_du", WATZ_PELLET);
        after(CreativeContents.Tab.CONTROL, "watz_pellet_depleted_du", WATZ_PELLET_DEPLETED);
        after(CreativeContents.Tab.CONTROL, "pellet_rtg_americium", java.util.List.of(RTG_BERKELIUM));
        after(CreativeContents.Tab.CONTROL, "pellet_rtg_depleted_neptunium", java.util.List.of(RTG_DEPLETED_AMERICIUM));
        after(CreativeContents.Tab.CONSUMABLES, "jetpack_tank", java.util.List.of(OXY_PLSS, LOX_TANK));
        after(CreativeContents.Tab.BLOCKS, "stone_resource_bauxite", java.util.List.of(CONGLOMERATE));
        after(CreativeContents.Tab.BLOCKS, "bobblehead_cirno", java.util.List.of(BOBBLEHEAD_GWEN, BOBBLEHEAD_JUICE, BOBBLEHEAD_JAMESH_2, BOBBLEHEAD_MRKIMKIMORA));
    }
}
