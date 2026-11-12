// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** The little of 1.7's block materials NTM:Space's world generation asks about. */
public final class Material {

    public static final Material air = new Material(false, true, false);
    public static final Material water = new Material(true, true, false);
    public static final Material lava = new Material(true, true, false);
    public static final Material plants = new Material(false, true, false);
    public static final Material solid = new Material(false, false, true);

    private final boolean liquid;
    private final boolean replaceable;
    private final boolean opaque;

    private Material(boolean liquid, boolean replaceable, boolean opaque) {
        this.liquid = liquid;
        this.replaceable = replaceable;
        this.opaque = opaque;
    }

    public boolean isLiquid() {
        return liquid;
    }

    public boolean isReplaceable() {
        return replaceable;
    }

    public boolean isSolid() {
        return !liquid && !replaceable;
    }

    public static Material of(Block block) {
        if (block == null || block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) return air;
        if (block instanceof LiquidBlock liquid) {
            BlockState state = liquid.defaultBlockState();
            return state.getFluidState().is(Fluids.LAVA) || state.getFluidState().is(Fluids.FLOWING_LAVA) ? lava : water;
        }
        BlockState state = block.defaultBlockState();
        if (state.canBeReplaced()) return plants;
        return solid;
    }

    public static boolean isOpaqueCube(Block block) {
        if (block == null) return false;
        BlockState state = block.defaultBlockState();
        return state.canOcclude() && state.isSolid();
    }

    public static boolean isAir(Block block) {
        return of(block) == air;
    }
}
