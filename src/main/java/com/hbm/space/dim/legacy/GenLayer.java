// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

/**
 * The 1.7 biome layer stack NTM:Space builds its planets' biome maps from. Modern Minecraft
 * dropped layers for climate noise; this keeps their seeding so a planet's layer chain reads as
 * it did in NTM:Space. Every layer turns an area of its parent's biome ids into its own.
 */
public abstract class GenLayer {

    private static final long MUL = 6364136223846793005L;
    private static final long ADD = 1442695040888963407L;

    protected GenLayer parent;
    private long worldGenSeed;
    private long chunkSeed;
    private final long baseSeed;

    protected GenLayer(long seed) {
        long s = seed;
        for (int i = 0; i < 3; i++) {
            s *= s * MUL + ADD;
            s += seed;
        }
        baseSeed = s;
    }

    public void initWorldGenSeed(long seed) {
        worldGenSeed = seed;
        if (parent != null) parent.initWorldGenSeed(seed);
        for (int i = 0; i < 3; i++) {
            worldGenSeed *= worldGenSeed * MUL + ADD;
            worldGenSeed += baseSeed;
        }
    }

    public void initChunkSeed(long x, long z) {
        chunkSeed = worldGenSeed;
        for (int i = 0; i < 2; i++) {
            chunkSeed *= chunkSeed * MUL + ADD;
            chunkSeed += x;
            chunkSeed *= chunkSeed * MUL + ADD;
            chunkSeed += z;
        }
    }

    protected int nextInt(int bound) {
        int i = (int) ((chunkSeed >> 24) % bound);
        if (i < 0) i += bound;
        chunkSeed *= chunkSeed * MUL + ADD;
        chunkSeed += worldGenSeed;
        return i;
    }

    /** The biome ids of the area, row by row along x. */
    public abstract int[] getInts(int x, int z, int width, int depth);

    protected int selectRandom(int... ids) {
        return ids[nextInt(ids.length)];
    }

    protected int selectModeOrRandom(int a, int b, int c, int d) {
        if (b == c && c == d) return b;
        if (a == b && a == c) return a;
        if (a == b && a == d) return a;
        if (a == c && a == d) return a;
        if (a == b && c != d) return a;
        if (a == c && b != d) return a;
        if (a == d && b != c) return a;
        if (b == c && a != d) return b;
        if (b == d && a != c) return b;
        if (c == d && a != b) return c;
        return selectRandom(a, b, c, d);
    }
}
