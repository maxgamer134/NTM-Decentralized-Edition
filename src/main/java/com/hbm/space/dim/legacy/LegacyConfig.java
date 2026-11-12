// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim.legacy;

/** The config values NTM:Space's planet generation reads, at NTM:Space's defaults. */
@SuppressWarnings("unused")
public final class LegacyConfig {

    private LegacyConfig() {}

    public static final class GeneralConfig {
        private GeneralConfig() {}

        public static int coltanRate = 2;
        public static boolean enableDebugMode = false;
        public static boolean enableHardcoreDarkness = false;
    }

    public static final class MobConfig {
        private MobConfig() {}

        public static boolean enableMunCows = true;
    }

    public static final class SpaceConfig {
        private SpaceConfig() {}

        public static boolean crashOnBiomeConflict = true;
        public static int dresBasinBiome = 111;
        public static int dresBiome = 112;
        public static int dresDimension = 413_019;
        public static int dunaBiome = 122;
        public static int dunaDimension = 413_016;
        public static int dunaHillsBiome = 119;
        public static int dunaLowlandsBiome = 121;
        public static int dunaPolarBiome = 120;
        public static int dunaPolarHillsBiome = 118;
        public static boolean enableVolcanoGen = true;
        public static int eveBiome = 117;
        public static int eveDimension = 413_018;
        public static int eveMountainsBiome = 116;
        public static int eveOceanBiome = 115;
        public static int eveRiverBiome = 113;
        public static int eveSeismicBiome = 114;
        public static int ikeBiome = 93;
        public static int ikeDimension = 413_017;
        public static int laytheBiome = 97;
        public static int laytheCoastBiome = 94;
        public static int laytheDimension = 413_022;
        public static int laytheOceanBiome = 96;
        public static int laythePolarBiome = 95;
        public static int maxStationDistance = 32_000;
        public static int minmusBasinBiome = 123;
        public static int minmusBiome = 124;
        public static int minmusDimension = 413_021;
        public static int mohoBasaltBiome = 100;
        public static int mohoBiome = 101;
        public static int mohoDimension = 413_020;
        public static int mohoLavaBiome = 99;
        public static int mohoPlateauBiome = 98;
        public static int moonBiome = 125;
        public static int moonDimension = 413_015;
        public static int orbitBiome = 126;
        public static int orbitDimension = 413_023;
        public static int tektoDimension = 413_024;
        public static int tektoForestBiome = 89;
        public static int tektoHalogenHillBiome = 91;
        public static int tektoPolyvinylBiome = 92;
        public static int tektoRiverBiome = 90;
        public static int tektoVinylIslandBiome = 88;
        public static int thatmoBiome = 87;
        public static int thatmoDimension = 413_025;
    }

    public static final class WorldConfig {
        private WorldConfig() {}

        public static int alexandriteSpawn = 100;
        public static int aluminiumClusterSpawn = 3;
        public static int aluminiumSpawn = 7;
        public static int asbestosSpawn = 4;
        public static int berylliumSpawn = 6;
        public static int cinnebarSpawn = 1;
        public static int cobaltSpawn = 2;
        public static int copperClusterSpawn = 4;
        public static int copperSpawn = 12;
        public static int dunaOilSpawn = 100;
        public static int eveGasSpawn = 100;
        public static int fluoriteSpawn = 6;
        public static int ikeBrineSpawn = 100;
        public static int ironClusterSpawn = 4;
        public static int ironSpawn = 9;
        public static int laytheOilSpawn = 100;
        public static int leadSpawn = 6;
        public static int limestoneSpawn = 1;
        public static int lithiumSpawn = 6;
        public static int mineralSpawn = 5;
        public static int minmusBrineSpawn = 100;
        public static int munBrineSpawn = 100;
        public static int netherPhosphorusSpawn = 24;
        public static int nickelSpawn = 9;
        public static int niterSpawn = 6;
        public static int pyramidStructure = 4000;
        public static int radfreq = 5000;
        public static int rareSpawn = 6;
        public static int sulfurSpawn = 5;
        public static int tektoBedrockOilSpawn = 200;
        public static int tektoOilSpawn = 100;
        public static int thoriumSpawn = 7;
        public static int titaniumClusterSpawn = 2;
        public static int titaniumSpawn = 8;
        public static int tungstenSpawn = 10;
        public static int uraniumSpawn = 6;
        public static int zincSpawn = 8;
    }

}
