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
import com.hbm.space.rocket.SpaceLaunchPad;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The launch pad, its service tower sized to the rocket, and the rocket standing on it. */
public class RenderLaunchPadRocket
        implements BlockEntityRenderer<SpaceLaunchPad.PadBE, RenderLaunchPadRocket.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/rocket_pad.png");
    private static final ResourceLocation SUPPORT = Library.id("textures/models/machines/rocket_pad_support.png");
    private static @Nullable HFRWavefrontObject mesh;
    private static int base, towerBase, segment, segmentSmall, cap;

    private static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/machines/rocket_pad.obj"));
            base = m.partId("Base");
            towerBase = m.partId("Tower_Base");
            segment = m.partId("Tower_Segment");
            segmentSmall = m.partId("Tower_Segment_Small");
            cap = m.partId("Tower_Cap");
            mesh = m;
        }
        return mesh;
    }

    public static class State extends BlockEntityRenderState {
        @Nullable RocketStruct rocket;
        Direction facing = Direction.NORTH;
        int height;
        boolean sky;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceLaunchPad.PadBE be, State state, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        state.rocket = be.rocket;
        state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.height = be.height;
        state.sky = be.canSeeSky;
    }

    private static void part(PoseStack ps, SubmitNodeCollector col, int light, ResourceLocation texture, int part, boolean cull) {
        HFRWavefrontObject m = mesh();
        col.submitCustomGeometry(ps, cull ? RenderTypes.entityCutoutCull(texture) : RenderTypes.entityCutout(texture),
                (pose, buf) -> m.renderPart(pose, buf, light, -1, part));
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(switch (s.facing) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        }));
        mesh();
        part(ps, col, light, TEXTURE, base, true);

        if (s.sky && s.height >= 8) {
            ps.pushPose();
            part(ps, col, light, SUPPORT, towerBase, false);
            for (int oy = 8; oy < s.height - 2; oy += 3) {
                part(ps, col, light, SUPPORT, segment, false);
                ps.translate(0, 3, 0);
            }
            ps.translate(0, -3, 0);
            for (int oy = 0; oy < (s.height - 2) % 3; oy++) {
                part(ps, col, light, SUPPORT, segmentSmall, false);
                ps.translate(0, 1, 0);
            }
            ps.translate(0, -1, 0);
            part(ps, col, light, SUPPORT, cap, false);
            ps.popPose();
        }

        if (s.rocket != null) {
            ps.translate(0, 3, 0);
            RocketPronter.pront(s.rocket, ps, col, light, false);
        }
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
    public AABB getRenderBoundingBox(SpaceLaunchPad.PadBE be) {
        return AABB.INFINITE;
    }
}
