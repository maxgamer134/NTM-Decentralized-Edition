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
import com.hbm.particle.SpentCasing;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the Grenade family. */
public final class RenderersGrenade {

    private RenderersGrenade() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_HK69, new Renderers50AE.ItemRenderWeaponObj());
        LegacyGunRenderer.register(LegacyGuns.GUN_CONGOLAKE_LEGACY, new ItemRenderWeaponCongo());
    }

    public static final class ItemRenderWeaponCongo extends LegacyGunRenderer {
        private static final int CONGOLAKE_GUN = LegacyResources.congolake.partId("Gun");
        private static final int CONGOLAKE_PUMP = LegacyResources.congolake.partId("Pump");
        private static final int CONGOLAKE_SIGHT = LegacyResources.congolake.partId("Sight");
        private static final int CONGOLAKE_LOOP = LegacyResources.congolake.partId("Loop");
        private static final int CONGOLAKE_GUARDOUTER = LegacyResources.congolake.partId("GuardOuter");
        private static final int CONGOLAKE_GUARDINNER = LegacyResources.congolake.partId("GuardInner");
        private static final int CONGOLAKE_SHELL = LegacyResources.congolake.partId("Shell");
        private static final int CONGOLAKE_SHELLFORE = LegacyResources.congolake.partId("ShellFore");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();

		tex = LegacyResources.congolake_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.25D;
			rot(pose, 20, (float) (0), (float) (0), (float) (1));
			pose.translate(0.5, 0.0, -0.5);
			rot(pose, -10, (float) (0), (float) (1), (float) (0));
			rot(pose, 90, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s0), (float) (s0), (float) (s0));

			HbmAnimations.applyRelevantTransformation(pose, "Gun");
			part(c, pose, LegacyResources.congolake, tex, CONGOLAKE_GUN, light);


			pose.pushPose();
			{
				HbmAnimations.applyRelevantTransformation(pose, "Pump");
				part(c, pose, LegacyResources.congolake, tex, CONGOLAKE_PUMP, light);
			}
			pose.popPose();


			pose.pushPose();
			{
				HbmAnimations.applyRelevantTransformation(pose, "Sight");
				part(c, pose, LegacyResources.congolake, tex, CONGOLAKE_SIGHT, light);
			}
			pose.popPose();


			pose.pushPose();
			{
				HbmAnimations.applyRelevantTransformation(pose, "Loop");
				part(c, pose, LegacyResources.congolake, tex, CONGOLAKE_LOOP, light);
			}
			pose.popPose();


			pose.pushPose();
			{
				HbmAnimations.applyRelevantTransformation(pose, "GuardOuter");
				part(c, pose, LegacyResources.congolake, tex, CONGOLAKE_GUARDOUTER, light);

				pose.pushPose();
				{
					HbmAnimations.applyRelevantTransformation(pose, "GuardInner");
					part(c, pose, LegacyResources.congolake, tex, CONGOLAKE_GUARDINNER, light);
				}
				pose.popPose();
			}
			pose.popPose();


			pose.pushPose();
			{
				tex = LegacyResources.casings_tex;

				HbmAnimations.applyRelevantTransformation(pose, "Shell");

				ItemGunBase gun = (ItemGunBase)stack.getItem();
				BulletConfiguration bullet = BulletConfigSyncingUtil.pullConfig(gun.mainConfig.config.get(ItemGunBase.getMagType(stack)));
				int[] colors = bullet.spentCasing != null ? bullet.spentCasing.getColors() : new int[] { SpentCasing.COLOR_CASE_40MM };

				

				partColor(c, pose, LegacyResources.congolake, tex, CONGOLAKE_SHELL, light, colors[0]);
				
				

				partColor(c, pose, LegacyResources.congolake, tex, CONGOLAKE_SHELLFORE, light, colors.length > 1 ? colors[1] : colors[0]);

			}
			pose.popPose();
			
			break;
			
		case EQUIPPED:

			double scale = 0.35D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 15F, 1.0F, 0.0F, 0.0F);
			rot(pose, 15F, 0.0F, 0.0F, 1.0F);
			rot(pose, 15, 0.0F, 1.0F, 0.0F);
			pose.translate(1.25F, -1F, 1.5F);
			
			break;
			
		case ENTITY:

			double s1 = 0.2D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			
			break;
			
		case INVENTORY:
			
			double s = 2D;
			pose.translate(6, 9, 0);
			rot(pose, -135, (float) (0), (float) (0), (float) (1));
			rot(pose, -90, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}

		if(ctx != Ctx.FIRST_PERSON) {
			all(c, pose, LegacyResources.congolake, tex, light);
		}

		pose.popPose();
	
        }
    }
}
