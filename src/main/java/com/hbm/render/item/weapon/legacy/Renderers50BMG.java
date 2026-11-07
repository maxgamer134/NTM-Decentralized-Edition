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
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the 50BMG family. */
public final class Renderers50BMG {

    private Renderers50BMG() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_AR15, new ItemRenderWeaponAR15());
        LegacyGunRenderer.register(LegacyGuns.GUN_LUNATIC_MARKSMAN, new ItemRenderLunaticSniper());
    }

    public static final class ItemRenderWeaponAR15 extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;


		pose.pushPose();


		tex = LegacyResources.ar15_tex;

		HFRWavefrontObject model = LegacyResources.ar15;

		switch (ctx) {

			case FIRST_PERSON:

				double s0 = 0.25D;
				rot(pose, 25, (float) (0), (float) (0), (float) (1));
				pose.translate(1.25, 0, -0.25);
				rot(pose, -100, (float) (0), (float) (1), (float) (0));
				pose.scale((float) (s0), (float) (s0), (float) (s0));

				double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
				rot(pose, recoil[0] * 2.5, (float) (1), (float) (0), (float) (0));
				pose.translate(0, 0, recoil[0]);


				double[] tilt = HbmAnimations.getRelevantTransformation("TILT");
				pose.translate(0, tilt[0], 3);
				rot(pose, tilt[0] * -35, (float) (0), (float) (0), (float) (1));
				pose.translate(0, 0, -3);

				part(c, pose, model, tex, "main", light);

				double[] mag = HbmAnimations.getRelevantTransformation("MAG");
				pose.pushPose();
				pose.translate(0, 0, 5);
				rot(pose, mag[0] * 60 * (mag[2] == 1 ? 2.5 : 1), (float) (-1), (float) (0), (float) (0));
				pose.translate(0, 0, -5);
				part(c, pose, model, tex, "mag", light);
				pose.popPose();

				break;

			case EQUIPPED:

				double scale = 0.25D;
				pose.scale((float) (scale), (float) (scale), (float) (scale));
				rot(pose, 15F, 0.0F, 0.0F, 1.0F);
				rot(pose, -170, 0.0F, 1.0F, 0.0F);
				rot(pose, -15F, 1.0F, 0.0F, 0.0F);
				pose.translate(-2F, -0.9F, -0.75F);

				break;

			case ENTITY:

				double s1 = 0.2D;
				pose.scale((float) (s1), (float) (s1), (float) (s1));
				pose.translate(0, 1, 0);
				rot(pose, 90, (float) (0), (float) (1), (float) (0));

				break;

			case INVENTORY:

				double s = 1.45D;

				pose.translate(6, 9, 0);
				rot(pose, 90, (float) (0), (float) (1), (float) (0));
				rot(pose, 135, (float) (1), (float) (0), (float) (0));
				pose.scale((float) (s), (float) (s), (float) (-s));

				break;

			default:
				break;
		}

		if (ctx != Ctx.FIRST_PERSON) {
			all(c, pose, model, tex, light);
		}

		pose.popPose();
	
        }
    }

    public static final class ItemRenderLunaticSniper extends LegacyGunRenderer {
        private static final int LUNATIC_SNIPER_FULL_CYLINDER_007 = LegacyResources.lunatic_sniper.partId("Full_Cylinder.007");
        private static final int LUNATIC_SNIPER_SLIDE_CUBE_020_CUBE_007 = LegacyResources.lunatic_sniper.partId("Slide_Cube.020_Cube.007");
        private static final int LUNATIC_SNIPER_FULL_ROUND_BULLET = LegacyResources.lunatic_sniper.partId("Full_Round_Bullet");
        private static final int LUNATIC_SNIPER_SPENT_CASING_CASING = LegacyResources.lunatic_sniper.partId("Spent_Casing_Casing");
        private static final float scale1 = 0.2F;
        private static final float scale2 = 0.1F;
        private static final float scale3 = 0.15F;
        private static final float scale4 = 0.7F;


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
//		double[] eject = HbmAnimations.getRelevantTransformation("EJECT");
		double[] tilt = HbmAnimations.getRelevantTransformation("TILT");
//		double[] insert = HbmAnimations.getRelevantTransformation("INSERT_ROUND");
		tex = LegacyResources.lunatic_sniper_tex;

		switch (ctx) {
		case FIRST_PERSON:// In hand from POV
			
			if(com.hbm.items.weapon.legacy.ItemGunBase.isAiming(Minecraft.getInstance().player)) {
				pose.popPose();
				return;
			}

			rot(pose, -10F, 0F, 1F, 0F);
			rot(pose, -10F, 0F, 0F, 1F);
			rot(pose, 90F, 1F, 0F, 0F);
			rot(pose, -58.5F, 0F, 1F, 0F);
			rot(pose, 90F, 0F, 0F, 1F);
			pose.translate(-0.5F, 0F, 0F);
			pose.scale((float) (scale1), (float) (scale1), (float) (scale1));
			
			/// Begin animations ///
			
			// Move on recoil
			pose.translate(0, 0, recoil[1] * 10);
			rot(pose, recoil[0] * 10, (float) (0), (float) (0), (float) (1));
			// Move on reload
			pose.pushPose();
			rot(pose, tilt[0] * 2, (float) (0), (float) (0), (float) (1));
			rot(pose, -tilt[1] * 2, (float) (0), (float) (1), (float) (0));
			pose.translate(0, 0, -tilt[1] / 8);
			part(c, pose, LegacyResources.lunatic_sniper, tex, LUNATIC_SNIPER_FULL_CYLINDER_007, light);
			pose.popPose();
			// Release slide
			pose.pushPose();
			rot(pose, tilt[0] * 2, (float) (0), (float) (0), (float) (1));
			rot(pose, -tilt[1] * 2, (float) (0), (float) (1), (float) (0));
			pose.translate(0, 0, -tilt[2] * 15);
			part(c, pose, LegacyResources.lunatic_sniper, tex, LUNATIC_SNIPER_SLIDE_CUBE_020_CUBE_007, light);
			pose.popPose();
			// Drop in new round
			pose.pushPose();
			rot(pose, tilt[0], (float) (0), (float) (0), (float) (1));
			pose.translate(0, tilt[1], tilt[2]);
			part(c, pose, LegacyResources.lunatic_sniper, tex, LUNATIC_SNIPER_FULL_ROUND_BULLET, light);
			pose.popPose();
			// Eject casing
			pose.pushPose();
			pose.translate(0, 2, 0);//FIXME Where on earth is it?!
//			part(c, pose, LegacyResources.lunatic_sniper, tex, LUNATIC_SNIPER_SPENT_CASING_CASING, light);
			pose.popPose();
			break;
		case EQUIPPED:// In hand from other's POV
			rot(pose, -5F, 0F, 1F, 0F);
			rot(pose, -5F, 0F, 0F, 1F);
			rot(pose, 90F, 1F, 0F, 0F);
			rot(pose, -50F, 0F, 1F, 0F);
			rot(pose, 90F, 0F, 0F, 1F);
			pose.translate(-0.05F, -0.2F, -0.75F);
			pose.scale((float) (scale2 - scale2 * 2), (float) (scale2), (float) (scale2));
			pose.pushPose();
//			pose.translate(eject[0] / 2, 0, -5);
//			part(c, pose, LegacyResources.lunatic_sniper, tex, LUNATIC_SNIPER_SPENT_CASING_CASING, light);
			pose.popPose();
			break;
		case ENTITY:// Dropped stack
			pose.scale((float) (scale3), (float) (scale3), (float) (scale3));
			break;
		case INVENTORY:
			pose.translate(10F, 11.5F, 0F);
			rot(pose, -90F, 0.0F, 1.0F, 0.0F);
			rot(pose, -135F, 1.0F, 0.0F, 0.0F);
			pose.scale((float) (-scale4), (float) (scale4), (float) (scale4));
			break;
		default:
			break;
		}
		
		if(ctx != Ctx.FIRST_PERSON) {
			allExcept(c, pose, LegacyResources.lunatic_sniper, tex, light, "Full_Round_Bullet", "Spent_Casing_Casing");
		}

		pose.popPose();
	
        }
    }
}
