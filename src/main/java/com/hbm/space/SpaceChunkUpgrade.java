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

    /** 1: planet ores and coral attachment. */
    private static final int VERSION = 1;

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
        if (chunk.getData(DATA) >= VERSION) return;
        BlockSpaceOre.OreBody body = body(level);
        if (body != null) upgrade(level, chunk, body);
        chunk.setData(DATA, VERSION);
        chunk.setUnsaved(true);
    }

    private static boolean wanted(BlockState state, BlockSpaceOre.OreBody body) {
        if (state.getBlock() instanceof SpacePlants.Coral)
            return state.getValue(SpacePlants.Coral.ATTACH) == Direction.DOWN;
        return body != BlockSpaceOre.OreBody.KERBIN
                && state.hasProperty(BlockSpaceOre.BODY)
                && state.getValue(BlockSpaceOre.BODY) == BlockSpaceOre.OreBody.ORBIT
                && ModBlocks.isPlanetOre(state.getBlock());
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
                            fixed = SpacePlants.Coral.attach(state, level, pos);
                        } else {
                            fixed = state.setValue(BlockSpaceOre.BODY, body);
                        }
                        if (fixed != state) section.setBlockState(x, y, z, fixed, false);
                    }
        }
    }
}
