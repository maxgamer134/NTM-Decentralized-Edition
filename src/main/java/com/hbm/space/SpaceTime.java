// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.backport.SavedDataCompat;
import com.hbm.backport.SavedDataType;
import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Each planet keeps its own clock, and its days run as long as it turns (NTM:Space's
 * WorldProviderCelestial and CelestialBodyWorldSavedData). The overworld and orbit keep vanilla time.
 */
public final class SpaceTime {

    private SpaceTime() {}

    private static final class LocalTime extends SavedDataCompat {
        long time = -1;
    }

    private static final SavedDataType<LocalTime> TYPE = new SavedDataType<>(Library.id("local_time"), LocalTime::new,
            Codec.LONG.xmap(t -> {
                LocalTime l = new LocalTime();
                l.time = t;
                return l;
            }, l -> l.time), null);

    private static final Map<ServerLevel, LocalTime> CACHE = new WeakHashMap<>();

    /** The planet a level is the surface of, or null for vanilla time. */
    public static @Nullable CelestialBody body(Level level) {
        if (!SpaceToggle.enabled() || level.dimension() == Level.OVERWORLD) return null;
        CelestialBody body = CelestialBody.getBodyOrNull(level.dimension());
        if (body == null || body.parent == null || CelestialBody.inOrbit(level)) return null;
        return body;
    }

    /** Ticks from one local noon to the next: the turn, plus the bit more the orbit adds. */
    public static double dayLength(CelestialBody body) {
        return body.getRotationalPeriod() / (1 - (1 / body.getPlanet().getOrbitalPeriod()));
    }

    private static LocalTime data(ServerLevel level) {
        return CACHE.computeIfAbsent(level, l -> {
            LocalTime t = SavedDataType.computeIfAbsent(l.getDataStorage(), TYPE);
            if (t.time < 0) {
                t.time = l.getServer().overworld().getDayTime();
                t.setDirty();
            }
            return t;
        });
    }

    /** The planet's time on the server, or -1 to use vanilla's. */
    public static long get(Level level) {
        if (level.isClientSide || !(level instanceof ServerLevel server) || body(level) == null) return -1;
        return data(server).time;
    }

    /** Sets the planet's time; false for levels on vanilla time. */
    public static boolean set(Level level, long time) {
        if (!(level instanceof ServerLevel server) || body(level) == null) return false;
        LocalTime t = data(server);
        t.time = time;
        t.setDirty();
        return true;
    }

    public static void tick(ServerLevel level) {
        if (body(level) == null || !level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) return;
        LocalTime t = data(level);
        t.time++;
        t.setDirty();
    }

    /** After everyone slept, the next local morning (NTM:Space rewound to the start of the day instead). */
    public static long morning(Level level, long vanilla) {
        CelestialBody body = body(level);
        if (body == null) return vanilla;
        long day = (long) dayLength(body);
        long now = level.getDayTime();
        return now + day - now % day;
    }

    /** The solar angle, NaN for vanilla's. */
    public static float timeOfDay(Level level, float partialTicks) {
        CelestialBody body = body(level);
        if (body == null) return Float.NaN;
        double dayLength = dayLength(body);
        double f1 = (level.getDayTime() % dayLength + partialTicks) / dayLength - 0.25;
        if (f1 < 0) ++f1;
        if (f1 > 1) --f1;
        double f2 = f1;
        f1 = 0.5 - Math.cos(f1 * Math.PI) / 2.0;
        return (float) (f2 + (f1 - f2) / 3.0);
    }
}
