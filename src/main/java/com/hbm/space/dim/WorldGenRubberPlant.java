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





public class WorldGenRubberPlant extends WorldGenerator {

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		boolean flag = false;
		BiomeGenBase currentBiome = world.getBiomeGenForCoords(x, z);

		for (int l = 0; l < 64; ++l) {
			int px = x + rand.nextInt(8) - rand.nextInt(8);
			int py = y + rand.nextInt(4) - rand.nextInt(4);
			int pz = z + rand.nextInt(8) - rand.nextInt(8);



			if ((!false || py < 254) && world.getBlock(px, py - 1, pz) == LegacyBlocks.b("rubber_grass")) {

				int plantType = rand.nextInt(3);  // 0, 1, or 2

				switch (plantType) {
					case 0:
						plantType = 0;
						break;
					case 1:
						plantType = 1;
						break;
					case 2:
						plantType = 2;
						break;
				}

				world.setBlock(px, py, pz, LegacyBlocks.b("rubber_plant"), plantType, 2);
				flag = true;

			} else if ((!false || py < 254) && world.getBlock(px, py - 1, pz) == LegacyBlocks.b("vinyl_sand")) {
				if(rand.nextInt(64) == 0) {

					world.setBlock(px, world.getTopSolidOrLiquidBlock(px, pz), pz, LegacyBlocks.b("spike_cacti"), 0, 2);

				}

			}
			if (currentBiome == BiomeGenBaseTekto.forest) {
				if ((!false || py < 254) && world.getBlock(px, py - 1, pz) == LegacyBlocks.b("rubber_grass")) {
					int plantType = rand.nextInt(2);

					switch (plantType) {
						case 0:
							plantType = 1;
							break;
						case 1:
							plantType = 2;
							break;
					}

					int OtherplantType = rand.nextInt(3);

					switch (OtherplantType) {
					case 0:
						OtherplantType = 0;
						break;
					case 1:
						OtherplantType = 1;
						break;
					case 2:
						OtherplantType = 2;
						break;
				}
					if(rand.nextInt(4) == 0) {
						world.setBlock(px, py, pz, LegacyBlocks.b("spike_cacti"), plantType, 2);
					} else {
						world.setBlock(px, py, pz, LegacyBlocks.b("rubber_plant"), OtherplantType, 2);

					}
					if(rand.nextInt(10) == 0) {
						world.setBlock(px, py, pz, LegacyBlocks.b("spike_cacti"), 0, 2);

					}
					flag = true;

				}

			}
		}

		return flag;
	}

}
