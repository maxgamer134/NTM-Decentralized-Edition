// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.ARGB;
import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.BlockEntityRenderer;
import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter;
import com.hbm.space.SpaceAntimatter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space's RenderAntimatter, the part the block model can't draw: once the bomb holds its
 * pellet, a tiny black sphere bobs over it inside a crackling containment beam.
 */
public class RenderNukeAntimatter
        implements BlockEntityRenderer<SpaceAntimatter.BombBE, RenderNukeAntimatter.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/bombs/blomb_tex.png");
    private static final float SCALE = 0.1F;

    public static class State extends BlockEntityRenderState {
        boolean core;
        long time;
        float partial;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceAntimatter.BombBE be, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.core = be.hasCore();
        state.time = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        state.partial = partialTicks;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.core) return;
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.pushPose();
        ps.translate(0, 2.4 + Math.sin(((s.time + s.partial) * 0.1) % (Math.PI * 2)) * 0.125 * SCALE, 0);
        ps.scale(SCALE, SCALE, SCALE);
        col.submitCustomGeometry(ps, RenderTypes.entitySolid(TEXTURE), (pose, buf) ->
                ResourceManager.sphere_uv.render(pose, buf, LightTexture.FULL_BRIGHT, ARGB.color(255, 1, 1, 1)));
        ps.popPose();
        ps.translate(0, 1, 0);
        Vec3 beam = new Vec3(0, 2.5, 0);
        BeamPronter.prontBeam(ps, col, beam, BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.SOLID,
                0x101020, 0x101020, 0, 1, 0F, 6, SCALE * 0.1F, 0.5F);
        BeamPronter.prontBeam(ps, col, beam, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                0x202060, 0x202060, (int) (s.time / 2) % 1000, 6, SCALE, 2, 0.0625F * SCALE, 0.5F);
        BeamPronter.prontBeam(ps, col, beam, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                0x202060, 0x202060, (int) (s.time / 4) % 1000, 6, SCALE, 2, 0.0625F * SCALE, 0.5F);
        ps.popPose();
    }
}
