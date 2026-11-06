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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the 45ACP family. */
public final class Renderers45ACP {

    private Renderers45ACP() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_THOMPSON, new ItemRenderWeaponThompson());
        LegacyGunRenderer.register(LegacyGuns.GUN_UAC_PISTOL, new ItemRenderUACPistol());
    }

    public static final class ItemRenderWeaponThompson extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();

		tex = LegacyResources.thompson_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.25D;
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			pose.translate(0.5, -0.5, -0.5);
			rot(pose, -100, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			
			break;
			
		case EQUIPPED:

			double scale = 0.25D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 20F, 0.0F, 0.0F, 1.0F);
			rot(pose, -170, 0.0F, 1.0F, 0.0F);
			rot(pose, -15F, 1.0F, 0.0F, 0.0F);
			pose.translate(-2F, -3F, -0.5F);
			
			break;
			
		case ENTITY:

			double s1 = 0.2D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			
			break;
			
		case INVENTORY:

			double s = 1.75D;
			pose.translate(4, 11, 0);
			rot(pose, 90, (float) (0), (float) (1), (float) (0));
			rot(pose, 135, (float) (1), (float) (0), (float) (0));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}

		all(c, pose, LegacyResources.thompson, tex, light);
		
		pose.popPose();
	
        }
    }

    public static final class ItemRenderUACPistol extends LegacyGunRenderer {
        private static final int UAC_PISTOL_TRIGGER_PISTOL_0 = LegacyResources.uac_pistol.partId("trigger_pistol_0");
        private static final int UAC_PISTOL_MAG_PISTOL_0 = LegacyResources.uac_pistol.partId("mag_pistol_0");
        private static final int UAC_PISTOL_LOWER_PISTOL_0 = LegacyResources.uac_pistol.partId("lower_pistol_0");
        private static final int UAC_PISTOL_SLIDE_PISTOL_0 = LegacyResources.uac_pistol.partId("slide_pistol_0");
        private static final int UAC_PISTOL_BOOL_OBJ_BOOL_OBJ_DATA_002 = LegacyResources.uac_pistol.partId("bool_obj_bool_obj_data.002");
        private static final float scale1 = 0.05F;
        private static final float scale2 = 0.025F;
        private static final float scale3 = 0.05F;
        private static final float scale4 = 0.55F;


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		pose.pushPose();
		tex = LegacyResources.uac_pistol_tex;
		Player player = Minecraft.getInstance().player;

		double[] slideAnim = HbmAnimations.getRelevantTransformation("SLIDE");
		double[] hammerAnim = HbmAnimations.getRelevantTransformation("HAMMER");

		switch (ctx) {
		case ENTITY:// Dropped stack
			pose.scale((float) (scale1), (float) (scale1), (float) (scale1));
			break;
		case EQUIPPED:// In hand from other's POV
			pose.scale((float) (scale2), (float) (scale2), (float) (scale2));
			rot(pose, 90F, 0F, 1F, 0F);
			rot(pose, -45F, 1F, 0F, 0F);
			pose.translate(0F, -12F, 30F);

			pose.pushPose();
			part(c, pose, LegacyResources.uac_pistol, tex, UAC_PISTOL_TRIGGER_PISTOL_0, light); part(c, pose, LegacyResources.uac_pistol, tex, UAC_PISTOL_MAG_PISTOL_0, light); part(c, pose, LegacyResources.uac_pistol, tex, UAC_PISTOL_LOWER_PISTOL_0, light);
			pose.popPose();

			pose.pushPose();
			pose.translate(slideAnim[0], slideAnim[1], slideAnim[2] * 2);
			part(c, pose, LegacyResources.uac_pistol, tex, UAC_PISTOL_SLIDE_PISTOL_0, light);
			pose.popPose();

			pose.pushPose();
			rot(pose, hammerAnim[0], 1F, 0F, 0F);
			part(c, pose, LegacyResources.uac_pistol, tex, UAC_PISTOL_BOOL_OBJ_BOOL_OBJ_DATA_002, light);
			pose.popPose();
			break;
		case FIRST_PERSON:// In hand from POV
			pose.scale((float) (scale3), (float) (scale3), (float) (scale3));
			rot(pose, 90F, 0F, 1F, 0F);
			if(player.isShiftKeyDown()) {
				rot(pose, -25F, 1F, 0F, 0F);
				rot(pose, -5F, 0F, 1F, 0F);
				pose.translate(20.15F, 3.5F, 18F);
			} else {
				rot(pose, -25F, 1F, 0F, 0F);
				pose.translate(-3F, -5F, 20F);
			}
			pose.pushPose();
			pose.translate(slideAnim[0], slideAnim[1], slideAnim[2] * 2);
			part(c, pose, LegacyResources.uac_pistol, tex, UAC_PISTOL_SLIDE_PISTOL_0, light);
			pose.popPose();

			pose.pushPose();
			rot(pose, hammerAnim[0], 1F, 0F, 0F);
			part(c, pose, LegacyResources.uac_pistol, tex, UAC_PISTOL_BOOL_OBJ_BOOL_OBJ_DATA_002, light);
			pose.popPose();
			break;
		case INVENTORY:
			pose.scale((float) (scale4), (float) (scale4), (float) (scale4));
			rot(pose, 90F, 0F, 1F, 0F);
			rot(pose, 150F, 1F, 0F, 0F);
			pose.translate(0F, -8F, -24F);
			break;
		default:
			break;
		}
		if(ctx != Ctx.FIRST_PERSON && ctx != Ctx.EQUIPPED)
			all(c, pose, LegacyResources.uac_pistol, tex, light);
		else
			part(c, pose, LegacyResources.uac_pistol, tex, UAC_PISTOL_TRIGGER_PISTOL_0, light); part(c, pose, LegacyResources.uac_pistol, tex, UAC_PISTOL_MAG_PISTOL_0, light); part(c, pose, LegacyResources.uac_pistol, tex, UAC_PISTOL_LOWER_PISTOL_0, light);

		pose.popPose();
	
        }
    }
}
