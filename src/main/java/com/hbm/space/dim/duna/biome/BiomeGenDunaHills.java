// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.duna.biome;

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





public class BiomeGenDunaHills extends BiomeGenBaseDuna {

	public static final BiomeGenBase.Height height = new BiomeGenBase.Height(0.525F, 0.51F);

	private byte[] field_150621_aC;
	private long seed;
	private Noise.Perlin perlin1;
	private Noise.Perlin perlin2;
	private Noise.Perlin perlin3;
	private boolean field_150626_aH;
	private boolean field_150620_aI;

	public BiomeGenDunaHills(int id) {
		super(id);
		this.setBiomeName("Weathered Dunaian Hills");
		this.setHeight(height);
	}

	// Ripped from BiomeGenMesa
	@Override
	public void genTerrainBlocks(World world, Random rand, Block[] blocks, byte[] metas, int x, int z, double noise) {
		if (this.field_150621_aC == null || this.seed != world.getSeed()) {
			this.generateBuffers(world.getSeed());
		}

		if (this.perlin1 == null || this.perlin2 == null || this.seed != world.getSeed()) {
			Random random1 = new Random(this.seed);
			this.perlin1 = new Noise.Perlin(random1, 4);
			this.perlin2 = new Noise.Perlin(random1, 1);
		}

		this.seed = world.getSeed();
		double d5 = 0.0D;
		int k;
		int l;

		if (this.field_150626_aH) {
			k = (x & -16) + (z & 15);
			l = (z & -16) + (x & 15);
			double d1 = Math.min(Math.abs(noise),
					this.perlin1.func_151601_a((double) k * 0.25D, (double) l * 0.25D));

			if (d1 > 0.0D) {
				double d2 = 0.001953125D;
				double d3 = Math.abs(this.perlin2.func_151601_a((double) k * d2, (double) l * d2));
				d5 = d1 * d1 * 2.5D;
				double d4 = Math.ceil(d3 * 50.0D) + 14.0D;

				if (d5 > d4) {
					d5 = d4;
				}

				d5 += 64.0D;
			}
		}

		k = x & 15;
		l = z & 15;
		Block block = LegacyBlocks.b("ferric_clay");
		Block block2 = this.fillerBlock;
		int i1 = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
		boolean flag1 = Math.cos(noise / 3.0D * Math.PI) > 0.0D;
		int j1 = -1;
		boolean flag2 = false;
		int k1 = blocks.length / 256;

		for (int l1 = 255; l1 >= 0; --l1) {
			int i2 = (l * 16 + k) * k1 + l1;

			if ((blocks[i2] == null || Material.of(blocks[i2]) == Material.air) && l1 < (int) d5) {
				blocks[i2] = LegacyBlocks.b("duna_rock");
			}

			if (l1 <= 0 + rand.nextInt(5)) {
				blocks[i2] = Blocks.BEDROCK;
			} else {
				Block block1 = blocks[i2];

				if (block1 != null && Material.of(block1) != Material.air) {
					if (block1 == LegacyBlocks.b("duna_rock")) {
						byte b0;

						if (j1 == -1) {
							flag2 = false;

							if (i1 <= 0) {
								block = null;
								block2 = LegacyBlocks.b("duna_rock");
							} else if (l1 >= 59 && l1 <= 64) {
								block = LegacyBlocks.b("ferric_clay");
								block2 = this.fillerBlock;
							}

							if (l1 < 63 && (block == null || Material.of(block) == Material.air)) {
								block = Blocks.WATER;
							}

							j1 = i1 + Math.max(0, l1 - 63);

							if (l1 >= 62) {
								if (this.field_150620_aI && l1 > 86 + i1 * 2) {
									if (flag1) {
										blocks[i2] = LegacyBlocks.b("duna_sands");
									} else {
										blocks[i2] = LegacyBlocks.b("duna_sands");
									}
								} else if (l1 > 66 + i1) {
									b0 = 16;

									if (l1 >= 64 && l1 <= 127) {
										if (!flag1) {
											b0 = this.func_150618_d(x, l1, z);
										}
									} else {
										b0 = 1;
									}

									if (b0 < 16) {
										blocks[i2] = Blocks.TERRACOTTA;
										metas[i2] = 14;
									} else {
										blocks[i2] = LegacyBlocks.b("duna_rock");
									}
								} else {
									blocks[i2] = this.topBlock;
									flag2 = true;
								}
							} else {
								blocks[i2] = block2;
							}
						} else if (j1 > 0) {
							--j1;

							if (flag2) {
								blocks[i2] = LegacyBlocks.b("ferric_clay");
							} else {
								b0 = this.func_150618_d(x, l1, z);

								if(b0 == 4) { // yellow is ugly
									blocks[i2] = LegacyBlocks.b("ferric_clay");
								} else if (b0 == 0) { // white is good for yer teeth
									blocks[i2] = LegacyBlocks.b("stone_resource");
									metas[i2] = (byte)7;
								} else if (b0 < 16) {
									blocks[i2] = Blocks.TERRACOTTA;
									metas[i2] = b0;
								} else {
									blocks[i2] = LegacyBlocks.b("duna_rock");
								}
							}
						}
					}
				} else {
					j1 = -1;
				}
			}
		}
	}

	public void generateBuffers(long seed) {
		this.field_150621_aC = new byte[64];
		Arrays.fill(this.field_150621_aC, (byte) 16);
		Random random = new Random(seed);
		this.perlin3 = new Noise.Perlin(random, 1);
		int j;

		for (j = 0; j < 64; ++j) {
			j += random.nextInt(5) + 1;

			if (j < 64) {
				this.field_150621_aC[j] = 1;
			}
		}

		j = random.nextInt(4) + 2;
		int k;
		int l;
		int i1;
		int j1;

		for (k = 0; k < j; ++k) {
			l = random.nextInt(3) + 1;
			i1 = random.nextInt(64);

			for (j1 = 0; i1 + j1 < 64 && j1 < l; ++j1) {
				this.field_150621_aC[i1 + j1] = 4;
			}
		}

		k = random.nextInt(4) + 2;
		int k1;

		for (l = 0; l < k; ++l) {
			i1 = random.nextInt(3) + 2;
			j1 = random.nextInt(64);

			for (k1 = 0; j1 + k1 < 64 && k1 < i1; ++k1) {
				this.field_150621_aC[j1 + k1] = 12;
			}
		}

		l = random.nextInt(4) + 2;

		for (i1 = 0; i1 < l; ++i1) {
			j1 = random.nextInt(3) + 1;
			k1 = random.nextInt(64);

			for (int l1 = 0; k1 + l1 < 64 && l1 < j1; ++l1) {
				this.field_150621_aC[k1 + l1] = 14;
			}
		}

		i1 = random.nextInt(3) + 3;
		j1 = 0;

		for (k1 = 0; k1 < i1; ++k1) {
			byte b0 = 1;
			j1 += random.nextInt(16) + 4;

			for (int i2 = 0; j1 + i2 < 64 && i2 < b0; ++i2) {
				this.field_150621_aC[j1 + i2] = 0;

				if (j1 + i2 > 1 && random.nextBoolean()) {
					this.field_150621_aC[j1 + i2 - 1] = 8;
				}

				if (j1 + i2 < 63 && random.nextBoolean()) {
					this.field_150621_aC[j1 + i2 + 1] = 8;
				}
			}
		}
	}

	private byte func_150618_d(int p_150618_1_, int p_150618_2_, int p_150618_3_) {
		int l = (int)Math.round(this.perlin3.func_151601_a((double)p_150618_1_ * 1.0D / 512.0D, (double)p_150618_1_ * 1.0D / 512.0D) * 2.0D);
		return this.field_150621_aC[(p_150618_2_ + l + 64) % 64];
	}

}