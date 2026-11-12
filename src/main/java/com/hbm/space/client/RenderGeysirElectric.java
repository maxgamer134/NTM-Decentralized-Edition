// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.BlockEntityRenderer;
import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.render.util.BeamPronter;
import com.hbm.space.SpaceGeysers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** NTM:Space's RenderVol2: the electric geyser's lightning spike, fading out after each strike. */
public class RenderGeysirElectric
        implements BlockEntityRenderer<SpaceGeysers.ElectricBE, RenderGeysirElectric.State>, ConcurrentRenderStateExtraction {

    public static class State extends BlockEntityRenderState {
        boolean charging;
        float flash;
        long time;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(SpaceGeysers.ElectricBE be) {
        return AABB.INFINITE;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(SpaceGeysers.ElectricBE be, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.charging = be.chargetime != 0;
        state.flash = be.flashd;
        state.time = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (s.charging) return;
        float alpha = 1F - Math.min(1F, s.flash / 110F);
        Vec3 spike = new Vec3(0, 0, -100 - s.flash);
        ps.pushPose();
        // RenderVol2's transform for an upright geyser: the spike runs along -Z, turned to point up
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.translate(0, 0.5, -0.5);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        ps.translate(0, 0.5, 0.5);
        BeamPronter.prontBeam(ps, col, spike, BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.SOLID,
                0x101020, 0x101020, 0, 8, 0F, 6, 3F * alpha, alpha * 2F);
        BeamPronter.prontBeam(ps, col, spike, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                0x202060, 0x202060, (int) (s.time / 2) % 1000, 26, 3F, 2, 0.125F, 0.5F * alpha);
        BeamPronter.prontBeam(ps, col, spike, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                0x202060, 0x202060, (int) (s.time / 4) % 1000, 26, 3F, 2, 0.125F, 0.5F * alpha);
        ps.popPose();
    }
}
