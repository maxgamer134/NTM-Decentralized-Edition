// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.trait.CBT_Atmosphere;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * NTM:Space's crops: strawberries, mint, coffee and tea on farmland, and chlorinated paraffin
 * on Tekto farmland. Each needs an atmosphere it can breathe ({@link Crop#canBreathe}), which
 * the atmosphere system checks; {@link Crop#hydroponic} marks the ones a hydroponic bay grows.
 */
public final class SpaceCrops {

    private static final Predicate<CBT_Atmosphere> EARTHLIKE =
            a -> a.hasFluid(NTMFluids.byName("EARTHAIR"), 0.1) || a.hasFluid(NTMFluids.byName("OXYGEN"), 0.1);
    private static final Predicate<CBT_Atmosphere> TEKTO =
            a -> a.hasFluid(NTMFluids.byName("TEKTOAIR"), 0.1) || a.hasFluid(NTMFluids.byName("CHLORINE"), 0.1);

    public static final RegistryHandle<Crop> CROP_STRAWBERRY = crop("crop_strawberry", () -> Blocks.FARMLAND, EARTHLIKE, true, () -> SpaceCrops.STRAWBERRY.get());
    public static final RegistryHandle<Crop> CROP_MINT = crop("crop_mint", () -> Blocks.FARMLAND, EARTHLIKE, true, () -> SpaceCrops.MINT_LEAVES.get());
    public static final RegistryHandle<Crop> CROP_COFFEE = crop("crop_coffee", () -> Blocks.FARMLAND, EARTHLIKE, true, () -> SpaceCrops.BEAN_RAW.get());
    public static final RegistryHandle<Crop> CROP_TEA = crop("crop_tea", () -> Blocks.FARMLAND, EARTHLIKE, true, () -> SpaceCrops.TEASEEDS.get());
    public static final RegistryHandle<Crop> CROP_PARAFFIN = crop("crop_paraffin", () -> SpaceRubber.RUBBER_FARMLAND.get(), TEKTO, false, () -> SpaceCrops.PARAFFIN_SEEDS.get());

    public static final RegistryHandle<Item> STRAWBERRY = seed("strawberry", CROP_STRAWBERRY, 3, 0.4F);
    public static final RegistryHandle<Item> MINT_LEAVES = seed("mint_leaves", CROP_MINT, 3, 0.4F);
    public static final RegistryHandle<Item> BEAN_RAW = seed("bean_raw", CROP_COFFEE, 1, 0.2F);
    public static final RegistryHandle<Item> TEASEEDS = Reg.item("teaseeds", p -> new ItemNameBlockItem(CROP_TEA.get(), p), Item.Properties::new);
    public static final RegistryHandle<Item> TEA_LEAF = Reg.item("tea_leaf", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> PARAFFIN_SEEDS = seed("paraffin_seeds", CROP_PARAFFIN, 1, 0.2F);

    private SpaceCrops() {}

    private static RegistryHandle<Crop> crop(String name, Supplier<Block> soil, Predicate<CBT_Atmosphere> breathes, boolean hydro, Supplier<ItemLike> seed) {
        return Reg.block(name, p -> new Crop(p, soil, breathes, hydro, seed), () -> BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY));
    }

    /** NTM:Space's ItemSeedFood: food that plants its crop. */
    private static RegistryHandle<Item> seed(String name, RegistryHandle<Crop> crop, int food, float saturation) {
        return Reg.item(name, p -> new ItemNameBlockItem(crop.get(), p),
                () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(food).saturationModifier(saturation).build()));
    }

    public static void init() {
        var anchor = Library.id("lemon");
        for (var i : new RegistryHandle<?>[] {PARAFFIN_SEEDS, TEA_LEAF, TEASEEDS, BEAN_RAW, MINT_LEAVES, STRAWBERRY})
            CreativeContents.insertAfter(CreativeContents.Tab.CONSUMABLES, anchor, () -> new ItemStack((Item) i.get()));
    }

    public static class Crop extends CropBlock {
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

        private final Supplier<Block> soil;
        private final Predicate<CBT_Atmosphere> breathes;
        public final boolean hydroponic;
        private final Supplier<ItemLike> seed;

        public Crop(BlockBehaviour.Properties props, Supplier<Block> soil, Predicate<CBT_Atmosphere> breathes, boolean hydroponic, Supplier<ItemLike> seed) {
            super(props);
            this.soil = soil;
            this.breathes = breathes;
            this.hydroponic = hydroponic;
            this.seed = seed;
        }

        public boolean canBreathe(CBT_Atmosphere atmosphere) {
            return atmosphere != null && breathes.test(atmosphere);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(soil.get());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }

        /** NTM:Space grows a stage on every random tick, with no regard for light or water. */
        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int age = getAge(state);
            if (age < getMaxAge()) level.setBlock(pos, getStateForAge(age + 1), 2);
        }

        @Override
        protected ItemLike getBaseSeedId() {
            return seed.get();
        }
    }
}
