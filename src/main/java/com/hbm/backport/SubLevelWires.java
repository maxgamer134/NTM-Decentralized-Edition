// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blockentity.network.BlockEntityPylonBase;
import com.hbm.platform.NeoForgeServerEvents;
import com.hbm.util.ChunkUtil;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * backport-fix: BF-114 pylon and connector wires between the world and a physics build (Sable
 * sub-level), or between two builds.
 *
 * <p>Main has no moving blocks, so its wires measure, link and draw in one block frame. Here a
 * pylon on a build sits in a far-away plot: its wire length is measured between the world
 * positions of both ends, the wire is drawn to the other end's world position, and a wire whose
 * ends drift further apart than the shorter pylon's reach snaps.
 *
 * <p>Only pylons that have such a spanning wire are watched ({@link #track}), every {@link
 * #CHECK_TICKS} ticks; wires inside one frame (all of main's) never change length and are never
 * checked.
 */
public final class SubLevelWires {

    private static final int CHECK_TICKS = 5;

    // pylons with at least one wire leaving their own frame, per level
    private static final Map<ServerLevel, LongOpenHashSet> SPANNING = new WeakHashMap<>();

    private static int clock;

    private SubLevelWires() {}

    public static void init() {
        NeoForgeServerEvents.onServerTickPost(SubLevelWires::tick);
    }

    /** True when both positions are in the world, or both on the same build. */
    public static boolean sameSpace(Level level, BlockPos a, BlockPos b) {
        SubLevelAccess sa = SableCompanion.INSTANCE.getContaining(level, a);
        SubLevelAccess sb = SableCompanion.INSTANCE.getContaining(level, b);
        if (sa == null || sb == null) return sa == sb;
        return sa == sb || sa.getUniqueId().equals(sb.getUniqueId());
    }

    /** True when {@code pos} lies in the plot grid (a build's space), loaded or not. */
    public static boolean inPlot(Level level, BlockPos pos) {
        return SableCompanion.INSTANCE.isInPlotGrid(level, pos);
    }

    /** World position of a point given in {@code pylon}'s block frame. */
    public static Vec3 toWorld(Level level, Vec3 point) {
        return SubLevelSpace.toWorld(level, point.x, point.y, point.z);
    }

    /** Called whenever a pylon's wire list changes: watch it while a wire leaves its frame. */
    public static void track(ServerLevel level, BlockEntityPylonBase pylon) {
        long key = pylon.getBlockPos().asLong();
        if (pylon.spansSubLevels()) {
            SPANNING.computeIfAbsent(level, l -> new LongOpenHashSet()).add(key);
        } else {
            LongOpenHashSet set = SPANNING.get(level);
            if (set != null) set.remove(key);
        }
    }

    private static void tick(MinecraftServer server) {
        if (SPANNING.isEmpty() || ++clock % CHECK_TICKS != 0) return;
        for (Map.Entry<ServerLevel, LongOpenHashSet> entry : SPANNING.entrySet()) {
            LongOpenHashSet set = entry.getValue();
            if (set.isEmpty()) continue;
            ServerLevel level = entry.getKey();
            // snapping refreshes pylons, which edits the set: walk a copy
            for (long key : set.toLongArray()) check(level, set, key);
        }
    }

    private static void check(ServerLevel level, LongOpenHashSet set, long key) {
        BlockEntityPylonBase pylon =
                ChunkUtil.blockEntityIfLoaded(BlockEntityPylonBase.class, level, BlockPos.of(key));
        if (pylon == null || pylon.isRemoved()) {
            // unloaded or broken: picked up again by onGraphLoad / refreshConnections
            set.remove(key);
            return;
        }
        boolean snapped = false;
        for (long peerKey : pylon.connections) {
            BlockEntityPylonBase peer =
                    ChunkUtil.blockEntityIfLoaded(
                            BlockEntityPylonBase.class, level, BlockPos.of(peerKey));
            // an unloaded end can't move: leave the wire alone
            if (peer == null) continue;
            Vec3 a = toWorld(level, pylon.connectionPoint());
            Vec3 b = toWorld(level, peer.connectionPoint());
            double reach = Math.min(pylon.maxWireLength(), peer.maxWireLength());
            if (a.distanceToSqr(b) <= reach * reach) continue;
            PowerGraph.get(level).removeRemoteLink(key, peerKey);
            snap(level, a.add(b).scale(0.5D));
            peer.refreshConnections();
            snapped = true;
        }
        if (snapped) pylon.refreshConnections();
    }

    private static void snap(ServerLevel level, Vec3 at) {
        level.playSound(
                null, at.x, at.y, at.z, SoundEvents.LEASH_KNOT_BREAK, SoundSource.BLOCKS, 1F, 0.8F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.1);
    }
}
