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
import com.hbm.client.render.FusionPlasmaRenderTypes;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.space.machine.SpaceHTRF;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import com.hbm.backport.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The HTR-F4 NEO: its rotor spins up with a full charge and the plasma trail stretches out of the bell. */
public class RenderHTRF implements BlockEntityRenderer<SpaceHTRF.HTRFBE, RenderHTRF.State>, ConcurrentRenderStateExtraction {

    private static final ResourceLocation TEX = Library.id("textures/block/models/fusion/htrf4.png");
    private static final ResourceLocation TRAIL_TEX = Library.id("textures/models/fusion/htrf4_trail.png");

    private static @Nullable HFRWavefrontObject mesh;
    private static int base, engine, rotor, rotor2, exhaust;

    private static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/fusion/htrf4.obj"));
            base = m.partId("Base");
            engine = m.partId("Engine");
            rotor = m.partId("Rotor");
            rotor2 = m.partId("Rotor2");
            exhaust = m.partId("Exhaust");
            mesh = m;
        }
        return mesh;
    }

    public static class State extends BlockEntityRenderState {
        float yaw, rot, stretch, thrust, r, g, b;
        double sparkleSpin;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceHTRF.HTRFBE be, State s, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTicks, camera, breakProgress);
        s.yaw = Facing.yaw(be.getBlockState().getValue(BlockMultiblockCore.FACING), 90);
        s.rot = be.prevRotor + (be.rotor - be.prevRotor) * partialTicks;
        float stretch = be.getLevel() != null ? be.getLevel().random.nextFloat() : 0F;
        s.stretch = (1.2F - stretch * stretch * 0.1F) * be.thrustAmount;
        s.thrust = be.thrustAmount;
        s.r = be.plasmaR;
        s.g = be.plasmaG;
        s.b = be.plasmaB;
        s.sparkleSpin = System.currentTimeMillis() / 250D % 1D;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        HFRWavefrontObject m = mesh();
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, -2.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(TEX), (pose, buf) -> {
            m.renderPart(pose, buf, light, -1, base);
            m.renderPart(pose, buf, light, -1, engine);
        });

        ps.pushPose();
        ps.translate(0, 2.5, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(s.rot));
        ps.translate(0, -2.5, 0);
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(TEX), (pose, buf) -> {
            m.renderPart(pose, buf, light, -1, rotor);
            m.renderPart(pose, buf, light, -1, rotor2);
        });
        ps.popPose();

        if (s.stretch > 0) {
            ps.translate(0, 0, 12);
            ps.scale(1F, 1F, s.stretch);
            ps.translate(0, 0, -12);
            int full = 0xF000F0;
            int trail = ARGB.colorFromFloat(clamp(s.thrust), clamp(s.r), clamp(s.g), clamp(s.b));
            col.submitCustomGeometry(ps, RenderTypes.entityTranslucent(TRAIL_TEX),
                    (pose, buf) -> m.renderPart(pose, buf, full, trail, exhaust));
            // NTM turned the texture matrix sideways here, so the sparkles run down the trail as rings
            int sparkle = ARGB.colorFromFloat(clamp(s.thrust * 0.75F), clamp(s.r * 2F), clamp(s.g * 2F), clamp(s.b * 2F));
            col.submitCustomGeometry(ps, FusionPlasmaRenderTypes.plasma(ResourceManager.fusion_plasma_sparkle_tex),
                    (pose, buf) -> m.renderPart(pose, buf, full, sparkle, exhaust, 0F, 1F, 0F, (float) s.sparkleSpin));
        }
        ps.popPose();
    }

    private static float clamp(float f) {
        return Math.max(0F, Math.min(1F, f));
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
    public AABB getRenderBoundingBox(SpaceHTRF.HTRFBE be) {
        BlockPos p = be.getBlockPos();
        return new AABB(p.getX() - 11, p.getY() - 2, p.getZ() - 11, p.getX() + 12, p.getY() + 3, p.getZ() + 12);
    }
}
