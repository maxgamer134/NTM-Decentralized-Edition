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
import com.hbm.space.machine.SpaceDyson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The Dyson machines' renderers: the spin launcher's arm and payload, the receiver's coils and beam, and the converters. */
public final class RenderDyson {

    private RenderDyson() {}

    private static final ResourceLocation BEAM_TEXTURE = Library.id("textures/models/machines/xenon_trail.png");

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float spin;
        float spinPrev;
        float partial;
        long time;
        boolean active;
        int beamLength;
        boolean payload;
        boolean flying;
        int payloadTicks;
    }

    private static float turn(Direction facing) {
        return switch (facing) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
    }

    private interface Fill<B> {
        void fill(B be, State state, float partial);
    }

    private interface Draw {
        void draw(State s, PoseStack ps, SubmitNodeCollector col, HFRWavefrontObject m, ResourceLocation tex, int light);
    }

    private abstract static class Base<B extends BlockEntity>
            implements BlockEntityRenderer<B, State>, ConcurrentRenderStateExtraction {
        private final String meshName;
        private final ResourceLocation texture;
        private final Fill<B> fill;
        private final Draw draw;
        private final AABB box;
        private @Nullable HFRWavefrontObject mesh;

        Base(String meshName, Fill<B> fill, Draw draw, AABB box) {
            this.meshName = meshName;
            this.texture = Library.id("textures/models/machines/" + meshName + ".png");
            this.fill = fill;
            this.draw = draw;
            this.box = box;
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(B be, State state, float partialTicks, Vec3 camera,
                ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
            state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
            state.partial = partialTicks;
            state.time = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
            fill.fill(be, state, partialTicks);
        }

        @Override
        public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
            if (mesh == null) mesh = Meshes.load(Library.id("models/machines/" + meshName + ".obj"));
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(turn(s.facing)));
            draw.draw(s, ps, col, mesh, texture, s.lightCoords);
            ps.popPose();
        }

        @Override
        public AABB getRenderBoundingBox(B be) {
            return box.move(be.getBlockPos());
        }
    }

    private static void part(SubmitNodeCollector col, PoseStack ps, HFRWavefrontObject m, ResourceLocation tex, int light, String name) {
        int id = m.partId(name);
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(tex), (pose, buf) -> m.renderPart(pose, buf, light, -1, id));
    }

    /** Two crossed strips from the dish out along its beam. */
    private static void beam(SubmitNodeCollector col, PoseStack ps, int length) {
        col.submitCustomGeometry(ps, RenderTypes.entityTranslucentEmissive(BEAM_TEXTURE), (pose, buf) -> {
            int color = 0xCCFF8800;
            float w = 0.4F;
            strip(buf, pose, color, -w, 0, w, 0, length);
            strip(buf, pose, color, 0, -w, 0, w, length);
        });
    }

    private static void strip(VertexConsumer buf, PoseStack.Pose pose, int color, float x0, float y0, float x1, float y1, float length) {
        float[][] corners = {{x0, y0, 0, 0, 0}, {x1, y1, 0, 1, 0}, {x1, y1, length, 1, length / 4F}, {x0, y0, length, 0, length / 4F}};
        for (float[] c : corners)
            buf.addVertex(pose, c[0], c[1], c[2]).setColor(color).setUv(c[3], c[4])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, 1, 0);
        for (int i = corners.length - 1; i >= 0; i--) {
            float[] c = corners[i];
            buf.addVertex(pose, c[0], c[1], c[2]).setColor(color).setUv(c[3], c[4])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose, 0, -1, 0);
        }
    }

    private static void coils(State s, PoseStack ps, SubmitNodeCollector col, HFRWavefrontObject m, ResourceLocation tex, int light) {
        float t = s.active ? s.time + s.partial : 0;
        ps.pushPose();
        ps.translate(0, 1.5, 0);
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(t));
        ps.translate(0, -1.5, 0);
        part(col, ps, m, tex, light, "Coil1");
        part(col, ps, m, tex, light, "Coil3");
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(-t));
        ps.translate(0, -1.5, 0);
        part(col, ps, m, tex, light, "Coil2");
        ps.popPose();
        ps.popPose();
    }

    public static class Receiver extends Base<SpaceDyson.ReceiverBE> {
        public Receiver() {
            super("dyson_receiver",
                    (be, s, p) -> {
                        s.active = be.isReceiving;
                        s.beamLength = be.beamLength;
                    },
                    (s, ps, col, m, tex, light) -> {
                        part(col, ps, m, tex, light, "DysonReceiver");
                        coils(s, ps, col, m, tex, light);
                        if (s.active) {
                            ps.pushPose();
                            ps.translate(0, 1.5, 0);
                            beam(col, ps, s.beamLength);
                            ps.popPose();
                        }
                    },
                    new AABB(-25, 0, -25, 26, 19, 26));
        }
    }

    public static class ConverterHE extends Base<SpaceDyson.ConverterHEBE> {
        public ConverterHE() {
            super("dyson_he_converter", (be, s, p) -> s.active = be.isConverting,
                    (s, ps, col, m, tex, light) -> {
                        part(col, ps, m, tex, light, "HE_Converter");
                        coils(s, ps, col, m, tex, light);
                    },
                    new AABB(-6, 0, -6, 7, 6, 7));
        }
    }

    public static class ConverterTU extends Base<SpaceDyson.ConverterTUBE> {
        public ConverterTU() {
            super("dyson_tu_converter", (be, s, p) -> {},
                    (s, ps, col, m, tex, light) ->
                            col.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(tex), (pose, buf) -> m.render(pose, buf, light, -1)),
                    new AABB(-6, 0, -6, 7, 6, 7));
        }
    }

    public static class ConverterAnatmo extends Base<SpaceDyson.ConverterAnatmoBE> {
        public ConverterAnatmo() {
            super("dyson_anatmogenesis", (be, s, p) -> s.active = be.isConverting,
                    (s, ps, col, m, tex, light) -> {
                        part(col, ps, m, tex, light, "Anatmogenesis");
                        if (s.active) {
                            double t = s.time + s.partial;
                            ps.translate(0, Math.sin(t * 0.1) * 0.025, 0);
                        }
                        part(col, ps, m, tex, light, "Coils");
                    },
                    new AABB(-6, 0, -6, 7, 6, 7));
        }
    }

    public static class Launcher extends Base<SpaceDyson.LauncherBE> {
        public Launcher() {
            super("dyson_spinlaunch",
                    (be, s, p) -> {
                        s.spin = be.lastRotation + (be.rotation - be.lastRotation) * p;
                        s.payload = be.satCount > 0 && !be.isSpinningDown;
                        s.flying = be.isSpinningDown;
                        s.payloadTicks = be.payloadTicks;
                    },
                    (s, ps, col, m, tex, light) -> {
                        part(col, ps, m, tex, light, "Launch");
                        ps.pushPose();
                        ps.translate(0, 8.5, 0);
                        ps.mulPose(Axis.XP.rotationDegrees(45F));
                        ps.mulPose(Axis.YP.rotationDegrees(s.spin));
                        ps.mulPose(Axis.XP.rotationDegrees(-45F));
                        ps.translate(0, -8.5, 0);
                        part(col, ps, m, tex, light, "The_Thing_That_Rotates");
                        if (s.payload) part(col, ps, m, tex, light, "Payload");
                        ps.popPose();
                        if (s.flying) {
                            float p = s.payloadTicks + s.partial;
                            ps.translate(1, 8.5, 0);
                            ps.mulPose(Axis.XP.rotationDegrees(45F));
                            ps.mulPose(Axis.YP.rotationDegrees(90F));
                            ps.translate(p * 10F, 0, 0);
                            ps.mulPose(Axis.XP.rotationDegrees(-45F));
                            ps.translate(0, -8.5, 0);
                            part(col, ps, m, tex, light, "Payload");
                        }
                    },
                    new AABB(-100, 0, -100, 101, 100, 101));
        }
    }
}
