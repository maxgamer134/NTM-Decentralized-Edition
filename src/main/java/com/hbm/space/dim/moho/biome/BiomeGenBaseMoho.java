// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.moho.biome;

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
import com.hbm.space.dim.laythe.biome.*;
import com.hbm.space.dim.laythe.genlayer.*;
import com.hbm.space.dim.legacy.*;
import com.hbm.space.dim.mapgen.*;
import com.hbm.space.dim.minmus.*;
import com.hbm.space.dim.minmus.biome.*;
import com.hbm.space.dim.minmus.genlayer.*;
import com.hbm.space.dim.moho.*;
import com.hbm.space.dim.moho.genlayer.*;
import com.hbm.space.dim.moon.*;
import com.hbm.space.dim.noise.*;
import com.hbm.space.dim.tekto.*;
import com.hbm.space.dim.tekto.GenLayerTekto.*;
import com.hbm.space.dim.tekto.biome.*;
import com.hbm.space.dim.thatmo.*;





public abstract class BiomeGenBaseMoho extends BiomeGenBaseCelestial {

	public static final BiomeGenBase mohoCrag = new BiomeGenMohoCrag(SpaceConfig.mohoBiome);
	public static final BiomeGenBase mohoBasalt = new BiomeGenMohoBasalt(SpaceConfig.mohoBasaltBiome);
	public static final BiomeGenBase mohoLavaSea = new BiomeGenMohoLavaSea(SpaceConfig.mohoLavaBiome);
	public static final BiomeGenBase mohoPlateau = new BiomeGenMohoPlateau(SpaceConfig.mohoPlateauBiome);

	public BiomeGenBaseMoho(int id) {
		super(id);
		this.setDisableRain();

		BiomeDecoratorCelestial decorator = new BiomeDecoratorCelestial(LegacyBlocks.b("moho_stone"));
		decorator.lavaCount = 50;
		this.theBiomeDecorator = decorator;

		this.setTemperatureRainfall(1.0F, 0.0F);
	}

	@Override
	public void genTerrainBlocks(World world, Random rand, Block[] blocks, byte[] meta, int x, int z, double noise) {
		Block block = this.topBlock;
		byte b0 = (byte) (this.field_150604_aj & 255);
		Block block1 = this.fillerBlock;
		int k = -1;
		int l = (int) (noise / 8.0D + 8.0D + rand.nextDouble() * 0.50D);
		int bx = x & 15;
		int bz = z & 15;
		int s = blocks.length / 256;

		for(int by = 319; by >= -64; --by) {
			int i = (bz * 16 + bx) * s + by - ChunkProviderCelestial.MIN_Y;

			if(by <= -64 + rand.nextInt(5)) {
				blocks[i] = Blocks.BEDROCK;
			} else {
				Block block2 = blocks[i];

				if(block2 != null && Material.of(block2) != Material.air) {
					if(block2 == LegacyBlocks.b("moho_stone")) {
						if(k == -1) {
							if(l <= 0) {
								block = null;
								b0 = 0;
								block1 = LegacyBlocks.b("moho_stone");
							} else if(by >= 59 && by <= 64) {
								block = this.topBlock;
								b0 = (byte) (this.field_150604_aj & 255);
								block1 = this.fillerBlock;
							}

							if(by < 63 && (block == null || Material.of(block) == Material.air)) {
								if(this.getFloatTemperature(x, by, z) < 0.15F) {
									block = this.topBlock;
									b0 = 0;
								} else {
									block = this.topBlock;
									b0 = 0;
								}
							}

							k = l;

							if(by >= 62) {
								blocks[i] = block;
								meta[i] = b0;
							} else if(by < 56 - l) {
								block = null;
								block1 = LegacyBlocks.b("moho_stone");
								blocks[i] = LegacyBlocks.b("basalt");
							} else {
								blocks[i] = block1;
							}
						} else if(k > 0) {
							--k;
							blocks[i] = block1;

							if(k == 0 && block1 == Blocks.SAND) {
								k = rand.nextInt(4) + Math.max(0, by - 63);
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
