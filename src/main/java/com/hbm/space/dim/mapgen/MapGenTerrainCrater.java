// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.mapgen;

import com.hbm.space.dim.legacy.BiomeGenBase;
import com.hbm.space.dim.legacy.ChunkProviderCelestial;
import com.hbm.space.dim.legacy.MapGenBase;
import com.hbm.space.dim.legacy.Material;
import com.hbm.space.dim.legacy.World;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;

/**
 * NTM's terrain crater (world.gen.terrain.MapGenCrater): one in every {@code frequency} chunks,
 * a bowl with a quadratic floor, lined with rock and with regolith in its middle third.
 */
public class MapGenTerrainCrater extends MapGenBase {

	private int frequency = 100;
	private int minSize = 8;
	private int maxSize = 64;

	public Block regolith;
	public Block rock;

	public BiomeGenBase targetBiome;

	public MapGenTerrainCrater(int frequency) {
		this.frequency = Math.max(1, frequency);
	}

	public void setSize(int minSize, int maxSize) {
		this.minSize = minSize;
		this.maxSize = maxSize;

		this.range = (maxSize / 8) + 1;
	}

	private double depthFunc(double x, double rad, double depth) {
		return -Math.pow(x, 2) / Math.pow(rad, 2) * depth + depth;
	}

	// This function is looped over from -this.range to +this.range on both XZ axes.
	@Override
	protected void func_151538_a(World world, int offsetX, int offsetZ, int chunkX, int chunkZ, Block[] blocks) {
		if(rand.nextInt(frequency) == 0 && (targetBiome == null || targetBiome == world.getBiomeGenForCoords(offsetX * 16, offsetZ * 16))) {
			int xCoord = -offsetX + chunkX;
			int zCoord = -offsetZ + chunkZ;

			double radius = rand.nextInt(maxSize - minSize) + minSize;
			double depth = radius * 0.35D;

			for(int bx = 15; bx >= 0; bx--) { // bx, bz is the coordinate of the block we're modifying, relative to the generating chunk origin
				for(int bz = 15; bz >= 0; bz--) {
					for(int y = ChunkProviderCelestial.MAX_Y - 1; y >= ChunkProviderCelestial.MIN_Y; y--) {
						int index = ChunkProviderCelestial.idx(bx, bz, y);

						if(blocks[index] != null && (Material.isOpaqueCube(blocks[index]) || Material.of(blocks[index]).isLiquid())) {
							// x, z are the coordinates relative to the target virtual chunk origin
							int x = xCoord * 16 + bx;
							int z = zCoord * 16 + bz;

							// y is at the current height now
							double r = Math.sqrt(x * x + z * z);

							if(r - rand.nextInt(3) <= radius) {
								// Carve out to intended depth, never through the floor
								int above = y - ChunkProviderCelestial.MIN_Y;
								int dep = (int) Mth.clamp(depthFunc(r, radius, depth), 0, above - 1);
								for(int i = 0; i < dep; i++) {
									blocks[index - i] = null;
								}

								index -= dep;
								above -= dep;

								dep = Math.min(3, above - 1);

								// Fill back in
								if(r + rand.nextInt(3) <= radius / 3D) {
									for(int i = 0; i < dep; i++) {
										blocks[index - i] = regolith;
									}
								} else {
									for(int i = 0; i < dep; i++) {
										blocks[index - i] = rock;
									}
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
