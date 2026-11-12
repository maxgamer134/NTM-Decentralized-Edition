// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import java.util.Random;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * 1.7's octave noise, on the game's own improved and simplex noise. Each octave is seeded from
 * one shared java.util.Random stream, as in 1.7, so a planet's noise follows its seed the way it
 * did in NTM:Space.
 */
public final class Noise {

    private Noise() {}

    private static LegacyRandomSource fork(Random rand) {
        return new LegacyRandomSource(rand.nextLong());
    }

    /** NoiseGeneratorOctaves: improved noise summed over halving frequencies. */
    public static class Octaves {
        private final ImprovedNoise[] octaves;

        public Octaves(Random rand, int count) {
            octaves = new ImprovedNoise[count];
            for (int i = 0; i < count; i++) octaves[i] = new ImprovedNoise(fork(rand));
        }

        /** Fills a sizeX by sizeZ by sizeY area (y fastest), sampled at the given spacing. */
        public double[] generateNoiseOctaves(double[] out, int x, int y, int z, int sizeX, int sizeY, int sizeZ,
                double scaleX, double scaleY, double scaleZ) {
            int n = sizeX * sizeY * sizeZ;
            if (out == null || out.length < n) out = new double[n];
            else java.util.Arrays.fill(out, 0, n, 0);
            double f = 1;
            for (ImprovedNoise octave : octaves) {
                double amp = 1 / f;
                int i = 0;
                for (int ix = 0; ix < sizeX; ix++) {
                    double px = wrap((x + ix) * f * scaleX);
                    for (int iz = 0; iz < sizeZ; iz++) {
                        double pz = wrap((z + iz) * f * scaleZ);
                        for (int iy = 0; iy < sizeY; iy++) {
                            double py = (y + iy) * f * scaleY;
                            out[i++] += octave.noise(px, py, pz) * amp;
                        }
                    }
                }
                f /= 2;
            }
            return out;
        }

        /** The 2D form: one layer at y 10. */
        public double[] generateNoiseOctaves(double[] out, int x, int z, int sizeX, int sizeZ, double scaleX, double scaleZ, double unused) {
            return generateNoiseOctaves(out, x, 10, z, sizeX, 1, sizeZ, scaleX, 1, scaleZ);
        }

        private static double wrap(double v) {
            long l = (long) Math.floor(v);
            return v - l + l % 16777216L;
        }
    }

    /** NoiseGeneratorPerlin: simplex noise summed over octaves, for surface depth. */
    public static class Perlin {
        private final SimplexNoise[] octaves;

        public Perlin(Random rand, int count) {
            octaves = new SimplexNoise[count];
            for (int i = 0; i < count; i++) octaves[i] = new SimplexNoise(fork(rand));
        }

        public double func_151601_a(double x, double z) {
            double sum = 0, f = 1;
            for (SimplexNoise octave : octaves) {
                sum += octave.getValue(x * f, z * f) / f;
                f /= 2;
            }
            return sum;
        }

        public double[] func_151599_a(double[] out, double x, double z, int sizeX, int sizeZ, double scaleX, double scaleZ, double persistence) {
            return func_151600_a(out, x, z, sizeX, sizeZ, scaleX, scaleZ, persistence, 0.5);
        }

        public double[] func_151600_a(double[] out, double x, double z, int sizeX, int sizeZ, double scaleX, double scaleZ,
                double persistence, double lacunarity) {
            int n = sizeX * sizeZ;
            if (out == null || out.length < n) out = new double[n];
            else java.util.Arrays.fill(out, 0, n, 0);
            double amp = 1, freq = 1;
            for (SimplexNoise octave : octaves) {
                int i = 0;
                for (int ix = 0; ix < sizeX; ix++)
                    for (int iz = 0; iz < sizeZ; iz++)
                        out[i++] += octave.getValue((x + ix) * scaleX * freq, (z + iz) * scaleZ * freq) * 0.55 / amp * 70 / 70;
                freq *= persistence;
                amp *= lacunarity;
            }
            return out;
        }
    }
}
