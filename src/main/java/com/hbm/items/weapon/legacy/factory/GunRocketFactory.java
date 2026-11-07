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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/GunRocketFactory, value for value. */
public final class GunRocketFactory {

    private GunRocketFactory() {}

	
	public static GunConfiguration getGustavConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 30;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 30;
		config.firingDuration = 0;
		config.ammoCap = 1;
		config.reloadType = GunConfiguration.RELOAD_SINGLE;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CIRCUMFLEX;
		config.firingSound = "hbm:weapon.rpgShoot";
		config.reloadSound = GunConfiguration.RSOUND_LAUNCHER;
		config.reloadSoundEnd = false;
		
		config.name = "gustav";
		config.manufacturer = EnumGunManufacturer.SAAB;
		config.comment.add("Fun fact of the day: Recoilless");
		config.comment.add("rifles don't actually fire rockets.");
		
		config.config = new ArrayList<Integer>();
		config.config.add(ROCKET_NORMAL);
		config.config.add(ROCKET_HE);
		config.config.add(ROCKET_INCENDIARY);
		config.config.add(ROCKET_PHOSPHORUS);
		config.config.add(ROCKET_SHRAPNEL);
		config.config.add(ROCKET_EMP);
		config.config.add(ROCKET_GLARE);
		config.config.add(ROCKET_TOXIC);
		config.config.add(ROCKET_CANISTER);
		config.config.add(ROCKET_SLEEK);
		config.config.add(ROCKET_NUKE);
		config.config.add(ROCKET_CHAINSAW);
		config.durability = 140;
		
		return config;
	}
	
	public static GunConfiguration getQuadroConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 5;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 100;
		config.firingDuration = 0;
		config.ammoCap = 4;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CIRCUMFLEX;
		config.firingSound = "hbm:weapon.rpgShoot";
		config.reloadSound = "hbm:weapon.quadroReload";
		config.reloadSoundEnd = false;
		
		config.animations.put(AnimType.CYCLE, new BusAnimation()
				.addBus("QUADRO_RECOIL", new BusAnimationSequence()
						.addPos(0, 0, -0.5, 50)
						.addPos(0, 0, 0, 50)
						)
				);
		
		config.animations.put(AnimType.RELOAD, new BusAnimation()
				.addBus("QUADRO_RELOAD_ROTATE", new BusAnimationSequence()
						.addPos(0, 0, 60, 750)
						.addPos(0, 0, 60, 3500)
						.addPos(0, 0, 0, 750)
						)
				.addBus("QUADRO_RELOAD_PUSH", new BusAnimationSequence()
						.addPos(-1, -1, 0, 0)
						.addPos(-1, -1, 0, 750)
						.addPos(-1, 0, 0, 500)
						.addPos(0, 0, 0, 3000)
						.addPos(0, 0, 0, 750)
						)
				);
		
		config.name = "quadro";
		config.manufacturer = EnumGunManufacturer.MANN;
		config.comment.add("For the next three hundred years, people who needed to get to the second");
		config.comment.add("floor used the only method available to them, which was rocket jumping.");
		config.comment.add("This persisted until 1857, when the young bearded inventor named");
		config.comment.add("President Abraham Lincoln invented stairs.");
		
		config.config = new ArrayList<Integer>();
		config.config.add(ROCKET_NORMAL_LASER);
		config.config.add(ROCKET_HE_LASER);
		config.config.add(ROCKET_INCENDIARY_LASER);
		config.config.add(ROCKET_PHOSPHORUS_LASER);
		config.config.add(ROCKET_SHRAPNEL_LASER);
		config.config.add(ROCKET_EMP_LASER);
		config.config.add(ROCKET_GLARE_LASER);
		config.config.add(ROCKET_TOXIC_LASER);
		config.config.add(ROCKET_CANISTER);
		config.config.add(ROCKET_SLEEK_LASER);
		config.config.add(ROCKET_NUKE_LASER);
		config.config.add(ROCKET_CHAINSAW_LASER);
		config.durability = 500;
		
		return config;
	}
	
	public static GunConfiguration getKarlConfig() {
		
		GunConfiguration config = getGustavConfig();
		
		config.reloadDuration = 20;
		
		config.name = "karl";
		config.manufacturer = EnumGunManufacturer.UNKNOWN;
		config.comment.clear();
		
		config.config = new ArrayList<Integer>();
		config.config.add(ROCKET_HE);
		config.config.add(ROCKET_EMP);
		config.config.add(ROCKET_SLEEK);
		config.config.add(ROCKET_TOXIC);
		config.config.add(ROCKET_CANISTER);
		config.config.add(ROCKET_NUKE);
		config.config.add(ROCKET_CHAINSAW);
		config.config.add(ROCKET_ERROR);
		config.durability = 500;
		
		return config;
	}
	
	public static GunConfiguration getPanzConfig() {
		
		GunConfiguration config = getGustavConfig();
		
		config.reloadDuration = 25;
		config.hasSights = true;
		
		config.name = "panz";
		config.manufacturer = EnumGunManufacturer.ENZINGER;
		config.comment.clear();
		config.comment.add("Panzer-Shrek");
		
		config.durability = 260;
		
		return config;
	}
	
	public static BulletConfiguration getRocketConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.explosive = 4F;
		bullet.trail = 0;
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketHEConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_HE;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 15;
		bullet.explosive = 6.5F;
		bullet.trail = 1;
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketIncendiaryConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_INCENDIARY;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 15;
		bullet.explosive = 4F;
		bullet.incendiary = 5;
		bullet.trail = 2;
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketEMPConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_EMP;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.explosive = 2.5F;
		bullet.emp = 10;
		bullet.trail = 4;
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketSleekConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_SLEEK;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.explosive = 10F;
		bullet.trail = 6;
		bullet.gravity = 0;
		bullet.jolt = 6.5D;
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketShrapnelConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_SHRAPNEL;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.explosive = 4F;
		bullet.shrapnel = 25;
		bullet.trail = 3;
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketGlareConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_GLARE;
		bullet.velocity = 5.0F;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 20;
		bullet.explosive = 4F;
		bullet.incendiary = 5;
		bullet.trail = 5;
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketNukeConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_NUCLEAR;
		bullet.velocity = 1.5F;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 35;
		bullet.explosive = 0;
		bullet.incendiary = 0;
		bullet.trail = 7;
		
		bullet.bntImpact = (bulletnt, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
			BulletConfigFactory.nuclearExplosion(bulletnt, pos, com.hbm.explosion.ExplosionNukeSmall.PARAMS_MEDIUM);
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketChlorineConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_TOXIC;
		bullet.velocity = 1.5F;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 10;
		bullet.explosive = 0;
		bullet.chlorine = 50;
		bullet.trail = 7;
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketRPCConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_RPC;
		bullet.velocity = 3.0F;
		bullet.dmgMin = 20;
		bullet.dmgMax = 25;
		bullet.wear = 15;
		bullet.explosive = 0;
		bullet.incendiary = 0;
		bullet.trail = 8;
		bullet.gravity = 0.000D;
		bullet.ricochetAngle = 90;
		bullet.LBRC = 100;
		bullet.doesPenetrate = true;
		
		bullet.bntRicochet = (bulletnt, pos) -> {
			// main broke wood, plants, glass and leaves; 1.21 has no block materials, so the matching tags
			net.minecraft.world.level.Level worldObj = bulletnt.level();
			net.minecraft.world.level.block.state.BlockState st = worldObj.getBlockState(pos);
			if(!worldObj.isClientSide() && (st.instrument() == net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASS && st.ignitedByLava() ||
					st.is(net.minecraft.tags.BlockTags.FLOWERS) || st.is(net.minecraft.tags.BlockTags.SAPLINGS) || st.is(net.minecraft.tags.BlockTags.CROPS) || st.is(net.minecraft.tags.BlockTags.REPLACEABLE) ||
					st.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) || st.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES) ||
					st.is(net.minecraft.tags.BlockTags.LEAVES)))
				worldObj.destroyBlock(pos, false);

		};
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketPhosphorusConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_PHOSPHORUS;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 15;
		bullet.explosive = 4F;
		bullet.incendiary = 5;
		bullet.trail = 9;
		
		bullet.bntImpact = BulletConfigFactory.getPhosphorousEffect(10, 60 * 20, 100, 0.5D, 1F);
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketCanisterConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_CANISTER;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.explosive = 2F;
		bullet.trail = 0;
		
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
		
		return bullet;
	}
	
	public static BulletConfiguration getRocketErrorConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardRocketConfig();
		
		bullet.ammo = LegacyGuns.AMMO_ROCKET_DIGAMMA;
		bullet.velocity = 0.5F;
		bullet.dmgMin = 10;
		bullet.dmgMax = 15;
		bullet.wear = 35;
		bullet.explosive = 0;
		bullet.incendiary = 0;
		bullet.trail = 7;
		
		bullet.bntImpact = (bulletnt, pos, side) -> {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();

			if(bulletnt.level().isClientSide())
				return;

			com.hbm.entity.effect.EntitySpear spear = new com.hbm.entity.effect.EntitySpear(com.hbm.entity.ModEntities.SPEAR.get(), bulletnt.level());
			spear.setPos(bulletnt.getX(), bulletnt.getY() + 100, bulletnt.getZ());

			bulletnt.level().addFreshEntity(spear);
		};
		
		return bullet;
	}
}
