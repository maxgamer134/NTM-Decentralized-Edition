// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.client.model.Meshes;
import com.hbm.client.render.PlushieFigures;
import com.hbm.client.render.RenderPlushie;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.space.SpacePlushies.SpacePlushie;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** RenderPlushie's vocaloids: each a single mesh whose skin blushes while squeezed. */
public final class SpacePlushieFigures {

    private SpacePlushieFigures() {}

    /** NTM:Space blushes for the first five ticks of the eleven-tick squeeze (squishTimer > 6). */
    private static final RenderPlushie.Face BLUSH = new RenderPlushie.Face(7, Long.MAX_VALUE);
    private static final RenderPlushie.Face CALM = new RenderPlushie.Face(Long.MIN_VALUE, 6);

    public static void register() {
        Vocaloid teto = new Vocaloid("teto");
        PlushieFigures.register(SpacePlushie.TETO, new Figure(teto, 0.5F, 0.5F));
        PlushieFigures.register(SpacePlushie.FATO, new Figure(teto, 1.0F, 0.5F));
        PlushieFigures.register(SpacePlushie.MIKU, new Figure(new Vocaloid("miku"), 0.5F, 0.5F));
        PlushieFigures.register(SpacePlushie.NERU, new Figure(new Vocaloid("neru"), 0.5F, 0.5F));
    }

    /** One vocaloid's mesh and its two skins, loaded on first use on the render side. */
    private static final class Vocaloid {
        private final String name;
        private volatile @Nullable Loaded loaded;

        Vocaloid(String name) {
            this.name = name;
        }

        Loaded get() {
            Loaded l = loaded;
            if (l == null) {
                synchronized (this) {
                    l = loaded;
                    if (l == null) {
                        l = new Loaded(
                                Meshes.load(Library.id("models/trinkets/" + name + ".obj")),
                                new RenderPlushie.Part(Library.id("textures/models/trinkets/" + name + ".png"), true),
                                new RenderPlushie.Part(Library.id("textures/models/trinkets/" + name + "_squished.png"), true));
                        loaded = l;
                    }
                }
            }
            return l;
        }
    }

    private record Loaded(HFRWavefrontObject mesh, RenderPlushie.Part skin, RenderPlushie.Part blush) {}

    /** horizontal and vertical are NTM:Space's block scales; the item pose doubles them, as upstream does. */
    private record Figure(Vocaloid vocaloid, float horizontal, float vertical) implements RenderPlushie.Figure {

        @Override
        public void scale(PoseStack poses) {
            poses.scale(horizontal, vertical, horizontal);
        }

        @Override
        public void itemPose(PoseStack poses) {
            poses.translate(0, 0.25, 0);
            poses.scale(horizontal * 3F, vertical * 3F, horizontal * 3F);
        }

        @Override
        public void visitParts(PoseStack poses, RenderPlushie.PartVisitor visitor) {
            Loaded l = vocaloid.get();
            for (int group = 0; group < l.mesh().groups.length; group++) {
                visitor.accept(l.skin(), l.mesh(), group, CALM, poses);
                visitor.accept(l.blush(), l.mesh(), group, BLUSH, poses);
            }
        }

        @Override
        public List<HFRWavefrontObject> meshes() {
            return List.of(vocaloid.get().mesh());
        }
    }
}
