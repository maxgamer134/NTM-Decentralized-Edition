// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.client.ClientEffects;
import com.hbm.space.machine.SpaceLPW2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** The LPW-2's exhaust while it burns: NTM's "missileContrail", a rocket flame thrown out of the bell. */
public final class LPW2Contrail {

    private LPW2Contrail() {}

    public static void spawn(SpaceLPW2.LPW2BE be, Direction dir) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockPos p = be.getBlockPos();
        ClientEffects.spawnRocketFlame(level, p.getX() + dir.getStepX() * 8.5, p.getY() + 4.5, p.getZ() + dir.getStepZ() * 8.5, 3F,
                dir.getStepX() * 10, 0, dir.getStepZ() * 10, 20 + level.random.nextInt(20));
    }
}
