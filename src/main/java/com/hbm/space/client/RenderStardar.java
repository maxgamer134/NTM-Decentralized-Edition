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
import com.hbm.space.rocket.SpaceStardar;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The StarDar: its legs and rack, and the dish turning to whatever it listens to. */
public class RenderStardar
        implements BlockEntityRenderer<SpaceStardar.StardarBE, RenderStardar.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/stardar.png");
    private static final float DISH_OFFSET = 10.6F;
    private static @Nullable HFRWavefrontObject mesh;
    private static int base, rotation, pitch;

    private static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/machines/stardar.obj"));
            base = m.partId("base");
            rotation = m.partId("rotation");
            pitch = m.partId("pitch");
            mesh = m;
        }
        return mesh;
    }

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float yaw, pitch;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceStardar.StardarBE be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.yaw = be.prevDishYaw + (be.dishYaw - be.prevDishYaw) * partialTicks;
        state.pitch = be.prevDishPitch + (be.dishPitch - be.prevDishPitch) * partialTicks;
    }

    private static void part(PoseStack ps, SubmitNodeCollector col, int light, int part) {
        HFRWavefrontObject m = mesh();
        col.submitCustomGeometry(ps, RenderTypes.entityCutout(TEXTURE), (pose, buf) -> m.renderPart(pose, buf, light, -1, part));
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        mesh();
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, -3, 0.5);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(switch (s.facing) {
            case NORTH -> 180F;
            case WEST -> 270F;
            case EAST -> 90F;
            default -> 0F;
        }));
        part(ps, col, light, base);
        ps.popPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        part(ps, col, light, rotation);
        ps.translate(0, DISH_OFFSET, 0);
        ps.mulPose(Axis.XP.rotationDegrees(s.pitch));
        ps.translate(0, -DISH_OFFSET, 0);
        part(ps, col, light, pitch);
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
    public AABB getRenderBoundingBox(SpaceStardar.StardarBE be) {
        return AABB.INFINITE;
    }
}
