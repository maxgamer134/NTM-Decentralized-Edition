// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.trait.CBT_Atmosphere;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/**
 * NTM:Space's BlockNTSapling: vinyl and PVC saplings that grow on rubber soil into TTree's
 * trunks crowned with an ellipsoid of leaves (vinyl logs with plastic leaves, PVC logs with
 * rubber leaves). NTM:Space grew them only on rubber grass and silt though they could be planted
 * on rubber farmland too; here they grow wherever they can be planted.
 */
public class SpaceSapling extends BushBlock implements BonemealableBlock {

    public enum Type implements StringRepresentable {
        VINYL, PVC;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    public static final RegistryHandle<SpaceSapling> SAPLING_PVC = Reg.blockItem("sapling_pvc", SpaceSapling::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()
                    .sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY),
            (block, props) -> new net.minecraft.world.item.BlockItem(block, props) {
                @Override
                public net.minecraft.network.chat.Component getName(ItemStack stack) {
                    var props2 = stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
                    Type type = props2 == null ? null : props2.get(TYPE);
                    return net.minecraft.network.chat.Component.translatable(
                            getDescriptionId() + "." + (type == null ? Type.VINYL : type).getSerializedName());
                }
            });

    public SpaceSapling(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(TYPE, Type.VINYL));
    }

    public static void init() {
        var anchor = Library.id("moon_turf");
        CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, anchor, () -> SpaceRubber.stack(SAPLING_PVC.get(), TYPE, Type.PVC));
        CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, anchor, () -> SpaceRubber.stack(SAPLING_PVC.get(), TYPE, Type.VINYL));
    }

    public static boolean canBreathe(CBT_Atmosphere atmosphere) {
        return atmosphere != null && (atmosphere.hasFluid(com.hbm.inventory.fluid.NTMFluids.byName("TEKTOAIR"), 0.1)
                || atmosphere.hasFluid(com.hbm.inventory.fluid.NTMFluids.byName("CHLORINE"), 0.1));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return simpleCodec(SpaceSapling::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return SpaceRubber.rubberSoil(state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return (level.getRawBrightness(pos, 0) >= 8 || level.canSeeSky(pos)) && super.canSurvive(state, level, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) grow(level, pos, state, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < 0.45F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        grow(level, pos, state, random);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return SpaceRubber.stack(this, TYPE, state.getValue(TYPE));
    }

    private static void grow(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        boolean pvc = state.getValue(TYPE) == Type.PVC;
        Block log = pvc ? SpaceBlocks.PVC_LOG.get() : SpaceBlocks.VINYL_LOG.get();
        Block leaves = pvc ? SpaceRubber.RUBBER_LEAVES.get() : SpaceRubber.PET_LEAVES.get();
        // NTM:Space's TTree arguments for each sapling: offset, smallest, tallest, xz and y radius
        if (pvc) tree(level, pos, random, 2, 5, 7, 4, 3, log, leaves);
        else tree(level, pos, random, 2, 4, 5, 3, 2, log, leaves);
    }

    /** TTree.generate: a straight trunk and an ellipsoid of leaves around its top. */
    static boolean tree(ServerLevel level, BlockPos pos, RandomSource rand, int offset, int smallest, int tallest,
            int xz, int ry, Block log, Block leaf) {
        int height = rand.nextInt(smallest) + tallest;
        if (pos.getY() < level.getMinBuildHeight() + 1 || pos.getY() + height + 1 > level.getMaxBuildHeight()) return false;
        if (!SpaceRubber.rubberSoil(level.getBlockState(pos.below()))) return false;
        for (int i = 1; i < height; i++)
            if (!level.getBlockState(pos.above(i)).canBeReplaced()) return false;
        level.removeBlock(pos, false);
        for (int i = 0; i < height; i++) level.setBlock(pos.above(i), log.defaultBlockState(), 2);
        int top = pos.getY() + height - 1;
        int bulb = pos.getY() + height - offset;
        for (int dy = -ry; dy <= ry; dy++) {
            for (int dx = -xz; dx <= xz; dx++) {
                for (int dz = -xz; dz <= xz; dz++) {
                    double e = Math.pow(dx / (double) xz, 2) + Math.pow(dy / (double) ry, 2) + Math.pow(dz / (double) xz, 2);
                    if (e > 1) continue;
                    BlockPos p = new BlockPos(pos.getX() + dx, bulb + dy, pos.getZ() + dz);
                    if (!level.getBlockState(p).isAir()) continue;
                    // the leaves' distance to the trunk, so they stand as long as the tree does
                    int distance = Math.abs(dx) + Math.abs(dz) + Math.max(0, p.getY() - top) + Math.max(0, pos.getY() - p.getY());
                    BlockState state = leaf.defaultBlockState();
                    if (state.hasProperty(LeavesBlock.DISTANCE)) {
                        state = distance < LeavesBlock.DECAY_DISTANCE
                                ? state.setValue(LeavesBlock.DISTANCE, Math.max(1, distance))
                                : state.setValue(LeavesBlock.PERSISTENT, true);
                    }
                    level.setBlock(p, state, 2);
                }
            }
        }
        return true;
    }
}
