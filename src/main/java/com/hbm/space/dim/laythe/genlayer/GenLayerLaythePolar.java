// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.laythe.genlayer;

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




public class GenLayerLaythePolar extends GenLayer {

	public GenLayerLaythePolar(long seed, GenLayer parent) {
		super(seed);
		this.parent = parent;
	}

	@Override
	public int[] getInts(int p_75904_1_, int p_75904_2_, int p_75904_3_, int p_75904_4_) {
		int i1 = p_75904_1_ - 1;
		int j1 = p_75904_2_ - 1;
		int k1 = p_75904_3_ + 2;
		int l1 = p_75904_4_ + 2;
		int[] aint = this.parent.getInts(i1, j1, k1, l1);
		int[] aint1 = IntCache.getIntCache(p_75904_3_ * p_75904_4_);

		for(int i2 = 0; i2 < p_75904_4_; ++i2) {
			for(int j2 = 0; j2 < p_75904_3_; ++j2) {
				int k2 = aint[j2 + 0 + (i2 + 0) * k1];
				int l2 = aint[j2 + 2 + (i2 + 0) * k1];
				int i3 = aint[j2 + 0 + (i2 + 2) * k1];
				int j3 = aint[j2 + 2 + (i2 + 2) * k1];
				int k3 = aint[j2 + 1 + (i2 + 1) * k1];
				this.initChunkSeed((long)(j2 + p_75904_1_), (long)(i2 + p_75904_2_));
				if(k3 == SpaceConfig.laytheOceanBiome && k2 == SpaceConfig.laytheOceanBiome && l2 == SpaceConfig.laytheOceanBiome && i3 == SpaceConfig.laytheOceanBiome && j3 == SpaceConfig.laytheOceanBiome) {
					aint1[j2 + i2 * p_75904_3_] = BiomeGenBaseLaythe.laythePolar.biomeID;
				} else {
					aint1[j2 + i2 * p_75904_3_] = k3;
				}
			}
		}

		return aint1;
	}
}

