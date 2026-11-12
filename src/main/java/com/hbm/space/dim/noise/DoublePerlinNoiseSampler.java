// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.noise;

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
import com.hbm.space.dim.tekto.*;
import com.hbm.space.dim.tekto.GenLayerTekto.*;
import com.hbm.space.dim.tekto.biome.*;
import com.hbm.space.dim.thatmo.*;



//dear greg cave guy: sorry im just messing around :p
public class DoublePerlinNoiseSampler {

	private final double amplitude;
	private final OctavePerlinNoiseSampler firstSampler;
	private final OctavePerlinNoiseSampler secondSampler;

	public static DoublePerlinNoiseSampler create(Random random, int offset, double... octaves) {
		List<Double> list = new ArrayList<>();
		for (double octave : octaves) {
			list.add(octave);
		}

		return new DoublePerlinNoiseSampler(random, offset, list);
	}

	public static DoublePerlinNoiseSampler create(Random random, int offset, List<Double> octaves) {
		return new DoublePerlinNoiseSampler(random, offset, octaves);
	}

	private DoublePerlinNoiseSampler(Random random, int offset, List<Double> octaves) {
		this.firstSampler = OctavePerlinNoiseSampler.create(random, offset, octaves);
		this.secondSampler = OctavePerlinNoiseSampler.create(random, offset, octaves);
		int i = 2147483647;
		int j = -2147483648;
		ListIterator<Double> doubleListIterator = octaves.listIterator();

		while(doubleListIterator.hasNext()) {
			int k = doubleListIterator.nextIndex();
			double d = doubleListIterator.next();
			if (d != 0.0D) {
				i = Math.min(i, k);
				j = Math.max(j, k);
			}
		}

		this.amplitude = 0.16666666666666666D / createAmplitude(j - i);
	}

	private static double createAmplitude(int octaves) {
		return 0.1D * (1.0D + 1.0D / (double)(octaves + 1));
	}

	public double sample(double x, double y, double z) {
		double d = x * 1.0181268882175227D;
		double e = y * 1.0181268882175227D;
		double f = z * 1.0181268882175227D;
		return (this.firstSampler.sample(x, y, z) + this.secondSampler.sample(d, e, f)) * this.amplitude;
	}
}
