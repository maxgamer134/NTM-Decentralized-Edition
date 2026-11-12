// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

/** The constants of NTM:Space's celestial mechanics. */
public final class AstronomyUtil {

    /** The G in G * M1 * M2 / r. */
    public static final float GRAVITATIONAL_CONSTANT = 6.6743015e-11F;

    /** Added onto a body's radius to get the parking orbit rockets start transfers from. */
    public static final float DEFAULT_ALTITUDE_KM = 100;

    public static final long SECONDS_IN_DAY = 20 * 60;
    public static final long SECONDS_IN_KSP_DAY = 6 * 60 * 60;
    public static final long TICKS_IN_DAY = SECONDS_IN_DAY * 20;

    /** KSP day length to Minecraft day length: orbits run a good deal faster than in KSP. */
    public static final double DAY_FACTOR = (double) SECONDS_IN_DAY / (double) SECONDS_IN_KSP_DAY;

    /** How fast the player falls on Kerbin, in m/s/s (0.08 a tick). */
    public static final float STANDARD_GRAVITY = 1.6F;

    public static final float PLAYER_GRAVITY_MODIFIER = STANDARD_GRAVITY / 9.81F;

    /** Kerbin's orbit is 1 AU. */
    public static final float KM_IN_AU = 13_599_840F;

    /** How quickly time moves, for testing the mechanics. */
    public static long TIME_MULTIPLIER = 1;

    /** Millibuckets per atmosphere: one atmosphere is a gigabucket. */
    public static final double MB_PER_ATM = 1_000_000_000D * 1_000D;

    private AstronomyUtil() {}
}
