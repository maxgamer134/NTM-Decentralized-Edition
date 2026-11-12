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





public class MapGenBubble extends MapGenBaseMeta {

	/**
	 * Generates oil bubbles, which are generally wider than a chunk, in a safe + cascadeless manner
	 * Pretty much just an oblate sphere generator (dimensions: 3 x 1 x 3)
	 */

	private final int frequency;
	private int minSize = 8;
	private int maxSize = 64;

	public int minY = 15;
	public int rangeY = 25;

	public boolean fuzzy;

	public Block block;
	public byte meta = 0;
	public Block replace = Blocks.STONE;

	public Predicate<BiomeGenBase> canSpawn;

	public MapGenBubble(int frequency) {
		this.frequency = frequency;
	}

	public void setSize(int minSize, int maxSize) {
		this.minSize = minSize;
		this.maxSize = maxSize;

		this.range = (maxSize / 8) + 1;
	}

	@Override
	protected void func_151538_a(World world, int offsetX, int offsetZ, int chunkX, int chunkZ, Block[] blocks) {
		
		int effecFreq = frequency;
		BiomeGenBase biome = world.getBiomeGenForCoords(offsetX * 16, offsetZ * 16);
		if(biome.temperature >= 2 && biome.rainfall < 0.1) effecFreq /= 3;
		if(effecFreq <= 0) effecFreq = 1;
		
		if(rand.nextInt(effecFreq) == effecFreq - 1 && (canSpawn == null || canSpawn.test(biome))) {
			int xCoord = (chunkX - offsetX) * 16 + rand.nextInt(16);
			int zCoord = (chunkZ - offsetZ) * 16 + rand.nextInt(16);

			int yCoord = rand.nextInt(rangeY) + minY;

			double radius = rand.nextInt(maxSize - minSize) + minSize;
			double radiusSqr = (radius * radius) / 2; // original OilBubble implementation divided the square by 2 for some reason

			int yMin = Math.max(1, Mth.floor(yCoord - radius));
			int yMax = Math.min(127, Mth.ceil(yCoord + radius));

			for(int bx = 15; bx >= 0; bx--) // bx, bz is the coordinate of the block we're modifying, relative to the generating chunk origin
			for(int bz = 15; bz >= 0; bz--)
			for(int by = yMin; by < yMax; by++) {
				int index = (bx * 16 + bz) * 256 + by;

				if(blocks[index] == replace) {
					// x, z are the coordinates relative to the target virtual chunk origin
					int x = xCoord + bx;
					int z = zCoord + bz;
					int y = yCoord - by;

					double rSqr = x * x + z * z + y * y * 3;
					if(fuzzy) rSqr -= rand.nextDouble() * radiusSqr / 3;
					if(rSqr < radiusSqr) {
						blocks[index] = block;
						metas[index] = meta;
					}
				}
			}
			
			if(rand.nextInt(1) == 0) {
				addSurfaceSpot(xCoord, zCoord, blocks);
			}
		}
	}
	
	protected void addSurfaceSpot(int xCoord, int zCoord, Block[] blocks) {

		int deadMetaCount = 5;
		int spotCount = 150;
		int spotWidth = 7;

		// Add oil spot damage
		for(int i = 0; i < spotCount; i++) {
			int offX = (int)(rand.nextGaussian() * spotWidth);
			int offZ = (int)(rand.nextGaussian() * spotWidth);
			int rx = offX - xCoord;
			int rz = offZ - zCoord;

			if(rx >= 0 && rx < 16 && rz >= 0 && rz < 16) {
				// find ground level
				for(int y = 127; y >= 4; y--) {
					int index = (rx * 16 + rz) * 256 + y;

					if(blocks[index] != null && Material.isOpaqueCube(blocks[index])) {
						for(int oy = 1; oy > -3; oy--) {
							int subIndex = index + oy;
							
							int distSq = offX * offX + offZ * offZ;
							boolean inner = distSq < (spotWidth / 2) * (spotWidth / 2);

							if(blocks[subIndex] == Blocks.GRASS_BLOCK || blocks[subIndex] == Blocks.DIRT) {
								blocks[subIndex] = inner ? LegacyBlocks.b("dirt_oily") : LegacyBlocks.b("dirt_dead");

								// this generation occurs BEFORE decoration, so we have no plants to modify
								// so we'll instead just add some new ones right now
								if(!inner && oy == 0 && rand.nextInt(20) == 0) {
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
							}
						}

						break;
					}
				}
			}
		}
		
		// and now for the hole(tm)
		for(int i = 1; i < 6; i++) {
			ForgeDirection dir = ForgeDirection.getOrientation(i);
			int x = dir.offsetX - xCoord;
			int z = dir.offsetZ - zCoord;
			
			if(x >= 0 && x < 16 && z >= 0 && z < 16) {
				int solids = 0;
				
				for(int y = 127; y >= 0; y--) {
					int index = (x * 16 + z) * 256 + y;
					if(blocks[index] == null) continue;
					
					if(Material.of(blocks[index]).isLiquid()) break; 
					
					if(Material.isOpaqueCube(blocks[index])) {
						solids++;
						
						// this approach might break a little when the surface has holes and uneveness but i don't care lmao
						if(i > 1) {
							blocks[index] = LegacyBlocks.b("stone_cracked");
							if(solids >= 4) break;
						} else {
							if(solids < 3) blocks[index] = Blocks.AIR;
							if(solids == 3) blocks[index] = LegacyBlocks.b("oil_spill");
							if(solids > 3 && solids < 7) blocks[index] = LegacyBlocks.b("stone_cracked");
							if(solids >= 7) break;
						}
					}
				}
			}
		}
	}
}
