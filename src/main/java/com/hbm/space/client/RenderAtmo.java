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
import com.hbm.space.machine.SpaceAtmoMachines;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The compressor's and the emitter's fans. */
public final class RenderAtmo {

    private RenderAtmo() {}

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float fan;
    }

    private static void part(SubmitNodeCollector col, PoseStack ps, HFRWavefrontObject m, ResourceLocation tex, int light, String name) {
        int id = m.partId(name);
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(tex), (pose, buf) -> m.renderPart(pose, buf, light, -1, id));
    }

    public static class Compressor
            implements BlockEntityRenderer<SpaceAtmoMachines.CompressorBE, State>, ConcurrentRenderStateExtraction {
        private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/atmo_vent.png");
        private static @Nullable HFRWavefrontObject mesh;

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(SpaceAtmoMachines.CompressorBE be, State state, float partialTicks, Vec3 camera,
                ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
            state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
            state.fan = be.prevRot + (be.rot - be.prevRot) * partialTicks;
        }

        @Override
        public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
            if (mesh == null) mesh = Meshes.load(Library.id("models/machines/atmo_vent.obj"));
            HFRWavefrontObject m = mesh;
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(180F));
            switch (s.facing) {
                case SOUTH -> {
                    ps.mulPose(Axis.YP.rotationDegrees(180F));
                    ps.translate(1, 0, 0);
                }
                case WEST -> {
                    ps.mulPose(Axis.YP.rotationDegrees(90F));
                    ps.translate(1, 0, -1);
                }
                case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270F));
                default -> ps.translate(0, 0, -1);
            }
            part(col, ps, m, TEXTURE, s.lightCoords, "Body_Cylinder");
            ps.translate(-0.19, 0, 0.19);
            ps.mulPose(Axis.YP.rotationDegrees(-s.fan));
            ps.translate(0.19, 0, -0.19);
            part(col, ps, m, TEXTURE, s.lightCoords, "Fan_Cylinder.001");
            ps.popPose();
        }

        @Override
        public AABB getRenderBoundingBox(SpaceAtmoMachines.CompressorBE be) {
            return new AABB(be.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 10, 0);
        }
    }

    public static class Emitter
            implements BlockEntityRenderer<SpaceAtmoMachines.EmitterBE, State>, ConcurrentRenderStateExtraction {
        private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/outtake.png");
        private static @Nullable HFRWavefrontObject mesh;

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(SpaceAtmoMachines.EmitterBE be, State state, float partialTicks, Vec3 camera,
                ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
            state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
            state.fan = be.prevFan + (be.fan - be.prevFan) * partialTicks;
        }

        @Override
        public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
            if (mesh == null) mesh = Meshes.load(Library.id("models/machines/outtake.obj"));
            HFRWavefrontObject m = mesh;
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(switch (s.facing) {
                case NORTH -> 90F;
                case WEST -> 180F;
                case SOUTH -> 270F;
                default -> 0F;
            }));
            ps.translate(-0.5, 0, 0.5);
            part(col, ps, m, TEXTURE, s.lightCoords, "Base");
            ps.mulPose(Axis.YP.rotationDegrees(-s.fan));
            part(col, ps, m, TEXTURE, s.lightCoords, "Fan");
            ps.popPose();
        }

        @Override
        public AABB getRenderBoundingBox(SpaceAtmoMachines.EmitterBE be) {
            return new AABB(be.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 3, 0);
        }
    }
}
