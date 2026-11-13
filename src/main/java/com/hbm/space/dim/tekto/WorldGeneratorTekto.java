// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.tekto;

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
import com.hbm.space.dim.moho.genlayer.*;
import com.hbm.space.dim.moon.*;
import com.hbm.space.dim.noise.*;
import com.hbm.space.dim.tekto.GenLayerTekto.*;
import com.hbm.space.dim.tekto.biome.*;
import com.hbm.space.dim.thatmo.*;





public class WorldGeneratorTekto implements Planet.Decorator {

	public WorldGeneratorTekto() {
		// NTM:Space registered its structures here; they come with the structure port
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world) {
		if(true) {
			generateTekto(world, random, chunkX * 16, chunkZ * 16);
		}
	}

	private void generateTekto(World world, Random rand, int i, int j) {
		int meta = world.bodyMeta();
		Block stone = world.stone();

		Features.generateOre(world, rand, i, j, WorldConfig.cobaltSpawn * 2,  6, 4, 8, LegacyBlocks.b("ore_cobalt"), meta, stone);

		BiomeGenBase biome = world.getBiomeGenForCoords(i + 16, j + 16);

		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				for(int y = 32; y < ChunkProviderCelestial.MAX_Y; y++) { // volcano peaks stand on the taller land
					int ox = i + x + 8;
					int oz = j + z + 8;
					Block b = world.getBlock(ox, y, oz);
					if(b == LegacyBlocks.b("geysir_chloric")) {
						world.setBlock(ox, y, oz, LegacyBlocks.b("geysir_chloric"));
						world.markBlockForUpdate(ox, y, oz);
					}
				}
			}
		}

		if(biome == BiomeGenBaseTekto.polyvinylPlains) {
			for(int o = 0; o < 2; o++) {
				if(rand.nextInt(10) == 0) {
					int x = i + rand.nextInt(16) + 8;
					int z = j + rand.nextInt(16) + 8;
					int y = world.getHeightValue(x, z);

					WorldGenAbstractTree customTreeGen = new TTree(false, 4, 2, 10, 2, 4, false, LegacyBlocks.b("pvc_log"), LegacyBlocks.b("rubber_leaves"));
					customTreeGen.generate(world, rand, x, y, z);
				}

				if(rand.nextInt(8) == 0) {
					int x = i + rand.nextInt(16) + 8;
					int z = j + rand.nextInt(16) + 8;
					int y = world.getHeightValue(x, z);

					WorldGenAbstractTree chopped = new TTree(false, 2, 4, 5, 3, 2, false, LegacyBlocks.b("vinyl_log"), LegacyBlocks.b("pet_leaves"));
					chopped.generate(world, rand, x, y, z);
				}
			}

		}

		if(biome == BiomeGenBaseTekto.halogenHills) {
			if(rand.nextInt(12) == 0) {
				for(int o = 0; o < 4; o++) {
					int x = i + rand.nextInt(16) + 8;
					int z = j + rand.nextInt(16) + 8;
					int y = world.getHeightValue(x, z);

					WorldGenAbstractTree customTreeGen = new TTree(false, 3, 2, 14, 3, 3, false, LegacyBlocks.b("pvc_log"), LegacyBlocks.b("rubber_leaves"));
					customTreeGen.generate(world, rand, x, y, z);
				}
			}
		}

		if(biome == BiomeGenBaseTekto.forest) {
			for(int o = 0; o < 8; o++) {
				int x = i + rand.nextInt(16) + 8;
				int z = j + rand.nextInt(16) + 8;
				int y = world.getHeightValue(x, z);

				if(rand.nextInt(2) == 0) {
					WorldGenAbstractTree customTreeGen = new TTree(false, 3, 2, 20, 3, 5, true, LegacyBlocks.b("pvc_log"), LegacyBlocks.b("rubber_leaves"));
					customTreeGen.generate(world, rand, x, y, z);
				} else {
					WorldGenAbstractTree tustomTreeGen = new TTree(false, 3, 1, 1, 3, 5, false, LegacyBlocks.b("pvc_log"), LegacyBlocks.b("rubber_leaves"));
					tustomTreeGen.generate(world, rand, x, y, z);
				}
			}
		}
	}

}
