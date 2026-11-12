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





public class WorldGenWaterPlant extends WorldGenerator {

	public int seaLevel = 64;

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		boolean flag = false;

		for(int l = 0; l < 128; ++l) {
			int px = x + rand.nextInt(8) - rand.nextInt(8);
			int py = y + rand.nextInt(4) - rand.nextInt(4);
			int pz = z + rand.nextInt(8) - rand.nextInt(8);

			if(py < seaLevel - 1 && world.getBlock(px, py, pz) == Blocks.WATER && world.getBlock(px, py - 1, pz) == LegacyBlocks.b("laythe_silt")) {
				int type = rand.nextInt(7);

				switch(type) {
				case 0:
				case 1:
				case 2:
					world.setBlock(px, py, pz, LegacyBlocks.b("laythe_short"), 0, 2);
					break;
				case 3:
					world.setBlock(px, py, pz, LegacyBlocks.b("laythe_glow"), 0, 2);
					break;
				case 4:
				case 5:
					if(py < seaLevel - 2) {
						world.setBlock(px, py, pz, LegacyBlocks.b("plant_tall_laythe"), 0, 2);
						world.setBlock(px, py + 1, pz, LegacyBlocks.b("plant_tall_laythe"), 8, 2);
					}
					break;
				case 6:
					if(py < seaLevel - 4) {
						int height = 2 + rand.nextInt(Math.min(8, seaLevel - py - 2)); 
						for(int h = 0; h < height; ++h) {
							world.setBlock(px, py + h, pz, LegacyBlocks.b("laythe_kelp"), 0, 1);
						}	                	
					}
					break;
				}

				flag = true;
			}
		}

		return flag;
	}

}
