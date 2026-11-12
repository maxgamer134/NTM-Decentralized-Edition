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
import com.hbm.space.machine.SpaceDishControl;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The StarDar remote access console, with a drive showing while the linked StarDar holds one. */
public class RenderDishControl
        implements BlockEntityRenderer<SpaceDishControl.DishControlBE, RenderDishControl.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/dish_controller.png");
    private static @Nullable HFRWavefrontObject mesh;
    private static int console, drive, joystick;

    private static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/machines/dish_controller.obj"));
            console = m.partId("dish_control");
            drive = m.partId("drive");
            joystick = m.partId("joystick");
            mesh = m;
        }
        return mesh;
    }

    public static class State extends BlockEntityRenderState {
        float yaw;
        boolean hasDrive;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceDishControl.DishControlBE be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(BlockMultiblockCore.FACING), 90);
        state.hasDrive = be.hasDrive;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        HFRWavefrontObject m = mesh();
        int light = s.lightCoords;
        boolean hasDrive = s.hasDrive;
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(TEXTURE), (pose, buf) -> {
            m.renderPart(pose, buf, light, -1, console);
            m.renderPart(pose, buf, light, -1, joystick);
            if (hasDrive) m.renderPart(pose, buf, light, -1, drive);
        });
        ps.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SpaceDishControl.DishControlBE be) {
        return new AABB(be.getBlockPos()).inflate(1, 0, 1);
    }
}
