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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun9mmFactory, value for value. */
public final class Gun9mmFactory {

    private Gun9mmFactory() {}

	
	private static final GunConfiguration.Ejector EJECTOR_SMG;
	private static final SpentCasing CASING9MM;

	static {
		EJECTOR_SMG = new GunConfiguration.Ejector().setMotion(-0.3, 0.6, 0).setOffset(-0.35, -0.2, 0.55).setAngleRange(0.01F, 0.03F);
		CASING9MM = new SpentCasing(CasingType.STRAIGHT).setScale(1F, 1F, 0.6F).setBounceMotion(0.01F, 0.03F).setColor(SpentCasing.COLOR_CASE_BRASS);
	}
	
	public static GunConfiguration getMP40Config() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 2;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.reloadDuration = 20;
		config.firingDuration = 0;
		config.ammoCap = 32;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_SPLIT;
		config.durability = 2500;
		config.reloadSound = GunConfiguration.RSOUND_MAG;
		config.firingSound = "hbm:weapon.rifleShoot";
		config.reloadSoundEnd = false;
		
		config.name = "mp40";
		config.manufacturer = EnumGunManufacturer.ERFURT;
		
		config.config = new ArrayList<Integer>();
		config.config.add(P9_NORMAL);
		config.config.add(P9_AP);
		config.config.add(P9_DU);
		config.config.add(CHL_P9);
		config.config.add(P9_ROCKET);
		
		config.ejector = EJECTOR_SMG;
		
		return config;
	}
	
	//rechambered to .45
	/*public static GunConfiguration getThompsonConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 2;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.reloadDuration = 20;
		config.firingDuration = 0;
		config.ammoCap = 30;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_SPLIT;
		config.durability = 2500;
		config.reloadSound = GunConfiguration.RSOUND_MAG;
		config.firingSound = "hbm:weapon.rifleShoot";
		config.reloadSoundEnd = false;
		
		config.name = "tommy9";
		config.manufacturer = EnumGunManufacturer.AUTO_ORDINANCE;
		
		config.config = new ArrayList<Integer>();
		config.config.add(P9_NORMAL);
		config.config.add(P9_AP);
		config.config.add(P9_DU);
		config.config.add(CHL_P9);
		config.config.add(P9_ROCKET);
		
		return config;
	}*/

	static float inaccuracy = 5;
	public static BulletConfiguration get9mmConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_9MM;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 10;
		bullet.dmgMax = 14;
		
		bullet.spentCasing = CASING9MM.clone().register("old9MMStock");
		
		return bullet;
	}
	
	public static BulletConfiguration get9mmAPConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_9MM_AP;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 18;
		bullet.dmgMax = 20;
		bullet.leadChance = 10;
		bullet.wear = 15;
		
		bullet.spentCasing = CASING9MM.clone().register("old9MMAP");
		
		return bullet;
	}
	
	public static BulletConfiguration get9mmDUConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_9MM_DU;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 22;
		bullet.dmgMax = 26;
		bullet.leadChance = 50;
		bullet.wear = 25;
		
		bullet.spentCasing = CASING9MM.clone().register("old9MMDU");
		
		return bullet;
	}
	
	public static BulletConfiguration get9mmRocketConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_9MM_ROCKET;
		bullet.velocity = 5;
		bullet.explosive = 7.5F;
		bullet.trail = 5;
		
		bullet.spentCasing = CASING9MM.clone().register("old9MMRocket");
		
		return bullet;
	}

}
