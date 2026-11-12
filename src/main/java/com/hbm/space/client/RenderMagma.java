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
import com.hbm.blockentity.machine.BlockEntityCrucible.PourStream;
import com.hbm.client.model.Meshes;
import com.hbm.client.render.FlatCutout;
import com.hbm.client.render.RenderCrucible;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.space.machine.SpaceMagma;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The magma drill: its head and blades counter-spin, lava wells up under it and its spout pours. */
public class RenderMagma implements BlockEntityRenderer<SpaceMagma.MagmaBE, RenderMagma.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEXTURE = Library.id("textures/models/machines/magma_drill.png");
    private static final ResourceLocation LAVA = ResourceLocation.withDefaultNamespace("block/lava_still");
    private static @Nullable HFRWavefrontObject mesh;
    private static int foundation, drill, pistons, cables, drillHead, blades;

    private static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/machines/magma_drill.obj"));
            foundation = m.partId("Foundation");
            drill = m.partId("Drill");
            pistons = m.partId("Pistons");
            cables = m.partId("Cables");
            drillHead = m.partId("DrillHead");
            blades = m.partId("Blades");
            mesh = m;
        }
        return mesh;
    }

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float rotation, lavaHeight;
        double time;
        final List<PourStream> streams = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceMagma.MagmaBE be, State s, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTicks, camera, breakProgress);
        s.facing = be.facing();
        s.rotation = Mth.lerp(partialTicks, be.prevDrillRotation, be.drillRotation);
        s.lavaHeight = Mth.lerp(partialTicks, be.prevLavaHeight, be.lavaHeight);
        s.time = be.getLevel().getGameTime() + partialTicks;
        s.streams.clear();
        long now = be.getLevel().getGameTime();
        for (PourStream stream : be.streams) if (now - stream.birth() + partialTicks < 20F) s.streams.add(stream);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        HFRWavefrontObject m = mesh();
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, -3.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(Facing.yaw(s.facing, 180)));
        part(col, ps, m, light, foundation, drill, pistons, cables);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.rotation));
        part(col, ps, m, light, drillHead);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-s.rotation));
        part(col, ps, m, light, blades);
        ps.popPose();

        float h = s.lavaHeight;
        if (h > 0.01F) {
            TextureAtlasSprite lava = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(LAVA);
            col.submitCustomGeometry(ps, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, buf) -> {
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        float x0 = ox - 0.5F, x1 = ox + 0.5F, z0 = oz - 0.5F, z1 = oz + 0.5F;
                        float[][] corners = {{x0, z0, lava.getU0(), lava.getV0()}, {x0, z1, lava.getU0(), lava.getV1()},
                            {x1, z1, lava.getU1(), lava.getV1()}, {x1, z0, lava.getU1(), lava.getV0()}};
                        for (float[] c : corners) {
                            buf.addVertex(pose, c[0], h, c[1]).setColor(-1).setUv(c[2], c[3])
                                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                                    .setNormal(pose, 0F, 1F, 0F);
                        }
                    }
                }
            });
        }
        ps.popPose();

        Direction dir = s.facing;
        for (PourStream stream : s.streams) {
            ps.pushPose();
            ps.translate(0.5 + dir.getStepX() * 3.875, 1.0, 0.5 + dir.getStepZ() * 3.875);
            float age = (float) (s.time - stream.birth());
            col.submitCustomGeometry(ps, FlatCutout.of(ResourceManager.foundry_stream_tex), (pose, buf) ->
                    RenderCrucible.emitStream(pose, buf, stream.color(), dir, stream.len(), age, 0.625F, 0.625F));
            ps.popPose();
        }
    }

    private static void part(SubmitNodeCollector col, PoseStack ps, HFRWavefrontObject m, int light, int... parts) {
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(TEXTURE), (pose, buf) -> {
            for (int p : parts) m.renderPart(pose, buf, light, -1, p);
        });
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
    public AABB getRenderBoundingBox(SpaceMagma.MagmaBE be) {
        BlockPos p = be.getBlockPos();
        return new AABB(p.getX() - 4, p.getY() - 7, p.getZ() - 4, p.getX() + 5, p.getY() + 4, p.getZ() + 5);
    }
}
