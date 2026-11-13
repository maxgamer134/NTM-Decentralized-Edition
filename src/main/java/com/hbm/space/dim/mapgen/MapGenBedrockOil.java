// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.mapgen;

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




public class MapGenBedrockOil extends MapGenBaseMeta {

	/**
	 * Similar to oil bubbles, but with a few more behaviours, like adding oily dirt
	 * no porous stone don't @ me
	 */

	private final int frequency;

	public Block block = LegacyBlocks.b("ore_bedrock_oil");
	public Block replace = Blocks.STONE;
	public byte meta = 0;

	public int spotWidth = 5;
	public int spotCount = 50;
	public boolean addWillows = true;

	public MapGenBedrockOil(int frequency) {
		this.frequency = frequency;
		this.range = 4;
	}

	@Override
	protected void func_151538_a(World world, int offsetX, int offsetZ, int chunkX, int chunkZ, Block[] blocks) {
		if(rand.nextInt(frequency) == frequency - 2) {
			int xCoord = (chunkX - offsetX) * 16 + rand.nextInt(16);
			int zCoord = (chunkZ - offsetZ) * 16 + rand.nextInt(16);

			// Add the bedrock oil spot
			for(int bx = 15; bx >= 0; bx--)
			for(int bz = 15; bz >= 0; bz--)
			for(int y = 0; y < 5; y++) {
				// on the bedrock floor, now at the bottom of the deeper world
				int index = ChunkProviderCelestial.idx(bx, bz, ChunkProviderCelestial.MIN_Y + y);

				if(blocks[index] == replace || blocks[index] == Blocks.BEDROCK) {
					// x, z are the coordinates relative to the target virtual chunk origin
					int x = xCoord + bx;
					int z = zCoord + bz;

					if(Math.abs(x) < 5 && Math.abs(z) < 5 && Math.abs(x) + Math.abs(y) + Math.abs(z) <= 6) {
						blocks[index] = block;
						metas[index] = meta;
					}
				}
			}

			int deadMetaCount = 5;

			// Add oil spot damage
			for(int i = 0; i < spotCount; i++) {
				int rx = (int)(rand.nextGaussian() * spotWidth) - xCoord;
				int rz = (int)(rand.nextGaussian() * spotWidth) - zCoord;

				if(rx >= 0 && rx < 16 && rz >= 0 && rz < 16) {
					// find ground level
					for(int y = 127; y >= 4; y--) {
						int index = ChunkProviderCelestial.idx(rx, rz, y);

						if(blocks[index] != null && Material.isOpaqueCube(blocks[index])) {
							for(int oy = 1; oy > -3; oy--) {
								int subIndex = index + oy;

								if(blocks[subIndex] == Blocks.GRASS_BLOCK || blocks[subIndex] == Blocks.DIRT) {
									blocks[subIndex] = rand.nextInt(10) == 0 ? LegacyBlocks.b("dirt_oily") : LegacyBlocks.b("dirt_dead");

									if(addWillows && oy == 0 && rand.nextInt(50) == 0) {
										blocks[subIndex + 1] = LegacyBlocks.b("plant_flower");
										metas[subIndex + 1] = (byte)4;
									}

									// this generation occurs BEFORE decoration, so we have no plants to modify
									// so we'll instead just add some new ones right now
									if(oy == 0 && rand.nextInt(20) == 0) {
										blocks[subIndex + 1] = LegacyBlocks.b("plant_dead");
										metas[subIndex + 1] = (byte)rand.nextInt(deadMetaCount);
									}

									break;
								} else if(blocks[subIndex] == Blocks.SAND || blocks[subIndex] == LegacyBlocks.b("ore_oil_sand")) {
									if(metas[subIndex] == 1) {
										blocks[subIndex] = LegacyBlocks.b("sand_dirty_red");
									} else {
										blocks[subIndex] = LegacyBlocks.b("sand_dirty");
									}
									break;
								} else if(blocks[subIndex] == Blocks.STONE) {
									blocks[subIndex] = LegacyBlocks.b("stone_cracked");
									break;
								} else if(blocks[subIndex] == LegacyBlocks.b("rubber_silt") || blocks[subIndex] == LegacyBlocks.b("rubber_grass") || blocks[subIndex] == LegacyBlocks.b("vinyl_sand")) {
									blocks[subIndex] = LegacyBlocks.b("sellafield_slaked");
									break;
								}
							}

							break;
						}
					}
				}
			}
		}
	}


}
