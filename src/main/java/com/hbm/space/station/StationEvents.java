// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.station;

import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.space.SolarSystemSavedData;
import com.hbm.space.SpaceDimensions;
import com.hbm.space.rocket.EntityRideableRocket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** The orbit's rules: items drift, players hear their station, and the cell's edge drops you home. */
public final class StationEvents {

    private StationEvents() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre event) -> {
            if (event.getLevel().dimension() != SpaceDimensions.ORBIT) return;
            if (!(event.getLevel() instanceof ServerLevel level)) return;
            for (Entity entity : level.getAllEntities()) drift(entity);
            SolarSystemSavedData data = SolarSystemSavedData.get(level);
            for (ServerPlayer player : level.players()) {
                OrbitalStation station = OrbitalStation.getStationFromPosition(data, player.getBlockX(), player.getBlockZ());
                if (level.getGameTime() % 20 == 0) SpaceStations.syncTo(player, station);
                if (player.getVehicle() instanceof EntityRideableRocket) continue;
                double rx = Math.floorMod((int) Math.floor(player.getX()), OrbitalStation.STATION_SIZE);
                double rz = Math.floorMod((int) Math.floor(player.getZ()), OrbitalStation.STATION_SIZE);
                int minBuffer = OrbitalStation.BUFFER_SIZE, maxBuffer = OrbitalStation.STATION_SIZE - minBuffer;
                int minWarning = minBuffer + OrbitalStation.WARNING_SIZE, maxWarning = OrbitalStation.STATION_SIZE - minWarning;
                if (rx < minBuffer || rx > maxBuffer || rz < minBuffer || rz > maxBuffer) {
                    // off the station's patch of sky: back down to the body it orbits
                    ServerLevel to = level.getServer().getLevel(station.orbiting.dimension);
                    if (to == null) continue;
                    int range = Math.max(1, NeoForgeHbmConfig.runtime().spaceMaxProbeDistance());
                    player.teleportTo(to, level.random.nextInt(range * 2) - range, 800, level.random.nextInt(range * 2) - range,
                            player.getYRot(), player.getXRot());
                } else if (rx < minWarning || rx > maxWarning || rz < minWarning || rz > maxWarning) {
                    player.displayClientMessage(Component.translatable("info.orbitfall").withStyle(ChatFormatting.RED), true);
                }
            }
        });
    }

    /** Weightless items: drag, and no fall. Both sides run it, so they agree. */
    public static void drift(Entity entity) {
        if (entity instanceof ItemEntity item) {
            Vec3 v = item.getDeltaMovement();
            item.setDeltaMovement(v.x * 0.9, 0.04, v.z * 0.9); // gravity then takes it to exactly 0
        }
    }
}
