// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only
package com.hbm.render.item.weapon.legacy;

import static com.hbm.render.item.weapon.legacy.LegacyResources.*;

import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.items.weapon.legacy.LegacyGuns;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the one-off guns. */
public final class RenderersSpecial {
    private RenderersSpecial() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER_INVERTED, new RevolverInverted());
        LegacyGunRenderer.register(LegacyGuns.GUN_JACK, new Renderers5mm.ItemRenderOverkill());
        LegacyGunRenderer.register(LegacyGuns.GUN_EUTHANASIA, new Renderers5mm.ItemRenderOverkill());
        LegacyGunRenderer.register(LegacyGuns.GUN_TWIGUN, new Renderers5mm.ItemRenderOverkill());
        LegacyGunRenderer.register(LegacyGuns.GUN_DEFABRICATOR, new Renderers5mm.ItemRenderOverkill());
        LegacyGunRenderer.register(LegacyGuns.GUN_DASH, new Renderers5mm.ItemRenderOverkill());
        LegacyGunRenderer.register(LegacyGuns.GUN_HP, new Renderers5mm.ItemRenderOverkill());
        LegacyGunRenderer.register(LegacyGuns.GUN_SPARK, new Renderers5mm.ItemRenderOverkill());
        LegacyGunRenderer.register(LegacyGuns.GUN_FOLLY_LEGACY, new Renderers5mm.ItemRenderOverkill());
        LegacyGunRenderer.register(LegacyGuns.GUN_CHEMTHROWER_LEGACY, new Chemthrower());
        LegacyGunRenderer.register(LegacyGuns.GUN_LEVER_ACTION_SONATA, new Sonata());
    }

    /** main's ItemRenderRevolverInverted: the python, flipped round on the shooter while drawn. */
    public static final class RevolverInverted extends LegacyGunRenderer {
        private static final HFRWavefrontObject M = ff_python;
        private static final int BODY = M.partId("Body"), CYLINDER = M.partId("Cylinder"),
                GRIP = M.partId("Grip"), HAMMER = M.partId("Hammer");

        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            var holder = Minecraft.getInstance().player;
            boolean inUse = holder != null && holder.getUseItem() == stack && stack.getUseAnimation() == UseAnim.BOW;
            switch (ctx) {
                case FIRST_PERSON -> {
                    pose.translate(0.75, 0.5, 0);
                    pose.scale(1.5F, 1.5F, 1.5F);
                    rot(pose, 100, 0, -1, 0);
                    rot(pose, 25, 1, 0, 0);
                    if (inUse) {
                        pose.translate(-0.4, 0, 0);
                        rot(pose, 198, 0, 1, 0);
                        rot(pose, -10, 1, 0, 0);
                    }
                }
                case EQUIPPED -> {
                    rot(pose, 195, 0, 1, 0);
                    rot(pose, -10, 0, 0, 1);
                    rot(pose, -10, 1, 0, 0);
                    pose.translate(-0.5, 0.1, -0.25);
                    pose.scale(1.5F, 1.5F, 1.5F);
                    if (inUse) {
                        pose.translate(-0.05, 0, 0);
                        rot(pose, -15, 0, 0, 1);
                        rot(pose, 160, 0, 1, 0);
                    }
                }
                case ENTITY -> rot(pose, 90, 0, 1, 0);
                default -> {}
            }
            part(c, pose, M, ff_gun_bright, BODY, light);
            part(c, pose, M, ff_gun_bright, CYLINDER, light);
            part(c, pose, M, ff_wood, GRIP, light);
            part(c, pose, M, ff_iron, HAMMER, light);
        }
    }

    /** main's ItemRenderGunAnim for the sonata: the flipped ModelLeverAction, the lever swinging on its pin. */
    public static final class Sonata extends LegacyGunRenderer {
        private static final HFRWavefrontObject M = com.hbm.client.model.Meshes.faceNormals(com.hbm.lib.Library.id("models/weapons/legacy/lever_action_sonata.obj"));
        private static final net.minecraft.resources.ResourceLocation T = com.hbm.lib.Library.id("textures/models/model_lever_action.png");
        private static final String[] BODY = {"Barrel1","Barrel2","Barrel3","Barrel4","Grip","Front1","Front2","BodyFront","BodyTop","BodyMain","BodyPlate","HandleMain","HandleBottom","HandleBack","Trigger"};
        private static final java.util.Set<String> LEVER = java.util.Set.of("LeverFront", "LeverBottom", "LeverMid", "LeverFrontPlate", "LeverBackBottom", "LeverBackTop", "LeverBack");

        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            float anim = com.hbm.items.weapon.legacy.ItemGunSpecial.Sonata.getRotationFromAnim(stack);
            float off = com.hbm.items.weapon.legacy.ItemGunSpecial.Sonata.getOffsetFromAnim(stack);
            if (ctx == Ctx.FIRST_PERSON) {
                rot(pose, -135.0F, 0, 0, 1);
                pose.translate(-0.5F, 0.0F, -0.2F);
                pose.scale(0.25F, 0.25F, 0.25F);
                pose.translate(-0.2F, -0.1F, -0.1F);
                rot(pose, 180F, 0, 1, 0);
                rot(pose, 15F, 0, 0, 1);
                pose.translate(2.3F, 0.2F, 0.8F);
                if (anim > 0) {
                    rot(pose, anim * -25, 0, 0, 1);
                    pose.translate(off * 1.5F, off * -1.5F, 0.0F);
                }
            } else if (ctx != Ctx.INVENTORY) {
                rot(pose, -200.0F, 0, 0, 1);
                rot(pose, 75.0F, 0, 1, 0);
                rot(pose, -30.0F, 1, 0, 0);
                pose.translate(0.0F, -0.2F, -0.5F);
                rot(pose, -5.0F, 0, 0, 1);
                pose.scale(0.5F, 0.5F, 0.5F);
                pose.translate(-0.3F, -0.4F, 0.15F);
                rot(pose, 180F, 0, 1, 0);
                rot(pose, 25F, 0, 0, 1);
                pose.translate(2.3F, 0.2F, 0.8F);
            }
            for (String g : BODY) {
                part(c, pose, M, T, M.partId(g), light);
            }
            pose.pushPose();
            pose.translate(7 / 16F, 9 / 16F, -1 / 16F);
            pose.mulPose(com.mojang.math.Axis.ZP.rotation(anim));
            pose.translate(-7 / 16F, -9 / 16F, 1 / 16F);
            for (String g : LEVER) part(c, pose, M, T, M.partId(g), light);
            pose.popPose();
        }
    }

    /** main's ItemRenderWeaponChemthrower: the gauge needle follows the fill. */
    public static final class Chemthrower extends LegacyGunRenderer {
        private static final HFRWavefrontObject M = com.hbm.main.ResourceManager.chemthrower;
        private static final net.minecraft.resources.ResourceLocation T = com.hbm.main.ResourceManager.chemthrower_tex;
        private static final int GUN = M.partId("Gun"), HOSE = M.partId("Hose"), NOZZLE = M.partId("Nozzle"), GAUGE = M.partId("Gauge");

        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            switch (ctx) {
                case FIRST_PERSON -> {
                    rot(pose, 25, 0, 0, 1);
                    pose.translate(1.0, 0.0, 0.0);
                    rot(pose, 170, 0, 1, 0);
                    pose.scale(0.25F, 0.25F, 0.25F);
                }
                case EQUIPPED -> {
                    pose.scale(0.25F, 0.25F, 0.25F);
                    rot(pose, 100, 0, 1, 0);
                    rot(pose, -10, 1, 0, 0);
                    rot(pose, 10, 0, 0, 1);
                    pose.translate(-0.25F, -2.5F, 1.75F);
                }
                case ENTITY -> pose.scale(0.25F, 0.25F, 0.25F);
                case INVENTORY -> {
                    pose.translate(9, 9, 0);
                    rot(pose, 180, 1, 0, 0);
                    rot(pose, 45, 0, 0, -1);
                    pose.scale(1.5F, 1.5F, -1.5F);
                }
            }
            part(c, pose, M, T, GUN, light);
            part(c, pose, M, T, HOSE, light);
            part(c, pose, M, T, NOZZLE, light);
            var gun = (com.hbm.items.weapon.legacy.ItemGunBase) stack.getItem();
            double d = (double) com.hbm.items.weapon.legacy.ItemGunBase.getMag(stack) / gun.mainConfig.ammoCap;
            pose.translate(0, 0.875, 1.75);
            rot(pose, 135 - d * 270, 1, 0, 0);
            pose.translate(0, -0.875, -1.75);
            part(c, pose, M, T, GAUGE, light);
        }
    }
}
