// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.atmosphere;

import com.hbm.space.SpaceToggle;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.space.CelestialBody;
import com.hbm.space.trait.CBT_Atmosphere;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space's ChunkAtmosphereHandler: the sealed volumes air vents fill, the atmosphere at any
 * block (the body's, with the volumes it sits in added on), and what that atmosphere lets exist
 * there: torches and fire need oxygen, water needs pressure, plants need air.
 */
public final class SpaceAtmosphere {

    static final int MAX_BLOB_RADIUS = 256;
    /**
     * How much CO2 growing turns to O2, balanced around one pressurized room living off
     * 25 trees (955s to grow) or 200 crops (300s a stage).
     */
    public static final int TREE_GROWTH_CONVERSION = 400;
    public static final int CROP_GROWTH_CONVERSION = 15;
    public static final int GRASS_GROWTH_CONVERSION = 100;

    private static final Map<ServerLevel, LinkedHashMap<IAtmosphereProvider, AtmosphereBlob>> BLOBS = new WeakHashMap<>();
    private static final Map<ServerLevel, ArrayDeque<Change>> CHANGES = new WeakHashMap<>();
    private static final Map<ServerLevel, ArrayDeque<Growth>> GROWTHS = new WeakHashMap<>();

    private record Change(BlockPos pos, boolean wasSealed) {}

    private record Growth(@Nullable Block from, Block into, BlockPos pos, int time) {}

    private SpaceAtmosphere() {}

    private static LinkedHashMap<IAtmosphereProvider, AtmosphereBlob> blobs(ServerLevel level) {
        return BLOBS.computeIfAbsent(level, l -> new LinkedHashMap<>());
    }

    /** The body's own atmosphere, a copy safe to change; none in orbit or off the solar system. */
    private static CBT_Atmosphere celestial(Level level) {
        if (CelestialBody.inOrbit(level)) return new CBT_Atmosphere();
        CelestialBody body = CelestialBody.getBodyOrNull(level.dimension());
        if (body == null) return earth();
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        return atmosphere == null ? new CBT_Atmosphere() : atmosphere.copy();
    }

    /** Dimensions off the solar system (the Nether, other mods') keep Earth's air. */
    private static CBT_Atmosphere earth() {
        return new CBT_Atmosphere(NTMFluids.byName("EARTHAIR"), 1.0);
    }

    public static @Nullable CBT_Atmosphere getAtmosphere(Entity entity) {
        return getAtmosphere(entity.level(), BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ()), null);
    }

    public static @Nullable CBT_Atmosphere getAtmosphere(Level level, BlockPos pos) {
        return getAtmosphere(level, pos, null);
    }

    public static @Nullable CBT_Atmosphere getAtmosphere(Level level, BlockPos pos, @Nullable AtmosphereBlob exclude) {
        CBT_Atmosphere atmosphere = celestial(level);
        if (level instanceof ServerLevel server) {
            for (AtmosphereBlob blob : blobs(server).values()) {
                if (blob == exclude || !blob.contains(pos)) continue;
                double pressure = blob.handler.getFluidPressure();
                if (pressure < 0) atmosphere.reduce(-pressure);
                else atmosphere.add(blob.handler.getFluidType(), pressure);
            }
        }
        return atmosphere.fluids.isEmpty() ? null : atmosphere;
    }

    public static List<AtmosphereBlob> getBlobs(ServerLevel level, BlockPos pos) {
        List<AtmosphereBlob> in = new ArrayList<>();
        for (AtmosphereBlob blob : blobs(level).values()) if (blob.contains(pos)) in.add(blob);
        return in;
    }

    public static boolean hasAtmosphere(ServerLevel level, BlockPos pos) {
        for (AtmosphereBlob blob : blobs(level).values()) if (blob.contains(pos)) return true;
        return false;
    }

    private static List<AtmosphereBlob> within(ServerLevel level, BlockPos pos, int radius) {
        List<AtmosphereBlob> list = new ArrayList<>();
        long r = (long) radius * radius;
        for (AtmosphereBlob blob : blobs(level).values()) if (blob.getRootPosition().distSqr(pos) <= r) list.add(blob);
        return list;
    }

    /** 21% Earth air or 9% oxygen. */
    public static boolean canBreathe(@Nullable CBT_Atmosphere atmosphere) {
        return atmosphere != null
                && (atmosphere.hasFluid(NTMFluids.byName("EARTHAIR"), 0.21) || atmosphere.hasFluid(NTMFluids.OXYGEN, 0.09));
    }

    /** Enough pressure to keep liquids from boiling off. */
    public static boolean hasLiquidPressure(@Nullable CBT_Atmosphere atmosphere) {
        return atmosphere != null && atmosphere.getPressure() >= 0.19D;
    }

    public static boolean willCorrode(@Nullable CBT_Atmosphere atmosphere) {
        return atmosphere != null && atmosphere.hasTrait(FT_Corrosive.class, 0.2);
    }

    /** Takes air for a burning machine from the body, or a volume it sits in. */
    public static boolean breatheAir(ServerLevel level, BlockPos pos, int amount) {
        if (!CelestialBody.inOrbit(level)) {
            CBT_Atmosphere atmosphere = celestial(level);
            if (atmosphere.hasFluid(NTMFluids.byName("EARTHAIR"), 0.19) || atmosphere.hasFluid(NTMFluids.OXYGEN, 0.09)) return true;
        }
        for (AtmosphereBlob blob : getBlobs(level, pos)) {
            var h = blob.handler;
            boolean air = (h.getFluidType() == NTMFluids.byName("EARTHAIR") && h.getFluidPressure() >= 0.19)
                    || (h.getFluidType() == NTMFluids.OXYGEN && h.getFluidPressure() >= 0.09);
            if (air) {
                blob.consume(amount);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes a block the atmosphere where it stands can't keep, dropping it; true if it did.
     * Only blocks that care are judged: torches and fire (oxygen), water (pressure) and plants.
     */
    static boolean runEffectsOnBlock(@Nullable CBT_Atmosphere atmosphere, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        boolean needsPressure = state.is(Blocks.WATER);
        boolean litBurner = SpaceCombustion.isLitBurner(block);
        boolean needsO2 = (block instanceof TorchBlock && !(block instanceof RedstoneTorchBlock)) || block instanceof BaseFireBlock
                || litBurner;
        boolean needsAir = block instanceof BushBlock;
        if (!needsPressure && !needsO2 && !needsAir) return false;
        boolean canExist;
        if (needsO2) canExist = canBreathe(atmosphere) || (atmosphere != null && atmosphere.hasFluid(NTMFluids.OXYGEN, 0.09));
        else if (needsPressure) canExist = hasLiquidPressure(atmosphere);
        else if (block instanceof IPlantableBreathing breathing) canExist = breathing.canBreathe(atmosphere);
        else canExist = atmosphere != null
                && (atmosphere.hasFluid(NTMFluids.OXYGEN, 0.01) || atmosphere.hasFluid(NTMFluids.byName("EARTHAIR"), 0.1));
        if (canExist) return false;
        if (litBurner) {
            if (!SpaceToggle.changes()) return false;
            level.setBlock(pos, SpaceCombustion.emptyBurner(), 3);
            return true;
        }
        level.destroyBlock(pos, !(block instanceof BaseFireBlock) && !needsPressure);
        if (needsPressure) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return true;
    }

    public static void registerAtmosphere(IAtmosphereProvider handler) {
        var blobs = blobs(handler.getAtmosphereLevel());
        if (blobs.containsKey(handler)) return;
        AtmosphereBlob blob = new AtmosphereBlob(handler);
        blobs.put(handler, blob);
        blob.addBlock(handler.getRootPosition());
    }

    public static void unregisterAtmosphere(IAtmosphereProvider handler) {
        blobs(handler.getAtmosphereLevel()).remove(handler);
    }

    /** For an {@link IBlockSealable} that opened or closed. */
    public static void sealChanged(ServerLevel level, BlockPos pos) {
        if (AtmosphereBlob.isBlockSealed(level, pos)) sealed(level, pos);
        else opened(level, pos);
    }

    private static void sealed(ServerLevel level, BlockPos pos) {
        for (AtmosphereBlob blob : within(level, pos, MAX_BLOB_RADIUS)) {
            if (blob.contains(pos)) {
                blob.removeBlock(pos);
            } else if (!blob.contains(blob.getRootPosition())) {
                blob.runDepress = false;
                blob.addBlock(blob.getRootPosition());
            }
        }
    }

    private static void opened(ServerLevel level, BlockPos pos) {
        for (AtmosphereBlob blob : within(level, pos, MAX_BLOB_RADIUS)) {
            for (Direction dir : Direction.values()) {
                if (blob.contains(pos.relative(dir))) {
                    blob.runDepress = true;
                    blob.depressDir = dir;
                    blob.addBlock(pos);
                    break;
                }
            }
        }
    }

    /**
     * Every block change on the server, from the chunk hook. Volumes react once a tick, so a
     * machine that rebuilds itself or an explosion floods each volume once rather than per block.
     */
    public static void blockChanged(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        boolean tracked = BLOBS.containsKey(level) && !BLOBS.get(level).isEmpty();
        boolean judged = newState.is(Blocks.WATER) || newState.getBlock() instanceof TorchBlock
                || newState.getBlock() instanceof BaseFireBlock || newState.getBlock() instanceof BushBlock
                || SpaceCombustion.isLitBurner(newState.getBlock());
        if (!tracked && !judged) return;
        boolean wasSealed = tracked && wasSealed(level, pos, oldState);
        CHANGES.computeIfAbsent(level, l -> new ArrayDeque<>()).add(new Change(pos.immutable(), wasSealed));
    }

    private static boolean wasSealed(ServerLevel level, BlockPos pos, BlockState old) {
        if (old.isAir() || old.liquid() || !old.isSolid()) return false;
        if (old.getBlock() instanceof IBlockSealable) return true;
        // a door's cell was shut if it was closed, however thin the panel
        if (old.getBlock() instanceof com.hbm.blocks.multiblock.BlockMultiblockGeometryCell) {
            BlockPos core = com.hbm.blocks.multiblock.MultiblockSurface.coreOfAny(level, pos, old);
            if (core != null && level.getBlockState(core).getBlock() instanceof com.hbm.blocks.generic.BlockDoorGeneric)
                return !com.hbm.blocks.multiblock.BlockMultiblockGeometryCell.isOpenCell(old);
        }
        return old.isCollisionShapeFullBlock(level, pos);
    }

    public static void tick(ServerLevel level) {
        ArrayDeque<Change> changes = CHANGES.get(level);
        if (changes != null && !changes.isEmpty()) {
            Change change;
            while ((change = changes.poll()) != null) {
                BlockPos pos = change.pos();
                boolean sealed = AtmosphereBlob.isBlockSealed(level, pos);
                if (sealed && !change.wasSealed()) sealed(level, pos);
                else if (!sealed && change.wasSealed()) opened(level, pos);
                if (!runEffectsOnBlock(getAtmosphere(level, pos), level, pos)) trackPlant(level, pos);
            }
        }
        tickTerraforming(level);
        if (level.getGameTime() % 20 == 0) for (AtmosphereBlob blob : blobs(level).values()) blob.checkGrowth();
    }

    public static void onTreeGrow(ServerLevel level, BlockPos pos) {
        for (AtmosphereBlob blob : within(level, pos, MAX_BLOB_RADIUS)) {
            if (blob.contains(pos)) {
                blob.produce(TREE_GROWTH_CONVERSION);
                return;
            }
        }
    }

    private static void trackPlant(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock)) return;
        for (AtmosphereBlob blob : within(level, pos, MAX_BLOB_RADIUS)) if (blob.contains(pos)) blob.addPlant(level, pos);
    }

    /** Spreads one block into another from a spot, as seeding or terraforming does. */
    public static void addGrowth(ServerLevel level, @Nullable Block from, Block into, BlockPos pos, int count, int size) {
        var queue = GROWTHS.computeIfAbsent(level, l -> new ArrayDeque<>());
        for (int i = 0; i < count; i++) queue.add(new Growth(from, into, pos.immutable(), size));
    }

    private static boolean grows(@Nullable Block from, BlockState state) {
        if (from != null) return state.is(from);
        return state.is(Blocks.DIRT) || state.is(ModBlocks.WASTE_EARTH.get()) || state.is(ModBlocks.DIRT_DEAD.get())
                || state.is(ModBlocks.DIRT_OILY.get());
    }

    private static void tickTerraforming(ServerLevel level) {
        ArrayDeque<Growth> growths = GROWTHS.get(level);
        if (growths == null) return;
        for (int g = 0; g < 64; g++) {
            Growth growth = growths.poll();
            if (growth == null) return;
            outer:
            for (int i = 0; i < 4; i++) {
                Direction dir = Direction.from2DDataValue(level.random.nextInt(4));
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos at = growth.pos().relative(dir).above(dy);
                    if (!grows(growth.from(), level.getBlockState(at))) continue;
                    level.setBlockAndUpdate(at, growth.into().defaultBlockState());
                    if (growth.into() == Blocks.GRASS_BLOCK) level.levelEvent(2005, at.above(), 0);
                    if (growth.time() > 0) growths.add(new Growth(growth.from(), growth.into(), at, growth.time() - 1));
                    break outer;
                }
            }
        }
    }

    static boolean isCorrosive(Fluid fluid) {
        return NTMFluidProperties.hasTrait(fluid, FT_Corrosive.class);
    }
}
