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





public class MapGenTiltedSpires extends MapGenBase {

	private final int chanceHigh;
	private final int chanceLow;
	private final float threshold;

	public int minSize = 6;
	public int maxSize = 16;
	public float minPoint = 0.5F;
	public float maxPoint = 4.0F;
	public float minTilt = 0.2F;
	public float maxTilt = 2.5F;
	public boolean curve = false;
	public int mid = 64;

	public Block regolith;
	public Block rock;

	private DoublePerlinNoiseSampler perlin;

	// Note that the chance is effectively squared, so make it lower than you normally would
	public MapGenTiltedSpires(int chanceHigh, int chanceLow, float threshold) {
		this.chanceHigh = chanceHigh;
		this.chanceLow = chanceLow;
		this.threshold = threshold * 2F - 1F; // normalizing to -1, 1 for perlin
		range = 4;
	}

	public void setRange(int range) {
		this.range = range;
	}

	// setup seeded random
	@Override
	public void func_151539_a(Object chunk, World world, int chunkX, int chunkZ, Block[] blocks) {
		if (worldObj != world) {
			this.rand.setSeed(world.getSeed() + 69);
			this.perlin = DoublePerlinNoiseSampler.create(new Random(rand.nextLong()), -8, 1.0D, 2.0D);
		}

		super.func_151539_a(chunk, world, chunkX, chunkZ, blocks);
	}

	// This function is looped over from -this.range to +this.range on both XZ axes.
	@Override
	protected void func_151538_a(World world, int offsetX, int offsetZ, int chunkX, int chunkZ, Block[] blocks) {
		int chance = perlin.sample(offsetX * 16, 0, offsetZ * 16) > threshold ? chanceHigh : chanceLow;

		if(rand.nextInt(chance) == Math.abs(offsetX) % chance && rand.nextInt(chance) == Math.abs(offsetZ) % chance) {

			float coneRadius = rand.nextInt(maxSize - minSize) + minSize;
			float stretch = 1F / (rand.nextFloat() * rand.nextFloat() * (maxPoint - minPoint) + minPoint);
			float direction = rand.nextFloat() * (float)Math.PI * 2F;
			float tilt = rand.nextFloat() * (maxTilt - minTilt) + minTilt;

			if(curve) tilt *= 0.01;

			int xCoord = -offsetX + chunkX;
			int zCoord = -offsetZ + chunkZ;

			// tilt offset
			float tx = (float)Math.cos(direction) * tilt;
			float tz = (float)Math.sin(direction) * tilt;

			// backwards offset (to reduce required range, improving performance)
			int ox = (int)(tx * (curve ? 800 : 8));
			int oz = (int)(tz * (curve ? 800 : 8));

			// the spires stand 1.5 times as tall, as the land they rise from does
			int top = Math.min(ChunkProviderCelestial.MAX_Y - 2, Features.stretchUp(mid + 63));

			for(int bx = 15; bx >= 0; bx--) { // bx, bz is the coordinate of the block we're modifying, relative to the generating chunk origin
				for(int bz = 15; bz >= 0; bz--) {
					int d = 0;

					for(int y = top; y >= ChunkProviderCelestial.MIN_Y; y--) {
						int index = ChunkProviderCelestial.idx(bx, bz, y);

						// Run until the first opaque block
						if(blocks[index] == null || !Material.isOpaqueCube(blocks[index])) {
							// x, z are the coordinates relative to the target virtual chunk origin
							int x = xCoord * 16 + bx - ox;
							int z = zCoord * 16 + bz - oz;
							float oy = (y > 63 ? 63 + (y - 63) / 1.5F : y) - mid;

							float factor = oy;
							if(curve) factor *= factor;

							x += tx * factor;
							z += tz * factor;

							float rs = x * x + z * z;
							float radiusSqr = coneRadius - oy * stretch;
							if(radiusSqr > 0) radiusSqr *= radiusSqr;

							if(rs < radiusSqr) {
								blocks[index] = rock;
								if(d == 1) blocks[index + 1] = regolith;
								d++;
							} else if(d > 0) {
								break;
							}
						} else {
							break;
						}
					}
				}
			}
		}
	}

}
