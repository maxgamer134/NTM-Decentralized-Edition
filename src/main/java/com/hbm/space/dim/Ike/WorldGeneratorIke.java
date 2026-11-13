// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.Ike;

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





public class WorldGeneratorIke implements Planet.Decorator {

	public WorldGeneratorIke() {
		// NTM:Space registered its structures here; they come with the structure port
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world) {
		if(true) {
			generateIke(world, random, chunkX * 16, chunkZ * 16);
			com.hbm.space.SpaceExtras.conglomerate(world, "ike", chunkX * 16, chunkZ * 16);
		}
	}

	private void generateIke(World world, Random rand, int i, int j) {
		int meta = world.bodyMeta();
		Block stone = world.stone();
		com.hbm.space.SpaceExtras.sulfurCave(world, i, j, false);

		Features.generateOre(world, rand, i, j, WorldConfig.asbestosSpawn, 8, 3, 22, LegacyBlocks.b("ore_asbestos"), meta, stone);
		Features.generateOre(world, rand, i, j, WorldConfig.copperSpawn, 9, 4, 27, LegacyBlocks.b("ore_copper"), meta, stone);
		Features.generateOre(world, rand, i, j, WorldConfig.ironSpawn,  8, 1, 33, LegacyBlocks.b("ore_iron"), meta, stone);
		Features.generateOre(world, rand, i, j, WorldConfig.lithiumSpawn,  6, 4, 8, LegacyBlocks.b("ore_lithium"), meta, stone);
		Features.generateOre(world, rand, i, j, 2, 4, 15, 40, LegacyBlocks.b("ore_coltan"), meta, stone);

		//okay okay okay, lets say on duna you DO make solvent, this is now awesome because you can now make gallium arsenide to then head to
		//dres and the likes :)

	
		// The pyramid tombs and their divine shard pedestals come with the structure port
		Features.generateOre(world, rand, i, j, WorldConfig.mineralSpawn, 10, 12, 32, LegacyBlocks.b("ore_mineral"), meta, stone);


	}

}