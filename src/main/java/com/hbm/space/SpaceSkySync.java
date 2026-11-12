// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.space.station.OrbitalStation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * What the sky shows that the server knows: the orbital stations circling the body a player is on
 * and the satellites around it (PermaSyncHandler's CBT and SATELLITES parts), and the nuclear
 * flashes seen on bodies from afar (CelestialNukeShockHandler). NTM:Space resent all of it every
 * tick; here the stations and satellites go to a player only when what they would see changes,
 * checked each second, and the flashes go to everyone when one goes off.
 */
public final class SpaceSkySync {

    /** The war projectiles' impact (hbm:misc.impact). */
    public static final RegistryHandle<SoundEvent> WAR_IMPACT = NeoForgeRegistrar.registerSound("misc.impact");

    private SpaceSkySync() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getLevel().isClientSide() || !(event.getEntity() instanceof EntityNukeExplosionMK5 explosion)) return;
            if (explosion.tickCount > 1 || !(event.getLevel() instanceof ServerLevel level)) return;
            trigger(level, explosion.getX(), explosion.getZ(), explosion.getRadius());
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) NukeShockSyncPayload.send(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> LAST_SENT.remove(event.getEntity().getUUID()));
    }

    // ---------------------------------------------------------------- stations and satellites

    private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

    /** Client side: the stations orbiting the body the player is on, as {dX, dZ}. */
    public static volatile List<int[]> clientStations = List.of();
    /** Client side: the satellites around the player's body, frequency to satellite type name. */
    public static volatile Map<Integer, String> clientSatellites = Map.of();

    private static void tick(MinecraftServer server) {
        long time = server.overworld().getGameTime();
        if (!SHOCKS.isEmpty()) cleanup(SHOCKS, time);
        if (time % 20 != 0) return;
        SolarSystemSavedData solarSystem = SolarSystemSavedData.get(server.overworld());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SkyObjectsSyncPayload payload = SkyObjectsSyncPayload.of(collectStations(player, solarSystem), collectSatellites(player));
            int hash = Arrays.hashCode(payload.data());
            Integer last = LAST_SENT.put(player.getUUID(), hash);
            if (last == null || last != hash) com.hbm.packet.PacketWire.sendTo(payload, player);
        }
    }

    private static List<int[]> collectStations(ServerPlayer player, SolarSystemSavedData solarSystem) {
        ServerLevel level = player.serverLevel();
        if (CelestialBody.inOrbit(level)) return List.of();
        CelestialBody body = CelestialBody.getBodyOrNull(level.dimension());
        if (body == null) return List.of();
        List<int[]> stations = new ArrayList<>();
        for (OrbitalStation station : solarSystem.stations().values()) {
            if (station.hasStation && station.orbiting == body) stations.add(new int[] {station.dX, station.dZ});
        }
        return stations;
    }

    /** SatelliteSavedData.getData(world, x, z): in orbit, the satellites of the body the station circles. */
    private static Map<Integer, String> collectSatellites(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        SatelliteSavedData data;
        if (CelestialBody.inOrbit(level)) {
            OrbitalStation station = OrbitalStation.getStationFromPosition(level, player.getBlockX(), player.getBlockZ());
            ServerLevel orbited = station == null || station.orbiting == null || station.orbiting.dimension == null
                    ? null : level.getServer().getLevel(station.orbiting.dimension);
            if (orbited == null) return Map.of();
            data = SatelliteSavedData.get(orbited);
        } else {
            data = SatelliteSavedData.at(level, player.getBlockX(), player.getBlockZ());
        }
        Map<Integer, String> sats = new LinkedHashMap<>();
        for (Map.Entry<Integer, Satellite> entry : data.sats.entrySet()) {
            if (entry.getValue() == null) continue;
            sats.put(entry.getKey(), entry.getValue().type().getSerializedName());
        }
        return sats;
    }

    // ---------------------------------------------------------------- nuclear flashes

    private static final long MAX_AGE = 220L;
    public static final int MAX_ACTIVE_SHOCKS = 4;
    private static final float FLASH_GRID = 16.0F;

    /** CelestialNukeShockHandler.ShockStatus: when and where on the body's disc, and how big. */
    public record ShockStatus(long time, float centerX, float centerY, float strength) {}

    /** By body name (NTM:Space keyed them by dimension id). */
    private static final Map<String, List<ShockStatus>> SHOCKS = new HashMap<>();
    private static volatile Map<String, List<ShockStatus>> clientShocks = Map.of();

    /** A nuke went off: the body it hit (the orbited one, from a station) flashes for everyone looking. */
    public static void trigger(ServerLevel level, double x, double z, float blastRadius) {
        CelestialBody body;
        if (CelestialBody.inOrbit(level)) {
            OrbitalStation station = OrbitalStation.getStationFromPosition(level, Mth.floor(x), Mth.floor(z));
            body = station == null ? null : station.orbiting;
        } else {
            body = CelestialBody.getBodyOrNull(level.dimension());
        }
        if (body == null) return;
        long time = level.getGameTime();
        float rawX = 0.18F + hashToUnit(x * 0.03125D + z * 0.0125D + time * 0.017D) * 0.64F;
        float rawY = 0.22F + hashToUnit(x * -0.015D + z * 0.0275D + time * 0.011D + 19.0D) * 0.52F;
        float strength = Mth.clamp(blastRadius / 140.0F, 0.35F, 1.0F);
        List<ShockStatus> statuses = SHOCKS.computeIfAbsent(body.name, k -> new ArrayList<>());
        prune(statuses, time);
        statuses.add(new ShockStatus(time, quantizeCenter(rawX), quantizeCenter(rawY), strength));
        while (statuses.size() > MAX_ACTIVE_SHOCKS) statuses.remove(0);
        NukeShockSyncPayload.sendToAll();
    }

    static Map<String, List<ShockStatus>> serverShocks(long time) {
        cleanup(SHOCKS, time);
        return SHOCKS;
    }

    static void setClientShocks(Map<String, List<ShockStatus>> shocks) {
        clientShocks = shocks;
    }

    /** The flashes on a body as the client last heard of them; old ones are dropped by the caller's time. */
    public static List<ShockStatus> getClientShocks(@Nullable CelestialBody body, long time) {
        if (body == null) return Collections.emptyList();
        List<ShockStatus> statuses = clientShocks.get(body.name);
        if (statuses == null || statuses.isEmpty()) return Collections.emptyList();
        List<ShockStatus> live = new ArrayList<>(statuses.size());
        for (ShockStatus status : statuses) if (time - status.time() <= MAX_AGE) live.add(status);
        return live;
    }

    public static void clearClient() {
        clientShocks = Map.of();
        clientStations = List.of();
        clientSatellites = Map.of();
    }

    private static void cleanup(Map<String, List<ShockStatus>> statuses, long time) {
        Iterator<Map.Entry<String, List<ShockStatus>>> iterator = statuses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<ShockStatus>> entry = iterator.next();
            prune(entry.getValue(), time);
            if (entry.getValue().isEmpty()) iterator.remove();
        }
    }

    private static void prune(List<ShockStatus> statuses, long time) {
        statuses.removeIf(status -> time - status.time() > MAX_AGE);
    }

    private static float hashToUnit(double value) {
        return (float) (value - Math.floor(value));
    }

    private static float quantizeCenter(float value) {
        float clamped = Mth.clamp(value, 0.05F, 0.95F);
        return (Mth.floor(clamped * FLASH_GRID) + 0.5F) / FLASH_GRID;
    }
}
