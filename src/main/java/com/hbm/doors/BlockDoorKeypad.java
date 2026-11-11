// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from susy-ntmdoors (Kyron04): https://github.com/Kyron04/susy-ntmdoors

package com.hbm.doors;

import com.hbm.interfaces.IToolable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A code pad for the door it touches. The first code entered binds the door; after that the code
 * opens and closes it, and R closes it. A screwdriver arms a reset that the current code confirms.
 */
public class BlockDoorKeypad extends BlockDoorDevice implements IToolable {

    // susy-ntmdoors' plate: the model spans 0.75 x 1 x 0.125, scaled 0.35 x 0.4125 x 0.25
    public BlockDoorKeypad(Properties props) {
        super(props, -0.13125, 0.29375, -0.5, 0.13125, 0.70625, -0.454375);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityDoorKeypad(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) com.hbm.doors.client.KeypadScreen.open(pos);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, @Nullable Direction side, Vec3 hit, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BlockEntityDoorKeypad keypad) keypad.armReset(player);
        return true;
    }
}
