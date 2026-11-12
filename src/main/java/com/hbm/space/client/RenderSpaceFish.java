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
import com.hbm.space.SpaceSeaMobs;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Squid;
import org.joml.Quaternionf;

/** Laythe's sea life: NTM:Space's OBJ fish, drawn as its models drew them, with a skin per variant. */
public class RenderSpaceFish<E extends LivingEntity> extends EntityRenderer<E, RenderSpaceFish.State>
        implements ConcurrentRenderStateExtraction {

    /** Which upstream model draws the mob. */
    public enum Kind { SCUTTER, SCRAP_FISH, DEPTH_SQUID, SCUTTLECRAB, SIFTER_EEL }

    private final Kind kind;
    private final ResourceLocation[] textures;
    private final ResourceLocation meshId;
    private HFRWavefrontObject mesh;

    public RenderSpaceFish(EntityRendererProvider.Context context, Kind kind, String mesh, float shadow, String texturePrefix, String... variants) {
        super(context);
        this.kind = kind;
        this.shadowRadius = shadow;
        this.meshId = Library.id("models/mobs/" + mesh + ".obj");
        if (variants.length == 0) textures = new ResourceLocation[] {Library.id("textures/entity/" + texturePrefix + ".png")};
        else {
            textures = new ResourceLocation[variants.length];
            for (int i = 0; i < variants.length; i++) textures[i] = Library.id("textures/entity/" + texturePrefix + "_" + variants[i] + ".png");
        }
    }

    public static final class State extends EntityRenderState {
        float limbSwing, limbSwingAmount, headYaw, pitch, bodyYaw, death;
        int variant;
        boolean hurt;
        final Extra extra = new Extra();
    }

    /** What only the squid needs: its tentacles and its body pitch. */
    static final class Extra {
        float tentacleAngle, squidPitch;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(E entity, State s, float pt) {
        super.extractRenderState(entity, s, pt);
        s.limbSwing = entity.walkAnimation.position(pt);
        s.limbSwingAmount = entity.walkAnimation.speed(pt);
        s.bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        s.headYaw = Mth.rotLerp(pt, entity.yHeadRotO, entity.yHeadRot) - s.bodyYaw;
        s.pitch = Mth.lerp(pt, entity.xRotO, entity.getXRot());
        s.variant = entity instanceof SpaceSeaMobs.Variant v ? v.variant() : 0;
        s.hurt = entity.hurtTime > 0 || entity.deathTime > 0;
        s.death = entity.deathTime > 0 ? Math.min(1F, Mth.sqrt((entity.deathTime + pt - 1F) / 20F * 1.6F)) : 0F;
        if (entity instanceof Squid squid) {
            s.extra.tentacleAngle = Mth.lerp(pt, squid.oldTentacleAngle, squid.tentacleAngle);
            s.extra.squidPitch = Mth.lerp(pt, squid.xBodyRotO, squid.xBodyRot);
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (mesh == null) mesh = Meshes.load(meshId);
        HFRWavefrontObject m = mesh;
        int light = s.lightCoords;
        int color = s.hurt ? 0xFFFF9999 : -1;
        var type = RenderTypes.entityCutoutNoCull(textures[Math.min(s.variant, textures.length - 1)]);
        Consumer<String> draw = name -> {
            int part = m.partId(name);
            col.submitCustomGeometry(ps, type, (pose, buf) -> m.renderPart(pose, buf, light, color, part));
        };
        ps.pushPose();
        // RenderLiving: turn to the body's yaw, topple when dead, then flip into model space
        rot(ps, 180F - s.bodyYaw, 0, 1, 0);
        if (s.death > 0) rot(ps, s.death * 90F, 0, 0, 1);
        ps.scale(-1F, -1F, 1F);
        ps.translate(0F, -1.5078125F, 0F);
        switch (kind) {
            case SCUTTER -> scutter(ps, draw, s.limbSwing, s.limbSwingAmount, s.headYaw, s.pitch, s.extra);
            case SCRAP_FISH -> scrapFish(ps, draw, s.limbSwing, s.limbSwingAmount, s.headYaw, s.pitch, s.extra);
            case DEPTH_SQUID -> depthSquid(ps, draw, s.limbSwing, s.limbSwingAmount, s.headYaw, s.pitch, s.extra);
            case SCUTTLECRAB -> scuttlecrab(ps, draw, s.limbSwing, s.limbSwingAmount, s.headYaw, s.pitch, s.extra);
            case SIFTER_EEL -> sifterEel(ps, draw, s.limbSwing, s.limbSwingAmount, s.headYaw, s.pitch, s.extra);
        }
        ps.popPose();
        super.submit(s, ps, col, camera);
    }

    static void rot(PoseStack ps, double degrees, double x, double y, double z) {
        ps.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(degrees), (float) x, (float) y, (float) z));
    }

    // the models, as NTM:Space's ModelScutter, ModelScrapFish, ModelDepthSquid, ModelScuttlecrab and ModelSifterEel drew them

    static void scutter(PoseStack ps, Consumer<String> draw, float limbSwing, float limbSwingAmount, float rotationHeadYaw, float rotationPitch, Extra e) {


        ps.pushPose();
        {

            double cy0 = Math.sin(limbSwing % (Math.PI * 2));
            double cy1 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.2);
            double cy2 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.4);
            double cy3 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.6);

            rot(ps, 180.0F, 0, 0, 1);
            ps.translate(0, -1.5F, 0);

            draw.accept("body");

            // Head
            ps.pushPose();
            {
                rot(ps, rotationPitch, 1, 0, 0);
                rot(ps, rotationHeadYaw, 0, 1, 0);
                draw.accept("head");
            }
            ps.popPose();

            // Side fins
            ps.pushPose();
            {
                rot(ps, cy0 * 20, 0, 0, 1);
                draw.accept("leftfin1");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy0 * -20, 0, 0, 1);
                draw.accept("rightfin1");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy1 * 20, 0, 0, 1);
                draw.accept("leftfin2");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy1 * -20, 0, 0, 1);
                draw.accept("rightfin2");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy2 * 20, 0, 0, 1);
                draw.accept("leftfin3");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy2 * -20, 0, 0, 1);
                draw.accept("rightfin3");
            }
            ps.popPose();

            // Tail fin
            ps.pushPose();
            {
                rot(ps, cy1 * 10, 0, 0, 1);
                rot(ps, cy2 * -5, 1, 0, 0);
                draw.accept("tailmeat");
                rot(ps, cy3 * -5, 1, 0, 0);
                draw.accept("tailthin");
            }
            ps.popPose();

        }
        ps.popPose();
    }

    static void scrapFish(PoseStack ps, Consumer<String> draw, float limbSwing, float limbSwingAmount, float rotationHeadYaw, float rotationPitch, Extra e) {


        ps.pushPose();
        {

            double cy0 = Math.sin(limbSwing % (Math.PI * 2));
            double cy1 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.2);
            double cy2 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.4);
            double cy3 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.6);
            double cy4 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.8);
            double cy5 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 1.0);

            rot(ps, 180.0F, 0, 0, 1);
            ps.translate(0, -1.5F, 0);

            draw.accept("Body");

            // Head
            ps.pushPose();
            {
                ps.translate(0, 0, -0.3125F);
                rot(ps, rotationPitch, 1, 0, 0);
                rot(ps, rotationHeadYaw, 0, 1, 0);
                ps.translate(0, 0, 0.3125F);
                draw.accept("Head");

            }
            ps.popPose();

            // Side fins
            ps.pushPose();
            {
                rot(ps, cy0 * 20, 1, 0, 0);
                draw.accept("FinL");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy0 * -20, 1, 0, 0);
                draw.accept("FinR");
            }
            ps.popPose();

            //Ventral fins
            ps.pushPose();
            {
                rot(ps, cy1 * 20, 1, 0, 0);
                draw.accept("VentralFL");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy1 * -20, 1, 0, 0);
                draw.accept("VentralFR");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy2 * 20, 1, 0, 0);
                draw.accept("VentralBL");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy2 * -20, 1, 0, 0);
                draw.accept("VentralBR");
            }
            ps.popPose();

            // Tail fin
            ps.pushPose();
            {
                rot(ps, cy3 * 10, 0, 1, 0);
                draw.accept("Dorsal");

                rot(ps, cy4 * 6, 0, 1, 0);
                draw.accept("Tail");

                rot(ps, cy5 * 4, 0, 1, 0);
                draw.accept("TailFin");
            }
            ps.popPose();

        }
        ps.popPose();
    }

    static void depthSquid(PoseStack ps, Consumer<String> draw, float limbSwing, float limbSwingAmount, float rotationHeadYaw, float rotationPitch, Extra e) {


        float tentacleAngle = e.tentacleAngle;
        float squidPitch = e.squidPitch;

        ps.pushPose();
        {
            double cy0 = Math.sin(limbSwing % (Math.PI * 2));
            double cy1 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.4);

            ps.translate(0, 1.0F, 0);
            rot(ps, squidPitch, 1, 0, 0);

            draw.accept("Body");

            // Side fins
            ps.pushPose();
            {
                rot(ps, cy1 * 20, 1, 0, 0);
                rot(ps, cy0 * 10, 0, 1, 0);
                draw.accept("FinL");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy1 * 20, 1, 0, 0);
                rot(ps, cy0 * -10, 0, 1, 0);
                draw.accept("FinR");
            }
            ps.popPose();

            // dibblies
            for(int i = 0; i < 8; i++) {
                ps.pushPose();
                {
                    ps.translate(0, -0.9, 0);
                    rot(ps, (float)i * -45.0F, 0, 1, 0);
                    rot(ps, tentacleAngle * (180F / (float)Math.PI), 1, 0, 0);
                    ps.translate(0, 0.9, 0);
                    draw.accept("T0");
                }
                ps.popPose();
            }

        }
        ps.popPose();
    }

    static void scuttlecrab(PoseStack ps, Consumer<String> draw, float limbSwing, float limbSwingAmount, float rotationHeadYaw, float rotationPitch, Extra e) {


        ps.pushPose();
        {

            double cy0 = Math.sin(limbSwing * 6 % (Math.PI * 2));
            double cy1 = Math.sin(limbSwing * 6 % (Math.PI * 2) - Math.PI * 0.4);
            double cy2 = Math.sin(limbSwing * 6 % (Math.PI * 2) - Math.PI * 0.8);
            double cy3 = Math.sin(limbSwing * 6 % (Math.PI * 2) - Math.PI * 1.2);
            double cy4 = Math.sin(limbSwing * 6 % (Math.PI * 2) - Math.PI * 1.6);

            rot(ps, 180.0F, 0, 0, 1);
            ps.translate(0, -1.5F, 0);

            draw.accept("Body");

            ps.pushPose();
            {
                ps.translate(-0.125F, 0.5625F, -0.3125F);
                rot(ps, rotationPitch, 1, 0, 0);
                rot(ps, rotationHeadYaw, 0, 1, 0);
                ps.translate(0.125F, -0.5625F, 0.3125F);
                draw.accept("EyeL");
            }
            ps.popPose();
            ps.pushPose();
            {
                ps.translate(0.125F, 0.5625F, -0.3125F);
                rot(ps, rotationPitch, 1, 0, 0);
                rot(ps, rotationHeadYaw, 0, 1, 0);
                ps.translate(-0.125F, -0.5625F, 0.3125F);
                draw.accept("EyeR");
            }
            ps.popPose();


            draw.accept("ClawL");
            draw.accept("ClawR");


            ps.pushPose();
            {
                rot(ps, cy0 * 8, 0, 0, 1);
                draw.accept("LegL1");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy1 * 8, 0, 0, 1);
                draw.accept("LegL2");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy2 * 8, 0, 0, 1);
                draw.accept("LegL3");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy3 * 8, 0, 0, 1);
                draw.accept("LegL4");
            }
            ps.popPose();


            ps.pushPose();
            {
                rot(ps, cy1 * -8, 0, 0, 1);
                draw.accept("LegR1");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy2 * -8, 0, 0, 1);
                draw.accept("LegR2");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy3 * -8, 0, 0, 1);
                draw.accept("LegR3");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy4 * -8, 0, 0, 1);
                draw.accept("LegR4");
            }
            ps.popPose();

        }
        ps.popPose();
    }

    static void sifterEel(PoseStack ps, Consumer<String> draw, float limbSwing, float limbSwingAmount, float rotationHeadYaw, float rotationPitch, Extra e) {


        ps.pushPose();
        {

            double cy0 = Math.sin(limbSwing % (Math.PI * 2));
            double cy1 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.2);
            double cy2 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.4);
            double cy3 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.6);
            double cy4 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.9);
            double cy5 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 1.2);
            double cy6 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 1.5);
            double cy7 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 1.9);

            rot(ps, 180.0F, 0, 0, 1);
            ps.translate(0, -1.5F, 0);

            // Head
            ps.pushPose();
            {
                ps.translate(0, 0, -0.5F);
                rot(ps, rotationPitch, 1, 0, 0);
                rot(ps, rotationHeadYaw, 0, 1, 0);
                ps.translate(0, 0, 0.5F);
                draw.accept("Head");
                draw.accept("Jaw");

            }
            ps.popPose();

            // Side fins
            ps.pushPose();
            {
                rot(ps, cy0 * 20, 1, 0, 0);
                draw.accept("FinL");
            }
            ps.popPose();
            ps.pushPose();
            {
                rot(ps, cy0 * -20, 1, 0, 0);
                draw.accept("FinR");
            }
            ps.popPose();

            // Tail fin
            ps.pushPose();
            {
                draw.accept("Body1");

                rot(ps, cy1 * 10, 0, 1, 0);
                rot(ps, cy2 * -2, 0, 1, 0);
                draw.accept("Dorsal1");


                draw.accept("Body2");
                rot(ps, cy3 * -2, 0, 1, 0);
                draw.accept("Dorsal2");
                draw.accept("Dorsal3");
                rot(ps, cy4 * -6, 0, 1, 0);

                draw.accept("Body3");

                draw.accept("Ventral1");
                draw.accept("Ventral2");
                draw.accept("Ventral3");
                rot(ps, cy5 * -4, 0, 1, 0);
                draw.accept("Dorsal4");
                draw.accept("Ventral4");
                draw.accept("Body4");
                rot(ps, cy6 * -6, 0, 1, 0);
                draw.accept("Body5");

                draw.accept("Body6");

                rot(ps, cy7 * -5, 0, 1, 0);
                draw.accept("Tail");

            }

            ps.popPose();

        }
        ps.popPose();
    }
}
