// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.eve.genlayer;

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
import com.hbm.space.dim.laythe.*;
import com.hbm.space.dim.laythe.biome.*;
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




public class GenLayerEveRiverMix extends RiverMix {

	private GenLayer biomePatternGeneratorChain;
	private GenLayer riverPatternGeneratorChain;

	public GenLayerEveRiverMix(long seed, GenLayer biomePatternGeneratorChain, GenLayer riverPatternGeneratorChain) {
		super(seed, biomePatternGeneratorChain, riverPatternGeneratorChain);

		this.biomePatternGeneratorChain = biomePatternGeneratorChain;
		this.riverPatternGeneratorChain = riverPatternGeneratorChain;
	}

	@Override
	public void initWorldGenSeed(long seed) {
		this.biomePatternGeneratorChain.initWorldGenSeed(seed);
		this.riverPatternGeneratorChain.initWorldGenSeed(seed);
		super.initWorldGenSeed(seed);
	}

	@Override
	public int[] getInts(int x, int z, int width, int length) {
		int[] inputBiomeIds = this.biomePatternGeneratorChain.getInts(x, z, width, length);
		int[] riverBiomeIds = this.riverPatternGeneratorChain.getInts(x, z, width, length);
		int[] outputBiomeIds = IntCache.getIntCache(width * length);

		for(int i1 = 0; i1 < width * length; ++i1) {
			if(inputBiomeIds[i1] != BiomeGenBaseEve.eveOcean.biomeID) {
				if(riverBiomeIds[i1] == BiomeGenBase.river.biomeID) {
					if(BiomeGenBaseEve.getBiomeGenArray()[inputBiomeIds[i1]] != null) {
						outputBiomeIds[i1] = BiomeGenBaseEve.eveRiver.biomeID;
					} else if(inputBiomeIds[i1] != BiomeGenBase.mushroomIsland.biomeID && inputBiomeIds[i1] != BiomeGenBase.mushroomIslandShore.biomeID) {
						outputBiomeIds[i1] = riverBiomeIds[i1] & 255;
					} else {
						outputBiomeIds[i1] = BiomeGenBase.mushroomIslandShore.biomeID;
					}
				} else {
					outputBiomeIds[i1] = inputBiomeIds[i1];
				}
			} else {
				outputBiomeIds[i1] = inputBiomeIds[i1];
			}
		}

		return outputBiomeIds;
	}
}