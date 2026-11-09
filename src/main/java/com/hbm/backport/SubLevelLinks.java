// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sable compat (not in ntm-next): wires and pipes stretched between the world and physics builds
 * (Sable sub-levels), or between two builds. Pylon wires and pipe anchors link absolute block
 * positions; a block on a build sits in a plot millions of blocks away, so lengths, the wiring
 * readout and the drawn wire came out in plot space. Here every end is taken where it is drawn in
 * the world, and brought back into the frame of the block that draws it. Both ends in the same
 * space (world-world, or the same build) keep the plain arithmetic they always had.
 *
 * <p>Goes through Sable Companion (bundled): without Sable every position is a world position and
 * nothing changes. Works on both sides (the client asks its own level).
 */
public final class SubLevelLinks {

    private SubLevelLinks() {}

    /** The build {@code pos} lies on, or null for the world. */
    public static @Nullable SubLevelAccess frameOf(Level level, BlockPos pos) {
        return SableCompanion.INSTANCE.getContaining(level, pos);
    }

    /** True when the two blocks sit in different spaces (world and build, or two builds). */
    public static boolean crosses(Level level, BlockPos a, BlockPos b) {
        SubLevelAccess fa = frameOf(level, a), fb = frameOf(level, b);
        if (fa == null || fb == null) return fa != fb;
        return !Objects.equals(fa.getUniqueId(), fb.getUniqueId());
    }

    /** Where the point {@code p} (in the space of the block it belongs to) is drawn in the world. */
    public static Vec3 toWorld(Level level, Vec3 p) {
        SubLevelAccess frame = SableCompanion.INSTANCE.getContaining(level, p);
        return frame == null ? p : frame.logicalPose().transformPosition(p);
    }

    /** The distance between two points as drawn in the world, each given in its own space. */
    public static double worldDistance(Level level, Vec3 a, Vec3 b) {
        return Math.sqrt(toWorld(level, a).distanceToSqr(toWorld(level, b)));
    }

    /** The pose a build is drawn at {@code partialTick} of the way from its last tick to this one. */
    private static dev.ryanhcode.sable.companion.math.Pose3dc renderPose(
            SubLevelAccess frame, float partialTick) {
        var last = frame.lastPose();
        var now = frame.logicalPose();
        if (last == null || partialTick >= 1F) return now;
        return last.lerp(now, partialTick, new dev.ryanhcode.sable.companion.math.Pose3d());
    }

    /**
     * World up ((0, 1, 0)) in the frame {@code self} is drawn in, with the build at its pose
     * {@code partialTick} of the way through the tick. Lets two renderers on differently tilted
     * builds hang a wire the same way in the world.
     */
    public static Vec3 worldUp(Level level, BlockPos self, float partialTick) {
        SubLevelAccess frame = frameOf(level, self);
        Vec3 up = new Vec3(0, 1, 0);
        return frame == null ? up : renderPose(frame, partialTick).transformNormalInverse(up);
    }

    /**
     * A number that changes whenever the build holding {@code pos} is drawn somewhere else, and is 0
     * for the world. Lets a renderer skip rebuilding what hangs between builds that are standing still.
     */
    public static long poseKey(Level level, BlockPos pos, float partialTick) {
        SubLevelAccess frame = frameOf(level, pos);
        if (frame == null) return 0L;
        var p = renderPose(frame, partialTick);
        var o = p.orientation();
        long h = Double.doubleToLongBits(p.position().x());
        h = h * 31 + Double.doubleToLongBits(p.position().y());
        h = h * 31 + Double.doubleToLongBits(p.position().z());
        h = h * 31 + Double.doubleToLongBits(o.x());
        h = h * 31 + Double.doubleToLongBits(o.y());
        h = h * 31 + Double.doubleToLongBits(o.z());
        h = h * 31 + Double.doubleToLongBits(o.w());
        return h | 1L;
    }

    /**
     * Like {@link #relative(Level, BlockPos, BlockPos, Vec3)}, but with both builds at the pose they
     * are drawn at this frame. Two renderers that each draw half of something between two builds
     * get the same world point from this, so the halves meet.
     */
    public static Vec3 relative(
            Level level, BlockPos self, BlockPos peer, Vec3 peerPoint, float partialTick) {
        if (!crosses(level, self, peer))
            return peerPoint.subtract(self.getX(), self.getY(), self.getZ());
        SubLevelAccess theirs = SableCompanion.INSTANCE.getContaining(level, peerPoint);
        Vec3 world = theirs == null ? peerPoint : renderPose(theirs, partialTick).transformPosition(peerPoint);
        SubLevelAccess mine = frameOf(level, self);
        Vec3 local =
                mine == null
                        ? world
                        : renderPose(mine, partialTick).transformPositionInverse(world);
        return local.subtract(self.getX(), self.getY(), self.getZ());
    }

    /**
     * The point {@code peerPoint} (in the space of block {@code peer}) relative to block {@code self},
     * in the frame {@code self} is drawn in: what a renderer at {@code self} needs to reach it.
     */
    public static Vec3 relative(Level level, BlockPos self, BlockPos peer, Vec3 peerPoint) {
        if (!crosses(level, self, peer))
            return peerPoint.subtract(self.getX(), self.getY(), self.getZ());
        Vec3 world = toWorld(level, peerPoint);
        SubLevelAccess mine = frameOf(level, self);
        Vec3 local = mine == null ? world : mine.logicalPose().transformPositionInverse(world);
        return local.subtract(self.getX(), self.getY(), self.getZ());
    }
}
