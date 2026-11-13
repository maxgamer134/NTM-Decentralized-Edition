// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.space.SolarSystem.AstroMetric;
import com.hbm.space.station.OrbitalStation;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Eclipses as both sides see them (WorldProviderCelestial.getEclipseFactor and getEclipseAmount),
 * and the sunlight machines get through them (WorldProviderCelestial.getSunPower and
 * WorldProviderOrbit.getSunPower).
 */
public final class SpaceEclipse {

    private SpaceEclipse() {}

    /** How much of the star the nearest large body covers, 0 to 1. */
    public static double factor(List<AstroMetric> metrics, double sunSize, double maxSize) {
        double factor = 0;
        double sunArc = arc(sunSize);
        for (AstroMetric metric : metrics) {
            if (metric.apparentSize < 1) continue;
            double planetArc = arc(Mth.clamp(metric.apparentSize, 0, maxSize));
            double minPhase = 1 - (planetArc + sunArc);
            double maxPhase = 1 - (planetArc - sunArc);
            if (metric.phaseObscure < minPhase) continue;
            double thisFactor = 1 - (metric.phaseObscure - maxPhase) / (minPhase - maxPhase);
            factor = Math.min(Math.max(factor, thisFactor), 1.0);
        }
        return factor;
    }

    // apparentSize to 0-1 of the sky; the flat quads warp the larger they get
    private static double arc(double apparentSize) {
        return apparentSize * 0.0017D + Math.sqrt(apparentSize * 0.00003D);
    }

    /** The body whose surface a level is: a planet, or Kerbin while Space changes NTM. */
    public static @Nullable CelestialBody surface(Level level) {
        CelestialBody body = SpaceTime.body(level);
        if (body == null && level.dimension() == Level.OVERWORLD && SpaceToggle.changes())
            body = CelestialBody.getBodyOrNull(Level.OVERWORLD);
        return body;
    }

    private static final Map<Level, double[]> CACHE = new WeakHashMap<>();

    /** The eclipse over a planet's (or Kerbin's) surface this tick, 0 elsewhere; cached per tick. */
    public static double amount(Level level) {
        CelestialBody body = surface(level);
        if (body == null) return 0;
        long tick = level.getGameTime();
        double[] cached;
        synchronized (CACHE) {
            cached = CACHE.get(level);
        }
        if (cached != null && cached[0] == tick) return cached[1];
        float solarAngle = level.getTimeOfDay(0);
        List<AstroMetric> metrics = SolarSystem.calculateMetricsFromBody(level, 0, body, solarAngle);
        double eclipse = factor(metrics, SolarSystem.calculateSunSize(body), SolarSystem.MAX_APPARENT_SIZE_SURFACE);
        synchronized (CACHE) {
            CACHE.put(level, new double[] {tick, eclipse});
        }
        return eclipse;
    }

    /**
     * The sunlight a solar machine gets, 1 in Kerbin's: the body's, less what an eclipse covers;
     * in orbit the station's, eased toward the target body while it transfers.
     */
    public static float sunPower(Level level, BlockPos pos) {
        if (CelestialBody.inOrbit(level))
            return OrbitalStation.getStationFromPosition(level, pos.getX(), pos.getZ()).getSunPower(level);
        CelestialBody body = surface(level);
        if (body == null) return CelestialBody.getBody(level).getSunPower();
        return body.getSunPower() * (1 - (float) amount(level));
    }
}
