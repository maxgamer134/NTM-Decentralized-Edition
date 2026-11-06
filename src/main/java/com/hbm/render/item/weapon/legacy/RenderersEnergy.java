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

/** backport-fix: BF-161 main 1.0.27_X5000's renderers for the Energy family. */
public final class RenderersEnergy {

    private RenderersEnergy() {}

    public static void init() {
        LegacyGunRenderer.register(LegacyGuns.GUN_COILGUN_LEGACY, new ItemRenderWeaponCoilgun());
        LegacyGunRenderer.register(LegacyGuns.GUN_FLAMER_LEGACY, new Renderers50AE.ItemRenderWeaponObj());
        LegacyGunRenderer.register(LegacyGuns.GUN_CRYOCANNON, new ItemRenderWeaponCryoCannon());
        LegacyGunRenderer.register(LegacyGuns.GUN_FIREEXT_LEGACY, new ItemRenderFireExt());
        LegacyGunRenderer.register(LegacyGuns.GUN_ZOMG, new ItemRenderZOMG());
        LegacyGunRenderer.register(LegacyGuns.GUN_EMP, new ItemRenderEMPRay());
    }

    public static final class ItemRenderWeaponCoilgun extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();


		tex = LegacyResources.coilgun_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.25D;
			rot(pose, 20, (float) (0), (float) (0), (float) (1));
			pose.translate(0.75, 0.3, -0.5);
			rot(pose, -10, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s0), (float) (s0), (float) (s0));

			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
			pose.translate(-1.5 - recoil[0] * 0.5, 0, 0);
			rot(pose, recoil[0] * 45, (float) (0), (float) (0), (float) (1));
			pose.translate(1.5, 0, 0);

			double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
			pose.translate(-2.5, 0, 0);
			rot(pose, reload[0] * -45, (float) (0), (float) (0), (float) (1));
			pose.translate(2.5, 0, 0);
			
			break;
			
		case EQUIPPED:

			double scale = 0.25D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 15F, 0.0F, 0.0F, 1.0F);
			rot(pose, -80, 0.0F, 1.0F, 0.0F);
			rot(pose, -15F, 0.0F, 0.0F, 1.0F);
			pose.translate(1.25F, 0.5F, -1.75F);
			
			break;
			
		case ENTITY:

			double s1 = 0.2D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			
			break;
			
		case INVENTORY:
			
			double s = 3.5D;
			pose.translate(6, 8, 0);
			rot(pose, -135, (float) (0), (float) (0), (float) (1));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}

		all(c, pose, LegacyResources.coilgun, tex, light);

		pose.popPose();
	
        }
    }

    public static final class ItemRenderWeaponCryoCannon extends LegacyGunRenderer {
        // main's ItemRenderWeaponCryoCannon gauges: one texture per fill, turbine and pressure step
        private static final ResourceLocation[] fill_tex = new ResourceLocation[15];
        private static final ResourceLocation[] pressure_tex = new ResourceLocation[12];
        private static final ResourceLocation[] turbine_tex = new ResourceLocation[9];
        static {
            for (int i = 0; i < fill_tex.length; i++) fill_tex[i] = Library.id("textures/models/weapons/cryo_cannon/fill_" + i + ".png");
            for (int i = 0; i < pressure_tex.length; i++) pressure_tex[i] = Library.id("textures/models/weapons/cryo_cannon/pressure_" + i + ".png");
            for (int i = 0; i < turbine_tex.length; i++) turbine_tex[i] = Library.id("textures/models/weapons/cryo_cannon/turbine_" + i + ".png");
        }
        private static final int CRYOCANNON_GUN = LegacyResources.cryocannon.partId("Gun");
        private static final int CRYOCANNON_ROTOR = LegacyResources.cryocannon.partId("Rotor");
        private static final int CRYOCANNON_FUEL = LegacyResources.cryocannon.partId("Fuel");
        private static final int CRYOCANNON_SPIN = LegacyResources.cryocannon.partId("Spin");
        private static final int CRYOCANNON_PRESSURE = LegacyResources.cryocannon.partId("Pressure");


        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();



		tex = LegacyResources.cryocannon_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.25D;
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			pose.translate(1, 0, -0.3);
			rot(pose, 80, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			
			ItemGunBase gun = (ItemGunBase) stack.getItem();

			part(c, pose, LegacyResources.cryocannon, tex, CRYOCANNON_GUN, light);
			part(c, pose, LegacyResources.cryocannon, tex, CRYOCANNON_ROTOR, light);
			tex = fill_tex[net.minecraft.util.Mth.clamp(ItemGunBase.getMag(stack) * fill_tex.length / gun.mainConfig.ammoCap, 0, fill_tex.length - 1)];
			part(c, pose, LegacyResources.cryocannon, tex, CRYOCANNON_FUEL, light);
			tex = turbine_tex[net.minecraft.util.Mth.clamp(turbine_tex.length - 1 - ItemCryoCannon.getTurbine(stack) * turbine_tex.length / 100, 0, turbine_tex.length - 1)];
			part(c, pose, LegacyResources.cryocannon, tex, CRYOCANNON_SPIN, light);
			tex = pressure_tex[net.minecraft.util.Mth.clamp(ItemCryoCannon.getPressure(stack) * pressure_tex.length / 1000, 0, pressure_tex.length - 1)];
			part(c, pose, LegacyResources.cryocannon, tex, CRYOCANNON_PRESSURE, light);
			
			break;
			
		case EQUIPPED:

			double scale = 0.5D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 15F, 0.0F, 0.0F, 1.0F);
			rot(pose, 10F, 0.0F, 1.0F, 0.0F);
			rot(pose, 15F, 1.0F, 0.0F, 0.0F);
			pose.translate(0.75F, -2.5F, 3.5F);
			
			break;
			
		case ENTITY:

			double s1 = 0.25D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			pose.translate(0, 0, 3);
			
			break;
			
		case INVENTORY:
			
			double s = 2.5D;
			pose.translate(1, 6, 0);
			rot(pose, -135, (float) (0), (float) (0), (float) (1));
			rot(pose, -90, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}

		if(ctx != Ctx.FIRST_PERSON) all(c, pose, LegacyResources.cryocannon, tex, light);

		pose.popPose();
	
        }
    }

    public static final class ItemRenderFireExt extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		pose.pushPose();

		int magType = ItemGunBase.getMagType(stack);
		int config = ((ItemGunBase)LegacyGuns.GUN_FIREEXT_LEGACY.get()).mainConfig.config.get(magType);
		// main switched on the ammo meta (0 water, 1 foam, 2 sand) and drew water with the foam skin, foam with the sand one
		net.minecraft.world.item.Item ammo = BulletConfigSyncingUtil.pullConfig(config).ammo.get();
		if(ammo == LegacyGuns.AMMO_FIREEXT_LEGACY.get()) tex = LegacyResources.fireext_foam_tex;
		else if(ammo == LegacyGuns.AMMO_FIREEXT_FOAM_LEGACY.get()) tex = LegacyResources.fireext_sand_tex;
		else tex = LegacyResources.fireext_tex;
		
		switch (ctx) {
		
		case FIRST_PERSON:
			
			double s0 = 0.35D;
			rot(pose, 25, (float) (0), (float) (0), (float) (1));
			pose.translate(0.5, -0.5, -0.5F);
			rot(pose, 80, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s0), (float) (s0), (float) (s0));
			
			break;
			
		case EQUIPPED:

			double scale = 0.5D;
			pose.scale((float) (scale), (float) (scale), (float) (scale));
			rot(pose, 20F, 0.0F, 0.0F, 1.0F);
			rot(pose, -5, 0.0F, 1.0F, 1.0F);
			rot(pose, 10, 0.0F, 1.0F, 0.0F);
			rot(pose, 15F, 1.0F, 0.0F, 0.0F);
			pose.translate(0.75F, -2.75F, 0.5F);
			
			break;
			
		case ENTITY:

			double s1 = 0.3D;
			pose.scale((float) (s1), (float) (s1), (float) (s1));
			
			break;
			
		case INVENTORY:

			double s = 4.5D;
			pose.translate(2, 14, 0);
			rot(pose, -90, (float) (0), (float) (1), (float) (0));
			rot(pose, -135, (float) (1), (float) (0), (float) (0));
			rot(pose, GameTime.now() / 10 % 360, (float) (0), (float) (1), (float) (0));
			pose.scale((float) (s), (float) (s), (float) (-s));
			
			break;
			
		default: break;
		}

		all(c, pose, LegacyResources.fireext, tex, light);

		pose.popPose();
	
        }
    }

    public static final class ItemRenderZOMG extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		switch (ctx) {
		case FIRST_PERSON:
			pose.pushPose();

				tex = Library.id("textures/models/ModelZOMG.png");
				rot(pose, -150.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(-0.5F, 0.0F, -0.2F);
				pose.scale(0.75F, 0.75F, 0.75F);
				pose.translate(-0.4F, -0.5F, -0.1F);
				all(c, pose, LegacyResources.model_zomg, tex, light);
			pose.popPose();
			break;
			
		case EQUIPPED:
			pose.pushPose();

				tex = Library.id("textures/models/ModelZOMG.png");
				rot(pose, -200.0F, 0.0F, 0.0F, 1.0F);
				rot(pose, 75.0F, 0.0F, 1.0F, 0.0F);
				rot(pose, -30.0F, 1.0F, 0.0F, 0.0F);
				pose.translate(0.0F, -0.2F, -0.5F);
				rot(pose, -5.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(0.5F, -0.2F, 0.0F);
				pose.scale(1.5F, 1.5F, 1.5F);
				pose.translate(-0.4F, -0.1F, -0.1F);
				all(c, pose, LegacyResources.model_zomg, tex, light);
			pose.popPose();
			break;
			
		case ENTITY:
			pose.pushPose();

				tex = Library.id("textures/models/ModelZOMG.png");
				rot(pose, 180.0F, 0.0F, 0.0F, 1.0F);
				pose.scale(1.5F, 1.5F, 1.5F);
				pose.translate(0F, -0.5F, 0F);
				all(c, pose, LegacyResources.model_zomg, tex, light);
			pose.popPose();
		default: break;
		}
	
        }
    }

    public static final class ItemRenderEMPRay extends LegacyGunRenderer {



        @Override
        public void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector c, int light) {
            ResourceLocation tex = null;

		
		float f = 0;
		
		switch (ctx) {
		case FIRST_PERSON:
			pose.pushPose();

				tex = Library.id("textures/models/ModelEMPRay.png");
				rot(pose, -135.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(-0.5F, 0.0F, -0.2F);
				pose.scale(0.5F, 0.5F, 0.5F);
				pose.scale(0.5F, 0.5F, 0.5F);
				pose.translate(-0.8F, -0.2F, 0.0F);
				rot(pose, -15.0F, 0.0F, 0.0F, 1.0F);
				all(c, pose, LegacyResources.model_empray, tex, light);
			pose.popPose();
			break;
		case EQUIPPED:
		case ENTITY:
			pose.pushPose();

				tex = Library.id("textures/models/ModelEMPRay.png");
				rot(pose, -200.0F, 0.0F, 0.0F, 1.0F);
				rot(pose, 75.0F, 0.0F, 1.0F, 0.0F);
				rot(pose, -30.0F, 1.0F, 0.0F, 0.0F);
				pose.translate(0.0F, -0.2F, -0.5F);
				rot(pose, -5.0F, 0.0F, 0.0F, 1.0F);
				pose.translate(0.5F, -0.2F, 0.0F);
				pose.scale(0.5F, 0.5F, 0.5F);
				pose.translate(-1.8F, -0.2F, 0.2F);
				all(c, pose, LegacyResources.model_empray, tex, light);
			pose.popPose();
		default: break;
		}
	
        }
    }
}
