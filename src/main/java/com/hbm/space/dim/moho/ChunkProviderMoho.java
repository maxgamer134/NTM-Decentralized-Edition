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




public class ChunkProviderMoho extends ChunkProviderCelestial {

	private ExperimentalCaveGenerator caveGenV2 = new ExperimentalCaveGenerator(1, 52, 5.0F);
	private MapgenRavineButBased rgen = new MapgenRavineButBased();

	private MapGenVNoise noise = new MapGenVNoise();
	private MapGenCrater smallCrater = new MapGenCrater(6);
	private MapGenCrater largeCrater = new MapGenCrater(64);
	private MapGenVolcano volcano = new MapGenVolcano(72);
	private MapGenPlateau plateau = new MapGenPlateau(worldObj);

	public ChunkProviderMoho(World world, long seed) {
		super(world, seed);

		smallCrater.setSize(8, 32);
		largeCrater.setSize(96, 128);
		volcano.setSize(64, 128);
		volcano.setMaterial(LegacyBlocks.b("volcano_core"), LegacyBlocks.b("basalt"));

		smallCrater.regolith = largeCrater.regolith = LegacyBlocks.b("moho_regolith");
		smallCrater.rock = largeCrater.rock = LegacyBlocks.b("moho_stone");

		caveGenV2.stoneBlock = LegacyBlocks.b("moho_stone");
		rgen.stoneBlock = LegacyBlocks.b("moho_stone");
		stoneBlock = LegacyBlocks.b("moho_stone");
		seaBlock = Blocks.LAVA;

		noise.surfBlock = LegacyBlocks.b("moho_stone");
		noise.rockBlock = LegacyBlocks.b("moho_stone");
		noise.fluidBlock = Blocks.LAVA;
		noise.crackSize = 0.5;
		noise.cellSize = 27;
		noise.plateStartY = 62;
		noise.plateThickness = 25;
		noise.applyToBiome = BiomeGenBaseMoho.mohoLavaSea;

		plateau.maxPlateauAddition = 6;
		plateau.surfrock = LegacyBlocks.b("moho_regolith");
		plateau.stoneBlock = LegacyBlocks.b("moho_stone");
		plateau.fillblock = Blocks.LAVA;
		plateau.maxPlateauAddition = 6;
		plateau.stepHeight = 2;
		plateau.noiseScale = 0.03;
		plateau.applyToBiome = BiomeGenBaseMoho.mohoPlateau;
	}

	@Override
	protected Block getFlatWorldBlock(Block block) {
		if(block == Blocks.WATER || block == Blocks.WATER) return Blocks.LAVA;
		if(block == Blocks.GRASS_BLOCK || block == Blocks.SAND) return LegacyBlocks.b("moho_regolith");
		if(block == Blocks.DIRT || block == Blocks.STONE || block == Blocks.SANDSTONE) return LegacyBlocks.b("moho_stone");
		if(block == Blocks.SNOW) return Blocks.AIR;
		return block;
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		BlockMetaBuffer buffer = super.getChunkPrimer(x, z);

		boolean hasLavaSea = false;
		boolean hasPlateau = false;

		for(int i = 0; i < biomesForGeneration.length; i++) {
			if(biomesForGeneration[i] == BiomeGenBaseMoho.mohoLavaSea) hasLavaSea = true;
			if(biomesForGeneration[i] == BiomeGenBaseMoho.mohoPlateau) hasPlateau = true;
			if(hasLavaSea && hasPlateau) break;
		}

		if(hasLavaSea) noise.func_151539_a(this, worldObj, x, z, buffer.blocks);

		if(hasPlateau) plateau.func_151539_a(this, worldObj, x, z, buffer.blocks);

		caveGenV2.func_151539_a(this, worldObj, x, z, buffer.blocks);
		rgen.func_151539_a(this, worldObj, x, z, buffer.blocks);
		smallCrater.func_151539_a(this, worldObj, x, z, buffer.blocks);
		largeCrater.func_151539_a(this, worldObj, x, z, buffer.blocks);
		volcano.func_151539_a(this, worldObj, x, z, buffer.blocks);
		return buffer;
	}

}
