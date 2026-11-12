// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The 1.7 world NTM:Space's planet generation talks to: its seed and biome map always, and while
 * a chunk is being decorated the blocks of the region around it. Reads and writes outside that
 * region are ignored, as the region cannot reach them.
 */
public class World {

    public final long seed;
    public final WorldChunkManager chunkManager;
    public final Random rand = new Random();
    private WorldGenLevel level;
    /** The planet's ore metadata (its body) and its stone, as WorldProviderCelestial gave them. */
    public int bodyMeta;
    /** Whether bedrock ice generates, and what new bedrock ores take to drill. */
    public boolean hasIce;
    public com.hbm.inventory.fluid.FluidStackNTM bedrockAcid;
    public Block stone = Blocks.STONE;

    public World(long seed, WorldChunkManager chunkManager) {
        this.seed = seed;
        this.chunkManager = chunkManager;
    }

    /** The same world, writing into a decoration region. */
    public World in(WorldGenLevel level) {
        World w = new World(seed, chunkManager);
        w.level = level;
        w.bodyMeta = bodyMeta;
        w.stone = stone;
        return w;
    }

    public WorldGenLevel level() {
        return level;
    }

    public int bodyMeta() {
        return bodyMeta;
    }

    public Block stone() {
        return stone;
    }

    public long getSeed() {
        return seed;
    }

    public WorldChunkManager getWorldChunkManager() {
        return chunkManager;
    }

    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return chunkManager == null ? null : chunkManager.getBiomeGenAt(x, z);
    }

    public int getHeight() {
        return 256;
    }

    private boolean reachable(int x, int y, int z) {
        if (level == null || y < 0 || y > 255) return false;
        BlockPos pos = new BlockPos(x, y, z);
        return level.hasChunk(x >> 4, z >> 4) && level.ensureCanWrite(pos);
    }

    public boolean blockExists(int x, int y, int z) {
        return reachable(x, y, z);
    }

    public BlockState getState(int x, int y, int z) {
        return reachable(x, y, z) ? level.getBlockState(new BlockPos(x, y, z)) : Blocks.AIR.defaultBlockState();
    }

    public Block getBlock(int x, int y, int z) {
        return LegacyBlocks.block(getState(x, y, z));
    }

    public int getBlockMetadata(int x, int y, int z) {
        return LegacyBlocks.meta(getState(x, y, z));
    }

    public boolean isAirBlock(int x, int y, int z) {
        return getState(x, y, z).isAir();
    }

    public boolean setBlock(int x, int y, int z, Block block) {
        return setBlock(x, y, z, block, 0, 3);
    }

    public boolean setBlock(int x, int y, int z, Block block, int meta, int flags) {
        return setState(x, y, z, LegacyBlocks.state(block, meta));
    }

    public boolean setState(int x, int y, int z, BlockState state) {
        if (!reachable(x, y, z)) return false;
        return level.setBlock(new BlockPos(x, y, z), state, 2);
    }

    public boolean setBlockToAir(int x, int y, int z) {
        return setBlock(x, y, z, Blocks.AIR);
    }

    public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flags) {
        return setBlock(x, y, z, getBlock(x, y, z), meta, flags);
    }

    public void markBlockForUpdate(int x, int y, int z) {}

    /** Lets a placed liquid start flowing, as 1.7's immediate update did. */
    public void flowLiquid(int x, int y, int z) {
        if (!reachable(x, y, z)) return;
        var pos = new BlockPos(x, y, z);
        var fluid = level.getFluidState(pos);
        if (!fluid.isEmpty()) level.scheduleTick(pos, fluid.getType(), 0);
    }

    public BlockEntity getTileEntity(int x, int y, int z) {
        return reachable(x, y, z) ? level.getBlockEntity(new BlockPos(x, y, z)) : null;
    }

    /** The height of the first block light cannot pass, from the top. */
    public int getHeightValue(int x, int z) {
        return level == null ? 0 : level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    public int getTopSolidOrLiquidBlock(int x, int z) {
        return level == null ? 0 : level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
    }

    public long getTotalWorldTime() {
        return 0;
    }
}
