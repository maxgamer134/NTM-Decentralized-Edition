// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

/*******************************************************************************
 * Copyright 2015 SteveKunG - More Planets Mod
 *
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/

package com.hbm.space.dim.laythe.biome;

import com.hbm.space.dim.legacy.*;
import com.hbm.space.dim.legacy.Layers.*;
import com.hbm.space.dim.legacy.Features.*;
import com.hbm.space.dim.legacy.LegacyConfig.*;
import com.hbm.space.dim.Planet;
import java.util.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;
import java.util.function.*;
import java.util.stream.*;
import com.hbm.util.BobMathUtil;
import com.hbm.space.dim.*;
import com.hbm.space.dim.Ike.*;
import com.hbm.space.dim.dres.*;
import com.hbm.space.dim.dres.biome.*;
import com.hbm.space.dim.dres.genlayer.*;
import com.hbm.space.dim.duna.*;
import com.hbm.space.dim.duna.biome.*;
import com.hbm.space.dim.duna.genlayer.*;
import com.hbm.space.dim.eve.*;
import com.hbm.space.dim.eve.biome.*;
import com.hbm.space.dim.eve.genlayer.*;
import com.hbm.space.dim.laythe.*;
import com.hbm.space.dim.laythe.genlayer.*;
import com.hbm.space.dim.legacy.*;
import com.hbm.space.dim.mapgen.*;
import com.hbm.space.dim.minmus.*;
import com.hbm.space.dim.minmus.biome.*;
import com.hbm.space.dim.minmus.genlayer.*;
import com.hbm.space.dim.moho.*;
import com.hbm.space.dim.moho.biome.*;
import com.hbm.space.dim.moho.genlayer.*;
import com.hbm.space.dim.moon.*;
import com.hbm.space.dim.noise.*;
import com.hbm.space.dim.tekto.*;
import com.hbm.space.dim.tekto.GenLayerTekto.*;
import com.hbm.space.dim.tekto.biome.*;
import com.hbm.space.dim.thatmo.*;





public abstract class BiomeGenBaseLaythe extends BiomeGenBaseCelestial {

	public static final BiomeGenBase laytheIsland = new BiomeGenLaytheIslands(SpaceConfig.laytheBiome);
	public static final BiomeGenBase laytheOcean = new BiomeGenLaytheOcean(SpaceConfig.laytheOceanBiome);
	public static final BiomeGenBase laythePolar = new BiomeGenLaythePolar(SpaceConfig.laythePolarBiome);
	public static final BiomeGenBase laytheCoast = new BiomeGenLaytheCoast(SpaceConfig.laytheCoastBiome);

	public BiomeGenBaseLaythe(int id) {
		super(id);
		this.waterColorMultiplier = 0x5b009a;

		// entities come with the entity port: this.waterCreatures.add(new BiomeGenBase.SpawnListEntry(EntityScutterfish.class, 10, 4, 4));
		// entities come with the entity port: this.waterCreatures.add(new BiomeGenBase.SpawnListEntry(EntitySifterEel.class, 10, 1, 3));
		
		BiomeDecoratorCelestial decorator = new BiomeDecoratorCelestial(Blocks.STONE);
		decorator.waterPlantsPerChunk = 32;
		// decorator.seaLevel = 96;
		this.theBiomeDecorator = decorator;
		this.theBiomeDecorator.generateLakes = false;

		this.topBlock = LegacyBlocks.b("laythe_silt");
		this.fillerBlock = LegacyBlocks.b("laythe_silt");
	}

	// Same as vanilla but without the gravel
	@Override
	public void genTerrainBlocks(World world, Random rand, Block[] blocks, byte[] meta, int x, int z, double noise) {
		Block block = this.topBlock;
		byte b0 = (byte) (this.field_150604_aj & 255);
		Block block1 = this.fillerBlock;
		int k = -1;
		int l = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
		int i1 = x & 15;
		int j1 = z & 15;
		int k1 = blocks.length / 256;

		for(int l1 = 319; l1 >= -64; --l1) {
			int i2 = (j1 * 16 + i1) * k1 + l1 - ChunkProviderCelestial.MIN_Y;

			if(l1 <= -64 + rand.nextInt(5)) {
				blocks[i2] = Blocks.BEDROCK;
			} else {
				Block block2 = blocks[i2];

				if(block2 != null && Material.of(block2) != Material.air) {
					if(block2 == Blocks.STONE) {
						if(k == -1) {
							if(l <= 0) {
								block = null;
								b0 = 0;
								block1 = Blocks.STONE;
							} else if(l1 >= 59 && l1 <= 64) {
								block = this.topBlock;
								b0 = (byte) (this.field_150604_aj & 255);
								block1 = this.fillerBlock;
							}

							if(l1 < 63 && (block == null || Material.of(block) == Material.air)) {
								if(this.getFloatTemperature(x, l1, z) < 0.15F) {
									block = Blocks.ICE;
									b0 = 0;
								} else {
									block = Blocks.WATER;
									b0 = 0;
								}
							}

							k = l;

							if(l1 >= 62) {
								blocks[i2] = block;
								meta[i2] = b0;
							} else if(l1 < 56 - l) {
								block = null;
								block1 = Blocks.STONE;
								blocks[i2] = LegacyBlocks.b("laythe_silt");
							} else {
								blocks[i2] = block1;
							}
						} else if(k > 0) {
							--k;
							blocks[i2] = block1;

							if(k == 0 && block1 == Blocks.SAND) {
								k = rand.nextInt(4) + Math.max(0, l1 - 63);
								block1 = Blocks.SANDSTONE;
							}
						}
					}
				} else {
					k = -1;
				}
			}
		}
	}
}