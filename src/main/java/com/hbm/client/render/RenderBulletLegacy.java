// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.EntityRenderState;
import com.hbm.backport.client.core.EntityRenderer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.hbm.items.weapon.legacy.BulletConfiguration;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's render/entity/projectile/RenderBullet for the
 * pre-rework bullets. Styles whose family isn't ported yet draw as the plain rifle bullet.
 */
public class RenderBulletLegacy extends EntityRenderer<EntityBulletBaseNT, RenderBulletLegacy.State>
        implements ConcurrentRenderStateExtraction {

    private static final ResourceLocation BULLET_RIFLE = Library.id("textures/models/projectiles/bullet_rifle.png");
    private static final ResourceLocation BULLET_PISTOL = Library.id("textures/models/projectiles/bullet_pistol.png");
    private static final ResourceLocation BUCKSHOT = Library.id("textures/models/projectiles/pellet_buckshot.png");
    private static final ResourceLocation TAU = Library.id("textures/models/tau.png");
    private static final ResourceLocation EMPLACER = Library.id("textures/models/emplacer.png");

    private static final int PART_RIFLE = ResourceManager.projectiles.partId("BulletRifle");
    private static final int PART_PISTOL = ResourceManager.projectiles.partId("BulletPistol");
    private static final int PART_BUCKSHOT = ResourceManager.projectiles.partId("Buckshot");

    public RenderBulletLegacy(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityBulletBaseNT bullet, State state, float partialTicks) {
        super.extractRenderState(bullet, state, partialTicks);
        state.yaw = Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot());
        state.pitch = Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot());
        state.style = bullet.getStyle();
        state.trail = bullet.getTrail();
        state.id = bullet.getId();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(state.pitch + 180));
        pose.scale(1.5F, 1.5F, 1.5F);

        if (state.style != BulletConfiguration.STYLE_BLADE)
            pose.mulPose(Axis.XP.rotationDegrees(new Random(state.id).nextInt(90) - 45));

        int light = state.lightCoords;
        switch (state.style) {
            case BulletConfiguration.STYLE_NONE -> {}
            case BulletConfiguration.STYLE_PISTOL -> model(collector, pose, BULLET_PISTOL, PART_PISTOL, light);
            case BulletConfiguration.STYLE_BOLT -> dart(collector, pose, state.trail, state.id);
            case BulletConfiguration.STYLE_PELLET -> model(collector, pose, BUCKSHOT, PART_BUCKSHOT, light);
            default -> bullet(collector, pose, state.trail, light);
        }

        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    private static void bullet(SubmitNodeCollector collector, PoseStack pose, int type, int light) {
        // main draws trails 1 and 2 with its java ModelBullet; the rifle mesh stands in for it
        ResourceLocation tex = type == 2 ? EMPLACER : type == 1 ? TAU : BULLET_RIFLE;
        model(collector, pose, tex, PART_RIFLE, light);
    }

    private static void model(SubmitNodeCollector collector, PoseStack pose, ResourceLocation tex, int part, int light) {
        pose.pushPose();
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        RenderType type = RenderTypes.entityCutoutCull(tex);
        collector.submitCustomGeometry(
                pose, type, (p, buffer) -> ResourceManager.projectiles.renderPart(p, buffer, light, -1, part));
        pose.popPose();
    }

    private static void dart(SubmitNodeCollector collector, PoseStack pose, int style, int eID) {

        float red = 1F;
        float green = 1F;
        float blue = 1F;

        switch (style) {
            case BulletConfiguration.BOLT_LASER -> {
                red = 1F;
                green = 0F;
                blue = 0F;
            }
            case BulletConfiguration.BOLT_NIGHTMARE -> {
                red = 1F;
                green = 1F;
                blue = 0F;
            }
            case BulletConfiguration.BOLT_LACUNAE -> {
                red = 0.25F;
                green = 0F;
                blue = 0.75F;
            }
            case BulletConfiguration.BOLT_WORM -> {
                red = 0F;
                green = 1F;
                blue = 0F;
            }
            case BulletConfiguration.BOLT_GLASS_CYAN -> {
                red = 0F;
                green = 1F;
                blue = 1F;
            }
            case BulletConfiguration.BOLT_GLASS_BLUE -> {
                red = 0F;
                green = 0F;
                blue = 1F;
            }
            case BulletConfiguration.BOLT_ZOMG -> {
                Random rand = new Random((long) eID * eID);
                red = rand.nextInt(2) * 0.6F;
                green = rand.nextInt(2) * 0.6F;
                blue = rand.nextInt(2) * 0.6F;
            }
            default -> {}
        }

        final float r = red, g = green, b = blue;
        pose.pushPose();
        pose.scale(1F / 4F, 1F / 8F, 1F / 8F);
        pose.scale(-1, 1, 1);
        pose.scale(2, 2, 2);
        collector.submitCustomGeometry(
                pose,
                BeamRenderTypes.ADDITIVE,
                (p, t) -> {
                    // front
                    tri(t, p, r, g, b, 6, 0, 0, 1, 3, -1, -1, 0, 3, 1, -1, 0);
                    tri(t, p, r, g, b, 3, -1, 1, 0, 6, 0, 0, 1, 3, 1, 1, 0);
                    tri(t, p, r, g, b, 3, -1, -1, 0, 6, 0, 0, 1, 3, -1, 1, 0);
                    tri(t, p, r, g, b, 6, 0, 0, 1, 3, 1, -1, 0, 3, 1, 1, 0);
                    // mid
                    tri(t, p, r, g, b, 6, 0, 0, 1, 4, -0.5F, -0.5F, 1, 4, 0.5F, -0.5F, 1);
                    tri(t, p, r, g, b, 4, -0.5F, 0.5F, 1, 6, 0, 0, 1, 4, 0.5F, 0.5F, 1);
                    tri(t, p, r, g, b, 4, -0.5F, -0.5F, 1, 6, 0, 0, 1, 4, -0.5F, 0.5F, 1);
                    tri(t, p, r, g, b, 6, 0, 0, 1, 4, 0.5F, -0.5F, 1, 4, 0.5F, 0.5F, 1);
                    // tail
                    quad(t, p, r, g, b, 4, 0.5F, -0.5F, 4, 0.5F, 0.5F, 0, 0.5F, 0.5F, 0, 0.5F, -0.5F);
                    quad(t, p, r, g, b, 4, -0.5F, -0.5F, 4, -0.5F, 0.5F, 0, -0.5F, 0.5F, 0, -0.5F, -0.5F);
                    quad(t, p, r, g, b, 4, -0.5F, 0.5F, 4, 0.5F, 0.5F, 0, 0.5F, 0.5F, 0, -0.5F, 0.5F);
                    quad(t, p, r, g, b, 4, -0.5F, -0.5F, 4, 0.5F, -0.5F, 0, 0.5F, -0.5F, 0, -0.5F, -0.5F);
                });
        pose.popPose();
    }

    private static void v(VertexConsumer t, PoseStack.Pose p, float r, float g, float b, float a, float x, float y, float z) {
        t.addVertex(p, x, y, z).setColor(r, g, b, a);
    }

    /** a triangle in the quad buffer: the last corner twice */
    private static void tri(
            VertexConsumer t, PoseStack.Pose p, float r, float g, float b,
            float x0, float y0, float z0, float a0,
            float x1, float y1, float z1, float a1,
            float x2, float y2, float z2, float a2) {
        v(t, p, r, g, b, a0, x0, y0, z0);
        v(t, p, r, g, b, a1, x1, y1, z1);
        v(t, p, r, g, b, a2, x2, y2, z2);
        v(t, p, r, g, b, a2, x2, y2, z2);
    }

    /** main's tail quads: the first two corners opaque, the last two clear */
    private static void quad(
            VertexConsumer t, PoseStack.Pose p, float r, float g, float b,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3) {
        v(t, p, r, g, b, 1, x0, y0, z0);
        v(t, p, r, g, b, 1, x1, y1, z1);
        v(t, p, r, g, b, 0, x2, y2, z2);
        v(t, p, r, g, b, 0, x3, y3, z3);
    }

    public static class State extends EntityRenderState {
        float yaw;
        float pitch;
        int style;
        int trail;
        int id;
    }
}
