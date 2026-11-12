// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.atmosphere;

import com.hbm.space.trait.CBT_Atmosphere;
import org.jspecify.annotations.Nullable;

/** A plant with its own idea of what air it lives in, as Tekto's do. */
public interface IPlantableBreathing {

    boolean canBreathe(@Nullable CBT_Atmosphere atmosphere);
}
