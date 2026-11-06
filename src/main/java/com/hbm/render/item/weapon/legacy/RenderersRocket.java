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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the Rocket family. */
public final class RenderersRocket {

    private RenderersRocket() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_RPG, new ItemRenderRpg());
        LegacyGunRenderer.register(LegacyGuns.GUN_KARL, new ItemRenderRpg());
        LegacyGunRenderer.register(LegacyGuns.GUN_PANZERSCHRECK_LEGACY, new ItemRenderRpg());
        LegacyGunRenderer.register(LegacyGuns.GUN_QUADRO_LEGACY, new ItemRenderWeaponQuadro());
    }

    public static final class ItemRenderRpg extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		switch (ctx) {
		case FIRST_PERSON:
			pose.pushPose();
				if(stack.getItem() == LegacyGuns.GUN_RPG.get())
					tex = Library.id("textures/models/ModelGustav.png");
				if(stack.getItem() == LegacyGuns.GUN_KARL.get())
					tex = Library.id("textures/models/ModelGustavYellow.png");
				if(stack.getItem() == LegacyGuns.GUN_PANZERSCHRECK_LEGACY.get())
					tex = Library.id("textures/models/ModelPanzerschreck.png");
				
				rot(pose, -135.0F, 0.0F, 0.0F, 1.0F);
				pose.scale(0.5F, 0.5F, 0.5F);
				pose.translate(0.4F, -1.0F, -0.7F);

				if(stack.getItem() == LegacyGuns.GUN_PANZERSCHRECK_LEGACY.get()) {
					pose.scale(1.5F, 1.5F, 1.5F);
					
					if(Minecraft.getInstance().player.isShiftKeyDown())
						pose.translate(-0.2F, -0.3F, -0.5F);
					else
						pose.translate(-0.1F, 0.0F, 0.0F);
				} else {
					pose.translate(0F, -0.1F, -0.4F);
				}
				
				rot(pose, -20.0F, 0.0F, 0.0F, 1.0F);
				rot(pose, 5.0F, 0.0F, 1.0F, 0.0F);
				pose.translate(-0.2F, 0.0F, -0.2F);
				
				if(stack.getItem() == LegacyGuns.GUN_RPG.get())
					all(c, pose, LegacyResources.model_gustav, tex, light);
				if(stack.getItem() == LegacyGuns.GUN_KARL.get())
					all(c, pose, LegacyResources.model_gustav, tex, light);
				if(stack.getItem() == LegacyGuns.GUN_PANZERSCHRECK_LEGACY.get())
					all(c, pose, LegacyResources.model_panzerschreck, tex, light);
			pose.popPose();
			break;
		case EQUIPPED:
		case ENTITY:
			pose.pushPose();
				if(stack.getItem() == LegacyGuns.GUN_RPG.get())
					tex = Library.id("textures/models/ModelGustav.png");
				if(stack.getItem() == LegacyGuns.GUN_KARL.get())
					tex = Library.id("textures/models/ModelGustavYellow.png");
				if(stack.getItem() == LegacyGuns.GUN_PANZERSCHRECK_LEGACY.get())
					tex = Library.id("textures/models/ModelPanzerschreck.png");
				
				rot(pose, -200.0F, 0.0F, 0.0F, 1.0F);
				rot(pose, 75.0F, 0.0F, 1.0F, 0.0F);
				rot(pose, -30.0F, 1.0F, 0.0F, 0.0F);
				pose.translate(0.0F, -0.2F, -0.5F);
				rot(pose, -5.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(0.9F, 0.0F, 0.1F);

				if(stack.getItem() == LegacyGuns.GUN_PANZERSCHRECK_LEGACY.get()) {
					pose.translate(-0.5F, -0.1F, 0F);
					pose.scale(1.5F, 1.5F, 1.5F);
				}
				
				if(stack.getItem() == LegacyGuns.GUN_RPG.get())
					all(c, pose, LegacyResources.model_gustav, tex, light);
				if(stack.getItem() == LegacyGuns.GUN_KARL.get())
					all(c, pose, LegacyResources.model_gustav, tex, light);
				if(stack.getItem() == LegacyGuns.GUN_PANZERSCHRECK_LEGACY.get())
					all(c, pose, LegacyResources.model_panzerschreck, tex, light);
			pose.popPose();
		default: break;
		}
	
        }
    }

    public static final class ItemRenderWeaponQuadro extends LegacyGunRenderer {
        private static final int QUADRO_LAUNCHER = LegacyResources.quadro.partId("Launcher");
        private static final int QUADRO_ROCKETS = LegacyResources.quadro.partId("Rockets");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();

		Player player = Minecraft.getInstance().player;
		
		switch (ctx) {
		
		case FIRST_PERSON:

			if(stack.getItem() == LegacyGuns.GUN_QUADRO_LEGACY.get()) {
				pose.translate(0.75F, 0.0F, -0.15F);
				rot(pose, 90F, 0.0F, 1.0F, 0.0F);
				rot(pose, -25F, 1.0F, 0.0F, 0.0F);
				rot(pose, -10F, 0.0F, 1.0F, 0.0F);
				pose.scale((float) (0.5), (float) (0.5), (float) (0.5));
				
				if(player.isShiftKeyDown()) {
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

			if(stack.getItem() == LegacyGuns.GUN_QUADRO_LEGACY.get()) {
				rot(pose, 20F, 1.0F, 0.0F, 1.0F);
				rot(pose, 10F, 0.0F, 1.0F, 0.0F);
				pose.translate(0.4F, -0.35F, -0.4F);
				pose.scale((float) (0.75), (float) (0.75), (float) (0.75));
			}
			
			break;
			
		case ENTITY:
			
			if(stack.getItem() == LegacyGuns.GUN_QUADRO_LEGACY.get()) {
				rot(pose, -90F, 0.0F, 1.0F, 0.0F);
				pose.translate(0.0F, 0.0F, 0.0F);
				pose.scale((float) (0.75), (float) (0.75), (float) (0.75));
			}
			
			break;
			
		case INVENTORY:

			if(stack.getItem() == LegacyGuns.GUN_QUADRO_LEGACY.get()) {
				pose.scale((float) (4.5), (float) (4.5), (float) (-4.5));
				pose.translate(1.0F, 2.5F, 0.0F);
				rot(pose, 180F, 1.0F, 0.0F, 0.0F);
				rot(pose, -90F, 0.0F, 1.0F, 0.0F);
				rot(pose, -45F, 1.0F, 0.0F, 0.0F);
			}
			
			break;
			
		default: break;
		}

		if(stack.getItem() == LegacyGuns.GUN_QUADRO_LEGACY.get()) {

			tex = LegacyResources.quadro_tex;
			part(c, pose, LegacyResources.quadro, tex, QUADRO_LAUNCHER, light);
			
			if(ItemGunBase.getMag(stack) > 0 || ItemGunBase.getIsReloading(stack) && ctx != Ctx.INVENTORY) {
				pose.pushPose();
				
				pose.translate(0, -1, 0);
	
				double[] push = HbmAnimations.getRelevantTransformation("QUADRO_RELOAD_PUSH");
				pose.translate(0, 3, 0);
				rot(pose, push[1] * 30, (float) (1), (float) (0), (float) (0));
				pose.translate(0, -3, 0);
				pose.translate(0, 0, push[0] * 3);
				
				tex = LegacyResources.quadro_rocket_tex;
				part(c, pose, LegacyResources.quadro, tex, QUADRO_ROCKETS, light);
				pose.popPose();
			}

		}

		pose.popPose();
	
        }
    }
}
