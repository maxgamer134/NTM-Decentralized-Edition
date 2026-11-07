// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.legacy;

import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.items.weapon.legacy.BulletConfigSyncingUtil;
import com.hbm.items.weapon.legacy.BulletConfiguration;
import com.hbm.items.weapon.legacy.ItemGunBase;
import com.hbm.items.weapon.legacy.LegacyGuns;
import com.hbm.lib.Library;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the 50AE family. */
public final class Renderers50AE {

    private Renderers50AE() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_DEAGLE, new ItemRenderWeaponObj());
    }

    public static final class ItemRenderWeaponObj extends LegacyGunRenderer {

        private static final String[] FLECHETTE_PARTS = {"body", "chamber", "barrel", "gren_tube", "grenades", "pivot", "top", "drum", "base", "trigger", "stock"};
        private static final ResourceLocation[] FLECHETTE_TEX = {LegacyResources.flechette_body, LegacyResources.flechette_chamber, LegacyResources.flechette_barrel, LegacyResources.flechette_gren_tube, LegacyResources.flechette_grenades, LegacyResources.flechette_pivot, LegacyResources.flechette_top, LegacyResources.flechette_drum, LegacyResources.flechette_base, LegacyResources.flechette_trigger, LegacyResources.flechette_stock};
        private static final int[] FLECHETTE_IDS = LegacyResources.flechette.partIds(FLECHETTE_PARTS);

        /** main's renderFlechette: every part of the flechette rifle under its own texture */
        private void renderFlechette(SubmitNodeCollector c, PoseStack pose, ResourceLocation tex, int light) {
            for (int i = 0; i < FLECHETTE_IDS.length; i++)
                part(c, pose, LegacyResources.flechette, FLECHETTE_TEX[i], FLECHETTE_IDS[i], light);
        }
        private static final int QUADRO_LAUNCHER = LegacyResources.quadro.partId("Launcher");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();

		Player player = Minecraft.getInstance().player;

		if(is(stack, "gun_hk69"))
			tex = LegacyResources.hk69_tex;

		if(stack.getItem() == LegacyGuns.GUN_DEAGLE.get())
			tex = LegacyResources.universal_bright;

		if(is(stack, "gun_flamer_legacy"))
			tex = LegacyResources.flamer_tex;

		if(is(stack, "gun_quadro_legacy"))
			tex = LegacyResources.quadro_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			

			if(is(stack, "gun_hk69")) {
				pose.translate(1.0F, 0.5F, 0.0F);
				rot(pose, 90F, 0.0F, 1.0F, 0.0F);
				rot(pose, -25F, 1.0F, 0.0F, 0.0F);
				rot(pose, -10F, 0.0F, 1.0F, 0.0F);
				pose.scale((float) (0.75), (float) (0.75), (float) (0.75));
				
				if(com.hbm.items.weapon.legacy.ItemGunBase.isAiming(player)) {
					pose.translate(1.16F, 0.35F, -0.8F);
					rot(pose, 5.5F, 0.0F, 1.0F, 0.0F);
					rot(pose, 4F, 1.0F, 0.0F, 0.0F);
				}
			}

			if(stack.getItem() == LegacyGuns.GUN_DEAGLE.get()) {
				rot(pose, 90F, 0.0F, 1.0F, 0.0F);
				rot(pose, 25F, -1.0F, 0.0F, 0.0F);
				pose.translate(0.2F, 0.2F, 0.8F);
				pose.scale((float) (0.2), (float) (0.2), (float) (0.2));
				
				if(com.hbm.items.weapon.legacy.ItemGunBase.isAiming(player)) {
					pose.translate(3.7F, 1.7F, 0F);
					rot(pose, -5F, 0.0F, 1.0F, 0.0F);
				}
			}

			if(is(stack, "gun_flamer_legacy")) {
				pose.translate(1.0F, 0.0F, -0.15F);
				rot(pose, 90F, 0.0F, 1.0F, 0.0F);
				rot(pose, -25F, 1.0F, 0.0F, 0.0F);
				rot(pose, -10F, 0.0F, 1.0F, 0.0F);
				pose.scale((float) (0.5), (float) (0.5), (float) (0.5));
				
				if(com.hbm.items.weapon.legacy.ItemGunBase.isAiming(player)) {
					pose.translate(0.75F, 0.2F, 0.3F);
				}
			}

			if(is(stack, "gun_flechette")) {
				
				rot(pose, 25F, 0.0F, 0.0F, 1.0F);
				pose.translate(1.0F, -1.0F, -0.0F);
				rot(pose, 170F, 0.0F, 1.0F, 0.0F);
				pose.scale((float) (0.25), (float) (0.25), (float) (0.25));
				
				if(com.hbm.items.weapon.legacy.ItemGunBase.isAiming(player)) {
					rot(pose, 5F, 0.0F, 1.0F, 0.0F);
					rot(pose, -2F, 0.0F, 0.0F, 1.0F);
					pose.translate(-2.0F, 1.2F, 3.7F);
				}

				double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
				pose.translate(recoil[0], recoil[1], recoil[2]);
			}

			if(is(stack, "gun_quadro_legacy")) {
				pose.translate(0.75F, 0.0F, -0.15F);
				rot(pose, 90F, 0.0F, 1.0F, 0.0F);
				rot(pose, -25F, 1.0F, 0.0F, 0.0F);
				rot(pose, -10F, 0.0F, 1.0F, 0.0F);
				pose.scale((float) (0.5), (float) (0.5), (float) (0.5));
				
				if(com.hbm.items.weapon.legacy.ItemGunBase.isAiming(player)) {
					rot(pose, 5F, 0.0F, 1.0F, 0.0F);
					pose.translate(1.0F, 0.5F, 0.3F);
				}

				double[] recoil = HbmAnimations.getRelevantTransformation("QUADRO_RECOIL");
				pose.translate(0, 0, recoil[2]);

				double[] reload = HbmAnimations.getRelevantTransformation("QUADRO_RELOAD_ROTATE");
				rot(pose, reload[2], (float) (1), (float) (0), (float) (0));
			}
			
			break;
			
		case EQUIPPED:

			if(is(stack, "gun_hk69")) {
				rot(pose, 20F, 1.0F, 0.0F, 1.0F);
				rot(pose, 10F, 0.0F, 1.0F, 0.0F);
				pose.translate(0.4F, 0.0F, 0.55F);
				pose.scale((float) (0.75), (float) (0.75), (float) (0.75));
			}

			if(stack.getItem() == LegacyGuns.GUN_DEAGLE.get()) {
				rot(pose, 90F, 0.0F, 1.0F, 0.0F);
				rot(pose, 35F, -1.0F, 0.0F, 0.0F);
				pose.translate(0.0F, -0.1F, 0.8F);
				pose.scale((float) (0.15), (float) (0.15), (float) (0.15));
			}

			if(is(stack, "gun_flamer_legacy")) {
				rot(pose, 20F, 1.0F, 0.0F, 1.0F);
				rot(pose, 10F, 0.0F, 1.0F, 0.0F);
				pose.translate(0.4F, -0.25F, 0.2F);
				pose.scale((float) (0.35), (float) (0.35), (float) (0.35));
			}

			if(is(stack, "gun_flechette")) {
				rot(pose, 35F, 0.0F, 0.0F, 1.0F);
				rot(pose, 180F, 0.0F, 1.0F, 0.0F);
				pose.translate(-1.4F, -0.55F, 0.0F);
				pose.scale((float) (0.125), (float) (0.125), (float) (0.125));
			}

			if(is(stack, "gun_quadro_legacy")) {
				rot(pose, 20F, 1.0F, 0.0F, 1.0F);
				rot(pose, 10F, 0.0F, 1.0F, 0.0F);
				pose.translate(0.4F, -0.35F, -0.4F);
				pose.scale((float) (0.75), (float) (0.75), (float) (0.75));
			}
			
			break;
			
		case ENTITY:
			
			if(is(stack, "gun_hk69")) {
				pose.translate(0.0F, 0.2F, 0.0F);
				rot(pose, -90F, 0.0F, 1.0F, 0.0F);
				pose.scale((float) (0.75), (float) (0.75), (float) (0.75));
			}

			if(stack.getItem() == LegacyGuns.GUN_DEAGLE.get()) {
				pose.translate(0.0F, 0.2F, 0.0F);
				rot(pose, -90F, 0.0F, 1.0F, 0.0F);
				pose.scale((float) (0.25), (float) (0.25), (float) (0.25));
			}
			
			if(is(stack, "gun_flamer_legacy")) {
				pose.translate(0.25F, 0.2F, 0.0F);
				rot(pose, -90F, 0.0F, 1.0F, 0.0F);
				pose.scale((float) (0.25), (float) (0.25), (float) (0.25));
			}
			
			if(is(stack, "gun_flechette")) {
				pose.translate(-0.25F, 0.0F, 0.0F);
				pose.scale((float) (0.125), (float) (0.125), (float) (0.125));
			}
			
			if(is(stack, "gun_quadro_legacy")) {
				rot(pose, -90F, 0.0F, 1.0F, 0.0F);
				pose.translate(0.0F, 0.0F, 0.0F);
				pose.scale((float) (0.75), (float) (0.75), (float) (0.75));
			}
			
			break;
			
		case INVENTORY:

			if(is(stack, "gun_hk69")) {
				pose.scale((float) (7.5), (float) (7.5), (float) (-7.5));
				pose.translate(0.85F, 1.2F, 0.0F);
				rot(pose, 180F, 1.0F, 0.0F, 0.0F);
				rot(pose, -90F, 0.0F, 1.0F, 0.0F);
				rot(pose, -45F, 1.0F, 0.0F, 0.0F);
			}
			
			if(stack.getItem() == LegacyGuns.GUN_DEAGLE.get()) {
				pose.scale((float) (2.5), (float) (2.5), (float) (-2.5));
				pose.translate(3.5F, 3F, 0.0F);
				rot(pose, 180F, 1.0F, 0.0F, 0.0F);
				rot(pose, -90F, 0.0F, 1.0F, 0.0F);
				rot(pose, -45F, 1.0F, 0.0F, 0.0F);
			}
			
			if(is(stack, "gun_flamer_legacy")) {
				pose.scale((float) (2.0), (float) (2.0), (float) (-2.0));
				pose.translate(4.0F, 5.0F, 0.0F);
				rot(pose, 180F, 1.0F, 0.0F, 0.0F);
				rot(pose, -90F, 0.0F, 1.0F, 0.0F);
				rot(pose, -45F, 1.0F, 0.0F, 0.0F);
			}
			
			if(is(stack, "gun_flechette")) {
				pose.scale((float) (1.2), (float) (1.2), (float) (-1.2));
				pose.translate(2.5F, 8.0F, 0.0F);
				rot(pose, 180F, 1.0F, 0.0F, 0.0F);
				rot(pose, -45F, 0.0F, 0.0F, 1.0F);
			}
			
			if(is(stack, "gun_quadro_legacy")) {
				pose.scale((float) (4.5), (float) (4.5), (float) (-4.5));
				pose.translate(1.0F, 2.5F, 0.0F);
				rot(pose, 180F, 1.0F, 0.0F, 0.0F);
				rot(pose, -90F, 0.0F, 1.0F, 0.0F);
				rot(pose, -45F, 1.0F, 0.0F, 0.0F);
			}
			
			break;
			
		default: break;
		}

		if(is(stack, "gun_hk69")) {

			all(c, pose, LegacyResources.hk69, tex, light);

		}

		if(stack.getItem() == LegacyGuns.GUN_DEAGLE.get()) {

			all(c, pose, LegacyResources.deagle, tex, light);

		}

		if(is(stack, "gun_flamer_legacy")) {

			all(c, pose, LegacyResources.flamer, tex, light);

		}

		if(is(stack, "gun_flechette")) {
			renderFlechette(c, pose, tex, light);
		}

		if(is(stack, "gun_quadro_legacy")) {

			part(c, pose, LegacyResources.quadro, tex, QUADRO_LAUNCHER, light);

		}

		pose.popPose();
	
        }
    }
}
