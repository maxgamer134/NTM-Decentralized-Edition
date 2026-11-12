// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.client.render.BobbleFigures;
import com.hbm.client.render.RenderBobble;
import com.hbm.lib.Library;
import com.hbm.space.SpaceExtras;

/** The skin of NTM:Space's Gwen bobblehead, standing in the default pose like upstream. */
public final class SpaceBobbleFigures {

    private SpaceBobbleFigures() {}

    public static void register() {
        BobbleFigures.register(SpaceExtras.SpaceBobble.GWEN,
                new RenderBobble.Figure(Library.id("textures/models/trinkets/gwen.png"), BobbleFigures.NO_POSE, null));
    }
}
