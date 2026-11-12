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
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.model.Meshes;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.space.rocket.RocketStruct;
import com.hbm.space.station.SpaceStationLauncher;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The launching port: the docking port in its own livery, with the rocket being built hanging under it. */
public class RenderStationLauncher
        implements BlockEntityRenderer<SpaceStationLauncher.LauncherBE, RenderStationLauncher.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/docking_port_launcher.png");
    private static @Nullable HFRWavefrontObject mesh;
    private static int port, arm;

    private static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/machines/docking_port.obj"));
            port = m.partId("Port");
            arm = m.partId("ArmZP");
            mesh = m;
        }
        return mesh;
    }

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float arms;
        @Nullable RocketStruct rocket;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceStationLauncher.LauncherBE be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.arms = be.prevRot + (be.rot - be.prevRot) * partialTicks;
        RocketStruct rocket = be.rocket;
        state.rocket = rocket != null && rocket.extraIssues.isEmpty() && (rocket.capsule != null || !rocket.stages.isEmpty()) ? rocket : null;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        HFRWavefrontObject m = mesh();
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 1, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(switch (s.facing) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        }));
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(TEXTURE), (pose, buf) -> m.renderPart(pose, buf, light, -1, port));
        for (int i = 0; i < 4; i++) {
            ps.pushPose();
            ps.translate(0, -1.75, -2);
            ps.mulPose(Axis.XP.rotationDegrees(-s.arms));
            ps.translate(0, 1.75, 2);
            col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(TEXTURE), (pose, buf) -> m.renderPart(pose, buf, light, -1, arm));
            ps.popPose();
            ps.mulPose(Axis.YP.rotationDegrees(90));
        }
        if (s.rocket != null) {
            ps.translate(0, -s.rocket.getHeight() - 0.5, 0);
            RocketPronter.pront(s.rocket, ps, col, light, false);
        }
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(SpaceStationLauncher.LauncherBE be) {
        return AABB.INFINITE;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
