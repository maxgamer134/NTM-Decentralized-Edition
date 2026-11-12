// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.ARGB;
import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.EntityRenderState;
import com.hbm.backport.client.core.EntityRenderer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.client.model.Meshes;
import com.hbm.client.render.BeamRenderTypes;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.Vertices;
import com.hbm.space.SpaceInvaders;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** The Radioactive Angel as NTM:Space's RenderBFA drew it: a pulsing flash behind it and six beating wings. */
public class RenderBFA extends EntityRenderer<SpaceInvaders.BFAngel, RenderBFA.State> implements ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEX = Library.id("textures/entity/bfa_body.png");
    private static final double SCALE = 2D;
    private HFRWavefrontObject mesh;

    public RenderBFA(EntityRendererProvider.Context context) {
        super(context);
        this.shadowStrength = 0F;
    }

    public static final class State extends EntityRenderState {
        float yaw, pitch, time;
        boolean beam;
        double beamLength;
        int age;
    }

    @Override
    protected boolean affectedByCulling(SpaceInvaders.BFAngel entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceInvaders.BFAngel entity, State s, float pt) {
        super.extractRenderState(entity, s, pt);
        s.yaw = Mth.rotLerp(pt, entity.yRotO, entity.getYRot());
        s.pitch = Mth.lerp(pt, entity.xRotO, entity.getXRot());
        s.time = entity.tickCount + pt;
        s.age = entity.tickCount;
        s.beam = entity.getBeam();
        s.beamLength = 0;
        if (s.beam) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int x = Mth.floor(entity.getX()), z = Mth.floor(entity.getZ());
            int iy = 0;
            for (int y = (int) Math.ceil(entity.getY()); y >= entity.level().getMinBuildHeight(); y--) {
                if (!entity.level().getBlockState(pos.set(x, y, z)).isAir()) {
                    iy = y;
                    break;
                }
            }
            s.beamLength = entity.getY() - iy;
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (mesh == null) mesh = Meshes.load(Library.id("models/mobs/balefire_angel.obj"));
        HFRWavefrontObject m = mesh;
        int light = s.lightCoords;
        var type = RenderTypes.entityCutoutNoCull(TEX);

        ps.pushPose();
        ps.translate(0, 1, 0);
        RenderSpaceFish.rot(ps, -s.yaw, 0, 1, 0);
        RenderSpaceFish.rot(ps, s.pitch, 1, 0, 0);

        double time = s.time;
        double cy0 = Math.sin(time * 0.3 % (Math.PI * 2));
        double cy1 = Math.sin(time * 0.25 % (Math.PI * 2) - Math.PI * 0.2);
        double cy2 = Math.sin(time * 0.3 % (Math.PI * 2) - Math.PI * 0.4);

        // Space passed the partial tick in twice here
        flash(ps, col, 0.4D, (float) time * 0.06F);

        ps.pushPose();
        ps.scale(2, 2, 2);
        ps.translate(0.5, 0.2, 0.5);
        ps.translate(0, 0.5F, 0);
        part(col, ps, type, m, light, "eye");
        part(col, ps, type, m, light, "body");
        wing(col, ps, type, m, light, "wingL1", cy0 * 2, 0, 0, 1, cy0 * 20);
        wing(col, ps, type, m, light, "wingR1", cy0 * -2, 0, 0, 1, cy0 * -20);
        wing(col, ps, type, m, light, "wingL2", cy1 * 5, 0, 0, 1, cy1 * 20);
        wing(col, ps, type, m, light, "wingR2", cy1 * -5, 0, 0, 1, cy1 * -20);
        wing(col, ps, type, m, light, "wingL3", cy1 * 5, 1, 0, 0, cy2 * 20);
        wing(col, ps, type, m, light, "wingR3", cy1 * -5, 1, 0, 0, cy2 * -20);
        ps.popPose();

        if (s.beam && s.beamLength > 0) {
            Vec3 skeleton = new Vec3(0, -s.beamLength, 0);
            BeamPronter.prontBeam(ps, col, skeleton, EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0x101020, 0x101020, 0,
                    (int) (s.beamLength + 1), 0F, 6, (float) SCALE * 0.75F);
            BeamPronter.prontBeam(ps, col, skeleton, EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, s.age / 2,
                    (int) (s.beamLength / 2 + 1), (float) SCALE * 1.5F, 2, 0.0625F);
            BeamPronter.prontBeam(ps, col, skeleton, EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, s.age / 4,
                    (int) (s.beamLength / 2 + 1), (float) SCALE * 1.5F, 2, 0.0625F);
        }
        ps.popPose();
        super.submit(s, ps, col, camera);
    }

    private static void part(SubmitNodeCollector col, PoseStack ps, net.minecraft.client.renderer.RenderType type, HFRWavefrontObject m,
            int light, String name) {
        int id = m.partId(name);
        col.submitCustomGeometry(ps, type, (pose, buf) -> m.renderPart(pose, buf, light, -1, id));
    }

    private static void wing(SubmitNodeCollector col, PoseStack ps, net.minecraft.client.renderer.RenderType type, HFRWavefrontObject m,
            int light, String name, double tilt, double ax, double ay, double az, double swing) {
        ps.pushPose();
        RenderSpaceFish.rot(ps, tilt, ax, ay, az);
        RenderSpaceFish.rot(ps, swing, 0, 1, 0);
        // upstream drew the wings between disable/enableStandardItemLighting: no directional shading
        int id = m.partId(name);
        col.submitCustomGeometry(ps, type, (pose, buf) -> m.renderPart(pose, unlit(buf), light, -1, id));
        ps.popPose();
    }

    /** 64 additive spikes, pinkish at the root, fading to green at the tips, turning slowly with the seed. */
    private static void flash(PoseStack ps, SubmitNodeCollector col, double intensity, float seed) {
        Random random = new Random(425L);
        ps.pushPose();
        ps.translate(1, 2.2, 1);
        float scale = 2;
        float i2 = (float) (intensity * intensity);
        int tip = argb(Math.min(1F, i2 * 2F), 1.0F, 0.6F, 0.6F);
        int edge = argb(0F, 0.1F, 0.6F, 0.1F);
        for (int i = 0; i < 64; i++) {
            RenderSpaceFish.rot(ps, random.nextFloat() * 360.0F + seed, 1, 0, 0);
            RenderSpaceFish.rot(ps, random.nextFloat() * 360.0F + seed, 0, 1, 0);
            RenderSpaceFish.rot(ps, random.nextFloat() * 360.0F + seed, 0, 0, 1);
            RenderSpaceFish.rot(ps, random.nextFloat() * 360.0F + seed, 1, 0, 0);
            RenderSpaceFish.rot(ps, random.nextFloat() * 360.0F + seed, 0, 1, 0);
            float v1 = (random.nextFloat() * 20.0F + 5.0F + 10.0F) * (i2 * scale);
            float v2 = (random.nextFloat() * 2.0F + 1.0F + 2.0F) * (i2 * scale);
            col.submitCustomGeometry(ps, BeamRenderTypes.ADDITIVE_CULL, (pose, buf) -> spike(pose, buf, tip, edge, v1, v2));
        }
        ps.popPose();
    }

    // the triangle fan centre, A, B, C, A as three degenerate quads
    private static void spike(PoseStack.Pose pose, VertexConsumer buf, int tip, int edge, float v1, float v2) {
        float ax = -0.866F * v2, az = -0.5F * v2;
        float bx = 0.866F * v2, bz = -0.5F * v2;
        float cx = 0F, cz = v2;
        tri(pose, buf, tip, edge, ax, v1, az, bx, v1, bz);
        tri(pose, buf, tip, edge, bx, v1, bz, cx, v1, cz);
        tri(pose, buf, tip, edge, cx, v1, cz, ax, v1, az);
    }

    private static void tri(PoseStack.Pose pose, VertexConsumer buf, int tip, int edge, float x1, float y1, float z1, float x2, float y2,
            float z2) {
        Vertices.emit(buf, pose, 0F, 0F, 0F, tip);
        Vertices.emit(buf, pose, x1, y1, z1, edge);
        Vertices.emit(buf, pose, x2, y2, z2, edge);
        Vertices.emit(buf, pose, x2, y2, z2, edge);
    }

    private static int argb(float a, float r, float g, float b) {
        return ARGB.color(Math.round(a * 255F), Math.round(r * 255F), Math.round(g * 255F), Math.round(b * 255F));
    }

    /** Every vertex faces straight up, which the entity shader lights fully: 1.7's unlit look. */
    private static com.mojang.blaze3d.vertex.VertexConsumer unlit(com.mojang.blaze3d.vertex.VertexConsumer d) {
        return new com.mojang.blaze3d.vertex.VertexConsumer() {
            @Override public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) { d.addVertex(x, y, z); return this; }
            @Override public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r, int g, int b, int a) { d.setColor(r, g, b, a); return this; }
            @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) { d.setUv(u, v); return this; }
            @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) { d.setUv1(u, v); return this; }
            @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) { d.setUv2(u, v); return this; }
            @Override public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) { d.setNormal(0F, 1F, 0F); return this; }
            @Override public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
                d.addVertex(x, y, z, color, u, v, overlay, light, 0F, 1F, 0F);
            }
        };
    }
}
