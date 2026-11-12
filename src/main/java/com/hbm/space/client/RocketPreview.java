// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.gui.render.pip.PictureInPictureRenderer;
import com.hbm.backport.client.gui.state.pip.PictureInPictureRenderState;
import com.hbm.space.rocket.RocketStruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.LightTexture;
import org.jspecify.annotations.Nullable;

/** A rocket turning on its axis in a screen, stood on the bottom of its box. */
public final class RocketPreview extends PictureInPictureRenderer<RocketPreview.State> {

    /** {@code scale} is pixels per metre, {@code offset} how far up the rocket the view sits. */
    public record State(RocketStruct rocket, float scale, float offset, float spin, int x0, int y0, int x1, int y1)
            implements PictureInPictureRenderState {
        @Override
        public @Nullable ScreenRectangle scissorArea() {
            return null;
        }

        @Override
        public ScreenRectangle bounds() {
            return new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
        }
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected String getTextureLabel() {
        return "NTM:Space rocket preview";
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height - 8F * guiScale;
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose, SubmitNodeCollector collector) {
        pose.scale(-1, -1, 1);
        pose.mulPose(Axis.YN.rotationDegrees(state.spin()));
        pose.translate(0, -state.offset(), 0);
        RocketPronter.pront(state.rocket(), pose, collector, LightTexture.FULL_BRIGHT, false);
    }
}
