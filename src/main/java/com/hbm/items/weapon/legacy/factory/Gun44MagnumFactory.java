// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy.factory;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;

import static com.hbm.items.weapon.legacy.BulletConfigSyncingUtil.*;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.*;
import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.hbm.items.weapon.legacy.*;
import com.hbm.items.weapon.legacy.factory.*;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageTypes;
import net.minecraft.resources.ResourceLocation;
import com.hbm.items.weapon.legacy.LegacyExplosion.ExAttrib;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.SpentCasing.CasingType;
import com.hbm.potion.HbmPotion;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.item.weapon.legacy.LegacyResources;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun44MagnumFactory, value for value. */
public final class Gun44MagnumFactory {

    private Gun44MagnumFactory() {}

	
	private static final GunConfiguration.Ejector EJECTOR_PIP;
	private static final SpentCasing CASING44;

	static {
		EJECTOR_PIP = new GunConfiguration.Ejector().setMotion(0, 0, -0.05).setOffset(0, -0.15, 0).setAngleRange(0.01F, 0.05F).setAfterReload().setAmount(6);
		CASING44 = new SpentCasing(CasingType.STRAIGHT).setScale(1.5F, 1.0F, 1.5F).setBounceMotion(0.01F, 0.05F).setColor(SpentCasing.COLOR_CASE_44);
	}
	
	public static GunConfiguration getBaseConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 10;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 50;
		config.firingDuration = 0;
		config.ammoCap = 6;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CLASSIC;
		config.reloadSound = GunConfiguration.RSOUND_REVOLVER;
		config.firingSound = "hbm:weapon.44Shoot";
		config.reloadSoundEnd = false;
		
		config.config.addAll(LegacyCollection.m44Normal);
		
		config.ejector = EJECTOR_PIP;

		config.loadAnimations = () -> {
			config.animations.put(AnimType.CYCLE, LegacyResources.novac_anim.get("Fire"));
		};
		
		return config;
	}
	
	public static GunConfiguration getNovacConfig() {
		
		GunConfiguration config = getBaseConfig();
		
		config.durability = 2500;
		
		config.name = "ifHorseshoe";
		config.manufacturer = EnumGunManufacturer.IF;
		config.comment.add("Fallout New Vegas wasn't THAT good.");
		
		return config;
	}
	
	public static final ResourceLocation scope_lilmac = Library.id("textures/misc/scope_44.png");
	
	public static GunConfiguration getMacintoshConfig() {
		
		GunConfiguration config = getBaseConfig();
		
		config.durability = 31_000;
		
		config.name = "ifScope";
		config.manufacturer = EnumGunManufacturer.IF;
		config.comment.add("Poppin' mentats like tic tacs");
		
		config.allowsInfinity = false;
		config.hasSights = true;
		config.absoluteFOV = true;
		config.zoomFOV = 0.25F;
		config.scopeTexture = scope_lilmac;
		
		config.config = new ArrayList<Integer>();
		config.config.addAll(LegacyCollection.m44Normal);
		
		return config;
	}
	
	public static GunConfiguration getBlackjackConfig() {
		
		GunConfiguration config = getBaseConfig();
		
		config.durability = 4000;
		config.ammoCap = 5;

		config.allowsInfinity = false;
		config.name = "ifVanity";
		config.manufacturer = EnumGunManufacturer.IF;
		config.comment.add("Alcoholism is cool!");
		
		config.config = new ArrayList<Integer>();
		config.config.addAll(LegacyCollection.m44Normal);
		
		config.ejector = EJECTOR_PIP.copy().setAmount(5);
		
		return config;
	}
	
	public static GunConfiguration getSilverConfig() {
		
		GunConfiguration config = getBaseConfig();
		
		config.durability = 4000;
		config.ammoCap = 6;

		config.allowsInfinity = false;
		config.name = "ifStorm";
		config.manufacturer = EnumGunManufacturer.IF;
		config.comment.add("Our friendship is based on abusive behaviour");
		config.comment.add("and mutual hate. It's not that complicated.");
		
		config.config = new ArrayList<Integer>();
		config.config.addAll(LegacyCollection.m44Normal);
		
		return config;
	}
	
	public static GunConfiguration getRedConfig() {
		
		GunConfiguration config = getBaseConfig();
		
		config.durability = 4000;
		config.ammoCap = 8;

		config.allowsInfinity = false;
		config.name = "ifPit";
		config.manufacturer = EnumGunManufacturer.IF;
		config.comment.add("Explore the other side");
		config.comment.add("...from afar!");
		
		config.config = new ArrayList<Integer>();
		config.config.addAll(LegacyCollection.m44All);
		
		config.ejector = EJECTOR_PIP.copy().setAmount(64);
		
		return config;
	}
	
	public static BulletConfiguration getNoPipConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		// the old .44 rounds are gone: post-rework .44 feeds it
		bullet.ammo = () -> ModItems.AMMO_STANDARD.get(EnumAmmo.M44_FMJ);
		bullet.alsoAccepts = new EnumAmmo[] {EnumAmmo.M44_FMJ, EnumAmmo.M44_SP, EnumAmmo.M44_BP, EnumAmmo.M44_JHP, EnumAmmo.M44_EXPRESS};
		bullet.dmgMin = 18;
		bullet.dmgMax = 26;
		
		bullet.spentCasing = CASING44.clone().register("old44NoPip");
		
		return bullet;
	}
	
	public static BulletConfiguration getNoPipAPConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = () -> ModItems.AMMO_STANDARD.get(EnumAmmo.M44_AP);
		bullet.alsoAccepts = new EnumAmmo[] {EnumAmmo.M44_AP};
		bullet.dmgMin = 25;
		bullet.dmgMax = 32;
		bullet.wear = 15;
		bullet.leadChance = 10;
		
		bullet.spentCasing = CASING44.clone().register("old44AP");
		
		return bullet;
	}
	
	
	
	
	
	
	

}
