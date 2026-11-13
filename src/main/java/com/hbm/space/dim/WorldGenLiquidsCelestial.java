// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim;

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
import com.hbm.space.dim.moho.genlayer.*;
import com.hbm.space.dim.moon.*;
import com.hbm.space.dim.noise.*;
import com.hbm.space.dim.tekto.*;
import com.hbm.space.dim.tekto.GenLayerTekto.*;
import com.hbm.space.dim.tekto.biome.*;
import com.hbm.space.dim.thatmo.*;




public class WorldGenLiquidsCelestial extends WorldGenerator {

	// Identical to WorldGenLiquids except you can specify the stone block to replace

	private final Block liquidBlock;
	private final Block targetBlock;

	public WorldGenLiquidsCelestial(Block liquidBlock, Block targetBlock) {
		this.liquidBlock = liquidBlock;
		this.targetBlock = targetBlock;
	}

	public boolean generate(World world, Random rand, int x, int y, int z) {
		if(!DeepStone.matches(world.getBlock(x, y + 1, z), targetBlock)) {
			return false;
		} else if(!DeepStone.matches(world.getBlock(x, y - 1, z), targetBlock)) {
			return false;
		} else if(Material.of(world.getBlock(x, y, z)) != Material.air && !DeepStone.matches(world.getBlock(x, y, z), targetBlock)) {
			return false;
		} else {
			int l = 0;

			if(DeepStone.matches(world.getBlock(x - 1, y, z), targetBlock)) {
				++l;
			}

			if(DeepStone.matches(world.getBlock(x + 1, y, z), targetBlock)) {
				++l;
			}

			if(DeepStone.matches(world.getBlock(x, y, z - 1), targetBlock)) {
				++l;
			}

			if(DeepStone.matches(world.getBlock(x, y, z + 1), targetBlock)) {
				++l;
			}

			int i1 = 0;

			if(world.isAirBlock(x - 1, y, z)) {
				++i1;
			}

			if(world.isAirBlock(x + 1, y, z)) {
				++i1;
			}

			if(world.isAirBlock(x, y, z - 1)) {
				++i1;
			}

			if(world.isAirBlock(x, y, z + 1)) {
				++i1;
			}

			if(l == 3 && i1 == 1) {
				world.setBlock(x, y, z, this.liquidBlock, 0, 2);
				world.flowLiquid(x, y, z);
			}

			return true;
		}
	}
}
