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




public class ChunkProviderDuna extends ChunkProviderCelestial {

	private ExperimentalCaveGenerator caveGenSmall = new ExperimentalCaveGenerator(2, 12, 0.12F);
	private ExperimentalCaveGenerator caveGenV2 = new ExperimentalCaveGenerator(2, 40, 3.0F);

	private MapGenPlateau genPlateau = new MapGenPlateau(worldObj);

	private MapGenBubble oil = new MapGenBubble(WorldConfig.dunaOilSpawn);

	public ChunkProviderDuna(World world, long seed) {
		super(world, seed);
		stoneBlock = LegacyBlocks.b("duna_rock");

		caveGenV2.lavaBlock = LegacyBlocks.b("basalt");
		caveGenV2.stoneBlock = LegacyBlocks.b("duna_cobble");
		caveGenSmall.lavaBlock = LegacyBlocks.b("duna_sands");
		caveGenSmall.stoneBlock = LegacyBlocks.b("duna_rock");

		caveGenSmall.smallCaveSize = 0.1F;

		caveGenV2.onlyBiome = BiomeGenBaseDuna.dunaLowlands;
		caveGenSmall.ignoreBiome = BiomeGenBaseDuna.dunaLowlands;

		genPlateau.surfrock = LegacyBlocks.b("duna_sands");
		genPlateau.stoneBlock = LegacyBlocks.b("duna_rock");
		genPlateau.fillblock = LegacyBlocks.b("duna_sands");
		genPlateau.applyToBiome = BiomeGenBaseDuna.dunaHills;

		oil.block = LegacyBlocks.b("ore_oil");
		oil.meta = (byte)world.bodyMeta();
		oil.replace = LegacyBlocks.b("duna_rock");
		oil.setSize(8, 16);
	}

	@Override
	protected Block getFlatWorldBlock(Block block) {
		if(block == Blocks.GRASS_BLOCK || block == Blocks.SAND || block == Blocks.WATER || block == Blocks.WATER) return LegacyBlocks.b("duna_sands");
		if(block == Blocks.DIRT || block == Blocks.STONE || block == Blocks.SANDSTONE) return LegacyBlocks.b("duna_rock");
		if(block == Blocks.SNOW) return Blocks.SNOW;
		return block;
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		// Instead of calling super, we want to do some generation before _and_ after block replacement, for shaping!
		BlockMetaBuffer buffer = new BlockMetaBuffer();
		generateBlocks(x, z, buffer.blocks);
		biomesForGeneration = this.worldObj.getWorldChunkManager().loadBlockGeneratorData(biomesForGeneration, x * 16, z * 16, 16, 16);

		boolean hasLowlands = false;
		boolean hasNotLowlands = false;
		boolean hasPlateau = false;

		for(int i = 0; i < biomesForGeneration.length; i++) {
			if(biomesForGeneration[i] == BiomeGenBaseDuna.dunaLowlands) hasLowlands = true; else hasNotLowlands = true;
			if(biomesForGeneration[i] == BiomeGenBaseDuna.dunaHills) hasPlateau = true;
			if(hasLowlands && hasNotLowlands && hasPlateau) break;
		}

		// Pre-biome blocks
		if(hasPlateau) genPlateau.func_151539_a(this, worldObj, x, z, buffer.blocks);

		replaceBlocksForBiome(x, z, buffer.blocks, buffer.metas, biomesForGeneration);

		// Post-biome blocks
		if(hasLowlands) caveGenV2.func_151539_a(this, worldObj, x, z, buffer.blocks);
		if(hasNotLowlands) caveGenSmall.func_151539_a(this, worldObj, x, z, buffer.blocks);

		oil.setMetas(buffer.metas);
		oil.func_151539_a(this, worldObj, x, z, buffer.blocks);

		return buffer;
	}

}