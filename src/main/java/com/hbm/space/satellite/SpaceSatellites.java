// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.satellite;

import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.space.CelestialBody;
import com.hbm.space.SpaceDimensions;
import com.hbm.space.station.OrbitalStation;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Satellites as rocket payloads: a custom rocket carries one under its fairing and puts it in
 * orbit of the body its drive names, where NTM's satellites then live per body.
 */
public final class SpaceSatellites {

    private SpaceSatellites() {}

    /** Satellites live per body: SatelliteSavedData.getData and XSatelliteRegistry.getTargetDimensionId. */
    public static void install() {
        // in orbit the satellites are those of the body the station at x, z circles
        SatelliteSavedData.AT.install("hbm_space", (level, x, z) -> {
            if (!CelestialBody.inOrbit(level)) return SatelliteSavedData.get(level);
            OrbitalStation station = OrbitalStation.getStationFromPosition(level, x, z);
            ServerLevel orbited = station.orbiting == null || station.orbiting.dimension == null
                    ? null : level.getServer().getLevel(station.orbiting.dimension);
            return SatelliteSavedData.get(orbited == null ? level : orbited);
        });
        // the relay is Duna's, the lunar miner the Mun's, the miner Dres'; the rest stay where launched
        Satellite.ORBIT_LEVEL.install("hbm_space", (level, type) -> {
            ResourceKey<Level> target = type == SatelliteType.RELAY ? SpaceDimensions.DUNA
                    : type == SatelliteType.LUNAR_MINER ? SpaceDimensions.MUN
                    : type == SatelliteType.MINER ? SpaceDimensions.DRES : null;
            ServerLevel targetLevel = target == null ? null : level.getServer().getLevel(target);
            return targetLevel == null ? level : targetLevel;
        });
    }

    /** NTM:Space's satellite masses in kg, by item; the rest weigh 16t. */
    private static final Map<String, Integer> MASS = Map.of(
            "hbm:sat_laser", 48_000,
            "hbm:sat_foeq", 20_000,
            "hbm:sat_resonator", 20_000,
            "hbm:sat_miner", 64_000,
            "hbm:sat_lunar_miner", 64_000,
            "hbm:sat_gerald", 128_000,
            "hbm:sat_dyson_relay", 32_000);

    public static int mass(String item) {
        return MASS.getOrDefault(item, 16_000);
    }

    public static void orbitFromRocket(ServerLevel level, ItemStack satellite, int freq, double x, double y, double z) {
        if (!satellite.isEmpty()) Satellite.orbit(level, satellite, freq, x, y, z);
    }
}
