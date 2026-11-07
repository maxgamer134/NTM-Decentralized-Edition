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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the Powered family. */
public final class RenderersPowered {

    private RenderersPowered() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_GLASS_CANNON, new ItemRenderWeaponGlass());
    }

    public static final class ItemRenderWeaponGlass extends LegacyGunRenderer {
        private static final int GLASS_CANNON_GUN = LegacyResources.glass_cannon.partId("Gun");
        private static final int GLASS_CANNON_PANEL = LegacyResources.glass_cannon.partId("Panel");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();


		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.5D;
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			pose.translate(2.25, 0.0, 0.125);
			rot(pose, -10, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			
			break;
			
		case EQUIPPED:

			double scale = 0.25D;
			pose.scale((float) (-scale), (float) (scale), (float) (scale));
			rot(pose, 20F, -3.0F, -0.75F, -1.0F);
			rot(pose, -170, 0.0F, 1.0F, 0.0F);
			rot(pose, -30F, 2.0F, -1F, -5.0F);
			pose.translate(5F, -0.35F, 0.25F);
			
			break;
			
		case ENTITY:

			double s1 = 0.5D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			
			break;
			
		case INVENTORY:
			
			double s = 1.65D;
			pose.translate(8F, 8F, 0F);
			rot(pose, 90, (float) (0), (float) (0), (float) (1));
			rot(pose, 135, (float) (0), (float) (0), (float) (1));
			pose.scale((float) (s), (float) (s), (float) (s));
			
			break;
			
		default: break;
		}








		part(c, pose, LegacyResources.glass_cannon, tex, GLASS_CANNON_GUN, light);








		tex = LegacyResources.glass_cannon_panel_tex;
		part(c, pose, LegacyResources.glass_cannon, tex, GLASS_CANNON_PANEL, light);
		
		int color = 0xFFFFFF;
		double freq = 1;
		
		if(stack.getItem() instanceof ItemEnergyGunBase) {
			BulletConfiguration config = ((ItemEnergyGunBase)stack.getItem()).getConfig(stack);
			String name = config.modeName;
			switch(name) {
				case "weapon.elecGun.glass_cannon.radio": color = 0xaa2200; freq = 0.5; break;
				case "weapon.elecGun.glass_cannon.micro": color = 0xdc221f; freq = 1; break;
				case "weapon.elecGun.glass_cannon.ir": color = 0xfc3d3a; freq = 1.5; break;
				case "weapon.elecGun.glass_cannon.visible": color = 0x8fe325; freq = 2; break;
				case "weapon.elecGun.glass_cannon.uv": color = 0x37d5f3; freq = 2.5; break;
				case "weapon.elecGun.glass_cannon.xray": color = 0x2542fd; freq = 3; break;
				case "weapon.elecGun.glass_cannon.gamma": color = 0xdc20f3; freq = 3.5; break;
			}
		}
		
		float px = 0.0625F;
		pose.translate(-2F, px * 18, -px * 14);
		rot(pose, 90, (float) (0), (float) (1), (float) (0));
		pose.translate(-0.7F, -0.86F, -0.33F);
		
		// main drew the wave untextured and unlit: white texture at full light, coloured per vertex
		int sub = 32;
		double width = px * 21.25;
		double len = width / sub;
		double time = GameTime.now() / -100D;
		double amplitude = 0.075;
		final int argb = 0xFF000000 | color;
		final double fq = freq;
		c.submitCustomGeometry(pose, com.hbm.backport.client.rendertype.RenderTypes.entityCutoutNoCull(com.hbm.client.render.RenderTextures.WHITE), (p, buf) -> {
			for(int i = 0; i < sub; i++) {
				double h0 = Math.sin(fq * i * 0.5 + time) * amplitude;
				double h1 = Math.sin(fq * (i + 1) * 0.5 + time) * amplitude;
				double[][] v = {{-px * 0.25 + h1, len * (i + 1)}, {px * 0.25 + h1, len * (i + 1)}, {px * 0.25 + h0, len * i}, {-px * 0.25 + h0, len * i}};
				for(double[] q : v)
					com.hbm.render.util.Vertices.emit(buf, p, 0F, (float) q[0], (float) q[1], argb, 0.5F, 0.5F, 0xF000F0, 1F, 0F, 0F);
			}
		});

		pose.popPose();
		
	
        }
    }
}
