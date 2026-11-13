// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** The stock 1.7 features NTM:Space places on planets: ore veins and lakes. */
public final class Features {

    private Features() {}

    /** One kind of ore placement a planet's generator asked for, as the recipe viewer lists it. */
    public record Vein(Block ore, int meta, Block target, double attempts, int size, int minY, int maxY, String kind) {}

    private static final ThreadLocal<List<Vein>> RECORDING = new ThreadLocal<>();

    /** Runs a generator with every ore placement written down instead of placed. */
    public static List<Vein> record(Runnable generator) {
        List<Vein> veins = new ArrayList<>();
        RECORDING.set(veins);
        try {
            generator.run();
        } finally {
            RECORDING.remove();
        }
        return veins;
    }

    /** True when recording: the placement is written down and must not happen. */
    public static boolean recorded(Block ore, int meta, Block target, double attempts, int size, int minY, int maxY, String kind) {
        List<Vein> veins = RECORDING.get();
        if (veins == null) return false;
        veins.add(new Vein(ore, meta, target, attempts, size, minY, maxY, kind));
        return true;
    }

    /** WorldGenMinable: a vein of ore along a short random line through the target rock. */
    public static class WorldGenMinable extends WorldGenerator {
        private final Block ore;
        private final int meta;
        private final int count;
        private final Block target;

        public WorldGenMinable(Block ore, int count) {
            this(ore, 0, count, Blocks.STONE);
        }

        public WorldGenMinable(Block ore, int count, Block target) {
            this(ore, 0, count, target);
        }

        public WorldGenMinable(Block ore, int meta, int count, Block target) {
            this.ore = ore;
            this.meta = meta;
            this.count = count;
            this.target = target;
        }

        @Override
        public boolean generate(World world, Random rand, int x, int y, int z) {
            float angle = rand.nextFloat() * (float) Math.PI;
            double x0 = x + 8 + Mth.sin(angle) * count / 8F, x1 = x + 8 - Mth.sin(angle) * count / 8F;
            double z0 = z + 8 + Mth.cos(angle) * count / 8F, z1 = z + 8 - Mth.cos(angle) * count / 8F;
            double y0 = y + rand.nextInt(3) - 2, y1 = y + rand.nextInt(3) - 2;
            for (int i = 0; i <= count; i++) {
                double cx = x0 + (x1 - x0) * i / count;
                double cy = y0 + (y1 - y0) * i / count;
                double cz = z0 + (z1 - z0) * i / count;
                double size = rand.nextDouble() * count / 16.0;
                double rh = (Mth.sin(i * (float) Math.PI / count) + 1F) * size + 1.0;
                int minX = Mth.floor(cx - rh / 2), minY = Mth.floor(cy - rh / 2), minZ = Mth.floor(cz - rh / 2);
                int maxX = Mth.floor(cx + rh / 2), maxY = Mth.floor(cy + rh / 2), maxZ = Mth.floor(cz + rh / 2);
                for (int bx = minX; bx <= maxX; bx++) {
                    double dx = (bx + 0.5 - cx) / (rh / 2);
                    if (dx * dx >= 1) continue;
                    for (int by = minY; by <= maxY; by++) {
                        double dy = (by + 0.5 - cy) / (rh / 2);
                        if (dx * dx + dy * dy >= 1) continue;
                        for (int bz = minZ; bz <= maxZ; bz++) {
                            double dz = (bz + 0.5 - cz) / (rh / 2);
                            if (dx * dx + dy * dy + dz * dz < 1 && com.hbm.space.dim.DeepStone.matches(world.getBlock(bx, by, bz), target))
                                world.setBlock(bx, by, bz, ore, meta, 2);
                        }
                    }
                }
            }
            return true;
        }
    }

    /** WorldGenLakes: a blob of liquid pressed into the ground, ringed by its surroundings. */
    public static class WorldGenLakes extends WorldGenerator {
        private final Block liquid;

        public WorldGenLakes(Block liquid) {
            this.liquid = liquid;
        }

        @Override
        public boolean generate(World world, Random rand, int x, int y, int z) {
            x -= 8;
            z -= 8;
            while (y > 5 && world.isAirBlock(x, y, z)) y--;
            if (y <= 4) return false;
            y -= 4;
            boolean[] shape = new boolean[2048];
            int blobs = rand.nextInt(4) + 4;
            for (int i = 0; i < blobs; i++) {
                double sx = rand.nextDouble() * 6 + 3, sy = rand.nextDouble() * 4 + 2, sz = rand.nextDouble() * 6 + 3;
                double cx = rand.nextDouble() * (16 - sx - 2) + 1 + sx / 2;
                double cy = rand.nextDouble() * (8 - sy - 4) + 2 + sy / 2;
                double cz = rand.nextDouble() * (16 - sz - 2) + 1 + sz / 2;
                for (int lx = 1; lx < 15; lx++)
                    for (int lz = 1; lz < 15; lz++)
                        for (int ly = 1; ly < 7; ly++) {
                            double dx = (lx - cx) / (sx / 2), dy = (ly - cy) / (sy / 2), dz = (lz - cz) / (sz / 2);
                            if (dx * dx + dy * dy + dz * dz < 1) shape[(lx * 16 + lz) * 8 + ly] = true;
                        }
            }
            for (int lx = 0; lx < 16; lx++)
                for (int lz = 0; lz < 16; lz++)
                    for (int ly = 0; ly < 8; ly++) {
                        if (shape[(lx * 16 + lz) * 8 + ly] || !edge(shape, lx, ly, lz)) continue;
                        Material m = Material.of(world.getBlock(x + lx, y + ly, z + lz));
                        if (ly >= 4 && m.isLiquid()) return false;
                        if (ly < 4 && !m.isSolid() && world.getBlock(x + lx, y + ly, z + lz) != liquid) return false;
                    }
            for (int lx = 0; lx < 16; lx++)
                for (int lz = 0; lz < 16; lz++)
                    for (int ly = 0; ly < 8; ly++)
                        if (shape[(lx * 16 + lz) * 8 + ly])
                            world.setBlock(x + lx, y + ly, z + lz, ly >= 4 ? Blocks.AIR : liquid, 0, 2);
            return true;
        }

        private static boolean edge(boolean[] s, int x, int y, int z) {
            return x < 15 && s[((x + 1) * 16 + z) * 8 + y] || x > 0 && s[((x - 1) * 16 + z) * 8 + y]
                    || z < 15 && s[(x * 16 + z + 1) * 8 + y] || z > 0 && s[(x * 16 + z - 1) * 8 + y]
                    || y < 7 && s[(x * 16 + z) * 8 + y + 1] || y > 0 && s[(x * 16 + z) * 8 + y - 1];
        }
    }

    /**
     * A 1.7 ore height on the planets' taller world: the old rock below the sea, 0 to 63, now
     * spans the new depth from -64, so the ores spread through it as the modern overworld's do.
     */
    public static int stretch(int y) {
        return y < 64 ? y * 2 + ChunkProviderCelestial.MIN_Y : y;
    }

    /** DungeonToolbox.generateOre: some veins of an ore at random heights in a chunk. */
    /** NTM's DepthDeposit: a ball of ore in filler, in the planet's stone and its bedrock. */
    public static void depthDeposit(World world, int x, int yMin, int yDev, int z, int size, double fill, Block block, Random rand, int chance, Block target, Block filler) {
        if (recorded(block, 0, target, 1.0 / chance, size, yMin, yMin + Math.max(1, yDev) - 1, "space_deposit")) return;
        if (rand.nextInt(chance) != 0) return;
        int cx = x + rand.nextInt(16) + 8, cy = stretch(yMin + rand.nextInt(yDev)), cz = z + rand.nextInt(16) + 8;
        for (int i = cx - size; i <= cx + size; i++)
            for (int j = cy - size; j <= cy + size; j++)
                for (int k = cz - size; k <= cz + size; k++) {
                    if (j < ChunkProviderCelestial.MIN_Y + 1 || j > 126) continue;
                    double len = Math.sqrt((cx - i) * (cx - i) + (cy - j) * (cy - j) + (cz - k) * (cz - k));
                    Block here = world.getBlock(i, j, k);
                    if (!com.hbm.space.dim.DeepStone.matches(here, target) && here != net.minecraft.world.level.block.Blocks.BEDROCK) continue;
                    if (len + rand.nextInt(2) < size * fill) world.setBlock(i, j, k, block);
                    else if (len + rand.nextInt(2) <= size) world.setBlock(i, j, k, filler);
                }
    }

    public static void generateOre(World world, Random rand, int x, int z, int veins, int amount, int minHeight, int variance,
            Block ore, int meta, Block target) {
        if (veins <= 0 || ore == null || ore == Blocks.AIR) return;
        if (recorded(ore, meta, target, veins, amount, minHeight, minHeight + Math.max(1, variance) - 1, "vein")) return;
        for (int i = 0; i < veins; i++) {
            int bx = x + rand.nextInt(16);
            int by = stretch(minHeight + (variance > 0 ? rand.nextInt(variance) : 0));
            int bz = z + rand.nextInt(16);
            new WorldGenMinable(ore, meta, amount, target).generate(world, rand, bx, by, bz);
        }
    }

    public static void generateOre(World world, Random rand, int x, int z, int veins, int amount, int minHeight, int variance,
            Block ore, Block target) {
        generateOre(world, rand, x, z, veins, amount, minHeight, variance, ore, 0, target);
    }
}
