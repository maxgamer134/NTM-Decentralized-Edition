// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** NTM:Space's planets by name, as the data pack's dimensions name their generator. */
public final class Planets {

    private static final Map<String, Planet> PLANETS = new ConcurrentHashMap<>();

    private Planets() {}

    public static void register(Planet planet) {
        if (PLANETS.putIfAbsent(planet.name, planet) != null) throw new IllegalStateException("planet " + planet.name + " exists");
    }

    public static Planet get(String name) {
        SpacePlanets.bootstrap();
        Planet planet = PLANETS.get(name);
        if (planet == null) throw new IllegalArgumentException("no NTM:Space planet " + name);
        return planet;
    }

    /** The world seed; 1.7's planets generated from it, as they do here. */
    public static long seed() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? 0L : server.getWorldData().worldGenOptions().seed();
    }
}
