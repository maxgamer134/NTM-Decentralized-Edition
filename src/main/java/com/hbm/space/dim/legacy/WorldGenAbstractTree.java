// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import net.minecraft.world.level.block.Block;

/** 1.7's tree generator base: what a tree may grow through. */
public abstract class WorldGenAbstractTree extends WorldGenerator {

    protected WorldGenAbstractTree(boolean notify) {}

    protected boolean func_150523_a(Block block) {
        Material m = Material.of(block);
        return m == Material.air || m == Material.plants || block == net.minecraft.world.level.block.Blocks.GRASS_BLOCK
                || block == net.minecraft.world.level.block.Blocks.DIRT || block instanceof net.minecraft.world.level.block.LeavesBlock;
    }
}
