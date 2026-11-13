// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.duna;

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





public class WorldGeneratorDuna implements Planet.Decorator {

	public WorldGeneratorDuna() {
		// NTM:Space registered its structures here; they come with the structure port
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world) {
		if(true) {
			generateDuna(world, random, chunkX * 16, chunkZ * 16);
		}
	}

	private void generateDuna(World world, Random rand, int i, int j) {
		int meta = world.bodyMeta();
		Block stone = world.stone();
		com.hbm.space.SpaceExtras.sulfurCave(world, i, j, false);

		Features.generateOre(world, rand, i, j, WorldConfig.ironSpawn, 8, 32, 64, LegacyBlocks.b("ore_iron"), meta, stone);
		Features.generateOre(world, rand, i, j, WorldConfig.zincSpawn, 9, 4, 27, LegacyBlocks.b("ore_zinc"), meta, stone);

		// Basalt rich in minerals, but only in basaltic caves!
		Features.generateOre(world, rand, i, j, 12, 6, 0, 16, LegacyBlocks.b("ore_basalt"), 0, LegacyBlocks.b("basalt"));
		Features.generateOre(world, rand, i, j, 8, 8, 0, 16, LegacyBlocks.b("ore_basalt"), 1, LegacyBlocks.b("basalt"));
		Features.generateOre(world, rand, i, j, 8, 9, 0, 16, LegacyBlocks.b("ore_basalt"), 2, LegacyBlocks.b("basalt"));
		Features.generateOre(world, rand, i, j, 2, 4, 0, 16, LegacyBlocks.b("ore_basalt"), 3, LegacyBlocks.b("basalt"));
		Features.generateOre(world, rand, i, j, 6, 10, 0, 16, LegacyBlocks.b("ore_basalt"), 4, LegacyBlocks.b("basalt"));


		// the martian start base (SpaceMartian checks the world is a martian one)
		if(i == 0 && j == 0 && world.level() != null) {
			com.hbm.space.SpaceMartian.buildBase(world.level());
		}

		// a broken behemoth lantern, half the time with a booklet beside it
		if(rand.nextInt(1234) == 0 && world.level() != null) {
			int x = i + rand.nextInt(16);
			int z = j + rand.nextInt(16);
			int y = world.getHeightValue(x, z);
			net.minecraft.world.level.WorldGenLevel level = world.level();
			com.hbm.world.HbmFeatures.lanternBehemoth().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(
					java.util.Optional.empty(), level, null, net.minecraft.util.RandomSource.create(rand.nextLong()),
					new net.minecraft.core.BlockPos(x, y, z), net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
		}
	}

}