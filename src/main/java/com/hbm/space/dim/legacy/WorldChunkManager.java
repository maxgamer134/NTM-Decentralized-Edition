// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import java.util.Arrays;

/**
 * 1.7's biome provider: NTM:Space's WorldChunkManagerCelestial over a planet's two layer chains
 * (the coarse one terrain heights blend over and the block resolution one), or a single biome
 * everywhere, as WorldChunkManagerHell gave the Mun and other one-biome bodies.
 */
public class WorldChunkManager {

    private final GenLayer biomeLayer;
    private final GenLayer detailLayer;
    private final BiomeGenBase single;
    private final BiomeCache cache = new BiomeCache();

    public WorldChunkManager(GenLayer biomeLayer, GenLayer detailLayer, long seed) {
        this.biomeLayer = biomeLayer;
        this.detailLayer = detailLayer;
        this.single = null;
        biomeLayer.initWorldGenSeed(seed);
        detailLayer.initWorldGenSeed(seed);
    }

    /** WorldChunkManagerHell. */
    public WorldChunkManager(BiomeGenBase single) {
        this.biomeLayer = null;
        this.detailLayer = null;
        this.single = single;
    }

    public BiomeGenBase getBiomeGenAt(int x, int z) {
        if (single != null) return single;
        return cache.get(x, z);
    }

    /** The coarse biomes, at a quarter of block resolution. */
    public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] biomes, int x, int z, int width, int depth) {
        if (biomes == null || biomes.length < width * depth) biomes = new BiomeGenBase[width * depth];
        if (single != null) {
            Arrays.fill(biomes, 0, width * depth, single);
            return biomes;
        }
        int[] ids;
        synchronized (this) {
            ids = biomeLayer.getInts(x, z, width, depth);
        }
        for (int i = 0; i < width * depth; i++) biomes[i] = BiomeGenBase.getBiome(ids[i]);
        return biomes;
    }

    /** The biomes at block resolution. */
    public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] biomes, int x, int z, int width, int depth) {
        if (biomes == null || biomes.length < width * depth) biomes = new BiomeGenBase[width * depth];
        if (single != null) {
            Arrays.fill(biomes, 0, width * depth, single);
            return biomes;
        }
        if (width == 16 && depth == 16 && (x & 15) == 0 && (z & 15) == 0) {
            System.arraycopy(cache.chunk(x >> 4, z >> 4), 0, biomes, 0, 256);
            return biomes;
        }
        int[] ids;
        synchronized (this) {
            ids = detailLayer.getInts(x, z, width, depth);
        }
        for (int i = 0; i < width * depth; i++) biomes[i] = BiomeGenBase.getBiome(ids[i]);
        return biomes;
    }

    public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] biomes, int x, int z, int width, int depth, boolean cached) {
        return loadBlockGeneratorData(biomes, x, z, width, depth);
    }

    /** The block resolution biomes of recently asked chunks. */
    private final class BiomeCache {
        private static final int SIZE = 1024;
        private final long[] keys = new long[SIZE];
        private final BiomeGenBase[][] values = new BiomeGenBase[SIZE][];

        BiomeCache() {
            Arrays.fill(keys, Long.MIN_VALUE);
        }

        BiomeGenBase get(int x, int z) {
            return chunk(x >> 4, z >> 4)[(x & 15) + (z & 15) * 16];
        }

        BiomeGenBase[] chunk(int cx, int cz) {
            long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
            int slot = (int) ((key * 0x9E3779B97F4A7C15L) >>> 54) & (SIZE - 1);
            synchronized (WorldChunkManager.this) {
                if (keys[slot] == key) return values[slot];
                int[] ids = detailLayer.getInts(cx << 4, cz << 4, 16, 16);
                BiomeGenBase[] biomes = new BiomeGenBase[256];
                for (int i = 0; i < 256; i++) biomes[i] = BiomeGenBase.getBiome(ids[i]);
                keys[slot] = key;
                values[slot] = biomes;
                return biomes;
            }
        }
    }
}
