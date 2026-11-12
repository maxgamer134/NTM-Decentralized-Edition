// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.BlockEntityRenderer;
import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.client.model.Meshes;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.space.machine.SpaceGasDock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The rocket that climbs out of the gas dock to fetch its gas. */
public class RenderGasDock implements BlockEntityRenderer<SpaceGasDock.DockBE, RenderGasDock.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/gasrocket.png");
    private static @Nullable HFRWavefrontObject mesh;

    public static class State extends BlockEntityRenderState {
        float height;
        boolean visible;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceGasDock.DockBE be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.visible = be.launchTicks < 100;
        state.height = Mth.clamp(be.launchTicks + (be.hasRocket ? -partialTicks : partialTicks), 0, 200);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.visible) return;
        if (mesh == null) mesh = Meshes.load(Library.id("models/miner_rocket.obj"));
        HFRWavefrontObject m = mesh;
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.75 + s.height, 0.5);
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(TEXTURE), (pose, buf) -> m.render(pose, buf, light, -1));
        ps.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SpaceGasDock.DockBE be) {
        return new AABB(be.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 210, 0);
    }
}
