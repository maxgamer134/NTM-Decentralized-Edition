// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.client.render.BobbleFigures;
import com.hbm.client.render.RenderBobble;
import com.hbm.lib.Library;
import com.hbm.space.SpaceExtras;

/** NTM:Space's bobblehead figures: skins in upstream's default pose, and MrKimkimora's own body. */
public final class SpaceBobbleFigures {

    private static final java.util.function.Supplier<com.hbm.render.loader.HFRWavefrontObject> KIMKIMORA =
            com.google.common.base.Suppliers.memoize(() -> com.hbm.client.model.Meshes.load(Library.id("models/trinkets/bobble_mrkimkimora.obj")));

    private SpaceBobbleFigures() {}

    public static void register() {
        BobbleFigures.register(SpaceExtras.SpaceBobble.GWEN,
                new RenderBobble.Figure(Library.id("textures/models/trinkets/gwen.png"), BobbleFigures.NO_POSE, null));
        BobbleFigures.register(SpaceExtras.SpaceBobble.JUICE,
                new RenderBobble.Figure(Library.id("textures/models/trinkets/juicy_lad.png"), BobbleFigures.NO_POSE, null));
        BobbleFigures.register(SpaceExtras.SpaceBobble.JAMESH_2,
                new RenderBobble.Figure(Library.id("textures/models/trinkets/jamesh_2.png"), BobbleFigures.NO_POSE, null));
        // MrKimkimora is his own figure, not the posed guy
        BobbleFigures.register(SpaceExtras.SpaceBobble.MRKIMKIMORA,
                new RenderBobble.Figure(Library.id("textures/models/trinkets/mrkimkimora.png"), BobbleFigures.NO_POSE,
                        () -> new RenderBobble.Body(KIMKIMORA.get(), KIMKIMORA.get().partId("MrKimkimora"))));
    }
}
