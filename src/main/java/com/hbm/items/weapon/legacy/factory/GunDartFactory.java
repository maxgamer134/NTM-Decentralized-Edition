// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy.factory;

import static com.hbm.items.weapon.legacy.BulletConfigSyncingUtil.*;

import com.hbm.items.weapon.legacy.ItemGunDart;
import com.hbm.extprop.HbmLivingProps;
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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/GunDartFactory, value for value. */
public final class GunDartFactory {

    private GunDartFactory() {}


	public static GunConfiguration getDarterConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 1;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.hasSights = false;
		config.reloadDuration = 20;
		config.firingDuration = 0;
		config.ammoCap = 1;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CROSS;
		config.durability = 1000;
		config.reloadSound = GunConfiguration.RSOUND_GRENADE;
		config.firingSound = "hbm:weapon.dartShoot";
		config.reloadSoundEnd = false;
		config.showAmmo = true;
		
		config.name = "dart";
		config.manufacturer = EnumGunManufacturer.NONE;
		
		config.config = new ArrayList<>();
		config.config.add(NEEDLE_GPS);
		config.config.add(NEEDLE_NUKE);
		
		return config;
	}

	public static GunConfiguration getMymyConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 1;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.hasSights = false;
		config.reloadDuration = 10;
		config.firingDuration = 0;
		config.ammoCap = 1;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.NONE;
		config.durability = 1000;
		config.reloadSound = GunConfiguration.RSOUND_GRENADE;
		config.firingSound = "hbm:weapon.dartShoot";
		config.reloadSoundEnd = false;
		config.showAmmo = true;
		
		config.name = "nerf";
		config.manufacturer = EnumGunManufacturer.HASBRO;
		
		config.config = new ArrayList<>();
		config.config.add(DART_NORMAL);
		config.config.add(NEEDLE_NUKE);
		
		return config;
	}

	public static BulletConfiguration getGPSConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_DART;
		bullet.velocity = 5.0F;
		bullet.spread = 0;
		bullet.dmgMin = 1;
		bullet.dmgMax = 2;
		bullet.doesRicochet = true;
		bullet.doesPenetrate = false;
		bullet.style = bullet.STYLE_FLECHETTE;
		bullet.leadChance = 0;
		
		bullet.effects = new ArrayList<>();
		bullet.effects.add(new MobEffectInstance(MobEffects.WITHER, 60 * 20, 2));
		
		bullet.bntHurt = (bulletnt, hit) -> {

			if(bulletnt.level().isClientSide())
				return;

			if(hit instanceof Player) {

				if(((Player) hit).getInventory().contains(new net.minecraft.world.item.ItemStack(com.hbm.items.ModItems.INGOT_METEORITE_FORGED.get())))
					return;

				if(bulletnt.getThrower() instanceof Player) {

					Player shooter = (Player) bulletnt.getThrower();

					if(shooter.getMainHandItem() != null && shooter.getMainHandItem().getItem() == LegacyGuns.GUN_DARTER.get()) {
						ItemGunDart.writePlayer(shooter.getMainHandItem(), (Player) hit);
						LegacySounds.play(shooter, "random.orb", 1.0F, 1.0F);
					}
				}
			}
		};
		
		return bullet;
	}

	public static BulletConfiguration getNukeConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_DART_NUCLEAR;
		bullet.velocity = 5.0F;
		bullet.spread = 0;
		bullet.dmgMin = 1;
		bullet.dmgMax = 2;
		bullet.doesRicochet = true;
		bullet.doesPenetrate = false;
		bullet.style = bullet.STYLE_FLECHETTE;
		bullet.leadChance = 0;
		
		bullet.bntHurt = (bulletnt, hit) -> {

			if(bulletnt.level().isClientSide())
				return;

			if(hit instanceof LivingEntity) {

				LivingEntity e = (LivingEntity) hit;

				if(HbmLivingProps.getRadiation(e) < 250)
					HbmLivingProps.setRadiation(e, 250);
				if(HbmLivingProps.getData(e).bombTimer <= 0)
					HbmLivingProps.getData(e).bombTimer = com.hbm.main.Polaroid.id() * 60 * 20;
			}
		};
		
		return bullet;
	}

	public static BulletConfiguration getNERFConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_DART_NERF;
		bullet.velocity = 1.0F;
		bullet.gravity = 0.04D;
		bullet.dmgMin = 0;
		bullet.dmgMax = 0;
		bullet.leadChance = 0;
		
		return bullet;
	}
}
