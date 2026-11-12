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




public class ChunkProviderMinmus extends ChunkProviderCelestial {

	private MapGenBase caveGenerator = new MapGenVanillaCaves(LegacyBlocks.b("minmus_stone")).withLava(LegacyBlocks.b("minmus_smooth"));

	private MapGenCrater smallCrater = new MapGenCrater(5);
	private MapGenCrater largeCrater = new MapGenCrater(96);

	private MapGenBubble brine = new MapGenBubble(WorldConfig.minmusBrineSpawn);

	public ChunkProviderMinmus(World world, long seed) {
		super(world, seed);

		smallCrater.setSize(6, 24);
		largeCrater.setSize(64, 128);

		smallCrater.regolith = largeCrater.regolith = LegacyBlocks.b("minmus_regolith");
		smallCrater.rock = largeCrater.rock = LegacyBlocks.b("minmus_stone");

		brine.block = LegacyBlocks.b("ore_brine");
		brine.meta = (byte)world.bodyMeta();
		brine.replace = LegacyBlocks.b("minmus_stone");
		brine.setSize(8, 16);

		stoneBlock = LegacyBlocks.b("minmus_stone");
		seaBlock = LegacyBlocks.b("minmus_smooth");
		seaLevel = 63;
	}

	@Override
	protected Block getFlatWorldBlock(Block block) {
		if(block == Blocks.WATER || block == Blocks.WATER) return LegacyBlocks.b("minmus_smooth");
		if(block == Blocks.GRASS_BLOCK || block == Blocks.SAND) return LegacyBlocks.b("minmus_regolith");
		if(block == Blocks.DIRT || block == Blocks.STONE || block == Blocks.SANDSTONE) return LegacyBlocks.b("minmus_stone");
		if(block == Blocks.SNOW) return Blocks.AIR;
		return block;
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		BlockMetaBuffer buffer = super.getChunkPrimer(x, z);
		brine.setMetas(buffer.metas);

		caveGenerator.func_151539_a(this, worldObj, x, z, buffer.blocks);
		smallCrater.func_151539_a(this, worldObj, x, z, buffer.blocks);
		largeCrater.func_151539_a(this, worldObj, x, z, buffer.blocks);
		brine.func_151539_a(this, worldObj, x, z, buffer.blocks);

		return buffer;
	}

}
