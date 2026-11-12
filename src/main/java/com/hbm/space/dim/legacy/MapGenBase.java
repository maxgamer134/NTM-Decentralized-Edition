// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import java.util.Random;
import net.minecraft.world.level.block.Block;

/**
 * 1.7's carver base: every chunk within {@link #range} of the one being generated seeds the
 * random from its position and may carve into it, so features cross chunk borders seamlessly.
 */
public class MapGenBase {

    protected int range = 8;
    protected Random rand = new Random();
    protected World worldObj;

    public void func_151539_a(Object provider, World world, int x, int z, Block[] blocks) {
        int r = range;
        worldObj = world;
        rand.setSeed(world.getSeed());
        long a = rand.nextLong();
        long b = rand.nextLong();
        for (int cx = x - r; cx <= x + r; cx++) {
            for (int cz = z - r; cz <= z + r; cz++) {
                rand.setSeed((long) cx * a ^ (long) cz * b ^ world.getSeed());
                func_151538_a(world, cx, cz, x, z, blocks);
            }
        }
    }

    /** Carves what the chunk at (offsetX, offsetZ) starts into the chunk being generated. */
    protected void func_151538_a(World world, int offsetX, int offsetZ, int chunkX, int chunkZ, Block[] blocks) {}
}
