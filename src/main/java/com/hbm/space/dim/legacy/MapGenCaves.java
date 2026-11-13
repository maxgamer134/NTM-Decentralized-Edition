// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.7's cave carver: worm tunnels, now and then from a round room, wandering through the rock.
 * NTM:Space's planet caves extend it and decide in {@link #digBlock} what a carved block becomes.
 */
public class MapGenCaves extends MapGenBase {

    protected void func_151542_a(long seed, int chunkX, int chunkZ, Block[] blocks, double x, double y, double z) {
        func_151541_a(seed, chunkX, chunkZ, blocks, x, y, z, 1.0F + rand.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5D);
    }

    protected void func_151541_a(long seed, int chunkX, int chunkZ, Block[] blocks, double x, double y, double z,
            float width, float yaw, float pitch, int step, int steps, double squash) {
        double cx = chunkX * 16 + 8;
        double cz = chunkZ * 16 + 8;
        float yawSpeed = 0.0F;
        float pitchSpeed = 0.0F;
        Random random = new Random(seed);
        if (steps <= 0) {
            int max = range * 16 - 16;
            steps = max - random.nextInt(max / 4);
        }
        boolean room = false;
        if (step == -1) {
            step = steps / 2;
            room = true;
        }
        int branch = random.nextInt(steps / 2) + steps / 4;
        for (boolean steep = random.nextInt(6) == 0; step < steps; ++step) {
            double rh = 1.5D + Mth.sin(step * (float) Math.PI / steps) * width;
            double rv = rh * squash;
            float cp = Mth.cos(pitch);
            x += Mth.cos(yaw) * cp;
            y += Mth.sin(pitch);
            z += Mth.sin(yaw) * cp;
            pitch *= steep ? 0.92F : 0.7F;
            pitch += pitchSpeed * 0.1F;
            yaw += yawSpeed * 0.1F;
            pitchSpeed *= 0.9F;
            yawSpeed *= 0.75F;
            pitchSpeed += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawSpeed += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
            if (!room && step == branch && width > 1.0F && steps > 0) {
                func_151541_a(random.nextLong(), chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F,
                        yaw - (float) Math.PI / 2F, pitch / 3.0F, step, steps, 1.0D);
                func_151541_a(random.nextLong(), chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F,
                        yaw + (float) Math.PI / 2F, pitch / 3.0F, step, steps, 1.0D);
                return;
            }
            if (!room && random.nextInt(4) == 0) continue;
            double dx = x - cx, dz = z - cz;
            double left = steps - step;
            double reach = width + 2.0F + 16.0F;
            if (dx * dx + dz * dz - left * left > reach * reach) return;
            if (x < cx - 16.0D - rh * 2.0D || z < cz - 16.0D - rh * 2.0D || x > cx + 16.0D + rh * 2.0D || z > cz + 16.0D + rh * 2.0D)
                continue;
            int x0 = Mth.floor(x - rh) - chunkX * 16 - 1, x1 = Mth.floor(x + rh) - chunkX * 16 + 1;
            int y0 = Mth.floor(y - rv) - 1, y1 = Mth.floor(y + rv) + 1;
            int z0 = Mth.floor(z - rh) - chunkZ * 16 - 1, z1 = Mth.floor(z + rh) - chunkZ * 16 + 1;
            x0 = Math.max(x0, 0);
            x1 = Math.min(x1, 16);
            y0 = Math.max(y0, -63);
            y1 = Math.min(y1, 312);
            z0 = Math.max(z0, 0);
            z1 = Math.min(z1, 16);
            boolean water = false;
            for (int bx = x0; !water && bx < x1; ++bx)
                for (int bz = z0; !water && bz < z1; ++bz)
                    for (int by = y1 + 1; !water && by >= y0 - 1; --by) {
                        if (by < -64 || by >= 320) continue;
                        if (isOceanBlock(blocks, ChunkProviderCelestial.idx(bx, bz, by), bx, by, bz, chunkX, chunkZ)) water = true;
                        if (by != y0 - 1 && bx != x0 && bx != x1 - 1 && bz != z0 && bz != z1 - 1) by = y0;
                    }
            if (water) continue;
            for (int bx = x0; bx < x1; ++bx) {
                double fx = (bx + chunkX * 16 + 0.5D - x) / rh;
                for (int bz = z0; bz < z1; ++bz) {
                    double fz = (bz + chunkZ * 16 + 0.5D - z) / rh;
                    int index = ChunkProviderCelestial.idx(bx, bz, y1);
                    boolean top = false;
                    if (fx * fx + fz * fz < 1.0D) {
                        for (int by = y1 - 1; by >= y0; --by) {
                            double fy = (by + 0.5D - y) / rv;
                            if (fy > -0.7D && fx * fx + fy * fy + fz * fz < 1.0D) {
                                if (isTopBlock(blocks, index, bx, by, bz, chunkX, chunkZ)) top = true;
                                digBlock(blocks, index, bx, by, bz, chunkX, chunkZ, top);
                            }
                            --index;
                        }
                    }
                }
            }
            if (room) break;
        }
    }

    @Override
    protected void func_151538_a(World world, int offsetX, int offsetZ, int chunkX, int chunkZ, Block[] blocks) {
        int count = rand.nextInt(rand.nextInt(rand.nextInt(15) + 1) + 1);
        if (rand.nextInt(7) != 0) count = 0;
        for (int i = 0; i < count; ++i) {
            double x = offsetX * 16 + rand.nextInt(16);
            double y = rand.nextInt(rand.nextInt(184) + 8) - 64;
            double z = offsetZ * 16 + rand.nextInt(16);
            int tunnels = 1;
            if (rand.nextInt(4) == 0) {
                func_151542_a(rand.nextLong(), chunkX, chunkZ, blocks, x, y, z);
                tunnels += rand.nextInt(4);
            }
            for (int t = 0; t < tunnels; ++t) {
                float yaw = rand.nextFloat() * (float) Math.PI * 2.0F;
                float pitch = (rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
                float width = rand.nextFloat() * 2.0F + rand.nextFloat();
                if (rand.nextInt(10) == 0) width *= rand.nextFloat() * rand.nextFloat() * 3.0F + 1.0F;
                func_151541_a(rand.nextLong(), chunkX, chunkZ, blocks, x, y, z, width, yaw, pitch, 0, 0, 1.0D);
            }
        }
    }

    protected boolean isOceanBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ) {
        return data[index] == Blocks.WATER;
    }

    protected boolean isTopBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ) {
        BiomeGenBase biome = worldObj.getBiomeGenForCoords(x + chunkX * 16, z + chunkZ * 16);
        return data[index] == biome.topBlock;
    }

    /** Carves one block: air, or lava below y 10, and the filler under a carved top grows the top. */
    protected void digBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        BiomeGenBase biome = worldObj.getBiomeGenForCoords(x + chunkX * 16, z + chunkZ * 16);
        Block block = data[index];
        if (block == Blocks.STONE || block == biome.fillerBlock || block == biome.topBlock) {
            if (y < -54) {
                data[index] = Blocks.LAVA;
            } else {
                data[index] = null;
                if (foundTop && index > 0 && data[index - 1] == biome.fillerBlock) data[index - 1] = biome.topBlock;
            }
        }
    }
}
