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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun50AEFactory, value for value. */
public final class Gun50AEFactory {

    private Gun50AEFactory() {}

	
	private static final GunConfiguration.Ejector EJECTOR_PISTOL;
	private static final SpentCasing CASING50AE;

	static {
		EJECTOR_PISTOL = new GunConfiguration.Ejector().setMotion(-0.3, 0.7, 0).setOffset(-0.5, 0, 0.5).setAngleRange(0.01F, 0.03F);
		CASING50AE = new SpentCasing(CasingType.STRAIGHT).setScale(1.5F).setBounceMotion(0.01F, 0.03F).setColor(SpentCasing.COLOR_CASE_BRASS).setupSmoke(0.25F, 0.5D, 60, 20);
	}
	
	public static GunConfiguration getBaseConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 10;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 10;
		config.firingDuration = 0;
		config.ammoCap = 7;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CLASSIC;
		config.reloadSound = GunConfiguration.RSOUND_REVOLVER;
		config.firingSound = "hbm:weapon.deagleShoot";
		config.reloadSoundEnd = false;
		
		return config;
	}
	
	public static GunConfiguration getDeagleConfig() {
		
		GunConfiguration config = getBaseConfig();
		
		config.durability = 2500;
		
		config.name = "deagle";
		config.manufacturer = EnumGunManufacturer.MAGNUM_R_IMI;
		
		config.absoluteFOV = true;
		config.zoomFOV = 0.5F;
		
		config.hasSights = true;
		config.config = LegacyCollection.ae50;
		
		config.ejector = EJECTOR_PISTOL;
		
		return config;
	}

	private static float inaccuracy = 0.0005F;
	public static BulletConfiguration get50AEConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50AE;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 28;
		bullet.dmgMax = 32;
		
		bullet.spentCasing = CASING50AE.clone().register("old50AEStock");
		
		return bullet;
	}

	public static BulletConfiguration get50APConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50AE_AP;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 30;
		bullet.dmgMax = 36;
		bullet.leadChance = 10;
		bullet.wear = 15;
		
		bullet.spentCasing = CASING50AE.clone().register("old50AEAP");
		
		return bullet;
	}

	public static BulletConfiguration get50DUConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50AE_DU;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 38;
		bullet.dmgMax = 46;
		bullet.leadChance = 50;
		bullet.wear = 25;
		
		bullet.spentCasing = CASING50AE.clone().register("old50AEDU");
		
		return bullet;
	}

	public static BulletConfiguration get50StarConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50AE_STAR;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 52;
		bullet.dmgMax = 60;
		bullet.leadChance = 100;
		bullet.wear = 25;
		
		bullet.spentCasing = CASING50AE.clone().register("old50AEStar");
		
		return bullet;
	}

}
