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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the RocketHoming family. */
public final class RenderersRocketHoming {

    private RenderersRocketHoming() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_STINGER_LEGACY, new ItemRenderStinger());
        LegacyGunRenderer.register(LegacyGuns.GUN_SKYSTINGER, new ItemRenderStinger());
    }

    public static final class ItemRenderStinger extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		ResourceLocation stingerTex = stack.getItem() == LegacyGuns.GUN_STINGER_LEGACY.get() ? LegacyResources.stinger_tex : LegacyResources.sky_stinger_tex;
		tex = stingerTex;
		pose.pushPose();
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.25D;
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			pose.translate(-0.5, -0.7, -0.5);
			rot(pose, -100, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s0), (float) (s0), (float) (s0));
				
			break;
			
		case EQUIPPED:

			double scale = 0.25D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 20F, 0.0F, 0.0F, 1.0F);
			rot(pose, -170, 0.0F, 1.0F, 0.0F);
			rot(pose, -15F, 1.0F, 0.0F, 0.0F);
			pose.translate(-2F, -3F, 4.0F);
			
			break;
			
		case ENTITY:

			double s1 = 0.2D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			pose.translate(0F, -2.5F, 0F);
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
		all(c, pose, LegacyResources.stinger, tex, light);
		pose.popPose();
	
        }
    }
}
