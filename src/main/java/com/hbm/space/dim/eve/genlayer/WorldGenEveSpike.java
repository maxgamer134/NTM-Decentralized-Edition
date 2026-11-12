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





public class WorldGenEveSpike extends WorldGenerator {

	public boolean generate(World p_76484_1_, Random p_76484_2_, int p_76484_3_, int p_76484_4_, int p_76484_5_) {
		while(p_76484_1_.isAirBlock(p_76484_3_, p_76484_4_, p_76484_5_) && p_76484_4_ > 2) {
			--p_76484_4_;
		}

		if(p_76484_1_.getBlock(p_76484_3_, p_76484_4_, p_76484_5_) != LegacyBlocks.b("eve_silt")) {
			return false;
		} else {
			p_76484_4_ += p_76484_2_.nextInt(4);
			int l = p_76484_2_.nextInt(4) + 10;
			int i1 = l / 4 + p_76484_2_.nextInt(2);

			if(i1 > 1 && p_76484_2_.nextInt(2) == 0) {
				p_76484_4_ += 10 + p_76484_2_.nextInt(30);
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
						float f2 = (float) Mth.abs(i2) - 0.25F;

						if((l1 == 0 && i2 == 0 || f1 * f1 + f2 * f2 <= f * f) && (l1 != -k1 && l1 != k1 && i2 != -k1 && i2 != k1 || p_76484_2_.nextFloat() <= 0.75F)) {
							Block block = p_76484_1_.getBlock(p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2);

							if(Material.of(block) == Material.air || block == LegacyBlocks.b("eve_silt") || block == LegacyBlocks.b("eve_rock")) {
								this.func_150515_a(p_76484_1_, p_76484_3_ + l1, p_76484_4_ + j1, p_76484_5_ + i2, LegacyBlocks.b("eve_rock"));
							}

							if(j1 != 0 && k1 > 1) {
								block = p_76484_1_.getBlock(p_76484_3_ + l1, p_76484_4_ - j1, p_76484_5_ + i2);

								if (Material.of(block) == Material.air || block == LegacyBlocks.b("eve_silt") || block == LegacyBlocks.b("eve_rock")) {
									this.func_150515_a(p_76484_1_, p_76484_3_ + l1, p_76484_4_ - j1, p_76484_5_ + i2, LegacyBlocks.b("eve_rock"));
								}
							}
						}
					}
				}
			}

			j1 = i1 - 1;

			if(j1 < 0) {
				j1 = 0;
			} else if(j1 > 1) {
				j1 = 1;
			}

			for(int j2 = -j1; j2 <= j1; ++j2) {
				k1 = -j1;

				while(k1 <= j1) {
					l1 = p_76484_4_ - 1;
					int k2 = 50;

					if(Math.abs(j2) == 1 && Math.abs(k1) == 1) {
						k2 = p_76484_2_.nextInt(5);
					}

					while(true) {
						if (l1 > 50) {
							Block block1 = p_76484_1_.getBlock(p_76484_3_ + j2, l1, p_76484_5_ + k1);

							if(Material.of(block1) == Material.air || block1 == LegacyBlocks.b("eve_silt") || block1 == LegacyBlocks.b("eve_silt") || block1 == LegacyBlocks.b("eve_silt") || block1 == LegacyBlocks.b("eve_rock")) {
								this.func_150515_a(p_76484_1_, p_76484_3_ + j2, l1, p_76484_5_ + k1, LegacyBlocks.b("eve_rock"));
								--l1;
								--k2;

								if(k2 <= 0) {
									l1 -= p_76484_2_.nextInt(5) + 1;
									k2 = p_76484_2_.nextInt(5);
								}

								continue;
							}
						}

						++k1;
						break;
					}
				}
			}

			return true;
		}
	}
}