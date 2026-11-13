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
        Satellite.ORBIT_LEVEL.install("hbm_space", (level, type) -> {
            ResourceKey<Level> target = target(type);
            ServerLevel targetLevel = target == null ? null : level.getServer().getLevel(target);
            return targetLevel == null ? level : targetLevel;
        });
        com.hbm.platform.NeoForgeServerEvents.onServerTickPre(server -> {
            if (!migrated) {
                migrated = true;
                migrate(server);
            }
        });
        com.hbm.platform.NeoForgeServerEvents.onServerStopping(server -> migrated = false);
    }

    /** The relay is Duna's, the lunar miner the Mun's, the miner Dres'; the rest stay where launched. */
    private static @org.jspecify.annotations.Nullable ResourceKey<Level> target(SatelliteType type) {
        return type == SatelliteType.RELAY ? SpaceDimensions.DUNA
                : type == SatelliteType.LUNAR_MINER ? SpaceDimensions.MUN
                : type == SatelliteType.MINER ? SpaceDimensions.DRES : null;
    }

    private static boolean migrated;

    /** 1: relays and miners filed under their own body, where new launches put them. */
    private static final int VERSION = 1;

    /**
     * Relays and miners launched before they were filed under their body still sat in the data
     * of the level they left from, where their body's machines no longer look: once per world,
     * they move to their body. A frequency already taken there keeps the old one where it is.
     */
    private static void migrate(net.minecraft.server.MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        Migration marker = com.hbm.backport.SavedDataType.computeIfAbsent(overworld.getDataStorage(), Migration.TYPE);
        if (marker.version >= VERSION) return;
        for (ServerLevel level : server.getAllLevels()) {
            SatelliteSavedData data = SatelliteSavedData.get(level);
            for (Map.Entry<Integer, Satellite> entry : Map.copyOf(data.sats).entrySet()) {
                ResourceKey<Level> key = target(entry.getValue().type());
                if (key == null || key.equals(level.dimension())) continue;
                ServerLevel body = server.getLevel(key);
                if (body == null) continue;
                SatelliteSavedData there = SatelliteSavedData.get(body);
                if (there.isFreqTaken(entry.getKey())) {
                    com.hbm.NuclearTech.LOGGER.warn("Satellite {} on frequency {} stays in {}: the frequency is taken in {}",
                            entry.getValue().type(), entry.getKey(), level.dimension().location(), key.location());
                    continue;
                }
                data.descend(entry.getKey());
                there.put(entry.getKey(), entry.getValue());
            }
        }
        marker.version = VERSION;
        marker.setDirty();
    }

    /** Which of the satellite migrations a world has had. */
    public static final class Migration extends com.hbm.backport.SavedDataCompat {
        static final com.mojang.serialization.Codec<Migration> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
                                com.mojang.serialization.Codec.INT.optionalFieldOf("version", 0).forGetter(m -> m.version))
                        .apply(i, Migration::new));
        static final com.hbm.backport.SavedDataType<Migration> TYPE = new com.hbm.backport.SavedDataType<>(
                com.hbm.lib.Library.id("space_satellite_migration"), Migration::new, CODEC, null);

        int version;

        public Migration() {}

        Migration(int version) {
            this.version = version;
        }
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
