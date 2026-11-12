// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.moho;

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
import com.hbm.space.dim.moho.biome.*;
import com.hbm.space.dim.moho.genlayer.*;
import com.hbm.space.dim.moon.*;
import com.hbm.space.dim.noise.*;
import com.hbm.space.dim.tekto.*;
import com.hbm.space.dim.tekto.GenLayerTekto.*;
import com.hbm.space.dim.tekto.biome.*;
import com.hbm.space.dim.thatmo.*;





public class WorldGeneratorMoho implements Planet.Decorator {

	public WorldGeneratorMoho() {
		// NTM:Space registered its structures here; they come with the structure port
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world) {
		if(true) {
			generateMoho(world, random, chunkX * 16, chunkZ * 16);
		}
	}

	private void generateMoho(World world, Random rand, int i, int j) {
		int meta = world.bodyMeta();
		Block stone = world.stone();

		Features.generateOre(world, rand, i, j, WorldConfig.mineralSpawn, 10, 12, 32, LegacyBlocks.b("ore_mineral"), meta, stone);

		Features.generateOre(world, rand, i, j, 14, 12, 5, 30, LegacyBlocks.b("ore_glowstone"), meta, stone);
		Features.generateOre(world, rand, i, j, WorldConfig.netherPhosphorusSpawn, 6, 8, 64, LegacyBlocks.b("ore_fire"), meta, stone);
		Features.generateOre(world, rand, i, j, 8, 4, 0, 24, LegacyBlocks.b("ore_australium"), meta, stone);

		Features.generateOre(world, rand, i, j, 1, 12, 8, 32, LegacyBlocks.b("ore_shale"), meta, stone);

		Features.generateOre(world, rand, i, j, 10, 32, 0, 128, LegacyBlocks.b("basalt"), 0, stone);

		// More basalt ores!
		Features.generateOre(world, rand, i, j, 16, 6, 16, 64, LegacyBlocks.b("ore_basalt"), 0, LegacyBlocks.b("basalt"));
		Features.generateOre(world, rand, i, j, 12, 8, 8, 32, LegacyBlocks.b("ore_basalt"), 1, LegacyBlocks.b("basalt"));
		Features.generateOre(world, rand, i, j, 8, 9, 8, 48, LegacyBlocks.b("ore_basalt"), 2, LegacyBlocks.b("basalt"));
		Features.generateOre(world, rand, i, j, 2, 4, 0, 24, LegacyBlocks.b("ore_basalt"), 3, LegacyBlocks.b("basalt"));
		Features.generateOre(world, rand, i, j, 8, 10, 16, 64, LegacyBlocks.b("ore_basalt"), 4, LegacyBlocks.b("basalt"));

		for(int k = 0; k < 2; k++){
			int x = i + rand.nextInt(16) + 8;
			int z = j + rand.nextInt(16) + 8;
			int d = 16 + rand.nextInt(96);

			for(int y = d - 5; y <= d; y++) {
				Block b = world.getBlock(x, y, z);
				if(world.getBlock(x, y + 1, z) == Blocks.AIR && (b == LegacyBlocks.b("moho_stone") || b == LegacyBlocks.b("moho_regolith"))) {
					world.setBlock(x, y, z, LegacyBlocks.b("geysir_nether"));
					world.setBlock(x+1, y, z, Blocks.NETHERRACK);
					world.setBlock(x-1, y, z, Blocks.NETHERRACK);
					world.setBlock(x, y, z+1, Blocks.NETHERRACK);
					world.setBlock(x, y, z-1, Blocks.NETHERRACK);
					world.setBlock(x+1, y-1, z, Blocks.NETHERRACK);
					world.setBlock(x-1, y-1, z, Blocks.NETHERRACK);
					world.setBlock(x, y-1, z+1, Blocks.NETHERRACK);
					world.setBlock(x, y-1, z-1, Blocks.NETHERRACK);
				}
			}
		}

		// Kick the volcanoes into action, and fix SOME floating lava
		// a full fix for floating lava would cause infinite cascades so we uh, don't
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				for(int y = 32; y < 128; y++) {
					int ox = i + x + 8;
					int oz = j + z + 8;
					Block b = world.getBlock(ox, y, oz);

					if(b == Blocks.LAVA && world.getBlock(ox, y - 1, oz) == Blocks.AIR) {
						world.setBlock(ox, y - 1, oz, Blocks.LAVA, 0, 0);
						world.markBlockForUpdate(ox, y - 1, oz);
					} else if(b == LegacyBlocks.b("volcano_core")) {
						world.setBlock(ox, y, oz, LegacyBlocks.b("volcano_core"), 1, 0);
						world.markBlockForUpdate(ox, y, oz);
					}
				}
			}
		}
	}

}