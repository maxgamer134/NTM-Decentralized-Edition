// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.blocks.generic.BlockFallingBase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * NTM:Space's tinted terrain (BlockTint, BlockFallingTint). Each spot picks one of several
 * textures, through weighted blockstate variants, and SHADE darkens it as the metadata did:
 * world generation shades regolith deeper down.
 */
public class BlockSpaceTint extends Block {

    public static final IntegerProperty SHADE = IntegerProperty.create("shade", 0, 15);

    public BlockSpaceTint(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(SHADE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHADE);
    }

    /** NTM:Space's colorMultiplier: grey, from white at shade 0 to black at 15. */
    public static int color(BlockState state) {
        int v = Math.round(255 * (1F - state.getValue(SHADE) / 15F));
        return 0xFF000000 | v << 16 | v << 8 | v;
    }

    /** The falling kind (laythe silt). */
    public static class Falling extends BlockFallingBase {

        public Falling(Properties props) {
            super(props);
            registerDefaultState(defaultBlockState().setValue(SHADE, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(SHADE);
        }
    }
}
