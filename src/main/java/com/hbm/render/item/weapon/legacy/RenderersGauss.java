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
import com.hbm.items.weapon.legacy.*;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the Gauss family. */
public final class RenderersGauss {

    private RenderersGauss() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_XVL1456, new ItemRenderWeaponTau());
    }

    public static final class ItemRenderWeaponTau extends LegacyGunRenderer {
        private static final int TAU_BODY = LegacyResources.tau.partId("Body");
        private static final int TAU_ROTOR = LegacyResources.tau.partId("Rotor");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();


		tex = LegacyResources.tau_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.35D;
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			pose.translate(1.5, 0, 0.1);
			rot(pose, 80, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
			pose.translate(0, 0, recoil[0] * -1);
			pose.translate(0, 0, -3);
			rot(pose, recoil[0] * -5, (float) (1), (float) (0), (float) (0));
			pose.translate(0, 0, 3);
			
			part(c, pose, LegacyResources.tau, tex, TAU_BODY, light);
			
			if(ItemGunGauss.getCharge(stack) > 0) {
				pose.translate(0, -0.2, 0);
				rot(pose, GameTime.now() % 360D, (float) (0), (float) (0), (float) (1));
				pose.translate(0, 0.2, 0);
			}
			
			part(c, pose, LegacyResources.tau, tex, TAU_ROTOR, light);
			
			break;
			
		case EQUIPPED:

			double scale = 0.25D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 20F, 0.0F, 0.0F, 1.0F);
			rot(pose, 10, 0.0F, 1.0F, 0.0F);
			rot(pose, 15F, 1.0F, 0.0F, 0.0F);
			pose.translate(2F, 1F, 3F);
			
			all(c, pose, LegacyResources.tau, tex, light);
			
			break;
			
		case ENTITY:

			double s1 = 0.25D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			pose.translate(0, 1, 0);
			
			all(c, pose, LegacyResources.tau, tex, light);
			
			break;
			
		case INVENTORY:

			double s = 1.6D;
			pose.translate(8, 7, 0);
			rot(pose, -90, (float) (0), (float) (1), (float) (0));
			rot(pose, -135, (float) (1), (float) (0), (float) (0));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			all(c, pose, LegacyResources.tau, tex, light);
			
			break;
			
		default: break;
		}



		pose.popPose();
	
        }
    }
}
