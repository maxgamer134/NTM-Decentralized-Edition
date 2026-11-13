// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim;

import com.hbm.NuclearTech;
import com.hbm.space.dim.legacy.ChunkProviderCelestial;
import com.hbm.space.dim.legacy.LegacyBlocks;
import com.hbm.space.dim.legacy.World;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * A planet's chunks, made by its NTM:Space generator: terrain, biome surface and carvers in one
 * pass while the chunk fills, then the biome's and the planet's decorators once its neighbours
 * exist. A planet's 1.7 generator and carvers keep state between calls, so each planet makes one
 * chunk at a time.
 */
public class CelestialChunkGenerator extends ChunkGenerator {

    public static final MapCodec<CelestialChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    CelestialBiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.source))
            .apply(i, CelestialChunkGenerator::new));

    private final CelestialBiomeSource source;
    private final Planet planet;

    public CelestialChunkGenerator(CelestialBiomeSource source) {
        super(source);
        this.source = source;
        this.planet = source.planet;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private ChunkProviderCelestial provider() {
        return planet.provider(Planets.seed());
    }

    /** Recently made chunks' buffers, as structure placement asks for the same chunks many times. */
    private static final int CACHE_SIZE = 32;
    private final java.util.LinkedHashMap<Long, ChunkProviderCelestial.BlockMetaBuffer> cache = new java.util.LinkedHashMap<>(CACHE_SIZE, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Long, ChunkProviderCelestial.BlockMetaBuffer> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    /** The provider the cached buffers came from: another world's seed makes another one. */
    private ChunkProviderCelestial cacheProvider;

    /**
     * A chunk's blocks as this planet generates them, in the 1.7 buffer. The buffer may be shared
     * with other callers, so it must only be read.
     */
    public ChunkProviderCelestial.BlockMetaBuffer buffer(int x, int z) {
        ChunkProviderCelestial provider = provider();
        long key = net.minecraft.world.level.ChunkPos.asLong(x, z);
        synchronized (cache) {
            if (cacheProvider != provider) {
                cache.clear();
                cacheProvider = provider;
            }
            ChunkProviderCelestial.BlockMetaBuffer cached = cache.get(key);
            if (cached != null) return cached;
        }
        synchronized (provider) {
            // another thread may have made it while this one waited
            synchronized (cache) {
                ChunkProviderCelestial.BlockMetaBuffer cached = cacheProvider == provider ? cache.get(key) : null;
                if (cached != null) return cached;
            }
            ChunkProviderCelestial.BlockMetaBuffer buffer = provider.getChunkPrimer(x, z);
            provider.modernize(x, z, buffer);
            synchronized (cache) {
                if (cacheProvider == provider) cache.put(key, buffer);
            }
            return buffer;
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        ChunkProviderCelestial.BlockMetaBuffer buffer = buffer(cx, cz);
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int y = ChunkProviderCelestial.MIN_Y; y < ChunkProviderCelestial.MAX_Y; y++) {
            if (y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) continue;
            LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int i = ChunkProviderCelestial.idx(x, z, y);
                    Block block = buffer.blocks[i];
                    if (block == null || block == Blocks.AIR) continue;
                    BlockState state = LegacyBlocks.state(block, buffer.metas[i]);
                    section.setBlockState(x, y & 15, z, state, false);
                    ocean.update(x, y, z, state);
                    surface.update(x, y, z, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        super.applyBiomeDecoration(level, chunk, structures);
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        long seed = Planets.seed();
        World world = planet.world(seed).in(level);
        ChunkProviderCelestial provider = provider();
        // each step on its own, so one failing leaves the others
        try {
            // the global strata hung off DecorateBiomeEvent.Pre, ahead of the biome's decorator
            if (planet.decorators.contains(SpacePlanets.GLOBAL_STRATA)) SpacePlanets.GLOBAL_STRATA.generate(null, cx, cz, world);
        } catch (RuntimeException e) {
            NuclearTech.LOGGER.error("NTM:Space could not lay the strata in chunk {}, {} of {}", cx, cz, planet.name, e);
        }
        try {
            synchronized (provider) {
                provider.populate(world, cx, cz);
            }
        } catch (RuntimeException e) {
            NuclearTech.LOGGER.error("NTM:Space could not populate chunk {}, {} of {}", cx, cz, planet.name, e);
        }
        Random random = new Random(seed);
        long a = random.nextLong(), b = random.nextLong();
        for (Planet.Decorator decorator : planet.decorators) {
            if (decorator == SpacePlanets.GLOBAL_STRATA) continue;
            random.setSeed((long) cx * a ^ (long) cz * b ^ seed);
            try {
                decorator.generate(random, cx, cz, world);
            } catch (RuntimeException e) {
                NuclearTech.LOGGER.error("NTM:Space decorator {} failed in chunk {}, {} of {}", decorator, cx, cz, planet.name, e);
            }
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes, StructureManager structures,
            ChunkAccess chunk, GenerationStep.Carving step) {}

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {}

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // as the noise generator does: Mun's cows only come with the terrain
        net.minecraft.world.level.ChunkPos pos = region.getCenter();
        var biome = region.getBiome(pos.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
        var random = new net.minecraft.world.level.levelgen.WorldgenRandom(new net.minecraft.world.level.levelgen.LegacyRandomSource(
                net.minecraft.world.level.levelgen.RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(region.getSeed(), pos.getMinBlockX(), pos.getMinBlockZ());
        net.minecraft.world.level.NaturalSpawner.spawnMobsForChunkGeneration(region, biome, pos, random);
    }

    @Override
    public int getGenDepth() {
        return ChunkProviderCelestial.H;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return ChunkProviderCelestial.MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        ChunkProviderCelestial.BlockMetaBuffer buffer = buffer(x >> 4, z >> 4);
        for (int y = ChunkProviderCelestial.MAX_Y - 1; y >= ChunkProviderCelestial.MIN_Y; y--) {
            Block block = buffer.blocks[ChunkProviderCelestial.idx(x & 15, z & 15, y)];
            if (block != null && type.isOpaque().test(LegacyBlocks.state(block, 0))) return y + 1;
        }
        return ChunkProviderCelestial.MIN_Y;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        ChunkProviderCelestial.BlockMetaBuffer buffer = buffer(x >> 4, z >> 4);
        BlockState[] states = new BlockState[ChunkProviderCelestial.H];
        for (int y = 0; y < ChunkProviderCelestial.H; y++) {
            int i = ChunkProviderCelestial.idx(x & 15, z & 15, y + ChunkProviderCelestial.MIN_Y);
            states[y] = LegacyBlocks.state(buffer.blocks[i], buffer.metas[i]);
        }
        return new NoiseColumn(ChunkProviderCelestial.MIN_Y, states);
    }

    /** The planet's stone, for old chunks' floors. */
    public BlockState stone() {
        return provider().stone().defaultBlockState();
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("NTM:Space planet " + planet.name);
    }
}
