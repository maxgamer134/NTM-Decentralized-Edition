// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.EntityRenderState;
import com.hbm.backport.client.core.EntityRenderer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.client.model.Meshes;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.space.SpaceRobots;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** The tankbot and the war behemoth, drawn as NTM:Space's RenderTankbot and RenderBehemoth drew them. */
public class RenderSpaceRobot<E extends LivingEntity> extends EntityRenderer<E, RenderSpaceRobot.State>
        implements ConcurrentRenderStateExtraction {

    private static final ResourceLocation TANKBOT_TEX = Library.id("textures/entity/robtex.png");
    private static final ResourceLocation BODY = Library.id("textures/entity/wm_body.png");
    private static final ResourceLocation EYE = Library.id("textures/entity/wm_eye.png");
    private static final ResourceLocation HELMET = Library.id("textures/entity/wm_helmet.png");
    private static final ResourceLocation LEG = Library.id("textures/entity/wm_leg.png");
    private static final ResourceLocation KNEE = Library.id("textures/entity/wm_knee.png");
    private static final ResourceLocation HIP = Library.id("textures/entity/wm_hip.png");

    private final boolean behemoth;
    private HFRWavefrontObject mesh;

    public RenderSpaceRobot(EntityRendererProvider.Context context, boolean behemoth) {
        super(context);
        this.behemoth = behemoth;
        this.shadowRadius = 1.0F;
        this.shadowStrength = 0F;
    }

    public static final class State extends EntityRenderState {
        float limbSwing, bodyYaw, headYawAbs, headTargetYaw, pitch, death;
        boolean hurt;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(E entity, State s, float pt) {
        super.extractRenderState(entity, s, pt);
        s.limbSwing = entity.walkAnimation.position(pt);
        s.bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        s.headYawAbs = Mth.rotLerp(pt, entity.yHeadRotO, entity.yHeadRot);
        s.hurt = entity.hurtTime > 0 || entity.deathTime > 0;
        s.death = entity.deathTime > 0 ? Math.min(1F, Mth.sqrt((entity.deathTime + pt - 1F) / 20F * 1.6F)) : 0F;
        if (entity instanceof SpaceRobots.Tankbot tankbot) {
            LivingEntity target = tankbot.syncedTarget();
            if (target != null) {
                double targetYaw = Math.atan2(target.getZ() - tankbot.getZ(), target.getX() - tankbot.getX()) * (180 / Math.PI) - 90;
                tankbot.headTargetYaw += Mth.wrapDegrees(targetYaw - tankbot.headTargetYaw) * 0.1;
            }
            s.headTargetYaw = (float) tankbot.headTargetYaw;
        } else if (entity instanceof SpaceRobots.WarBehemoth wb) {
            LivingEntity target = wb.syncedTarget();
            if (target != null) {
                double dx = target.getX() - wb.getX();
                double dy = (target.getY() - target.getEyeHeight()) - (wb.getY() + wb.getEyeHeight());
                double dz = target.getZ() - wb.getZ();
                double targetPitch = Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (-180 / Math.PI);
                wb.aimPitch += (float) (Mth.wrapDegrees(targetPitch - wb.aimPitch) * 0.1);
            }
            s.pitch = wb.aimPitch;
        }
    }

    private void draw(SubmitNodeCollector col, PoseStack ps, ResourceLocation tex, int light, int color, String part) {
        HFRWavefrontObject m = mesh;
        int id = m.partId(part);
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(tex), (pose, buf) -> m.renderPart(pose, buf, light, color, id));
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (mesh == null) mesh = Meshes.load(Library.id(behemoth ? "models/mobs/war_machine.obj" : "models/mobs/tankbot.obj"));
        int light = s.lightCoords;
        int color = s.hurt ? 0xFFFF9999 : -1;
        ps.pushPose();
        RenderSpaceFish.rot(ps, 180F - s.bodyYaw, 0, 1, 0);
        if (s.death > 0) RenderSpaceFish.rot(ps, s.death * 90F, 0, 0, 1);
        ps.scale(-1F, -1F, 1F);
        ps.translate(0F, -1.5078125F, 0F);
        ps.pushPose();
        RenderSpaceFish.rot(ps, 180, 1, 0, 0);
        ps.translate(0, -1.5F, 0);
        if (behemoth) behemoth(s, ps, col, light, color);
        else tankbot(s, ps, col, light, color);
        ps.popPose();
        ps.popPose();
        super.submit(s, ps, col, camera);
    }

    private void tankbot(State st, PoseStack ps, SubmitNodeCollector col, int light, int color) {
        ps.pushPose();
        ps.scale(2F, 2F, 2F);
        double walkCycle = st.limbSwing;
        double cy2 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI);
        double cy3 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI * 0.75);
        ResourceLocation t = TANKBOT_TEX;
        draw(col, ps, t, light, color, "body");
        ps.pushPose();
        leg(col, ps, t, light, color, 0.625, 0.0625, 10 - cy3 * 10, cy2 * -2, 0, "front2");
        leg(col, ps, t, light, color, 0.625, 0.4375, cy3 - cy2 * 10, cy2 - cy3 * -2, 0, "front1");
        leg(col, ps, t, light, color, 0.625, 0.9375, cy3 - cy2 * 10, cy2 - cy3 * -2, 0, "back1");
        // the last leg rocks forward and back instead of side to side
        ps.pushPose();
        ps.translate(0.25, 0.625, 0.9375);
        RenderSpaceFish.rot(ps, cy2 - cy3 * 5, 0, 1, 0);
        RenderSpaceFish.rot(ps, cy2 * 2, 1, 0, 0);
        ps.translate(-0.25, -0.625, -0.9375);
        for (String p : new String[] {"back2foot", "back2leg", "back2knee"}) draw(col, ps, t, light, color, p);
        ps.popPose();
        ps.popPose();
        ps.pushPose();
        RenderSpaceFish.rot(ps, -st.headTargetYaw + st.headYawAbs, 0, 1, 0);
        RenderSpaceFish.rot(ps, -90, 0, 1, 0);
        for (String p : new String[] {"head", "barrelbottom", "barreltop"}) draw(col, ps, t, light, color, p);
        ps.popPose();
        ps.popPose();
    }

    private void leg(SubmitNodeCollector col, PoseStack ps, ResourceLocation t, int light, int color, double y, double z,
            double yaw, double roll, double unused, String name) {
        ps.pushPose();
        ps.translate(0.25, y, z);
        RenderSpaceFish.rot(ps, yaw, 0, 1, 0);
        RenderSpaceFish.rot(ps, roll, 0, 0, 1);
        ps.translate(-0.25, -y, -z);
        for (String p : new String[] {name + "foot", name + "leg", name + "knee"}) draw(col, ps, t, light, color, p);
        ps.popPose();
    }

    private void behemoth(State st, PoseStack ps, SubmitNodeCollector col, int light, int color) {
        ps.pushPose();
        ps.scale(1.2F, 1.2F, 1.2F);
        double walkCycle = st.limbSwing;
        ps.translate(0, -0.5F, 0F);
        RenderSpaceFish.rot(ps, -180, 0, 1, 0);
        ps.pushPose();
        ps.translate(0, 8.6F, 0.5F);
        RenderSpaceFish.rot(ps, -st.pitch, 1, 0, 0);
        ps.translate(0, -8.7F, -0.5F);
        for (String p : new String[] {"body", "hatch1", "hatch2", "gun"}) draw(col, ps, BODY, light, color, p);
        draw(col, ps, HELMET, light, color, "helmet");
        draw(col, ps, EYE, light, color, "eye");
        ps.popPose();

        double stepHeight = 5.5, stepLength = 2.5, stepTime = walkCycle * 0.25;
        double leftFootY = Math.max(Math.sin(stepTime) * stepHeight - 2.0, 7.0);
        double leftFootX = Math.sin(stepTime) * stepLength + 5.0;
        double leftFootZ = Math.cos(stepTime) * stepLength + 1.5;
        double rightFootY = Math.max(Math.sin(stepTime + Math.PI) * stepHeight - 4.0, 7.0);
        double rightFootX = Math.sin(stepTime + Math.PI) * stepLength + 5.0;
        double rightFootZ = Math.cos(stepTime + Math.PI) * stepLength + 1.5;
        ps.pushPose();
        legIK(col, ps, light, color, "right", rightFootX, rightFootY, rightFootZ);
        ps.popPose();
        ps.pushPose();
        legIK(col, ps, light, color, "left", leftFootX, leftFootY, leftFootZ);
        ps.popPose();
        ps.popPose();
    }

    private void legIK(SubmitNodeCollector col, PoseStack ps, int light, int color, String leg, double footX, double footY, double footZ) {
        double thighLength = 6.0, shinLength = 8.0;
        double dx = footX, dy = footY - 8.5, dz = footZ - 1.5;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double cosKnee = (thighLength * thighLength + shinLength * shinLength - distance * distance) / (2 * thighLength * shinLength);
        double knee = Math.acos(cosKnee);
        double hip = Math.atan2(dy, dx) - Math.atan2(shinLength * Math.sin(knee), thighLength + shinLength * Math.cos(knee));
        ps.translate(0, 8.5F, 0.5F);
        RenderSpaceFish.rot(ps, hip * (180.0 / Math.PI), 1, 0, 0);
        ps.translate(0, -8.6F, -0.5F);
        draw(col, ps, HIP, light, color, "hip_" + leg);
        ps.translate(0, 8.6F, 0.5F);
        ps.pushPose();
        ps.translate(0, -1.8F, 0F);
        RenderSpaceFish.rot(ps, knee * (180.0 / Math.PI), 1, 0, 0);
        ps.translate(0, -6.7F, -0.5F);
        draw(col, ps, KNEE, light, color, "knee_" + leg);
        ps.translate(0, 6.7F, 0.5F);
        ps.translate(0, -5.6F, 0F);
        RenderSpaceFish.rot(ps, cosKnee * (-90.0 / Math.PI), 1, 0, 0);
        ps.translate(0, -1F, -0.5F);
        draw(col, ps, LEG, light, color, "leg_" + leg);
        ps.popPose();
    }
}
