// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)



package com.hbm.space.dim.tekto.biome;

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
import com.hbm.space.dim.tekto.*;
import com.hbm.space.dim.tekto.GenLayerTekto.*;
import com.hbm.space.dim.thatmo.*;




public abstract class BiomeGenBaseTekto extends BiomeGenBaseCelestial {

	public static final BiomeGenBase polyvinylPlains = new BiomeGenPolyvinylPlains(SpaceConfig.tektoPolyvinylBiome);
	public static final BiomeGenBase halogenHills = new BiomeGenHalogenHills(SpaceConfig.tektoHalogenHillBiome);
	public static final BiomeGenBase tetrachloricRiver = new BiomeGenTetrachloricRiver(SpaceConfig.tektoRiverBiome);
	public static final BiomeGenBase forest = new BiomeGenForest(SpaceConfig.tektoForestBiome);
	public static final BiomeGenBase vinylsands = new BiomeGenVinylSands(SpaceConfig.tektoVinylIslandBiome);

	public BiomeGenBaseTekto(int id) {
		super(id);
		this.waterColorMultiplier = 0x5b009a;

		setTemperatureRainfall(1.0F, 0.5F);

		BiomeDecoratorCelestial decorator = new BiomeDecoratorCelestial(LegacyBlocks.b("basalt"));
		decorator.rubberPlantsPerChunk = 64;
		this.theBiomeDecorator = decorator;
		this.theBiomeDecorator.generateLakes = false;

		this.topBlock = LegacyBlocks.b("vinyl_sand");
		this.fillerBlock = LegacyBlocks.b("vinyl_sand");
	}
}