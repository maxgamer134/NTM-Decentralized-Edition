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
        double[] terrain = generateNoiseField(x * 4, 0, z * 4, biomes);
        for (int k = 0; k < 4; ++k) {
            int l = k * 5;
            int i1 = (k + 1) * 5;
            for (int j1 = 0; j1 < 4; ++j1) {
                int k1 = (l + j1) * 33;
                int l1 = (l + j1 + 1) * 33;
                int i2 = (i1 + j1) * 33;
                int j2 = (i1 + j1 + 1) * 33;
                for (int k2 = 0; k2 < 32; ++k2) {
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
                            int j3 = (i3 + k * 4) << 12 | (j1 * 4) << 8 | (k2 * 8 + l2);
                            j3 -= 256;
                            double d16 = (d11 - d10) * 0.25D;
                            double d15 = d10 - d16;
                            for (int k3 = 0; k3 < 4; ++k3) {
                                if ((d15 += d16) > 0.0D) blocks[j3 += 256] = stoneBlock;
                                else if (k2 * 8 + l2 < seaLevel) blocks[j3 += 256] = seaBlock;
                                else blocks[j3 += 256] = null;
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
        double[] first = firstOrder.generateNoiseOctaves(null, x, y, z, 5, 33, 5, firstOrderFreq.x, firstOrderFreq.y, firstOrderFreq.z);
        double[] second = secondOrder.generateNoiseOctaves(null, x, y, z, 5, 33, 5, secondOrderFreq.x, secondOrderFreq.y, secondOrderFreq.z);
        double[] third = thirdOrder.generateNoiseOctaves(null, x, y, z, 5, 33, 5, thirdOrderFreq.x, thirdOrderFreq.y, thirdOrderFreq.z);
        double[] height = heightOrder.generateNoiseOctaves(null, x, z, 5, 5, heightOrderFreq.x, heightOrderFreq.y, heightOrderFreq.z);
        double[] terrain = new double[825];
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
                for (int j2 = 0; j2 < 33; ++j2) {
                    double d6 = ((double) j2 - d5) * 12.0D * 128.0D / 256.0D / d14;
                    if (d6 < 0.0D) d6 *= 4.0D;
                    double d7 = first[l] / 512.0D;
                    double d8 = second[l] / 512.0D;
                    double d9 = (third[l] / 10.0D + 1.0D) / 2.0D;
                    double d10 = reclamp ? Mth.clampedLerp(d7, d8, d9) - d6 : d8 - d6;
                    if (j2 > 29) {
                        double d11 = (float) (j2 - 29) / 3.0F;
                        d10 = d10 * (1.0D - d11) + -10.0D * d11;
                    }
                    terrain[l] = d10;
                    ++l;
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

    public static class BlockMetaBuffer {
        public final Block[] blocks = new Block[65536];
        public final byte[] metas = new byte[65536];
        public Random rand;
    }
}
