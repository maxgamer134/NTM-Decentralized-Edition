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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/GunRocketHomingFactory, value for value. */
public final class GunRocketHomingFactory {

    private GunRocketHomingFactory() {}


	public static GunConfiguration getStingerConfig() {

		GunConfiguration config = new GunConfiguration();

		config.rateOfFire = 20;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 20;
		config.firingDuration = 0;
		config.ammoCap = 1;
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_KRUCK;
		config.firingSound = "hbm:weapon.rpgShoot";
		config.reloadSound = GunConfiguration.RSOUND_LAUNCHER;
		config.reloadSoundEnd = false;

		config.name = "stinger";
		config.manufacturer = EnumGunManufacturer.RAYTHEON;
		config.comment.add("Woosh, beep-beep-beep!");

		config.config = new ArrayList<Integer>();
		config.config.add(ROCKET_STINGER);
		config.config.add(ROCKET_STINGER_HE);
		config.config.add(ROCKET_STINGER_INCENDIARY);
		config.config.add(ROCKET_STINGER_NUCLEAR);
		config.config.add(ROCKET_STINGER_BONES);
		config.durability = 250;

		return config;
	}

	public static GunConfiguration getSkyStingerConfig() {
GunConfiguration config = new GunConfiguration();

		config.rateOfFire = 20;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 20;
		config.firingDuration = 0;
		config.ammoCap = 1;
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_KRUCK;
		config.firingSound = "hbm:weapon.rpgShoot";
		config.reloadSound = GunConfiguration.RSOUND_LAUNCHER;
		config.reloadSoundEnd = false;

		config.name = "stingerOneSky";
		config.manufacturer = EnumGunManufacturer.EQUESTRIA;
		config.comment.add("Oh, I get it, because of the...nyeees!");
		config.comment.add("It all makes sense now!");
		config.comment.add("");
		config.comment.add("Rockets travel faster, are Three times stronger");
		config.comment.add("and fires a second rocket for free");
		config.comment.add("");
		config.comment.add("[LEGENDARY WEAPON]");

		config.config = new ArrayList<Integer>();
		config.config.add(ROCKET_STINGER);
		config.config.add(ROCKET_STINGER_HE);
		config.config.add(ROCKET_STINGER_INCENDIARY);
		config.config.add(ROCKET_STINGER_NUCLEAR);
		config.config.add(ROCKET_STINGER_BONES);
		config.durability = 1000;

		return config;
	}

	public static BulletConfiguration getRocketStingerConfig() {
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();

		bullet.ammo = LegacyGuns.AMMO_STINGER_ROCKET;
		bullet.dmgMin = 20;
		bullet.dmgMax = 25;
		bullet.explosive = 4F;
		bullet.trail = 0;

		bullet.bntUpdate = BulletConfigFactory.getHomingBehavior(25, 180);
		return bullet;
	}

	public static BulletConfiguration getRocketStingerHEConfig() {
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();

		bullet.ammo = LegacyGuns.AMMO_STINGER_ROCKET_HE;
		bullet.dmgMin = 30;
		bullet.dmgMax = 35;
		bullet.explosive = 8F;
		bullet.trail = 0;
		bullet.wear = 15;

		bullet.bntUpdate = BulletConfigFactory.getHomingBehavior(25, 180);
		return bullet;
	}

	public static BulletConfiguration getRocketStingerIncendiaryConfig() {
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();

		bullet.ammo = LegacyGuns.AMMO_STINGER_ROCKET_INCENDIARY;
		bullet.dmgMin = 15;
		bullet.dmgMax = 20;
		bullet.explosive = 4F;
		bullet.trail = 0;
		bullet.wear = 12;

		bullet.bntUpdate = BulletConfigFactory.getHomingBehavior(25, 180);
		return bullet;
	}

	public static BulletConfiguration getRocketStingerNuclearConfig() {
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();

		bullet.ammo = LegacyGuns.AMMO_STINGER_ROCKET_NUCLEAR;
		bullet.dmgMin = 50;
		bullet.dmgMax = 55;
		bullet.explosive = 15F;
		bullet.trail = 0;
		bullet.wear = 30;

		bullet.bntUpdate = BulletConfigFactory.getHomingBehavior(25, 180);
		return bullet;
	}

	public static BulletConfiguration getRocketStingerBonesConfig() {
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();

		bullet.ammo = LegacyGuns.AMMO_STINGER_ROCKET_BONES;
		bullet.dmgMin = 20;
		bullet.dmgMax = 25;
		bullet.explosive = 8F;
		bullet.trail = 0;

		bullet.bntUpdate = BulletConfigFactory.getHomingBehavior(25, 180);
		return bullet;
	}
} 