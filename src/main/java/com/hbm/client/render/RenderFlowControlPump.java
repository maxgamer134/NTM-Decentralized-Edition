// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blockentity.network.BlockEntityFlowControlPump;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.core.BlockEntityRenderer;
import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.core.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The pump body is the baked block model; this draws the rails and the spinning rotor. */
public class RenderFlowControlPump
        implements BlockEntityRenderer<BlockEntityFlowControlPump, RenderFlowControlPump.State>,
                ConcurrentRenderStateExtraction {
    private static final int RAIL_TOP = ResourceManager.flow_control_pump.partId("RailTop");
    private static final int RAIL_BOTTOM = ResourceManager.flow_control_pump.partId("RailBottom");
    private static final int ROTOR = ResourceManager.flow_control_pump.partId("Rotor");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public RenderFlowControlPump() {
        this.model = ResourceManager.flow_control_pump;
        this.bodyType = WorldRenderPipeline.oneSidedCutout(ResourceManager.flow_control_pump_tex);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityFlowControlPump be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 1,
                pos.getZ() + 2);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityFlowControlPump be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(BlockMultiblockCore.coreFacing(be.getBlockState()), 90);
        state.topRail = be.topRail;
        state.bottomRail = be.bottomRail;
        state.rotor = Mth.lerp(partialTicks, be.prevRotor, be.rotor);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        if (s.topRail) part(col, ps, light, RAIL_TOP);
        if (s.bottomRail) part(col, ps, light, RAIL_BOTTOM);

        ps.translate(0.0, 0.5, 0.0);
        ps.mulPose(Axis.ZN.rotationDegrees(s.rotor));
        ps.translate(0.0, -0.5, 0.0);
        part(col, ps, light, ROTOR);

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public boolean topRail;
        public boolean bottomRail;
        public float rotor;
    }
}
