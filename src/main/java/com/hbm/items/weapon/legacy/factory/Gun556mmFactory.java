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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun556mmFactory, value for value. */
public final class Gun556mmFactory {

    private Gun556mmFactory() {}


	private static final GunConfiguration.Ejector EJECTOR_RIFLE;
	private static final GunConfiguration.Ejector EJECTOR_GRENADE;
	private static final SpentCasing CASING556;

	static {
		EJECTOR_RIFLE = new GunConfiguration.Ejector().setMotion(-0.35, 0.6, 0).setOffset(-0.35, 0, 0.35).setAngleRange(0.01F, 0.03F);
		EJECTOR_GRENADE = new GunConfiguration.Ejector().setAngleRange(0.02F, 0.03F).setDelay(30);
		CASING556 = new SpentCasing(CasingType.BOTTLENECK).setScale(1.25F).setBounceMotion(0.01F, 0.03F).setColor(SpentCasing.COLOR_CASE_BRASS);
	}

	public static GunConfiguration getEuphieConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 2;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.hasSights = false;
		config.reloadDuration = 20;
		config.firingDuration = 0;
		config.ammoCap = 40;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CROSS;
		config.durability = 10000;
		config.reloadSound = GunConfiguration.RSOUND_MAG;
		config.firingSound = "hbm:weapon.hksShoot";
		config.reloadSoundEnd = false;
		
		config.name = "baeAR";
		config.manufacturer = EnumGunManufacturer.BAE;
		
		config.comment.add("Why is this gun so sticky?");
		
		//config.config = new ArrayList<>();
		//config.config.add(R556_GOLD);
		
		config.config = LegacyCollection.r556;
		
		return config;
	}
	
	public static GunConfiguration getSPIWConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 3;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.hasSights = true;
		config.reloadDuration = 25;
		config.firingDuration = 0;
		config.ammoCap = 20;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_BOX;
		config.durability = 7000;
		config.reloadSound = GunConfiguration.RSOUND_MAG;
		config.firingSound = "hbm:weapon.hksShoot";
		config.reloadSoundEnd = false;
		
		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence()
						.addPos(0.5, 0, 0, 25)
						.addPos(0, 0, 0, 75)
						)
				);
		
		config.name = "spiw";
		config.manufacturer = EnumGunManufacturer.H_AND_R;
		
		config.comment.add("Launch some flechettes in the breeze");
		config.comment.add("Find his arms nailed to the trees");
		config.comment.add("Napalm sticks to kids");

		config.config = LegacyCollection.r556Flechette;
		
		config.ejector = EJECTOR_RIFLE;
		
		return config;
	}
	
	public static GunConfiguration getGLauncherConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 60;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.hasSights = true;
		config.reloadDuration = 40;
		config.firingDuration = 0;
		config.ammoCap = 0;
		config.reloadType = GunConfiguration.RELOAD_NONE;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CIRCUMFLEX;
		config.firingSound = "hbm:weapon.glauncher";
		config.reloadSound = GunConfiguration.RSOUND_GRENADE;
		config.reloadSoundEnd = false;
		
		config.config = LegacyCollection.grenade;
		
		config.ejector = EJECTOR_GRENADE;
		
		return config;
	}

	private static float inaccuracy = 2.5F;
	public static BulletConfiguration get556Config() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_556;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 16;
		bullet.dmgMax = 20;
		
		bullet.spentCasing = CASING556.clone().register("old556Stock");
		
		return bullet;
	}

	public static BulletConfiguration get556GoldConfig() {
		
		BulletConfiguration bullet = get556Config();
		
		bullet.ammo = LegacyGuns.AMMO_556_GOLD;
		bullet.dmgMin = 250;
		bullet.dmgMax = 320;
		bullet.spread = 0.0F;
		
		bullet.spentCasing = null;
		
		return bullet;
	}

	public static BulletConfiguration get556PhosphorusConfig() {
		
		BulletConfiguration bullet = get556Config();
		
		bullet.ammo = LegacyGuns.AMMO_556_PHOSPHORUS;
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
		
		bullet.spentCasing = CASING556.clone().register("old556Phos");
		
		return bullet;
	}

	public static BulletConfiguration get556APConfig() {
		
		BulletConfiguration bullet = get556Config();
		
		bullet.ammo = LegacyGuns.AMMO_556_AP;
		bullet.dmgMin = 20;
		bullet.dmgMax = 26;
		bullet.wear = 15;
		bullet.leadChance = 10;
		
		bullet.spentCasing = CASING556.clone().register("old556AP");
		
		return bullet;
	}

	public static BulletConfiguration get556DUConfig() {
		
		BulletConfiguration bullet = get556Config();
		
		bullet.ammo = LegacyGuns.AMMO_556_DU;
		bullet.dmgMin = 24;
		bullet.dmgMax = 32;
		bullet.wear = 25;
		bullet.leadChance = 50;
		
		bullet.spentCasing = CASING556.clone().register("old556DU");
		
		return bullet;
	}

	public static BulletConfiguration get556StarConfig() {
		
		BulletConfiguration bullet = get556Config();
		
		bullet.ammo = LegacyGuns.AMMO_556_STAR;
		bullet.dmgMin = 30;
		bullet.dmgMax = 36;
		bullet.wear = 25;
		bullet.leadChance = 100;
		
		bullet.spentCasing = CASING556.clone().register("old556Star");
		
		return bullet;
	}

	public static BulletConfiguration get556SleekConfig() {
		
		BulletConfiguration bullet = get556Config();
		
		bullet.ammo = LegacyGuns.AMMO_556_SLEEK;
		bullet.dmgMin = 45;
		bullet.dmgMax = 50;
		bullet.wear = 10;
		bullet.leadChance = 100;
		bullet.doesPenetrate = false;
		
		bullet.bntHit = (bulletnt, hit) -> {

			if(bulletnt.level().isClientSide())
				return;

			EntityBulletBaseNT meteor = new EntityBulletBaseNT(bulletnt.level(), MASKMAN_METEOR);
			meteor.setPos(hit.getX(), hit.getY() + 30 + meteor.level().random.nextInt(10), hit.getZ());
			meteor.setDeltaMovement(meteor.getDeltaMovement().x, -1D, meteor.getDeltaMovement().z);
			if (bulletnt.getThrower() != null) meteor.initThrower(bulletnt.getThrower());
			bulletnt.level().addFreshEntity(meteor);
		};

		bullet.bntImpact = (bulletnt, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();

			if(bulletnt.level().isClientSide())
				return;

			if(y == -1)
				return;

			EntityBulletBaseNT meteor = new EntityBulletBaseNT(bulletnt.level(), MASKMAN_METEOR);
			meteor.setPos(bulletnt.getX(), bulletnt.getY() + 30 + meteor.level().random.nextInt(10), bulletnt.getZ());
			meteor.setDeltaMovement(meteor.getDeltaMovement().x, -1D, meteor.getDeltaMovement().z);
			if (bulletnt.getThrower() != null) meteor.initThrower(bulletnt.getThrower());
			bulletnt.level().addFreshEntity(meteor);
		};
		
		bullet.spentCasing = CASING556.clone().register("old556IF");
		
		return bullet;
	}

	public static BulletConfiguration get556TracerConfig() {
		
		BulletConfiguration bullet = get556Config();

		bullet.ammo = LegacyGuns.AMMO_556_TRACER;
		bullet.vPFX = "reddust";
		
		bullet.spentCasing = CASING556.clone().register("old556Trac");
		
		return bullet;
	}

	public static BulletConfiguration get556FlechetteConfig() {
		
		BulletConfiguration bullet = get556Config();

		bullet.ammo = LegacyGuns.AMMO_556_FLECHETTE;
		bullet.dmgMin = 26;
		bullet.dmgMax = 32;
		bullet.HBRC = 2;
		bullet.LBRC = 95;
		bullet.wear = 15;
		bullet.style = BulletConfiguration.STYLE_FLECHETTE;
		bullet.doesPenetrate = false;
		BulletConfigFactory.makeFlechette(bullet);
		
		bullet.spentCasing = CASING556.clone().register("old556Flec");
		
		return bullet;
	}

	public static BulletConfiguration get556FlechetteIncendiaryConfig() {
		
		BulletConfiguration bullet = get556FlechetteConfig();
		
		bullet.ammo = LegacyGuns.AMMO_556_FLECHETTE_INCENDIARY;
		bullet.incendiary = 5;
		BulletConfigFactory.makeFlechette(bullet);
		
		bullet.spentCasing = CASING556.clone().register("old556FlecInc");
		
		return bullet;
	}

	public static BulletConfiguration get556FlechettePhosphorusConfig() {
		
		BulletConfiguration bullet = get556FlechetteConfig();
		
		bullet.ammo = LegacyGuns.AMMO_556_FLECHETTE_PHOSPHORUS;
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
		
		bullet.spentCasing = CASING556.clone().register("old556FlecPhos");
		
		return bullet;
	}

	public static BulletConfiguration get556FlechetteDUConfig() {
		
		BulletConfiguration bullet = get556FlechetteConfig();
		
		bullet.ammo = LegacyGuns.AMMO_556_FLECHETTE_DU;
		bullet.dmgMin = 46;
		bullet.dmgMax = 52;
		bullet.wear = 25;
		bullet.leadChance = 50;
		bullet.doesPenetrate = true;
		
		bullet.spentCasing = CASING556.clone().register("old556FlecDU");
		
		return bullet;
	}

	public static BulletConfiguration get556FlechetteSleekConfig() {
		
		BulletConfiguration bullet = get556FlechetteConfig();
		
		bullet.ammo = LegacyGuns.AMMO_556_FLECHETTE_SLEEK;
		bullet.dmgMin = 45;
		bullet.dmgMax = 50;
		bullet.wear = 10;
		bullet.leadChance = 50;
		bullet.doesPenetrate = false;
		
		bullet.bntHit = (bulletnt, hit) -> {

			if(bulletnt.level().isClientSide())
				return;

			EntityBulletBaseNT meteor = new EntityBulletBaseNT(bulletnt.level(), MASKMAN_METEOR);
			meteor.setPos(hit.getX(), hit.getY() + 30 + meteor.level().random.nextInt(10), hit.getZ());
			meteor.setDeltaMovement(meteor.getDeltaMovement().x, -1D, meteor.getDeltaMovement().z);
			if (bulletnt.getThrower() != null) meteor.initThrower(bulletnt.getThrower());
			bulletnt.level().addFreshEntity(meteor);
		};
		
		bullet.bntImpact = (bulletnt, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();

			if(bulletnt.level().isClientSide())
				return;

			if(y == -1)
				return;

			EntityBulletBaseNT meteor = new EntityBulletBaseNT(bulletnt.level(), MASKMAN_METEOR);
			meteor.setPos(bulletnt.getX(), bulletnt.getY() + 30 + meteor.level().random.nextInt(10), bulletnt.getZ());
			meteor.setDeltaMovement(meteor.getDeltaMovement().x, -1D, meteor.getDeltaMovement().z);
			if (bulletnt.getThrower() != null) meteor.initThrower(bulletnt.getThrower());
			bulletnt.level().addFreshEntity(meteor);
		};
		
		bullet.spentCasing = CASING556.clone().register("old556FlecIF");
		
		return bullet;
	}
	
	public static BulletConfiguration get556KConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_556_K;
		bullet.dmgMin = 0;
		bullet.dmgMax = 0;
		bullet.maxAge = 0;
		
		return bullet;
	}
}
