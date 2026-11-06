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
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.items.weapon.legacy.LegacyExplosion.ExAttrib;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun4GaugeFactory, value for value. */
public final class Gun4GaugeFactory {

    private Gun4GaugeFactory() {}

	
	private static final GunConfiguration.Ejector EJECTOR_SHOTGUN;
	private static final SpentCasing CASING4GAUGE;

	static {
		EJECTOR_SHOTGUN = new GunConfiguration.Ejector().setMotion(-0.4, 0.4, 0).setOffset(-0.5, 0, 0.5).setAngleRange(0.01F, 0.03F);
		CASING4GAUGE = new SpentCasing(CasingType.SHOTGUN).setScale(2.5F).setBounceMotion(0.01F, 0.03F);
	}
	
	private static GunConfiguration getShotgunConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 15;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 10;
		config.firingDuration = 0;
		config.ammoCap = 4;
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		config.allowsInfinity = true;
		config.hasSights = true;
		config.absoluteFOV = true;
		config.zoomFOV = 0.5F;
		config.crosshair = Crosshair.L_CIRCLE;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		
		config.ejector = EJECTOR_SHOTGUN;
		
		return config;
	}
	
	public static GunConfiguration getKS23Config() {
		
		GunConfiguration config = getShotgunConfig();
		
		config.durability = 3000;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		config.reloadSoundEnd = false;
		config.firingSound = "hbm:weapon.revolverShootAlt";
		config.firingPitch = 0.65F;
		
		config.ejector = EJECTOR_SHOTGUN.copy().setDelay(10);
		
		config.name = "ks23";
		config.manufacturer = EnumGunManufacturer.TULSKY;

		config.emptyReloadAdditionalDuration = 5;

		config.reloadAnimationsSequential = true;

		config.loadAnimations = () -> {
			config.animations.put(AnimType.CYCLE, LegacyResources.ks23_anim.get("Fire"));
			config.animations.put(AnimType.RELOAD, LegacyResources.ks23_anim.get("ReloadStart"));
			config.animations.put(AnimType.RELOAD_EMPTY, LegacyResources.ks23_anim.get("ReloadEmptyStart"));
			config.animations.put(AnimType.RELOAD_CYCLE, LegacyResources.ks23_anim.get("Reload"));
			config.animations.put(AnimType.RELOAD_END, LegacyResources.ks23_anim.get("ReloadEnd"));
		};

		config.config = LegacyCollection.g4;
		
		return config;
	}
	
	public static GunConfiguration getSauerConfig() {
		
		GunConfiguration config = getShotgunConfig();

		config.rateOfFire = 20;
		config.ammoCap = 0;
		config.reloadType = GunConfiguration.RELOAD_NONE;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.durability = 3000;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		config.firingSound = "hbm:weapon.sauergun";
		config.firingPitch = 1.0F;
		
		config.ejector = EJECTOR_SHOTGUN.copy().setDelay(12);
		
		config.name = "sauer";
		config.manufacturer = EnumGunManufacturer.CUBE;
		
		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("SAUER_RECOIL", new BusAnimationSequence()
						.addPos(0.5, 0, 0, 50)
						.addPos(0, 0, 0, 50)
						)
				.addBus("SAUER_TILT", new BusAnimationSequence()
						.addPos(0.0, 0, 0, 200)	// do nothing for 200ms
						.addPos(0, 0, 30, 150)	//tilt forward
						.addPos(45, 0, 30, 150)	//tilt sideways
						.addPos(45, 0, 30, 200)	//do nothing for 200ms (eject)
						.addPos(0, 0, 30, 150)	//restore sideways
						.addPos(0, 0, 0, 150)	//restore forward
						)
				.addBus("SAUER_COCK", new BusAnimationSequence()
						.addPos(0, 0, 0, 500)	//do nothing for 500ms
						.addPos(1, 0, 0, 100)	//pull back lever for 100ms
						.addPos(0, 0, 0, 100)	//release lever for 100ms
						)
				.addBus("SAUER_SHELL_EJECT", new BusAnimationSequence()
						.addPos(0, 0, 0, 500)	//do nothing for 500ms
						.addPos(0, 0, 1, 500)	//FLING!
						)
				);
		
		config.config = LegacyCollection.g4;
		
		return config;
	}
	
	public static BulletConfiguration get4GaugeConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE;
		bullet.dmgMin = 5;
		bullet.dmgMax = 8;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaStock").setColor(0xFFD800, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}
	
	public static BulletConfiguration get4GaugeSlugConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_SLUG;
		bullet.dmgMin = 25;
		bullet.dmgMax = 32;
		bullet.wear = 7;
		bullet.style = BulletConfiguration.STYLE_NORMAL;
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaSlug").setColor(0xE01A1A, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeFlechetteConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_FLECHETTE;
		bullet.dmgMin = 8;
		bullet.dmgMax = 15;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		bullet.wear = 15;
		bullet.style = BulletConfiguration.STYLE_FLECHETTE;
		bullet.HBRC = 2;
		bullet.LBRC = 95;
		BulletConfigFactory.makeFlechette(bullet);
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaFlech").setColor(0x1537FF, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeFlechettePhosphorusConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_FLECHETTE_PHOSPHORUS;
		bullet.dmgMin = 8;
		bullet.dmgMax = 15;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		bullet.wear = 15;
		bullet.style = BulletConfiguration.STYLE_FLECHETTE;
		bullet.HBRC = 2;
		bullet.LBRC = 95;
		bullet.incendiary = 5;
		
		// main cleared the curative items (milk does not wash it off); a plain effect here
		MobEffectInstance eff = new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HbmPotion.phosphorus.get()), 20 * 20, 0, true, true);
		bullet.effects = new ArrayList<>();
		bullet.effects.add(new MobEffectInstance(eff));
		
		bullet.bntImpact = (bulletnt, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
			
			LegacyFx.vanillaBurst(bulletnt.level(), bulletnt.getX(), bulletnt.getY(), bulletnt.getZ(), ParticleTypes.FLAME, 15, 0.05D);
		};
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaPhos").setColor(0xF6871A, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeExplosiveConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_EXPLOSIVE;
		bullet.velocity *= 2;
		bullet.gravity *= 2;
		bullet.dmgMin = 20;
		bullet.dmgMax = 25;
		bullet.wear = 25;
		bullet.trail = 1;
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaExp").setColor(0x3F8243, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeMiningConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_SEMTEX;
		bullet.velocity *= 2;
		bullet.gravity *= 2;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 25;
		bullet.trail = 1;
		bullet.explosive = 0.0F;
		
		bullet.bntImpact = (bulletnt, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
			
			if(bulletnt.level().isClientSide())
				return;
			
			LegacyExplosion.explode(bulletnt.level(), null, bulletnt.getX(), bulletnt.getY(), bulletnt.getZ(), 4, ExAttrib.ALLDROP, ExAttrib.NOHURT);
			
			ExplosionLarge.spawnParticles(bulletnt.level(), bulletnt.getX(), bulletnt.getY(), bulletnt.getZ(), 15);
		};
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaSem").setColor(0x5C5C5C, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeBalefireConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardGrenadeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_BALEFIRE;
		bullet.velocity *= 2;
		bullet.gravity *= 2;
		bullet.dmgMin = 50;
		bullet.dmgMax = 65;
		bullet.wear = 25;
		bullet.trail = 1;
		bullet.explosive = 0.0F;
		
		bullet.bntImpact = (bulletnt, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
			
			if(bulletnt.level().isClientSide())
				return;
			
			LegacyExplosion.explode(bulletnt.level(), null, bulletnt.getX(), bulletnt.getY(), bulletnt.getZ(), 6, ExAttrib.BALEFIRE);
			
			ExplosionLarge.spawnParticles(bulletnt.level(), bulletnt.getX(), bulletnt.getY(), bulletnt.getZ(), 30);
		};
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaBale").setColor(0x7BFF44, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}

	public static BulletConfiguration getGrenadeKampfConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_KAMPF;
		bullet.spread = 0.0F;
		bullet.gravity = 0.0D;
		bullet.wear = 15;
		bullet.explosive = 3.5F;
		bullet.style = BulletConfiguration.STYLE_GRENADE;
		bullet.trail = 4;
		bullet.vPFX = "smoke";
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaKampf").setColor(0xE7BA48, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}

	public static BulletConfiguration getGrenadeCanisterConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_CANISTER;
		bullet.spread = 0.0F;
		bullet.gravity = 0.0D;
		bullet.wear = 15;
		bullet.explosive = 1F;
		bullet.style = BulletConfiguration.STYLE_GRENADE;
		bullet.trail = 4;
		bullet.vPFX = "smoke";
		
		bullet.bntUpdate = (bulletnt) -> {
			
			if(!bulletnt.level().isClientSide()) {
				
				if(bulletnt.tickCount > 10) {
					bulletnt.discard();
					
					for(int i = 0; i < 50; i++) {
						
						EntityBulletBaseNT bolt = new EntityBulletBaseNT(bulletnt.level(), M44_AP);
						bolt.setPos(bulletnt.getX(), bulletnt.getY(), bulletnt.getZ());
						bolt.setThrowableHeading(bulletnt.getDeltaMovement().x, bulletnt.getDeltaMovement().y, bulletnt.getDeltaMovement().z, 0.25F, 0.1F);
						if (bulletnt.getThrower() != null) bolt.initThrower(bulletnt.getThrower());
						bulletnt.level().addFreshEntity(bolt);
					}
				}
			}
		};
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaCan").setColor(0xCACACA, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeSleekConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardAirstrikeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_SLEEK;
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaIF").setColor(0x1D1D1D, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}
	
	public static BulletConfiguration get4GaugeClawConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_CLAW;
		bullet.dmgMin = 6;
		bullet.dmgMax = 9;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		bullet.leadChance = 100;
		
		bullet.bntHurt = (bulletnt, hit) -> {
			
			if(bulletnt.level().isClientSide())
				return;
			
			if(hit instanceof LivingEntity) {
				LivingEntity living = (LivingEntity) hit;
				float f = living.getHealth();
				
				if(f > 0) {
					f = Math.max(0, f - 2);
					living.setHealth(f);
					
					if(f == 0)
						living.die(bulletnt.getConfig().getDamage(bulletnt, bulletnt.getThrower()));
				}
			}
		};
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaClaw").setColor(0x5E38CC, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}
	
	public static BulletConfiguration get4GaugeVampireConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_VAMPIRE;
		bullet.dmgMin = 6;
		bullet.dmgMax = 9;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		bullet.leadChance = 100;
		bullet.style = BulletConfiguration.STYLE_FLECHETTE;
		
		bullet.bntHurt = (bulletnt, hit) -> {

			if(bulletnt.level().isClientSide())
				return;

			if(hit instanceof Player) {
				Player player = (Player) hit;

				// main wiped Witchery's vampire data here (WitcheryExtendedPlayer); Witchery has no port, so nothing to wipe
				if (player.isAlive()) return;
			}
		};
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaVamp").setColor(0x278400, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}
	
	public static BulletConfiguration get4GaugeVoidConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBuckshotConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_VOID;
		bullet.dmgMin = 6;
		bullet.dmgMax = 9;
		bullet.bulletsMin *= 2;
		bullet.bulletsMax *= 2;
		bullet.leadChance = 0;
		
		bullet.bntHurt = (bulletnt, hit) -> {
				
			if(bulletnt.level().isClientSide())
				return;

			if(hit instanceof Player) {
				Player player = (Player) hit;

				player.getInventory().dropAll();
				player.level().explode(bulletnt.getThrower(), player.getX(), player.getY(), player.getZ(), 5.0F, true, Level.ExplosionInteraction.TNT);
			}
		};
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaVoid").setColor(0x3F3F3F, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}

	public static BulletConfiguration get4GaugeQuackConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_4GAUGE_TITAN;
		bullet.velocity *= 2D;
		bullet.spread = 0.0F;
		bullet.gravity = 0.0D;
		bullet.wear = 10;
		bullet.explosive = 1F;
		bullet.style = BulletConfiguration.STYLE_BOLT;
		bullet.trail = 4;
		bullet.vPFX = "explode";
		
		bullet.bntUpdate = (bulletnt) -> {

			if(!bulletnt.level().isClientSide()) {
				if(bulletnt.tickCount % 2 == 0) {

					List<PathfinderMob> creatures = bulletnt.level().getEntitiesOfClass(PathfinderMob.class, bulletnt.getBoundingBox().inflate(10, 10, 10));
					for(PathfinderMob creature : creatures) {

						if(creature.getClass().getCanonicalName().startsWith("net.minecraft.entity.titan")) {
							// main passed 0, 0, 0 here, which blew up at the world origin; only the titans mod spawned these
							BulletConfigFactory.nuclearExplosion(creature, null, ExplosionNukeSmall.PARAMS_TOTS);

							creature.discard();
						}
					}
				}
			}
		};
		
		bullet.spentCasing = CASING4GAUGE.clone().register("old4GaDucc").setColor(0x1E1E1E, SpentCasing.COLOR_CASE_4GA);
		
		return bullet;
	}
}
