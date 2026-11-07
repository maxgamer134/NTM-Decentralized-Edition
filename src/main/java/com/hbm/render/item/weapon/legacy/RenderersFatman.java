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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the Fatman family. */
public final class RenderersFatman {

    private RenderersFatman() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_FATMAN_LEGACY, new ItemRenderFatMan());
        LegacyGunRenderer.register(LegacyGuns.GUN_PROTO, new ItemRenderFatMan());
        LegacyGunRenderer.register(LegacyGuns.GUN_MIRV, new ItemRenderMIRVLauncher());
        LegacyGunRenderer.register(LegacyGuns.GUN_BF, new ItemRenderBFLauncher());
    }

    public static final class ItemRenderFatMan extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		switch (ctx) {
		case FIRST_PERSON:
			pose.pushPose();

				tex = Library.id("textures/models/fatman_launcher.png");
				rot(pose, -150.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(-0.8F, -0.1F, -0.2F);
				all(c, pose, LegacyResources.model_fatman, tex, light);
			pose.popPose();
			break;
		case EQUIPPED:
		case ENTITY:
			pose.pushPose();

				tex = Library.id("textures/models/fatman_launcher.png");
				rot(pose, -200.0F, 0.0F, 0.0F, 1.0F);
				rot(pose, 75.0F, 0.0F, 1.0F, 0.0F);
				rot(pose, -30.0F, 1.0F, 0.0F, 0.0F);
				pose.translate(0.0F, -0.2F, -0.5F);
				rot(pose, -5.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(0.5F, -0.2F, 0.0F);
				pose.scale(1.5F, 1.5F, 1.5F);
				all(c, pose, LegacyResources.model_fatman, tex, light);
			pose.popPose();
		default: break;
		}
	
        }
    }

    public static final class ItemRenderMIRVLauncher extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		switch (ctx) {
		case FIRST_PERSON:
			pose.pushPose();

				tex = Library.id("textures/models/mirv_launcher.png");
				rot(pose, -150.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(-0.8F, -0.1F, -0.2F);
				all(c, pose, LegacyResources.model_mirvlauncher, tex, light);
			pose.popPose();
			break;
		case EQUIPPED:
		case ENTITY:
			pose.pushPose();

				tex = Library.id("textures/models/mirv_launcher.png");
				rot(pose, -200.0F, 0.0F, 0.0F, 1.0F);
				rot(pose, 75.0F, 0.0F, 1.0F, 0.0F);
				rot(pose, -30.0F, 1.0F, 0.0F, 0.0F);
				pose.translate(0.0F, -0.2F, -0.5F);
				rot(pose, -5.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(0.5F, -0.2F, 0.0F);
				pose.scale(1.5F, 1.5F, 1.5F);
				all(c, pose, LegacyResources.model_mirvlauncher, tex, light);
			pose.popPose();
		default: break;
		}
	
        }
    }

    public static final class ItemRenderBFLauncher extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		switch (ctx) {
		case FIRST_PERSON:
			pose.pushPose();

				tex = Library.id("textures/models/bf_launcher.png");
				rot(pose, -150.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(-0.8F, -0.1F, -0.2F);
				all(c, pose, LegacyResources.model_bflauncher, tex, light);
			pose.popPose();
			break;
		case EQUIPPED:
		case ENTITY:
			pose.pushPose();

				tex = Library.id("textures/models/bf_launcher.png");
				rot(pose, -200.0F, 0.0F, 0.0F, 1.0F);
				rot(pose, 75.0F, 0.0F, 1.0F, 0.0F);
				rot(pose, -30.0F, 1.0F, 0.0F, 0.0F);
				pose.translate(0.0F, -0.2F, -0.5F);
				rot(pose, -5.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(0.5F, -0.2F, 0.0F);
				pose.scale(1.5F, 1.5F, 1.5F);
				all(c, pose, LegacyResources.model_bflauncher, tex, light);
			pose.popPose();
		default: break;
		}
	
        }
    }
}
