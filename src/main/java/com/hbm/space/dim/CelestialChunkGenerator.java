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

    /** A chunk's blocks as this planet generates them, in the 1.7 buffer. */
    public ChunkProviderCelestial.BlockMetaBuffer buffer(int x, int z) {
        ChunkProviderCelestial provider = provider();
        synchronized (provider) {
            ChunkProviderCelestial.BlockMetaBuffer buffer = provider.getChunkPrimer(x, z);
            provider.modernize(x, z, buffer);
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

    /** The rock under y 0 here, for old chunks' floors. */
    public BlockState deepStone() {
        ChunkProviderCelestial provider = provider();
        Block deep = DeepStone.of(provider.stone());
        return (deep != null ? deep : provider.stone()).defaultBlockState();
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("NTM:Space planet " + planet.name);
    }
}
