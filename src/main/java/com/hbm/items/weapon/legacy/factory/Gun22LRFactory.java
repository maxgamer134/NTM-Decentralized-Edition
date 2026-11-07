// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy.factory;

import static com.hbm.items.weapon.legacy.BulletConfigSyncingUtil.*;

import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.hbm.items.weapon.legacy.*;
import com.hbm.items.weapon.legacy.factory.*;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.lib.ModDamageTypes;
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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun22LRFactory, value for value. */
public final class Gun22LRFactory {

    private Gun22LRFactory() {}

	
	private static final GunConfiguration.Ejector EJECTOR_22LR;
	private static final SpentCasing CASING22LR;

	static {
		EJECTOR_22LR = new GunConfiguration.Ejector().setMotion(-0.4, 0.1, 0).setOffset(-0.35, -0.2, 0.35).setAngleRange(0.01F, 0.03F);
		CASING22LR = new SpentCasing(CasingType.STRAIGHT).setScale(0.8F).setBounceMotion(0.05F, 0.02F).setColor(SpentCasing.COLOR_CASE_BRASS);
	}
	
	public static GunConfiguration getUziConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 1;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.reloadDuration = 20;
		config.firingDuration = 0;
		config.ammoCap = 32;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CROSS;
		config.durability = 3000;
		config.reloadSound = GunConfiguration.RSOUND_MAG;
		config.firingSound = "hbm:weapon.uziShoot";
		config.reloadSoundEnd = false;
		
		config.name = "uzi";
		config.manufacturer = EnumGunManufacturer.IMI;
		config.comment.add("Mom, where are my mittens?");
		
		config.config = LegacyCollection.lr22;
		
		config.ejector = EJECTOR_22LR;
		
		return config;
	}
	
	public static GunConfiguration getSaturniteConfig() {
		
		GunConfiguration config = getUziConfig();
		
		config.durability = 4500;
		
		config.name = "uziSatur";
		config.manufacturer = EnumGunManufacturer.IMI_BIGMT;

		config.config = LegacyCollection.lr22Inc;
		
		return config;
	}

	static float inaccuracy = 5;
	public static BulletConfiguration get22LRConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_22LR;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 6;
		bullet.dmgMax = 8;
		
		bullet.spentCasing = CASING22LR.clone().register("old22LRStock");
		
		return bullet;
	}
	
	public static BulletConfiguration get22LRAPConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_22LR_AP;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 12;
		bullet.dmgMax = 16;
		bullet.leadChance = 10;
		bullet.wear = 15;
		
		bullet.spentCasing = CASING22LR.clone().register("old22LRAP");
		
		return bullet;
	}

}
