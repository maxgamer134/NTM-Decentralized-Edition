// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.atmosphere;

import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A sealed volume a provider fills, as a graph of its air blocks (after Advanced Rocketry's).
 * NTM:Space flood-filled on a thread pool while reading the world, which crashed servers (#340);
 * this fills on the server thread, and {@link SpaceAtmosphere} batches the block changes that
 * trigger it to once a tick, which is what made air vents lag (#163).
 */
public final class AtmosphereBlob {

    private final AdjacencyGraph<BlockPos> graph = new AdjacencyGraph<>();
    final IAtmosphereProvider handler;
    private final LinkedHashMap<BlockPos, Integer> plants = new LinkedHashMap<>();
    /** Whether failing to seal blows the volume out through the opening. */
    boolean runDepress;
    Direction depressDir = Direction.UP;

    AtmosphereBlob(IAtmosphereProvider handler) {
        this.handler = handler;
    }

    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> NEVER_SEALS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, com.hbm.lib.Library.id("never_seals"));

    public static boolean isBlockSealed(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return false;
        // unloaded chunks count as walls, so a volume never loads the world it searches
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return true;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        var block = state.getBlock();
        if (block instanceof FarmBlock || block instanceof FenceBlock) return false;
        // the machines NTM:Space marked IBlockSealable with isSealed false: full blocks with gaps
        if (state.is(NEVER_SEALS)) return false;
        if (block instanceof IBlockSealable sealable) return sealable.isSealed(level, pos);
        // a door's cells seal by the door's rules
        if (block instanceof BlockMultiblockCell) {
            BlockPos core = com.hbm.blocks.multiblock.MultiblockSurface.coreOfAny(level, pos, state);
            if (core != null && level.getBlockState(core).getBlock() instanceof IBlockSealable sealable)
                return sealable.isSealed(level, pos);
        }
        // machines can't seal, almost all have gaps
        if (block instanceof BlockMultiblockCore || block instanceof BlockMultiblockCell) return false;
        // liquids need to know which volume they sit in, to know whether they boil off
        if (state.liquid() || !state.isSolid()) return false;
        if (state.is(BlockTags.LEAVES)) return false;
        return state.isCollisionShapeFullBlock(level, pos);
    }

    public IAtmosphereProvider handler() {
        return handler;
    }

    public BlockPos getRootPosition() {
        return handler.getRootPosition();
    }

    public void consume(int amount) {
        handler.consume(amount);
    }

    public void produce(int amount) {
        handler.produce(amount);
    }

    public boolean contains(BlockPos pos) {
        return graph.contains(pos);
    }

    public Set<BlockPos> getLocations() {
        return graph.getKeys();
    }

    public int getBlobSize() {
        return graph.size();
    }

    private boolean touches(BlockPos pos) {
        if (graph.size() == 0) return true;
        for (Direction d : Direction.values()) if (contains(pos.relative(d))) return true;
        return false;
    }

    /** Grows the volume from a block next to it, through every open block it reaches. */
    void addBlock(BlockPos start) {
        if (contains(start) || !touches(start)) return;
        ServerLevel level = handler.getAtmosphereLevel();
        BlockPos root = getRootPosition();
        long max = (long) handler.getMaxBlobRadius() * handler.getMaxBlobRadius();
        ArrayDeque<BlockPos> stack = new ArrayDeque<>();
        HashSet<BlockPos> found = new HashSet<>();
        stack.push(start);
        found.add(start);
        while (!stack.isEmpty()) {
            BlockPos at = stack.pop();
            for (Direction d : Direction.values()) {
                BlockPos next = at.relative(d);
                if (graph.contains(next) || found.contains(next) || isBlockSealed(level, next)) continue;
                if (next.distSqr(root) > max) {
                    // BANG HISSS: failed to seal, void
                    if (runDepress) decompress(start, depressDir);
                    clearBlob();
                    return;
                }
                stack.push(next);
                found.add(next);
            }
        }
        for (BlockPos pos : found) addSingleBlock(level, pos);
        handler.onBlobCreated(this);
    }

    private void addSingleBlock(ServerLevel level, BlockPos pos) {
        if (graph.contains(pos)) return;
        HashSet<BlockPos> adjacent = new HashSet<>();
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (graph.contains(n)) adjacent.add(n);
        }
        graph.add(pos, adjacent);
        if (level.getBlockState(pos).getBlock() instanceof BonemealableBlock) addPlant(level, pos);
    }

    void removeBlock(BlockPos pos) {
        graph.remove(pos);
        BlockPos root = getRootPosition();
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (graph.contains(n) && !n.equals(root) && (!graph.contains(root) || !graph.doesPathExist(n, root)))
                runEffectOnWorldBlocks(graph.removeAllNodesConnectedTo(n));
        }
    }

    void clearBlob() {
        Collection<BlockPos> all = new HashSet<>(getLocations());
        graph.clear();
        runEffectOnWorldBlocks(all);
    }

    /**
     * What losing this volume does to the blocks it held. NTM:Space judged them all by the
     * atmosphere at the root, so plants on the far side of a closing door died though another
     * volume still held them (#429); each block is judged where it stands.
     */
    private void runEffectOnWorldBlocks(Collection<BlockPos> blocks) {
        ServerLevel level = handler.getAtmosphereLevel();
        for (BlockPos pos : blocks) {
            var atmosphere = SpaceAtmosphere.getAtmosphere(level, pos, this);
            SpaceAtmosphere.runEffectsOnBlock(atmosphere, level, pos);
        }
    }

    private void decompress(BlockPos pos, Direction dir) {
        ServerLevel level = handler.getAtmosphereLevel();
        Vec3 hole = Vec3.atCenterOf(pos);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0F, 1.6F);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.25F);
        // NTM:Space's EntityDepress: for 20 ticks a white jet blows out through the breach and
        // everything within 8 blocks is pulled toward it
        SpaceDepress.start(level, hole, dir.getOpposite(), 20);
    }

    void checkGrowth() {
        ServerLevel level = handler.getAtmosphereLevel();
        Iterator<Map.Entry<BlockPos, Integer>> it = plants.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            BlockState state = level.getBlockState(entry.getKey());
            if (!(state.getBlock() instanceof BonemealableBlock)) {
                it.remove();
                continue;
            }
            int age = age(state);
            if (age != entry.getValue()) {
                produce(Math.max(age - entry.getValue(), 0) * SpaceAtmosphere.CROP_GROWTH_CONVERSION);
                entry.setValue(age);
            }
        }
    }

    void addPlant(ServerLevel level, BlockPos pos) {
        plants.put(pos.immutable(), age(level.getBlockState(pos)));
    }

    /** A plant's growth stage, 1.7's crop metadata. */
    private static int age(BlockState state) {
        for (Property<?> p : state.getProperties())
            if (p instanceof IntegerProperty ip && p.getName().equals("age")) return state.getValue(ip);
        return 0;
    }
}
