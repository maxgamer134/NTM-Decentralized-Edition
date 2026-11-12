// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import java.util.Random;

/** A 1.7 feature: places itself around a position in a world. */
public abstract class WorldGenerator {
    protected WorldGenerator() {}

    protected WorldGenerator(boolean notify) {}

    public abstract boolean generate(World world, Random rand, int x, int y, int z);

    protected void func_150515_a(World world, int x, int y, int z, net.minecraft.world.level.block.Block block) {
        world.setBlock(x, y, z, block, 0, 2);
    }

    protected void setBlockAndNotifyAdequately(World world, int x, int y, int z, net.minecraft.world.level.block.Block block, int meta) {
        world.setBlock(x, y, z, block, meta, 2);
    }
}
