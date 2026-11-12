// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.moho.genlayer;

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
import com.hbm.space.dim.moho.biome.*;
import com.hbm.space.dim.moon.*;
import com.hbm.space.dim.noise.*;
import com.hbm.space.dim.tekto.*;
import com.hbm.space.dim.tekto.GenLayerTekto.*;
import com.hbm.space.dim.tekto.biome.*;
import com.hbm.space.dim.thatmo.*;




public class GenLayerMohoPlateauExtend extends GenLayer {

	public GenLayerMohoPlateauExtend(long seed, GenLayer genLayer) {
		super(seed);
		this.parent = genLayer;
	}

	public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
		return this.getIntsExtend(areaX, areaY, areaWidth, areaHeight);
	}

	private int[] getIntsExtend(int areaX, int areaY, int areaWidth, int areaHeight) {
		int i = areaX - 1;
		int j = areaY - 1;
		int k = 1 + areaWidth + 1;
		int l = 1 + areaHeight + 1;
		int[] aint = this.parent.getInts(i, j, k, l);
		int[] aint1 = IntCache.getIntCache(areaWidth * areaHeight);

		for(int i1 = 0; i1 < areaHeight; ++i1) {
			for(int j1 = 0; j1 < areaWidth; ++j1) {
				this.initChunkSeed((long) (j1 + areaX), (long) (i1 + areaY));
				int k1 = aint[j1 + 1 + (i1 + 1) * k];

				if(k1 == BiomeGenBaseMoho.mohoCrag.biomeID) {
					int l1 = aint[j1 + 1 + (i1 + 1 - 1) * k];
					int i2 = aint[j1 + 1 + 1 + (i1 + 1) * k];
					int j2 = aint[j1 + 1 - 1 + (i1 + 1) * k];
					int k2 = aint[j1 + 1 + (i1 + 1 + 1) * k];
					boolean flag = ((l1 == BiomeGenBaseMoho.mohoCrag.biomeID || l1 == BiomeGenBaseMoho.mohoPlateau.biomeID)
						&& (i2 == BiomeGenBaseMoho.mohoCrag.biomeID || i2 == BiomeGenBaseMoho.mohoPlateau.biomeID)
						&& (j2 == BiomeGenBaseMoho.mohoCrag.biomeID || j2 == BiomeGenBaseMoho.mohoPlateau.biomeID)
						&& (k2 == BiomeGenBaseMoho.mohoCrag.biomeID || k2 == BiomeGenBaseMoho.mohoPlateau.biomeID));
					if(flag) {
						k1 = BiomeGenBaseMoho.mohoPlateau.biomeID;
					}
				}

				aint1[j1 + i1 * areaWidth] = k1;
			}
		}

		return aint1;
	}

}
