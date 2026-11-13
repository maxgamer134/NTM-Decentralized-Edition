// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.api.entity.PlayerRespawns;
import com.hbm.space.atmosphere.SpaceAtmosphere;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * WorldProviderCelestial.getRespawnDimension: dying on another body or in orbit only respawns you
 * there if you have a bed on it that you could breathe at; otherwise you wake in the overworld.
 */
public final class SpaceRespawn {

    private SpaceRespawn() {}

    public static void init() {
        PlayerRespawns.register(SpaceRespawn::respawn);
    }

    private static DimensionTransition respawn(ServerPlayer player, DimensionTransition.PostDimensionTransition then) {
        ServerLevel level = player.serverLevel();
        boolean orbit = CelestialBody.inOrbit(level);
        if (!orbit && (level.dimension() == Level.OVERWORLD || CelestialBody.getBodyOrNull(level.dimension()) == null)) return null;
        // a martian world's death on Duna brings you back to Duna (SpaceMartian)
        if (level.dimension() == SpaceDimensions.DUNA && SpaceMartian.isMartian(player.server.overworld())) return null;

        BlockPos bed = player.getRespawnDimension() == level.dimension() ? player.getRespawnPosition() : null;
        if (bed != null && SpaceAtmosphere.canBreathe(SpaceAtmosphere.getAtmosphere(level, bed))) return null;

        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = player.adjustSpawnLocation(overworld, overworld.getSharedSpawnPos());
        return new DimensionTransition(overworld, Vec3.atBottomCenterOf(spawn), Vec3.ZERO, overworld.getSharedSpawnAngle(), 0F, then);
    }
}
