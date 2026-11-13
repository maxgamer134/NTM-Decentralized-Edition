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




public class BiomeDecoratorCelestial extends BiomeDecorator {

	// Same as BiomeDecorator, but skips any vanilla plant life and supports different stone types

	public int lavaCount = 20;

	public int waterPlantsPerChunk = 0;
	public int rubberPlantsPerChunk = 0;
	public int coralPerChunk = 0;

	public int seaLevel = 63;
	public WorldGenWaterPlant genPlants;
	public WorldGenRubberPlant genRPlants;
	public WorldGenWaterCoral genCoral;

	// ACTUAL lakes, not the single block stuff
	// honestly MCP couldja give things better names pls?
	public int lakeChancePerChunk = 0;
	public Block lakeBlock = Blocks.WATER;

	private final Block stoneBlock;

	public BiomeDecoratorCelestial(Block stoneBlock) {
		this.stoneBlock = stoneBlock;
		this.genPlants = new WorldGenWaterPlant();
		this.genRPlants = new WorldGenRubberPlant();
		this.genCoral = new WorldGenWaterCoral();
	}

	@Override
	protected void genDecorations(BiomeGenBase biome) {
		this.generateOres();

		boolean doGen = true;
		if(doGen && this.generateLakes) {
			for (int i = 0; i < lavaCount; ++i) {
				int x = this.chunk_X + this.randomGenerator.nextInt(16) + 8;
				int y = com.hbm.space.dim.legacy.Features.stretch(this.randomGenerator.nextInt(this.randomGenerator.nextInt(this.randomGenerator.nextInt(240) + 8) + 8));
				int z = this.chunk_Z + this.randomGenerator.nextInt(16) + 8;
				(new WorldGenLiquidsCelestial(Blocks.LAVA, stoneBlock)).generate(this.currentWorld, this.randomGenerator, x, y, z);
			}
		}

		if(doGen && this.lakeChancePerChunk > 0) {
			if(this.randomGenerator.nextInt(lakeChancePerChunk) == 0) {
				int x = this.chunk_X + this.randomGenerator.nextInt(16) + 8;
				int y = this.randomGenerator.nextInt(256);
				int z = this.chunk_Z + this.randomGenerator.nextInt(16) + 8;
				(new WorldGenLakes(lakeBlock)).generate(this.currentWorld, this.randomGenerator, x, y, z);
			}
		}

		doGen = true;
		if(doGen && this.waterPlantsPerChunk > 0) {
			for (int i = 0; i < waterPlantsPerChunk; ++i) {
				int x = this.chunk_X + this.randomGenerator.nextInt(16) + 8;
				int z = this.chunk_Z + this.randomGenerator.nextInt(16) + 8;
				int y = this.randomGenerator.nextInt(seaLevel);
				genPlants.seaLevel = seaLevel;
				genPlants.generate(currentWorld, randomGenerator, x, y, z);
			}
		}
		doGen = true;
		if(doGen && this.rubberPlantsPerChunk > 0) {
			for (int i = 0; i < rubberPlantsPerChunk; ++i) {
				int x = this.chunk_X + this.randomGenerator.nextInt(16) + 8;
				int z = this.chunk_Z + this.randomGenerator.nextInt(16) + 8;
				int y = this.randomGenerator.nextInt(80);
				genRPlants.generate(currentWorld, randomGenerator, x, y, z);
			}
		}

		doGen = true;
		if(doGen && this.coralPerChunk > 0) {
			for (int i = 0; i < coralPerChunk; ++i) {
				int x = this.chunk_X + this.randomGenerator.nextInt(16) + 8;
				int z = this.chunk_Z + this.randomGenerator.nextInt(16) + 8;
				int y = this.randomGenerator.nextInt(seaLevel);
				genCoral.seaLevel = seaLevel;
				genCoral.generate(currentWorld, randomGenerator, x, y, z);
			}
		}
	}

}
