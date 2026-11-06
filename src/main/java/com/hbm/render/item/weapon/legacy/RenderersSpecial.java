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
}
