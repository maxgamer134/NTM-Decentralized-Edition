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
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.machine.SpaceOrrery;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The orrery's star at the block's centre, the bodies around it to scale and the orbits they follow. */
public class RenderOrrery implements BlockEntityRenderer<SpaceOrrery.OrreryBE, RenderOrrery.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public static class State extends BlockEntityRenderState {
        @Nullable CelestialBody sun;
        List<SolarSystem.OrreryMetric> metrics = List.of();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceOrrery.OrreryBE be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        Level level = be.getLevel();
        if (level == null) return;
        state.sun = CelestialBody.getStar(level);
        state.metrics = SolarSystem.calculatePositionsOrrery(level, partialTicks);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        CelestialBody sun = s.sun;
        if (sun == null) return;
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);

        col.submitCustomGeometry(ps, RenderTypes.entityTranslucentEmissive(sun.texture),
                (pose, buf) -> cube(pose, buf, 0, 0, 0, 1F, 0.375F, 0.625F));

        double scale = 1 / Math.min(sun.radiusKm, SolarSystem.ORRERY_MAX_RADIUS);
        for (SolarSystem.OrreryMetric metric : s.metrics) {
            double bodyScale = metric.body.radiusKm;
            if (bodyScale < 2_000) bodyScale = (bodyScale / 63) * (bodyScale / 63) + 1_000;
            float size = (float) (bodyScale * scale);
            // the orrery's y is up and NTM's orbital plane is x and y, so those two swap
            float x = (float) (metric.position.x * scale), y = (float) (metric.position.z * scale), z = (float) (metric.position.y * scale);
            col.submitCustomGeometry(ps, RenderTypes.entityTranslucentEmissive(metric.body.texture),
                    (pose, buf) -> cube(pose, buf, x, y, z, size, 0F, 1F));

            float[] c = metric.body.color;
            int argb = 0xFF000000 | ((int) (c[0] * 255) << 16) | ((int) (c[1] * 255) << 8) | (int) (c[2] * 255);
            col.submitCustomGeometry(ps, RenderTypes.entityTranslucentEmissive(WHITE), (pose, buf) -> {
                Vec3[] path = metric.orbitalPath;
                for (int i = 0; i < path.length; i++) {
                    Vec3 from = path[(i + path.length - 1) % path.length];
                    Vec3 to = path[i];
                    if (from == null || to == null) continue;
                    ribbon(pose, buf, (float) (from.x * scale), (float) (from.z * scale), (float) (from.y * scale),
                            (float) (to.x * scale), (float) (to.z * scale), (float) (to.y * scale), argb);
                }
            });
        }
        ps.popPose();
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buf, float x, float y, float z, float u, float v, int argb,
            float nx, float ny, float nz) {
        buf.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(pose, nx, ny, nz);
    }

    /** A cube of the given edge length around a point, its texture showing the uv range min to max on every face. */
    private static void cube(PoseStack.Pose pose, VertexConsumer buf, float cx, float cy, float cz, float size, float min, float max) {
        float h = size / 2;
        float x0 = cx - h, x1 = cx + h, y0 = cy - h, y1 = cy + h, z0 = cz - h, z1 = cz + h;
        quad(pose, buf, new float[] {x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0}, min, max, 0, 0, -1);
        quad(pose, buf, new float[] {x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1}, min, max, -1, 0, 0);
        quad(pose, buf, new float[] {x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1}, min, max, 0, 0, 1);
        quad(pose, buf, new float[] {x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0}, min, max, 1, 0, 0);
        quad(pose, buf, new float[] {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1}, min, max, 0, -1, 0);
        quad(pose, buf, new float[] {x1, y1, z0, x0, y1, z0, x0, y1, z1, x1, y1, z1}, min, max, 0, 1, 0);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buf, float[] p, float min, float max, float nx, float ny, float nz) {
        float[][] uv = {{max, max}, {min, max}, {min, min}, {max, min}};
        for (int i = 0; i < 4; i++) vertex(pose, buf, p[i * 3], p[i * 3 + 1], p[i * 3 + 2], uv[i][0], uv[i][1], -1, nx, ny, nz);
    }

    /** A thin double-sided strip along an orbit's segment, drawn as two crossed quads so it shows from any side. */
    private static void ribbon(PoseStack.Pose pose, VertexConsumer buf, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        float w = 0.004F;
        for (int axis = 0; axis < 2; axis++) {
            float dx = axis == 0 ? w : 0, dy = axis == 1 ? w : 0;
            vertex(pose, buf, x0 - dx, y0 - dy, z0, 0, 0, argb, 0, 1, 0);
            vertex(pose, buf, x0 + dx, y0 + dy, z0, 0, 1, argb, 0, 1, 0);
            vertex(pose, buf, x1 + dx, y1 + dy, z1, 1, 1, argb, 0, 1, 0);
            vertex(pose, buf, x1 - dx, y1 - dy, z1, 1, 0, argb, 0, 1, 0);
        }
    }

    @Override
    public AABB getRenderBoundingBox(SpaceOrrery.OrreryBE be) {
        return new AABB(be.getBlockPos()).inflate(49, 19, 49);
    }
}
