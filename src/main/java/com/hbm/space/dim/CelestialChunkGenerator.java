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

    private ChunkProviderCelestial.BlockMetaBuffer buffer(int x, int z) {
        ChunkProviderCelestial provider = provider();
        synchronized (provider) {
            return provider.getChunkPrimer(x, z);
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        ChunkProviderCelestial.BlockMetaBuffer buffer = buffer(cx, cz);
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int y = 0; y < 256; y++) {
            if (y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) continue;
            LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int i = x << 12 | z << 8 | y;
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
        try {
            synchronized (provider) {
                provider.populate(world, cx, cz);
            }
            Random random = new Random(seed);
            long a = random.nextLong(), b = random.nextLong();
            for (Planet.Decorator decorator : planet.decorators) {
                random.setSeed((long) cx * a ^ (long) cz * b ^ seed);
                decorator.generate(random, cx, cz, world);
            }
        } catch (RuntimeException e) {
            NuclearTech.LOGGER.error("NTM:Space could not decorate chunk {}, {} of {}", cx, cz, planet.name, e);
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes, StructureManager structures,
            ChunkAccess chunk, GenerationStep.Carving step) {}

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {}

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {}

    @Override
    public int getGenDepth() {
        return 256;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        ChunkProviderCelestial.BlockMetaBuffer buffer = buffer(x >> 4, z >> 4);
        for (int y = 255; y >= 0; y--) {
            Block block = buffer.blocks[(x & 15) << 12 | (z & 15) << 8 | y];
            if (block != null && type.isOpaque().test(LegacyBlocks.state(block, 0))) return y + 1;
        }
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        ChunkProviderCelestial.BlockMetaBuffer buffer = buffer(x >> 4, z >> 4);
        BlockState[] states = new BlockState[256];
        for (int y = 0; y < 256; y++) {
            int i = (x & 15) << 12 | (z & 15) << 8 | y;
            states[y] = LegacyBlocks.state(buffer.blocks[i], buffer.metas[i]);
        }
        return new NoiseColumn(0, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("NTM:Space planet " + planet.name);
    }
}
