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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun45ACPFactory, value for value. */
public final class Gun45ACPFactory {

    private Gun45ACPFactory() {}

	
	private static final SpentCasing CASING45;
	
	static {
		CASING45 = new SpentCasing(CasingType.STRAIGHT).setBounceMotion(0.01F, 0.05F).setScale(1.25F, 1.25F, 1F).setColor(SpentCasing.COLOR_CASE_BRASS).register("old45ACP");
	}

	public static GunConfiguration getThompsonConfig() {

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
		config.durability = 5000;
		config.reloadSound = GunConfiguration.RSOUND_MAG;
		config.firingSound = "hbm:weapon.rifleShoot";
		config.reloadSoundEnd = false;

		config.name = "tommy";
		config.manufacturer = EnumGunManufacturer.AUTO_ORDINANCE;

		config.config = new ArrayList<Integer>();
		config.config.addAll(LegacyCollection.acp45);

		return config;
	}

	public static GunConfiguration getUACPistolConfig() {
		GunConfiguration config = new GunConfiguration();

		config.rateOfFire = 4;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 10;
		config.firingDuration = 8;
		config.ammoCap = 21;
		config.durability = 10000;
		config.reloadType = 1;
		config.allowsInfinity = true;
		config.hasSights = true;
		config.absoluteFOV = true;
		config.zoomFOV = 0.5F;
		config.crosshair = Crosshair.CROSS;
		config.reloadSound = GunConfiguration.RSOUND_MAG;
		config.firingSound = "hbm:weapon.deagleShoot";
		config.reloadSoundEnd = true;

		config.name = "uacPistol";
		config.manufacturer = EnumGunManufacturer.UAC;

		config.config.addAll(LegacyCollection.acp45);

		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("SLIDE", new BusAnimationSequence()
						.addPos(0, 0, 0, 10)// Wait for hammer
						.addPos(0, 0, -3.5, 40)// Slide back
						.addPos(0, 0, 0, 40))// Return
				.addBus("HAMMER", new BusAnimationSequence()
						.addPos(15, 0, 0, 10)
						.addPos(0, 0, 0, 40)));

		return config;
	}

	public static GunConfiguration getUACPistolBurstConfig() {
		GunConfiguration config = getUACPistolConfig();
		config.rateOfFire = 5;
		config.roundsPerBurst = 3;
		config.firingDuration = 2;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_BURST;

		return config;
	}

	public static GunConfiguration getUACSMGConfig() {
		GunConfiguration config = new GunConfiguration();

		config.rateOfFire = 1;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.reloadDuration = 10;
		config.firingDuration = 4;
		config.ammoCap = 40;
		config.durability = 40000;
		config.reloadType = 1;
		config.allowsInfinity = true;
		config.hasSights = true;
		config.crosshair = Crosshair.CROSS;
		config.reloadSound = "hbm:weapon.SMGMagInPB3";
		config.firingSound = "hbm:weapon.SMGFirePB3";
		config.firingPitch = 1.15F;
		config.reloadSoundEnd = true;

		config.name = "uacSMG";
		config.manufacturer = EnumGunManufacturer.UAC;

		config.config.addAll(LegacyCollection.acp45);

		return config;
	}
	
	static float inaccuracy = 5;
	public static BulletConfiguration get45AutoConfig() {
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();

		bullet.ammo = LegacyGuns.AMMO_45;
		bullet.spread *= inaccuracy;
		bullet.dmgMax = 12;
		bullet.dmgMin = 16;
		
		bullet.spentCasing = CASING45;

		return bullet;
	}

	public static BulletConfiguration get45AutoAPConfig() {
		BulletConfiguration bullet = get45AutoConfig();

		bullet.ammo = LegacyGuns.AMMO_45_AP;
		bullet.dmgMax = 18;
		bullet.dmgMin = 26;
		bullet.wear = 15;
		bullet.leadChance = 10;
		
		bullet.spentCasing = CASING45;

		return bullet;
	}

	public static BulletConfiguration get45AutoDUConfig() {
		BulletConfiguration bullet = get45AutoConfig();

		bullet.ammo = LegacyGuns.AMMO_45_DU;
		bullet.dmgMax = 30;
		bullet.dmgMin = 44;
		bullet.wear = 25;
		bullet.leadChance = 50;
		
		bullet.spentCasing = CASING45;

		return bullet;
	}
}
