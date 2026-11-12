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
import com.hbm.space.machine.SpaceXenonThruster;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The xenon thruster, and the exhaust trail that stretches with its thrust. */
public class RenderXenonThruster
        implements BlockEntityRenderer<SpaceXenonThruster.ThrusterBE, RenderXenonThruster.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/xenon_thruster.png");
    private static final ResourceLocation TRAIL = Library.id("textures/models/machines/xenon_trail.png");
    private static @Nullable HFRWavefrontObject mesh;
    private static int thruster, exhaust;

    private static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/machines/xenon_thruster.obj"));
            thruster = m.partId("Thruster");
            exhaust = m.partId("Exhaust");
            mesh = m;
        }
        return mesh;
    }

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float thrust;
        float flicker;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceXenonThruster.ThrusterBE be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.thrust = be.thrustAmount;
        float r = be.getLevel() == null ? 0F : be.getLevel().random.nextFloat();
        state.flicker = 1.2F - r * r * 0.2F;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        HFRWavefrontObject m = mesh();
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, -1.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-90F + switch (s.facing) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        }));
        ps.translate(0, 0, -0.5);
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(TEXTURE), (pose, buf) -> m.renderPart(pose, buf, light, -1, thruster));
        float stretch = s.flicker * s.thrust;
        if (stretch > 0F) {
            ps.translate(0, 0, -1F);
            ps.scale(1F, 1F, stretch);
            ps.translate(0, 0, 1F);
            int glow = 0xF000F0;
            int color = ((int) (s.thrust * 255F) << 24) | 0xFFFFFF;
            col.submitCustomGeometry(ps, RenderTypes.entityTranslucentEmissive(TRAIL), (pose, buf) -> m.renderPart(pose, buf, glow, color, exhaust));
        }
        ps.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SpaceXenonThruster.ThrusterBE be) {
        return new AABB(be.getBlockPos()).inflate(3, 2, 3);
    }
}
