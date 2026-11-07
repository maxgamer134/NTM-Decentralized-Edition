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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the 22LR family. */
public final class Renderers22LR {

    private Renderers22LR() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_UZI_LEGACY, new ItemRenderUZI());
        LegacyGunRenderer.register(LegacyGuns.GUN_UZI_SILENCER, new ItemRenderUZI());
        LegacyGunRenderer.register(LegacyGuns.GUN_UZI_SATURNITE, new ItemRenderUZI());
        LegacyGunRenderer.register(LegacyGuns.GUN_UZI_SATURNITE_SILENCER, new ItemRenderUZI());
    }

    public static final class ItemRenderUZI extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		switch (ctx) {
		case FIRST_PERSON:
			pose.pushPose();

				rot(pose, -135.0F, 0.0F, 0.0F, 1.0F);
				pose.scale(0.25F, 0.25F, 0.25F);
				pose.translate(-2.5F, -0.5F, -0.9F);
				
				rot(pose, -15.0F, 0.0F, 0.0F, 1.0F);

				if(stack.getItem() == LegacyGuns.GUN_UZI_LEGACY.get() || stack.getItem() == LegacyGuns.GUN_UZI_SILENCER.get())
					tex = Library.id("textures/models/model_uzi.png");
				if(stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE.get() || stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE_SILENCER.get())
					tex = Library.id("textures/models/model_uzi_saturnite.png");
				
				all(c, pose, LegacyResources.model_uzi, tex, light);
				
				if(stack.getItem() == LegacyGuns.GUN_UZI_LEGACY.get() || stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE.get()) {
					tex = Library.id("textures/models/model_uzi_barrel.png");
					all(c, pose, LegacyResources.model_uzibarrel, tex, light);
				}
				
				if(stack.getItem() == LegacyGuns.GUN_UZI_SILENCER.get() || stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE_SILENCER.get()) {
					tex = Library.id("textures/models/model_uzi_silencer.png");
					all(c, pose, LegacyResources.model_uzisilencer, tex, light);
				}
				
			pose.popPose();
			break;
		case EQUIPPED:
			pose.pushPose();

			rot(pose, -200.0F, 0.0F, 0.0F, 1.0F);
			rot(pose, 75.0F, 0.0F, 1.0F, 0.0F);
			rot(pose, -30.0F, 1.0F, 0.0F, 0.0F);
			rot(pose, -5.0F, 0.0F, 0.0F, 1.0F);
			
			pose.scale(0.5F, 0.5F, 0.5F);
			
			pose.translate(0.2F, -0.8F, -1.0F);

			if(stack.getItem() == LegacyGuns.GUN_UZI_LEGACY.get() || stack.getItem() == LegacyGuns.GUN_UZI_SILENCER.get())
				tex = Library.id("textures/models/model_uzi.png");
			if(stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE.get() || stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE_SILENCER.get())
				tex = Library.id("textures/models/model_uzi_saturnite.png");
			all(c, pose, LegacyResources.model_uzi, tex, light);
			
			if(stack.getItem() == LegacyGuns.GUN_UZI_LEGACY.get() || stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE.get()) {
				tex = Library.id("textures/models/model_uzi_barrel.png");
				all(c, pose, LegacyResources.model_uzibarrel, tex, light);
			}
			
			if(stack.getItem() == LegacyGuns.GUN_UZI_SILENCER.get() || stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE_SILENCER.get()) {
				tex = Library.id("textures/models/model_uzi_silencer.png");
				all(c, pose, LegacyResources.model_uzisilencer, tex, light);
			}
			
		pose.popPose();
		break;
		case ENTITY:
			pose.pushPose();

				rot(pose, 180.0F, 0.0F, 0.0F, 1.0F);
				
				pose.scale(0.5F, 0.5F, 0.5F);
				
				pose.translate(0.0F, -1.0F, 0.0F);

				if(stack.getItem() == LegacyGuns.GUN_UZI_LEGACY.get() || stack.getItem() == LegacyGuns.GUN_UZI_SILENCER.get())
					tex = Library.id("textures/models/model_uzi.png");
				if(stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE.get() || stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE_SILENCER.get())
					tex = Library.id("textures/models/model_uzi_saturnite.png");
				all(c, pose, LegacyResources.model_uzi, tex, light);
				
				if(stack.getItem() == LegacyGuns.GUN_UZI_LEGACY.get() || stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE.get()) {
					tex = Library.id("textures/models/model_uzi_barrel.png");
					all(c, pose, LegacyResources.model_uzibarrel, tex, light);
				}
				
				if(stack.getItem() == LegacyGuns.GUN_UZI_SILENCER.get() || stack.getItem() == LegacyGuns.GUN_UZI_SATURNITE_SILENCER.get()) {
					tex = Library.id("textures/models/model_uzi_silencer.png");
					all(c, pose, LegacyResources.model_uzisilencer, tex, light);
				}
				
			pose.popPose();
		default: break;
		}
	
        }
    }
}
