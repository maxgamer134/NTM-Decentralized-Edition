// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.atmosphere;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.RocketFlameBurstPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** NTM:Space's EntityDepress, the decompression at a hull breach, without the entity. */
public final class SpaceDepress {
    private static final double RANGE = 8;
    private static final List<Breach> ACTIVE = new ArrayList<>();
    private static boolean registered;

    private record Breach(ServerLevel level, Vec3 pos, Direction dir, int[] ttl) {}

    private SpaceDepress() {}

    static synchronized void start(ServerLevel level, Vec3 pos, Direction dir, int ttl) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
                if (event.getLevel() instanceof ServerLevel server) tick(server);
            });
        }
        ACTIVE.add(new Breach(level, pos, dir, new int[] {ttl}));
    }

    private static synchronized void tick(ServerLevel level) {
        ACTIVE.removeIf(b -> {
            if (b.level() != level) return false;
            tick(b);
            return --b.ttl()[0] <= 0;
        });
    }

    private static void tick(Breach b) {
        ServerLevel level = b.level();
        RandomSource rand = level.random;
        Vec3 p = b.pos();
        Direction dir = b.dir();
        PacketWire.sendToAllAround(
                RocketFlameBurstPayload.depress(
                        p.x + rand.nextGaussian() * 0.25, p.y + rand.nextGaussian() * 0.25, p.z + rand.nextGaussian() * 0.25,
                        dir.getStepX() * 0.5 + rand.nextGaussian() * 0.25,
                        dir.getStepY() * 0.5 + rand.nextGaussian() * 0.25,
                        dir.getStepZ() * 0.5 + rand.nextGaussian() * 0.25,
                        100 + rand.nextInt(20)),
                new TargetPoint(level, p.x, p.y, p.z, 150));

        for (Entity entity : level.getEntities((Entity) null, new AABB(p, p).inflate(RANGE))) {
            // a player standing in another room's artificial gravity isn't pulled
            if (entity instanceof Player player && SpaceEnvironment.gravityOf(player) > 0
                    && com.hbm.space.CelestialBody.inOrbit(level)) continue;
            Vec3 vec = p.subtract(entity.position());
            if (vec.length() > RANGE) continue;
            boolean succ = switch (dir) {
                case UP -> entity.getY() < p.y;
                case DOWN -> entity.getY() > p.y;
                case EAST -> entity.getX() < p.x;
                case WEST -> entity.getX() > p.x;
                case SOUTH -> entity.getZ() < p.z;
                case NORTH -> entity.getZ() > p.z;
            };
            Vec3 push = vec.normalize().scale(succ ? 0.1 : -0.1);
            entity.setDeltaMovement(entity.getDeltaMovement().add(push));
            entity.hurtMarked = true;
        }
    }
}
