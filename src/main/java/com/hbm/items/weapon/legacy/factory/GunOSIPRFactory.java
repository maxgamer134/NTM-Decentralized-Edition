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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/GunOSIPRFactory, value for value. */
public final class GunOSIPRFactory {

    private GunOSIPRFactory() {}

	
	public static GunConfiguration getOSIPRConfig() {
		
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
		config.crosshair = Crosshair.L_ARROWS;
		config.durability = 50_000;
		config.reloadSound = "hbm:weapon.osiprReload";
		config.firingSound = "hbm:weapon.osiprShoot";
		config.reloadSoundEnd = false;
		
		config.name = "osipr";
		config.manufacturer = EnumGunManufacturer.COMBINE;
		
		config.config = new ArrayList<Integer>();
		config.config.add(SPECIAL_OSIPR);
		
		return config;
	}
	
	public static GunConfiguration getAltConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 15;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 20;
		config.firingDuration = 0;
		config.ammoCap = 0;
		config.reloadType = GunConfiguration.RELOAD_NONE;
		config.allowsInfinity = true;
		config.firingSound = "hbm:weapon.singFlyby";
		
		config.config = new ArrayList<Integer>();
		config.config.add(SPECIAL_OSIPR_CHARGED);
		
		return config;
	}

	static float inaccuracy = 1.25F;
	public static BulletConfiguration getPulseConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.GUN_OSIPR_AMMO;
		bullet.ammoCount = 30;
		bullet.doesRicochet = false;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 15;
		bullet.dmgMax = 21;
		bullet.trail = 2;
		
		return bullet;
	}
	
	public static BulletConfiguration getPulseChargedConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.GUN_OSIPR_AMMO2;
		bullet.ricochetAngle = 360;
		bullet.LBRC = 100;
		bullet.HBRC = 100;
		bullet.bounceMod = 1;
		bullet.style = BulletConfiguration.STYLE_ORB;
		bullet.damageType = ModDamageTypes.COMBINE;
		bullet.liveAfterImpact = true;
		bullet.spread = 0;
		bullet.gravity = 0;
		bullet.maxAge = 150;
		bullet.velocity = 2;

		bullet.bntHurt = (ball, entity) -> {
			if(entity instanceof LivingEntity) {
				LivingEntity entityLiving = (LivingEntity) entity;
				entity.push(ball.getDeltaMovement().x / 2, ball.getDeltaMovement().y / 2, ball.getDeltaMovement().z / 2);

				if(entity == ball.getThrower())
					return;

				if(entityLiving.getHealth() <= 1000) {
					entityLiving.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HbmPotion.bang.get()), 1, 0));
					if (ball.getThrower() instanceof LivingEntity lastAttacker) entityLiving.setLastHurtByMob(lastAttacker);
				} else if(entityLiving.getHealth() > 1000) {
					ball.discard();
					return;
				}

			}
		};

		bullet.bntRicochet = (ball, pos) -> {
 int x = pos.getX(), y = pos.getY(), z = pos.getZ();
			if(pos != null && ball.level().getBlockState(pos).getBlock() instanceof com.hbm.blocks.bomb.BlockDetonatable det) det.onShot(ball.level(), pos);
		};

		bullet.bntImpact = (ball, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
			if(pos != null && ball.level().getBlockState(pos).getBlock() instanceof com.hbm.blocks.bomb.BlockDetonatable det) det.onShot(ball.level(), pos);
		};
		
		return bullet;
	}
}
