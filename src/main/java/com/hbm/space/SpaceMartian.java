// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.blocks.machine.BlockSpotlight;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.registration.RegistryHandle;
import com.hbm.registration.Reg;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Martian Mode: the "Martian" world preset (NTM:Space's WorldTypeTeleport "martian"). Players start
 * on Duna instead of the overworld, at the base Duna's chunk 0,0 builds as it decorates, and a death
 * on Duna brings them back to Duna: to their bed there, or else to Duna's world spawn point.
 *
 * <p>The preset is the normal one with its overworld on the {@code hbm:martian} noise settings (a copy
 * of the overworld's), which is how the server tells it apart.
 */
public final class SpaceMartian {

    private SpaceMartian() {}

    public static final ResourceKey<NoiseGeneratorSettings> SETTINGS =
            ResourceKey.create(Registries.NOISE_SETTINGS, Library.id("martian"));
    private static final ResourceLocation BASE = Library.id("space/martian_base");
    private static final ResourceKey<StructureProcessorList> PROCESSORS =
            ResourceKey.create(Registries.PROCESSOR_LIST, Library.id("space_structure"));
    /** Upstream's HbmPlayerProps.hasWarped, kept across deaths. */
    private static final String PERSISTED = "PlayerPersisted";
    private static final String WARPED = "hbm_martian_warped";

    /** The aluminium ladder the base's shafts are built from (Space-only, like ladder_titanium). */
    public static final RegistryHandle<LadderBlock> LADDER_ALUMINIUM = Reg.blockItem("ladder_aluminium", LadderBlock::new,
            () -> BlockBehaviour.Properties.of().strength(0.25F, 1.2F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.DESTROY));

    /** The tungsten ladder from NTM:Space's structures (Space-only). */
    public static final RegistryHandle<LadderBlock> LADDER_TUNGSTEN = Reg.blockItem("ladder_tungsten", LadderBlock::new,
            () -> BlockBehaviour.Properties.of().strength(0.25F, 1.2F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.DESTROY));

    /** Players whose last death was on Duna, between the death and the respawn. */
    private static final Set<UUID> DIED_ON_DUNA = ConcurrentHashMap.newKeySet();

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, Library.id("ladder_titanium"), () -> new ItemStack(LADDER_ALUMINIUM.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, Library.id("ladder_aluminium"), () -> new ItemStack(LADDER_TUNGSTEN.get()));
        // WorldTypeTeleport.onPlayerJoin, once per player
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player) || !isMartian(player.server.overworld())) return;
            CompoundTag persisted = player.getPersistentData().getCompound(PERSISTED);
            if (persisted.getBoolean(WARPED)) return;
            warp(player);
            persisted.putBoolean(WARPED, true);
            player.getPersistentData().put(PERSISTED, persisted);
        });
        // upstream WorldProviderDuna.getRespawnDimension: on a martian world a death on Duna respawns on Duna
        NeoForge.EVENT_BUS.addListener((PlayerEvent.Clone event) -> {
            if (event.isWasDeath() && event.getOriginal().level().dimension() == SpaceDimensions.DUNA)
                DIED_ON_DUNA.add(event.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            if (!DIED_ON_DUNA.remove(player.getUUID()) || !isMartian(player.server.overworld())) return;
            if (player.level().dimension() != SpaceDimensions.DUNA) respawnOnDuna(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> DIED_ON_DUNA.remove(event.getEntity().getUUID()));
    }

    public static boolean isMartian(ServerLevel overworld) {
        return overworld.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator noise
                && noise.generatorSettings().is(SETTINGS);
    }

    /** CelestialTeleporter.teleport(player, duna, 0, 0, 0, grounded): drop in 5 above the top block at 0,0. */
    private static void warp(ServerPlayer player) {
        ServerLevel duna = player.server.getLevel(SpaceDimensions.DUNA);
        if (duna == null) return;
        int top = duna.getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0) - 1;
        player.teleportTo(duna, 0.5, top + 5, 0.5, player.getYRot(), player.getXRot());
    }

    /**
     * No bed on Duna: 1.7's respawnPlayer puts the player at the dimension's spawn point (Duna shares the
     * overworld's world info, so its x, y and z) and moves them up until they no longer collide.
     */
    private static void respawnOnDuna(ServerPlayer player) {
        ServerLevel duna = player.server.getLevel(SpaceDimensions.DUNA);
        if (duna == null) return;
        BlockPos spawn = duna.getSharedSpawnPos();
        duna.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
        double x = spawn.getX() + 0.5, z = spawn.getZ() + 0.5;
        int y = Math.max(spawn.getY(), duna.getMinBuildHeight());
        while (y < duna.getMaxBuildHeight() && !duna.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(x, y, z))) y++;
        player.teleportTo(duna, x, y, z, player.getYRot(), player.getXRot());
    }

    /**
     * WorldGeneratorDuna's martian branch, run as Duna decorates chunk 0,0: the base, centred on 0,0 and
     * sunk one block into the ground. 1.7 wrote it into the neighbouring chunks straight away; here the
     * part that lies in the chunks this decoration may write goes in now, and the strips beyond them
     * follow on the server thread once those chunks have generated.
     */
    public static void buildBase(WorldGenLevel level) {
        MinecraftServer server = level.getServer();
        if (server == null || !isMartian(server.overworld())) return;
        Optional<StructureTemplate> template = server.getStructureManager().get(BASE);
        if (template.isEmpty()) return;
        Vec3i size = template.get().getSize();
        int x0 = -size.getX() / 2, z0 = -size.getZ() / 2;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0) - 1;
        BlockPos origin = new BlockPos(x0, y, z0);
        BoundingBox all = new BoundingBox(x0, y, z0, x0 + size.getX() - 1, y + size.getY() - 1, z0 + size.getZ() - 1);
        int lo = -16, hi = 31; // chunks -1..1 around chunk 0,0
        place(level, template.get(), origin, clip(all, lo, hi, lo, hi));
        if (all.minX() >= lo && all.minZ() >= lo && all.maxX() <= hi && all.maxZ() <= hi) return;
        ServerLevel duna = level.getLevel();
        server.execute(() -> {
            BoundingBox[] rest = {
                clip(all, all.minX(), lo - 1, all.minZ(), all.maxZ()), clip(all, hi + 1, all.maxX(), all.minZ(), all.maxZ()),
                clip(all, lo, hi, all.minZ(), lo - 1), clip(all, lo, hi, hi + 1, all.maxZ())};
            for (BoundingBox box : rest) {
                if (box == null) continue;
                for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
                    for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) duna.getChunk(cx, cz);
                place(duna, template.get(), origin, box);
            }
        });
    }

    private static BoundingBox clip(BoundingBox box, int minX, int maxX, int minZ, int maxZ) {
        int ax = Math.max(box.minX(), minX), bx = Math.min(box.maxX(), maxX);
        int az = Math.max(box.minZ(), minZ), bz = Math.min(box.maxZ(), maxZ);
        return ax > bx || az > bz ? null : new BoundingBox(ax, box.minY(), az, bx, box.maxY(), bz);
    }

    private static void place(ServerLevelAccessor level, StructureTemplate template, BlockPos origin, BoundingBox box) {
        if (box == null) return;
        StructurePlaceSettings settings = new StructurePlaceSettings().setBoundingBox(box);
        level.registryAccess().lookupOrThrow(Registries.PROCESSOR_LIST).get(PROCESSORS).ifPresent(list -> {
            for (StructureProcessor processor : list.value().list()) settings.addProcessor(processor);
        });
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), 2);
        // upstream builds this one with Spotlight.disableOnGeneration off: its lamps work, not broken
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BlockSpotlight && state.getValue(BlockSpotlight.BROKEN)) {
                BlockPos at = pos.immutable();
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                level.setBlock(at, state.setValue(BlockSpotlight.BROKEN, false).setValue(BlockSpotlight.LIT, false), Block.UPDATE_CLIENTS);
            }
        }
    }
}
