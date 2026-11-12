// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.eve;

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





public class WorldGeneratorEve implements Planet.Decorator {

	WorldGenElectricVolcano volcano = new WorldGenElectricVolcano(30, 22, LegacyBlocks.b("eve_silt"), LegacyBlocks.b("eve_rock"));

	public WorldGeneratorEve() {
		// NTM:Space registered its structures here; they come with the structure port
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world) {
		if(true) {
			generateEve(world, random, chunkX * 16, chunkZ * 16);
		}
	}

	private void generateEve(World world, Random rand, int i, int j) {
		int meta = world.bodyMeta();
		Block stone = world.stone();

		Features.generateOre(world, rand, i, j, 12,  8, 1, 33, LegacyBlocks.b("ore_niobium"), meta, stone);
		Features.generateOre(world, rand, i, j, 8,  4, 5, 48, LegacyBlocks.b("ore_iodine"), meta, stone);
		Features.generateOre(world, rand, i, j, 1,  4, 1, 16, LegacyBlocks.b("ore_schrabidium"), meta, stone);

		int x = i + rand.nextInt(16) + 8;
		int z = j + rand.nextInt(16) + 8;
		int y = world.getHeightValue(x, z);

		BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
		if(biome == BiomeGenBaseEve.eveSeismicPlains) {
			new WorldGenEveSpike().generate(world, rand, x, y, z);
		}

		// TODO: these span multiple chunks, fix this cascade!
		if(rand.nextInt(100) == 0) {
			volcano.generate(world, rand, x, y, z);
		}
	}

}
