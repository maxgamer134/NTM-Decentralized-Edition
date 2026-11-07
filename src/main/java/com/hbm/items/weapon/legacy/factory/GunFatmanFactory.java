// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy.factory;

import static com.hbm.items.weapon.legacy.BulletConfigSyncingUtil.*;

import com.hbm.items.weapon.legacy.LegacyExplosion;
import com.hbm.items.weapon.legacy.LegacyExplosion.ExAttrib;
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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/GunFatmanFactory, value for value. */
public final class GunFatmanFactory {

    private GunFatmanFactory() {}

	
	public static GunConfiguration getFatmanConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 20;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 120;
		config.firingDuration = 0;
		config.ammoCap = 1;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CIRCUMFLEX;
		config.firingSound = "hbm:weapon.fatmanShoot";
		config.reloadSound = GunConfiguration.RSOUND_FATMAN;
		config.reloadSoundEnd = false;
		
		config.name = "m42";
		config.manufacturer = EnumGunManufacturer.F_STRONG;
		
		config.config = new ArrayList<Integer>();
		config.config.add(NUKE_NORMAL);
		config.config.add(NUKE_LOW);
		config.config.add(NUKE_HIGH);
		config.config.add(NUKE_TOTS);
		config.config.add(NUKE_SAFE);
		config.config.add(NUKE_PUMPKIN);
		config.config.add(NUKE_BARREL);
		config.durability = 1000;
		
		return config;
	}
	
	public static GunConfiguration getMIRVConfig() {
		
		GunConfiguration config = getFatmanConfig();
		
		config.name = "m42MIRV";
		config.manufacturer = EnumGunManufacturer.F_STRONG;
		
		config.config = new ArrayList<Integer>();
		config.config.add(NUKE_MIRV_NORMAL);
		config.config.add(NUKE_MIRV_LOW);
		config.config.add(NUKE_MIRV_HIGH);
		config.config.add(NUKE_MIRV_SAFE);
		config.config.add(NUKE_MIRV_SPECIAL);
		config.durability = 1000;
		
		return config;
	}
	
	public static GunConfiguration getBELConfig() {
		
		GunConfiguration config = getFatmanConfig();
		
		config.name = "bel";
		config.manufacturer = EnumGunManufacturer.F_STRONG;
		
		config.config = new ArrayList<Integer>();
		config.config.add(NUKE_AMAT);
		
		return config;
	}
	
	public static GunConfiguration getProtoConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 20;
		config.roundsPerCycle = 8;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 120;
		config.firingDuration = 0;
		config.ammoCap = 8;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CIRCUMFLEX;
		config.firingSound = "hbm:weapon.fatmanShoot";
		config.reloadSound = GunConfiguration.RSOUND_FATMAN;
		config.reloadSoundEnd = false;
		
		config.name = "m42";
		config.manufacturer = EnumGunManufacturer.F_STRONG;
		
		config.config = new ArrayList<Integer>();
		config.config.add(NUKE_PROTO_NORMAL);
		config.config.add(NUKE_PROTO_LOW);
		config.config.add(NUKE_PROTO_HIGH);
		config.config.add(NUKE_PROTO_TOTS);
		config.config.add(NUKE_PROTO_SAFE);
		config.config.add(NUKE_PROTO_PUMPKIN);
		config.config.add(NUKE_BARREL);
		config.durability = 1000;
		
		return config;
	}
	
	public static BulletConfiguration getNukeConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardNukeConfig();
		bullet.ammo = LegacyGuns.AMMO_NUKE;
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				BulletConfigFactory.nuclearExplosion(bullet, pos, com.hbm.explosion.ExplosionNukeSmall.PARAMS_MEDIUM);
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getNukeLowConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardNukeConfig();
		bullet.ammo = LegacyGuns.AMMO_NUKE_LOW;
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				BulletConfigFactory.nuclearExplosion(bullet, pos, com.hbm.explosion.ExplosionNukeSmall.PARAMS_LOW);
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getNukeHighConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardNukeConfig();
		bullet.ammo = LegacyGuns.AMMO_NUKE_HIGH;
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				BulletConfigFactory.nuclearExplosion(bullet, pos, com.hbm.explosion.ExplosionNukeSmall.PARAMS_HIGH);
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getNukeTotsConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardNukeConfig();
		bullet.ammo = LegacyGuns.AMMO_NUKE_TOTS;
		bullet.bulletsMin = 8;
		bullet.bulletsMax = 8;
		bullet.spread = 0.1F;
		bullet.style = bullet.STYLE_GRENADE;
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				BulletConfigFactory.nuclearExplosion(bullet, pos, com.hbm.explosion.ExplosionNukeSmall.PARAMS_TOTS);
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getNukeSafeConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardNukeConfig();
		bullet.ammo = LegacyGuns.AMMO_NUKE_SAFE;
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				BulletConfigFactory.nuclearExplosion(bullet, pos, com.hbm.explosion.ExplosionNukeSmall.PARAMS_SAFE);
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getNukePumpkinConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardNukeConfig();
		bullet.ammo = LegacyGuns.AMMO_NUKE_PUMPKIN;
		bullet.explosive = 10F;
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				
				if(!bullet.level().isClientSide()) {

					double posX = bullet.getX();
					double posY = bullet.getY() + 0.5;
					double posZ = bullet.getZ();
					
					if(y >= 0) {
						posX = x + 0.5;
						posY = y + 1.5;
						posZ = z + 0.5;
					}
					
					com.hbm.explosion.ExplosionLarge.spawnParticles(bullet.level(), posX, posY, posZ, 45);
				}
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getNukeBarrelConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardNukeConfig();
		bullet.ammo = LegacyGuns.AMMO_NUKE_BARREL;
		bullet.explosive = 3F;
		bullet.style = bullet.STYLE_BARREL;
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				
				if(!bullet.level().isClientSide()) {

					double posX = bullet.getX();
					double posY = bullet.getY() + 0.5;
					double posZ = bullet.getZ();
					
					if(y >= 0) {
						posX = x + 0.5;
						posY = y + 1.5;
						posZ = z + 0.5;
					}

					x = (int)Math.floor(posX);
					y = (int)Math.floor(posY);
					z = (int)Math.floor(posZ);
					
					net.minecraft.world.level.Level worldObj = bullet.level();
					
					for(int ix = x - 3; ix <= x + 3; ix++) {
						for(int iy = y - 3; iy <= y + 3; iy++) {
							for(int iz = z - 3; iz <= z + 3; iz++) {
								
								if(worldObj.random.nextInt(3) == 0 && worldObj.getBlockState(new net.minecraft.core.BlockPos(ix, iy, iz)).canBeReplaced() && com.hbm.blocks.ModBlocks.FALLOUT.get().defaultBlockState().canSurvive(worldObj, new net.minecraft.core.BlockPos(ix, iy, iz))) {
									worldObj.setBlockAndUpdate(new net.minecraft.core.BlockPos(ix, iy, iz), com.hbm.blocks.ModBlocks.FALLOUT.get().defaultBlockState());
								} else if(worldObj.getBlockState(new net.minecraft.core.BlockPos(ix, iy, iz)).isAir()) {
									
									if(worldObj.random.nextBoolean())
										worldObj.setBlockAndUpdate(new net.minecraft.core.BlockPos(ix, iy, iz), com.hbm.blocks.ModBlocks.GAS_RADON.get().defaultBlockState());
									else
										worldObj.setBlockAndUpdate(new net.minecraft.core.BlockPos(ix, iy, iz), com.hbm.blocks.ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
								}
							}
						}
					}
					
					if (worldObj instanceof net.minecraft.server.level.ServerLevel rsl) com.hbm.handler.radiation.RadiationSystemNT.incrementRad(rsl, new net.minecraft.core.BlockPos(x, y, z), 100F);
					
					com.hbm.explosion.ExplosionLarge.spawnParticles(bullet.level(), posX, posY, posZ, 45);
				}
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getMirvConfig() {
		
		BulletConfiguration bullet = getNukeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_MIRV;
		bullet.style = BulletConfiguration.STYLE_MIRV;
		bullet.velocity *= 3;
		
		bullet.bntUpdate = new EntityBulletBaseNT.IBulletUpdateBehaviorNT() {

			@Override
			public void behaveUpdate(EntityBulletBaseNT bullet) {
				
				if(bullet.level().isClientSide())
					return;
				
				if(bullet.tickCount == 15) {
					bullet.discard();
					
					for(int i = 0; i < 6; i++) {
						
						EntityBulletBaseNT nuke = new EntityBulletBaseNT(bullet.level(), NUKE_NORMAL);
						nuke.setPos(bullet.getX(), bullet.getY(), bullet.getZ());
						double mod = 0.1D;
						nuke.setDeltaMovement(bullet.level().random.nextGaussian() * mod, nuke.getDeltaMovement().y, nuke.getDeltaMovement().z);
						nuke.setDeltaMovement(nuke.getDeltaMovement().x, -0.1D, nuke.getDeltaMovement().z);
						nuke.setDeltaMovement(nuke.getDeltaMovement().x, nuke.getDeltaMovement().y, bullet.level().random.nextGaussian() * mod);
						bullet.level().addFreshEntity(nuke);
					}
				}
			}
			
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getMirvLowConfig() {
		
		BulletConfiguration bullet = getNukeLowConfig();
		
		bullet.ammo = LegacyGuns.AMMO_MIRV_LOW;
		bullet.style = BulletConfiguration.STYLE_MIRV;
		bullet.velocity *= 3;
		
		bullet.bntUpdate = new EntityBulletBaseNT.IBulletUpdateBehaviorNT() {

			@Override
			public void behaveUpdate(EntityBulletBaseNT bullet) {
				
				if(bullet.level().isClientSide())
					return;
				
				if(bullet.tickCount == 15) {
					bullet.discard();
					
					for(int i = 0; i < 6; i++) {
						
						EntityBulletBaseNT nuke = new EntityBulletBaseNT(bullet.level(), NUKE_LOW);
						nuke.setPos(bullet.getX(), bullet.getY(), bullet.getZ());
						double mod = 0.1D;
						nuke.setDeltaMovement(bullet.level().random.nextGaussian() * mod, nuke.getDeltaMovement().y, nuke.getDeltaMovement().z);
						nuke.setDeltaMovement(nuke.getDeltaMovement().x, -0.1D, nuke.getDeltaMovement().z);
						nuke.setDeltaMovement(nuke.getDeltaMovement().x, nuke.getDeltaMovement().y, bullet.level().random.nextGaussian() * mod);
						bullet.level().addFreshEntity(nuke);
					}
				}
			}
			
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getMirvHighConfig() {
		
		BulletConfiguration bullet = getNukeHighConfig();
		
		bullet.ammo = LegacyGuns.AMMO_MIRV_HIGH;
		bullet.style = BulletConfiguration.STYLE_MIRV;
		bullet.velocity *= 3;
		
		bullet.bntUpdate = new EntityBulletBaseNT.IBulletUpdateBehaviorNT() {

			@Override
			public void behaveUpdate(EntityBulletBaseNT bullet) {
				
				if(bullet.level().isClientSide())
					return;
				
				if(bullet.tickCount == 15) {
					bullet.discard();
					
					for(int i = 0; i < 6; i++) {
						
						EntityBulletBaseNT nuke = new EntityBulletBaseNT(bullet.level(), NUKE_HIGH);
						nuke.setPos(bullet.getX(), bullet.getY(), bullet.getZ());
						double mod = 0.1D;
						nuke.setDeltaMovement(bullet.level().random.nextGaussian() * mod, nuke.getDeltaMovement().y, nuke.getDeltaMovement().z);
						nuke.setDeltaMovement(nuke.getDeltaMovement().x, -0.1D, nuke.getDeltaMovement().z);
						nuke.setDeltaMovement(nuke.getDeltaMovement().x, nuke.getDeltaMovement().y, bullet.level().random.nextGaussian() * mod);
						bullet.level().addFreshEntity(nuke);
					}
				}
			}
			
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getMirvSafeConfig() {
		
		BulletConfiguration bullet = getNukeSafeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_MIRV_SAFE;
		bullet.style = BulletConfiguration.STYLE_MIRV;
		bullet.velocity *= 3;
		
		bullet.bntUpdate = new EntityBulletBaseNT.IBulletUpdateBehaviorNT() {

			@Override
			public void behaveUpdate(EntityBulletBaseNT bullet) {
				
				if(bullet.level().isClientSide())
					return;
				
				if(bullet.tickCount == 15) {
					bullet.discard();
					
					for(int i = 0; i < 6; i++) {
						
						EntityBulletBaseNT nuke = new EntityBulletBaseNT(bullet.level(), NUKE_SAFE);
						nuke.setPos(bullet.getX(), bullet.getY(), bullet.getZ());
						double mod = 0.1D;
						nuke.setDeltaMovement(bullet.level().random.nextGaussian() * mod, nuke.getDeltaMovement().y, nuke.getDeltaMovement().z);
						nuke.setDeltaMovement(nuke.getDeltaMovement().x, -0.1D, nuke.getDeltaMovement().z);
						nuke.setDeltaMovement(nuke.getDeltaMovement().x, nuke.getDeltaMovement().y, bullet.level().random.nextGaussian() * mod);
						bullet.level().addFreshEntity(nuke);
					}
				}
			}
			
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getMirvSpecialConfig() {
		
		BulletConfiguration bullet = getNukeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_MIRV_SPECIAL;
		bullet.style = BulletConfiguration.STYLE_MIRV;
		bullet.velocity *= 3;
		
		bullet.bntUpdate = new EntityBulletBaseNT.IBulletUpdateBehaviorNT() {

			@Override
			public void behaveUpdate(EntityBulletBaseNT bullet) {
				
				if(bullet.level().isClientSide())
					return;
				
				if(bullet.tickCount == 15) {
					bullet.discard();
					
					for(int i = 0; i < 24; i++) {
						
						EntityBulletBaseNT nuke = null;
						
						if(i < 6)
							nuke = new EntityBulletBaseNT(bullet.level(), NUKE_LOW);
						else if(i < 12)
							nuke = new EntityBulletBaseNT(bullet.level(), NUKE_TOTS);
						else if(i < 18)
							nuke = new EntityBulletBaseNT(bullet.level(), NUKE_NORMAL);
						else
							nuke = new EntityBulletBaseNT(bullet.level(), NUKE_AMAT);
						
						nuke.setPos(bullet.getX(), bullet.getY(), bullet.getZ());
						
						double mod = 0.25D;
						nuke.setDeltaMovement(bullet.level().random.nextGaussian() * mod, nuke.getDeltaMovement().y, nuke.getDeltaMovement().z);
						nuke.setDeltaMovement(nuke.getDeltaMovement().x, -0.1D, nuke.getDeltaMovement().z);
						nuke.setDeltaMovement(nuke.getDeltaMovement().x, nuke.getDeltaMovement().y, bullet.level().random.nextGaussian() * mod);
						bullet.level().addFreshEntity(nuke);
					}
				}
			}
			
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getBalefireConfig() {
		
		BulletConfiguration bullet = BulletConfigFactory.standardNukeConfig();
		
		bullet.ammo = LegacyGuns.AMMO_NUKE_BALEFIRE;
		bullet.style = BulletConfiguration.STYLE_BF;
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {
			@Override public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				
				if(!bullet.level().isClientSide()) {

					double posX = bullet.getX();
					double posY = bullet.getY() + 0.5;
					double posZ = bullet.getZ();
					
					if(y >= 0) {
						posX = x + 0.5;
						posY = y + 1.5;
						posZ = z + 0.5;
					}
					
					LegacySounds.play(bullet.level(), x, y, z, "hbm:weapon.mukeExplosion", 15.0F, 1.0F);
					
					com.hbm.explosion.ExplosionLarge.spawnShrapnels(bullet.level(), posX, posY, posZ, 25);
					
					// main overrode the ray resolution to 64
					LegacyExplosion.explode(bullet.level(), null, posX, posY, posZ, 15F, ExAttrib.BALEFIRE, ExAttrib.NOPARTICLE, ExAttrib.NOSOUND, ExAttrib.NODROP);
					
					if (bullet.level() instanceof net.minecraft.server.level.ServerLevel msl) com.hbm.packet.PacketWire.sendToAllAround(new com.hbm.packet.toclient.MukePayload(x, y + 0.5, z, false, true), new com.hbm.handler.threading.TargetPoint(msl, bullet.getX(), bullet.getY(), bullet.getZ(), 250));
				}
			}
		};
		
		return bullet;
	}

}
