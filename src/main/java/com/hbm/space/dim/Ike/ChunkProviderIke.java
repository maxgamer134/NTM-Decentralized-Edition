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





public class ChunkProviderIke extends ChunkProviderCelestial {

	private MapGenBase caveGenerator = new MapGenCaves();
	private MapGenTiltedSpires spires = new MapGenTiltedSpires(6, 6, 0F);

	private MapGenBubble brine = new MapGenBubble(WorldConfig.ikeBrineSpawn);
	private MapGenTerrainCrater sellafield = new MapGenTerrainCrater(WorldConfig.radfreq / 10);

	public ChunkProviderIke(World world, long seed) {
		super(world, seed);

		spires.rock = LegacyBlocks.b("ike_stone");
		spires.regolith = LegacyBlocks.b("ike_regolith");
		spires.mid = 86;

		brine.block = LegacyBlocks.b("ore_brine");
		brine.meta = (byte)world.bodyMeta();
		brine.replace = LegacyBlocks.b("ike_stone");
		brine.setSize(8, 16);

		sellafield.regolith = LegacyBlocks.b("sellafield");
		sellafield.rock = LegacyBlocks.b("sellafield_slaked");

		stoneBlock = LegacyBlocks.b("ike_stone");
	}

	@Override
	protected Block getFlatWorldBlock(Block block) {
		if(block == Blocks.WATER || block == Blocks.WATER) return LegacyBlocks.b("bromine_block");
		if(block == Blocks.GRASS_BLOCK || block == Blocks.SAND) return LegacyBlocks.b("ike_regolith");
		if(block == Blocks.DIRT || block == Blocks.STONE || block == Blocks.SANDSTONE) return LegacyBlocks.b("ike_stone");
		if(block == Blocks.SNOW) return Blocks.AIR;
		return block;
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		BlockMetaBuffer buffer = super.getChunkPrimer(x, z);
		brine.setMetas(buffer.metas);

		spires.func_151539_a(this, worldObj, x, z, buffer.blocks);
		caveGenerator.func_151539_a(this, worldObj, x, z, buffer.blocks);
		brine.func_151539_a(this, worldObj, x, z, buffer.blocks);
		sellafield.func_151539_a(this, worldObj, x, z, buffer.blocks);

		return buffer;
	}

	// man fuck Ike, why you gotta be spawning shit again
	@SuppressWarnings("rawtypes")
	@Override
	public List getPossibleCreatures(EnumCreatureType creatureType, int x, int y, int z) {
        return null;
	}

}
