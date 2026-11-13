// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space;

import com.hbm.NuclearTech;
import com.hbm.blocks.ModBlocks;
import com.hbm.platform.NeoForgeAttachments;
import com.hbm.platform.NeoForgeServerEvents;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jspecify.annotations.Nullable;

/**
 * Brings chunks saved by earlier versions up to date once, when they first load: base ores on
 * another body take on its stone, and Laythe coral turns to the side it grows from. Each chunk
 * keeps the version it was brought to, so nothing a player placed afterwards is touched.
 */
public final class SpaceChunkUpgrade {

    /** 1: planet ores and coral attachment. 2: the planets' new depth, from y -64. */
    private static final int VERSION = 2;

    public static final Supplier<AttachmentType<Integer>> DATA =
            NeoForgeAttachments.register(
                    ResourceLocation.fromNamespaceAndPath(NuclearTech.MOD_ID, "space_chunk_version"),
                    () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    private SpaceChunkUpgrade() {}

    public static void init() {
        NeoForgeServerEvents.onChunkLoad(SpaceChunkUpgrade::onChunkLoad);
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

    private static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        int version = chunk.getData(DATA);
        if (version >= VERSION) return;
        if (version < 1) {
            BlockSpaceOre.OreBody body = body(level);
            if (body != null) upgrade(level, chunk, body);
        }
        if (version < 2) deepen(level, chunk);
        chunk.setData(DATA, VERSION);
        chunk.setUnsaved(true);
    }

    /**
     * A planet chunk saved while the planets began at y 0: the rock under it, which the planet
     * now generates down to -64, is filled in as a new chunk's would be, its old bedrock floor
     * turned to rock (bedrock ores stay where they are) and its biomes carried down.
     */
    private static void deepen(ServerLevel level, LevelChunk chunk) {
        if (!(level.getChunkSource().getGenerator() instanceof com.hbm.space.dim.CelestialChunkGenerator generator)) return;
        int min = com.hbm.space.dim.legacy.ChunkProviderCelestial.MIN_Y;
        if (chunk.getMinBuildHeight() != min) return;
        LevelChunkSection[] sections = chunk.getSections();
        int below = chunk.getSectionIndex(0);
        for (int i = 0; i < below; i++) if (!sections[i].hasOnlyAir()) return;
        LevelChunkSection floor = sections[below];
        var buffer = generator.buffer(chunk.getPos().x, chunk.getPos().z);
        java.util.List<BlockPos> lights = new java.util.ArrayList<>();
        for (int y = min; y < 5; y++) {
            LevelChunkSection section = sections[chunk.getSectionIndex(y)];
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++) {
                    if (y >= 0 && !section.getBlockState(x, y & 15, z).is(net.minecraft.world.level.block.Blocks.BEDROCK)) continue;
                    int i = com.hbm.space.dim.legacy.ChunkProviderCelestial.idx(x, z, y);
                    var block = buffer.blocks[i];
                    BlockState state = block == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                            : com.hbm.space.dim.legacy.LegacyBlocks.state(block, buffer.metas[i]);
                    if (y >= 0 && state.isAir()) state = generator.deepStone();
                    section.setBlockState(x, y & 15, z, state, false);
                    if (state.getLightEmission() > 0)
                        lights.add(new BlockPos(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z));
                }
        }
        for (int i = 0; i < below; i++)
            sections[i].fillBiomesFromNoise((qx, qy, qz, sampler) -> floor.getNoiseBiome(qx & 3, 0, qz & 3), null, 0, 0, 0);
        net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(chunk, java.util.EnumSet.of(
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR));
        var light = level.getChunkSource().getLightEngine();
        for (int i = 0; i < below; i++) light.updateSectionStatus(net.minecraft.core.SectionPos.of(chunk.getPos(), chunk.getSectionYFromSectionIndex(i)), false);
        for (BlockPos pos : lights) light.checkBlock(pos);
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
