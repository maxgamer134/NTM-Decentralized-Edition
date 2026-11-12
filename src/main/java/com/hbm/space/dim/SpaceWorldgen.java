// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim;

import com.hbm.NuclearTech;
import com.hbm.lib.Library;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The planets' world generation: the chunk generator and biome source codecs (hbm:celestial),
 * and the built-in data pack that holds the planets' dimensions, dimension types and biomes. Only
 * registered while NTM:Space is on; the pack is always active then, so worlds made before Space
 * gain the planets when it is switched on.
 */
public final class SpaceWorldgen {

    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, NuclearTech.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, NuclearTech.MOD_ID);

    static {
        GENERATORS.register("celestial", () -> CelestialChunkGenerator.CODEC);
        SOURCES.register("celestial", () -> CelestialBiomeSource.CODEC);
    }

    private SpaceWorldgen() {}

    public static void register(IEventBus modBus) {
        GENERATORS.register(modBus);
        SOURCES.register(modBus);
        modBus.addListener((AddPackFindersEvent event) -> event.addPackFinders(
                Library.id("resourcepacks/space"), PackType.SERVER_DATA, Component.translatable("pack.hbm.space"),
                PackSource.BUILT_IN, true, Pack.Position.TOP));
    }
}
