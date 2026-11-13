// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim;

import com.hbm.space.SpaceExtras;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * Each planet's stone and the deeper stone under y 0 that stands in for it, as deepslate does
 * for the overworld's: Laythe's plain stone takes deepslate itself.
 */
public final class DeepStone {

    private static volatile @Nullable Map<Block, Block> map;

    private DeepStone() {}

    private static Map<Block, Block> map() {
        Map<Block, Block> m = map;
        if (m != null) return m;
        m = new HashMap<>();
        m.put(Blocks.STONE, Blocks.DEEPSLATE);
        for (var deep : SpaceExtras.DEEP_STONES) {
            String stone = deep.id().getPath().substring("deep_".length());
            m.put(BuiltInRegistries.BLOCK.get(com.hbm.lib.Library.id(stone)), deep.get());
        }
        return map = m;
    }

    /** The deep stone under a planet stone, or null when it has none. */
    public static @Nullable Block of(@Nullable Block stone) {
        return stone == null ? null : map().get(stone);
    }

    /** Whether a block is the given stone, or the deep stone under it, for ores to replace. */
    public static boolean matches(Block here, Block stone) {
        return here == stone || here != null && here == of(stone);
    }
}
