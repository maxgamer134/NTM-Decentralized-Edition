// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.core.BlockEntityRenderState;
import com.hbm.backport.client.core.BlockEntityRenderer;
import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.EntityRenderState;
import com.hbm.backport.client.core.EntityRenderer;
import com.hbm.backport.client.core.ModelFeatureRenderer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.client.model.Meshes;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.space.SpaceInvaders;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Random;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The combat drop pod: landed, tilted at random with its two hatches swinging open, as NTM:Space's RenderCombatPod drew it. */
public class RenderCombatPod implements BlockEntityRenderer<SpaceInvaders.PodBE, RenderCombatPod.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation WHITE = Library.id("textures/models/bombs/combat_drop_pod.white.png");
    private static final ResourceLocation RED = Library.id("textures/models/bombs/combat_drop_pod.red.png");
    private static final ResourceLocation YELLOW = Library.id("textures/models/bombs/combat_drop_pod.yellow.png");

    private static @Nullable HFRWavefrontObject mesh;
    private static int bomb, hatch1, hatch2;

    static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/combat_drop_pod.obj"));
            bomb = m.partId("bomb");
            hatch1 = m.partId("hatch1");
            hatch2 = m.partId("hatch2");
            mesh = m;
        }
        return mesh;
    }

    static ResourceLocation skin(int color) {
        return switch (color) {
            case 1 -> RED;
            case 2 -> YELLOW;
            default -> WHITE;
        };
    }

    public static class State extends BlockEntityRenderState {
        double yaw, pitch, roll, open, open2;
        int color;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceInvaders.PodBE be, State s, float pt, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, s, pt, camera, breakProgress);
        BlockPos p = be.getBlockPos();
        Random rand = new Random((p.getY() + p.getZ() * 27644437) * 27644437 + p.getX());
        s.yaw = rand.nextDouble() * 360;
        s.pitch = rand.nextDouble() * 8;
        s.roll = rand.nextDouble() * 6;
        s.color = be.color;
        s.open = be.prevHatchopen + (be.hatchopen - be.prevHatchopen) * pt;
        s.open2 = be.prevHatchopen2 + (be.hatchopen2 - be.prevHatchopen2) * pt;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        HFRWavefrontObject m = mesh();
        int light = s.lightCoords;
        var type = RenderTypes.entityCutoutCull(skin(s.color));
        ps.pushPose();
        ps.translate(0.5, 1.5, 0.5);
        ps.translate(0, -0.25F, 0);
        RenderSpaceFish.rot(ps, s.yaw, 0, 1, 0);
        RenderSpaceFish.rot(ps, s.pitch, 1, 0, 0);
        RenderSpaceFish.rot(ps, s.roll, 0, 0, 1);

        ps.pushPose();
        ps.translate(0, s.open * 0.013, s.open * 0.01);
        RenderSpaceFish.rot(ps, s.open, 1, 0, 0);
        col.submitCustomGeometry(ps, type, (pose, buf) -> m.renderPart(pose, buf, light, -1, hatch1));
        ps.popPose();

        ps.pushPose();
        ps.translate(0, -s.open2 * 0.013, -s.open * 0.01);
        RenderSpaceFish.rot(ps, s.open2, 1, 0, 0);
        col.submitCustomGeometry(ps, type, (pose, buf) -> m.renderPart(pose, buf, light, -1, hatch2));
        ps.popPose();

        col.submitCustomGeometry(ps, type, (pose, buf) -> m.renderPart(pose, buf, light, -1, bomb));
        ps.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SpaceInvaders.PodBE be) {
        BlockPos p = be.getBlockPos();
        return new AABB(p.getX() - 3, p.getY(), p.getZ() - 3, p.getX() + 4, p.getY() + 8, p.getZ() + 4);
    }

    /** The pod on its way down: the whole model, upright, in its skin colour. */
    public static class Falling extends EntityRenderer<SpaceInvaders.CombatDropPod, Falling.State> implements ConcurrentRenderStateExtraction {

        public Falling(EntityRendererProvider.Context context) {
            super(context);
        }

        public static final class State extends EntityRenderState {
            int color;
        }

        @Override
        protected boolean affectedByCulling(SpaceInvaders.CombatDropPod entity) {
            return false;
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(SpaceInvaders.CombatDropPod entity, State s, float pt) {
            super.extractRenderState(entity, s, pt);
            s.color = entity.getColor();
        }

        @Override
        public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
            HFRWavefrontObject m = mesh();
            int light = s.lightCoords;
            col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(skin(s.color)), (pose, buf) -> m.render(pose, buf, light, -1));
            super.submit(s, ps, col, camera);
        }
    }
}
