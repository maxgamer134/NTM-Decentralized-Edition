// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.Locale;

/** NTM:Space's simple plants and grasses. */
public final class SpacePlants {

    public static final RegistryHandle<RubberGrass> RUBBER_GRASS = Reg.blockItem("rubber_grass", RubberGrass::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.GRASS).strength(0.5F, 1.5F));
    public static final RegistryHandle<Block> ASTROTURF = Reg.blockItem("astroturf", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).sound(SoundType.GRASS).strength(0.6F)
                    .isValidSpawn((state, level, pos, type) -> false));
    public static final RegistryHandle<VinylVine> VINYL_VINES = Reg.blockItem("vinyl_vines", VinylVine::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).sound(SoundType.VINE).strength(1.0F, 0.6F)
                    .replaceable().noCollission().randomTicks().pushReaction(PushReaction.DESTROY));
    public static final RegistryHandle<WaterPlant> LAYTHE_SHORT = Reg.blockItem("laythe_short", WaterPlant::new, SpacePlants::waterPlant);
    public static final RegistryHandle<WaterPlant> LAYTHE_GLOW = Reg.blockItem("laythe_glow", WaterPlant::new,
            () -> waterPlant().lightLevel(s -> 15));
    public static final RegistryHandle<Coral> LAYTHE_CORAL = Reg.blockItem("laythe_coral", Coral::new, SpacePlants::waterPlant);
    public static final RegistryHandle<CoralBlock> LAYTHE_CORAL_BLOCK = Reg.blockItem("laythe_coral_block", CoralBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).sound(SoundType.GRASS).strength(0.5F, 0.5F));

    public static final RegistryHandle<Kelp> LAYTHE_KELP = Reg.blockItem("laythe_kelp", Kelp::new, SpacePlants::waterPlant);
    public static final RegistryHandle<TallWaterPlant> PLANT_TALL_LAYTHE = Reg.blockItem("plant_tall_laythe", TallWaterPlant::new, SpacePlants::waterPlant);

    public static final RegistryHandle<Item> SALTLEAF = Reg.item("saltleaf", Item::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.5F).build()));

    private SpacePlants() {}

    private static BlockBehaviour.Properties waterPlant() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WATER).sound(SoundType.WET_GRASS).instabreak()
                .replaceable().noCollission().pushReaction(PushReaction.DESTROY);
    }

    public static void init() {
        var anchor = Library.id("moon_turf");
        for (CoralType t : reversed(CoralType.values()))
            CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, anchor, () -> stack(LAYTHE_CORAL_BLOCK.get(), CoralBlock.CORAL, t));
        for (CoralType t : reversed(CoralType.values()))
            CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, anchor, () -> stack(LAYTHE_CORAL.get(), Coral.CORAL, t));
        for (var b : new RegistryHandle<?>[] {PLANT_TALL_LAYTHE, LAYTHE_KELP, LAYTHE_GLOW, LAYTHE_SHORT, VINYL_VINES, ASTROTURF, RUBBER_GRASS})
            CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, anchor, () -> new ItemStack((Block) b.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.PARTS, Library.id("powder_coal_tiny"), () -> new ItemStack(SALTLEAF.get()));
    }

    private static CoralType[] reversed(CoralType[] values) {
        CoralType[] out = new CoralType[values.length];
        for (int i = 0; i < values.length; i++) out[i] = values[values.length - 1 - i];
        return out;
    }

    private static ItemStack stack(Block block, EnumProperty<CoralType> prop, CoralType type) {
        ItemStack stack = new ItemStack(block);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(prop, type));
        return stack;
    }

    /** NTM:Space's BlockCoral.EnumCoral. */
    public enum CoralType implements StringRepresentable {
        TUBE, BRAIN, BUBBLE, FIRE, HORN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Rubber grass: it turns back into rubber silt once something covers it, and grows nothing. */
    public static class RubberGrass extends Block {
        public RubberGrass(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos from, boolean moved) {
            BlockState above = level.getBlockState(pos.above());
            if (!level.isClientSide() && (!above.getFluidState().isEmpty() || above.isRedstoneConductor(level, pos.above())))
                level.setBlockAndUpdate(pos, SpaceBlocks.RUBBER_SILT.get().defaultBlockState());
        }
    }

    /** Vinyl vines: untinted vines. */
    public static class VinylVine extends VineBlock {
        public VinylVine(BlockBehaviour.Properties props) {
            super(props);
        }
    }

    /** Laythe seagrass and glowgrass: underwater grass that drops up to three salt leaves. */
    public static class WaterPlant extends SeagrassBlock {
        public WaterPlant(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return false;
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return false;
        }
    }

    /** Laythe coral: five underwater corals that break into calcium powder. */
    public static class Coral extends WaterPlant {
        public static final EnumProperty<CoralType> CORAL = EnumProperty.create("coral", CoralType.class);

        public Coral(BlockBehaviour.Properties props) {
            super(props);
            registerDefaultState(defaultBlockState().setValue(CORAL, CoralType.TUBE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(CORAL);
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, LevelReader level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
            return stack(this, CORAL, state.getValue(CORAL));
        }
    }

    /** Laythe coral blocks, one for each coral. */
    public static class CoralBlock extends Block {
        public static final EnumProperty<CoralType> CORAL = Coral.CORAL;

        public CoralBlock(BlockBehaviour.Properties props) {
            super(props);
            registerDefaultState(defaultBlockState().setValue(CORAL, CoralType.TUBE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(CORAL);
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, LevelReader level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
            return stack(this, CORAL, state.getValue(CORAL));
        }
    }

    /** Laythe kelp: grows in columns on laythe silt, the topmost piece drawn as the tip. */
    public static class Kelp extends WaterPlant {
        public static final net.minecraft.world.level.block.state.properties.BooleanProperty TOP =
                net.minecraft.world.level.block.state.properties.BooleanProperty.create("top");

        public Kelp(BlockBehaviour.Properties props) {
            super(props);
            registerDefaultState(defaultBlockState().setValue(TOP, true));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TOP);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
            return state.is(this) || state.is(SpaceBlocks.LAYTHE_SILT.get());
        }

        @Override
        public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
            BlockState state = super.getStateForPlacement(ctx);
            return state == null ? null : state.setValue(TOP, !ctx.getLevel().getBlockState(ctx.getClickedPos().above()).is(this));
        }

        @Override
        protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState other,
                net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos otherPos) {
            BlockState updated = super.updateShape(state, dir, other, level, pos, otherPos);
            return updated.is(this) ? updated.setValue(TOP, !level.getBlockState(pos.above()).is(this)) : updated;
        }
    }

    /** Laythe saltgrass: a two-block underwater grass rooted in laythe silt. */
    public static class TallWaterPlant extends net.minecraft.world.level.block.TallSeagrassBlock {
        public TallWaterPlant(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
            return state.is(SpaceBlocks.LAYTHE_SILT.get());
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, LevelReader level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
            return new ItemStack(this);
        }
    }
}
