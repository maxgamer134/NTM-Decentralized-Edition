// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.blocks.multiblock;

import com.hbm.space.SpaceToggle;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;

/**
 * NTM:Space: a player who places a multiblock on top of themselves can walk out of it instead of
 * being stuck inside. They pass through multiblock blocks until they stand clear of them.
 */
public final class MultiblockPhasing {

    /** Ticks after placing during which the player stays phased even before the blocks arrive. */
    private static final int GRACE = 10;

    private static final Map<Player, Integer> PHASED = new WeakHashMap<>();

    private MultiblockPhasing() {}

    static void placed(Player player) {
        if (!SpaceToggle.changes()) return;
        synchronized (PHASED) {
            PHASED.put(player, player.tickCount);
        }
    }

    /** True if this collision query comes from a phased player, who then sees no multiblock shape. */
    static boolean phased(CollisionContext context) {
        if (!(context instanceof EntityCollisionContext entity) || !(entity.getEntity() instanceof Player player))
            return false;
        synchronized (PHASED) {
            Integer since = PHASED.get(player);
            if (since == null) return false;
            if (player.tickCount - since <= GRACE || inside(player)) return true;
            PHASED.remove(player);
            return false;
        }
    }

    private static boolean inside(Player player) {
        BlockPos feet = BlockPos.containing(player.getX(), player.getY() + 0.1D, player.getZ());
        return isMultiblock(player.level().getBlockState(feet).getBlock())
                || isMultiblock(player.level().getBlockState(feet.above()).getBlock());
    }

    private static boolean isMultiblock(Block block) {
        return block instanceof BlockMultiblockCore || block instanceof BlockMultiblockCell;
    }
}
