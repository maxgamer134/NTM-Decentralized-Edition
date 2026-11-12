// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.player.Player;

/**
 * An NTM:Space ore. One block serves every body: the body picks the stone the ore sits in, as
 * NTM:Space's metadata did. Orbit, the default, is plain stone.
 */
public class BlockSpaceOre extends Block {

    public enum OreBody implements StringRepresentable {
        ORBIT, KERBIN, MUN, MINMUS, DUNA, MOHO, DRES, EVE, IKE, LAYTHE, TEKTO;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static OreBody of(SolarSystem.Body body) {
            return values()[body.ordinal()];
        }
    }

    public static final EnumProperty<OreBody> BODY = EnumProperty.create("body", OreBody.class);

    public BlockSpaceOre(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(BODY, OreBody.ORBIT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BODY);
    }

    public ItemStack stackFor(OreBody body) {
        ItemStack stack = new ItemStack(this);
        if (body != OreBody.ORBIT) stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(BODY, body));
        return stack;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return stackFor(state.getValue(BODY));
    }
}
