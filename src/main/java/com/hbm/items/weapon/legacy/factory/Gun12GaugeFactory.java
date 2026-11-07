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
import com.hbm.particle.SpentCasing;
import com.hbm.particle.SpentCasing.CasingType;
import com.hbm.potion.HbmPotion;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.item.weapon.legacy.LegacyResources;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun12GaugeFactory, value for value. */
public final class Gun12GaugeFactory {

    private Gun12GaugeFactory() {}

	
	private static final GunConfiguration.Ejector EJECTOR_SPAS, EJECTOR_SPAS_ALT, EJECTOR_BENELLI, EJECTOR_UBOINIK, EJECTOR_SSG;
	private static final SpentCasing CASING12GAUGE;

	static {
		EJECTOR_SPAS = new GunConfiguration.Ejector().setMotion(-0.4, 0.1, 0).setOffset(-0.35, 0, 0.5).setAngleRange(0.01F, 0.03F).setDelay(12);
		EJECTOR_SPAS_ALT = new GunConfiguration.Ejector().setMotion(-0.4, 0.1, 0).setOffset(-0.35, 0, 0.5).setAngleRange(0.01F, 0.03F).setDelay(12).setAmount(2);
		EJECTOR_BENELLI = new GunConfiguration.Ejector().setMotion(-0.4, 0.3, 0).setOffset(-0.3, 0, 0.5).setAngleRange(0.01F, 0.03F);
		EJECTOR_UBOINIK = new GunConfiguration.Ejector().setMotion(-0.4, 0.1, 0).setOffset(-0.35, -0.3, 0.5).setAngleRange(0.01F, 0.03F);
		EJECTOR_SSG = new GunConfiguration.Ejector().setMotion(0.2, 0, -0.2).setOffset(0.8, 0, 0).setAngleRange(0.05F, 0.02F).setDelay(20).setAmount(2);
		
		CASING12GAUGE = new SpentCasing(CasingType.SHOTGUN).setScale(1.5F).setBounceMotion(0.05F, 0.02F).setupSmoke(0.5F, 0.5D, 60, 20);
	}

	public static GunConfiguration getRemington870Config() {
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 25;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 10;
		config.firingDuration = 5;
		config.ammoCap = 5;
		config.durability = 1000;
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.CIRCLE;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		config.firingSound = "hbm:weapon.shotgunPump";
		
		config.name = "remington870";
		config.manufacturer = EnumGunManufacturer.REMINGTON;
		
		config.config = LegacyCollection.g12hs;
		
		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("RECOIL_TRANSLATE", new BusAnimationSequence()
					.addPos(0, 0, -2, 100)
					.addPos(0, 0, 0, 200)
					)
				.addBus("PUMP", new BusAnimationSequence()
					.addPos(0, 0, 0, 450)
					.addPos(0, 0, -1.8, 200)
					.addPos(0, 0, 0, 200)
					)
				);
		
		config.ejector = EJECTOR_SPAS;
		return config;
	}
	
	public static GunConfiguration getSpas12Config() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 20;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 10;
		config.emptyReloadAdditionalDuration = 5;
		config.firingDuration = 5;
		config.ammoCap = 8;
		config.durability = 2500;
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.CIRCLE;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		config.reloadSoundEnd = false;
		config.firingSound = "hbm:weapon.shotgunPump";
		
		config.name = "spas12";
		config.manufacturer = EnumGunManufacturer.BLACK_MESA;
		config.comment.add("\"Here, I have a more suitable gun for you. You'll need it - Catch!\"");
		config.comment.add("Alt-fire with Mouse 2 (Right-click) to fire 2 shells at once");
		
		config.config = LegacyCollection.g12hs;

		config.reloadAnimationsSequential = true;

		config.loadAnimations = () -> {
			config.animations.put(AnimType.CYCLE, LegacyResources.spas_12_anim.get("Fire"));
			config.animations.put(AnimType.ALT_CYCLE, LegacyResources.spas_12_anim.get("FireAlt"));
			config.animations.put(AnimType.RELOAD, LegacyResources.spas_12_anim.get("ReloadStart"));
			config.animations.put(AnimType.RELOAD_EMPTY, LegacyResources.spas_12_anim.get("ReloadEmptyStart"));
			config.animations.put(AnimType.RELOAD_CYCLE, LegacyResources.spas_12_anim.get("Reload"));
			config.animations.put(AnimType.RELOAD_END, LegacyResources.spas_12_anim.get("ReloadEnd"));
		};
		
		config.ejector = EJECTOR_SPAS;
		
		return config;
	}
	
	public static GunConfiguration getSpas12AltConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 35;
		config.roundsPerCycle = 2;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.firingDuration = 10;
		config.ammoCap = 8;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		config.firingSound = "hbm:weapon.shotgunPumpAlt";
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		
		config.config = LegacyCollection.g12hs;
		
		config.ejector = EJECTOR_SPAS_ALT;

		return config;
	}
	
	public static GunConfiguration getUboinikConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 8;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 10;
		config.firingDuration = 0;
		config.ammoCap = 6;
		config.durability = 1500;
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CIRCLE;
		config.reloadSound = GunConfiguration.RSOUND_REVOLVER;
		config.firingSound = "hbm:weapon.shotgunShoot";
		
		config.name = "uboinik";
		config.manufacturer = EnumGunManufacturer.METRO;

		config.config = LegacyCollection.g12hs;
		
		config.ejector = EJECTOR_UBOINIK;
		
		return config;
	}
	
	public static GunConfiguration getShottyConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 30;
		config.roundsPerCycle = 2;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 10;
		config.firingDuration = 0;
		config.ammoCap = 0;
		config.durability = 3000;
		config.reloadType = GunConfiguration.RELOAD_NONE;
		config.allowsInfinity = true;
		config.isCentered = true;
		config.crosshair = Crosshair.L_CIRCLE;
		config.reloadSound = GunConfiguration.RSOUND_REVOLVER;
		config.firingSound = "hbm:weapon.shottyShoot";

		config.loadAnimations = () -> {
			config.animations.put(AnimType.CYCLE, LegacyResources.supershotty_anim.get("Fire"));
		};
		
		config.name = "supershotty";
		config.manufacturer = EnumGunManufacturer.UAC;
		config.comment.add("God-damned ARCH-VILES!");
		
		config.config = LegacyCollection.g12hs;
		
		config.ejector = EJECTOR_SSG;
		
		return config;
	}

	public static GunConfiguration getBenelliConfig() {
		
		GunConfiguration config = getUboinikConfig();

		config.gunMode = 0;
		config.firingMode = 1;
		config.rateOfFire = 5;
		config.ammoCap = 8;
		config.reloadDuration = 8;
		config.crosshair = Crosshair.CIRCLE;
		config.hasSights = true;
		config.durability = 250000;
		config.allowsInfinity = true;
		config.firingSound = "hbm:weapon.deagleShoot";
		config.firingPitch = 0.75F;
		config.reloadType = 2;

		config.name = "benelli";
		config.manufacturer = EnumGunManufacturer.BENELLI;
		config.comment.add("Eat your heart out SPAS-12");
		config.config = LegacyCollection.g12;

		config.ejector = EJECTOR_BENELLI;

		return config;
	}

	public static GunConfiguration getBenelliModConfig() {
		
		GunConfiguration config = getBenelliConfig();

		config.reloadType = 1;
		config.ammoCap = 24;
		config.reloadDuration = 21;
		config.emptyReloadAdditionalDuration = 15;
		config.reloadSound = GunConfiguration.RSOUND_MAG;
		config.reloadSoundEmpty = GunConfiguration.RSOUND_MAG_BOLT;
		config.reloadSoundEnd = false;
		config.name += "Drum";

		config.loadAnimations = () -> {
			config.animations.put(AnimType.CYCLE, LegacyResources.benelli_anim.get("Fire"));
			config.animations.put(AnimType.RELOAD, LegacyResources.benelli_anim.get("Reload"));
			config.animations.put(AnimType.RELOAD_EMPTY, LegacyResources.benelli_anim.get("ReloadEmpty"));
		};

		return config;
	}
	
	public static BulletConfiguration get12GaugeConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = LegacyGuns.AMMO_12GAUGE;
		bullet.dmgMin = 5;
		bullet.dmgMax = 7;
		
		bullet.spentCasing = CASING12GAUGE.clone().register("old12GaStock").setColor(0x2847FF, SpentCasing.COLOR_CASE_12GA);
		
		return bullet;
	}
	
	public static BulletConfiguration get12GaugeFireConfig() {
		
		BulletConfiguration bullet = get12GaugeConfig();

		bullet.ammo = LegacyGuns.AMMO_12GAUGE_INCENDIARY;
		bullet.wear = 15;
		bullet.dmgMin = 5;
		bullet.dmgMax = 7;
		bullet.incendiary = 5;
		
		bullet.spentCasing = CASING12GAUGE.clone().register("old12GaInc").setColor(0xFF6329, SpentCasing.COLOR_CASE_12GA).setupSmoke(1F, 0.5D, 60, 40);
		
		return bullet;
	}
	
	public static BulletConfiguration get12GaugeShrapnelConfig() {
		
		BulletConfiguration bullet = get12GaugeConfig();

		bullet.ammo = LegacyGuns.AMMO_12GAUGE_SHRAPNEL;
		bullet.wear = 15;
		bullet.dmgMin = 10;
		bullet.dmgMax = 17;
		bullet.ricochetAngle = 15;
		bullet.HBRC = 80;
		bullet.LBRC = 95;
		
		bullet.spentCasing = CASING12GAUGE.clone().register("old12GaShrap").setColor(0xF0E800, SpentCasing.COLOR_CASE_12GA);
		
		return bullet;
	}
	
	public static BulletConfiguration get12GaugeDUConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = LegacyGuns.AMMO_12GAUGE_DU;
		bullet.wear = 20;
		bullet.dmgMin = 18;
		bullet.dmgMax = 22;
		bullet.doesPenetrate = true;
		bullet.leadChance = 50;
		
		bullet.spentCasing = CASING12GAUGE.clone().register("old12GaDU").setColor(0x62A362, SpentCasing.COLOR_CASE_12GA);
		
		return bullet;
	}
	
	public static BulletConfiguration get12GaugeAMConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = LegacyGuns.AMMO_12GAUGE_MARAUDER;
		bullet.wear = 20;
		bullet.dmgMin = 100;
		bullet.dmgMax = 500;
		bullet.leadChance = 50;
		
		bullet.bntHurt = (bulletnt, hit) -> {
				
			if(hit instanceof LivingEntity)
				((LivingEntity)hit).addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HbmPotion.bang.get()), 20, 0));
			
		};
		
		bullet.spentCasing = CASING12GAUGE.clone().register("old12GaAM").setColor(0x416645, SpentCasing.COLOR_CASE_12GA);
		
		return bullet;
	}
	
	public static BulletConfiguration get12GaugeSleekConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardAirstrikeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_12GAUGE_SLEEK;
		
		bullet.spentCasing = CASING12GAUGE.clone().register("old12GaIF").setColor(0x2A2A2A, SpentCasing.COLOR_CASE_12GA);
		
		return bullet;
	}
	
	public static BulletConfiguration get12GaugePercussionConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_12GAUGE_PERCUSSION;
		bullet.velocity = 2F;
		bullet.spread = 0F;
		bullet.wear = 10;
		bullet.dmgMin = 30F;
		bullet.dmgMax = 30F;
		bullet.maxAge = 0;
		
		bullet.spentCasing = CASING12GAUGE.clone().register("old12GaPerc").setColor(0x9E1616, SpentCasing.COLOR_CASE_12GA).setupSmoke(1F, 0.5D, 60, 40);

		bullet.bntUpdate = (bulletnt) -> {
			
			if(!bulletnt.level().isClientSide()) {
					
				Vec3 vec = bulletnt.getDeltaMovement();
				double radius = 4;
				double x = bulletnt.getX() + vec.x;
				double y = bulletnt.getY() + vec.y;
				double z = bulletnt.getZ() + vec.z;
				AABB aabb = new AABB(x, y, z, x, y, z).inflate(radius, radius, radius);
				List<Entity> list = bulletnt.level().getEntities(bulletnt.getThrower(), aabb);
				
				for(Entity e : list) {
					DamageSource source = bulletnt.getThrower() instanceof Player player ? bulletnt.level().damageSources().playerAttack(player) : bulletnt.level().damageSources().magic();
					e.hurt(source, 30F);
				}
	
				LegacyFx.plasmaBlast(bulletnt.level(), x, y, z, 0.75F, 0.75F, 0.75F, (float) -bulletnt.getXRot() + 90, (float) bulletnt.getYRot(), 2F);
				
				bulletnt.discard();
			}
		};
		
		return bullet;
	}
}
