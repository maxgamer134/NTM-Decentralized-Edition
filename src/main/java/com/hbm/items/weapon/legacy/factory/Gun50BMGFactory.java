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
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun50BMGFactory, value for value. */
public final class Gun50BMGFactory {

    private Gun50BMGFactory() {}

	
	private static final GunConfiguration.Ejector EJECTOR_BMG;
	private static final GunConfiguration.Ejector EJECTOR_SNIPER;
	private static final SpentCasing CASING50BMG;
	private static final SpentCasing CASINGLUNA;

	static {
		EJECTOR_BMG = new GunConfiguration.Ejector().setMotion(-0.35, 0.9, 0).setOffset(-0.45, -0.2, 0.35).setAngleRange(0.01F, 0.05F);
		EJECTOR_SNIPER = new GunConfiguration.Ejector().setMotion(-2, 0.15, 0).setOffset(-0.45, -0.2, 0.35).setAngleRange(0.02F, 0.05F);
		CASING50BMG = new SpentCasing(CasingType.BOTTLENECK).setScale(3F).setBounceMotion(0.01F, 0.05F).setColor(SpentCasing.COLOR_CASE_BRASS).setupSmoke(0.125F, 0.5D, 60, 20);
		CASINGLUNA = new SpentCasing(CasingType.BOTTLENECK).setScale(4F).setBounceMotion(0.02F, 0.05F).setColor(SpentCasing.COLOR_CASE_BRASS).setupSmoke(0.125F, 0.5D, 60, 30);
	}
	
	public static BulletConfiguration getLunaticSabotRound() {
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();

		bullet.ammo = LegacyGuns.AMMO_LUNA;
		bullet.spread = 0.0F;
		bullet.dmgMax = 500F;
		bullet.dmgMin = 450F;
		bullet.headshotMult = 2.5F;
		bullet.wear = 2000;
		bullet.velocity = 10F;
		bullet.doesPenetrate = true;
		bullet.leadChance = 20;

		bullet.blockDamage = false;
		bullet.bntImpact = (projectile, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
 projectile.level().explode(projectile, x, y, z, 2.0F, false, Level.ExplosionInteraction.NONE);
 };
		
		bullet.spentCasing = CASINGLUNA.clone().register("oldLunaStock");

		return bullet;
	}

	public static BulletConfiguration getLunaticIncendiaryRound() {
		BulletConfiguration bullet = getLunaticSabotRound().clone();

		bullet.ammo = LegacyGuns.AMMO_LUNA_INCENDIARY;
		bullet.incendiary = 10;
		bullet.bntImpact = (projectile, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
 projectile.level().explode(projectile, x, y, z, 5.0F, true, Level.ExplosionInteraction.NONE);
 };
		
		bullet.spentCasing = CASINGLUNA.clone().register("oldLunaInc");

		return bullet;
	}

	public static BulletConfiguration getLunaticExplosiveRound() {
		BulletConfiguration bullet = getLunaticSabotRound().clone();

		bullet.ammo = LegacyGuns.AMMO_LUNA_EXPLOSIVE;
		bullet.explosive = 25;
		bullet.destroysBlocks = true;
		bullet.bntImpact = (projectile, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
 projectile.level().explode(projectile, x, y, z, 25.0F, true, Level.ExplosionInteraction.NONE);
 };
		
		bullet.spentCasing = CASINGLUNA.clone().register("oldLunaExp");

		return bullet;
	}

	public static GunConfiguration getAR15Config() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 1;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.reloadDuration = 20;
		config.firingDuration = 0;
		config.reloadSoundEnd = false;
		config.ammoCap = 50;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.NONE;
		config.durability = 100_000;
		config.reloadSound = GunConfiguration.RSOUND_MAG;
		config.firingSound = "hbm:turret.howard_fire";
		
		config.name = "ar15_50";
		config.manufacturer = EnumGunManufacturer.ARMALITE;
		
		config.config = new ArrayList<Integer>();
		config.config.add(BMG50_FLECHETTE_AM);
		config.config.add(BMG50_FLECHETTE_PO);
		config.config.add(BMG50_FLECHETTE_NORMAL);
		config.config.add(BMG50_NORMAL);
		config.config.add(BMG50_INCENDIARY);
		config.config.add(BMG50_PHOSPHORUS);
		config.config.add(BMG50_EXPLOSIVE);
		config.config.add(BMG50_AP);
		config.config.add(BMG50_DU);
		config.config.add(BMG50_STAR);
		config.config.add(CHL_BMG50);
		config.config.add(BMG50_SLEEK);
		
		config.ejector = EJECTOR_BMG;

		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence()
						.addPos(1, 0, 0, 25)
						.addPos(0, 0, 0, 75)
				)
		);
		config.animations.put(AnimType.RELOAD, new BusAnimation()
				.addBus("TILT", new BusAnimationSequence()
						.addPos(1, 0, 0, 125)
						.addPos(1, 0, 0, 750)
						.addPos(0, 0, 0, 125)
				)
				.addBus("MAG", new BusAnimationSequence()
						.addPos(0, 0, 1, 200)
						.addPos(1, 0, 1, 200)
						.addPos(0, 0, 0, 200)
				)
		);

		return config;
	}

	public static GunConfiguration getAR15BurstConfig(){
		GunConfiguration config = getAR15Config();
		config.rateOfFire = 4;
		config.roundsPerBurst = 3;
		config.firingDuration = 2;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_BURST;

		return config;
	}

	public static GunConfiguration getM2Config() {
		GunConfiguration config = getAR15Config();
		
		config.rateOfFire = 2;
		config.durability *= 10;
		config.ammoCap = 0;
		config.crosshair = Crosshair.L_BOX;
		config.reloadType = GunConfiguration.RELOAD_NONE;
		config.hasSights = true;
		config.zoomFOV = 0.66F;
		config.allowsInfinity = true;
		config.durability = 10_000;
		config.firingSound = "hbm:turret.chekhov_fire";
		config.equipSound = "hbm:turret.howard_reload";
		
		config.name = "m2";
		config.manufacturer = EnumGunManufacturer.COLT;
		config.comment.add("\"A single man can do unbelievable things...");
		config.comment.add("A single man with a .50 cal machine gun can do even more.\"");
		
		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence()
						.addPos(1, 0, 0, 25)
						.addPos(0, 0, 0, 75)
						)
				);
		
		config.ejector = EJECTOR_BMG;
		
		config.config.clear();
		config.config.addAll(LegacyCollection.bmg50);
		
		return config;
	}
	
	public static final ResourceLocation scope_luna = Library.id("textures/misc/scope_luna.png");
	
	public static GunConfiguration getLunaticMarksman() {
		GunConfiguration config = new GunConfiguration();

		config.rateOfFire = 15;
		config.reloadDuration = 15;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.roundsPerCycle = 1;
		config.firingSound = "hbm:weapon.hicalShot";
		config.firingPitch = 0.75F;
		config.ammoCap = 4;
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		config.hasSights = true;
		config.zoomFOV = 0.2F; //x5 magnification
		config.scopeTexture = scope_luna;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CLASSIC;
		config.reloadSound = GunConfiguration.RSOUND_SHOTGUN;
		config.reloadSoundEnd = true;
		config.durability = 500_000;

		config.name = "lunaSniper";
		config.manufacturer = EnumGunManufacturer.LUNA;
		config.comment.add("\"You do not spark joy\"");

		config.config = new ArrayList<>();
		config.config.add(ROUND_LUNA_SNIPER_SABOT);
		config.config.add(ROUND_LUNA_SNIPER_INCENDIARY);
		config.config.add(ROUND_LUNA_SNIPER_EXPLOSIVE);

		config.animations.put(AnimType.CYCLE,
				new BusAnimation()
						.addBus("RECOIL", new BusAnimationSequence()
								.addPos(-0.45, 0.15, 0, 40) // Moves back  and raise slightly
								.addPos(0, 0, 0, 75)) // Then forward  again
						.addBus("EJECT", new BusAnimationSequence().addPos(0, 0, 0, 30) // Wait
								.addPos(50, 0, 0, 120))); // Fly // out

		config.ejector = EJECTOR_SNIPER;
		return config;
	}

	static float inaccuracy = 2.5F;
	public static BulletConfiguration get50BMGConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50BMG;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 30;
		bullet.dmgMax = 36;
		
		bullet.spentCasing = CASING50BMG.clone().register("old50BMGStock");
		
		return bullet;
	}

	public static BulletConfiguration get50BMGFireConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50BMG_INCENDIARY;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 30;
		bullet.dmgMax = 36;
		bullet.wear = 15;
		bullet.incendiary = 5;
		
		bullet.spentCasing = CASING50BMG.clone().register("old50BMGInc");
		
		return bullet;
	}

	public static BulletConfiguration get50BMGPhosphorusConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50BMG_PHOSPHORUS;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 30;
		bullet.dmgMax = 36;
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
		
		bullet.spentCasing = CASING50BMG.clone().register("old50BMGPhos");
		
		return bullet;
	}

	public static BulletConfiguration get50BMGExplosiveConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50BMG_EXPLOSIVE;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 60;
		bullet.dmgMax = 64;
		bullet.wear = 25;
		bullet.explosive = 1;
		
		bullet.spentCasing = CASING50BMG.clone().register("old50BMGExp");
		
		return bullet;
	}

	public static BulletConfiguration get50BMGAPConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50BMG_AP;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 62;
		bullet.dmgMax = 68;
		bullet.wear = 15;
		bullet.leadChance = 10;
		
		bullet.spentCasing = CASING50BMG.clone().register("old50BMGAP");
		
		return bullet;
	}

	public static BulletConfiguration get50BMGDUConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50BMG_DU;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 80;
		bullet.dmgMax = 86;
		bullet.wear = 25;
		bullet.leadChance = 50;
		
		bullet.spentCasing = CASING50BMG.clone().register("old50BMGDU");
		
		return bullet;
	}

	public static BulletConfiguration get50BMGStarConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50BMG_STAR;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 98;
		bullet.dmgMax = 102;
		bullet.wear = 25;
		bullet.leadChance = 100;
		
		bullet.spentCasing = CASING50BMG.clone().register("old50BMGStar");
		
		return bullet;
	}

	public static BulletConfiguration get50BMGSleekConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50BMG_SLEEK;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 50;
		bullet.dmgMax = 70;
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
		
		bullet.spentCasing = CASING50BMG.clone().register("old50BMGIF");
		
		return bullet;
	}
	
	public static BulletConfiguration get50BMGFlechetteConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_50BMG_FLECHETTE;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 50;
		bullet.dmgMax = 54;
		bullet.style = bullet.STYLE_FLECHETTE;
		BulletConfigFactory.makeFlechette(bullet);
		
		bullet.spentCasing = CASING50BMG.clone().register("old50BMGFlech");
		
		return bullet;
	}
	
	public static BulletConfiguration get50BMGFlechetteAMConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();

		bullet.ammo = LegacyGuns.AMMO_50BMG_FLECHETTE_AM;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 60;
		bullet.dmgMax = 64;
		bullet.style = bullet.STYLE_FLECHETTE;
		BulletConfigFactory.makeFlechette(bullet);
		
		bullet.bntHit = (bulletnt, hit) -> {

			if(bulletnt.level().isClientSide())
				return;

			if(hit instanceof LivingEntity) {
				ContaminationUtil.contaminate((LivingEntity) hit, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 100F);
			}
		};
		
		bullet.spentCasing = CASING50BMG.clone().register("old50BMGAM");
		
		return bullet;
	}
	
	public static BulletConfiguration get50BMGFlechettePOConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();

		bullet.ammo = LegacyGuns.AMMO_50BMG_FLECHETTE_PO;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 60;
		bullet.dmgMax = 64;
		bullet.style = bullet.STYLE_FLECHETTE;
		BulletConfigFactory.makeFlechette(bullet);
		
		bullet.bntHit = (bulletnt, hit) -> {

			if(bulletnt.level().isClientSide())
				return;

			if(hit instanceof LivingEntity) {
				ContaminationUtil.contaminate((LivingEntity) hit, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 50F);
			}
		};
		
		bullet.spentCasing = CASING50BMG.clone().register("old50BMGPO");
		
		return bullet;
	}
}
