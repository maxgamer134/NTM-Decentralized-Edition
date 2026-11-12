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
import com.hbm.client.ClientEffects;
import com.hbm.client.model.Meshes;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.main.ResourceManager;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.particle.RocketFlameColor;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.space.machine.SpaceHTR3;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The HTR3: its bell sways, the piston pumps and the cover breathes while it burns. */
public class RenderHTR3 implements BlockEntityRenderer<SpaceHTR3.HTR3BE, RenderHTR3.State>, ConcurrentRenderStateExtraction {

    private static @Nullable HFRWavefrontObject mesh;
    private static int center, pipeL1, pipeL2, pipeR1, pipeR2, coverTop, coverBottom, piston, shroudH, shroudV,
            suspTop, suspBottom, engine;

    private static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = Meshes.load(Library.id("models/machines/htr3.obj"));
            center = m.partId("Center");
            pipeL1 = m.partId("PipeL1");
            pipeL2 = m.partId("PipeL2");
            pipeR1 = m.partId("PipeR1");
            pipeR2 = m.partId("PipeR2");
            coverTop = m.partId("CoverTop");
            coverBottom = m.partId("CoverBottom");
            piston = m.partId("Piston");
            shroudH = m.partId("ShroudH");
            shroudV = m.partId("ShroudV");
            suspTop = m.partId("SuspensionTop");
            suspBottom = m.partId("SuspensionBottom");
            engine = m.partId("Engine");
            mesh = m;
        }
        return mesh;
    }

    public static class State extends BlockEntityRenderState {
        float yaw;
        double sway, h, v, piston, cover;
    }

    /** The exhaust: a rocket flame thrown out of the bell, coloured by the propellant as in NTM. */
    public static void contrail(SpaceHTR3.HTR3BE be, Direction dir) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockPos p = be.getBlockPos();
        ClientEffects.spawnRocketFlame(level, p.getX() + dir.getStepX() * 7, p.getY() + 1, p.getZ() + dir.getStepZ() * 7, 3F,
                dir.getStepX() * 10, 0, dir.getStepZ() * 10, 40 + level.random.nextInt(40), color(be.propellant()));
    }

    private static RocketFlameColor color(@Nullable Fluid type) {
        if (type == null) return RocketFlameColor.FLAME;
        if (type == NTMFluids.byName("GAS_WATZ") || type == NTMFluids.byName("WASTEGAS")
                || type == NTMFluids.byName("GASEOUS_THORIUM_BROMIDE")) return new RocketFlameColor(1F, 1F, 0F, true, false, false, 240);
        if (type == NTMFluids.byName("GASEOUS_SCHRABIDIUM_BROMIDE")) return new RocketFlameColor(1F, 1F, 1F, true, false, false, 240);
        if (type == NTMFluids.byName("GASEOUS_URANIUM_BROMIDE") || type == NTMFluids.byName("GASEOUS_PLUTONIUM_BROMIDE"))
            return new RocketFlameColor(0F, 1F, 0F, false, false, false, 240);
        return RocketFlameColor.FLAME;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceHTR3.HTR3BE be, State s, float partialTicks, Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTicks, camera, breakProgress);
        s.yaw = Facing.yaw(be.getBlockState().getValue(BlockMultiblockCore.FACING), 90);
        double t = be.lastTime + (be.time - be.lastTime) * partialTicks;
        double swayTimer = (t / 3D) % (Math.PI * 4);
        s.sway = (Math.sin(swayTimer) + Math.sin(swayTimer * 2) + Math.sin(swayTimer * 4) + 2.23255D) * 0.5;
        double bellTimer = (t / 5D) % (Math.PI * 4);
        s.h = (Math.sin(bellTimer + Math.PI) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
        s.v = (Math.sin(bellTimer) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
        s.piston = BobMathUtil.sps((t / 5D) % (Math.PI * 2));
        double coverTimer = (t / 5D) % (Math.PI * 4);
        s.cover = (Math.sin(coverTimer) + Math.sin(coverTimer * 2) + Math.sin(coverTimer * 4)) * 0.5;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        HFRWavefrontObject m = mesh();
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, -3.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        part(col, ps, m, light, center, pipeL1, pipeL2, pipeR1, pipeR2);

        ps.pushPose();
        ps.translate(0, 0, -s.sway * 0.125);
        ps.pushPose();
        ps.translate(0, 0, s.piston * 0.375D + 0.375D);
        part(col, ps, m, light, piston);
        ps.popPose();
        ps.pushPose();
        ps.translate(0, 3.5, -1);
        ps.mulPose(Axis.YP.rotationDegrees((float) (s.v * 2D)));
        ps.mulPose(Axis.XP.rotationDegrees((float) (s.h * 2D)));
        ps.translate(0, -3.5, 1);
        part(col, ps, m, light, engine);
        ps.popPose();
        ps.popPose();

        ps.pushPose();
        ps.translate(0, -s.h * 0.125, 0);
        part(col, ps, m, light, shroudH);
        ps.popPose();
        ps.pushPose();
        ps.translate(s.v * 0.125, 0, 0);
        part(col, ps, m, light, shroudV);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0, -s.cover * 0.125);
        part(col, ps, m, light, coverTop, coverBottom);
        ps.popPose();
        ps.pushPose();
        ps.translate(0, 0, 3.5);
        ps.scale(1F, 1F, (float) ((3 + s.cover * 0.125) / 3));
        ps.translate(0, 0, -3.5);
        part(col, ps, m, light, suspBottom, suspTop);
        ps.popPose();
        ps.popPose();
    }

    private static void part(SubmitNodeCollector col, PoseStack ps, HFRWavefrontObject m, int light, int... parts) {
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutCull(ResourceManager.lpw2_tex), (pose, buf) -> {
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
    public AABB getRenderBoundingBox(SpaceHTR3.HTR3BE be) {
        BlockPos p = be.getBlockPos();
        return new AABB(p.getX() - 10, p.getY() - 3, p.getZ() - 10, p.getX() + 11, p.getY() + 4, p.getZ() + 11);
    }
}
