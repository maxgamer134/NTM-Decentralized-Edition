// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim;

import com.hbm.NuclearTech;
import com.hbm.blocks.ModBlocks;
import com.hbm.space.dim.legacy.ChunkProviderCelestial;
import com.hbm.space.dim.legacy.LegacyBlocks;
import com.hbm.space.dim.legacy.World;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.TickPriority;
import org.jspecify.annotations.Nullable;

/**
 * The rock an old planet chunk lacks under y 0 and the ores in it, made as a new chunk's are:
 * the planet's terrain for the column, then the decoration of the chunk and of the neighbours
 * whose decoration reaches into it, with the same seeds. It all goes into a copy of the chunk's
 * deep, so it can run off the server thread and touches nothing until the result is put in.
 */
public final class PlanetDepthUpgrade {

    /** The deep a result covers: the new floor up to the top of 1.7's bedrock, y 4. */
    public static final int BOTTOM = ChunkProviderCelestial.MIN_Y, TOP = 5;
    /** The chunk as loaded is known up to here, for the decoration to read around the deep. */
    public static final int KNOWN = 16;

    private PlanetDepthUpgrade() {}

    /** A cell of a result, x then z then y from the floor. */
    public static int index(int x, int y, int z) {
        return ((y - BOTTOM) << 8) | (z << 4) | x;
    }

    public static int cells() {
        return 256 * (TOP - BOTTOM);
    }

    /**
     * The deep before and after decoration, the block entities decoration made in it (bedrock
     * ores) and the liquids it set flowing.
     */
    public record Result(BlockState[] before, BlockState[] after, Map<BlockPos, BlockEntity> blockEntities, LongSet fluids) {}

    /** What can stay in the old floor's place: solid, and not a block that falls. */
    public static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.isSolid() && !(state.getBlock() instanceof FallingBlock);
    }

    /**
     * The deep of chunk {@code pos} as the planet makes it today. {@code loaded} is the chunk's
     * y 0 to 15 as it was saved ({@code y << 8 | z << 4 | x}): where it was bedrock (1.7's floor,
     * y 0 to 4) the planet's rock goes, kept solid, and the rest of it is left as it is.
     */
    public static Result compute(ServerLevel level, CelestialChunkGenerator generator, ChunkPos pos, BlockState[] loaded) {
        Planet planet = ((CelestialBiomeSource) generator.getBiomeSource()).planet;
        long seed = Planets.seed();
        ChunkProviderCelestial provider = planet.provider(seed);
        BlockState stone = provider.stone().defaultBlockState();
        ChunkProviderCelestial.BlockMetaBuffer buffer = generator.buffer(pos.x, pos.z);
        Region region = new Region(level, pos, stone);
        boolean oldBedrockOre = false;
        boolean[] floor = new boolean[256 * TOP];
        for (int y = BOTTOM; y < KNOWN; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++) {
                    BlockState state;
                    if (y < 0) {
                        state = terrain(buffer, x, y, z);
                    } else {
                        BlockState old = loaded[(y << 8) | (z << 4) | x];
                        if (old.is(ModBlocks.ORE_BEDROCK.get())) oldBedrockOre = true;
                        if (y < TOP && old.is(Blocks.BEDROCK)) {
                            floor[(y << 8) | (z << 4) | x] = true;
                            state = terrain(buffer, x, y, z);
                            if (!solid(state)) state = stone;
                        } else {
                            state = old;
                        }
                    }
                    region.init(x, y, z, state);
                }
        BlockState[] before = region.deep();

        // decoration starts 8 blocks into the next chunks, so this chunk's comes from these four
        for (int dx = -1; dx <= 0; dx++)
            for (int dz = -1; dz <= 0; dz++) decorate(planet, seed, provider, region, pos.x + dx, pos.z + dz);

        // a chunk that kept its old bedrock ore gets no second one under it
        if (oldBedrockOre) region.revertBedrockOres(before);
        // and the old floor stays solid, so nothing above it falls or floods into the deep
        for (int y = 0; y < TOP; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++)
                    if (floor[(y << 8) | (z << 4) | x] && !solid(region.get(x, y, z))) region.init(x, y, z, stone);
        return new Result(before, region.deep(), region.blockEntities(), region.fluids);
    }

    private static BlockState terrain(ChunkProviderCelestial.BlockMetaBuffer buffer, int x, int y, int z) {
        int i = ChunkProviderCelestial.idx(x, z, y);
        Block block = buffer.blocks[i];
        return block == null ? Blocks.AIR.defaultBlockState() : LegacyBlocks.state(block, buffer.metas[i]);
    }

    /** CelestialChunkGenerator.applyBiomeDecoration's planet half, for one chunk, into the region. */
    private static void decorate(Planet planet, long seed, ChunkProviderCelestial provider, Region region, int cx, int cz) {
        World world = planet.world(seed).in(region);
        try {
            if (planet.decorators.contains(SpacePlanets.GLOBAL_STRATA)) SpacePlanets.GLOBAL_STRATA.generate(null, cx, cz, world);
        } catch (RuntimeException e) {
            NuclearTech.LOGGER.debug("NTM:Space could not redo the strata of {}, {} on {}", cx, cz, planet.name, e);
        }
        try {
            synchronized (provider) {
                provider.populate(world, cx, cz);
            }
        } catch (RuntimeException e) {
            NuclearTech.LOGGER.debug("NTM:Space could not redo the biome decoration of {}, {} on {}", cx, cz, planet.name, e);
        }
        Random random = new Random(seed);
        long a = random.nextLong(), b = random.nextLong();
        for (Planet.Decorator decorator : planet.decorators) {
            if (decorator == SpacePlanets.GLOBAL_STRATA) continue;
            random.setSeed((long) cx * a ^ (long) cz * b ^ seed);
            try {
                decorator.generate(random, cx, cz, world);
            } catch (RuntimeException e) {
                NuclearTech.LOGGER.debug("NTM:Space could not redo a decorator of {}, {} on {}", cx, cz, planet.name, e);
            }
        }
    }

    /**
     * One chunk's deep as a world: it reads the copy of the chunk (and the planet's rock around
     * it, air above what is known) and writes only into the deep of the chunk. Everything that
     * would reach the live world (entities, sounds, ticks) is dropped, liquids set flowing kept.
     */
    static final class Region implements WorldGenLevel {

        private final ServerLevel level;
        private final ChunkPos pos;
        private final BlockState stone;
        private final ProtoChunk chunk;
        private @Nullable ProtoChunk outside;
        private final RandomSource random;
        private final BiomeManager biomes;
        final LongSet fluids = new LongOpenHashSet();
        private long subTicks;

        Region(ServerLevel level, ChunkPos pos, BlockState stone) {
            this.level = level;
            this.pos = pos;
            this.stone = stone;
            this.chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
            this.random = RandomSource.create(level.getSeed() ^ pos.toLong());
            this.biomes = new BiomeManager(this, BiomeManager.obfuscateSeed(level.getSeed()));
        }

        void init(int x, int y, int z, BlockState state) {
            chunk.getSection(chunk.getSectionIndex(y)).setBlockState(x, y & 15, z, state, false);
        }

        BlockState get(int x, int y, int z) {
            return chunk.getSection(chunk.getSectionIndex(y)).getBlockState(x, y & 15, z);
        }

        BlockState[] deep() {
            BlockState[] cells = new BlockState[cells()];
            for (int y = BOTTOM; y < TOP; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) cells[index(x, y, z)] = get(x, y, z);
            return cells;
        }

        Map<BlockPos, BlockEntity> blockEntities() {
            return new HashMap<>(chunk.getBlockEntities());
        }

        /** New bedrock ores on the floor back to bedrock, and the depth rock capping them back to what it was. */
        void revertBedrockOres(BlockState[] before) {
            BlockState ore = ModBlocks.ORE_BEDROCK.get().defaultBlockState();
            Block depth = LegacyBlocks.b("stone_depth");
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++) {
                    if (!get(x, BOTTOM, z).is(ore.getBlock()) || before[index(x, BOTTOM, z)].is(ore.getBlock())) continue;
                    init(x, BOTTOM, z, before[index(x, BOTTOM, z)]);
                    chunk.removeBlockEntity(new BlockPos(pos.getMinBlockX() + x, BOTTOM, pos.getMinBlockZ() + z));
                    for (int ix = Math.max(0, x - 3); ix <= Math.min(15, x + 3); ix++)
                        for (int iz = Math.max(0, z - 3); iz <= Math.min(15, z + 3); iz++)
                            for (int y = BOTTOM + 1; y < BOTTOM + 7; y++)
                                if (get(ix, y, iz).is(depth) && !before[index(ix, y, iz)].is(depth)) init(ix, y, iz, before[index(ix, y, iz)]);
                }
        }

        private boolean inside(BlockPos at) {
            return (at.getX() >> 4) == pos.x && (at.getZ() >> 4) == pos.z;
        }

        private boolean writable(BlockPos at) {
            return inside(at) && at.getY() >= BOTTOM && at.getY() < TOP;
        }

        @Override
        public BlockState getBlockState(BlockPos at) {
            if (at.getY() < BOTTOM || at.getY() >= level.getMaxBuildHeight()) return Blocks.VOID_AIR.defaultBlockState();
            if (inside(at)) return chunk.getBlockState(at);
            return at.getY() < KNOWN ? stone : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos at) {
            return getBlockState(at).getFluidState();
        }

        @Override
        public boolean setBlock(BlockPos at, BlockState state, int flags, int recursion) {
            if (!writable(at)) return false;
            BlockState old = chunk.getBlockState(at);
            init(at.getX() & 15, at.getY(), at.getZ() & 15, state);
            if (state.hasBlockEntity() && state.getBlock() instanceof EntityBlock block) {
                BlockEntity be = block.newBlockEntity(at.immutable(), state);
                if (be != null) chunk.setBlockEntity(be);
                else chunk.removeBlockEntity(at);
            } else if (old.hasBlockEntity()) {
                chunk.removeBlockEntity(at);
            }
            return true;
        }

        @Override
        public boolean removeBlock(BlockPos at, boolean moved) {
            return setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        }

        @Override
        public boolean destroyBlock(BlockPos at, boolean drop, @Nullable Entity entity, int recursion) {
            return !getBlockState(at).isAir() && setBlock(at, Blocks.AIR.defaultBlockState(), 3, recursion);
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos at) {
            return inside(at) ? chunk.getBlockEntity(at) : null;
        }

        @Override
        public boolean isStateAtPosition(BlockPos at, Predicate<BlockState> test) {
            return test.test(getBlockState(at));
        }

        @Override
        public boolean isFluidAtPosition(BlockPos at, Predicate<FluidState> test) {
            return test.test(getFluidState(at));
        }

        @Override
        public boolean ensureCanWrite(BlockPos at) {
            return true;
        }

        @Override
        public @Nullable ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean require) {
            if (x == pos.x && z == pos.z) return chunk;
            if (outside == null)
                outside = new ProtoChunk(pos, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
            return outside;
        }

        @Override
        public boolean hasChunk(int x, int z) {
            return true;
        }

        /** Over the top of the world, so what a surface decoration puts down is out of reach. */
        @Override
        public int getHeight(Heightmap.Types type, int x, int z) {
            return level.getMaxBuildHeight();
        }

        @Override
        public void scheduleTick(BlockPos at, Fluid fluid, int delay, TickPriority priority) {
            if (writable(at)) fluids.add(at.asLong());
        }

        @Override
        public void scheduleTick(BlockPos at, Fluid fluid, int delay) {
            if (writable(at)) fluids.add(at.asLong());
        }

        @Override
        public LevelTickAccess<Block> getBlockTicks() {
            return BlackholeTickAccess.emptyLevelList();
        }

        @Override
        public LevelTickAccess<Fluid> getFluidTicks() {
            return BlackholeTickAccess.emptyLevelList();
        }

        @Override
        public long nextSubTickCount() {
            return subTicks++;
        }

        @Override
        public Holder<Biome> getNoiseBiome(int x, int y, int z) {
            return getUncachedNoiseBiome(x, y, z);
        }

        @Override
        public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
            return level.getUncachedNoiseBiome(x, y, z);
        }

        @Override
        public BiomeManager getBiomeManager() {
            return biomes;
        }

        @Override
        public @Nullable Player getNearestPlayer(double x, double y, double z, double distance, @Nullable Predicate<Entity> test) {
            return null;
        }

        @Override
        public int getSkyDarken() {
            return 0;
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            return 1.0F;
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return level.getLightEngine();
        }

        @Override
        public boolean addFreshEntity(Entity entity) {
            return false;
        }

        @Override
        public WorldBorder getWorldBorder() {
            return level.getWorldBorder();
        }

        @Override
        public boolean isClientSide() {
            return false;
        }

        @Override
        public ServerLevel getLevel() {
            return level;
        }

        @Override
        public RegistryAccess registryAccess() {
            return level.registryAccess();
        }

        @Override
        public FeatureFlagSet enabledFeatures() {
            return level.enabledFeatures();
        }

        @Override
        public LevelData getLevelData() {
            return level.getLevelData();
        }

        @Override
        public DifficultyInstance getCurrentDifficultyAt(BlockPos at) {
            return new DifficultyInstance(level.getDifficulty(), level.getDayTime(), 0L, level.getMoonBrightness());
        }

        /** None: what asks for the server (the martian base) builds into the live world, not the deep. */
        @Override
        public @Nullable MinecraftServer getServer() {
            return null;
        }

        @Override
        public ChunkSource getChunkSource() {
            return level.getChunkSource();
        }

        @Override
        public long getSeed() {
            return level.getSeed();
        }

        @Override
        public int getSeaLevel() {
            return level.getSeaLevel();
        }

        @Override
        public RandomSource getRandom() {
            return random;
        }

        @Override
        public void playSound(@Nullable Player player, BlockPos at, SoundEvent sound, SoundSource source, float volume, float pitch) {}

        @Override
        public void addParticle(ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) {}

        @Override
        public void levelEvent(@Nullable Player player, int type, BlockPos at, int data) {}

        @Override
        public void gameEvent(Holder<GameEvent> event, Vec3 at, GameEvent.Context context) {}

        @Override
        public DimensionType dimensionType() {
            return level.dimensionType();
        }

        @Override
        public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> type, AABB box, Predicate<? super T> test) {
            return Collections.emptyList();
        }

        @Override
        public List<Entity> getEntities(@Nullable Entity except, AABB box, @Nullable Predicate<? super Entity> test) {
            return Collections.emptyList();
        }

        @Override
        public List<Player> players() {
            return Collections.emptyList();
        }

        @Override
        public int getMinBuildHeight() {
            return level.getMinBuildHeight();
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }
    }
}
