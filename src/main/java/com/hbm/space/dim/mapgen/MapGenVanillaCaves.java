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



public class MapGenVanillaCaves extends MapGenCaves {

	// Same as vanilla cavegen but supports celestial stone
	private final Block stoneBlock;

	private Block lavaBlock = Blocks.LAVA;

	public MapGenVanillaCaves(Block stoneBlock) {
		this.stoneBlock = stoneBlock;
	}

	public MapGenVanillaCaves withLava(Block lavaBlock) {
		this.lavaBlock = lavaBlock;
		return this;
	}

	@Override
	protected void digBlock(Block[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
		BiomeGenBase biome = worldObj.getBiomeGenForCoords(x + chunkX * 16, z + chunkZ * 16);
		Block block = data[index];

		if(block == stoneBlock || block == biome.fillerBlock || block == biome.topBlock) {
			if(y < -54) {
				data[index] = lavaBlock;
			} else {
				data[index] = null;

				if(foundTop && data[index - 1] == biome.fillerBlock) {
					data[index - 1] = biome.topBlock;
				}
			}
		}
	}

}
