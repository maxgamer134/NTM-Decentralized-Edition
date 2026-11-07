// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.legacy;

import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.items.weapon.legacy.BulletConfigSyncingUtil;
import com.hbm.items.weapon.legacy.BulletConfiguration;
import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.items.weapon.legacy.ItemGunBase;
import com.hbm.items.weapon.legacy.ItemGunPip;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.hbm.render.util.Vertices;
import com.hbm.items.weapon.legacy.LegacyGuns;
import com.hbm.lib.Library;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the 44 family. */
public final class Renderers44 {

    private Renderers44() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER_PIP, new ItemRenderWeaponLilMac());
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER_NOPIP, new ItemRenderWeaponNovac());
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER_BLACKJACK, new ItemRenderWeaponNovac());
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER_SILVER, new ItemRenderWeaponNovac());
        LegacyGunRenderer.register(LegacyGuns.GUN_REVOLVER_RED, new ItemRenderWeaponNovac());
    }

    public static final class ItemRenderWeaponLilMac extends LegacyGunRenderer {
        private static final ResourceLocation LILMAC_PLUME = Library.id("textures/models/weapons/lilmac_plume.png");
        private static final int LILMAC_SCOPE = LegacyResources.lilmac.partId("Scope");
        private static final int LILMAC_GUN = LegacyResources.lilmac.partId("Gun");
        private static final int LILMAC_PIVOT = LegacyResources.lilmac.partId("Pivot");
        private static final int LILMAC_CYLINDER = LegacyResources.lilmac.partId("Cylinder");
        private static final int LILMAC_BULLETS = LegacyResources.lilmac.partId("Bullets");
        private static final int LILMAC_CASINGS = LegacyResources.lilmac.partId("Casings");
        private static final int LILMAC_HAMMER = LegacyResources.lilmac.partId("Hammer");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		//prevent rendering when using scope
		if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get() && ctx == Ctx.FIRST_PERSON && (Minecraft.getInstance().player != null && com.hbm.items.weapon.legacy.ItemGunBase.isAiming(Minecraft.getInstance().player))) return;
		
		pose.pushPose();
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.1D;
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			pose.translate(1.0, 0.25, -0.25);
			rot(pose, 170, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			double width = 0.5D;
			
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
			double[] reloadLift = HbmAnimations.getRelevantTransformation("RELOAD_LIFT");
			double[] reloadJolt = HbmAnimations.getRelevantTransformation("RELOAD_JOLT");
			double[] equipSpin = HbmAnimations.getRelevantTransformation("ROTATE");
			pose.translate(2, 0, 0);
			rot(pose, equipSpin[0], (float) (0), (float) (0), (float) (1));
			pose.translate(-2, 0, 0);

			pose.pushPose();




			pose.translate(-10, 2.25, 0);
			pose.translate(-recoil[2] * 3.5, -recoil[2] * 1.375, 0);
			
			// main's smoke ribbon: node[0] forward, node[2] side, the width along the side
			if(equipSpin[0] == 0) ItemRenderWeaponBase.renderSmokeNodes(c, pose, ItemGunPip.smokeNodes, 0.5D, light);




			pose.popPose();
			
			pose.translate(0, reloadLift[0] / -22D, 0);
			
			pose.translate(recoil[0], recoil[1], recoil[2]);
			rot(pose, recoil[2] * 10, (float) (0), (float) (0), (float) (1));
			rot(pose, reloadLift[0], (float) (0), (float) (0), (float) (1));
			pose.translate(reloadJolt[0], 0, 0);
			
			double[] reloadTilt = HbmAnimations.getRelevantTransformation("RELAOD_TILT");
			rot(pose, reloadTilt[0], (float) (1), (float) (0), (float) (0));

			tex = LegacyResources.lilmac_scope_tex;
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_SCOPE, light);
			
			tex = LegacyResources.lilmac_tex;
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_GUN, light);

			double[] cylinderFlip = HbmAnimations.getRelevantTransformation("RELOAD_CYLINDER");
			pose.pushPose(); /// DRUM PUSH ///
			rot(pose, cylinderFlip[0], (float) (1), (float) (0), (float) (0));
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_PIVOT, light);
			pose.translate(0, 1.75, 0);
			rot(pose, HbmAnimations.getRelevantTransformation("DRUM")[2] * -60, (float) (1), (float) (0), (float) (0));
			pose.translate(0, -1.75, 0);
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_CYLINDER, light);
			double[] reloadBullets = HbmAnimations.getRelevantTransformation("RELOAD_BULLETS");
			pose.translate(reloadBullets[0], reloadBullets[1], reloadBullets[2]);
			if(HbmAnimations.getRelevantTransformation("RELOAD_BULLETS_CON")[0] != 1)
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_BULLETS, light);
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_CASINGS, light);
			pose.popPose(); /// DRUM POP ///
			
			pose.pushPose(); /// HAMMER ///
			pose.translate(4, 1.25, 0);
			rot(pose, -30 + 30 * HbmAnimations.getRelevantTransformation("HAMMER")[2], (float) (0), (float) (0), (float) (1));
			pose.translate(-4, -1.25, 0);
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_HAMMER, light);
			pose.popPose();

			int flash = 75;
			if(GameTime.now() - ItemGunPip.lastShot < flash) {


				pose.pushPose();
				pose.translate(0.125, 2.25, 0);
				
				double fire = (GameTime.now() - ItemGunPip.lastShot) / (double) flash;
				
				double height = 4 * fire;
				double length = 15 * fire;
				double lift = 3 * fire;
				double offset = 1 * fire;
				double lengthOffset = 0.125;
				final double h = height, l = length, li = lift, o = offset, lo = lengthOffset;
				// main drew the plume additive (SRC_ALPHA, ONE) without lighting
				c.submitCustomGeometry(pose, WeaponRenderTypes.flashLit(LILMAC_PLUME), (p, b) -> {
					double[][] v = {
						{0, -h, -o, 1, 1}, {0, h, -o, 0, 1}, {0, h + li, l - o, 0, 0}, {0, -h + li, l - o, 1, 0},
						{0, h, o, 0, 1}, {0, -h, o, 1, 1}, {0, -h + li, -l + o, 1, 0}, {0, h + li, -l + o, 0, 0},
						{0, -h, -o, 1, 1}, {0, h, -o, 0, 1}, {lo, h, l - o, 0, 0}, {lo, -h, l - o, 1, 0},
						{0, h, o, 0, 1}, {0, -h, o, 1, 1}, {lo, -h, -l + o, 1, 0}, {lo, h, -l + o, 0, 0}};
					for (double[] q : v)
						Vertices.emit(b, p, (float) q[0], (float) q[1], (float) q[2], -1, (float) q[3], (float) q[4], 0xF000F0, 0F, 1F, 0F);
				});
				pose.popPose();

			}
			
			break;
			
		case EQUIPPED:

			double scale = 0.1D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 15F, 0.0F, 0.0F, 1.0F);
			rot(pose, 100, 0.0F, 1.0F, 0.0F);
			rot(pose, 15F, 0.0F, 0.0F, 1.0F);
			pose.translate(-3F, 1F, 4F);
			
			break;
			
		case ENTITY:

			double s1 = 0.075D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			pose.translate(0F, 1F, 0F);
			
			break;
			
		case INVENTORY:



			double s = 0.8D;
			pose.translate(8, 8, 0);
			rot(pose, 180, (float) (0), (float) (1), (float) (0));
			rot(pose, 135, (float) (0), (float) (0), (float) (1));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}

		if(ctx != Ctx.FIRST_PERSON) {

			tex = LegacyResources.lilmac_scope_tex;
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_SCOPE, light);
			tex = LegacyResources.lilmac_tex;
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_GUN, light);
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_CYLINDER, light);
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_BULLETS, light);
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_CASINGS, light);
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_PIVOT, light);
			part(c, pose, LegacyResources.lilmac, tex, LILMAC_HAMMER, light);

		}
		
		pose.popPose();
	
        }
    }

    public static final class ItemRenderWeaponNovac extends LegacyGunRenderer {
        private static final int[] NOVAC_NO_SCOPE = LegacyResources.novac.partIds("Cylinder", "Body", "Hammer", "Trigger");
        private static final int NOVAC_BODY = LegacyResources.novac.partId("Body");
        private static final int NOVAC_CYLINDER = LegacyResources.novac.partId("Cylinder");
        private static final int NOVAC_HAMMER = LegacyResources.novac.partId("Hammer");
        private static final int NOVAC_TRIGGER = LegacyResources.novac.partId("Trigger");
        private static final int NOVAC_SCOPE = LegacyResources.novac.partId("Scope");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		//prevent rendering when using scope
		if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get() && ctx == Ctx.FIRST_PERSON && (Minecraft.getInstance().player != null && com.hbm.items.weapon.legacy.ItemGunBase.isAiming(Minecraft.getInstance().player))) return;
		
		pose.pushPose();

		if(stack.getItem() == LegacyGuns.GUN_REVOLVER_NOPIP.get()) tex = LegacyResources.novac_tex;
		if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get()) tex = LegacyResources.lil_pip_tex;
		if(stack.getItem() == LegacyGuns.GUN_REVOLVER_BLACKJACK.get()) tex = LegacyResources.blackjack_tex;
		if(stack.getItem() == LegacyGuns.GUN_REVOLVER_SILVER.get()) tex = LegacyResources.lent_gun_tex;
		if(stack.getItem() == LegacyGuns.GUN_REVOLVER_RED.get()) tex = LegacyResources.red_key_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.4D;
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			rot(pose, 80, (float) (0), (float) (1), (float) (0));
			rot(pose, 180, 0.0F, 1.0F, 0.0F);
			pose.translate(-0.0F, 0.0F, -1.25F);
			pose.scale((float) (s0), (float) (s0), (float) (s0));

			HbmAnimations.applyRelevantTransformation(pose, "Body");
			part(c, pose, LegacyResources.novac, tex, NOVAC_BODY, light);

			pose.pushPose();
			HbmAnimations.applyRelevantTransformation(pose, "Cylinder");
			part(c, pose, LegacyResources.novac, tex, NOVAC_CYLINDER, light);
			pose.popPose();

			pose.pushPose();
			HbmAnimations.applyRelevantTransformation(pose, "Hammer");
			part(c, pose, LegacyResources.novac, tex, NOVAC_HAMMER, light);
			pose.popPose();

			pose.pushPose();
			HbmAnimations.applyRelevantTransformation(pose, "Trigger");
			part(c, pose, LegacyResources.novac, tex, NOVAC_TRIGGER, light);
			pose.popPose();

			if (stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get()) {
				tex = LegacyResources.novac_scope_tex;
				part(c, pose, LegacyResources.novac, tex, NOVAC_SCOPE, light);
			}

			pose.popPose();
			
			return;
			
		case EQUIPPED:

			double scale = 0.35D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 20F, 0.0F, 0.0F, 1.0F);
			rot(pose, -5, 0.0F, 0.0F, 1.0F);
			rot(pose, 10, 0.0F, 1.0F, 0.0F);
			rot(pose, 15F, 1.0F, 0.0F, 0.0F);
			rot(pose, 180, 0.0F, 1.0F, 0.0F);
			pose.translate(-1.25F, 0.25F, -1F);
			
			break;
			
		case ENTITY:

			double s1 = 0.3D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			
			break;
			
		case INVENTORY:

			double s = 3D;
			pose.translate(7, 8, 0);
			rot(pose, 90, (float) (0), (float) (1), (float) (0));
			rot(pose, 135, (float) (1), (float) (0), (float) (0));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}

		for (int id : NOVAC_NO_SCOPE) part(c, pose, LegacyResources.novac, tex, id, light);

		if(stack.getItem() == LegacyGuns.GUN_REVOLVER_PIP.get()) {
			tex = LegacyResources.novac_scope_tex;
			part(c, pose, LegacyResources.novac, tex, NOVAC_SCOPE, light);
		}

		pose.popPose();
	
        }
    }
}
