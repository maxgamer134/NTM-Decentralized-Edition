// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim;

import com.hbm.space.dim.legacy.BiomeGenBase;
import com.hbm.space.dim.legacy.ChunkProviderCelestial;
import com.hbm.space.dim.legacy.World;
import com.hbm.space.dim.legacy.WorldChunkManager;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.space.SolarSystem;
import net.minecraft.world.level.block.Block;

/**
 * How one of NTM:Space's planets generates: its biome map (from the world seed), its terrain
 * and carvers, the biomes it can have and the per-planet decorators NTM:Space ran after the
 * biome's own (ores and structures). One world per seed, shared by its biome source and its
 * chunk generator, as 1.7's world was.
 */
public final class Planet {

    /** NTM:Space's per-planet IWorldGenerator. */
    @FunctionalInterface
    public interface Decorator {
        void generate(Random random, int chunkX, int chunkZ, World world);
    }

    public final String name;
    private final int bodyMeta;
    private final Supplier<Block> stone;
    private final LongFunction<WorldChunkManager> biomes;
    private final BiFunction<World, Long, ChunkProviderCelestial> terrain;
    public final List<BiomeGenBase> possibleBiomes;
    public final List<Decorator> decorators;
    private final Map<Long, World> worlds = new ConcurrentHashMap<>();
    private final Map<Long, ChunkProviderCelestial> providers = new ConcurrentHashMap<>();

    private boolean ice;
    private Supplier<FluidStackNTM> acid = () -> null;

    public Planet(SolarSystem.Body body, Supplier<Block> stone, LongFunction<WorldChunkManager> biomes, BiFunction<World, Long, ChunkProviderCelestial> terrain,
            List<BiomeGenBase> possibleBiomes, List<Decorator> decorators) {
        this(body.name, body.ordinal(), stone, biomes, terrain, possibleBiomes, decorators);
    }

    /** A planet off NTM:Space's body list, as Thatmo is; its ores carry the given body metadata. */
    public Planet(String name, int bodyMeta, Supplier<Block> stone, LongFunction<WorldChunkManager> biomes, BiFunction<World, Long, ChunkProviderCelestial> terrain,
            List<BiomeGenBase> possibleBiomes, List<Decorator> decorators) {
        this.name = name;
        this.bodyMeta = bodyMeta;
        this.stone = stone;
        this.biomes = biomes;
        this.terrain = terrain;
        this.possibleBiomes = possibleBiomes;
        this.decorators = decorators;
    }

    /** Bedrock ice generates here. */
    public Planet ice() {
        this.ice = true;
        return this;
    }

    /** What new bedrock ores here take to drill, over the tier's usual fluid. */
    public Planet acid(Supplier<FluidStackNTM> acid) {
        this.acid = acid;
        return this;
    }

    public World world(long seed) {
        return worlds.computeIfAbsent(seed, s -> {
            World world = new World(s, biomes.apply(s));
            world.bodyMeta = bodyMeta;
            world.hasIce = ice;
            world.bedrockAcid = acid.get();
            world.stone = stone.get();
            return world;
        });
    }

    public ChunkProviderCelestial provider(long seed) {
        return providers.computeIfAbsent(seed, s -> terrain.apply(world(s), s));
    }
}
