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




public class MapGenVolcano extends MapGenBase {

	private int chancePerChunk = 100;
	private int minSize = 32;
	private int maxSize = 64;

	private Block coreBlock;
	private Block materialBlock;

	// Note that the chance is effectively squared, so make it lower than you normally would
	public MapGenVolcano(int chancePerChunk) {
		this.chancePerChunk = chancePerChunk;
	}

	public void setSize(int minSize, int maxSize) {
		this.minSize = minSize;
		this.maxSize = maxSize;

		this.range = (maxSize / 8) + 1;
	}

	public void setMaterial(Block coreBlock, Block mateBlock) {
		this.coreBlock = coreBlock;
		this.materialBlock = mateBlock;
	}
	private double heightFunc(double x, double rad, double depth) {
		double xs = x / (rad * 2);
		double inner = (x * x * x) / (rad / 4) + 32;
		double outer = 1 / (xs * xs);
		return Math.min(inner, outer) * depth;
	}

	// This function is looped over from -this.range to +this.range on both XZ axes.
	@Override
	protected void func_151538_a(World world, int offsetX, int offsetZ, int chunkX, int chunkZ, Block[] blocks) {

		if(rand.nextInt(chancePerChunk) == Math.abs(offsetX) % chancePerChunk && rand.nextInt(chancePerChunk) == Math.abs(offsetZ) % chancePerChunk) {

			double radius = rand.nextInt(maxSize - minSize) + minSize;
			double depth = 0.75D;

			int xCoord = -offsetX + chunkX;
			int zCoord = -offsetZ + chunkZ;

			for(int bx = 15; bx >= 0; bx--) { // bx, bz is the coordinate of the block we're modifying, relative to the generating chunk origin
				for(int bz = 15; bz >= 0; bz--) {
					for(int y = 318; y >= -64; y--) {
						int index = ChunkProviderCelestial.idx(bx, bz, y);

						if(blocks[index] != null && Material.isOpaqueCube(blocks[index])) {
							// x, z are the coordinates relative to the target virtual chunk origin
							int x = xCoord * 16 + bx;
							int z = zCoord * 16 + bz;

							// y is at the current height now
							double r = Math.sqrt(x * x + z * z);

							if(r - rand.nextInt(16) <= radius) {
								// Carve out to intended depth
								int height = (int)Mth.clamp(heightFunc(r, radius, depth), 0, y - 1);
								if(height > 0) {
									for(int i = 0; i < height && i + y < 319; i++) {
										blocks[index + i] = materialBlock;
									}
								} else {
									for(int i = 0; i > height && i + y > ChunkProviderCelestial.MIN_Y + 1; i--) {
										blocks[index + i + 1] = null;
									}
								}

								index += height;
								y += height;

								if(x == 0 && z == 0 && SpaceConfig.enableVolcanoGen) blocks[index + 1] = coreBlock;
							}

							break;
						}
					}
				}
			}
		}
	}
}
