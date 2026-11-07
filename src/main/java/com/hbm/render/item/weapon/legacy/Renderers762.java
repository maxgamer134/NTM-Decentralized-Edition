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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the 762 family. */
public final class Renderers762 {

    private Renderers762() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_CALAMITY, new ItemRenderWeaponFFMG42());
        LegacyGunRenderer.register(LegacyGuns.GUN_BOLT_ACTION, new ItemRenderWeaponFFBolt(LegacyResources.rem700, LegacyResources.rem700_tex));
        LegacyGunRenderer.register(LegacyGuns.GUN_BOLT_ACTION_GREEN, new ItemRenderWeaponFFBolt(LegacyResources.rem700poly, LegacyResources.rem700poly_tex));
        LegacyGunRenderer.register(LegacyGuns.GUN_BOLT_ACTION_SATURNITE, new ItemRenderWeaponFFBolt(LegacyResources.rem700sat, LegacyResources.rem700sat_tex));
    }

    public static final class ItemRenderWeaponFFMG42 extends LegacyGunRenderer {
        private static final int MG42_MAG = LegacyResources.mg42.partId("Mag");
        private static final int MG42_GUN = LegacyResources.mg42.partId("Gun");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();


		tex = LegacyResources.mg42_tex;
		boolean renderMag = true;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.65D;
			rot(pose, 170, (float) (0), (float) (1), (float) (0));
			rot(pose, -25, (float) (0), (float) (0), (float) (1));
			pose.translate(-0.125, 0, 0);
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
			pose.translate(recoil[0] * 0.25, 0, 0);
			
			double[] reload = HbmAnimations.getRelevantTransformation("MAG");
			rot(pose, reload[1] * 15, (float) (1), (float) (0), (float) (0));
			
			pose.pushPose();
			
			rot(pose, reload[1] * -45, (float) (1), (float) (0), (float) (0));
			pose.translate(0, reload[1] * 0.25, -reload[1]);
			part(c, pose, LegacyResources.mg42, tex, MG42_MAG, light);
			pose.popPose();
			
			renderMag = false;
			
			break;
			
		case EQUIPPED:

			double scale = 0.35D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 100, (float) (0), (float) (1), (float) (0));
			rot(pose, -10, (float) (1), (float) (0), (float) (0));
			rot(pose, 10, (float) (0), (float) (0), (float) (1));
			pose.translate(-0.5, 0.75, 1.25);
			
			break;
			
		case ENTITY:

			double s1 = 0.3D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			
			break;
			
		case INVENTORY:

			double s = 1.75D;
			pose.translate(10, 9.5, 0);
			rot(pose, 180, (float) (1), (float) (0), (float) (0));
			rot(pose, -45, (float) (0), (float) (0), (float) (1));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}

		part(c, pose, LegacyResources.mg42, tex, MG42_GUN, light);
		if(renderMag) part(c, pose, LegacyResources.mg42, tex, MG42_MAG, light);

		pose.popPose();
	
        }
    }

    public static final class ItemRenderWeaponFFBolt extends LegacyGunRenderer {

        private final HFRWavefrontObject model;
        private final ResourceLocation texture;
        public ItemRenderWeaponFFBolt(HFRWavefrontObject model, ResourceLocation texture) {
            this.model = model;
            this.texture = texture;
        }

        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();


		boolean renderBolt = true;
		tex = texture;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			if(stack.getItem() == LegacyGuns.GUN_BOLT_ACTION_SATURNITE.get() && Minecraft.getInstance().player.isShiftKeyDown()) {
				pose.popPose();
				return;
			}
			
			double s0 = 0.5D;
			pose.translate(0.5, 0.25, -0.2);
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			rot(pose, 15, (float) (0), (float) (0), (float) (1));
			rot(pose, 20, (float) (0), (float) (-1), (float) (0));
			
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
			pose.translate(recoil[0] * -0.5, 0, 0);

			double[] pull = HbmAnimations.getRelevantTransformation("LEVER_PULL");
			double[] rotate = HbmAnimations.getRelevantTransformation("LEVER_ROTATE");
			
			pose.pushPose();
			pose.translate(pull[0], 0, 0);
			double heightOffset = 0.52D;
			pose.translate(0, heightOffset, 0);
			rot(pose, rotate[0] * 35, (float) (-1), (float) (0), (float) (0));
			pose.translate(0, -heightOffset, 0);
			part(c, pose, model, tex, "Bolt", light);
			pose.popPose();
			
			renderBolt = false;

			/*if(stack.getItem() == LegacyGuns.GUN_BOLT_ACTION_SATURNITE.get()) {
				pose.pushPose();


				Tessellator tessellator = Tessellator.instance;
				int color = 0x00FF00;
	
				tessellator.startDrawing(3);
				tessellator.setColorOpaque_I(color);
				tessellator.addVertex(5, 0, 0);
				tessellator.addVertex(150, 0, 0);
				tessellator.draw();


				pose.popPose();
			}*/
			
			break;
			
		case EQUIPPED:

			double scale = 0.25D;
			rot(pose, -75, (float) (0), (float) (1), (float) (0));
			rot(pose, -10, (float) (0), (float) (0), (float) (1));
			rot(pose, 10, (float) (1), (float) (0), (float) (0));
			pose.translate(0.3, 0.15, -0.5);
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			
			break;
			
		case ENTITY:

			double s1 = 0.25D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			rot(pose, 90, (float) (0), (float) (1), (float) (0));
			
			break;
			
		case INVENTORY:

			double s = 1.5D;
			pose.translate(10, 11, 0);
			rot(pose, -135, (float) (0), (float) (0), (float) (1));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}
		
		part(c, pose, model, tex, "Gun", light);
		if(renderBolt)
			part(c, pose, model, tex, "Bolt", light);

		pose.popPose();
	
        }
    }
}
