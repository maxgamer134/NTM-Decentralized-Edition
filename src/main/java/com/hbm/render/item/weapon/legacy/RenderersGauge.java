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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the Gauge family. */
public final class RenderersGauge {

    private RenderersGauge() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_UBOINIK, new ItemRenderUboinik());
        LegacyGunRenderer.register(LegacyGuns.GUN_REMINGTON, new ItemRenderWeaponRemington());
        LegacyGunRenderer.register(LegacyGuns.GUN_SPAS12_LEGACY, new ItemRenderWeaponSpas12());
        LegacyGunRenderer.register(LegacyGuns.GUN_BENELLI, new ItemRenderBenelli());
        LegacyGunRenderer.register(LegacyGuns.GUN_SUPERSHOTGUN, new ItemRenderWeaponShotty());
        LegacyGunRenderer.register(LegacyGuns.GUN_KS23, new ItemRenderWeaponKS23());
        LegacyGunRenderer.register(LegacyGuns.GUN_SAUER, new ItemRenderWeaponSauer());
        LegacyGunRenderer.register(LegacyGuns.GUN_LEVER_ACTION, new ItemRenderWeaponFFMaresLeg(LegacyResources.ff_gun_bright, LegacyResources.ff_wood));
        LegacyGunRenderer.register(LegacyGuns.GUN_LEVER_ACTION_DARK, new ItemRenderWeaponFFMaresLeg(LegacyResources.ff_gun_normal, LegacyResources.ff_wood_red));
    }

    public static final class ItemRenderWeaponRemington extends LegacyGunRenderer {
        private static final int REMINGTON_GUN = LegacyResources.remington.partId("Gun");
        private static final int REMINGTON_PUMP = LegacyResources.remington.partId("Pump");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();


		tex = LegacyResources.remington_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double[] recoilT = HbmAnimations.getRelevantTransformation("RECOIL_TRANSLATE");
			double[] pump = HbmAnimations.getRelevantTransformation("PUMP");
			
			double s0 = 0.35D;
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			rot(pose, -10, (float) (0), (float) (1), (float) (0));
			pose.translate(1.25, -1.25, -0.25);
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			
			pose.translate(recoilT[2], 0, 0);
			
			part(c, pose, LegacyResources.remington, tex, REMINGTON_GUN, light);
			
			pose.translate(pump[2] * 0.5, 0, 0);
			
			part(c, pose, LegacyResources.remington, tex, REMINGTON_PUMP, light);
			
			break;
			
		case EQUIPPED:

			double scale = 0.25D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 20F, 0.0F, 0.0F, 1.0F);
			rot(pose, -80, 0.0F, 1.0F, 0.0F);
			rot(pose, -15F, 0.0F, 0.0F, 1.0F);
			pose.translate(2.5F, -3.5F, -2F);
			
			all(c, pose, LegacyResources.remington, tex, light);
			
			break;
			
		case ENTITY:

			double s1 = 0.25D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			pose.translate(0, -3, 0);
			
			all(c, pose, LegacyResources.remington, tex, light);
			
			break;
			
		case INVENTORY:

			double s = 1.25D;
			pose.translate(4, 11, 0);
			rot(pose, -135, (float) (0), (float) (0), (float) (1));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			all(c, pose, LegacyResources.remington, tex, light);
			
			break;
			
		default: break;
		}


		pose.popPose();
	
        }
    }

    public static final class ItemRenderWeaponSpas12 extends LegacyGunRenderer {
        private static final int SPAS_12_MAINBODY = LegacyResources.spas_12.partId("MainBody");
        private static final int SPAS_12_PUMPGRIP = LegacyResources.spas_12.partId("PumpGrip");
        private static final int SPAS_12_SHELL = LegacyResources.spas_12.partId("Shell");
        private static final int SPAS_12_SHELLFORE = LegacyResources.spas_12.partId("ShellFore");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();


		tex = LegacyResources.spas_12_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.5D;
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			pose.translate(0, -0.8, 0.1);
			rot(pose, -100, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s0), (float) (s0), (float) (s0));

			HbmAnimations.applyRelevantTransformation(pose, "MainBody");
			part(c, pose, LegacyResources.spas_12, tex, SPAS_12_MAINBODY, light);
			
			pose.pushPose();
			HbmAnimations.applyRelevantTransformation(pose, "PumpGrip");
			part(c, pose, LegacyResources.spas_12, tex, SPAS_12_PUMPGRIP, light);
			pose.popPose();


            pose.pushPose();
            tex = LegacyResources.casings_tex;

            HbmAnimations.applyRelevantTransformation(pose, "Shell");

            ItemGunBase gun = (ItemGunBase)stack.getItem();
            BulletConfiguration bullet = BulletConfigSyncingUtil.pullConfig(gun.mainConfig.config.get(ItemGunBase.getMagType(stack)));
            int[] colors = bullet.spentCasing.getColors();

            

            partColor(c, pose, LegacyResources.spas_12, tex, SPAS_12_SHELL, light, colors[1]);
            
            

            partColor(c, pose, LegacyResources.spas_12, tex, SPAS_12_SHELLFORE, light, colors[0]);

            pose.popPose();
			
			break;
			
		case EQUIPPED:

			double scale = 0.25D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 20F, 0.0F, 0.0F, 1.0F);
			rot(pose, 190, 0.0F, 1.0F, 0.0F);
			rot(pose, -15F, 1.0F, 0.0F, 0.0F);
			pose.translate(-2F, -1F, 2F);
			
			all(c, pose, LegacyResources.spas_12, tex, light);
			
			break;
			
		case ENTITY:

			double s1 = 0.25D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			pose.translate(0, 0, 4);
			
			all(c, pose, LegacyResources.spas_12, tex, light);
			
			break;
			
		case INVENTORY:

			double s = 1.6D;
			pose.translate(12, 13.5, 0);
			rot(pose, -90, (float) (0), (float) (1), (float) (0));
			rot(pose, -135, (float) (1), (float) (0), (float) (0));
			pose.scale((float) (s), (float) (s), (float) (s));
			
			all(c, pose, LegacyResources.spas_12, tex, light);
			
			break;
			
		default: break;
		}
		
		pose.popPose();
	
        }
    }

    public static final class ItemRenderBenelli extends LegacyGunRenderer {
        private static final int BENELLI_BODY = LegacyResources.benelli.partId("Body");
        private static final int BENELLI_BOLT = LegacyResources.benelli.partId("Bolt");
        private static final int BENELLI_DRUM = LegacyResources.benelli.partId("Drum");
        private static final int BENELLI_SHELL = LegacyResources.benelli.partId("Shell");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		Player player = Minecraft.getInstance().player;
		pose.pushPose();

		tex = LegacyResources.benelli_tex;
		final float scale1 = 0.2F;
		final double scale2 = 0.065D;
		final double scale3 = 0.52D;
		
		switch (ctx) {
		case FIRST_PERSON:// In hand from POV
			rot(pose, -90.0F, 0.0F, 1.0F, 0.0F);
			if (com.hbm.items.weapon.legacy.ItemGunBase.isAiming(player)) {
				rot(pose, 25.0F, 1.0F, 0.0F, 0.0F);
				rot(pose, -5F, 0.0F, 1.0F, 0.0F);
				pose.translate(-1.007F, 0F, -2.5F);
			}
			else {
				rot(pose, -10F, 0.0F, 1.0F, 0.0F);
				rot(pose, 20.0F, 1.0F, 0.0F, 0.0F);
				pose.translate(0.0F, -0.5F, -2.5F);
			}

			pose.scale((float) (scale1), (float) (scale1), (float) (scale1));

			HbmAnimations.applyRelevantTransformation(pose, "Body");
			part(c, pose, LegacyResources.benelli, tex, BENELLI_BODY, light);

			pose.pushPose();

			HbmAnimations.applyRelevantTransformation(pose, "Bolt");
			part(c, pose, LegacyResources.benelli, tex, BENELLI_BOLT, light);

			pose.popPose();

			pose.pushPose();

			HbmAnimations.applyRelevantTransformation(pose, "Drum");
			part(c, pose, LegacyResources.benelli, tex, BENELLI_DRUM, light);

			pose.popPose();

			pose.pushPose();

			HbmAnimations.applyRelevantTransformation(pose, "Shell");
			part(c, pose, LegacyResources.benelli, tex, BENELLI_SHELL, light);

			pose.popPose();

			break;
		case EQUIPPED:// In hand from other's POV
			rot(pose, 15F, 0.0F, 0.0F, 1.0F);
			rot(pose, -170, 0.0F, 1.0F, 0.0F);
			rot(pose, -15F, 1.0F, 0.0F, 0.0F);
			pose.translate(-0.4F, 0.05F, -0.5F);
			pose.scale((float) (scale2 - scale2 * 2), (float) (scale2), (float) (scale2));

			pose.pushPose();
			pose.popPose();
			break;
		case ENTITY:// Dropped entity
			pose.scale((float) (0.0625D), (float) (0.0625D), (float) (0.0625D));
			break;
		case INVENTORY:// Inventory icon
			pose.scale((float) (scale3), (float) (scale3), (float) (-scale3));
			pose.translate(14.4F, 15.0F, 0.0F);
			rot(pose, 270.0F, 10.0F, 0.0F, 0.0F);
			rot(pose, 52.5F, 0.0F, 10.0F, 0.0F);
			rot(pose, 270.0F, 0.0F, 0.0F, 10.0F);
		default:
			break;
		}
		
		if(ctx != Ctx.FIRST_PERSON)
			all(c, pose, LegacyResources.benelli, tex, light);
		pose.popPose();

	
        }
    }

    public static final class ItemRenderWeaponShotty extends LegacyGunRenderer {
        private static final int SHOTTY_BODY = LegacyResources.shotty.partId("Body");
        private static final int SHOTTY_BARREL = LegacyResources.shotty.partId("Barrel");
        private static final int SHOTTY_SHELLL = LegacyResources.shotty.partId("ShellL");
        private static final int SHOTTY_SHELLR = LegacyResources.shotty.partId("ShellR");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();


		tex = LegacyResources.shotty_tex;
		
		Player player = Minecraft.getInstance().player;
		
		switch (ctx) {
		
		case FIRST_PERSON:

			String animSuffix = linearAnimations ? "Lame" : "";
			
			pose.scale(0.5F, 0.5F, 0.5F);
			rot(pose, 20F, 0.0F, 0.0F, 1.0F);
			rot(pose, -95F, 0.0F, 1.0F, 0.0F);
			pose.translate(-2.0F, 0.25F, -2.5F);

			HbmAnimations.applyRelevantTransformation(pose, "Body" + animSuffix);
			part(c, pose, LegacyResources.shotty, tex, SHOTTY_BODY, light);

			HbmAnimations.applyRelevantTransformation(pose, "Barrel" + animSuffix);
			part(c, pose, LegacyResources.shotty, tex, SHOTTY_BARREL, light);

			// If we've run out of ammo, stop drawing the shells after ejection has completed
			HbmAnimations.Animation anim = HbmAnimations.getRelevantAnim();
			int millis = anim != null ? (int)(GameTime.now() - anim.startMillis) : 0;

			if(ItemGunBase.getBeltSize(player, ItemGunBase.getBeltType(player, stack, true)) > 0 || millis < 1000) {
				pose.pushPose();
				HbmAnimations.applyRelevantTransformation(pose, "ShellL" + animSuffix);
				part(c, pose, LegacyResources.shotty, tex, SHOTTY_SHELLL, light);
				pose.popPose();
				
				pose.pushPose();
				HbmAnimations.applyRelevantTransformation(pose, "ShellR" + animSuffix);
				part(c, pose, LegacyResources.shotty, tex, SHOTTY_SHELLR, light);
				pose.popPose();
			}
			
			break;
			
		case EQUIPPED:

			rot(pose, -170F, 0.0F, 1.0F, 0.0F);
			rot(pose, -10F, 0.0F, 0.0F, 1.0F);
			rot(pose, -10F, 1.0F, 0.0F, 0.0F);
			rot(pose, 5F, 0.0F, 1.0F, 0.0F);
			pose.translate(-0.4F, 0.0F, -0.5F);
			pose.scale((float) (0.35), (float) (0.35), (float) (0.35));
			part(c, pose, LegacyResources.shotty, tex, SHOTTY_BODY, light);
			part(c, pose, LegacyResources.shotty, tex, SHOTTY_BARREL, light);
			
			break;
			
		case ENTITY:

			pose.scale((float) (0.5), (float) (0.5), (float) (0.5));
			pose.translate(0.0F, 0.2F, 0.0F);
			part(c, pose, LegacyResources.shotty, tex, SHOTTY_BODY, light);
			part(c, pose, LegacyResources.shotty, tex, SHOTTY_BARREL, light);
			break;
			
		default: break;
		}

		pose.popPose();
	
        }
    }

    public static final class ItemRenderWeaponKS23 extends LegacyGunRenderer {
        private static final int KS23_BODY = LegacyResources.ks23.partId("Body");
        private static final int KS23_TRIGGER = LegacyResources.ks23.partId("Trigger");
        private static final int KS23_BOLT = LegacyResources.ks23.partId("Bolt");
        private static final int KS23_GUARD = LegacyResources.ks23.partId("Guard");
        private static final int KS23_SHELL = LegacyResources.ks23.partId("Shell");
        private static final int KS23_SHELLFORE = LegacyResources.ks23.partId("ShellFore");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();

		Player player = Minecraft.getInstance().player;

        tex = LegacyResources.ks23_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
            pose.translate(1.0F, 0.5F, -0.25F);
            rot(pose, 25F, 0.0F, 0.0F, 1.0F);
            rot(pose, -100, 0.0F, 1.0F, 0.0F);
            pose.scale((float) (0.75), (float) (0.75), (float) (0.75));
            
            if(com.hbm.items.weapon.legacy.ItemGunBase.isAiming(player)) {
                rot(pose, 4.5F, 0.0F, 1.0F, 0.0F);
                rot(pose, -2F, 0.0F, 0.0F, 1.0F);
                pose.translate(-0.9F, 0.2F, 0.325F);
            }

            HbmAnimations.applyRelevantTransformation(pose, "Body");

            part(c, pose, LegacyResources.ks23, tex, KS23_BODY, light);
            part(c, pose, LegacyResources.ks23, tex, KS23_TRIGGER, light);

            pose.pushPose();
            HbmAnimations.applyRelevantTransformation(pose, "Bolt");
            part(c, pose, LegacyResources.ks23, tex, KS23_BOLT, light);
            pose.popPose();

            pose.pushPose();
            HbmAnimations.applyRelevantTransformation(pose, "Guard");
            part(c, pose, LegacyResources.ks23, tex, KS23_GUARD, light);
            pose.popPose();


            pose.pushPose();
            tex = LegacyResources.casings_tex;

            HbmAnimations.applyRelevantTransformation(pose, "Shell");

            ItemGunBase gun = (ItemGunBase)stack.getItem();
            BulletConfiguration bullet = BulletConfigSyncingUtil.pullConfig(gun.mainConfig.config.get(ItemGunBase.getMagType(stack)));
            int[] colors = bullet.spentCasing.getColors();

            

            partColor(c, pose, LegacyResources.ks23, tex, KS23_SHELL, light, colors[1]);
            
            

            partColor(c, pose, LegacyResources.ks23, tex, KS23_SHELLFORE, light, colors[0]);

            pose.popPose();


            pose.popPose();
			
			return;
			
		case EQUIPPED:

            rot(pose, 20F, 1.0F, 0.0F, 1.0F);
            rot(pose, -170F, 0.0F, 1.0F, 0.0F);
            pose.translate(-0.5F, 0.0F, -0.9F);
            pose.scale((float) (0.5), (float) (0.5), (float) (0.5));
			
			break;
			
		case ENTITY:
			
            pose.translate(0.3F, 0.2F, 0.0F);
            pose.scale((float) (0.5), (float) (0.5), (float) (0.5));
			
			break;
			
		case INVENTORY:

            pose.translate(7F, 8F, 0.0F);
            pose.scale((float) (4), (float) (4), (float) (-4));
            rot(pose, -90, (float) (0), (float) (1), (float) (0));
            rot(pose, -135F, 1.0F, 0.0F, 0.0F);
			
			break;
			
		default: break;
		}

        all(c, pose, LegacyResources.ks23, tex, light);

		pose.popPose();
	
        }
    }

    public static final class ItemRenderWeaponSauer extends LegacyGunRenderer {
        private static final int SAUERGUN_LEVER = LegacyResources.sauergun.partId("Lever");
        private static final int SAUERGUN_SHELL = LegacyResources.sauergun.partId("Shell");
        private static final int SAUERGUN_GUN = LegacyResources.sauergun.partId("Gun");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();


		tex = LegacyResources.sauergun_tex;
		
		Player player = Minecraft.getInstance().player;
		
		switch (ctx) {
		
		case FIRST_PERSON:

			double[] recoil = HbmAnimations.getRelevantTransformation("SAUER_RECOIL");
			double[] tilt = HbmAnimations.getRelevantTransformation("SAUER_TILT");
			double[] cock = HbmAnimations.getRelevantTransformation("SAUER_COCK");
			double[] eject = HbmAnimations.getRelevantTransformation("SAUER_SHELL_EJECT");
			
			double s0 = 0.5D;
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			
			pose.translate(0.0F, -0.5F, 0.0F);
			rot(pose, -100F, 0.0F, 1.0F, 0.0F);
			rot(pose, 20F, 1.0F, 0.0F, 0.0F);

			if(recoil != null)
				pose.translate(0, 0, recoil[0]);

			if(com.hbm.items.weapon.legacy.ItemGunBase.isAiming(player)) {
				rot(pose, -3F, 0.0F, 0.0F, 1.0F);
				rot(pose, 2F, 0.0F, 1.0F, 0.0F);
				rot(pose, 3F, 1.0F, 0.0F, 0.0F);
				pose.translate(-2.0F, 0.5F, 0.3F);
			}

			if(tilt != null) {
				pose.translate(0, -5, 0);
				rot(pose, tilt[2] * -0.5, (float) (1), (float) (0), (float) (0));
				pose.translate(0, 5, 0);
				rot(pose, tilt[0], (float) (0), (float) (0), (float) (1));

				pose.translate(0, 0, cock[0] * 2);
				part(c, pose, LegacyResources.sauergun, tex, SAUERGUN_LEVER, light);
				pose.translate(0, 0, -cock[0] * 2);

				pose.translate(eject[2] * 10, -eject[2], 0);
				rot(pose, eject[2] * 90, (float) (-1), (float) (0), (float) (0));
				part(c, pose, LegacyResources.sauergun, tex, SAUERGUN_SHELL, light);
				rot(pose, eject[2] * 90, (float) (1), (float) (0), (float) (0));
				pose.translate(-eject[2] * 10, eject[2], 0);
				
			} else {

				part(c, pose, LegacyResources.sauergun, tex, SAUERGUN_LEVER, light);
			}
			
			break;
			
		case EQUIPPED:

			double scale = 0.5D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 20F, 0.0F, 0.0F, 1.0F);
			rot(pose, -170, 0.0F, 1.0F, 0.0F);
			rot(pose, -15F, 1.0F, 0.0F, 0.0F);
			pose.translate(-1F, -0.3F, 0.0F);
			part(c, pose, LegacyResources.sauergun, tex, SAUERGUN_LEVER, light);
			break;
			
		case ENTITY:

			double s1 = 0.25D;
			rot(pose, 90F, 0.0F, 1.0F, 0.0F);
			pose.translate(0.0F, 0.0F, 0.5F);
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			part(c, pose, LegacyResources.sauergun, tex, SAUERGUN_LEVER, light);
			break;
			
		case INVENTORY:

			double s = 2.25D;
			pose.scale((float) (s), (float) (s), (float) (-s));
			pose.translate(4.0F, 4.5F, 0.0F);
			rot(pose, 180F, 1.0F, 0.0F, 0.0F);
			rot(pose, 90F, 0.0F, 1.0F, 0.0F);
			rot(pose, 45F, 1.0F, 0.0F, 0.0F);
			part(c, pose, LegacyResources.sauergun, tex, SAUERGUN_LEVER, light);
			
			break;
			
		default: break;
		}

		part(c, pose, LegacyResources.sauergun, tex, SAUERGUN_GUN, light);

		pose.popPose();
	
        }
    }

    public static final class ItemRenderWeaponFFMaresLeg extends LegacyGunRenderer {
        private static final int FF_MARESLEG_GRIP = LegacyResources.ff_maresleg.partId("Grip");
        private static final int FF_MARESLEG_GUN = LegacyResources.ff_maresleg.partId("Gun");
        private static final int FF_MARESLEG_LEVER = LegacyResources.ff_maresleg.partId("Lever");
        private final ResourceLocation main;
        private final ResourceLocation grip;
        public ItemRenderWeaponFFMaresLeg(ResourceLocation main, ResourceLocation grip) {
            this.main = main;
            this.grip = grip;
        }

        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();

		Player player = Minecraft.getInstance().player;

		double lever = 0;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 1.25D;
			pose.translate(0.75, 0.5, 0);
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			
			if(!com.hbm.items.weapon.legacy.ItemGunBase.isAiming(player)) {
				rot(pose, 100, (float) (0), (float) (-1), (float) (0));
				rot(pose, -5, (float) (0), (float) (1), (float) (0));
				rot(pose, 20, (float) (1), (float) (0), (float) (0));
			} else {
				rot(pose, 97.5, (float) (0), (float) (-1), (float) (0));
				pose.translate(-0.71, 0.375, 0);
				rot(pose, 25, (float) (1), (float) (0), (float) (0));
				rot(pose, 1, (float) (0), (float) (1), (float) (0));
				rot(pose, -3, (float) (0), (float) (0), (float) (1));
				rot(pose, 0.2, (float) (0), (float) (1), (float) (0));
				pose.translate(-0.006, -0.005, 0.4);
			}

			double[] recoil = HbmAnimations.getRelevantTransformation("LEVER_RECOIL");
			pose.translate(0, 0, recoil[0] * 0.5);
			
			double[] rotation = HbmAnimations.getRelevantTransformation("LEVER_ROTATE");
			lever = rotation[2];
			pose.translate(0, 0, 0.5);
			rot(pose, Math.min(lever, 30), (float) (1), (float) (0), (float) (0));
			pose.translate(0, 0, -0.5);
			
			break;
			
		case EQUIPPED:

			double scale = 1.25D;
			rot(pose, 195, (float) (0), (float) (1), (float) (0));
			rot(pose, -10, (float) (0), (float) (0), (float) (1));
			rot(pose, -10, (float) (1), (float) (0), (float) (0));
			pose.translate(-0.45, 0.1, -0.5);
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			
			break;
			
		case ENTITY:

			double s1 = 1D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			rot(pose, 90, (float) (0), (float) (1), (float) (0));
			
			break;
			
		case INVENTORY:

			double s = 9D;
			pose.translate(8, 9, 0);
			rot(pose, -135, (float) (0), (float) (0), (float) (1));
			rot(pose, 90, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}
		
		tex = grip;
		part(c, pose, LegacyResources.ff_maresleg, tex, FF_MARESLEG_GRIP, light);
		tex = main;
		part(c, pose, LegacyResources.ff_maresleg, tex, FF_MARESLEG_GUN, light);
		pose.translate(0, 0.1, 0.25);
		rot(pose, lever * 1.5, (float) (1), (float) (0), (float) (0));
		pose.translate(0, -0.1, -0.25);
		part(c, pose, LegacyResources.ff_maresleg, tex, FF_MARESLEG_LEVER, light);
		
		pose.popPose();
	
        }
    }
    /** main's ItemRenderUboinik: the 1.7.10 ModelUboinik box model, baked to a mesh; the gui keeps the flat icon */
    public static final class ItemRenderUboinik extends LegacyGunRenderer {
        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            switch (ctx) {
                case FIRST_PERSON -> {
                    rot(pose, -135, 0, 0, 1);
                    pose.translate(-0.5F, 0.0F, -0.2F);
                    pose.scale(0.5F, 0.5F, 0.5F);
                    pose.translate(-0.2F, -0.4F, -0.1F);
                    rot(pose, -20, 0, 0, 1);
                    rot(pose, 5, 0, 1, 0);
                    pose.translate(-0.2F, 0.0F, -0.2F);
                }
                case EQUIPPED, ENTITY -> {
                    rot(pose, -200, 0, 0, 1);
                    rot(pose, 75, 0, 1, 0);
                    rot(pose, -30, 1, 0, 0);
                    pose.translate(0.0F, -0.2F, -0.5F);
                    rot(pose, -5, 0, 0, 1);
                    pose.translate(0.5F, -0.2F, 0.0F);
                    pose.scale(0.75F, 0.75F, 0.75F);
                    pose.translate(-1.4F, 0.0F, 0.0F);
                }
                default -> {
                    return;
                }
            }
            all(c, pose, LegacyResources.uboinik, LegacyResources.uboinik_tex, light);
        }
    }
}
