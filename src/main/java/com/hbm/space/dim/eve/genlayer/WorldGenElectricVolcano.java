// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.eve.genlayer;

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





public class WorldGenElectricVolcano extends WorldGenerator {

	public Block volBlock;
	public Block mainMat;

	public int height;
	public int width;

	public Block surfaceBlock;
	public Block stoneBlock;

	public WorldGenElectricVolcano(int height, int width, Block surfaceBlock, Block stoneBlock) {
		super();
		this.height = height;
		this.width = width;
		this.surfaceBlock = surfaceBlock;
		this.stoneBlock = stoneBlock;

		this.volBlock = LegacyBlocks.b("basalt");
		this.mainMat = LegacyBlocks.b("geysir_electric");
	}

	public boolean generate(World p_76484_1_, Random p_76484_2_, int p_76484_3_, int p_76484_4_, int p_76484_5_) {

		if(p_76484_2_.nextInt(10) != 0) {
			return false;
		}
		while (p_76484_1_.isAirBlock(p_76484_3_, p_76484_4_, p_76484_5_) && p_76484_4_ > 2) {
			--p_76484_4_;
		}

		if(p_76484_1_.getBlock(p_76484_3_, p_76484_4_, p_76484_5_) != surfaceBlock) {
			return false;
		} else {
			p_76484_4_ += p_76484_2_.nextInt(1);
			int l = p_76484_2_.nextInt(4) + height;
			int i1 = l / 4 + p_76484_2_.nextInt(width);

			if(i1 > 1 && p_76484_2_.nextInt(5) == 0) {
				p_76484_2_.nextInt(30);
			}

			int j1;
			int k1;
			int l1;

			for(j1 = 0; j1 < l; ++j1) {
				float f = (1.0F - (float) j1 / (float) l) * (float) i1;
				k1 = Mth.ceil(f);

				for(l1 = -k1; l1 <= k1; ++l1) {
					float f1 = (float) Mth.abs(l1) - 0.25F;

					for(int i2 = -k1; i2 <= k1; ++i2) {
						float f2 = (float) Mth.abs(i2) - 0.75F;

						if((l1 == 0 && i2 == 0 || f1 * f1 + f2 * f2 <= f * f) && (l1 != -k1 && l1 != k1 && i2 != -k1 && i2 != k1 || p_76484_2_.nextFloat() <= 0.75F)) {
							Block block = p_76484_1_.getBlock(p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2);

							if(Material.of(block) == Material.air || block == surfaceBlock || block == stoneBlock) {
								p_76484_1_.setBlock(p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2, LegacyBlocks.b("basalt"));
							}

							if(j1 != 0 && k1 > 1) {
								block = p_76484_1_.getBlock(p_76484_3_ + l1, p_76484_4_ - j1, p_76484_5_ + i2);

								if(Material.of(block) == Material.air || block == surfaceBlock || block == stoneBlock) {
									this.func_150515_a(p_76484_1_, p_76484_3_ + l1, p_76484_4_ - j1, p_76484_5_ + i2, LegacyBlocks.b("ore_depth_nether_neodymium"));
								}
							}
						}
						if((l1 == 0 && j1 == 0) || f1 * f1 + f2 * f2 > f * f) {
							Block block = p_76484_1_.getBlock(p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2);

							if(Material.of(block) == Material.air || block == surfaceBlock || block == stoneBlock) {
								this.func_150515_a(p_76484_1_, p_76484_3_, p_76484_4_ + j1, p_76484_5_, Blocks.AIR);
							}
						}
						if((l1 == 0 && j1 == 0) || f1 * f1 + f2 * f2 > f * f) {
							Block block = p_76484_1_.getBlock(p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2);

							if(Material.of(block) == Material.air || block == surfaceBlock || block == stoneBlock) {
								this.func_150515_a(p_76484_1_, p_76484_3_, p_76484_4_ + 5, p_76484_5_, LegacyBlocks.b("geysir_electric"));
							}
						}
					}
				}
			}

			return true;
		}
	}
}