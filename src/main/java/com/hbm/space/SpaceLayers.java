// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.blocks.fluid.BlockFluidClassicBase;
import com.hbm.blocks.fluid.BlockSulfuricAcid;
import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.blocks.generic.BlockFallout;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.ModDamageTypes;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.trait.CBT_Temperature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/** NTM:Space's simple hand-written blocks: salted fallout, melted flesh that cooks on hot worlds, and its three fluids. */
public final class SpaceLayers {

    private static final Supplier<BlockBehaviour.Properties> FLESH = () -> BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).sound(SoundType.GRAVEL).strength(0.1F).noOcclusion()
            .replaceable().randomTicks().pushReaction(PushReaction.DESTROY);

    public static final RegistryHandle<SaltedFallout> SALTED_FALLOUT =
            Reg.blockItem("salted_fallout", SaltedFallout::new, () -> BlockFallout.defaultProperties().randomTicks());
    public static final RegistryHandle<MeltedFlesh> FLESH_BLOCK = Reg.blockItem("flesh_block", MeltedFlesh::new, FLESH);
    public static final RegistryHandle<MeltedFlesh> CHARRED_FLESH_BLOCK = Reg.blockItem("charred_flesh_block", MeltedFlesh::new, FLESH);
    public static final RegistryHandle<MeltedFlesh> CARBONIZED_FLESH_BLOCK = Reg.blockItem("carbonized_flesh_block", MeltedFlesh::new, FLESH);

    public static final RegistryHandle<Item> FLESH_ITEM = Reg.item("flesh", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> GRILLED_FLESH = Reg.item("grilled_flesh", Item::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build()));

    public static final ClassicFluid.Pair MERCURY_FLUID = fluid("mercury", 8, 5, 1840, () -> SpaceLayers.MERCURY_BLOCK.get(), "MERCURY");
    public static final RegistryHandle<BlockSulfuricAcid> MERCURY_BLOCK = fluidBlock("mercury_block", MERCURY_FLUID, ModDamageTypes.LEAD, 1F, MapColor.METAL);
    public static final ClassicFluid.Pair BROMINE_FLUID = fluid("bromine", 8, 15, 3000, () -> SpaceLayers.BROMINE_BLOCK.get(), "BROMINE");
    public static final RegistryHandle<BlockSulfuricAcid> BROMINE_BLOCK = fluidBlock("bromine_block", BROMINE_FLUID, ModDamageTypes.ACID, 5F, MapColor.COLOR_RED);
    public static final ClassicFluid.Pair CCL_FLUID = fluid("ccl", 8, 5, 1840, () -> SpaceLayers.CCL_BLOCK.get(), "CCL");
    public static final RegistryHandle<BlockSulfuricAcid> CCL_BLOCK = fluidBlock("ccl_block", CCL_FLUID, ModDamageTypes.LEAD, 1F, MapColor.COLOR_LIGHT_GRAY);

    /** NTM:Space's GenericFluid: quanta and tick delay from its viscosity, as NTM's sulfuric acid does. */
    private static ClassicFluid.Pair fluid(String name, int quanta, int delay, int density,
            Supplier<? extends BlockFluidClassicBase> block, String ntmFluid) {
        return Reg.classicFluid(name + "_fluid", new ClassicFluid.Spec(ClassicFluid.Physics.WATER, quanta, delay, density, true,
                Library.id("block/fluid/" + name), block, () -> ItemFluidBucket.make(NTMFluids.byName(ntmFluid))));
    }

    private static RegistryHandle<BlockSulfuricAcid> fluidBlock(String name, ClassicFluid.Pair fluid,
            ResourceKey<DamageType> type, float damage, MapColor color) {
        return Reg.block(name, props -> new BlockSulfuricAcid(fluid.source().get(), props, type, damage),
                () -> BlockBehaviour.Properties.of().mapColor(color).replaceable().noCollission().strength(0F, 300F)
                        .pushReaction(PushReaction.DESTROY).noLootTable().liquid());
    }

    public static final RegistryHandle<OilBarrel> OIL_BARREL =
            Reg.blockItem("oil_barrel", OilBarrel::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                    .sound(SoundType.METAL).strength(0.5F, 1.5F).noOcclusion().requiresCorrectToolForDrops())
                    .bakedBy(() -> new com.hbm.client.model.BarrelModel());
    public static final RegistryHandle<DetSalt> DET_SALT =
            Reg.blockItem("det_salt", DetSalt::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .sound(SoundType.METAL).strength(0.1F, 0.0F).requiresCorrectToolForDrops());

    private static final net.minecraft.world.level.block.state.properties.BlockSetType FENCE_GATE_SET = net.minecraft.world.level.block.state.properties.BlockSetType.register(new net.minecraft.world.level.block.state.properties.BlockSetType(
            "hbm_fence_gate", true, true, false, net.minecraft.world.level.block.state.properties.BlockSetType.PressurePlateSensitivity.EVERYTHING, SoundType.METAL,
            net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Library.id("block.fence_gate")), net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Library.id("block.fence_gate")), net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Library.id("block.fence_gate")), net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Library.id("block.fence_gate")),
            net.minecraft.sounds.SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, net.minecraft.sounds.SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
            net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_OFF, net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON));
    public static final RegistryHandle<FenceGate> FENCE_GATE =
            Reg.blockItem("fence_gate", props -> new FenceGate(FENCE_GATE_SET, props), () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .sound(SoundType.METAL).strength(5.0F, 0.6F).noOcclusion().requiresCorrectToolForDrops().pushReaction(PushReaction.DESTROY));

    private SpaceLayers() {}

    public static void init() {
        var anchor = Library.id("moon_turf");
        for (var b : new RegistryHandle<?>[] {CARBONIZED_FLESH_BLOCK, CHARRED_FLESH_BLOCK, FLESH_BLOCK, SALTED_FALLOUT})
            CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, anchor, () -> new ItemStack((Block) b.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.NUKES, Library.id("red_barrel"), () -> new ItemStack(OIL_BARREL.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.NUKES, Library.id("det_nuke"), () -> new ItemStack(DET_SALT.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, Library.id("door_metal"), () -> new ItemStack(FENCE_GATE.get()));
        var parts = Library.id("powder_coal_tiny");
        CreativeContents.insertAfter(CreativeContents.Tab.PARTS, parts, () -> new ItemStack(GRILLED_FLESH.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.PARTS, parts, () -> new ItemStack(FLESH_ITEM.get()));
    }

    static boolean hot(Level level) {
        CBT_Temperature t = CelestialBody.getTrait(level, CBT_Temperature.class);
        return t != null && t.degrees > 100;
    }

    /** Fallout that keeps adding 50 RAD to its chunk on every random tick. */
    public static class SaltedFallout extends BlockFallout {
        public SaltedFallout(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            RadiationSystemNT.incrementRad(level, pos, 50);
        }
    }

    /** An eighth-high layer of flesh: on worlds hotter than 100 degrees it chars, then carbonizes. */
    public static class MeltedFlesh extends Block {
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

        public MeltedFlesh(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            BlockState below = level.getBlockState(pos.below());
            if (below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE)) return false;
            return below.is(BlockTags.LEAVES) || below.isFaceSturdy(level, pos.below(), Direction.UP);
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
            return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            BlockState s = defaultBlockState();
            return s.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? s : null;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!hot(level)) return;
            if (this == FLESH_BLOCK.get()) level.setBlockAndUpdate(pos, CHARRED_FLESH_BLOCK.get().defaultBlockState());
            else if (this == CHARRED_FLESH_BLOCK.get()) level.setBlockAndUpdate(pos, CARBONIZED_FLESH_BLOCK.get().defaultBlockState());
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
            if (!hot(level)) return;
            double x = pos.getX() + rand.nextFloat(), y = pos.getY() + rand.nextFloat() * 0.5 + 0.5, z = pos.getZ() + rand.nextFloat();
            if (this == FLESH_BLOCK.get()) {
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
                level.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0, 0);
            } else if (this == CHARRED_FLESH_BLOCK.get()) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0, 0);
            }
        }
    }

    /** NTM:Space's oily barrel: a brown barrel for decoration, it never goes off. */
    public static class OilBarrel extends Block {
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

        public OilBarrel(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }
    }

    /** The salted nuclear charge: a det_nuke whose fallout is salted, so it keeps irradiating. */
    public static class DetSalt extends com.hbm.blocks.bomb.BlockDetNuke {
        public DetSalt(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        public com.hbm.interfaces.IBomb.BombReturnCode explode(Level level, BlockPos pos, net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity detonator) {
            if (com.hbm.items.weapon.GunContent.isDisabled(asItem())) return com.hbm.interfaces.IBomb.BombReturnCode.UNDEFINED;
            if (level.isClientSide()) return com.hbm.interfaces.IBomb.BombReturnCode.DETONATED;
            if (!com.hbm.blocks.bomb.TntRule.explodes(level)) return com.hbm.interfaces.IBomb.BombReturnCode.ERROR_TNT_DISABLED;
            level.removeBlock(pos, false);
            double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
            int r = com.hbm.data.ExplosionData.MISSILE_RADIUS.get();
            level.addFreshEntity(com.hbm.entity.logic.EntityNukeExplosionMK5.statFac(level, r, x, y, z).falloutLayer(SALTED_FALLOUT.get()));
            com.hbm.entity.effect.EntityNukeTorex.statFac(level, x, y, z, r);
            return com.hbm.interfaces.IBomb.BombReturnCode.DETONATED;
        }
    }

    /**
     * NTM:Space's metal fence gate: a door that slides aside instead of swinging. Closed it is a
     * one-pixel plate down the middle of the block; open it slides half a block over along the block's
     * edge, while its outline stays where the closed gate stands, as in NTM:Space.
     */
    public static class FenceGate extends net.minecraft.world.level.block.DoorBlock {
        private static final VoxelShape X_PLATE = Block.box(7.5, 0, 0, 8.5, 16, 16);
        private static final VoxelShape Z_PLATE = Block.box(0, 0, 7.5, 16, 16, 8.5);

        public FenceGate(net.minecraft.world.level.block.state.properties.BlockSetType type, BlockBehaviour.Properties props) {
            super(type, props);
        }

        private static VoxelShape plate(BlockState state) {
            return state.getValue(FACING).getAxis() == Direction.Axis.X ? X_PLATE : Z_PLATE;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return plate(state);
        }

        // NTM:Space's open gate is a one-pixel panel slid half a block along the block's edge
        // (func_150011_b): meta 0-3 are east, south, west, north; meta bit 16 the other hinge
        private static final VoxelShape[] OPEN_PANEL = {
            Block.box(8, 0, 0, 24, 16, 1), Block.box(8, 0, 15, 24, 16, 16), // east
            Block.box(15, 0, 8, 16, 16, 24), Block.box(0, 0, 8, 1, 16, 24), // south
            Block.box(-8, 0, 15, 8, 16, 16), Block.box(-8, 0, 0, 8, 16, 1), // west
            Block.box(0, 0, -8, 1, 16, 8), Block.box(15, 0, -8, 16, 16, 8) // north
        };

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            if (!state.getValue(OPEN)) return plate(state);
            int j = switch (state.getValue(FACING)) {
                case SOUTH -> 1;
                case WEST -> 2;
                case NORTH -> 3;
                default -> 0;
            };
            boolean otherSide = state.getValue(HINGE) == net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT;
            return OPEN_PANEL[j * 2 + (otherSide ? 1 : 0)];
        }
    }
}
