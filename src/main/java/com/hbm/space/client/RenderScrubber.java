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
import com.hbm.space.machine.SpaceScrubber;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The CO2 scrubber: its housing, and the fan spinning while it has power. */
public class RenderScrubber
        implements BlockEntityRenderer<SpaceScrubber.ScrubberBE, RenderScrubber.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/air_scrubber.png");
    private static @Nullable HFRWavefrontObject mesh;
    private static int base, fan;

    private static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/machines/air_scrubber.obj"));
            base = m.partId("Base");
            fan = m.partId("Fan");
            mesh = m;
        }
        return mesh;
    }

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float rotation;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceScrubber.ScrubberBE be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.rotation = Mth.lerp(partialTicks, be.prevRot, be.rot);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        HFRWavefrontObject m = mesh();
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(180F + switch (s.facing) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        }));
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(TEXTURE), (pose, buf) -> m.renderPart(pose, buf, light, -1, base));
        ps.mulPose(Axis.YP.rotationDegrees(s.rotation));
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(TEXTURE), (pose, buf) -> m.renderPart(pose, buf, light, -1, fan));
        ps.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SpaceScrubber.ScrubberBE be) {
        return new AABB(be.getBlockPos()).expandTowards(0, 1, 0).inflate(1);
    }
}
