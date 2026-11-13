// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.legacy;

import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * NTM:Space's ChunkProviderCelestial: 1.7's overworld terrain shaper, its noise and stone, sea
 * and air fill tuned per planet, with each biome then laying its own surface over the stone.
 * Chunks come out as 1.7 block buffers (x, then z, then y), which a planet's carvers work on
 * before {@link com.hbm.space.dim.CelestialChunkGenerator} copies them into the chunk.
 */
public abstract class ChunkProviderCelestial {

    /** The planets' build range, the modern world's: y -64 up to 320, 48 terrain cells of 8. */
    public static final int MIN_Y = -64, MAX_Y = 320, H = MAX_Y - MIN_Y;
    private static final int CELLS = H / 8, CELL_OFFSET = MIN_Y / 8;
    /** How much taller than 1.7's the land above the sea stands: the same shapes, stretched upward. */
    protected double heightStretch = 1.5D;

    /** A buffer index, x (or 1.7's swapped z) first, then the other, then world y. */
    public static int idx(int a, int b, int y) {
        return ((a << 4) | b) * H + (y - MIN_Y);
    }

    public Block stone() {
        return stoneBlock;
    }

    protected Block stoneBlock = Blocks.STONE;
    protected Block seaBlock = Blocks.AIR;
    protected int seaLevel = 63;

    protected Vec3 firstOrderFreq = new Vec3(684.412D, 684.412D, 684.412D);
    protected Vec3 secondOrderFreq = new Vec3(684.412D, 684.412D, 684.412D);
    protected Vec3 thirdOrderFreq = new Vec3(8.555150000000001D, 4.277575000000001D, 8.555150000000001D);
    protected Vec3 heightOrderFreq = new Vec3(200.0D, 200.0D, 0.5D);
    protected boolean amplified = false;
    protected boolean reclamp = true;

    protected World worldObj;
    /** The biomes of the chunk being generated, for planets that read them back. */
    protected BiomeGenBase[] biomesForGeneration;
    protected Random rand;

    private final Noise.Octaves firstOrder;
    private final Noise.Octaves secondOrder;
    private final Noise.Octaves thirdOrder;
    private final Noise.Perlin perlin;
    private final Noise.Octaves heightOrder;
    private final float[] parabolicField = new float[25];

    public ChunkProviderCelestial(World world, long seed) {
        this.worldObj = world;
        this.rand = new Random(seed);
        firstOrder = new Noise.Octaves(rand, 16);
        secondOrder = new Noise.Octaves(rand, 16);
        thirdOrder = new Noise.Octaves(rand, 8);
        perlin = new Noise.Perlin(rand, 4);
        heightOrder = new Noise.Octaves(rand, 16);
        for (int j = -2; j <= 2; ++j)
            for (int k = -2; k <= 2; ++k)
                parabolicField[j + 2 + (k + 2) * 5] = 10.0F / Mth.sqrt((float) (j * j + k * k) + 0.2F);
    }

    /** What a flat world's layers become here; planets are never flat, so unused. */
    protected Block getFlatWorldBlock(Block block) {
        return block;
    }

    /** A chunk's blocks, generated from scratch; one buffer per call, so threads don't share. */
    public BlockMetaBuffer getChunkPrimer(int x, int z) {
        BlockMetaBuffer buffer = new BlockMetaBuffer();
        Random random = new Random((long) x * 341873128712L + (long) z * 132897987541L);
        buffer.rand = random;
        generateBlocks(x, z, buffer.blocks);
        BiomeGenBase[] biomes = worldObj.getWorldChunkManager().loadBlockGeneratorData(null, x * 16, z * 16, 16, 16);
        biomesForGeneration = biomes;
        replaceBlocksForBiome(x, z, buffer.blocks, buffer.metas, biomes, random);
        return buffer;
    }

    protected void generateBlocks(int x, int z, Block[] blocks) {
        BiomeGenBase[] biomes = worldObj.getWorldChunkManager().getBiomesForGeneration(null, x * 4 - 2, z * 4 - 2, 10, 10);
        double[] terrain = generateNoiseField(x * 4, CELL_OFFSET, z * 4, biomes);
        for (int k = 0; k < 4; ++k) {
            int l = k * 5;
            int i1 = (k + 1) * 5;
            for (int j1 = 0; j1 < 4; ++j1) {
                int k1 = (l + j1) * (CELLS + 1);
                int l1 = (l + j1 + 1) * (CELLS + 1);
                int i2 = (i1 + j1) * (CELLS + 1);
                int j2 = (i1 + j1 + 1) * (CELLS + 1);
                for (int k2 = 0; k2 < CELLS; ++k2) {
                    double d1 = terrain[k1 + k2];
                    double d2 = terrain[l1 + k2];
                    double d3 = terrain[i2 + k2];
                    double d4 = terrain[j2 + k2];
                    double d5 = (terrain[k1 + k2 + 1] - d1) * 0.125D;
                    double d6 = (terrain[l1 + k2 + 1] - d2) * 0.125D;
                    double d7 = (terrain[i2 + k2 + 1] - d3) * 0.125D;
                    double d8 = (terrain[j2 + k2 + 1] - d4) * 0.125D;
                    for (int l2 = 0; l2 < 8; ++l2) {
                        double d10 = d1;
                        double d11 = d2;
                        double d12 = (d3 - d1) * 0.25D;
                        double d13 = (d4 - d2) * 0.25D;
                        for (int i3 = 0; i3 < 4; ++i3) {
                            int wy = k2 * 8 + l2 + MIN_Y;
                            int j3 = idx(i3 + k * 4, j1 * 4, wy) - H;
                            double d16 = (d11 - d10) * 0.25D;
                            double d15 = d10 - d16;
                            for (int k3 = 0; k3 < 4; ++k3) {
                                if ((d15 += d16) > 0.0D) blocks[j3 += H] = stoneBlock;
                                else if (wy < seaLevel) blocks[j3 += H] = seaBlock;
                                else blocks[j3 += H] = null;
                            }
                            d10 += d12;
                            d11 += d13;
                        }
                        d1 += d5;
                        d2 += d6;
                        d3 += d7;
                        d4 += d8;
                    }
                }
            }
        }
    }

    protected double[] generateNoiseField(int x, int y, int z, BiomeGenBase[] biomes) {
        double[] first = firstOrder.generateNoiseOctaves(null, x, y, z, 5, CELLS + 1, 5, firstOrderFreq.x, firstOrderFreq.y, firstOrderFreq.z);
        double[] second = secondOrder.generateNoiseOctaves(null, x, y, z, 5, CELLS + 1, 5, secondOrderFreq.x, secondOrderFreq.y, secondOrderFreq.z);
        double[] third = thirdOrder.generateNoiseOctaves(null, x, y, z, 5, CELLS + 1, 5, thirdOrderFreq.x, thirdOrderFreq.y, thirdOrderFreq.z);
        double[] height = heightOrder.generateNoiseOctaves(null, x, z, 5, 5, heightOrderFreq.x, heightOrderFreq.y, heightOrderFreq.z);
        double[] terrain = new double[25 * (CELLS + 1)];
        double[] column = new double[CELLS + 1];
        int l = 0;
        int i1 = 0;
        for (int j1 = 0; j1 < 5; ++j1) {
            for (int k1 = 0; k1 < 5; ++k1) {
                float f = 0.0F, f1 = 0.0F, f2 = 0.0F;
                BiomeGenBase centre = biomes[j1 + 2 + (k1 + 2) * 10];
                for (int l1 = -2; l1 <= 2; ++l1) {
                    for (int i2 = -2; i2 <= 2; ++i2) {
                        BiomeGenBase biome = biomes[j1 + l1 + 2 + (k1 + i2 + 2) * 10];
                        float f3 = biome.rootHeight;
                        float f4 = biome.heightVariation;
                        if (amplified && f3 > 0.0F) {
                            f3 = 1.0F + f3 * 2.0F;
                            f4 = 1.0F + f4 * 4.0F;
                        }
                        float f5 = parabolicField[l1 + 2 + (i2 + 2) * 5] / (f3 + 2.0F);
                        if (biome.rootHeight > centre.rootHeight) f5 /= 2.0F;
                        f += f4 * f5;
                        f1 += f3 * f5;
                        f2 += f5;
                    }
                }
                f /= f2;
                f1 /= f2;
                f = f * 0.9F + 0.1F;
                f1 = (f1 * 4.0F - 1.0F) / 8.0F;
                double d12 = height[i1] / 8000.0D;
                if (d12 < 0.0D) d12 = -d12 * 0.3D;
                d12 = d12 * 3.0D - 2.0D;
                if (d12 < 0.0D) {
                    d12 /= 2.0D;
                    if (d12 < -1.0D) d12 = -1.0D;
                    d12 /= 1.4D;
                    d12 /= 2.0D;
                } else {
                    if (d12 > 1.0D) d12 = 1.0D;
                    d12 /= 8.0D;
                }
                ++i1;
                double d13 = f1;
                double d14 = f;
                d13 += d12 * 0.2D;
                d13 = d13 * 8.5D / 8.0D;
                double d5 = 8.5D + d13 * 4.0D;
                // 1.7's column as it was, cell by cell from the old y 0 (and on down to the new floor)
                int base = l;
                for (int n = 0; n <= CELLS; ++n) {
                    int j2 = n + CELL_OFFSET;
                    double d6 = ((double) j2 - d5) * 12.0D * 128.0D / 256.0D / d14;
                    if (d6 < 0.0D) d6 *= 4.0D;
                    double d7 = first[l] / 512.0D;
                    double d8 = second[l] / 512.0D;
                    double d9 = (third[l] / 10.0D + 1.0D) / 2.0D;
                    double d10 = reclamp ? Mth.clampedLerp(d7, d8, d9) - d6 : d8 - d6;
                    if (j2 > 29) {
                        double d11 = Math.min(1.0D, (float) (j2 - 29) / 3.0F);
                        d10 = d10 * (1.0D - d11) + -10.0D * d11;
                    }
                    column[n] = d10;
                    ++l;
                }
                // then stretched upward from the sea: each new height reads the old one it stands for
                for (int n = 0; n <= CELLS; ++n) {
                    double wy = n * 8 + MIN_Y;
                    double old = wy > seaLevel ? seaLevel + (wy - seaLevel) / heightStretch : wy;
                    double c = (old - MIN_Y) / 8.0D;
                    int i0 = Math.min((int) Math.floor(c), CELLS - 1);
                    double d10 = Mth.lerp(c - i0, column[i0], column[i0 + 1]);
                    // the rare peak that would pass the build limit rounds off under it
                    if (n > CELLS - 3) {
                        double d11 = (n - (CELLS - 3)) / 3.0D;
                        d10 = d10 * (1.0D - d11) + -10.0D * d11;
                    }
                    terrain[base + n] = d10;
                }
            }
        }
        return terrain;
    }

    protected void replaceBlocksForBiome(int x, int z, Block[] blocks, byte[] metas, BiomeGenBase[] biomes) {
        replaceBlocksForBiome(x, z, blocks, metas, biomes, rand);
    }

    /** The creatures that may spawn at a spot; the biome's own, unless a planet says otherwise. */
    public java.util.List<?> getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
        return java.util.List.of();
    }

    protected void replaceBlocksForBiome(int x, int z, Block[] blocks, byte[] metas, BiomeGenBase[] biomes, Random random) {
        double d0 = 0.03125D;
        double[] stoneNoise = perlin.func_151599_a(null, x * 16, z * 16, 16, 16, d0 * 2.0D, d0 * 2.0D, 1.0D);
        for (int k = 0; k < 16; ++k)
            for (int l = 0; l < 16; ++l)
                biomes[l + k * 16].genTerrainBlocks(worldObj, random, blocks, metas, x * 16 + k, z * 16 + l, stoneNoise[l + k * 16]);
    }

    /** The biome's decoration of a chunk, into the region of the world around it. */
    public void populate(World world, int x, int z) {
        int k = x * 16;
        int l = z * 16;
        BiomeGenBase biome = world.getBiomeGenForCoords(k + 16, l + 16);
        Random random = new Random(world.getSeed());
        long i1 = random.nextLong() / 2L * 2L + 1L;
        long j1 = random.nextLong() / 2L * 2L + 1L;
        random.setSeed((long) x * i1 + (long) z * j1 ^ world.getSeed());
        biome.decorate(world, random, k, l);
    }

    private net.minecraft.world.level.levelgen.synth.SimplexNoise cheese, cheeseDetail, noodleA, noodleB;

    private void caveNoises() {
        if (cheese != null) return;
        var random = net.minecraft.util.RandomSource.create(worldObj.getSeed() ^ 0x6A09E667F3BCC909L ^ getClass().getName().hashCode());
        cheese = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        cheeseDetail = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        noodleA = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
        noodleB = new net.minecraft.world.level.levelgen.synth.SimplexNoise(random);
    }

    private static final int GRID_Y = H / 8 + 1;

    /**
     * The modern pass over a finished 1.7 chunk: cheese caverns and winding spaghetti tunnels
     * through the rock from the deep up to under the surface, lava pooled at the bottom as the
     * modern overworld's. 1.7's own carvers have
     * already run, so the old caves and the new ones cross.
     */
    public void modernize(int chunkX, int chunkZ, BlockMetaBuffer buffer) {
        caveNoises();
        Block[] blocks = buffer.blocks;
        // the noise on a coarse grid (4 by 8 by 4), filled in between as the terrain's is
        double[] c = new double[25 * GRID_Y], na = new double[25 * GRID_Y], nb = new double[25 * GRID_Y];
        for (int gx = 0; gx < 5; gx++)
            for (int gz = 0; gz < 5; gz++)
                for (int gy = 0; gy < GRID_Y; gy++) {
                    double wx = chunkX * 16 + gx * 4, wy = MIN_Y + gy * 8, wz = chunkZ * 16 + gz * 4;
                    int i = (gx * 5 + gz) * GRID_Y + gy;
                    c[i] = cheese.getValue(wx / 110D, wy / 55D, wz / 110D) * 0.75D + cheeseDetail.getValue(wx / 36D, wy / 22D, wz / 36D) * 0.25D;
                    na[i] = noodleA.getValue(wx / 60D, wy / 42D, wz / 60D);
                    nb[i] = noodleB.getValue(wx / 60D, wy / 42D, wz / 60D);
                }
        // each column's ground, and whether a sea (liquid, or frozen over) stands over it
        int[] tops = new int[256];
        boolean[] wets = new boolean[256];
        int[] wetLimits = new int[256];
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                int top = MIN_Y;
                boolean wet = false;
                for (int y = MAX_Y - 1; y >= MIN_Y; y--) {
                    Block b = blocks[idx(x, z, y)];
                    if (b == null || b == Blocks.AIR) continue;
                    if (isLiquid(b) || b.defaultBlockState().is(net.minecraft.tags.BlockTags.ICE)) {
                        wet = true;
                        continue;
                    }
                    top = y;
                    break;
                }
                tops[x << 4 | z] = top;
                wets[x << 4 | z] = wet;
                wetLimits[x << 4 | z] = wet ? top - 20 : Integer.MAX_VALUE;
            }
        // a sea in the next chunk: its columns from the bare terrain, the same whichever chunk asks first
        boolean seas = isLiquid(seaBlock);
        int[] west = seas ? wetLimits(chunkX - 1, chunkZ) : null, east = seas ? wetLimits(chunkX + 1, chunkZ) : null;
        int[] north = seas ? wetLimits(chunkX, chunkZ - 1) : null, south = seas ? wetLimits(chunkX, chunkZ + 1) : null;
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                int top = tops[x << 4 | z];
                boolean wet = wets[x << 4 | z];
                // no cave beside a sea column higher than that column's own caves reach
                int beside = Math.min(
                        Math.min(x > 0 ? wetLimits[(x - 1) << 4 | z] : west != null ? west[15 << 4 | z] : Integer.MAX_VALUE,
                                x < 15 ? wetLimits[(x + 1) << 4 | z] : east != null ? east[z] : Integer.MAX_VALUE),
                        Math.min(z > 0 ? wetLimits[x << 4 | (z - 1)] : north != null ? north[x << 4 | 15] : Integer.MAX_VALUE,
                                z < 15 ? wetLimits[x << 4 | (z + 1)] : south != null ? south[x << 4] : Integer.MAX_VALUE));
                int cheeseTop = Math.min(top - (wet ? 20 : 14), beside), noodleTop = Math.min(top - (wet ? 20 : 3), beside);
                int gx = x >> 2, gz = z >> 2;
                double fx = (x & 3) / 4D, fz = (z & 3) / 4D;
                for (int y = MIN_Y + 5; y <= Math.max(cheeseTop, noodleTop); y++) {
                    int i = idx(x, z, y);
                    if (blocks[i] != stoneBlock) continue;
                    int gy = (y - MIN_Y) >> 3;
                    double fy = ((y - MIN_Y) & 7) / 8D;
                    // fewer, smaller openings near the ground
                    double fade = Math.max(0D, (y - (top - 40)) / 40D);
                    boolean carve = y <= cheeseTop && lerp(c, gx, gy, gz, fx, fy, fz) > 0.5D + fade * 0.35D;
                    if (!carve && y <= noodleTop) {
                        double a = lerp(na, gx, gy, gz, fx, fy, fz), b = lerp(nb, gx, gy, gz, fx, fy, fz);
                        carve = a * a + b * b < 0.0045D * (1D - fade * 0.6D);
                    }
                    if (carve) {
                        blocks[i] = y < MIN_Y + 10 ? Blocks.LAVA : null;
                        buffer.metas[i] = 0;
                    }
                }
            }
    }

    private static boolean isLiquid(Block b) {
        return b != null && (b.defaultBlockState().liquid() || !b.defaultBlockState().getFluidState().isEmpty());
    }

    /** Recent chunks' sea columns from bare terrain, as neighbouring chunks ask for them repeatedly. */
    private final java.util.Map<Long, int[]> wetLimitCache = new java.util.LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Long, int[]> eldest) {
            return size() > 32;
        }
    };

    /**
     * For each column of a chunk's bare terrain (no surface or carvers), the highest a cave may
     * reach beside it: 20 under the sea floor where the sea stands, otherwise no limit.
     */
    private int[] wetLimits(int chunkX, int chunkZ) {
        long key = net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ);
        synchronized (wetLimitCache) {
            int[] cached = wetLimitCache.get(key);
            if (cached != null) return cached;
        }
        Block[] terrain = new Block[16 * 16 * H];
        generateBlocks(chunkX, chunkZ, terrain);
        int[] limits = new int[256];
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                int limit = Integer.MAX_VALUE;
                boolean wet = false;
                for (int y = MAX_Y - 1; y >= MIN_Y; y--) {
                    Block b = terrain[idx(x, z, y)];
                    if (b == null || b == Blocks.AIR) continue;
                    if (isLiquid(b)) {
                        wet = true;
                        continue;
                    }
                    if (wet) limit = y - 20;
                    break;
                }
                limits[x << 4 | z] = limit;
            }
        synchronized (wetLimitCache) {
            wetLimitCache.put(key, limits);
        }
        return limits;
    }

    private static double lerp(double[] g, int gx, int gy, int gz, double fx, double fy, double fz) {
        int i00 = (gx * 5 + gz) * GRID_Y + gy, i10 = ((gx + 1) * 5 + gz) * GRID_Y + gy;
        int i01 = (gx * 5 + gz + 1) * GRID_Y + gy, i11 = ((gx + 1) * 5 + gz + 1) * GRID_Y + gy;
        return net.minecraft.util.Mth.lerp3(fx, fy, fz,
                g[i00], g[i10], g[i00 + 1], g[i10 + 1],
                g[i01], g[i11], g[i01 + 1], g[i11 + 1]);
    }

    public static class BlockMetaBuffer {
        public final Block[] blocks = new Block[16 * 16 * H];
        public final byte[] metas = new byte[16 * 16 * H];
        public Random rand;
    }
}
