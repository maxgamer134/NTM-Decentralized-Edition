// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.laythe;

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





public class WorldGeneratorLaythe implements Planet.Decorator {

	public WorldGeneratorLaythe() {
		// NTM:Space registered its structures here; they come with the structure port
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world) {
		if(true) {
			generateLaythe(world, random, chunkX * 16, chunkZ * 16);
		}
	}

	private void generateLaythe(World world, Random rand, int i, int j) {
		int meta = world.bodyMeta();
		Block stone = world.stone();
		com.hbm.space.SpaceExtras.sulfurCave(world, i, j, true);
		com.hbm.space.SpaceExtras.oreCave(world, i, j, "asbestos", LegacyBlocks.b("stone_resource_asbestos").defaultBlockState(), null, "asbestos",
				null, 1.75D, 20, 25, 20, false);
		com.hbm.space.SpaceExtras.oreCave(world, i, j, "laythe_tumor", LegacyBlocks.b("tumor").defaultBlockState(), null, "flesh",
				null, 0.3D, 20, 25, 70, false);
		// darker silt banks in the seabed
		com.hbm.space.SpaceExtras.oreCave(world, i, j, "laythe_silt_6", LegacyBlocks.state(LegacyBlocks.b("laythe_silt"), 6), null, null,
				LegacyBlocks.b("laythe_silt"), 0.25D, 80, 54, 24, true);
		com.hbm.space.SpaceExtras.oreCave(world, i, j, "laythe_silt_3", LegacyBlocks.state(LegacyBlocks.b("laythe_silt"), 3), null, null,
				LegacyBlocks.b("laythe_silt"), 0.2D, 60, 58, 14, true);

		Features.generateOre(world, rand, i, j, WorldConfig.asbestosSpawn, 4, 16, 16, LegacyBlocks.b("ore_asbestos"), meta, stone);
		Features.generateOre(world, rand, i, j, WorldConfig.berylliumSpawn, 4, 5, 30, LegacyBlocks.b("ore_beryllium"), meta, stone);
		Features.generateOre(world, rand, i, j, WorldConfig.rareSpawn, 5, 5, 20, LegacyBlocks.b("ore_rare"), meta, stone);
	}

}
