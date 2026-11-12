// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.explosion;

import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.ExplosionBalefire;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.GasFlamePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * NTM:Space's antimatter bomb: the balefire crater without the balefire. It leaves no fire and no
 * euphemium, flashes snow and ice off as flame, and pools volcanic lava on the crater floor.
 *
 * <p>NTM:Space's version of this column loop had lost the line that clears each block, so neither
 * this nor the balefire and hydrogen bombs sharing the loop left any crater (its issue 469); and
 * on snow or ice it skipped to the next pass without moving down, hanging the server.
 */
public class ExplosionAntimatter extends ExplosionBalefire {

    public ExplosionAntimatter(int x, int y, int z, Level level, int rad) {
        super(x, y, z, level, rad);
    }

    @Override
    protected void breakColumn(int x, int z) {
        int dist = (int) (radius - Math.sqrt(x * x + z * z));
        if (dist <= 0) return;

        int pX = posX + x;
        int pZ = posZ + z;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, pX, pZ);
        int maxdepth = (int) (10 + radius * 0.25);
        int depth = (int) ((maxdepth * dist / (double) radius) + (Math.sin(dist * 0.15 + 2) * 2));
        depth = Math.max(y - depth, level.getMinBuildHeight());

        while (y > depth) {
            BlockPos pos = new BlockPos(pX, y, pZ);
            BlockState state = level.getBlockState(pos);
            if (isFrozen(state)) flash(pX, y, pZ, x, z);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            y--;
        }

        for (int i = depth; i > depth - 5; i--) {
            BlockPos pos = new BlockPos(pX, i, pZ);
            if (level.getRandom().nextInt(dist) == 0)
                level.setBlock(new BlockPos(pX, depth, pZ),
                        ModBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            if (slakes(level.getBlockState(pos)))
                level.setBlock(pos, ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static boolean isFrozen(BlockState state) {
        return state.is(BlockTags.SNOW) || state.is(BlockTags.ICE) || state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW);
    }

    /**
     * A flame drifting away from the blast. NTM:Space gave the flame the column's offset and
     * height as its speed, flinging it hundreds of blocks a tick, and sent one packet per block;
     * this sends one in eight.
     */
    private void flash(int pX, int y, int pZ, int dx, int dz) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server) || level.getRandom().nextInt(8) != 0) return;
        double length = Math.max(1, Math.sqrt(dx * dx + dz * dz));
        double mX = dx / length * 0.5;
        double mZ = dz / length * 0.5;
        PacketWire.sendToAllAround(
                new GasFlamePayload(pX + 0.5, y + 0.5, pZ + 0.5, mX, 0.2, mZ, GasFlamePayload.DEFAULT_SCALE),
                new TargetPoint(server, pX, y, pZ, 150));
    }
}
