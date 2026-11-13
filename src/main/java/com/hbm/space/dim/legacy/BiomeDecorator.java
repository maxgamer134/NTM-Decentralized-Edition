// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import java.util.Random;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.7's biome decorator. Its stock plant life needs Earth's soil, which planets have none of, so
 * only its stone ores are kept, and only Laythe has the stone for them; NTM:Space's own
 * decorators override {@link #genDecorations}.
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

    /**
     * 1.7's stock ores, in Earth's stone only: so on Laythe, whose rock is stone. Coal is left
     * out, as WorldGeneratorCelestial.onGenerateOre denied it on every planet.
     */
    protected void generateOres() {
        if (currentWorld == null || currentWorld.stone() != Blocks.STONE) return;
        genStandardOre1(20, Blocks.DIRT, 32, 0, 256);
        genStandardOre1(10, Blocks.GRAVEL, 32, 0, 256);
        genStandardOre1(20, Blocks.IRON_ORE, 8, 0, 64);
        genStandardOre1(2, Blocks.GOLD_ORE, 8, 0, 32);
        genStandardOre1(8, Blocks.REDSTONE_ORE, 7, 0, 16);
        genStandardOre1(1, Blocks.DIAMOND_ORE, 7, 0, 16);
        genStandardOre2(1, Blocks.LAPIS_ORE, 6, 16, 16);
    }

    /** Veins at heights spread evenly from min up to max. */
    protected void genStandardOre1(int count, Block ore, int size, int min, int max) {
        if (Features.recorded(ore, 0, Blocks.STONE, count, size, Features.stretch(min), Features.stretch(max - 1), "vein")) return;
        Features.WorldGenMinable gen = new Features.WorldGenMinable(ore, size);
        for (int l = 0; l < count; ++l) {
            int x = chunk_X + randomGenerator.nextInt(16);
            int y = Features.stretch(randomGenerator.nextInt(max - min) + min);
            int z = chunk_Z + randomGenerator.nextInt(16);
            gen.generate(currentWorld, randomGenerator, x, y, z);
        }
    }

    /** Veins at heights gathered around centre, spread either way. */
    protected void genStandardOre2(int count, Block ore, int size, int centre, int spread) {
        if (Features.recorded(ore, 0, Blocks.STONE, count, size, Features.stretch(centre - spread), Features.stretch(centre + spread - 2), "vein")) return;
        Features.WorldGenMinable gen = new Features.WorldGenMinable(ore, size);
        for (int l = 0; l < count; ++l) {
            int x = chunk_X + randomGenerator.nextInt(16);
            int y = Features.stretch(randomGenerator.nextInt(spread) + randomGenerator.nextInt(spread) + (centre - spread));
            int z = chunk_Z + randomGenerator.nextInt(16);
            gen.generate(currentWorld, randomGenerator, x, y, z);
        }
    }
}
