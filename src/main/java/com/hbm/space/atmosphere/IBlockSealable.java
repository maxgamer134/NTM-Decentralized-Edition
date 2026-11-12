// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.atmosphere;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A block that seals only some of the time, as a door does closed. It calls
 * {@link SpaceAtmosphere#sealChanged} when that changes.
 */
public interface IBlockSealable {

    boolean isSealed(Level level, BlockPos pos);
}
