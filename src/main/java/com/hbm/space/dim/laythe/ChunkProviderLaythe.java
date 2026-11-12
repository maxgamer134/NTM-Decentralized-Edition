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





public class ChunkProviderLaythe extends ChunkProviderCelestial {

	private MapGenGreg caveGenV3 = new MapGenGreg();
	private MapgenRavineButBased rgen = new MapgenRavineButBased();
	private MapGenRoots roots = new MapGenRoots();

	private MapGenTiltedSpires spires = new MapGenTiltedSpires(2, 14, 0.75F);
	private MapGenTiltedSpires snowires = new MapGenTiltedSpires(2, 14, 0.75F);

	private MapGenBubble oil = new MapGenBubble(WorldConfig.laytheOilSpawn);

	private List<BiomeGenBase.SpawnListEntry> spawnedOfFlesh = new ArrayList<BiomeGenBase.SpawnListEntry>();

	public ChunkProviderLaythe(World world, long seed) {
		super(world, seed);

		rgen.yMin = 32;
		rgen.frequency = 20;
		rgen.strataFreq = 3;
		rgen.strataScale = 8.0F;
		rgen.width = 16.0D;
		rgen.taper = 8.0D;
		rgen.allowUnderwater = true;
		rgen.height = 5.0D;

		snowires.rock = Blocks.PACKED_ICE;
		snowires.regolith = Blocks.SNOW_BLOCK;
		spires.rock = Blocks.STONE;
		spires.regolith = LegacyBlocks.b("laythe_silt");

		spires.curve = snowires.curve = true;
		spires.maxPoint = snowires.maxPoint = 6.0F;
		spires.maxTilt = snowires.maxTilt = 3.5F;

		oil.block = LegacyBlocks.b("ore_oil");
		oil.meta = (byte)world.bodyMeta();
		oil.replace = Blocks.STONE;
		oil.setSize(8, 16);

		seaBlock = Blocks.WATER;
		// seaLevel = 96;

		// entities come with the entity port: spawnedOfFlesh.add(new SpawnListEntry(EntityCreeperFlesh.class, 10, 4, 4));
	}

	@Override
	protected Block getFlatWorldBlock(Block block) {
		if(block == Blocks.GRASS_BLOCK || block == Blocks.SAND || block == Blocks.WATER || block == Blocks.WATER) return Blocks.WATER;
		if(block == Blocks.DIRT || block == Blocks.STONE || block == Blocks.SANDSTONE) return LegacyBlocks.b("laythe_silt");
		if(block == Blocks.SNOW) return Blocks.SNOW;
		return block;
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		BlockMetaBuffer buffer = super.getChunkPrimer(x, z);
		oil.setMetas(buffer.metas);

		if(biomesForGeneration[0] == BiomeGenBaseLaythe.laythePolar) {
			snowires.func_151539_a(this, worldObj, x, z, buffer.blocks);
		} else {
			spires.func_151539_a(this, worldObj, x, z, buffer.blocks);
		}
		roots.func_151539_a(this, worldObj, x, z, buffer.blocks);
		caveGenV3.func_151539_a(this, worldObj, x, z, buffer.blocks);
		rgen.func_151539_a(this, worldObj, x, z, buffer.blocks);
		oil.func_151539_a(this, worldObj, x, z, buffer.blocks);

		return buffer;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getPossibleCreatures(EnumCreatureType creatureType, int x, int y, int z) {
		if(creatureType == EnumCreatureType.monster && worldObj.getBlock(x, y - 1, z) == LegacyBlocks.b("tumor"))
			return spawnedOfFlesh;

		return super.getPossibleCreatures(creatureType, x, y, z);
	}

}