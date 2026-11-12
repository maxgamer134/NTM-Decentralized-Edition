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
import com.hbm.space.machine.SpaceAtmoTower;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The atmosphere tower's column. */
public class RenderAtmoTower
        implements BlockEntityRenderer<SpaceAtmoTower.TowerBE, RenderAtmoTower.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/machine_atmo_tower.png");
    private static @Nullable HFRWavefrontObject mesh;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceAtmoTower.TowerBE be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (mesh == null) mesh = Meshes.load(Library.id("models/machines/atmo_tower.obj"));
        HFRWavefrontObject m = mesh;
        int light = s.lightCoords;
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
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(TEXTURE), (pose, buf) -> m.render(pose, buf, light, -1));
        ps.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SpaceAtmoTower.TowerBE be) {
        return new AABB(be.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 7, 0);
    }
}
