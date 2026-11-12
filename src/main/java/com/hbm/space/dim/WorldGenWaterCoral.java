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





public class WorldGenWaterCoral extends WorldGenerator {

	public int seaLevel = 64;

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		boolean flag = false;

		for(int l = 0; l < 16; ++l) {
			int px = x + rand.nextInt(8) - rand.nextInt(8);
			int py = y + rand.nextInt(4) - rand.nextInt(4);
			int pz = z + rand.nextInt(8) - rand.nextInt(8);

			if(!world.blockExists(px, py, pz)) continue;

			if(py < seaLevel - 1 && Material.of(world.getBlock(px, py, pz)) == Material.water && world.getBlock(px, py - 1, pz) == LegacyBlocks.b("laythe_silt")) {

				int meta = rand.nextInt(5) + 8;

				world.setBlock(px, py - 1, pz, LegacyBlocks.b("laythe_coral_block"), meta, 2);

				int oy = 0;
				int ox = 0;
				int oz = 0;
				while(oy < 8) {
					if(py + oy > seaLevel - 3) break;
					if(rand.nextBoolean()) break;

					if(rand.nextBoolean()) {
						if(rand.nextBoolean()) {
							ox += rand.nextBoolean() ? 1 : -1;
						} else {
							oz += rand.nextBoolean() ? 1 : -1;
						}
					}

					if(world.blockExists(px + ox, py + oy, pz + oz)) world.setBlock(px + ox, py + oy, pz + oz, LegacyBlocks.b("laythe_coral_block"), meta, 2);
					if(rand.nextBoolean() && world.blockExists(px + ox + 1, py + oy, pz + oz)) world.setBlock(px + ox + 1, py + oy, pz + oz, LegacyBlocks.b("laythe_coral"), meta, 2);
					if(rand.nextBoolean() && world.blockExists(px + ox - 1, py + oy, pz + oz)) world.setBlock(px + ox - 1, py + oy, pz + oz, LegacyBlocks.b("laythe_coral"), meta, 2);
					if(rand.nextBoolean() && world.blockExists(px + ox, py + oy, pz + oz + 1)) world.setBlock(px + ox, py + oy, pz + oz + 1, LegacyBlocks.b("laythe_coral"), meta, 2);
					if(rand.nextBoolean() && world.blockExists(px + ox, py + oy, pz + oz - 1)) world.setBlock(px + ox, py + oy, pz + oz - 1, LegacyBlocks.b("laythe_coral"), meta, 2);

					oy++;
				}
				
				if(world.blockExists(px + ox, py + oy, pz + oz)) world.setBlock(px + ox, py + oy, pz + oz, LegacyBlocks.b("laythe_coral"), meta, 2);

				flag = true;
			}
		}

		return flag;
	}

}
