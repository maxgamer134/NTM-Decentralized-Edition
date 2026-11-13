// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.tekto;

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
import com.hbm.space.dim.mapgen.*;
import com.hbm.space.dim.minmus.*;
import com.hbm.space.dim.minmus.biome.*;
import com.hbm.space.dim.minmus.genlayer.*;
import com.hbm.space.dim.moho.*;
import com.hbm.space.dim.moho.biome.*;
import com.hbm.space.dim.moho.genlayer.*;
import com.hbm.space.dim.moon.*;
import com.hbm.space.dim.noise.*;
import com.hbm.space.dim.tekto.GenLayerTekto.*;
import com.hbm.space.dim.tekto.biome.*;
import com.hbm.space.dim.thatmo.*;





public class TTree extends WorldGenAbstractTree {

	int offset;
	int smallest;
	int tallest;
	int xz;
	int y;
	boolean vines;
	Block logBlock;
	Block leavBlock;

	public TTree(boolean notify, int offset, int smallest, int tallest, int xz, int y, boolean vines, Block log, Block leaf) {
		super(notify);
		this.offset = offset;
		this.smallest = smallest;
		this.tallest = tallest;
		this.xz = xz;
		this.y = y;
		this.vines = vines;
		this.logBlock = log;
		this.leavBlock = leaf;
	}

	@Override
	public boolean generate(World world, Random rand, int x, int y, int z) {
		int height = rand.nextInt(smallest) + tallest;

		if (y < 1 || y + height + 1 > 320) {
			return false;
		}

		Block blockBelow = world.getBlock(x, y - 1, z);
		if (blockBelow != LegacyBlocks.b("rubber_grass") && blockBelow != LegacyBlocks.b("rubber_silt")) {
			return false;
		}

		for (int i = 0; i < height; i++) {
			world.setBlock(x, y + i, z, logBlock, 0, 2);
		}


		int bulbStartY = y + height - offset;
		int bulbRadiusXz = xz;
		int bulbRadiusY = this.y;


		for (int dy = -bulbRadiusY; dy <= bulbRadiusY; dy++) {
			for (int dx = -bulbRadiusXz; dx <= bulbRadiusXz; dx++) {
				for (int dz = -bulbRadiusXz; dz <= bulbRadiusXz; dz++) {

					if (Math.pow(dx / (double)bulbRadiusXz, 2) + Math.pow(dy / (double)bulbRadiusY, 2) + Math.pow(dz / (double)bulbRadiusXz, 2) <= 1) {
						Block block = world.getBlock(x + dx, bulbStartY + dy, z + dz);
						if (Material.isAir(block)) {
							{
								// persistent, so the canopy stands even where the trunk's distance isn't tracked
								net.minecraft.world.level.block.state.BlockState leaf = LegacyBlocks.state(leavBlock, 0);
								if (leaf.hasProperty(net.minecraft.world.level.block.LeavesBlock.PERSISTENT))
									leaf = leaf.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
								world.setState(x + dx, bulbStartY + dy, z + dz, leaf);
							}
							if (vines) {
								for (int i = 0; i < 4; i++) {
									int vineX = x + dx + (i % 2 == 0 ? (i - 1) : 0);
									int vineZ = z + dz + (i % 2 == 1 ? (i - 2) : 0);

									if (rand.nextInt(12) == 0) {
										for (int j = 0; j < rand.nextInt(15); j++) {
											int vineY = bulbStartY + dy - j;
											if (world.isAirBlock(vineX, vineY, vineZ)) {
												world.setBlock(vineX, vineY, vineZ, LegacyBlocks.b("vinyl_vines"), 0, 2);
												if (vineX > x + dx) {
													world.setBlockMetadataWithNotify(vineX, vineY, vineZ, 0x2, 2);
												} else if (vineX < x + dx) {
													world.setBlockMetadataWithNotify(vineX, vineY, vineZ, 0x1, 2);
												} else if (vineZ > z + dz) {
													world.setBlockMetadataWithNotify(vineX, vineY, vineZ, 0x4, 2);
												} else if (vineZ < z + dz) {
													world.setBlockMetadataWithNotify(vineX, vineY, vineZ, 0x3, 2);
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
				}
			}
		}

		return true;
	}

}