// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy.factory;

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
		config.config.add(M44_PIP);
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
		config.config.add(M44_BJ);
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
		config.config.add(M44_SILVER);
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
		
		bullet.ammo = LegacyGuns.AMMO_44;
		bullet.dmgMin = 18;
		bullet.dmgMax = 26;
		
		bullet.spentCasing = CASING44.clone().register("old44NoPip");
		
		return bullet;
	}
	
	public static BulletConfiguration getNoPipAPConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_44_AP;
		bullet.dmgMin = 25;
		bullet.dmgMax = 32;
		bullet.wear = 15;
		bullet.leadChance = 10;
		
		bullet.spentCasing = CASING44.clone().register("old44AP");
		
		return bullet;
	}
	
	public static BulletConfiguration getNoPipDUConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_44_DU;
		bullet.dmgMin = 28;
		bullet.dmgMax = 40;
		bullet.wear = 25;
		bullet.leadChance = 50;
		
		bullet.spentCasing = CASING44.clone().register("old44DU");
		
		return bullet;
	}
	
	public static BulletConfiguration getPhosphorusConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_44_PHOSPHORUS;
		bullet.dmgMin = 18;
		bullet.dmgMax = 26;
		bullet.wear = 15;
		bullet.incendiary = 5;
		bullet.doesPenetrate = false;
		
		// main cleared the curative items (milk does not wash it off); a plain effect here
		MobEffectInstance eff = new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HbmPotion.phosphorus.get()), 20 * 20, 0, true, true);
		bullet.effects = new ArrayList<>();
		bullet.effects.add(new MobEffectInstance(eff));
		
		bullet.bntImpact = (bulletnt, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
			
			LegacyFx.vanillaBurst(bulletnt.level(), bulletnt.getX(), bulletnt.getY(), bulletnt.getZ(), ParticleTypes.FLAME, 15, 0.05D);
		};
		
		bullet.spentCasing = CASING44.clone().register("old44Phos");
		
		return bullet;
	}
	
	public static BulletConfiguration getNoPipStarConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_44_STAR;
		bullet.dmgMin = 42;
		bullet.dmgMax = 50;
		bullet.wear = 25;
		bullet.leadChance = 100;
		
		bullet.spentCasing = CASING44.clone().register("old44Star");
		
		return bullet;
	}
	
	public static BulletConfiguration getPipConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_44_PIP;
		bullet.dmgMin = 30;
		bullet.dmgMax = 36;
		bullet.wear = 25;
		bullet.doesPenetrate = false;
		
		bullet.bntHit = (bulletnt, hit) -> {
				
			if(!bulletnt.level().isClientSide()) {
				EntityBoxcar pippo = new EntityBoxcar(ModEntities.BOXCAR.get(), bulletnt.level());
				pippo.setPos(hit.getX(), hit.getY() + 50, hit.getZ());
				
				for(int j = 0; j < 50; j++) {
					LegacyFx.vanillaBurst(bulletnt.level(), pippo.getX() + (bulletnt.level().random.nextDouble() - 0.5) * 4, pippo.getY() + (bulletnt.level().random.nextDouble() - 0.5) * 12, pippo.getZ() + (bulletnt.level().random.nextDouble() - 0.5) * 4, ParticleTypes.LARGE_SMOKE, 1, 0);
				}
				bulletnt.level().addFreshEntity(pippo);
				
				LegacySounds.play(bulletnt.level(), pippo.getX(), pippo.getY() + 50, pippo.getZ(), "hbm:alarm.train_horn", 100F, 1F);
			}
		};
		
		bullet.spentCasing = CASING44.clone().register("old44Pip").setColor(0x532C64);
		
		return bullet;
	}
	
	public static BulletConfiguration getBJConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_44_BJ;
		bullet.dmgMin = 30;
		bullet.dmgMax = 36;
		bullet.wear = 25;
		bullet.doesPenetrate = false;
		
		bullet.bntHit = (bulletnt, hit) -> {
				
			if(!bulletnt.level().isClientSide()) {
				EntityDuchessGambit pippo = new EntityDuchessGambit(ModEntities.DUCHESS_GAMBIT.get(), bulletnt.level());
				pippo.setPos(hit.getX(), hit.getY() + 50, hit.getZ());
				
				for(int j = 0; j < 150; j++) {
					LegacyFx.vanillaBurst(bulletnt.level(), pippo.getX() + (bulletnt.level().random.nextDouble() - 0.5) * 7, pippo.getY() + (bulletnt.level().random.nextDouble() - 0.5) * 8, pippo.getZ() + (bulletnt.level().random.nextDouble() - 0.5) * 18, ParticleTypes.LARGE_SMOKE, 1, 0);
				}
				bulletnt.level().addFreshEntity(pippo);
				
				LegacySounds.play(bulletnt.level(), pippo.getX(), pippo.getY() + 50, pippo.getZ(), "hbm:weapon.boat", 100F, 1F);
			}
		};
		
		bullet.spentCasing = CASING44.clone().register("old44BJ").setColor(0x632B2C);
		
		return bullet;
	}
	
	public static BulletConfiguration getSilverStormConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
		
		bullet.ammo = LegacyGuns.AMMO_44_SILVER;
		bullet.dmgMin = 30;
		bullet.dmgMax = 36;
		bullet.wear = 25;
		bullet.doesPenetrate = false;
		
		bullet.bntHit = (bulletnt, hit) -> {
				
			if(!bulletnt.level().isClientSide()) {
				EntityBuilding pippo = new EntityBuilding(ModEntities.FALLING_BUILDING.get(), bulletnt.level());
				pippo.setPos(hit.getX(), hit.getY() + 50, hit.getZ());
				
				for(int j = 0; j < 150; j++) {
					LegacyFx.vanillaBurst(bulletnt.level(), pippo.getX() + (bulletnt.level().random.nextDouble() - 0.5) * 15, pippo.getY() + (bulletnt.level().random.nextDouble() - 0.5) * 15, pippo.getZ() + (bulletnt.level().random.nextDouble() - 0.5) * 15, ParticleTypes.LARGE_SMOKE, 1, 0);
				}
				bulletnt.level().addFreshEntity(pippo);
				
				LegacySounds.play(bulletnt.level(), pippo.getX(), pippo.getY() + 50, pippo.getZ(), "hbm:block.debris", 100F, 1F);
			}
		};
		
		bullet.spentCasing = CASING44.clone().register("old44Silver").setColor(0x2B5963);
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_44_ROCKET;
		bullet.velocity = 5;
		bullet.explosive = 15F;
		bullet.trail = 1;
		
		bullet.spentCasing = CASING44.clone().register("old44Rocket");
		
		return bullet;
	}

}
