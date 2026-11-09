// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blockentity.machine.BlockEntityLaunchpadLambda;
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
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.core.CameraRenderState;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * The silo is the baked block model; this draws the doors, the erector with its clamps and the
 * rocket. Main cuts the doors and the erector with GL clip planes set in the pad's frame, so the
 * planes are carried into each part's own frame here.
 */
public class RenderLaunchpadLambda
        implements BlockEntityRenderer<BlockEntityLaunchpadLambda, RenderLaunchpadLambda.State>,
                ConcurrentRenderStateExtraction {
    private static final HFRWavefrontObject PAD = ResourceManager.launchpad_lambda;
    private static final int DOOR_LEFT = PAD.partId("DoorLeft");
    private static final int DOOR_RIGHT = PAD.partId("DoorRight");
    private static final int ERECTOR = PAD.partId("Erector");
    private static final int ROTOR = PAD.partId("Rotor");
    private static final int PIVOT_UPPER_1 = PAD.partId("PivotUpper1");
    private static final int CLAMP_UPPER_1 = PAD.partId("ClampUpper1");
    private static final int PIVOT_LOWER_1 = PAD.partId("PivotLower1");
    private static final int CLAMP_LOWER_1 = PAD.partId("ClampLower1");
    private static final int PIVOT_UPPER_2 = PAD.partId("PivotUpper2");
    private static final int CLAMP_UPPER_2 = PAD.partId("ClampUpper2");
    private static final int PIVOT_LOWER_2 = PAD.partId("PivotLower2");
    private static final int CLAMP_LOWER_2 = PAD.partId("ClampLower2");
    private static final int ROCKET_PARTS = ResourceManager.lambda_rocket.getGroupNames().size();

    private final RenderType padType;
    private final RenderType rocketType;

    public RenderLaunchpadLambda() {
        this.padType = WorldRenderPipeline.oneSidedCutout(ResourceManager.launchpad_lambda_tex);
        this.rocketType = WorldRenderPipeline.oneSidedCutout(ResourceManager.lambda_rocket_tex);
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
    public AABB getRenderBoundingBox(BlockEntityLaunchpadLambda be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 7,
                pos.getY(),
                pos.getZ() - 7,
                pos.getX() + 8,
                pos.getY() + 30,
                pos.getZ() + 8);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityLaunchpadLambda be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        // the core is buried in the pad, light the moving parts from above it
        if (be.getLevel() != null)
            state.lightCoords =
                    LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above(3));
        state.yaw = Facing.yaw(BlockMultiblockCore.coreFacing(be.getBlockState()), 90);
        state.doors = be.getSmoothPos(BlockEntityLaunchpadLambda.INDEX_DOORS, partialTicks);
        state.erector =
                be.getSmoothPos(BlockEntityLaunchpadLambda.INDEX_ERECTOR, partialTicks) - 25F;
        state.rotor = be.getSmoothPos(BlockEntityLaunchpadLambda.INDEX_ROTOR, partialTicks);
        state.clamps = be.getSmoothPos(BlockEntityLaunchpadLambda.INDEX_CLAMPS, partialTicks);
        state.pistons = be.getSmoothPos(BlockEntityLaunchpadLambda.INDEX_PISTONS, partialTicks);
        state.renderRocket = be.erecting || be.erected;
        state.rocketFixed = be.erected;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        Chain c = new Chain(ps, col, s.lightCoords);

        if (s.rocketFixed) {
            c.push();
            c.translate(0, 2, 0);
            c.rotY(-s.yaw);
            c.rocket(rocketType, null);
            c.pop();
        }

        c.push();
        c.translate(0, 0, s.doors);
        c.part(padType, DOOR_LEFT, new float[] {0, 0, -1, 6});
        c.pop();

        c.push();
        c.translate(0, 0, -s.doors);
        c.part(padType, DOOR_RIGHT, new float[] {0, 0, 1, 6});
        c.pop();

        float[] floor = {0, 1, 0, -0.25F};
        c.push();
        c.translate(0, s.erector, 0);
        c.part(padType, ERECTOR, floor);

        c.translate(0, 13.25F, 0);
        c.rotX(s.rotor);
        c.translate(0, -13.25F, 0);
        c.part(padType, ROTOR, floor);

        clamp(c, floor, 3.5F, 19.75F, -s.clamps, s.pistons, PIVOT_UPPER_1, CLAMP_UPPER_1);
        clamp(c, floor, 3.5F, 6.75F, s.clamps, s.pistons, PIVOT_LOWER_1, CLAMP_LOWER_1);
        clamp(c, floor, -3.5F, 19.75F, s.clamps, -s.pistons, PIVOT_UPPER_2, CLAMP_UPPER_2);
        clamp(c, floor, -3.5F, 6.75F, -s.clamps, -s.pistons, PIVOT_LOWER_2, CLAMP_LOWER_2);

        if (s.renderRocket && !s.rocketFixed) {
            c.translate(0, 2, 0);
            c.rotY(-s.yaw);
            c.rocket(rocketType, floor);
        }
        c.pop();

        ps.popPose();
    }

    private void clamp(
            Chain c,
            float[] floor,
            float pivotX,
            float pivotY,
            float angle,
            float pistons,
            int pivot,
            int clamp) {
        c.push();
        c.translate(pivotX, pivotY, 0);
        c.rotZ(angle);
        c.translate(-pivotX, -pivotY, 0);
        c.part(padType, pivot, floor);
        c.translate(pistons, 0, 0);
        c.part(padType, clamp, floor);
        c.pop();
    }

    /** A pose stack that also keeps the transform from the pad's frame to the current part. */
    private static final class Chain {
        private final PoseStack ps;
        private final SubmitNodeCollector col;
        private final int light;
        private final Deque<Matrix4f> saved = new ArrayDeque<>();
        private Matrix4f local = new Matrix4f();

        Chain(PoseStack ps, SubmitNodeCollector col, int light) {
            this.ps = ps;
            this.col = col;
            this.light = light;
        }

        void push() {
            ps.pushPose();
            saved.push(new Matrix4f(local));
        }

        void pop() {
            ps.popPose();
            local = saved.pop();
        }

        void translate(float x, float y, float z) {
            ps.translate(x, y, z);
            local.translate(x, y, z);
        }

        void rotX(float degrees) {
            ps.mulPose(Axis.XP.rotationDegrees(degrees));
            local.rotateX((float) Math.toRadians(degrees));
        }

        void rotY(float degrees) {
            ps.mulPose(Axis.YP.rotationDegrees(degrees));
            local.rotateY((float) Math.toRadians(degrees));
        }

        void rotZ(float degrees) {
            ps.mulPose(Axis.ZP.rotationDegrees(degrees));
            local.rotateZ((float) Math.toRadians(degrees));
        }

        /**
         * A GL clip plane (a, b, c, D) of the pad's frame in the current part's frame, as the
         * mesh clipper takes it (keeps n . v - d >= 0).
         */
        private float[] localPlane(float[] gl) {
            Vector4f p = new Vector4f(gl[0], gl[1], gl[2], gl[3]);
            local.transformTranspose(p);
            return new float[] {p.x, p.y, p.z, -p.w};
        }

        void part(RenderType type, int part, float[] glPlane) {
            float[][] planes = {localPlane(glPlane)};
            int l = light;
            col.submitCustomGeometry(
                    ps,
                    type,
                    (pose, buffer) -> PAD.renderPartClipped(pose, buffer, l, -1, part, planes));
        }

        void rocket(RenderType type, float @Nullable [] glPlane) {
            int l = light;
            HFRWavefrontObject rocket = ResourceManager.lambda_rocket;
            if (glPlane == null) {
                col.submitCustomGeometry(ps, type, (pose, buffer) -> rocket.render(pose, buffer, l, -1));
                return;
            }
            float[][] planes = {localPlane(glPlane)};
            col.submitCustomGeometry(
                    ps,
                    type,
                    (pose, buffer) -> {
                        for (int i = 0; i < ROCKET_PARTS; i++)
                            rocket.renderPartClipped(pose, buffer, l, -1, i, planes);
                    });
        }
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float doors;
        public float erector;
        public float rotor;
        public float clamps;
        public float pistons;
        public boolean renderRocket;
        public boolean rocketFixed;
    }
}
