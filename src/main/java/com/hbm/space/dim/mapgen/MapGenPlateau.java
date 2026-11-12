// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.mapgen;

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



public class MapGenPlateau extends MapGenBase {

	public Block stoneBlock;
	public Block surfrock;
	public Block fillblock;

	public Noise.Perlin plateauNoise;
	public double noiseScale = 0.05;
	public int maxPlateauAddition = 12;
	public int stepHeight = 6;
	public int topsoilThickness = 2;

	public BiomeGenBase applyToBiome;

	public MapGenPlateau(World world) {
		this.plateauNoise = new Noise.Perlin(world.rand, 4);
	}

	@Override
	public void func_151539_a(Object provider, World world, int chunkX, int chunkZ, Block[] blocks) {
		int[][] plateauTops = new int[16][16];
		int[][] baseHeights = new int[16][16];
		for(int localX = 0; localX < 16; localX++) {
			for(int localZ = 0; localZ < 16; localZ++) {
				int baseHeight = getSurfaceHeight(blocks, localX, localZ);
				baseHeights[localX][localZ] = baseHeight;
				double noiseVal = plateauNoise.func_151601_a((chunkX * 16 + localX) * noiseScale, (chunkZ * 16 + localZ) * noiseScale);
				int plateauAddition = (int)(((noiseVal + 1) / 2.0) * maxPlateauAddition);
				plateauAddition = (plateauAddition / stepHeight) * stepHeight;
				plateauTops[localX][localZ] = baseHeight + plateauAddition;
			}
		}

		for(int localX = 0; localX < 16; localX++) {
			for(int localZ = 0; localZ < 16; localZ++) {
				if(applyToBiome != null) {
					BiomeGenBase biome = world.getBiomeGenForCoords(localX + chunkX * 16, localZ + chunkZ * 16);
					if(biome != applyToBiome) continue;
				}
				int baseHeight = baseHeights[localX][localZ];
				int plateauTop = plateauTops[localX][localZ];
				for(int y = baseHeight + 1; y < 256; y++) {
					int index = (localX * 16 + localZ) * 256 + y;
					if(y < plateauTop - topsoilThickness) {
						blocks[index] = stoneBlock;
					} else if(y < plateauTop) {
						boolean sameLeft  = (localX - 1 < 0)    || (plateauTops[localX - 1][localZ] == plateauTop);
						boolean sameRight = (localX + 1 >= 16)  || (plateauTops[localX + 1][localZ] == plateauTop);
						boolean sameFront = (localZ - 1 < 0)    || (plateauTops[localX][localZ - 1] == plateauTop);
						boolean sameBack  = (localZ + 1 >= 16)  || (plateauTops[localX][localZ + 1] == plateauTop);

						if(y == plateauTop - 1 && sameLeft && sameRight && sameFront && sameBack) {
							blocks[index] = fillblock;
						} else {
							blocks[index] = surfrock;
						}
					} else {
						blocks[index] = Blocks.AIR;
					}
				}
			}
		}
	}

	private int getSurfaceHeight(Block[] blocks, int localX, int localZ) {
		int baseHeight = 0;
		for(int y = 255; y >= 0; y--) {
			int index = (localX * 16 + localZ) * 256 + y;
			if(blocks[index] != Blocks.AIR) {
				baseHeight = y;
				break;
			}
		}
		return baseHeight;
	}

}
