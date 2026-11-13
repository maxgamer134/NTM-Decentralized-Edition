// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.blocks.machine;

import com.hbm.space.SpaceToggle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space's wrecked Leviathan turbine and powered condenser: the first explosion wrecks the
 * machine (it stops working and shows the damaged model until it is repaired with a blowtorch),
 * the second one removes it. Only with Space's changes on.
 */
public final class DamageableMachine {
    public static final BooleanProperty DAMAGED = BooleanProperty.create("damaged");

    private DamageableMachine() {}

    /** The core block entity's per-explosion guard, so a multiblock is hit once per explosion. */
    public interface Holder {
        @Nullable Explosion lastExplosion();

        void lastExplosion(@Nullable Explosion explosion);
    }

    public static boolean isDamaged(BlockState state) {
        return state.hasProperty(DAMAGED) && state.getValue(DAMAGED);
    }

    public static void setDamaged(Level level, BlockPos core, boolean damaged) {
        BlockState state = level.getBlockState(core);
        if (!state.hasProperty(DAMAGED) || state.getValue(DAMAGED) == damaged) return;
        level.setBlock(
                core, state.setValue(DAMAGED, damaged), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        BlockEntity be = level.getBlockEntity(core);
        if (be != null) be.setChanged();
    }

    /** True when the explosion was handled here (the machine was wrecked or removed). */
    public static boolean onExplosion(ServerLevel level, BlockPos core, Explosion explosion) {
        if (!SpaceToggle.changes()) return false;
        if (!(level.getBlockEntity(core) instanceof Holder holder)) return false;
        if (holder.lastExplosion() == explosion) return true;
        holder.lastExplosion(explosion);
        if (!isDamaged(level.getBlockState(core))) setDamaged(level, core, true);
        else level.removeBlock(core, false);
        return true;
    }
}
