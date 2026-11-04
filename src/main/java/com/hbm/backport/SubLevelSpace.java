// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * World space vs Sable sub-level space. Blocks of a physics build (Create: Aeronautics) live in a
 * "plot" millions of blocks away and are drawn at the build's pose, so any position taken from a
 * block entity on a build is a plot position. Whatever leaves the build (missiles, projectiles)
 * has to start from the matching world position instead.
 *
 * <p>Goes through Sable Companion (bundled): without Sable installed it returns the position as
 * is.
 */
public final class SubLevelSpace {

    private SubLevelSpace() {}

    /** The world position of {@code (x, y, z)}: unchanged unless it lies inside a sub-level plot. */
    public static Vec3 toWorld(Level level, double x, double y, double z) {
        Vector3d out =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level, new Vector3d(x, y, z), new Vector3d());
        return new Vec3(out.x, out.y, out.z);
    }

    /**
     * The sub-level with this id whose bounds reach into the column around {@code near}, or null if
     * it's gone (disassembled, unloaded) or moved further than {@code radius} away.
     */
    public static dev.ryanhcode.sable.companion.SubLevelAccess find(
            Level level, java.util.UUID id, Vec3 near, double radius) {
        dev.ryanhcode.sable.companion.math.BoundingBox3d column =
                new dev.ryanhcode.sable.companion.math.BoundingBox3d(
                        near.x - radius, level.getMinBuildHeight() - 512, near.z - radius,
                        near.x + radius, level.getMaxBuildHeight() + 2048, near.z + radius);
        for (dev.ryanhcode.sable.companion.SubLevelAccess sub :
                SableCompanion.INSTANCE.getAllIntersecting(level, column)) {
            if (id.equals(sub.getUniqueId())) return sub;
        }
        return null;
    }

    /*
     * backport-fix: BF-117 block entities that measure from themselves to entities. Sable hands a
     * search box around a plot position the world entities around the build (world coordinates),
     * and makes Entity.distanceTo / distanceToSqr build-aware, but raw coordinate math (getX() - x,
     * Vec3.distanceTo, block rays, push directions) still mixes plot and world. These bring a block
     * entity's own points into the world and back; with no build (or no Sable) they change nothing.
     */

    /** The build holding the block at {@code pos}, or null in the world (or without Sable). */
    public static @Nullable SubLevelAccess frame(
            @Nullable Level level, net.minecraft.core.BlockPos pos) {
        return level == null ? null : SableCompanion.INSTANCE.getContaining(level, pos);
    }

    /** A point of {@code sub}'s block frame in the world. */
    public static Vec3 toWorld(
            @Nullable SubLevelAccess sub,
            Vec3 local) {
        return sub == null ? local : sub.logicalPose().transformPosition(local);
    }

    /** A world point in {@code sub}'s block frame. */
    public static Vec3 toLocal(
            @Nullable SubLevelAccess sub,
            Vec3 world) {
        return sub == null ? world : sub.logicalPose().transformPositionInverse(world);
    }

    /** A direction of {@code sub}'s block frame in the world (rotation only). */
    public static Vec3 dirToWorld(
            @Nullable SubLevelAccess sub,
            Vec3 dir) {
        if (sub == null) return dir;
        Vector3d v = sub.logicalPose().orientation().transform(new Vector3d(dir.x, dir.y, dir.z));
        return new Vec3(v.x, v.y, v.z);
    }

    /** True when {@code (x, z)} lies inside a sub-level plot. */
    public static boolean inSubLevel(Level level, double x, double z) {
        return SableCompanion.INSTANCE.getContaining(level, x, z) != null;
    }
}
