// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

/** The stock 1.7 layers NTM:Space's planets chain: zooms, smoothing and rivers. */
public final class Layers {

    private Layers() {}

    /** Doubles the scale, picking each new cell from its corners. */
    public static class Zoom extends GenLayer {
        private final boolean fuzzy;

        public Zoom(long seed, GenLayer parent) {
            this(seed, parent, false);
        }

        public static GenLayer magnify(long seed, GenLayer layer, int times) {
            for (int i = 0; i < times; i++) layer = new Zoom(seed + i, layer);
            return layer;
        }

        protected Zoom(long seed, GenLayer parent, boolean fuzzy) {
            super(seed);
            this.parent = parent;
            this.fuzzy = fuzzy;
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int px = x >> 1, pz = z >> 1, pw = (width >> 1) + 2, pd = (depth >> 1) + 2;
            int[] in = parent.getInts(px, pz, pw, pd);
            int w = (pw - 1) << 1, d = (pd - 1) << 1;
            int[] tmp = new int[w * d];
            for (int row = 0; row < pd - 1; row++) {
                int o = (row << 1) * w;
                int col = 0;
                int a = in[col + row * pw];
                for (int b = in[col + (row + 1) * pw]; col < pw - 1; col++) {
                    initChunkSeed((long) (col + px) << 1, (long) (row + pz) << 1);
                    int c = in[col + 1 + row * pw];
                    int e = in[col + 1 + (row + 1) * pw];
                    tmp[o] = a;
                    tmp[o++ + w] = selectRandom(a, b);
                    tmp[o] = selectRandom(a, c);
                    tmp[o++ + w] = fuzzy ? selectRandom(a, c, b, e) : selectModeOrRandom(a, c, b, e);
                    a = c;
                    b = e;
                }
            }
            int[] out = new int[width * depth];
            for (int row = 0; row < depth; row++)
                System.arraycopy(tmp, (row + (z & 1)) * w + (x & 1), out, row * width, width);
            return out;
        }
    }

    public static class FuzzyZoom extends Zoom {
        public FuzzyZoom(long seed, GenLayer parent) {
            super(seed, parent, true);
        }
    }

    /** Rounds off lone cells between two matching neighbours. */
    public static class Smooth extends GenLayer {
        public Smooth(long seed, GenLayer parent) {
            super(seed);
            this.parent = parent;
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int pw = width + 2;
            int[] in = parent.getInts(x - 1, z - 1, pw, depth + 2);
            int[] out = new int[width * depth];
            for (int j = 0; j < depth; j++) {
                for (int i = 0; i < width; i++) {
                    int west = in[i + (j + 1) * pw], east = in[i + 2 + (j + 1) * pw];
                    int north = in[i + 1 + j * pw], south = in[i + 1 + (j + 2) * pw];
                    int self = in[i + 1 + (j + 1) * pw];
                    if (west == east && north == south) {
                        initChunkSeed(i + x, j + z);
                        self = nextInt(2) == 0 ? west : north;
                    } else {
                        if (west == east) self = west;
                        if (north == south) self = north;
                    }
                    out[i + j * width] = self;
                }
            }
            return out;
        }
    }

    /** Quadruples the scale into jittered Voronoi cells, the last step to block resolution. */
    public static class VoronoiZoom extends GenLayer {
        public VoronoiZoom(long seed, GenLayer parent) {
            super(seed);
            this.parent = parent;
        }

        private double jitter() {
            return (nextInt(1024) / 1024.0 - 0.5) * 3.6;
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            x -= 2;
            z -= 2;
            int px = x >> 2, pz = z >> 2, pw = (width >> 2) + 2, pd = (depth >> 2) + 2;
            int[] in = parent.getInts(px, pz, pw, pd);
            int w = (pw - 1) << 2, d = (pd - 1) << 2;
            int[] tmp = new int[w * d];
            for (int row = 0; row < pd - 1; row++) {
                int col = 0;
                int a = in[col + row * pw];
                for (int b = in[col + (row + 1) * pw]; col < pw - 1; col++) {
                    initChunkSeed((long) (col + px) << 2, (long) (row + pz) << 2);
                    double ax = jitter(), az = jitter();
                    initChunkSeed((long) (col + px + 1) << 2, (long) (row + pz) << 2);
                    double cx = jitter() + 4, cz = jitter();
                    initChunkSeed((long) (col + px) << 2, (long) (row + pz + 1) << 2);
                    double bx = jitter(), bz = jitter() + 4;
                    initChunkSeed((long) (col + px + 1) << 2, (long) (row + pz + 1) << 2);
                    double ex = jitter() + 4, ez = jitter() + 4;
                    int c = in[col + 1 + row * pw] & 255;
                    int e = in[col + 1 + (row + 1) * pw] & 255;
                    for (int i = 0; i < 4; i++) {
                        int o = ((row << 2) + i) * w + (col << 2);
                        for (int k = 0; k < 4; k++) {
                            double da = sq(i - az) + sq(k - ax), dc = sq(i - cz) + sq(k - cx);
                            double db = sq(i - bz) + sq(k - bx), de = sq(i - ez) + sq(k - ex);
                            if (da < dc && da < db && da < de) tmp[o++] = a;
                            else if (dc < da && dc < db && dc < de) tmp[o++] = c;
                            else if (db < da && db < dc && db < de) tmp[o++] = b;
                            else tmp[o++] = e;
                        }
                    }
                    a = c;
                    b = e;
                }
            }
            int[] out = new int[width * depth];
            for (int row = 0; row < depth; row++)
                System.arraycopy(tmp, (row + (z & 3)) * w + (x & 3), out, row * width, width);
            return out;
        }

        private static double sq(double v) {
            return v * v;
        }
    }

    /** Marks river cells along the edges between regions of the parent. */
    public static class River extends GenLayer {
        public River(long seed, GenLayer parent) {
            super(seed);
            this.parent = parent;
        }

        private static int fold(int id) {
            return id >= 2 ? 2 + (id & 1) : id;
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int pw = width + 2;
            int[] in = parent.getInts(x - 1, z - 1, pw, depth + 2);
            int[] out = new int[width * depth];
            for (int j = 0; j < depth; j++) {
                for (int i = 0; i < width; i++) {
                    int self = fold(in[i + 1 + (j + 1) * pw]);
                    boolean same = self == fold(in[i + (j + 1) * pw]) && self == fold(in[i + 1 + j * pw])
                            && self == fold(in[i + 2 + (j + 1) * pw]) && self == fold(in[i + 1 + (j + 2) * pw]);
                    out[i + j * width] = same ? -1 : BiomeGenBase.RIVER_ID;
                }
            }
            return out;
        }
    }

    /** Lays the river layer over the biome layer, as vanilla 1.7 did. */
    public static class RiverMix extends GenLayer {
        private final GenLayer biomes;
        private final GenLayer rivers;

        public RiverMix(long seed, GenLayer biomes, GenLayer rivers) {
            super(seed);
            this.biomes = biomes;
            this.rivers = rivers;
        }

        @Override
        public void initWorldGenSeed(long seed) {
            biomes.initWorldGenSeed(seed);
            rivers.initWorldGenSeed(seed);
            super.initWorldGenSeed(seed);
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int[] b = biomes.getInts(x, z, width, depth);
            int[] r = rivers.getInts(x, z, width, depth);
            int[] out = new int[width * depth];
            for (int i = 0; i < out.length; i++) {
                int id = b[i];
                if (id == BiomeGenBase.OCEAN_ID || id == BiomeGenBase.DEEP_OCEAN_ID) out[i] = id;
                else if (r[i] == BiomeGenBase.RIVER_ID) out[i] = id == BiomeGenBase.ICE_PLAINS_ID ? BiomeGenBase.FROZEN_RIVER_ID : r[i] & 255;
                else out[i] = id;
            }
            return out;
        }
    }
}
