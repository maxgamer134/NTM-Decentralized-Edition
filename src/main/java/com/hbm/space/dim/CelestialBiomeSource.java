// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim;

import com.hbm.space.dim.legacy.BiomeGenBase;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/** A planet's biomes, as its 1.7 layer chain lays them out. */
public class CelestialBiomeSource extends BiomeSource {

    public static final MapCodec<CelestialBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.STRING.fieldOf("planet").forGetter(s -> s.planet.name),
                    RegistryOps.retrieveGetter(Registries.BIOME))
            .apply(i, CelestialBiomeSource::new));

    final Planet planet;
    private final Map<BiomeGenBase, Holder<Biome>> holders = new HashMap<>();

    public CelestialBiomeSource(String planet, HolderGetter<Biome> biomes) {
        this.planet = Planets.get(planet);
        for (BiomeGenBase biome : this.planet.possibleBiomes) holders.put(biome, biomes.getOrThrow(biome.key));
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return planet.possibleBiomes.stream().map(holders::get);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        BiomeGenBase biome = planet.world(Planets.seed()).getBiomeGenForCoords((x << 2) + 2, (z << 2) + 2);
        Holder<Biome> holder = holders.get(biome);
        return holder != null ? holder : holders.get(planet.possibleBiomes.get(0));
    }
}
