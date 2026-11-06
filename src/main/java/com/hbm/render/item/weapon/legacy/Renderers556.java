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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the 556 family. */
public final class Renderers556 {

    private Renderers556() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_FLECHETTE, new Renderers50AE.ItemRenderWeaponObj());
        LegacyGunRenderer.register(LegacyGuns.GUN_MP, new ItemRenderMP());
        LegacyGunRenderer.register(LegacyGuns.GUN_VORTEX, new ItemRenderWeaponVortex());
    }

    public static final class ItemRenderMP extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;


		pose.pushPose();
		tex = Library.id("textures/models/ModelMP.png");
		
		switch (ctx) {
		case FIRST_PERSON:
			pose.translate(1.0F, 0.75F, 0.0F);
			rot(pose, 150F, 0.0F, 0.0F, -1.0F);
			pose.scale(0.75F, 0.75F, 0.75F);
			all(c, pose, LegacyResources.model_mp, tex, light);
			break;
			
		case EQUIPPED:
			pose.translate(0.35F, 0.5F, 0.0F);
			rot(pose, 100F, 0.0F, 1.0F, 0.0F);
			rot(pose, 170F, 1.0F, 0.0F, 0.0F);
			rot(pose, 15F, 0.0F, 0.0F, -1.0F);
			pose.scale(0.75F, 0.75F, 0.75F);
			all(c, pose, LegacyResources.model_mp, tex, light);
			break;
			
		case ENTITY:
			pose.translate(0.0F, 1.0F, 0.0F);
			rot(pose, 180F, 1.0F, 0.0F, 0.0F);
			pose.scale(0.75F, 0.75F, 0.75F);
			all(c, pose, LegacyResources.model_mp, tex, light);
			break;
			
		default: break;
		}
		
		pose.popPose();
	
        }
    }

    public static final class ItemRenderWeaponVortex extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();

		tex = LegacyResources.vortex_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.05D;
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			rot(pose, -5, (float) (0), (float) (1), (float) (0));
			pose.translate(17, -5, -5);
			
			break;
			
		case EQUIPPED:

			double scale = 0.075D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, -75, (float) (0), (float) (1), (float) (0));
			rot(pose, -10, (float) (0), (float) (0), (float) (1));
			rot(pose, 15, (float) (1), (float) (0), (float) (0));
			pose.translate(7, -4, -6);
			
			break;
			
		case ENTITY:

			double s1 = 0.05D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			
			break;
			
		case INVENTORY:

			double s = 0.3D;
			pose.translate(7, 11, 0);
			rot(pose, -135, (float) (0), (float) (0), (float) (1));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}

		all(c, pose, LegacyResources.vortex, tex, light);
		
		pose.popPose();
	
        }
    }
}
