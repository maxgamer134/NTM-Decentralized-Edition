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
import com.hbm.space.rocket.RocketStruct;
import com.hbm.space.rocket.RocketStruct.RocketStage;
import com.hbm.space.rocket.SpaceAssembly;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The assembly building's frame, the rocket on it once it adds up, and a work deck per stage. */
public class RenderRocketAssembly
        implements BlockEntityRenderer<SpaceAssembly.AssemblyBE, RenderRocketAssembly.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/rocket_assembly.png");
    private static @Nullable HFRWavefrontObject mesh;
    private static int base, support, deck;

    private static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/machines/rocket_assembly.obj"));
            base = m.partId("Base");
            support = m.partId("Support");
            deck = m.partId("Level");
            mesh = m;
        }
        return mesh;
    }

    public static class State extends BlockEntityRenderState {
        @Nullable RocketStruct rocket;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceAssembly.AssemblyBE be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.rocket = be.rocket;
    }

    private static void part(PoseStack ps, SubmitNodeCollector col, int light, int part) {
        HFRWavefrontObject m = mesh();
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(TEXTURE), (pose, buf) -> m.renderPart(pose, buf, light, -1, part));
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        ps.pushPose();
        // NTM:Space's switch reads the core's raw metadata (12 and up), so no case matches and the
        // building is never turned: its frame and legs are the same from every side
        ps.translate(0.5, -2.0, 0.5);
        mesh();
        part(ps, col, s.lightCoords, base);
        RocketStruct rocket = s.rocket;
        if (rocket != null && rocket.extraIssues.isEmpty()) {
            ps.pushPose();
            ps.translate(0, 2.95, 0);
            if (!rocket.stages.isEmpty() && rocket.stages.get(0).thruster != null)
                ps.translate(0, -rocket.stages.get(0).thruster.height, 0);
            RocketPronter.pront(rocket, ps, col, s.lightCoords, false);
            ps.popPose();
            // the support and deck parts already sit two blocks up in the model
            for (int i = 0; i < rocket.stages.size(); i++) {
                RocketStage stage = rocket.stages.get(i);
                RocketStage next = i < rocket.stages.size() - 1 ? rocket.stages.get(i + 1) : null;
                double height = 0;
                if (stage.fuselage != null) height += stage.fuselage.height * stage.getStack();
                if (next != null && next.thruster != null) height += next.thruster.height;
                while (height > 1) {
                    ps.translate(0, 1, 0);
                    part(ps, col, s.lightCoords, support);
                    height--;
                }
                ps.translate(0, height, 0);
                part(ps, col, s.lightCoords, deck);
            }
        }
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(SpaceAssembly.AssemblyBE be) {
        return AABB.INFINITE;
    }
}
