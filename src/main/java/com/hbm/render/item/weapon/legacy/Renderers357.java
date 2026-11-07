// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.legacy;

import static com.hbm.render.item.weapon.legacy.LegacyResources.*;

import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.items.weapon.legacy.ItemGunBase;
import com.hbm.items.weapon.legacy.ItemGunBio;
import com.hbm.items.weapon.legacy.LegacyGuns;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's .357 renderers: ItemRenderWeaponFFColt, FFCursed,
 * FFNightmare, FFNightmareDark and ItemRenderBioRevolver.
 */
public final class Renderers357 {

    private Renderers357() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER, new FFColt(ff_gun_bright, ff_iron, ff_wood));
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER_SATURNITE, new FFColt(ff_saturnite, ff_iron, ff_wood));
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER_GOLD, new FFColt(ff_gold, ff_gold, ff_gun_dark));
        LegacyGunRenderer.register(
                LegacyGuns.GUN_REVOLVER_SCHRABIDIUM, new FFColt(ff_schrabidium, ff_schrabidium, ff_gun_dark));
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER_CURSED, new FFCursed());
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER_NIGHTMARE, new FFNightmare());
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER_NIGHTMARE2, new FFNightmareDark());
        LegacyGunRenderer.register(LegacyGuns.GUN_BIO_REVOLVER, new BioRevolver());
    }

    /** main's FF revolver pose for first person and third person, shared by the colt and cursed */
    private static void ffHeld(LegacyGunRenderer.Ctx ctx, PoseStack pose, double fpRot) {
        if (ctx == LegacyGunRenderer.Ctx.FIRST_PERSON) {
            pose.translate(0.75, 0.5, 0);
            LegacyGunRenderer.scale(pose, 1.5);
            LegacyGunRenderer.rot(pose, 100, 0, -1, 0);
            LegacyGunRenderer.rot(pose, fpRot, 1, 0, 0);
        }
    }

    private static void ffRest(LegacyGunRenderer.Ctx ctx, PoseStack pose, double invX, double invY, double s) {
        switch (ctx) {
            case EQUIPPED -> {
                LegacyGunRenderer.rot(pose, 195, 0, 1, 0);
                LegacyGunRenderer.rot(pose, -10, 0, 0, 1);
                LegacyGunRenderer.rot(pose, -10, 1, 0, 0);
                pose.translate(-0.5, 0.1, -0.25);
                LegacyGunRenderer.scale(pose, 1.5);
            }
            case ENTITY -> LegacyGunRenderer.rot(pose, 90, 0, 1, 0);
            case INVENTORY -> {
                pose.translate(invX, invY, 0);
                LegacyGunRenderer.rot(pose, -135, 0, 0, 1);
                LegacyGunRenderer.rot(pose, 90, 0, 1, 0);
                pose.scale((float) s, (float) s, (float) -s);
            }
            default -> {}
        }
    }

    public static final class FFColt extends LegacyGunRenderer {
        private static final HFRWavefrontObject M = ff_python;
        private static final int BODY = M.partId("Body"), CYLINDER = M.partId("Cylinder"),
                GRIP = M.partId("Grip"), HAMMER = M.partId("Hammer");
        private final ResourceLocation main, hammer, grip;

        public FFColt(ResourceLocation main, ResourceLocation hammer, ResourceLocation grip) {
            this.main = main;
            this.hammer = hammer;
            this.grip = grip;
        }

        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            if (ctx == Ctx.FIRST_PERSON) {
                ffHeld(ctx, pose, 25);
                HbmAnimations.applyRelevantTransformation(pose, "Body");
                part(c, pose, M, main, BODY, light);
                pose.pushPose();
                HbmAnimations.applyRelevantTransformation(pose, "Cylinder");
                part(c, pose, M, main, CYLINDER, light);
                pose.popPose();
                part(c, pose, M, grip, GRIP, light);
                HbmAnimations.applyRelevantTransformation(pose, "Hammer");
                part(c, pose, M, hammer, HAMMER, light);
                return;
            }
            ffRest(ctx, pose, 8, 8, 17);
            part(c, pose, M, main, BODY, light);
            part(c, pose, M, main, CYLINDER, light);
            part(c, pose, M, grip, GRIP, light);
            part(c, pose, M, hammer, HAMMER, light);
        }
    }

    public static final class FFCursed extends LegacyGunRenderer {
        private static final HFRWavefrontObject M = cursed_revolver;
        private static final int BODY = M.partId("Body"), BARREL = M.partId("Barrel"), GRIP = M.partId("Grip"),
                SLIDE = M.partId("Slide"), MAGAZINE = M.partId("Magazine");

        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            if (ctx == Ctx.FIRST_PERSON) {
                ffHeld(ctx, pose, 20);
                HbmAnimations.applyRelevantTransformation(pose, "Body");
                part(c, pose, M, ff_cursed, BODY, light);
                part(c, pose, M, ff_cursed, BARREL, light);
                part(c, pose, M, ff_cursed, GRIP, light);
                pose.pushPose();
                HbmAnimations.applyRelevantTransformation(pose, "Slide");
                part(c, pose, M, ff_cursed, SLIDE, light);
                pose.popPose();
                pose.pushPose();
                HbmAnimations.applyRelevantTransformation(pose, "Magazine");
                part(c, pose, M, ff_cursed, MAGAZINE, light);
                pose.popPose();
                return;
            }
            ffRest(ctx, pose, 10, 11.5, 17);
            all(c, pose, M, ff_cursed, light);
        }
    }

    public static final class FFNightmare extends LegacyGunRenderer {
        private static final HFRWavefrontObject M = ff_nightmare;
        private static final int GUN = M.partId("Gun");
        private static final int[] BULLETS =
                M.partIds("Bullet1", "Bullet2", "Bullet3", "Bullet4", "Bullet5", "Bullet6");

        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            switch (ctx) {
                case FIRST_PERSON -> {
                    pose.translate(0.75, 0.55, 0);
                    scale(pose, 0.2);
                    rot(pose, 100, 0, -1, 0);
                    rot(pose, 25, 1, 0, 0);
                }
                case EQUIPPED -> {
                    rot(pose, 195, 0, 1, 0);
                    rot(pose, -10, 0, 0, 1);
                    rot(pose, -10, 1, 0, 0);
                    pose.translate(-0.5, 0.3, -0.25);
                    scale(pose, 0.2);
                }
                case ENTITY -> {
                    scale(pose, 0.15);
                    rot(pose, 90, 0, 1, 0);
                }
                case INVENTORY -> {
                    pose.translate(10, 9, 0);
                    rot(pose, -135, 0, 0, 1);
                    rot(pose, 90, 0, 1, 0);
                    pose.scale(1.75F, 1.75F, -1.75F);
                }
            }
            part(c, pose, M, ff_nightmare_orig_tex, GUN, light);
            int ammo = Math.min(ItemGunBase.getMag(stack), BULLETS.length);
            for (int i = 0; i < ammo; i++) part(c, pose, M, ff_nightmare_orig_tex, BULLETS[i], light);
        }
    }

    public static final class FFNightmareDark extends LegacyGunRenderer {
        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            switch (ctx) {
                case FIRST_PERSON -> {
                    pose.translate(0.75, 0.45, 0);
                    scale(pose, 0.4);
                    rot(pose, 170, 0, 1, 0);
                    rot(pose, 25, 0, 0, -1);
                }
                case EQUIPPED -> {
                    rot(pose, 105, 0, 1, 0);
                    rot(pose, -10, 1, 0, 0);
                    rot(pose, 10, 0, 0, 1);
                    pose.translate(-0.125, 0.1, 0.5);
                    scale(pose, 0.4);
                }
                case ENTITY -> {
                    scale(pose, 0.25);
                    rot(pose, 90, 0, 1, 0);
                }
                case INVENTORY -> {
                    pose.translate(10, 9, 0);
                    rot(pose, -135, 0, 0, 1);
                    rot(pose, 180, 0, 1, 0);
                    pose.scale(3F, 3F, -3F);
                }
            }
            all(c, pose, nightmare_dark, ff_nightmare_tex, light);
        }
    }

    public static final class BioRevolver extends LegacyGunRenderer {
        private static final HFRWavefrontObject M = ResourceManager.bio_revolver;
        private static final int GRIP = M.partId("Grip"), BARREL = M.partId("Barrel"), LATCH = M.partId("Latch"),
                DRUM = M.partId("Drum"), HAMMER = M.partId("Hammer");
        /** main's smoke nodes are (x, y, z) with the width on x; SmokeNode draws (forward, lift, side) */
        private static final Matrix4f SWAP_XZ = new Matrix4f(0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1);

        @Override
        protected boolean cull() {
            return false;
        }

        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            var tex = ResourceManager.bio_revolver_tex;
            switch (ctx) {
                case FIRST_PERSON -> {
                    rot(pose, 25, 0, 0, 1);
                    pose.translate(1.0, 0.25, -0.25);
                    rot(pose, 80, 0, 1, 0);
                    scale(pose, 0.1);

                    double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
                    pose.pushPose();
                    pose.translate(0.0, 1.75, 10.0);
                    pose.translate(0, -recoil[2] * 1.5, recoil[2]);
                    pose.last().pose().mul(SWAP_XZ);
                    pose.last().normal().mul(new Matrix3f(SWAP_XZ));
                    ItemRenderWeaponBase.renderSmokeNodes(c, pose, ItemGunBio.smokeNodes, 0.5D, light);
                    pose.popPose();

                    pose.translate(recoil[0], recoil[1], recoil[2]);
                    rot(pose, recoil[2] * 10, 1, 0, 0);
                    double[] reloadMove = HbmAnimations.getRelevantTransformation("RELOAD_MOVE");
                    pose.translate(reloadMove[0], reloadMove[1], reloadMove[2]);
                    double[] reloadRot = HbmAnimations.getRelevantTransformation("RELOAD_ROT");
                    rot(pose, reloadRot[0], 1, 0, 0);
                    rot(pose, reloadRot[2], 0, 0, 1);
                    rot(pose, reloadRot[1], 0, 1, 0);
                    part(c, pose, M, tex, GRIP, light);

                    pose.pushPose();
                    rot(pose, HbmAnimations.getRelevantTransformation("FRONT")[2], 1, 0, 0);
                    part(c, pose, M, tex, BARREL, light);
                    pose.pushPose();
                    pose.translate(0, 2.3125, -0.875);
                    rot(pose, HbmAnimations.getRelevantTransformation("LATCH")[2], 1, 0, 0);
                    pose.translate(0, -2.3125, 0.875);
                    part(c, pose, M, tex, LATCH, light);
                    pose.popPose();
                    pose.pushPose();
                    pose.translate(0, 1, 0);
                    rot(pose, HbmAnimations.getRelevantTransformation("DRUM")[2] * 60, 0, 0, 1);
                    pose.translate(0, -1, 0);
                    pose.translate(0, 0, HbmAnimations.getRelevantTransformation("DRUM_PUSH")[2]);
                    part(c, pose, M, tex, DRUM, light);
                    pose.popPose();
                    pose.popPose();

                    pose.pushPose();
                    pose.translate(0, 0, -4.5);
                    rot(pose, -45 + 45 * HbmAnimations.getRelevantTransformation("HAMMER")[2], 1, 0, 0);
                    pose.translate(0, 0, 4.5);
                    part(c, pose, M, tex, HAMMER, light);
                    pose.popPose();
                    return;
                }
                case EQUIPPED -> {
                    scale(pose, 0.1);
                    rot(pose, 15, 0, 0, 1);
                    rot(pose, 10, 0, 1, 0);
                    rot(pose, 15, 1, 0, 0);
                    pose.translate(4, 1, 4);
                }
                case ENTITY -> {
                    scale(pose, 0.075);
                    rot(pose, 90, 0, 1, 0);
                    pose.translate(0, 1, 0);
                }
                case INVENTORY -> {
                    pose.translate(8, 7, 0);
                    rot(pose, -90, 0, 1, 0);
                    rot(pose, -135, 1, 0, 0);
                    pose.scale(0.8F, 0.8F, -0.8F);
                }
            }
            all(c, pose, M, tex, light);
        }
    }
}
