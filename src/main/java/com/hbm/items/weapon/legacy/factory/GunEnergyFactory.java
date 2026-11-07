// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy.factory;

import com.hbm.items.weapon.legacy.LegacyExplosion;
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

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/GunEnergyFactory, value for value. */
public final class GunEnergyFactory {

    private GunEnergyFactory() {}

	
	public static GunConfiguration getChemConfig() {
		
		GunConfiguration config = new GunConfiguration();
		config.rateOfFire = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.allowsInfinity = false;
		config.ammoCap = 3_000;
		config.durability = 90_000;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.crosshair = Crosshair.CIRCLE;
		
		config.name = "Chemical Thrower";
		config.manufacturer = EnumGunManufacturer.LANGFORD;
		
		config.config = new ArrayList<Integer>();
		
		return config;
	}
	
	public static GunConfiguration getEMPConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 30;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 10;
		config.firingDuration = 0;
		config.ammoCap = 0;
		config.durability = 1500;
		config.reloadType = GunConfiguration.RELOAD_NONE;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_SPLIT;
		config.firingSound = "hbm:weapon.teslaShoot";
		
		config.name = "EMP Orb Projector";
		config.manufacturer = EnumGunManufacturer.MWT;
		
		config.config = new ArrayList<Integer>();
		config.config.add(SPECIAL_EMP);
		
		return config;
	}
	
	public static GunConfiguration getFlamerConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 1;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.reloadDuration = 20;
		config.reloadSoundEnd = false;
		config.firingDuration = 0;
		config.ammoCap = 100;
		config.durability = 1000;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CIRCLE;
		config.firingSound = "hbm:weapon.flamethrowerShoot";
		config.reloadSound = "hbm:weapon.flamerReload";
		
		config.name = "Heavy Duty Flamer";
		config.manufacturer = EnumGunManufacturer.MWT;

		config.comment.add("Dragon-slaying: Advanced techniques, part 1:");
		config.comment.add("Try not to get eaten by the dragon.");
		config.comment.add("");
		config.comment.add("Hope that helps.");
		
		config.config = new ArrayList<Integer>();
		config.config.add(FLAMER_NORMAL);
		config.config.add(FLAMER_NAPALM);
		config.config.add(FLAMER_WP);
		config.config.add(FLAMER_VAPORIZER);
		config.config.add(FLAMER_GAS);
		
		return config;
	}
	
	public static GunConfiguration getZOMGConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 1;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.reloadDuration = 10;
		config.reloadSoundEnd = false;
		config.firingDuration = 0;
		config.durability = 100000;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.ammoCap = 1000;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_ARROWS;
		config.firingSound = "hbm:weapon.zomgShoot";
		config.reloadSound = "hbm:weapon.b92Reload";
		
		config.name = "EMC101 Prismatic Negative Energy Cannon";
		config.manufacturer = EnumGunManufacturer.MWT;

		config.comment.add("Taste the rainbow!");
		
		config.config = new ArrayList<Integer>();
		config.config.add(ZOMG_BOLT);
		
		return config;
	}

	public static GunConfiguration getExtConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 1;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.reloadDuration = 20;
		config.reloadSoundEnd = false;
		config.firingDuration = 0;
		config.ammoCap = 300; //good for 15 seconds of continued spray
		config.durability = 10000;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CIRCLE;
		config.firingSound = "hbm:weapon.extinguisher";
		config.reloadSound = "hbm:weapon.flamerReload";
		
		config.name = "PROTEX Fire Exinguisher 6kg";
		config.manufacturer = EnumGunManufacturer.GLORIA;
		
		config.config = new ArrayList<Integer>();
		config.config.add(FEXT_NORMAL);
		config.config.add(FEXT_FOAM);
		config.config.add(FEXT_SAND);
		
		return config;
	}
	
	public static GunConfiguration getCoilgunConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 5;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_MANUAL;
		config.reloadDuration = 20;
		config.firingDuration = 0;
		config.ammoCap = 1;
		config.durability = 2_500;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.CIRCLE;
		config.firingSound = "hbm:weapon.coilgunShoot";
		config.reloadSoundEnd = false;
		config.reloadSound = "hbm:weapon.coilgunReload";
		
		config.name = "ArmsKore Coilgun";
		config.manufacturer = EnumGunManufacturer.DRG;
		
		config.config = new ArrayList<Integer>();
		config.config.add(COIL_NORMAL);
		config.config.add(COIL_DU);
		config.config.add(COIL_RUBBER);
		
		return config;
	}

	public static GunConfiguration getCryoCannonConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		config.rateOfFire = 1;
		config.roundsPerCycle = 1;
		config.gunMode = GunConfiguration.MODE_NORMAL;
		config.firingMode = GunConfiguration.FIRE_AUTO;
		config.firingDuration = 0;
		config.ammoCap = 1_000;
		config.durability = 10_000;
		config.reloadType = GunConfiguration.RELOAD_FULL;
		config.allowsInfinity = true;
		config.crosshair = Crosshair.L_CIRCLE;
		
		config.name = "Cryo Cannon";
		config.manufacturer = EnumGunManufacturer.DRG;
		
		config.config = new ArrayList<Integer>();
		config.config.add(CRYO_NORMAL);
		
		return config;
	}
	
	public static GunConfiguration getVortexConfig() {
		
		GunConfiguration config = new GunConfiguration();
		
		return config;
		
	}
	
	public static BulletConfiguration getOrbusConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.ammo = LegacyGuns.GUN_EMP_AMMO;
		
		bullet.velocity = 1F;
		bullet.spread = 0.0F;
		bullet.wear = 10;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.dmgMin = 10;
		bullet.dmgMax = 12;
		bullet.gravity = 0D;
		bullet.maxAge = 100;
		bullet.doesRicochet = false;
		bullet.doesPenetrate = true;
		bullet.doesBreakGlass = false;
		bullet.style = BulletConfiguration.STYLE_ORB;
		bullet.plink = BulletConfiguration.PLINK_NONE;
		bullet.emp = 10;
		
		bullet.damageType = ModDamageTypes.ELECTRIC;
		bullet.dmgProj = false;
		bullet.dmgBypass = true;
		
		bullet.effects = new ArrayList<>();
		bullet.effects.add(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 1));
		bullet.effects.add(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 20, 4));
		
		return bullet;
	}
	
	public static BulletConfiguration getCoilConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.ammo = LegacyGuns.AMMO_COILGUN;
		
		bullet.velocity = 7.5F;
		bullet.spread = 0.0F;
		bullet.wear = 10;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.dmgMin = 35;
		bullet.dmgMax = 45;
		bullet.gravity = 0D;
		bullet.maxAge = 50;
		bullet.doesPenetrate = true;
		bullet.isSpectral = true;

		bullet.style = bullet.STYLE_BOLT;
		bullet.trail = bullet.BOLT_NIGHTMARE;
		bullet.vPFX = "fireworks";
		
		bullet.bntUpdate = (entity) -> breakInPath(entity, 1.25F);
		
		return bullet;
	}
	
	public static BulletConfiguration getCoilDUConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.ammo = LegacyGuns.AMMO_COILGUN_DU;
		
		bullet.velocity = 7.5F;
		bullet.spread = 0.0F;
		bullet.wear = 25;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.dmgMin = 65;
		bullet.dmgMax = 80;
		bullet.gravity = 0D;
		bullet.maxAge = 50;
		bullet.doesPenetrate = true;
		bullet.isSpectral = true;

		bullet.style = bullet.STYLE_BOLT;
		bullet.trail = bullet.BOLT_NIGHTMARE;
		bullet.vPFX = "fireworks";
		
		bullet.bntUpdate = (entity) -> breakInPath(entity, 2.5F);
		
		return bullet;
	}
	
	public static BulletConfiguration getCoilRubberConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.ammo = LegacyGuns.AMMO_COILGUN_RUBBER;
		
		bullet.velocity = 5F;
		bullet.spread = 0.0F;
		bullet.wear = 10;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.dmgMin = 10;
		bullet.dmgMax = 20;
		bullet.gravity = 0D;
		bullet.maxAge = 50;
		bullet.doesPenetrate = false;
		bullet.isSpectral = false;
		bullet.doesRicochet = true;
		bullet.ricochetAngle = 30;
		bullet.HBRC = 90;
		bullet.LBRC = 100;
		bullet.bounceMod = 1;
		bullet.selfDamageDelay = 2;

		bullet.style = bullet.STYLE_PELLET;
		
		bullet.bntHurt = (entity, hit) -> {
			// main pushed the hit and sent players a knockback packet; hurtMarked syncs the push here
			Vec3 vec = entity.getDeltaMovement().normalize().scale(10);
			hit.setDeltaMovement(hit.getDeltaMovement().add(vec));
			hit.hurtMarked = true;
		};
		
		return bullet;
	}
	
	public static void breakInPath(EntityBulletBaseNT entity, float threshold) {
		
		if(entity.level().isClientSide()) return;
		
		Vec3 vec = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
		double motion = Math.max(vec.length(), 0.1);
		vec = vec.normalize();
		
		for(double d = 0; d < motion; d += 0.5) {
			net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(entity.getX() - vec.x * d, entity.getY() - vec.y * d, entity.getZ() - vec.z * d);
			net.minecraft.world.level.block.state.BlockState b = entity.level().getBlockState(pos);
			float hardness = b.getDestroySpeed(entity.level(), pos);
			if(!b.isAir() && hardness >= 0 && hardness < threshold) {
				entity.level().destroyBlock(pos, false);
			}
		}
	}
	
	public static BulletConfiguration getFlameConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.ammo = LegacyGuns.AMMO_FUEL;
		bullet.ammoCount = 100;
		
		bullet.velocity = 0.75F;
		bullet.spread = 0.025F;
		bullet.wear = 1;
		bullet.bulletsMin = 3;
		bullet.bulletsMax = 5;
		bullet.dmgMin = 2;
		bullet.dmgMax = 4;
		bullet.gravity = 0.01D;
		bullet.maxAge = 60;
		bullet.doesRicochet = false;
		bullet.doesPenetrate = true;
		bullet.doesBreakGlass = false;
		bullet.style = BulletConfiguration.STYLE_NONE;
		bullet.plink = BulletConfiguration.PLINK_NONE;
		bullet.vPFX = "flame";
		bullet.incendiary = 10;
		
		bullet.damageType = ModDamageTypes.FLAMETHROWER;
		bullet.dmgProj = false;
		bullet.dmgFire = true;
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				
				if(!bullet.level().isClientSide()) {
					LegacyFx.vanillaBurst(bullet.level(), bullet.getX(), bullet.getY(), bullet.getZ(), ParticleTypes.FLAME, 15, 0.1D);
				}
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getNapalmConfig() {
		
		BulletConfiguration bullet = getFlameConfig();
		
		bullet.ammo = LegacyGuns.AMMO_FUEL_NAPALM;
		bullet.wear = 2;
		bullet.dmgMin = 4;
		bullet.dmgMax = 6;
		bullet.maxAge = 200;
		
		return bullet;
	}
	
	public static BulletConfiguration getPhosphorusConfig() {
		
		BulletConfiguration bullet = getFlameConfig();
		
		bullet.ammo = LegacyGuns.AMMO_FUEL_PHOSPHORUS;
		bullet.wear = 2;
		bullet.spread = 0.0F;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		bullet.dmgMin = 4;
		bullet.dmgMax = 6;
		bullet.maxAge = 200;
		bullet.vPFX = "smoke";
		
		bullet.bntImpact = BulletConfigFactory.getPhosphorousEffect(5, 60 * 20, 25, 0.25, 0.1F);
		
		return bullet;
	}
	
	public static BulletConfiguration getVaporizerConfig() {
		
		BulletConfiguration bullet = getFlameConfig();
		
		bullet.ammo = LegacyGuns.AMMO_FUEL_VAPORIZER;
		bullet.wear = 4;
		bullet.spread = 0.25F;
		bullet.bulletsMin = 8;
		bullet.bulletsMax = 10;
		bullet.dmgMin = 6;
		bullet.dmgMax = 10;
		bullet.maxAge = 15;
		bullet.vPFX = "flame";
		bullet.incendiary = 0;
		
		bullet.dmgBypass = true;
		
		// main cleared the curative items (milk does not wash it off); a plain effect here
		MobEffectInstance eff = new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HbmPotion.phosphorus.get()), 20 * 20, 0, true, true);
		bullet.effects = new ArrayList<>();
		bullet.effects.add(new MobEffectInstance(eff));
		
		return bullet;
	}
	
	public static BulletConfiguration getGasConfig() {
		
		BulletConfiguration bullet = getFlameConfig();
		
		bullet.ammo = LegacyGuns.AMMO_FUEL_GAS;
		bullet.wear = 1;
		bullet.spread = 0.05F;
		bullet.gravity = 0D;
		bullet.bulletsMin = 5;
		bullet.bulletsMax = 7;
		bullet.dmgMin = 0;
		bullet.dmgMax = 0;
		bullet.vPFX = "cloud";
		bullet.incendiary = 0;
		
		bullet.dmgFire = false;
		
		bullet.bntImpact = BulletConfigFactory.getGasEffect(5, 60 * 20);
		
		return bullet;
	}
	
	public static BulletConfiguration getFextConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.ammo = LegacyGuns.AMMO_FIREEXT_LEGACY;
		bullet.ammoCount = 300;
		
		bullet.velocity = 0.75F;
		bullet.spread = 0.025F;
		bullet.wear = 1;
		bullet.bulletsMin = 2;
		bullet.bulletsMax = 3;
		bullet.dmgMin = 0;
		bullet.dmgMax = 0;
		bullet.gravity = 0.04D;
		bullet.maxAge = 100;
		bullet.doesRicochet = false;
		bullet.doesPenetrate = true;
		bullet.doesBreakGlass = false;
		bullet.style = BulletConfiguration.STYLE_NONE;
		bullet.plink = BulletConfiguration.PLINK_NONE;
		
		bullet.bntHurt = (bulletEntity, target) -> { target.clearFire(); };
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				
				if(!bullet.level().isClientSide()) {
					
					int ix = (int)Math.floor(bullet.getX());
					int iy = (int)Math.floor(bullet.getY());
					int iz = (int)Math.floor(bullet.getZ());
					
					boolean fizz = false;
					
					for(int i = -1; i <= 1; i++) {
						for(int j = -1; j <= 1; j++) {
							for(int k = -1; k <= 1; k++) {
								
								if(bullet.level().getBlockState(new net.minecraft.core.BlockPos(ix + i, iy + j, iz + k)).is(net.minecraft.world.level.block.Blocks.FIRE)) {
									bullet.level().removeBlock(new net.minecraft.core.BlockPos(ix + i, iy + j, iz + k), false);
									fizz = true;
								}
							}
						}
					}
					
					if(bullet.level() instanceof net.minecraft.server.level.ServerLevel srv) {
						net.minecraft.core.BlockPos cp = com.hbm.blocks.multiblock.MultiblockSurface.indexedCore(srv, new net.minecraft.core.BlockPos(ix, iy, iz));
						if(cp != null && srv.getBlockEntity(cp) instanceof com.hbm.interfaces.IRepairable rep) {
							rep.tryExtinguish(srv, new net.minecraft.core.BlockPos(ix, iy, iz), com.hbm.interfaces.IRepairable.EnumExtinguishType.WATER);
						}
					}
					
					if(fizz)
						LegacySounds.play(bullet.level(), bullet.getX(), bullet.getY(), bullet.getZ(), "random.fizz", 1.0F, 1.5F + bullet.level().random.nextFloat() * 0.5F);
				}
			}
		};
		
		bullet.bntUpdate = new EntityBulletBaseNT.IBulletUpdateBehaviorNT() {

			@Override
			public void behaveUpdate(EntityBulletBaseNT bullet) {
				
				if(bullet.level().isClientSide()) {
					
					bullet.level().addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState()), bullet.getX(), bullet.getY(), bullet.getZ(), bullet.getDeltaMovement().x + bullet.level().random.nextGaussian() * 0.05, bullet.getDeltaMovement().y - 0.2 + bullet.level().random.nextGaussian() * 0.05, bullet.getDeltaMovement().z + bullet.level().random.nextGaussian() * 0.05);
				} else {

					int x = (int)Math.floor(bullet.getX());
					int y = (int)Math.floor(bullet.getY());
					int z = (int)Math.floor(bullet.getZ());
					
					if(bullet.level().getBlockState(new net.minecraft.core.BlockPos(x, y, z)).is(com.hbm.blocks.ModBlocks.VOLCANIC_LAVA_BLOCK.get()) && bullet.level().getFluidState(new net.minecraft.core.BlockPos(x, y, z)).isSource()) {
						bullet.level().setBlockAndUpdate(new net.minecraft.core.BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState());
						bullet.discard();
					}
				}
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getFextFoamConfig() {
		
		BulletConfiguration bullet = getFextConfig();
		
		bullet.ammo = LegacyGuns.AMMO_FIREEXT_FOAM_LEGACY;
		bullet.spread = 0.05F;
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				
				if(!bullet.level().isClientSide()) {
					
					int ix = (int)Math.floor(bullet.getX());
					int iy = (int)Math.floor(bullet.getY());
					int iz = (int)Math.floor(bullet.getZ());
					
					boolean fizz = false;
					
					for(int i = -1; i <= 1; i++) {
						for(int j = -1; j <= 1; j++) {
							for(int k = -1; k <= 1; k++) {
								
								if(bullet.level().getBlockState(new net.minecraft.core.BlockPos(ix + i, iy + j, iz + k)).is(net.minecraft.world.level.block.Blocks.FIRE)) {
									bullet.level().removeBlock(new net.minecraft.core.BlockPos(ix + i, iy + j, iz + k), false);
									fizz = true;
								}
							}
						}
					}
					
					net.minecraft.world.level.block.state.BlockState bs = bullet.level().getBlockState(new net.minecraft.core.BlockPos(ix, iy, iz));
					net.minecraft.world.level.block.Block b = bs.getBlock();
					
					if(bullet.level() instanceof net.minecraft.server.level.ServerLevel srv) {
						net.minecraft.core.BlockPos cp = com.hbm.blocks.multiblock.MultiblockSurface.indexedCore(srv, new net.minecraft.core.BlockPos(ix, iy, iz));
						if(cp != null && srv.getBlockEntity(cp) instanceof com.hbm.interfaces.IRepairable rep) {
							rep.tryExtinguish(srv, new net.minecraft.core.BlockPos(ix, iy, iz), com.hbm.interfaces.IRepairable.EnumExtinguishType.FOAM);
						return;
						}
					}
					
					if(bs.canBeReplaced() && com.hbm.blocks.ModBlocks.FOAM_LAYER.get().defaultBlockState().canSurvive(bullet.level(), new net.minecraft.core.BlockPos(ix, iy, iz))) {
						
						if(b != com.hbm.blocks.ModBlocks.FOAM_LAYER.get()) {
							bullet.level().setBlockAndUpdate(new net.minecraft.core.BlockPos(ix, iy, iz), com.hbm.blocks.ModBlocks.FOAM_LAYER.get().defaultBlockState());
						} else {
							// main's layer meta is the layer count minus one
							int meta = bs.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) - 1;
							
							if(meta < 6)
								bullet.level().setBlock(new net.minecraft.core.BlockPos(ix, iy, iz), bs.setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, meta + 2), 3);
							else
								bullet.level().setBlockAndUpdate(new net.minecraft.core.BlockPos(ix, iy, iz), com.hbm.blocks.ModBlocks.BLOCK_FOAM.get().defaultBlockState());
						}
					}
					
					if(fizz)
						LegacySounds.play(bullet.level(), bullet.getX(), bullet.getY(), bullet.getZ(), "random.fizz", 1.0F, 1.5F + bullet.level().random.nextFloat() * 0.5F);
				}
			}
		};
		
		bullet.bntUpdate = new EntityBulletBaseNT.IBulletUpdateBehaviorNT() {

			@Override
			public void behaveUpdate(EntityBulletBaseNT bullet) {
				
				if(bullet.level().isClientSide()) {
					
					bullet.level().addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, com.hbm.blocks.ModBlocks.BLOCK_FOAM.get().defaultBlockState()), bullet.getX(), bullet.getY(), bullet.getZ(), bullet.getDeltaMovement().x + bullet.level().random.nextGaussian() * 0.05, bullet.getDeltaMovement().y - 0.2 + bullet.level().random.nextGaussian() * 0.05, bullet.getDeltaMovement().z + bullet.level().random.nextGaussian() * 0.05);
				}
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getFextSandConfig() {
		
		BulletConfiguration bullet = getFextConfig();
		
		bullet.ammo = LegacyGuns.AMMO_FIREEXT_SAND_LEGACY;
		bullet.spread = 0.1F;
		
		bullet.bntHurt = null; // does not extinguish entities
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				
				if(!bullet.level().isClientSide()) {
					
					int ix = (int)Math.floor(bullet.getX());
					int iy = (int)Math.floor(bullet.getY());
					int iz = (int)Math.floor(bullet.getZ());
					
					net.minecraft.world.level.block.state.BlockState bs = bullet.level().getBlockState(new net.minecraft.core.BlockPos(ix, iy, iz));
					net.minecraft.world.level.block.Block b = bs.getBlock();
					
					if(bullet.level() instanceof net.minecraft.server.level.ServerLevel srv) {
						net.minecraft.core.BlockPos cp = com.hbm.blocks.multiblock.MultiblockSurface.indexedCore(srv, new net.minecraft.core.BlockPos(ix, iy, iz));
						if(cp != null && srv.getBlockEntity(cp) instanceof com.hbm.interfaces.IRepairable rep) {
							rep.tryExtinguish(srv, new net.minecraft.core.BlockPos(ix, iy, iz), com.hbm.interfaces.IRepairable.EnumExtinguishType.SAND);
						return;
						}
					}
					
					if((bs.canBeReplaced() || b == com.hbm.blocks.ModBlocks.SAND_BORON_LAYER.get()) && com.hbm.blocks.ModBlocks.SAND_BORON_LAYER.get().defaultBlockState().canSurvive(bullet.level(), new net.minecraft.core.BlockPos(ix, iy, iz))) {
						
						if(b != com.hbm.blocks.ModBlocks.SAND_BORON_LAYER.get()) {
							bullet.level().setBlockAndUpdate(new net.minecraft.core.BlockPos(ix, iy, iz), com.hbm.blocks.ModBlocks.SAND_BORON_LAYER.get().defaultBlockState());
						} else {
							// main's layer meta is the layer count minus one
							int meta = bs.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS) - 1;
							
							if(meta < 6)
								bullet.level().setBlock(new net.minecraft.core.BlockPos(ix, iy, iz), bs.setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, meta + 2), 3);
							else
								bullet.level().setBlockAndUpdate(new net.minecraft.core.BlockPos(ix, iy, iz), com.hbm.blocks.ModBlocks.SAND_BORON.get().defaultBlockState());
						}
						
						if(bs.is(net.minecraft.world.level.block.Blocks.FIRE))
							LegacySounds.play(bullet.level(), bullet.getX(), bullet.getY(), bullet.getZ(), "random.fizz", 1.0F, 1.5F + bullet.level().random.nextFloat() * 0.5F);
					}
				}
			}
		};
		
		bullet.bntUpdate = new EntityBulletBaseNT.IBulletUpdateBehaviorNT() {

			@Override
			public void behaveUpdate(EntityBulletBaseNT bullet) {
				
				if(bullet.level().isClientSide()) {
					
					bullet.level().addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, com.hbm.blocks.ModBlocks.SAND_BORON.get().defaultBlockState()), bullet.getX(), bullet.getY(), bullet.getZ(), bullet.getDeltaMovement().x + bullet.level().random.nextGaussian() * 0.1, bullet.getDeltaMovement().y - 0.2 + bullet.level().random.nextGaussian() * 0.1, bullet.getDeltaMovement().z + bullet.level().random.nextGaussian() * 0.1);
				}
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getZOMGBoltConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		bullet.ammo = com.hbm.items.ModItems.NUGGET_EUPHEMIUM;
		bullet.ammoCount = 1000;
		bullet.wear = 1;
		bullet.velocity = 1F;
		bullet.spread = 0.125F;
		bullet.maxAge = 100;
		bullet.gravity = 0D;
		bullet.bulletsMin = 5;
		bullet.bulletsMax = 5;
		bullet.dmgMin = 10000;
		bullet.dmgMax = 25000;
		bullet.liveAfterImpact = true;
		
		bullet.damageType = ModDamageTypes.SUBATOMIC;
		bullet.dmgProj = false;
		bullet.dmgBypass = true;

		bullet.style = bullet.STYLE_BOLT;
		bullet.trail = bullet.BOLT_ZOMG;
		
		bullet.effects = new ArrayList<>();
		bullet.effects.add(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HbmPotion.bang.get()), 10 * 20, 0));
		
		bullet.bntImpact = new EntityBulletBaseNT.IBulletImpactBehaviorNT() {

			@Override
			public void behaveBlockHit(EntityBulletBaseNT bullet, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
 int x = pos == null ? -1 : pos.getX(), y = pos == null ? -1 : pos.getY(), z = pos == null ? -1 : pos.getZ();
 int sideHit = side == null ? -1 : side.get3DDataValue();
				
				if(!bullet.level().isClientSide()) {
					LegacyExplosion.zomg(bullet.level(), (int)Math.floor(bullet.getX()), (int)Math.floor(bullet.getY()), (int)Math.floor(bullet.getZ()), 5);
					LegacySounds.play(bullet.level(), bullet.getX(), bullet.getY(), bullet.getZ(), "hbm:entity.bombDet", 5.0F, 1.0F);
    				com.hbm.explosion.ExplosionLarge.spawnParticles(bullet.level(), bullet.getX(), bullet.getY(), bullet.getZ(), 5);
				}
			}
		};
		
		return bullet;
	}
	
	public static BulletConfiguration getCryoConfig() {
		BulletConfiguration bullet = new BulletConfiguration();
		bullet.ammo = LegacyGuns.GUN_CRYOLATOR_AMMO;
		bullet.ammoCount = 100;
		bullet.bulletsMin = 1;
		bullet.bulletsMax = 1;
		return bullet;
	}

	public static BulletConfiguration getTurbineConfig() {
		
		BulletConfiguration bullet = new BulletConfiguration();
		
		// main's placeholder "nothing" item: this config never takes ammo
		bullet.ammo = () -> net.minecraft.world.item.Items.AIR;
		bullet.dmgMin = 100;
		bullet.dmgMax = 150;
		bullet.velocity = 1F;
		bullet.gravity = 0.0;
		bullet.maxAge = 200;
		bullet.style = bullet.STYLE_BLADE;
		bullet.destroysBlocks = true;
		bullet.doesRicochet = false;
		
		return bullet;
	}

	public static BulletConfiguration getTurretConfig() {
		BulletConfiguration bullet = getFlameConfig();
		bullet.spread *= 2F;
		bullet.gravity = 0.0025D;
		return bullet;
	}
}
