// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.backport.SavedDataCompat;
import com.hbm.backport.SavedDataType;
import com.hbm.lib.Library;
import com.hbm.space.trait.CelestialBodyTrait;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * What every body can see of the others: terraformed traits (atmospheres, a destroyed star) and
 * the orbital stations. Kept with the overworld. Bodies with no saved traits use their defaults.
 *
 * <p>Clients get the traits through {@link SolarSystemSyncPayload}; {@link #liveTraits} answers
 * from the synced copy on the client thread and from this data everywhere else.
 */
public final class SolarSystemSavedData extends SavedDataCompat {

    public static final SavedDataType<SolarSystemSavedData> TYPE =
            new SavedDataType<>(
                    Library.id("solar_system"),
                    SolarSystemSavedData::new,
                    CompoundTag.CODEC.xmap(SolarSystemSavedData::load, SolarSystemSavedData::save),
                    null);

    private final Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traitMap =
            new HashMap<>();
    /** The orbital stations, by grid cell. */
    private final Map<Long, com.hbm.space.station.OrbitalStation> stations = new HashMap<>();

    private static volatile @Nullable SolarSystemSavedData published;
    private static volatile Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>>
            clientTraits = Map.of();
    private static volatile @Nullable Thread clientThread;

    public SolarSystemSavedData() {}

    /** The server's solar system; created with the overworld's data on first use. */
    public static SolarSystemSavedData get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new IllegalStateException("the solar system is only saved on a server");
        return get(server.overworld());
    }

    public static SolarSystemSavedData get(ServerLevel level) {
        SolarSystemSavedData data =
                SavedDataType.computeIfAbsent(level.getServer().overworld().getDataStorage(), TYPE);
        published = data;
        return data;
    }

    public static void onServerStopped() {
        published = null;
    }

    /** Called once from client setup, so trait lookups on the render thread read the synced copy. */
    public static void markClientThread() {
        clientThread = Thread.currentThread();
    }

    static @Nullable Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> liveTraits(String body) {
        if (Thread.currentThread() == clientThread) return clientTraits.get(body);
        SolarSystemSavedData data = published;
        if (data == null && ServerLifecycleHooks.getCurrentServer() != null) data = get();
        return data == null ? null : data.traitMap.get(body);
    }

    public static Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> clientTraits() {
        return clientTraits;
    }

    public static void updateClientTraits(
            @Nullable Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> traits) {
        clientTraits = traits == null ? Map.of() : traits;
    }

    public static void onClientDisconnect() {
        clientTraits = Map.of();
    }

    public void setTraits(String body, CelestialBodyTrait... traits) {
        if (traits.length == 0) {
            clearTraits(body);
            return;
        }
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> map = new LinkedHashMap<>();
        for (CelestialBodyTrait trait : traits) map.put(trait.getClass(), trait);
        traitMap.put(body, map);
        changed();
    }

    public void clearTraits(String body) {
        if (traitMap.remove(body) != null) changed();
    }

    public @Nullable Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits(String body) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> map = traitMap.get(body);
        return map == null ? null : Collections.unmodifiableMap(map);
    }

    public Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> allTraits() {
        return Collections.unmodifiableMap(traitMap);
    }

    public Map<Long, com.hbm.space.station.OrbitalStation> stations() {
        return stations;
    }

    /** Saves and tells every client: NTM:Space synced the traits each tick instead. */
    public void changed() {
        setDirty();
        SolarSystemSyncPayload.sendToAll(this);
    }

    /** Once a server tick: swarms decay and traits run their updates. */
    public void tick(MinecraftServer server) {
        com.hbm.space.trait.CBT_Dyson dyson = SolarSystem.kerbol.getTrait(com.hbm.space.trait.CBT_Dyson.class);
        if (dyson != null) {
            int before = dyson.size();
            dyson.attenuate(server.overworld().getRandom());
            // NTM:Space never saved decayed swarm members, so they came back on reload
            if (dyson.size() != before && traitMap.containsKey(SolarSystem.kerbol.name)) setDirty();
        }
        for (CelestialBody body : CelestialBody.getAllBodies())
            for (CelestialBodyTrait trait : body.getTraitsUnsafe().values()) trait.update(false, body);
    }

    private static SolarSystemSavedData load(CompoundTag nbt) {
        SolarSystemSavedData data = new SolarSystemSavedData();
        for (String key : nbt.getAllKeys()) {
            if (!key.startsWith("b_")) continue;
            CompoundTag body = nbt.getCompound(key);
            Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = new LinkedHashMap<>();
            for (String name : body.getAllKeys()) {
                CelestialBodyTrait trait = CelestialBodyTrait.create(name);
                if (trait == null) continue;
                trait.readFromNBT(body.getCompound(name));
                traits.put(trait.getClass(), trait);
            }
            data.traitMap.put(key.substring(2), traits);
        }
        ListTag list = nbt.getList("stations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            com.hbm.space.station.OrbitalStation station = com.hbm.space.station.OrbitalStation.load(list.getCompound(i));
            data.stations.put(com.hbm.space.station.OrbitalStation.key(station.dX, station.dZ), station);
        }
        return data;
    }

    private CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        traitMap.forEach(
                (body, traits) -> {
                    CompoundTag data = new CompoundTag();
                    for (CelestialBodyTrait trait : traits.values()) {
                        CompoundTag tag = new CompoundTag();
                        trait.writeToNBT(tag);
                        data.put(CelestialBodyTrait.nameOf(trait), tag);
                    }
                    nbt.put("b_" + body, data);
                });
        ListTag list = new ListTag();
        for (com.hbm.space.station.OrbitalStation station : stations.values()) list.add(station.save());
        nbt.put("stations", list);
        return nbt;
    }
}
