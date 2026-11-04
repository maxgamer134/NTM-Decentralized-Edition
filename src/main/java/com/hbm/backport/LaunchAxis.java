// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * backport-fix: BF-113 — missiles and rockets leave a launch pad that sits tilted on a physics build
 * (Create: Aeronautics) along the pad's own up axis instead of straight up.
 *
 * <p>Main only knows upright pads, so every launch here falls back to main's exact flight when the
 * pad is upright (not on a build, or the build stands level). On a tilted pad:
 *
 * <ul>
 *   <li>rockets (Lambda-98, Soyuz) start along the pad axis and bend to vertical over their first
 *       {@link #ROCKET_STEER_TICKS} ticks of thrust, so they still climb to orbit;
 *   <li>ballistic missiles fly main's arc, with an exit offset added that starts along the pad axis
 *       and fades back to zero over the first {@link #EXIT_LENGTH} blocks of flight, so they still
 *       land on their target;
 *   <li>interceptors fly their 40-tick boost along the pad axis.
 * </ul>
 */
public final class LaunchAxis {

    public static final Vec3 UP = new Vec3(0, 1, 0);

    /** Ticks of thrust over which a rocket turns from the pad axis to straight up. */
    public static final int ROCKET_STEER_TICKS = 100;

    /** Rocket thrust ramps by this much per tick (main's lambda and soyuz). */
    public static final double ROCKET_ACCEL_STEP = 0.00025D;

    /** Blocks of flight over which a missile's exit offset fades back into main's arc. */
    public static final double EXIT_LENGTH = 30D;

    private LaunchAxis() {}

    /**
     * World direction of the block frame's +Y at {@code (x, y, z)}: straight up unless the point lies
     * on a tilted Sable sub-level.
     */
    public static Vec3 padUp(Level level, double x, double y, double z) {
        if (level == null || !SubLevelSpace.inSubLevel(level, x, z)) return UP;
        Vec3 a = SubLevelSpace.toWorld(level, x, y, z);
        Vec3 d = SubLevelSpace.toWorld(level, x, y + 1D, z).subtract(a);
        if (d.lengthSqr() < 1.0e-6) return UP;
        d = d.normalize();
        return upright(d) ? UP : d;
    }

    /** Within about a quarter degree of straight up: main's flight applies. */
    public static boolean upright(Vec3 axis) {
        return axis.y > 0.99999D;
    }

    /** {@code axis} bent towards straight up: {@code k} 0 is the axis, 1 (or more) is straight up. */
    public static Vec3 steer(Vec3 axis, double k) {
        if (k >= 1D || upright(axis)) return UP;
        if (k <= 0D) return axis;
        Vec3 a = axis;
        // straight down has no side to turn through: pick one
        if (a.y < -0.999D) a = new Vec3(0.05D, a.y, 0D).normalize();
        Vec3 d = a.scale(1D - k).add(0D, k, 0D);
        return d.lengthSqr() < 1.0e-8 ? UP : d.normalize();
    }

    /** Steering progress of a rocket whose thrust ramp has reached {@code acceleration}. */
    public static double rocketSteer(double acceleration) {
        return acceleration / (ROCKET_ACCEL_STEP * ROCKET_STEER_TICKS);
    }

    /**
     * Offset from main's arc after {@code travelled} blocks of flight. Its slope at 0 is {@code axis -
     * UP}, so the first step (main's is straight up) points along the axis; it is back to zero, and
     * flat, at {@link #EXIT_LENGTH}.
     */
    public static Vec3 exitOffset(Vec3 axis, double travelled) {
        if (upright(axis) || travelled <= 0D || travelled >= EXIT_LENGTH) return Vec3.ZERO;
        double t = 1D - travelled / EXIT_LENGTH;
        return axis.subtract(UP).scale(travelled * t * t);
    }

    /** Rotation taking a model's +Y onto {@code dir}. */
    public static Quaternionf rotation(Vec3 dir) {
        return new Quaternionf().rotationTo(0F, 1F, 0F, (float) dir.x, (float) dir.y, (float) dir.z);
    }
}
