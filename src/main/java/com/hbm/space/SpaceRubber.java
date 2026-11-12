// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Locale;

/** NTM:Space's rubber and PVC flora: farmland, leaves, ground plants and spike cacti. */
public final class SpaceRubber {

    public static final RegistryHandle<RubberFarm> RUBBER_FARMLAND = Reg.blockItem("rubber_farmland", RubberFarm::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.GRAVEL).strength(1.0F, 0.6F)
                    .randomTicks().isViewBlocking((s, l, p) -> true).isSuffocating((s, l, p) -> true));
    public static final RegistryHandle<LeavesBlock> RUBBER_LEAVES = Reg.blockItem("rubber_leaves", LeavesBlock::new, SpaceRubber::leaves);
    public static final RegistryHandle<LeavesBlock> PET_LEAVES = Reg.blockItem("pet_leaves", LeavesBlock::new, SpaceRubber::leaves);
    public static final RegistryHandle<RubberPlant> RUBBER_PLANT = Reg.blockItem("rubber_plant", RubberPlant::new, SpaceRubber::plant);
    public static final RegistryHandle<SpikeCacti> SPIKE_CACTI = Reg.blockItem("spike_cacti", SpikeCacti::new, SpaceRubber::plant);

    public static final RegistryHandle<Item> LEAF_RUBBER = Reg.item("leaf_rubber", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> LEAF_PET = Reg.item("leaf_pet", Item::new, Item.Properties::new);

    private SpaceRubber() {}

    private static BlockBehaviour.Properties leaves() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.5F, 1.5F).randomTicks().sound(SoundType.GRASS)
                .noOcclusion().isValidSpawn((s, l, p, t) -> false).isSuffocating((s, l, p) -> false)
                .isViewBlocking((s, l, p) -> false).ignitedByLava().pushReaction(PushReaction.DESTROY).isRedstoneConductor((s, l, p) -> false);
    }

    private static BlockBehaviour.Properties plant() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.GRASS).instabreak().noCollission()
                .replaceable().offsetType(BlockBehaviour.OffsetType.NONE).pushReaction(PushReaction.DESTROY);
    }

    public static void init() {
        var anchor = Library.id("moon_turf");
        for (int i = SpikeCacti.Type.values().length - 1; i >= 0; i--) {
            SpikeCacti.Type t = SpikeCacti.Type.values()[i];
            CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, anchor, () -> stack(SPIKE_CACTI.get(), SpikeCacti.TYPE, t));
        }
        for (int i = RubberPlant.Type.values().length - 1; i >= 0; i--) {
            RubberPlant.Type t = RubberPlant.Type.values()[i];
            CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, anchor, () -> stack(RUBBER_PLANT.get(), RubberPlant.TYPE, t));
        }
        for (var b : new RegistryHandle<?>[] {PET_LEAVES, RUBBER_LEAVES, RUBBER_FARMLAND})
            CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, anchor, () -> new ItemStack((Block) b.get()));
        var parts = Library.id("powder_coal_tiny");
        CreativeContents.insertAfter(CreativeContents.Tab.PARTS, parts, () -> new ItemStack(LEAF_PET.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.PARTS, parts, () -> new ItemStack(LEAF_RUBBER.get()));
        // NTM:Space's onUseHoe: a hoe tills rubber grass and rubber silt into rubber farmland
        NeoForge.EVENT_BUS.addListener((BlockEvent.BlockToolModificationEvent event) -> {
            if (event.getItemAbility() != ItemAbilities.HOE_TILL) return;
            BlockState state = event.getState();
            if ((state.is(SpacePlants.RUBBER_GRASS.get()) || state.is(SpaceBlocks.RUBBER_SILT.get()))
                    && event.getLevel().getBlockState(event.getPos().above()).isAir())
                event.setFinalState(RUBBER_FARMLAND.get().defaultBlockState());
        });
    }

    static <T extends Enum<T> & StringRepresentable> ItemStack stack(Block block, EnumProperty<T> prop, T value) {
        ItemStack stack = new ItemStack(block);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(prop, value));
        return stack;
    }

    static boolean rubberSoil(BlockState state) {
        return state.is(SpacePlants.RUBBER_GRASS.get()) || state.is(SpaceBlocks.RUBBER_SILT.get()) || state.is(RUBBER_FARMLAND.get());
    }

    /** Rubber farmland: kept moist by carbon tetrachloride within four blocks, or by rain. */
    public static class RubberFarm extends Block {
        public static final BooleanProperty MOIST = BooleanProperty.create("moist");
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

        public RubberFarm(BlockBehaviour.Properties props) {
            super(props);
            registerDefaultState(defaultBlockState().setValue(MOIST, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(MOIST);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }

        @Override
        protected boolean useShapeForLightOcclusion(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean moist = nearCcl(level, pos) || level.isRainingAt(pos.above());
            if (moist != state.getValue(MOIST)) level.setBlock(pos, state.setValue(MOIST, moist), 2);
        }

        private static boolean nearCcl(LevelReader level, BlockPos pos) {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4)))
                if (level.getBlockState(p).is(SpaceLayers.CCL_BLOCK.get())) return true;
            return false;
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
            super.fallOn(level, state, pos, entity, distance);
            if (!level.isClientSide() && level.random.nextFloat() < distance - 0.5F
                    && (entity instanceof Player || level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)))
                level.setBlockAndUpdate(pos, SpaceBlocks.RUBBER_SILT.get().defaultBlockState());
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
            if (dir == Direction.UP && other.isSolid()) return SpaceBlocks.RUBBER_SILT.get().defaultBlockState();
            return state;
        }

        @Override
        public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, Direction facing, BlockState plant) {
            return TriState.TRUE;
        }
    }

    /** NTM:Space's rubber grass, fern and shrub: they grow on rubber soil and drop nothing. */
    public static class RubberPlant extends BushBlock {
        public enum Type implements StringRepresentable {
            GRASS, FERN, SHRUB;

            @Override
            public String getSerializedName() {
                return name().toLowerCase(Locale.ROOT);
            }
        }

        public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);

        public RubberPlant(BlockBehaviour.Properties props) {
            super(props);
            registerDefaultState(defaultBlockState().setValue(TYPE, Type.GRASS));
        }

        @Override
        protected com.mojang.serialization.MapCodec<? extends BushBlock> codec() {
            return simpleCodec(RubberPlant::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TYPE);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return rubberSoil(state);
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
            return stack(this, TYPE, state.getValue(TYPE));
        }
    }

    /** NTM:Space's spike cacti, bushes and flowers; the flower drops paraffin seeds. */
    public static class SpikeCacti extends BushBlock {
        public enum Type implements StringRepresentable {
            CACT, BUSH, FLOWER;

            @Override
            public String getSerializedName() {
                return name().toLowerCase(Locale.ROOT);
            }
        }

        public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);

        public SpikeCacti(BlockBehaviour.Properties props) {
            super(props);
            registerDefaultState(defaultBlockState().setValue(TYPE, Type.CACT));
        }

        @Override
        protected com.mojang.serialization.MapCodec<? extends BushBlock> codec() {
            return simpleCodec(SpikeCacti::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TYPE);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return rubberSoil(state) || state.is(SpaceBlocks.VINYL_SAND.get());
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
            return stack(this, TYPE, state.getValue(TYPE));
        }
    }
}
