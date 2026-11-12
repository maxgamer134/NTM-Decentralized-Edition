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
import com.hbm.space.machine.SpaceTransporter;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The transporter pad: its fuel pipe swings away while the rocket lifts off and comes down. */
public class RenderTransporterRocket
        implements BlockEntityRenderer<SpaceTransporter.TransporterBE, RenderTransporterRocket.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation PAD_TEX = Library.id("textures/models/machines/transporter_pad.png");
    private static final ResourceLocation ROCKET_TEX = Library.id("textures/models/miner_rocket.png");
    private static @Nullable HFRWavefrontObject pad, rocket;
    private static int base, pipe;

    public static class State extends BlockEntityRenderState {
        float yaw, pipeRot, height;
        boolean rocketVisible;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceTransporter.TransporterBE be, State s, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTicks, camera, breakProgress);
        s.yaw = Facing.yaw(be.facing(), 90);
        s.pipeRot = Mth.clamp(pipeEngage(be, partialTicks), 0, 1);
        s.rocketVisible = be.launchTicks < 100;
        s.height = Mth.clamp(be.launchTicks + (be.hasRocket ? -partialTicks : partialTicks), 0, 200);
    }

    private static float pipeEngage(SpaceTransporter.TransporterBE be, float interp) {
        if (be.launchTicks >= 0) return !be.hasRocket ? (be.launchTicks + interp) * 0.25F : 1.0F;
        return 1 - (-be.launchTicks - 1 + interp) * 0.25F;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (pad == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/machines/transporter_pad.obj"));
            base = m.partId("base");
            pipe = m.partId("pipe");
            pad = m;
        }
        if (rocket == null) rocket = Meshes.load(Library.id("models/miner_rocket.obj"));
        HFRWavefrontObject p = pad, r = rocket;
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(PAD_TEX), (pose, buf) -> p.renderPart(pose, buf, light, -1, base));

        ps.pushPose();
        ps.translate(0, 0.75, -0.75);
        ps.mulPose(Axis.XP.rotationDegrees(s.pipeRot * -30F));
        ps.translate(0, -0.75, 0.75);
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(PAD_TEX), (pose, buf) -> p.renderPart(pose, buf, light, -1, pipe));
        ps.popPose();

        if (s.rocketVisible) {
            ps.pushPose();
            ps.translate(0, 0.75 + s.height, 0);
            col.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(ROCKET_TEX), (pose, buf) -> r.render(pose, buf, light, -1));
            ps.popPose();
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
    public AABB getRenderBoundingBox(SpaceTransporter.TransporterBE be) {
        BlockPos p = be.getBlockPos();
        return new AABB(p).inflate(2, 0, 2).expandTowards(0, 210, 0);
    }
}
