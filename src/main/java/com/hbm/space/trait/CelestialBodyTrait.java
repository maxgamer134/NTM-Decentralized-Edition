// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.hbm.space.CelestialBody;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

/**
 * A property of a celestial body that can change while the game runs (its atmosphere, its
 * temperature, a war on it). The bodies hold their defaults; terraforming saves replacements in
 * {@link com.hbm.space.SolarSystemSavedData}, which also syncs them to clients. A trait saves
 * itself to NBT under its registered name and to the network, both no-ops for marker traits.
 */
public abstract class CelestialBodyTrait {

    /** A body fought over (Thatmo): marker, no data. */
    public static final class CBT_Battlefield extends CelestialBodyTrait {}

    /** A body overrun by an invasion: marker, no data. */
    public static final class CBT_Compromised extends CelestialBodyTrait {}

    public static final List<Class<? extends CelestialBodyTrait>> traitList = new ArrayList<>();
    public static final BiMap<String, Class<? extends CelestialBodyTrait>> traitMap =
            HashBiMap.create();
    private static final Map<Class<? extends CelestialBodyTrait>, Supplier<? extends CelestialBodyTrait>>
            FACTORIES = new HashMap<>();

    static {
        register("atmosphere", CBT_Atmosphere.class, CBT_Atmosphere::new);
        register("temperature", CBT_Temperature.class, CBT_Temperature::new);
        register("bees", CBT_Bees.class, CBT_Bees::new);
        register("war", CBT_War.class, CBT_War::new);
        register("destroyed", CBT_Destroyed.class, CBT_Destroyed::new);
        register("water", CBT_Water.class, CBT_Water::new);
        register("battle", CBT_Battlefield.class, CBT_Battlefield::new);
        register("infected", CBT_Compromised.class, CBT_Compromised::new);
        register("dyson", CBT_Dyson.class, CBT_Dyson::new);
        register("impact", CBT_Impact.class, CBT_Impact::new);
        register("lights", CBT_Lights.class, CBT_Lights::new);
        register("weather", CBT_Weather.class, CBT_Weather::new);
        register("invasion", CBT_Invasion.class, CBT_Invasion::new);
    }

    /** Registers a trait under the name it saves as (war, weather and invasion add theirs). */
    public static <T extends CelestialBodyTrait> void register(
            String name, Class<T> clazz, Supplier<T> factory) {
        if (traitMap.putIfAbsent(name, clazz) != null)
            throw new IllegalStateException("celestial body trait " + name + " is already registered");
        traitList.add(clazz);
        FACTORIES.put(clazz, factory);
    }

    public static @Nullable CelestialBodyTrait create(String name) {
        Class<? extends CelestialBodyTrait> clazz = traitMap.get(name);
        return clazz == null ? null : FACTORIES.get(clazz).get();
    }

    public static String nameOf(CelestialBodyTrait trait) {
        return traitMap.inverse().get(trait.getClass());
    }

    /** A copy safe to modify: Space handed out the saved instances, so edits leaked unsaved. */
    public CelestialBodyTrait copy() {
        CompoundTag tag = new CompoundTag();
        writeToNBT(tag);
        CelestialBodyTrait copy = create(nameOf(this));
        copy.readFromNBT(tag);
        return copy;
    }

    public void readFromNBT(CompoundTag nbt) {}

    public void writeToNBT(CompoundTag nbt) {}

    public void readFromBytes(FriendlyByteBuf buf) {}

    public void writeToBytes(FriendlyByteBuf buf) {}

    public void update(boolean isRemote, CelestialBody body) {}
}
