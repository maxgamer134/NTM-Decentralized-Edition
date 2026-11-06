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
        LegacyGunRenderer.register(LegacyGuns.GUN_CHEMTHROWER_LEGACY, new Chemthrower());
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
