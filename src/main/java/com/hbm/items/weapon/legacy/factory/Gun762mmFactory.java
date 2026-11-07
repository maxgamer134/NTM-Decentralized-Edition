// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy.factory;

import static com.hbm.items.weapon.legacy.BulletConfigSyncingUtil.*;

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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun762mmFactory, value for value. */
public final class Gun762mmFactory {

    private Gun762mmFactory() {}

	
	public static final ResourceLocation scope_bolt = Library.id("textures/misc/scope_bolt.png");
	
	private static final GunConfiguration.Ejector EJECTOR_RIFLE;
	private static final GunConfiguration.Ejector EJECTOR_BOLT;
	private static final SpentCasing CASING762NATO;

	static {
		EJECTOR_RIFLE = new GunConfiguration.Ejector().setMotion(-0.35, 0.6, 0).setOffset(-0.35, 0, 0.35).setAngleRange(0.01F, 0.03F);
		EJECTOR_BOLT = new GunConfiguration.Ejector().setMotion(-0.35, 0.6, 0).setOffset(-0.35, 0, 0.35).setAngleRange(0.01F, 0.03F).setDelay(15);
		CASING762NATO = new SpentCasing(CasingType.BOTTLENECK).setScale(1.7F).setBounceMotion(0.01F, 0.05F).setColor(SpentCasing.COLOR_CASE_BRASS);
	}
	
	public static GunConfiguration getCalamityConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 1;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.reloadDuration = 20;
		config.firingDuration = 0;
		config.ammoCap = 50;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.NONE;
		config.durability = 15 * 50 * 10; //15 * capacity * default wear
		config.reloadSound = GunConfiguration.RSOUND_MAG;
		config.firingSound = "hbm:weapon.calShoot";
		config.reloadSoundEnd = false;
		
		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence()
						.addPos(1, 0, 0, 25)
						.addPos(0, 0, 0, 75)
						)
				);
		
		config.animations.put(AnimType.RELOAD, new BusAnimation()
				.addBus("MAG", new BusAnimationSequence()
						.addPos(0, -1, 0, 500)
						.addPos(0, 0, 0, 500)
						)
				);
		
		config.name = "mg3";
		config.manufacturer = EnumGunManufacturer.WGW;
		
		config.config = LegacyCollection.r762;
		
		config.ejector = EJECTOR_RIFLE;
		
		return config;
	}
	
	public static GunConfiguration getUACDMRConfig() {
		final GunConfiguration config = new GunConfiguration();

		config.rateOfFire = 4;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.reloadDuration = 30;
		config.firingDuration = 8;
		config.ammoCap = 30;
		config.durability = 30000;
		config.reloadType = 1;
		config.allowsInfinity = true;
		config.hasSights = true;
		config.crosshair = Crosshair.CROSS;
		config.reloadSound = "hbm:weapon.DMRMagInPB3";
		config.firingSound = "hbm:weapon.DMRShootPB3Alt";
		config.reloadSoundEnd = true;

		config.name = "uacDMR";
		config.manufacturer = EnumGunManufacturer.UAC;

		config.config.addAll(LegacyCollection.r762);
		
		config.ejector = EJECTOR_RIFLE;

		return config;
	}

	public static GunConfiguration getUACCarbineConfig() {
		final GunConfiguration config = getUACDMRConfig();

		config.rateOfFire = 2;
		config.reloadDuration = 20;
		config.ammoCap = 40;
		config.durability = 40000;
		config.crosshair = Crosshair.SPLIT;
		config.reloadSound = "hbm:weapon.carbineMagInPB3";
		config.firingSound = "hbm:weapon.carbineShootPB3";

		config.name = "uacCarbine";

		return config;
	}

	public static GunConfiguration getUACLMGConfig() {
		final GunConfiguration config = getUACCarbineConfig();

		config.ammoCap = 60;
		config.durability = 50000;
		config.crosshair = Crosshair.BOX;
		config.reloadSound = "hbm:weapon.LMGMagInPB3";
		config.firingSound = "hbm:weapon.LMGShootPB3Alt";

		config.name = "uacLMG";

		return config;
	}

	public static GunConfiguration getM60Config() {
		final GunConfiguration config = new GunConfiguration();

		config.rateOfFire = 2;
		config.durability = 10000;
		config.roundsPerCycle = 1;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.reloadType = GunConfiguration.RELOAD_NONE;
		config.ammoCap = 0;
		config.allowsInfinity = true;
		config.hasSights = true;
		config.crosshair = Crosshair.L_BOX;
		config.firingSound = "hbm:weapon.LMGShootPB3";

		config.name = "m60";
		config.manufacturer = EnumGunManufacturer.SACO;
		config.comment.add("\"Get some!\"");
		config.comment.add(" ~ Stuart Brown (aka Ahoy)");
		config.config.addAll(LegacyCollection.r762);
		
		config.ejector = EJECTOR_RIFLE;

		return config;
	}
	
	public static GunConfiguration getBoltConfig() {

		GunConfiguration config = Gun20GaugeFactory.getShotgunConfig();

		config.ammoCap = 5;
		config.durability = 3000;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		config.firingSound = "hbm:weapon.revolverShoot";
		config.firingPitch = 0.75F;
		config.crosshair = Crosshair.CIRCLE;
		
		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence()
						.addPos(1, 0, 0, 25)
						.addPos(0, 0, 0, 75)
						)
				.addBus("LEVER_PULL", new BusAnimationSequence()
						.addPos(0, 0, 0, 375) //wait out recoil and lever flick
						.addPos(-1, 0, 0, 375) //pull back bolt
						.addPos(0, 0, 0, 375) //release bolt
						)
				.addBus("LEVER_ROTATE", new BusAnimationSequence()
						.addPos(0, 0, 0, 250) //wait out recoil
						.addPos(1, 0, 0, 125) //flick up lever in  125ms
						.addPos(1, 0, 0, 750) //pull action
						.addPos(0, 0, 0, 125) //flick down lever again
						)
				);
		
		config.name = "win20Inox";
		config.manufacturer = EnumGunManufacturer.WINCHESTER;
		
		config.ejector = EJECTOR_BOLT;
		
		config.config = LegacyCollection.r762;
		
		return config;
	}
	
	public static GunConfiguration getBoltGreenConfig() {

		GunConfiguration config = Gun20GaugeFactory.getShotgunConfig();

		config.ammoCap = 5;
		config.durability = 2500;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		config.firingSound = "hbm:weapon.revolverShoot";
		config.firingPitch = 0.75F;
		config.crosshair = Crosshair.CIRCLE;
		
		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence()
						.addPos(1, 0, 0, 25)
						.addPos(0, 0, 0, 75)
						)
				.addBus("LEVER_PULL", new BusAnimationSequence()
						.addPos(0, 0, 0, 375) //wait out recoil and lever flick
						.addPos(-1, 0, 0, 375) //pull back bolt
						.addPos(0, 0, 0, 375) //release bolt
						)
				.addBus("LEVER_ROTATE", new BusAnimationSequence()
						.addPos(0, 0, 0, 250) //wait out recoil
						.addPos(1, 0, 0, 125) //flick up lever in  125ms
						.addPos(1, 0, 0, 750) //pull action
						.addPos(0, 0, 0, 125) //flick down lever again
						)
				);
		
		config.name = "win20Poly";
		config.manufacturer = EnumGunManufacturer.WINCHESTER;
		
		config.ejector = EJECTOR_BOLT;
		
		config.config = LegacyCollection.r762;
		
		return config;
	}
	
	public static GunConfiguration getBoltSaturniteConfig() {
		
		GunConfiguration config = Gun20GaugeFactory.getShotgunConfig();
		
		config.ammoCap = 5;
		config.durability = 4000;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		config.firingSound = "hbm:weapon.revolverShoot";
		config.firingPitch = 0.75F;
		config.hasSights = true;
		config.absoluteFOV = true;
		config.zoomFOV = 0.25F;
		config.scopeTexture = scope_bolt;
		config.crosshair = Crosshair.CIRCLE;
		
		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence()
						.addPos(1, 0, 0, 25)
						.addPos(0, 0, 0, 75)
						)
				.addBus("LEVER_PULL", new BusAnimationSequence()
						.addPos(0, 0, 0, 375) //wait out recoil and lever flick
						.addPos(-1, 0, 0, 375) //pull back bolt
						.addPos(0, 0, 0, 375) //release bolt
						)
				.addBus("LEVER_ROTATE", new BusAnimationSequence()
						.addPos(0, 0, 0, 250) //wait out recoil
						.addPos(1, 0, 0, 125) //flick up lever in  125ms
						.addPos(1, 0, 0, 750) //pull action
						.addPos(0, 0, 0, 125) //flick down lever again
						)
				);
		
		config.name = "win20Satur";
		config.manufacturer = EnumGunManufacturer.WINCHESTER_BIGMT;
		
		config.ejector = EJECTOR_BOLT;
		
		config.config = LegacyCollection.r762;
		
		return config;
	}

	public static BulletConfiguration get762NATOConfig() {
		final BulletConfiguration bullet = Gun556mmFactory.get556Config().clone();

		bullet.ammo = LegacyGuns.AMMO_762;
		bullet.dmgMax = 20;
		bullet.dmgMin = 24;
		bullet.velocity *= 2.5;
		bullet.maxAge *= 2;
		bullet.spread /= 2;
		
		bullet.spentCasing = CASING762NATO.clone().register("old762NATOStock");

		return bullet;
	}

	public static BulletConfiguration get762APConfig() {
		final BulletConfiguration bullet = get762NATOConfig();

		bullet.ammo = LegacyGuns.AMMO_762_AP;
		bullet.dmgMax = 24;
		bullet.dmgMin = 28;
		
		bullet.spentCasing = CASING762NATO.clone().register("old762NATOAP");

		return bullet;
	}

	public static BulletConfiguration get762DUConfig() {
		final BulletConfiguration bullet = get762NATOConfig();

		bullet.ammo = LegacyGuns.AMMO_762_DU;
		bullet.dmgMax = 36;
		bullet.dmgMin = 40;
		
		bullet.spentCasing = CASING762NATO.clone().register("old762NATODU");

		return bullet;
	}

	public static BulletConfiguration get762TracerConfig() {
		final BulletConfiguration bullet = get762NATOConfig();

		bullet.ammo = LegacyGuns.AMMO_762_TRACER;
		bullet.vPFX = "reddust";
		
		bullet.spentCasing = CASING762NATO.clone().register("old762NATOTrac");

		return bullet;
	}

	public static BulletConfiguration get762WPConfig() {
		final BulletConfiguration bullet = get762NATOConfig();

		bullet.ammo = LegacyGuns.AMMO_762_PHOSPHORUS;
		bullet.setToFire(20 * 5);
		bullet.vPFX = "reddust";
		final // main cleared the curative items (milk does not wash it off); a plain effect here
		MobEffectInstance eff = new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HbmPotion.phosphorus.get()), 20 * 20, 0, true, true);
		bullet.effects = new ArrayList<>();
		bullet.effects.add(new MobEffectInstance(eff));
		
		bullet.spentCasing = CASING762NATO.clone().register("old762NATOPhos");

		return bullet;
	}

	public static BulletConfiguration get762BlankConfig() {
		final BulletConfiguration bullet = get762NATOConfig();

		bullet.ammo = LegacyGuns.AMMO_762_K;
		bullet.dmgMax = 0;
		bullet.dmgMin = 0;
		bullet.maxAge = 0;
		
		bullet.spentCasing = CASING762NATO.clone().register("old762NATOK");

		return bullet;
	}
}