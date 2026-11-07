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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the 5mm family. */
public final class Renderers5mm {

    private Renderers5mm() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_MINIGUN_LEGACY, new ItemRenderOverkill());
        LegacyGunRenderer.register(LegacyGuns.GUN_LACUNAE, new ItemRenderOverkill());
    }

    public static final class ItemRenderOverkill extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		float f = 0;

		// main read the holder from data[1]; the local player here
		var holder = net.minecraft.client.Minecraft.getInstance().player;
		if(holder != null && holder.getUseItem() == stack)
			f = holder.getTicksUsingItem();
		
		switch (ctx) {
		case FIRST_PERSON:
			pose.pushPose();

				if(is(stack, "gun_jack"))
					tex = Library.id("textures/models/ModelJack.png");
				if(is(stack, "gun_spark"))
					tex = Library.id("textures/models/ModelSpark.png");
				if(is(stack, "gun_hp"))
					tex = Library.id("textures/models/ModelHP.png");
				if(is(stack, "gun_euthanasia"))
					tex = Library.id("textures/models/ModelEuthanasia.png");
				if(is(stack, "gun_defabricator"))
					tex = Library.id("textures/models/ModelDefabricator.png");
				if(is(stack, "gun_dash"))
					tex = Library.id("textures/models/ModelDash.png");
				if(is(stack, "gun_twigun"))
					tex = Library.id("textures/models/ModelTwiGun.png");
				if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get())
					tex = Library.id("textures/models/ModelPip.png");
				if(stack.getItem() == LegacyGuns.GUN_REVOLVER_NOPIP.get())
					tex = Library.id("textures/models/ModelPipNoScope.png");
				if(stack.getItem() == LegacyGuns.GUN_REVOLVER_BLACKJACK.get())
					tex = Library.id("textures/models/ModelPipGrey.png");
				if(stack.getItem() == LegacyGuns.GUN_REVOLVER_SILVER.get())
					tex = Library.id("textures/models/ModelPipSilver.png");
				if(stack.getItem() == LegacyGuns.GUN_REVOLVER_RED.get())
					tex = Library.id("textures/models/ModelPipRed.png");
				if(stack.getItem() == LegacyGuns.GUN_MINIGUN_LEGACY.get())
					tex = Library.id("textures/models/ModelLacunae.png");
				if(stack.getItem() == LegacyGuns.GUN_LACUNAE.get())
					tex = Library.id("textures/models/ModelLacunaeReal.png");
				if(is(stack, "gun_folly_legacy"))
					tex = Library.id("textures/models/ModelFolly.png");

				rot(pose, -135.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(-0.5F, 0.0F, -0.2F);
				//pose.scale(2.0F, 2.0F, 2.0F);
				pose.scale(0.5F, 0.5F, 0.5F);
				//pose.translate(-0.4F, -0.1F, 0.1F);
				pose.translate(-0.2F, -0.1F, -0.1F);
				
				if(is(stack, "gun_defabricator"))
					pose.translate(0, 0.5F, 0.4F);
				
				if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get() ||
						stack.getItem() == LegacyGuns.GUN_REVOLVER_NOPIP.get() ||
						stack.getItem() == LegacyGuns.GUN_REVOLVER_BLACKJACK.get() ||
						stack.getItem() == LegacyGuns.GUN_REVOLVER_SILVER.get() ||
						stack.getItem() == LegacyGuns.GUN_REVOLVER_RED.get()) {
					pose.scale(0.60F, 0.60F, 0.60F);
					pose.translate(0.0F, 0.0F, -0.1F);
					rot(pose, -20.0F, 0.0F, 0.0F, 1.0F);
					rot(pose, 5.0F, 0.0F, 1.0F, 0.0F);
					pose.translate(-0.2F, 0.0F, -0.2F);
				}
				
				if(stack.getItem() == LegacyGuns.GUN_LACUNAE.get() ||
						stack.getItem() == LegacyGuns.GUN_MINIGUN_LEGACY.get()) {
					rot(pose, -15.0F, 0.0F, 0.0F, 1.0F);
					rot(pose, 180, (float) (0), (float) (1), (float) (0));
					pose.translate(0.5F, 0.3F, -0.2F);
				}
				
				if(is(stack, "gun_folly_legacy")) {
					rot(pose, -20.0F, 0.0F, 0.0F, 1.0F);
					rot(pose, 5.0F, 0.0F, 1.0F, 0.0F);
					pose.translate(-0.2F, 0.0F, -0.2F);
				}
				
				if(is(stack, "gun_jack"))
					all(c, pose, LegacyResources.model_jack, tex, light);
				if(is(stack, "gun_spark"))
					all(c, pose, LegacyResources.model_spark, tex, light);
				if(is(stack, "gun_hp"))
					all(c, pose, LegacyResources.model_hp, tex, light);
				if(is(stack, "gun_euthanasia"))
					all(c, pose, LegacyResources.model_euthanasia, tex, light);
				if(is(stack, "gun_defabricator"))
					all(c, pose, LegacyResources.model_defabricator, tex, light);
				if(is(stack, "gun_dash"))
					all(c, pose, LegacyResources.model_dash, tex, light);
				if(is(stack, "gun_twigun"))
					all(c, pose, LegacyResources.model_twigun, tex, light);
				if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get() ||
						stack.getItem() == LegacyGuns.GUN_REVOLVER_NOPIP.get() ||
						stack.getItem() == LegacyGuns.GUN_REVOLVER_BLACKJACK.get() ||
						stack.getItem() == LegacyGuns.GUN_REVOLVER_SILVER.get() ||
						stack.getItem() == LegacyGuns.GUN_REVOLVER_RED.get())
					all(c, pose, LegacyResources.model_pip, tex, light);
				
				if(stack.getItem() == LegacyGuns.GUN_LACUNAE.get() ||
						stack.getItem() == LegacyGuns.GUN_MINIGUN_LEGACY.get())
					all(c, pose, LegacyResources.model_lacunae, tex, light);
				
				if(is(stack, "gun_folly_legacy")) {
					int state = ItemGunBase.readNBT(stack, "state");
					int time = ItemGunBase.readNBT(stack, "timer");
					all(c, pose, LegacyResources.model_folly, tex, light);
				}
				
			pose.popPose();
			break;
		case EQUIPPED:
			pose.pushPose();

			if(is(stack, "gun_jack"))
				tex = Library.id("textures/models/ModelJack.png");
			if(is(stack, "gun_spark"))
				tex = Library.id("textures/models/ModelSpark.png");
			if(is(stack, "gun_hp"))
				tex = Library.id("textures/models/ModelHP.png");
			if(is(stack, "gun_euthanasia"))
				tex = Library.id("textures/models/ModelEuthanasia.png");
			if(is(stack, "gun_defabricator"))
				tex = Library.id("textures/models/ModelDefabricator.png");
			if(is(stack, "gun_dash"))
				tex = Library.id("textures/models/ModelDash.png");
			if(is(stack, "gun_twigun"))
				tex = Library.id("textures/models/ModelTwiGun.png");
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get())
				tex = Library.id("textures/models/ModelPip.png");
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_NOPIP.get())
				tex = Library.id("textures/models/ModelPipNoScope.png");
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_BLACKJACK.get())
				tex = Library.id("textures/models/ModelPipGrey.png");
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_SILVER.get())
				tex = Library.id("textures/models/ModelPipSilver.png");
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_RED.get())
				tex = Library.id("textures/models/ModelPipRed.png");
			if(stack.getItem() == LegacyGuns.GUN_MINIGUN_LEGACY.get())
				tex = Library.id("textures/models/ModelLacunae.png");
			if(stack.getItem() == LegacyGuns.GUN_LACUNAE.get())
				tex = Library.id("textures/models/ModelLacunaeReal.png");
			if(is(stack, "gun_folly_legacy"))
				tex = Library.id("textures/models/ModelFolly.png");
			
			rot(pose, -200.0F, 0.0F, 0.0F, 1.0F);
			rot(pose, 75.0F, 0.0F, 1.0F, 0.0F);
			rot(pose, -30.0F, 1.0F, 0.0F, 0.0F);
			pose.translate(0.0F, -0.2F, -0.5F);
			rot(pose, -5.0F, 0.0F, 0.0F, 1.0F);
			pose.translate(0.5F, -0.2F, 0.0F);
			//pose.scale(0.75F, 0.75F, 0.75F);
			pose.translate(-1.4F, 0.0F, 0.0F);
			if(is(stack, "gun_jack"))
				pose.translate(0.3F, 0, 0);
			if(is(stack, "gun_spark"))
				pose.translate(0.4F, 0, 0);
			if(is(stack, "gun_hp"))
				pose.translate(0.5F, 0.2F, 0);
			if(is(stack, "gun_defabricator"))
				pose.translate(0.5F, 0.6F, -0.2F);

			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_NOPIP.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_BLACKJACK.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_SILVER.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_RED.get()) {
				pose.scale(0.60F, 0.60F, 0.60F);
				pose.translate(0.7F, 0.3F, 0.0F);
			}
			if(stack.getItem() == LegacyGuns.GUN_LACUNAE.get() ||
					stack.getItem() == LegacyGuns.GUN_MINIGUN_LEGACY.get()) {
				rot(pose, 5.0F, 0.0F, 0.0F, 1.0F);
				rot(pose, 185, (float) (0), (float) (1), (float) (0));
				pose.translate(0.5F, 0.6F, 0.2F);
			}
			if(is(stack, "gun_folly_legacy")) {
				pose.translate(0.6F, 0.0F, 0.0F);
			}
			
			if(is(stack, "gun_jack"))
				all(c, pose, LegacyResources.model_jack, tex, light);
			if(is(stack, "gun_spark"))
				all(c, pose, LegacyResources.model_spark, tex, light);
			if(is(stack, "gun_hp"))
				all(c, pose, LegacyResources.model_hp, tex, light);
			if(is(stack, "gun_euthanasia"))
				all(c, pose, LegacyResources.model_euthanasia, tex, light);
			if(is(stack, "gun_defabricator"))
				all(c, pose, LegacyResources.model_defabricator, tex, light);
			if(is(stack, "gun_dash"))
				all(c, pose, LegacyResources.model_dash, tex, light);
			if(is(stack, "gun_twigun"))
				all(c, pose, LegacyResources.model_twigun, tex, light);
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_NOPIP.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_BLACKJACK.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_SILVER.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_RED.get())
				all(c, pose, LegacyResources.model_pip, tex, light);
			
			if(stack.getItem() == LegacyGuns.GUN_LACUNAE.get() ||
					stack.getItem() == LegacyGuns.GUN_MINIGUN_LEGACY.get())
				all(c, pose, LegacyResources.model_lacunae, tex, light);
			
			if(is(stack, "gun_folly_legacy")) {
				int state = ItemGunBase.readNBT(stack, "state");
				int time = ItemGunBase.readNBT(stack, "timer");
				all(c, pose, LegacyResources.model_folly, tex, light);
			}
			pose.popPose();
			break;
		case ENTITY:
			pose.pushPose();

			if(is(stack, "gun_jack"))
				tex = Library.id("textures/models/ModelJack.png");
			if(is(stack, "gun_spark"))
				tex = Library.id("textures/models/ModelSpark.png");
			if(is(stack, "gun_hp"))
				tex = Library.id("textures/models/ModelHP.png");
			if(is(stack, "gun_euthanasia"))
				tex = Library.id("textures/models/ModelEuthanasia.png");
			if(is(stack, "gun_defabricator"))
				tex = Library.id("textures/models/ModelDefabricator.png");
			if(is(stack, "gun_dash"))
				tex = Library.id("textures/models/ModelDash.png");
			if(is(stack, "gun_twigun"))
				tex = Library.id("textures/models/ModelTwiGun.png");
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get())
				tex = Library.id("textures/models/ModelPip.png");
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_NOPIP.get())
				tex = Library.id("textures/models/ModelPipNoScope.png");
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_BLACKJACK.get())
				tex = Library.id("textures/models/ModelPipGrey.png");
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_SILVER.get())
				tex = Library.id("textures/models/ModelPipSilver.png");
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_RED.get())
				tex = Library.id("textures/models/ModelPipRed.png");
			if(stack.getItem() == LegacyGuns.GUN_MINIGUN_LEGACY.get())
				tex = Library.id("textures/models/ModelLacunae.png");
			if(stack.getItem() == LegacyGuns.GUN_LACUNAE.get())
				tex = Library.id("textures/models/ModelLacunaeReal.png");
			if(is(stack, "gun_folly_legacy"))
				tex = Library.id("textures/models/ModelFolly.png");

			
			rot(pose, 180.0F, 1.0F, 0.0F, 0.0F);

			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_NOPIP.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_BLACKJACK.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_SILVER.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_RED.get()) {
				pose.scale(0.60F, 0.60F, 0.60F);
			}
			if(is(stack, "gun_calamity")) {
				pose.scale(0.75F, 0.75F, 0.75F);
			}
			if(stack.getItem() == LegacyGuns.GUN_LACUNAE.get() ||
					stack.getItem() == LegacyGuns.GUN_MINIGUN_LEGACY.get()) {
				pose.translate(0, -1, 0);
				rot(pose, 180.0F, 0.0F, 1.0F, 0.0F);
			}
			
			if(is(stack, "gun_jack"))
				all(c, pose, LegacyResources.model_jack, tex, light);
			if(is(stack, "gun_spark"))
				all(c, pose, LegacyResources.model_spark, tex, light);
			if(is(stack, "gun_hp"))
				all(c, pose, LegacyResources.model_hp, tex, light);
			if(is(stack, "gun_euthanasia"))
				all(c, pose, LegacyResources.model_euthanasia, tex, light);
			if(is(stack, "gun_defabricator"))
				all(c, pose, LegacyResources.model_defabricator, tex, light);
			if(is(stack, "gun_dash"))
				all(c, pose, LegacyResources.model_dash, tex, light);
			if(is(stack, "gun_twigun"))
				all(c, pose, LegacyResources.model_twigun, tex, light);
			if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_NOPIP.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_BLACKJACK.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_SILVER.get() ||
					stack.getItem() == LegacyGuns.GUN_REVOLVER_RED.get())
				all(c, pose, LegacyResources.model_pip, tex, light);
			
			if(stack.getItem() == LegacyGuns.GUN_LACUNAE.get() ||
					stack.getItem() == LegacyGuns.GUN_MINIGUN_LEGACY.get())
				all(c, pose, LegacyResources.model_lacunae, tex, light);
			
			if(is(stack, "gun_folly_legacy")) {
				int state = ItemGunBase.readNBT(stack, "state");
				int time = ItemGunBase.readNBT(stack, "timer");
				all(c, pose, LegacyResources.model_folly, tex, light);
			}
			pose.popPose();
			break;
		default: break;
		}
	
        }
    }
}
