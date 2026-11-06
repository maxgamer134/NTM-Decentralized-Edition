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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/GunGrenadeFactory, value for value. */
public final class GunGrenadeFactory {

    private GunGrenadeFactory() {}


	private static final GunConfiguration.Ejector EJECTOR_LAUNCHER;
	private static final GunConfiguration.Ejector EJECTOR_CONGOLAKE;
	private static final SpentCasing CASING40MM;

	static {
		EJECTOR_LAUNCHER = new GunConfiguration.Ejector().setAngleRange(0.02F, 0.03F).setAfterReload();
		EJECTOR_CONGOLAKE = new GunConfiguration.Ejector().setMotion(0.3, 0.1, 0).setAngleRange(0.02F, 0.03F).setDelay(15);
		CASING40MM = new SpentCasing(CasingType.STRAIGHT).setScale(4F, 4F, 3F).setBounceMotion(0.02F, 0.03F).setColor(SpentCasing.COLOR_CASE_40MM, 0x4B5443).setupSmoke(1F, 0.5D, 60, 40);
	}
	
	public static GunConfiguration getHK69Config() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 30;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.hasSights = true;
		config.zoomFOV = 0.66F;
		config.reloadDuration = 40;
		config.firingDuration = 0;
		config.ammoCap = 1;
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CIRCUMFLEX;
		config.firingSound = "hbm:weapon.hkShoot";
		config.reloadSound = GunConfiguration.RSOUND_GRENADE;
		config.reloadSoundEnd = false;
		
		config.name = "gPistol";
		config.manufacturer = EnumGunManufacturer.H_AND_K;

		config.config = LegacyCollection.grenade;
		config.durability = 300;
		
		config.ejector = EJECTOR_LAUNCHER;
		
		return config;
	}
	
	public static GunConfiguration getCongoConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 24;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 16;
		config.firingDuration = 0;
		config.ammoCap = 4;
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CIRCUMFLEX;
		config.firingSound = "hbm:weapon.glShoot";
		config.reloadSound = GunConfiguration.RSOUND_GRENADE_NEW;
		config.reloadSoundEnd = false;
		
		config.name = "congoLake";
		config.manufacturer = EnumGunManufacturer.NAWS;

		config.config = LegacyCollection.grenade;
		config.durability = 2500;
		
		config.ejector = EJECTOR_CONGOLAKE;

		config.reloadAnimationsSequential = true;

		config.loadAnimations = () -> {
			config.animations.put(AnimType.CYCLE, LegacyResources.congolake_anim.get("Fire"));
			config.animations.put(AnimType.CYCLE_EMPTY, LegacyResources.congolake_anim.get("FireEmpty"));
			config.animations.put(AnimType.RELOAD, LegacyResources.congolake_anim.get("ReloadStart"));
			config.animations.put(AnimType.RELOAD_EMPTY, LegacyResources.congolake_anim.get("ReloadEmpty"));
			config.animations.put(AnimType.RELOAD_CYCLE, LegacyResources.congolake_anim.get("Reload"));
			config.animations.put(AnimType.RELOAD_END, LegacyResources.congolake_anim.get("ReloadEnd"));
		};

		return config;
	}

	public static BulletConfiguration getGrenadeConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE;
		bullet.velocity = 2.0F;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 10;
		bullet.trail = 0;
		
		bullet.spentCasing = CASING40MM.clone().register("old40MMStock");
		
		return bullet;
	}
	
	public static BulletConfiguration getGrenadeHEConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE_HE;
		bullet.velocity = 2.0F;
		bullet.dmgMin = 20;
		bullet.dmgMax = 15;
		bullet.wear = 15;
		bullet.explosive = 5.0F;
		bullet.trail = 1;
		
		bullet.spentCasing = CASING40MM.clone().register("old40MMHE");
		
		return bullet;
	}
	
	public static BulletConfiguration getGrenadeIncendirayConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE_INCENDIARY;
		bullet.velocity = 2.0F;
		bullet.dmgMin = 15;
		bullet.dmgMax = 15;
		bullet.wear = 15;
		bullet.trail = 0;
		bullet.incendiary = 2;
		
		bullet.spentCasing = CASING40MM.clone().register("old40MMInc");
		
		return bullet;
	}
	
	public static BulletConfiguration getGrenadePhosphorusConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE_PHOSPHORUS;
		bullet.velocity = 2.0F;
		bullet.dmgMin = 15;
		bullet.dmgMax = 15;
		bullet.wear = 15;
		bullet.trail = 0;
		bullet.incendiary = 2;
		
		bullet.bntImpact = BulletConfigFactory.getPhosphorousEffect(10, 60 * 20, 100, 0.5D, 1F);
		
		bullet.spentCasing = CASING40MM.clone().register("old40MMPhos");
		
		return bullet;
	}
	
	public static BulletConfiguration getGrenadeChlorineConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE_TOXIC;
		bullet.velocity = 2.0F;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 10;
		bullet.trail = 3;
		bullet.explosive = 0;
		bullet.chlorine = 50;
		
		bullet.spentCasing = CASING40MM.clone().register("old40MMTox");
		
		return bullet;
	}
	
	public static BulletConfiguration getGrenadeSleekConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE_SLEEK;
		bullet.velocity = 2.0F;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 10;
		bullet.trail = 4;
		bullet.explosive = 7.5F;
		bullet.jolt = 6.5D;
		
		bullet.spentCasing = CASING40MM.clone().register("old40MMIF").setColor(SpentCasing.COLOR_CASE_40MM, 0x1C1C1C);
		
		return bullet;
	}
	
	public static BulletConfiguration getGrenadeConcussionConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE_CONCUSSION;
		bullet.velocity = 2.0F;
		bullet.dmgMin = 15;
		bullet.dmgMax = 20;
		bullet.blockDamage = false;
		bullet.explosive = 10.0F;
		bullet.trail = 3;
		
		bullet.spentCasing = CASING40MM.clone().register("old40MMCon").setColor(SpentCasing.COLOR_CASE_40MM, 0x3D5E1D);
		
		return bullet;
	}
	
	public static BulletConfiguration getGrenadeFinnedConfig() {
		
		BulletConfiguration bullet = getGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE_FINNED;
		bullet.gravity = 0.02;
		bullet.explosive = 1.5F;
		bullet.trail = 5;
		
		bullet.spentCasing = CASING40MM.clone().register("old40MMFin").setColor(SpentCasing.COLOR_CASE_40MM, 0x007FDB);
		
		return bullet;
	}
	
	public static BulletConfiguration getGrenadeNuclearConfig() {
		
		BulletConfiguration bullet = getGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE_NUCLEAR;
		bullet.velocity = 4;
		bullet.explosive = 0.0F;
		
		bullet.bntImpact = (bulletnt, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
			BulletConfigFactory.nuclearExplosion(bulletnt, pos, com.hbm.explosion.ExplosionNukeSmall.PARAMS_TOTS);
		};
		
		bullet.spentCasing = CASING40MM.clone().register("old40MMNuke").setColor(SpentCasing.COLOR_CASE_40MM, 0xE2C000);
		
		return bullet;
	}

	public static BulletConfiguration getGrenadeTracerConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE_TRACER;
		bullet.velocity = 2.0F;
		bullet.wear = 10;
		bullet.explosive = 0F;
		bullet.trail = 5;
		bullet.vPFX = "bluedust";
		
		bullet.spentCasing = CASING40MM.clone().register("old40MMTrac").setColor(0xEEEEEE, 0x0075CA);
		
		return bullet;
	}

	public static BulletConfiguration getGrenadeKampfConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE_KAMPF;
		bullet.spread = 0.0F;
		bullet.gravity = 0.0D;
		bullet.wear = 15;
		bullet.explosive = 3.5F;
		bullet.style = BulletConfiguration.STYLE_GRENADE;
		bullet.trail = 4;
		bullet.vPFX = "smoke";
		
		//bullet.spentCasing = CASING40MM.clone().register("old40MMKampf").setColor(0xEBC35E); //does not eject, whole cartridge leaves the gun
		
		return bullet;
	}

	public static BulletConfiguration getGrenadeLeadbursterConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_GRENADE_LEADBURSTER;
		bullet.spread = 0.0F;
		bullet.gravity = 0.01D;
		bullet.explosive = 0F;
		bullet.style = BulletConfiguration.STYLE_LEADBURSTER;
		bullet.doesRicochet = false;
		bullet.doesPenetrate = true;
		bullet.vPFX = "";

		bullet.bntImpact = (bulletnt, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
			
			switch(sideHit) {
			case 0: bulletnt.setXRot((float) (90)); break;
			case 1: bulletnt.setXRot((float) (-90)); break;
			case 2: bulletnt.setXRot(0); bulletnt.setYRot(0); break;
			case 3: bulletnt.setXRot(0); bulletnt.setYRot((float) 180); break;
			case 4: bulletnt.setXRot(0); bulletnt.setYRot(90); break;
			case 5: bulletnt.setXRot(0); bulletnt.setYRot((float) -90); break;
			}
			
			Vec3 vec = new Vec3(0, 0, 1);
			vec = vec.xRot((float) (bulletnt.getXRot() * Math.PI / 180D));
			vec = vec.yRot((float) (bulletnt.getYRot() * Math.PI / 180));

			double offset = 0.1;
			bulletnt.setPos(bulletnt.getX() - (vec.x * offset), bulletnt.getY(), bulletnt.getZ());
			bulletnt.setPos(bulletnt.getX(), bulletnt.getY() - (vec.y * offset), bulletnt.getZ());
			bulletnt.setPos(bulletnt.getX(), bulletnt.getY(), bulletnt.getZ() - (vec.z * offset));
			
			bulletnt.tickCount = 0;
			bulletnt.getStuck(pos, sideHit);
		};
		
		bullet.bntUpdate = (bulletnt) -> {
			if(bulletnt.level().isClientSide()) return;
			
			switch(bulletnt.getStuckIn()) {
			case 0: bulletnt.setXRot((float) (90)); break;
			case 1: bulletnt.setXRot((float) (-90)); break;
			case 2: bulletnt.setXRot(0); bulletnt.setYRot(0); break;
			case 3: bulletnt.setXRot(0); bulletnt.setYRot((float) 180); break;
			case 4: bulletnt.setXRot(0); bulletnt.setYRot(90); break;
			case 5: bulletnt.setXRot(0); bulletnt.setYRot((float) -90); break;
			}
			
			if(bulletnt.ticksInGround < 20) return;
			int timer = bulletnt.ticksInGround - 20;
			
			Vec3 offset = new Vec3(0, 0, -0.5);
			offset = offset.xRot((float) (bulletnt.getXRot() * Math.PI / 180D));
			offset = offset.yRot((float) (bulletnt.getYRot() * Math.PI / 180));
			
			if(bulletnt.tickCount >= 100) {
				bulletnt.discard();
				if (bulletnt.level() instanceof net.minecraft.server.level.ServerLevel sl) sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, bulletnt.getX() + offset.x, bulletnt.getY() + offset.y, bulletnt.getZ() + offset.z, 1, 0, 0, 0, 0);
			}
			
			if(timer > 60) return;
			
			LegacySounds.play(bulletnt.level(), bulletnt.getX(), bulletnt.getY(), bulletnt.getZ(), "hbm:weapon.silencerShoot", 2F, 1F);
			
			for(int i = 0; i < 5; i++) {
				Vec3 vec = new Vec3(0, 1, 0);
				vec = vec.xRot((float) Math.toRadians(11.25 * i));
				vec = vec.zRot((float) -Math.toRadians(13 * timer));
				vec = vec.xRot((float) (bulletnt.getXRot() * Math.PI / 180D));
				vec = vec.yRot((float) (bulletnt.getYRot() * Math.PI / 180));
				
				EntityBulletBaseNT pellet = new EntityBulletBaseNT(bulletnt.level(), R556_NORMAL);
				double dist = 0.5;
				double off = 0.5;
				pellet.setPos(bulletnt.getX() + vec.x * dist + offset.x * off, bulletnt.getY() + vec.y * dist + offset.y * off, bulletnt.getZ() + vec.z * dist + offset.z * off);
				double vel = 0.5;
				pellet.setDeltaMovement(vec.x * vel, pellet.getDeltaMovement().y, pellet.getDeltaMovement().z);
				pellet.setDeltaMovement(pellet.getDeltaMovement().x, vec.y * vel, pellet.getDeltaMovement().z);
				pellet.setDeltaMovement(pellet.getDeltaMovement().x, pellet.getDeltaMovement().y, vec.z * vel);
				
				float hyp = (float) Math.sqrt(pellet.getDeltaMovement().x * pellet.getDeltaMovement().x + pellet.getDeltaMovement().z * pellet.getDeltaMovement().z);
				pellet.setYRot((float) (Math.atan2(pellet.getDeltaMovement().x, pellet.getDeltaMovement().z) * 180.0D / Math.PI));
				pellet.yRotO = pellet.getYRot();
				pellet.setXRot((float) (Math.atan2(pellet.getDeltaMovement().y, (double) hyp) * 180.0D / Math.PI));
				pellet.xRotO = pellet.getXRot();
				
				bulletnt.level().addFreshEntity(pellet);
			}
		};
		
		return bullet;
	}
}
