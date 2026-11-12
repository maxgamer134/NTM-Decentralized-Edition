// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.satellite;

import com.hbm.saveddata.satellites.Satellite;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Satellites as rocket payloads: a custom rocket carries one under its fairing and puts it in
 * orbit of the body its drive names, where NTM's satellites then live per body.
 */
public final class SpaceSatellites {

    private SpaceSatellites() {}

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
