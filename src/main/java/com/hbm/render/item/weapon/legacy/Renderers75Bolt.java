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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the 75Bolt family. */
public final class Renderers75Bolt {

    private Renderers75Bolt() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_BOLTER_LEGACY, new ItemRenderWeaponBolter());
        LegacyGunRenderer.register(LegacyGuns.GUN_BOLTER_DIGAMMA, new ItemRenderWeaponBolter());
    }

    public static final class ItemRenderWeaponBolter extends LegacyGunRenderer {
        private static final int BOLTER_BODY = LegacyResources.bolter.partId("Body");
        private static final int BOLTER_MAG = LegacyResources.bolter.partId("Mag");
        private static final int BOLTER_BULLET = LegacyResources.bolter.partId("Bullet");
        private static final int BOLTER_CASING = LegacyResources.bolter.partId("Casing");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();


		if(stack.getItem() == LegacyGuns.GUN_BOLTER_LEGACY.get())
			tex = LegacyResources.bolter_tex;
		if(stack.getItem() == LegacyGuns.GUN_BOLTER_DIGAMMA.get())
			tex = LegacyResources.bolter_digamma_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.25D;
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			pose.translate(1.25, -0.25, -0.25);
			rot(pose, -100, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
			rot(pose, recoil[0] * 5, (float) (1), (float) (0), (float) (0));
			pose.translate(0, 0, recoil[0]);

			double[] tilt = HbmAnimations.getRelevantTransformation("TILT");
			pose.translate(0, tilt[0], 3);
			rot(pose, tilt[0] * 35, (float) (1), (float) (0), (float) (0));
			pose.translate(0, 0, -3);
			
			part(c, pose, LegacyResources.bolter, tex, BOLTER_BODY, light);

			double[] mag = HbmAnimations.getRelevantTransformation("MAG");
			pose.pushPose();
			pose.translate(0, 0, 5);
			rot(pose, mag[0] * 60 * (mag[2] == 1 ? 2.5 : 1), (float) (-1), (float) (0), (float) (0));
			pose.translate(0, 0, -5);
			part(c, pose, LegacyResources.bolter, tex, BOLTER_MAG, light);
			if(mag[2] != 1)
				part(c, pose, LegacyResources.bolter, tex, BOLTER_BULLET, light);
			pose.popPose();

			pose.pushPose();
			double[] casing = HbmAnimations.getRelevantTransformation("EJECT");
			pose.translate(casing[2] * 5, casing[2] * 2, 0);
			rot(pose, casing[2] * 60, (float) (1), (float) (0), (float) (0));
			part(c, pose, LegacyResources.bolter, tex, BOLTER_CASING, light);
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

			double s = 2.5D;
			pose.translate(6, 10, 0);
			rot(pose, 90, (float) (0), (float) (1), (float) (0));
			rot(pose, 135, (float) (1), (float) (0), (float) (0));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}
		
		if(ctx != Ctx.FIRST_PERSON) {
			all(c, pose, LegacyResources.bolter, tex, light);
		}

		pose.pushPose();
		// main drew the mag count with the font renderer, unlit (the repo's sedna bolter does the same)
		net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
		String s = ((ItemGunBase) stack.getItem()).getMag(stack) + "";
		float f3 = 0.04F;
		pose.translate(0.025F - (font.width(s) / 2) * 0.04F, 2.11F, 2.91F);
		pose.scale(f3, -f3, f3);
		rot(pose, 45, 1, 0, 0);
		c.submitText(pose, 0, 0, net.minecraft.network.chat.Component.literal(s).getVisualOrderText(), false,
				net.minecraft.client.gui.Font.DisplayMode.NORMAL, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, 0xFFFF0000, 0, 0);
		pose.popPose();
		
		pose.popPose();
	
        }
    }
}
