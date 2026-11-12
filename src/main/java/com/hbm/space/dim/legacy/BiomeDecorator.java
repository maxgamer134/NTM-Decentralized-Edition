// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import java.util.Random;

/**
 * 1.7's biome decorator. Its stock decorations (flowers, grass, trees, stone ores) all need
 * Earth's soil and stone, which planets have none of, so a planet biome that keeps the stock
 * decorator decorates nothing; NTM:Space's own decorators override {@link #genDecorations}.
 */
public class BiomeDecorator {

    public World currentWorld;
    public Random randomGenerator;
    public int chunk_X;
    public int chunk_Z;
    public boolean generateLakes = true;

    public void decorateChunk(World world, Random rand, BiomeGenBase biome, int x, int z) {
        currentWorld = world;
        randomGenerator = rand;
        chunk_X = x;
        chunk_Z = z;
        genDecorations(biome);
        currentWorld = null;
        randomGenerator = null;
    }

    protected void genDecorations(BiomeGenBase biome) {
        generateOres();
    }

    protected void generateOres() {}
}
