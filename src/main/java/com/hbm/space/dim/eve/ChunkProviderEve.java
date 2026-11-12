// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.eve;

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




public class ChunkProviderEve extends ChunkProviderCelestial {

	private final Noise.Perlin crackNoise;
	private final MapGenVNoise noise = new MapGenVNoise();

	private MapGenBubble oil = new MapGenBubble(WorldConfig.eveGasSpawn);

	public ChunkProviderEve(World world, long seed) {
		super(world, seed);
		reclamp = false;
		stoneBlock = LegacyBlocks.b("eve_rock");
		seaBlock = LegacyBlocks.b("mercury_block");
		this.crackNoise = new Noise.Perlin(world.rand, 4);

		noise.fluidBlock = LegacyBlocks.b("mercury_block");
		noise.rockBlock = LegacyBlocks.b("eve_rock");
		noise.surfBlock = LegacyBlocks.b("eve_silt");
		noise.cellSize = 72;
		noise.crackSize = 2.0;
		noise.plateThickness = 35;
		noise.shapeExponent = 2.0;
		noise.plateStartY = 57;

		noise.applyToBiome = BiomeGenBaseEve.eveOcean;

		oil.block = LegacyBlocks.b("ore_gas");
		oil.meta = (byte)world.bodyMeta();
		oil.replace = LegacyBlocks.b("eve_rock");
		oil.setSize(8, 16);
	}

	@Override
	protected Block getFlatWorldBlock(Block block) {
		if(block == Blocks.WATER || block == Blocks.WATER) return LegacyBlocks.b("mercury_block");
		if(block == Blocks.GRASS_BLOCK || block == Blocks.SAND) return LegacyBlocks.b("eve_silt");
		if(block == Blocks.DIRT || block == Blocks.STONE || block == Blocks.SANDSTONE) return LegacyBlocks.b("eve_rock");
		if(block == Blocks.SNOW) return Blocks.AIR;
		return block;
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		BlockMetaBuffer buffer = super.getChunkPrimer(x, z);
		oil.setMetas(buffer.metas);

		boolean hasOcean = false;
		boolean hasSeismic = false;

		for(int i = 0; i < biomesForGeneration.length; i++) {
			if(biomesForGeneration[i] == BiomeGenBaseEve.eveOcean) hasOcean = true;
			if(biomesForGeneration[i] == BiomeGenBaseEve.eveSeismicPlains) hasSeismic = true;
			if(hasOcean && hasSeismic) break;
		}

		if(hasSeismic) generateCracks(x, z, buffer);
		if(hasOcean) noise.func_151539_a(this, worldObj, x, z, buffer.blocks);

		oil.func_151539_a(this, worldObj, x, z, buffer.blocks);

		return buffer;
	}


	private void generateCracks(int chunkX, int chunkZ, BlockMetaBuffer buffer) {
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				if(biomesForGeneration[x + z * 16] != BiomeGenBaseEve.eveSeismicPlains) continue;

				double crackValue = crackNoise.func_151601_a((chunkX * 16 + x) * 0.3, (chunkZ * 16 + z) * 0.3);  // Lower scale value for more spread-out cracks

				if(crackValue > 0.9) {
					int bedrockY = -1;
					for(int y = 0; y < 256; y++) {
						int index = (x * 16 + z) * 256 + y;
						if(buffer.blocks[index] == Blocks.BEDROCK) {
							if(bedrockY == -1) bedrockY = y;
						} else {
							buffer.blocks[index] = Blocks.AIR;
						}
					}
					if(bedrockY != -1) {
						for(int y = bedrockY + 1; y < Math.min(bedrockY + 10, 256); y++) {
							int index = (x * 16 + z) * 256 + y;
							buffer.blocks[index] = Blocks.LAVA;
						}
					}
				}
			}
		}
	}

}