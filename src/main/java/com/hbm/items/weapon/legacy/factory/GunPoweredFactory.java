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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/GunPoweredFactory, value for value. */
public final class GunPoweredFactory {

    private GunPoweredFactory() {}

	
	public static GunConfiguration getGlassCannonConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.firingDuration = 0;
		config.durability = 2500;
		config.allowsInfinity = false;
		config.crosshair = Crosshair.BOX;
		config.firingSound = "hbm:weapon.zomgShoot";
		config.maxCharge = 1_000_000;
		config.chargeRate = 2500;
		
		config.name = "LIY2001 Anti-Material Electromagnetic Rifle Prototype";
		config.manufacturer = EnumGunManufacturer.OXFORD;
		
		config.config = new ArrayList<Integer>();
		config.config.add(GLASS_EMRADIO);
		config.config.add(GLASS_EMMICRO);
		config.config.add(GLASS_EMIR);
		config.config.add(GLASS_EMVISIBLE);
		config.config.add(GLASS_EMUV);
		config.config.add(GLASS_EMXRAY);
		config.config.add(GLASS_EMGAMMA);
		
		return config;
	}
	
	public static BulletConfiguration getEMRadioConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.velocity = 4.0F;
		bullet.spread = 0F;
		bullet.dmgMin = 35;
		bullet.dmgMax = 45;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.gravity = 0D;
		bullet.maxAge = 100;
		bullet.doesRicochet = true;
		bullet.ricochetAngle = 90;
		bullet.HBRC = 2;
		bullet.LBRC = 90;
		bullet.bounceMod = 1;
		bullet.doesPenetrate = true;
		bullet.style = BulletConfiguration.STYLE_BOLT;
		bullet.trail = BulletConfiguration.BOLT_LASER;
		bullet.plink = BulletConfiguration.PLINK_ENERGY;
		bullet.dischargePerShot = 25_000;
		bullet.firingRate = 20;
		bullet.modeName = "weapon.elecGun.glass_cannon.radio";
		bullet.chatColour = net.minecraft.ChatFormatting.DARK_RED;
		bullet.setToFire(200);
		
		return bullet;
	}
	
	public static BulletConfiguration getEMMicroConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.velocity = 3.0F;
		bullet.spread = 0F;
		bullet.dmgMin = 18;
		bullet.dmgMax = 22;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.gravity = 0D;
		bullet.maxAge = 100;
		bullet.doesRicochet = true;
		bullet.ricochetAngle = 90;
		bullet.HBRC = 2;
		bullet.LBRC = 90;
		bullet.bounceMod = 1;
		bullet.doesPenetrate = true;
		bullet.style = BulletConfiguration.STYLE_BOLT;
		bullet.trail = BulletConfiguration.BOLT_LASER;
		bullet.dischargePerShot = 12_500;
		bullet.firingRate = 15;
		bullet.modeName = "weapon.elecGun.glass_cannon.micro";
		bullet.chatColour = net.minecraft.ChatFormatting.RED;
		bullet.setToFire(200);
		
		return bullet;
	}
	
	public static BulletConfiguration getEMInfraredConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.velocity = 2.0F;
		bullet.spread = 0F;
		bullet.dmgMin = 9;
		bullet.dmgMax = 11;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.gravity = 0D;
		bullet.maxAge = 100;
		bullet.doesRicochet = true;
		bullet.ricochetAngle = 90;
		bullet.HBRC = 2;
		bullet.LBRC = 90;
		bullet.bounceMod = 1;
		bullet.doesPenetrate = true;
		bullet.style = BulletConfiguration.STYLE_BOLT;
		bullet.trail = BulletConfiguration.BOLT_LASER;
		bullet.dischargePerShot = 6_000;
		bullet.firingRate = 10;
		bullet.modeName = "weapon.elecGun.glass_cannon.ir";
		bullet.chatColour = net.minecraft.ChatFormatting.RED;
		bullet.setToFire(100);
		
		return bullet;
	}
	
	public static BulletConfiguration getEMVisibleConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.velocity = 2.0F;
		bullet.spread = 0F;
		bullet.dmgMin = 4;
		bullet.dmgMax = 6;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.gravity = 0D;
		bullet.maxAge = 100;
		bullet.doesRicochet = true;
		bullet.ricochetAngle = 90;
		bullet.HBRC = 2;
		bullet.LBRC = 90;
		bullet.bounceMod = 1;
		bullet.doesPenetrate = true;
		bullet.style = BulletConfiguration.STYLE_BOLT;
		bullet.trail = BulletConfiguration.BOLT_WORM;
		bullet.dischargePerShot = 2_500;
		bullet.firingRate = 5;
		bullet.modeName = "weapon.elecGun.glass_cannon.visible";
		bullet.chatColour = net.minecraft.ChatFormatting.GREEN;
		bullet.setToFire(100);
		
		return bullet;
	}
	
	public static BulletConfiguration getEMUVConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.velocity = 2.0F;
		bullet.spread = 0F;
		bullet.dmgMin = 3;
		bullet.dmgMax = 3;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.gravity = 0D;
		bullet.maxAge = 100;
		bullet.doesRicochet = true;
		bullet.ricochetAngle = 90;
		bullet.HBRC = 2;
		bullet.LBRC = 90;
		bullet.bounceMod = 1;
		bullet.doesPenetrate = true;
		bullet.style = BulletConfiguration.STYLE_BOLT;
		bullet.trail = BulletConfiguration.BOLT_GLASS_CYAN;
		bullet.dischargePerShot = 1_200;
		bullet.firingRate = 3;
		bullet.modeName = "weapon.elecGun.glass_cannon.uv";
		bullet.chatColour = net.minecraft.ChatFormatting.AQUA;
		bullet.setToFire(100);
		
		return bullet;
	}
	
	public static BulletConfiguration getEMXrayConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.velocity = 2.0F;
		bullet.spread = 0F;
		bullet.dmgMin = 2;
		bullet.dmgMax = 2;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.gravity = 0D;
		bullet.maxAge = 100;
		bullet.doesRicochet = true;
		bullet.ricochetAngle = 90;
		bullet.HBRC = 2;
		bullet.LBRC = 90;
		bullet.bounceMod = 1;
		bullet.doesPenetrate = true;
		bullet.style = BulletConfiguration.STYLE_BOLT;
		bullet.trail = BulletConfiguration.BOLT_GLASS_BLUE;
		bullet.dischargePerShot = 800;
		bullet.firingRate = 2;
		bullet.modeName = "weapon.elecGun.glass_cannon.xray";
		bullet.chatColour = net.minecraft.ChatFormatting.BLUE;
		bullet.setToFire(40);
		
		return bullet;
	}
	
	public static BulletConfiguration getEMGammaConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.velocity = 2.0F;
		bullet.spread = 0F;
		bullet.dmgMin = 1;
		bullet.dmgMax = 1;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.gravity = 0D;
		bullet.maxAge = 100;
		bullet.doesRicochet = true;
		bullet.ricochetAngle = 90;
		bullet.HBRC = 2;
		bullet.LBRC = 90;
		bullet.bounceMod = 1;
		bullet.doesPenetrate = true;
		bullet.style = BulletConfiguration.STYLE_BOLT;
		bullet.trail = BulletConfiguration.BOLT_LACUNAE;
		bullet.dischargePerShot = 400;
		bullet.firingRate = 1;
		bullet.modeName = "weapon.elecGun.glass_cannon.gamma";
		bullet.chatColour = net.minecraft.ChatFormatting.LIGHT_PURPLE;
		bullet.setToFire(40);
		
		return bullet;
	}

}
