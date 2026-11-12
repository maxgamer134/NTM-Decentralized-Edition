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
import com.hbm.space.machine.SpaceHydroponic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The bay's frame, with the grow lights glowing while it has power. */
public class RenderHydroponic
        implements BlockEntityRenderer<SpaceHydroponic.BayBE, RenderHydroponic.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/hydroponic.png");
    private static @Nullable HFRWavefrontObject mesh;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        boolean lights;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceHydroponic.BayBE be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.lights = be.lightsOn();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (mesh == null) mesh = Meshes.load(Library.id("models/machines/hydroponic.obj"));
        HFRWavefrontObject m = mesh;
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(switch (s.facing) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        }));
        int base = m.partId("Base");
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(TEXTURE), (pose, buf) -> m.renderPart(pose, buf, light, -1, base));
        if (s.lights) {
            int lights = m.partId("Lights");
            col.submitCustomGeometry(ps, RenderTypes.entityTranslucentEmissive(TEXTURE), (pose, buf) -> m.renderPart(pose, buf, 0xF000F0, -1, lights));
        }
        ps.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SpaceHydroponic.BayBE be) {
        return new AABB(be.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 2, 0);
    }
}
