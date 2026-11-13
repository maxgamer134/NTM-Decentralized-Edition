// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.minmus;

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





public class WorldGeneratorMinmus implements Planet.Decorator {

	public WorldGeneratorMinmus() {
		// NTM:Space registered its structures here; they come with the structure port
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world) {
		if(true) {
			generateMinmus(world, random, chunkX * 16, chunkZ * 16);
			com.hbm.space.SpaceExtras.layer(world, "minmus_regolith", LegacyBlocks.b("minmus_regolith").defaultBlockState(), 0.06D, 0.25D, 220D, chunkX * 16, chunkZ * 16);
			com.hbm.space.SpaceExtras.layer(world, "minmus_smooth", LegacyBlocks.b("minmus_smooth").defaultBlockState(), 0.05D, 0.15D, 280D, chunkX * 16, chunkZ * 16);
			com.hbm.space.SpaceExtras.conglomerate(world, "minmus", chunkX * 16, chunkZ * 16);
		}
	}

	private void generateMinmus(World world, Random rand, int i, int j) {
		int meta = world.bodyMeta();
		Block stone = world.stone();
		com.hbm.space.SpaceExtras.sulfurCave(world, i, j, false);

		Features.generateOre(world, rand, i, j, WorldConfig.nickelSpawn, 8, 1, 43, LegacyBlocks.b("ore_nickel"), meta, stone);
		Features.generateOre(world, rand, i, j, WorldConfig.titaniumSpawn, 12, 4, 27, LegacyBlocks.b("ore_titanium"), meta, stone);
		Features.generateOre(world, rand, i, j, 1, 16, 6, 40, LegacyBlocks.b("stone_resource"), 3, stone);
		Features.generateOre(world, rand, i, j, WorldConfig.copperSpawn * 3, 12, 8, 56, LegacyBlocks.b("ore_copper"), meta, stone);
	}



}
