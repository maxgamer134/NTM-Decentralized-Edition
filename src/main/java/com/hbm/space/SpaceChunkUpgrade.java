// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space;

import com.hbm.NuclearTech;
import com.hbm.blocks.ModBlocks;
import com.hbm.platform.NeoForgeAttachments;
import com.hbm.platform.NeoForgeServerEvents;
import com.hbm.space.dim.CelestialChunkGenerator;
import com.hbm.space.dim.PlanetDepthUpgrade;
import com.hbm.space.dim.legacy.ChunkProviderCelestial;
import com.hbm.util.ChunkUtil;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.EnumSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jspecify.annotations.Nullable;

/**
 * Brings chunks saved by earlier versions up to date once, when they first load: base ores on
 * another body take on its stone, Laythe coral turns to the side it grows from, glowstone ore
 * lights up, and a planet chunk saved while the planets began at y 0 gets the rock, ores and
 * bedrock ores a new chunk has under it. Each chunk keeps the version it was brought to, so
 * nothing a player placed afterwards is touched.
 *
 * <p>The deep is made off the server thread and put in a few chunks a tick. A chunk's version
 * only moves on once its deep is in, in the same tick, so a chunk that unloads or a server that
 * stops before then has lost nothing and does it again on the next load.
 */
public final class SpaceChunkUpgrade {

    /**
     * 1: planet ores and coral attachment. 2: the planets' new depth, from y -64. 3: the ores in
     * that depth and a solid old floor, for chunks 2 filled with bare rock; glowstone ore lit.
     */
    private static final int VERSION = 3;

    public static final Supplier<AttachmentType<Integer>> DATA =
            NeoForgeAttachments.register(
                    ResourceLocation.fromNamespaceAndPath(NuclearTech.MOD_ID, "space_chunk_version"),
                    () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    private SpaceChunkUpgrade() {}

    public static void init() {
        NeoForgeServerEvents.onChunkLoad(SpaceChunkUpgrade::onChunkLoad);
        NeoForgeServerEvents.onChunkUnload((level, chunk) -> {
            Map<Long, Job> waiting = PENDING.get(level.dimension());
            if (waiting != null) waiting.remove(chunk.getPos().toLong());
        });
        NeoForgeServerEvents.onServerTickPost(SpaceChunkUpgrade::tick);
        NeoForgeServerEvents.onServerStopping(server -> {
            PENDING.clear();
            DONE.clear();
        });
    }

    /** A chunk that needs its deep: one filled from y -64 up (2), or one 2 filled with bare rock (3). */
    private enum Mode { DEEPEN, ORES }

    private record Job(ServerLevel level, LevelChunk chunk, CelestialChunkGenerator generator, int version, Mode mode, LongList lights) {}

    private record Done(Job job, PlanetDepthUpgrade.Result result) {}

    /** The chunks waiting on their deep, by level and position. */
    private static final Map<ResourceKey<Level>, Map<Long, Job>> PENDING = new ConcurrentHashMap<>();
    private static final Queue<Done> DONE = new ConcurrentLinkedQueue<>();
    /** How long one tick may spend putting finished chunks in. */
    private static final long BUDGET_NANOS = 4_000_000L;
    private static volatile @Nullable ExecutorService workers;

    private static ExecutorService workers() {
        ExecutorService pool = workers;
        if (pool == null) {
            synchronized (SpaceChunkUpgrade.class) {
                pool = workers;
                if (pool == null) {
                    int threads = Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 4));
                    workers = pool = Executors.newFixedThreadPool(threads, task -> {
                        Thread thread = new Thread(task, "NTM Space chunk upgrade");
                        thread.setDaemon(true);
                        thread.setPriority(Thread.MIN_PRIORITY);
                        return thread;
                    });
                }
            }
        }
        return pool;
    }

    private static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        int version = chunk.getData(DATA);
        if (version >= VERSION) return;
        Map<Long, Job> waiting = PENDING.get(level.dimension());
        Job queued = waiting == null ? null : waiting.get(chunk.getPos().toLong());
        if (queued != null && queued.chunk() == chunk) return;
        if (version < 1) {
            BlockSpaceOre.OreBody body = body(level);
            if (body != null) upgrade(level, chunk, body);
        }
        LongList lights = glowstone(chunk);
        CelestialChunkGenerator generator = level.getChunkSource().getGenerator() instanceof CelestialChunkGenerator g
                && chunk.getMinBuildHeight() == ChunkProviderCelestial.MIN_Y ? g : null;
        Mode mode = generator == null ? null : shallow(chunk) ? Mode.DEEPEN : version == 2 && !decorated(chunk) ? Mode.ORES : null;
        if (mode == null) {
            var light = level.getChunkSource().getLightEngine();
            for (int i = 0; i < lights.size(); i++) light.checkBlock(BlockPos.of(lights.getLong(i)));
            chunk.setData(DATA, VERSION);
            chunk.setUnsaved(true);
            return;
        }
        if (version < 1) {
            chunk.setData(DATA, version = 1);
            chunk.setUnsaved(true);
        }
        submit(new Job(level, chunk, generator, version, mode, lights));
    }

    /** Nothing at all under y 0: a planet chunk from before the planets reached down to -64. */
    private static boolean shallow(LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        int below = chunk.getSectionIndex(0);
        for (int i = 0; i < below; i++) if (!sections[i].hasOnlyAir()) return false;
        return true;
    }

    /**
     * Planet ores under y 0, which bare rock never has: a chunk 2's generator made itself, not
     * one it filled in. Which one it was is not saved, so this is how the two tell apart.
     */
    private static boolean decorated(LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        int below = chunk.getSectionIndex(0);
        for (int i = 0; i < below; i++)
            if (!sections[i].hasOnlyAir() && sections[i].getStates().maybeHas(s -> ModBlocks.isPlanetOre(s.getBlock()))) return true;
        return false;
    }

    /** Glowstone ore, which now gives light: the light there only shows after a light check. */
    private static LongList glowstone(LevelChunk chunk) {
        LongList found = new LongArrayList();
        Block glow = SpaceBlocks.ORE_GLOWSTONE.get();
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.getStates().maybeHas(s -> s.is(glow))) continue;
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++)
                        if (section.getBlockState(x, y, z).is(glow))
                            found.add(BlockPos.asLong(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z));
        }
        return found;
    }

    private static void submit(Job job) {
        long key = job.chunk().getPos().toLong();
        Map<Long, Job> waiting = PENDING.computeIfAbsent(job.level().dimension(), k -> new ConcurrentHashMap<>());
        waiting.put(key, job);
        // the chunk's old floor and what stands on it, read here on the server thread
        LevelChunkSection section = job.chunk().getSection(job.chunk().getSectionIndex(0));
        BlockState[] loaded = new BlockState[16 * 256];
        for (int y = 0; y < 16; y++)
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++) loaded[(y << 8) | (z << 4) | x] = section.getBlockState(x, y, z);
        workers().execute(() -> {
            if (waiting.get(key) != job) return;
            try {
                DONE.add(new Done(job, PlanetDepthUpgrade.compute(job.level(), job.generator(), job.chunk().getPos(), loaded)));
            } catch (RuntimeException e) {
                waiting.remove(key, job);
                NuclearTech.LOGGER.error("NTM:Space could not make the deep of old chunk {} in {}", job.chunk().getPos(), job.level().dimension().location(), e);
            }
        });
    }

    /** Puts finished chunks in, for as long as the tick allows. */
    private static void tick(MinecraftServer server) {
        long start = System.nanoTime();
        Done done;
        while (System.nanoTime() - start < BUDGET_NANOS && (done = DONE.poll()) != null) {
            Job job = done.job();
            Map<Long, Job> waiting = PENDING.get(job.level().dimension());
            if (waiting == null || !waiting.remove(job.chunk().getPos().toLong(), job)) continue;
            if (job.level().getServer() != server) continue;
            LevelChunk live = job.level().getChunkSource().getChunkNow(job.chunk().getPos().x, job.chunk().getPos().z);
            if (live != job.chunk() || live.getData(DATA) != job.version()) continue;
            try {
                apply(job, done.result());
            } catch (RuntimeException e) {
                NuclearTech.LOGGER.error("NTM:Space could not put in the deep of old chunk {} in {}", live.getPos(), job.level().dimension().location(), e);
            }
        }
    }

    /**
     * The deep into the chunk. A chunk filled from y -64 up takes all of it under y 0, and its
     * old bedrock floor (y 0 to 4) turns to the rock and ores there; the rest of the old floor,
     * like an old bedrock ore with a drill working it, stays. A chunk 2 filled with bare rock only
     * takes the ores, strata and bedrock ores, and only where the rock is still as 2 left it.
     */
    private static void apply(Job job, PlanetDepthUpgrade.Result result) {
        ServerLevel level = job.level();
        LevelChunk chunk = job.chunk();
        BlockState[] before = result.before(), after = result.after();
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        int bottom = PlanetDepthUpgrade.BOTTOM, top = PlanetDepthUpgrade.TOP;
        if (job.mode() == Mode.ORES) {
            // a chunk made by 2's own generator already has these ores; one 2 filled has the rock
            int made = 0, bare = 0;
            for (int y = bottom; y < 0; y++) {
                LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        int i = PlanetDepthUpgrade.index(x, y, z);
                        if (after[i] == before[i]) continue;
                        BlockState now = section.getBlockState(x, y & 15, z);
                        if (now == after[i]) made++;
                        else if (now == before[i]) bare++;
                    }
            }
            if (made >= bare) {
                finish(level, chunk, job.lights(), 0L);
                return;
            }
        }
        LongList lights = new LongArrayList(job.lights());
        boolean[] put = new boolean[PlanetDepthUpgrade.cells()];
        long sections = 0L;
        for (int y = bottom; y < top; y++) {
            if (job.mode() == Mode.ORES && y >= 0) break;
            LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
            for (int z = 0; z < 16; z++)
                for (int x = 0; x < 16; x++) {
                    int i = PlanetDepthUpgrade.index(x, y, z);
                    BlockState now = section.getBlockState(x, y & 15, z), state = after[i];
                    boolean take = job.mode() == Mode.DEEPEN
                            ? y < 0 ? now.isAir() : now.is(Blocks.BEDROCK)
                            : state != before[i] && now == before[i] && PlanetDepthUpgrade.solid(state);
                    if (!take || now == state) continue;
                    section.setBlockState(x, y & 15, z, state);
                    put[i] = true;
                    sections |= 1L << chunk.getSectionIndex(y);
                    if (state.getLightEmission() > 0) lights.add(BlockPos.asLong(minX + x, y, minZ + z));
                }
        }
        for (Map.Entry<BlockPos, BlockEntity> entry : result.blockEntities().entrySet()) {
            BlockPos at = entry.getKey();
            if (at.getY() < bottom || at.getY() >= top || !put[PlanetDepthUpgrade.index(at.getX() & 15, at.getY(), at.getZ() & 15)]) continue;
            if (chunk.getBlockState(at) == entry.getValue().getBlockState()) chunk.addAndRegisterBlockEntity(entry.getValue());
        }
        for (long packed : result.fluids()) {
            BlockPos at = BlockPos.of(packed);
            if (!put[PlanetDepthUpgrade.index(at.getX() & 15, at.getY(), at.getZ() & 15)]) continue;
            FluidState fluid = chunk.getFluidState(at);
            if (!fluid.isEmpty()) level.scheduleTick(at, fluid.getType(), 0);
        }
        if (job.mode() == Mode.DEEPEN) {
            // the biomes carried down from the old bottom
            LevelChunkSection floor = chunk.getSection(chunk.getSectionIndex(0));
            for (int i = 0; i < chunk.getSectionIndex(0); i++)
                chunk.getSection(i).fillBiomesFromNoise((qx, qy, qz, sampler) -> floor.getNoiseBiome(qx & 3, 0, qz & 3), null, 0, 0, 0);
            Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.WORLD_SURFACE, Heightmap.Types.MOTION_BLOCKING,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR));
        }
        finish(level, chunk, lights, sections);
    }

    /** The version moves on with the blocks, and players who already have the chunk get it again. */
    private static void finish(ServerLevel level, LevelChunk chunk, LongList lights, long sections) {
        chunk.setData(DATA, VERSION);
        chunk.setUnsaved(true);
        if (sections == 0L && lights.isEmpty()) return;
        CompletableFuture<?> lit = ChunkUtil.updateLight(level, chunk, lights, sections);
        if (sections != 0L) ChunkUtil.broadcastWholesaleResend(level, chunk.getPos().toLong(), lit);
    }

    private static BlockSpaceOre.@Nullable OreBody body(ServerLevel level) {
        for (SolarSystem.Body body : SolarSystem.Body.values()) {
            if (body == SolarSystem.Body.ORBIT) continue;
            try {
                if (body.getDimension().equals(level.dimension())) return BlockSpaceOre.OreBody.of(body);
            } catch (RuntimeException e) {
                // a body without a dimension
            }
        }
        return null;
    }

    private static boolean wanted(BlockState state, BlockSpaceOre.OreBody body) {
        if (state.getBlock() instanceof SpacePlants.Coral)
            return state.getValue(SpacePlants.Coral.ATTACH) == Direction.DOWN;
        return body != BlockSpaceOre.OreBody.KERBIN
                && state.hasProperty(BlockSpaceOre.BODY)
                && state.getValue(BlockSpaceOre.BODY) == BlockSpaceOre.OreBody.ORBIT
                && ModBlocks.isPlanetOre(state.getBlock());
    }

    /**
     * The blocks around a loading chunk, read without loading anything: the chunk itself from its
     * sections and its neighbours only when they are already loaded. Asking the level for the chunk
     * that is loading, or one not loaded yet, would wait on it forever.
     */
    private static net.minecraft.world.level.BlockGetter loaded(ServerLevel level, LevelChunk chunk) {
        return new net.minecraft.world.level.BlockGetter() {
            @Override
            public BlockState getBlockState(BlockPos at) {
                int cx = at.getX() >> 4, cz = at.getZ() >> 4;
                if (cx == chunk.getPos().x && cz == chunk.getPos().z) return chunk.getBlockState(at);
                LevelChunk other = level.getChunkSource().getChunkNow(cx, cz);
                return other == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : other.getBlockState(at);
            }

            @Override
            public net.minecraft.world.level.material.FluidState getFluidState(BlockPos at) {
                return getBlockState(at).getFluidState();
            }

            @Override
            public net.minecraft.world.level.block.entity.@Nullable BlockEntity getBlockEntity(BlockPos at) {
                return null;
            }

            @Override
            public int getHeight() {
                return level.getHeight();
            }

            @Override
            public int getMinBuildHeight() {
                return level.getMinBuildHeight();
            }
        };
    }

    private static void upgrade(ServerLevel level, LevelChunk chunk, BlockSpaceOre.OreBody body) {
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.getStates().maybeHas(s -> wanted(s, body))) continue;
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!wanted(state, body)) continue;
                        BlockState fixed;
                        if (state.getBlock() instanceof SpacePlants.Coral) {
                            pos.set(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                            fixed = SpacePlants.Coral.attach(state, loaded(level, chunk), pos);
                        } else {
                            fixed = state.setValue(BlockSpaceOre.BODY, body);
                        }
                        if (fixed != state) section.setBlockState(x, y, z, fixed, false);
                    }
        }
    }
}
