// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.packet.PacketWire;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.HazePayload;
import com.hbm.packet.toclient.PlasmaBlastPayload;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** backport-fix: BF-161 main 1.0.27_X5000's aux particle packets and Library helpers the old guns use. */
public final class LegacyFx {

    private LegacyFx() {}

    /** main's "vanillaburst": count vanilla particles, gaussian motion */
    public static void vanillaBurst(Level level, double x, double y, double z, ParticleOptions type, int count, double motion) {
        if (level instanceof ServerLevel server) server.sendParticles(type, x, y, z, count, 0, 0, 0, motion);
    }

    public static void haze(Level level, double x, double y, double z) {
        if (level instanceof ServerLevel server)
            PacketWire.sendToAllAround(new HazePayload(x, y, z), new TargetPoint(server, x, y, z, 150));
    }

    public static void plasmaBlast(
            Level level, double x, double y, double z, float r, float g, float b, float pitch, float yaw, float scale) {
        if (level instanceof ServerLevel server)
            PacketWire.sendToAllAround(
                    new PlasmaBlastPayload(x, y, z, r, g, b, pitch, yaw, scale), new TargetPoint(server, x, y, z, 100));
    }

    /** Library.isObstructed: a block between the two points */
    public static boolean isObstructed(Level level, Entity context, double x, double y, double z, double a, double b, double c) {
        return level.clip(new ClipContext(new Vec3(x, y, z), new Vec3(a, b, c), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context))
                        .getType()
                == HitResult.Type.BLOCK;
    }
}
