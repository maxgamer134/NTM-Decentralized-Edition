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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun75BoltFactory, value for value. */
public final class Gun75BoltFactory {

    private Gun75BoltFactory() {}


	public static GunConfiguration getBolterConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 2;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.hasSights = false;
		config.reloadDuration = 40;
		config.firingDuration = 0;
		config.ammoCap = 30;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.NONE;
		config.durability = 10000;
		config.reloadSound = GunConfiguration.RSOUND_MAG;
		config.firingSound = "hbm:weapon.hksShoot";
		config.reloadSoundEnd = false;
		config.showAmmo = false;
		
		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("RECOIL", new BusAnimationSequence()
						.addPos(1, 0, 0, 25)
						.addPos(0, 0, 0, 75)
						)
				.addBus("EJECT", new BusAnimationSequence()
						.addPos(0, 0, 0, 25)
						.addPos(0, 0, 1, 75)
						)
				);
		
		config.animations.put(AnimType.RELOAD, new BusAnimation()
				.addBus("TILT", new BusAnimationSequence()
						.addPos(1, 0, 0, 250)
						.addPos(1, 0, 0, 1500)
						.addPos(0, 0, 0, 250)
						)
				.addBus("MAG", new BusAnimationSequence()
						.addPos(0, 0, 1, 500)
						.addPos(1, 0, 1, 500)
						.addPos(0, 0, 0, 500)
						)
				);
		
		config.name = "bolter";
		config.manufacturer = EnumGunManufacturer.CERIX;

		config.config = LegacyCollection.b75;
		
		return config;
	}

	private static float inaccuracy = 0.5F;
	public static BulletConfiguration get75BoltConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_75BOLT;
		bullet.ammoCount = 30;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 74;
		bullet.dmgMax = 82;
		bullet.doesRicochet = false;
		bullet.explosive = 0.25F;
		
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
						living.die(living.level().damageSources().source(ModDamageTypes.LEAD));
				}
			}
		};
		
		return bullet;
	}

	public static BulletConfiguration get75BoltIncConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_75BOLT_INCENDIARY;
		bullet.ammoCount = 30;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 72;
		bullet.dmgMax = 76;
		bullet.doesRicochet = false;
		bullet.explosive = 0.25F;

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
		
		return bullet;
	}
	
	public static BulletConfiguration get75BoltHEConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
		
		bullet.ammo = LegacyGuns.AMMO_75BOLT_HE;
		bullet.ammoCount = 30;
		bullet.spread *= inaccuracy;
		bullet.dmgMin = 94;
		bullet.dmgMax = 100;
		bullet.doesRicochet = false;
		bullet.explosive = 2.5F;
		bullet.blockDamage = false;
		
		return bullet;
	}
}
