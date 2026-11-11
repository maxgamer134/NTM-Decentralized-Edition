// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from susy-ntmdoors (Kyron04): https://github.com/Kyron04/susy-ntmdoors

package com.hbm.doors;

import com.hbm.backport.BlockEntityCompat;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityDoorGeneric;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The keypad's state. The code lives on the door, so every keypad on a door shares it and the
 * binding outlasts a broken keypad until the door itself goes.
 */
public class BlockEntityDoorKeypad extends BlockEntityCompat implements BlockDoorDevice.DoorDevice {

    private @Nullable BlockPos door;
    private boolean resetArmed;
    private int statusTicks;

    public BlockEntityDoorKeypad(BlockPos pos, BlockState state) {
        super(DoorAccess.KEYPAD_BE.get(), pos, state);
    }

    public void tickServer() {
        if (statusTicks > 0 && --statusTicks == 0) light(0);
    }

    private @Nullable BlockEntityDoorGeneric door() {
        BlockEntityDoorGeneric found = DoorAccess.touchingDoor(level, worldPosition, door);
        BlockPos at = found == null ? null : found.getBlockPos();
        if (at == null ? door != null : !at.equals(door)) {
            door = at;
            setChanged();
        }
        return found;
    }

    private void light(int status) {
        BlockState state = getBlockState();
        if (state.getValue(BlockDoorDevice.STATUS) != status)
            level.setBlock(worldPosition, state.setValue(BlockDoorDevice.STATUS, status), Block.UPDATE_CLIENTS);
    }

    private void answer(@Nullable Player player, boolean ok, String key) {
        light(ok ? 1 : 2);
        statusTicks = 30;
        DoorAccess.answer(level, worldPosition, player, ok, key);
    }

    @Override
    public void placedBy(Player player) {
        BlockEntityDoorGeneric found = door();
        if (found == null) return;
        if (found.scannerBound) answer(player, false, "scanner_door");
        else if (found.keypadCode != null) answer(player, true, "synced");
    }

    /** A code typed on the pad: binds the door the first time, then toggles it. */
    public void enter(Player player, String code) {
        if (!code.matches("\\d{1,6}")) {
            answer(player, false, "invalid");
            return;
        }
        BlockEntityDoorGeneric found = door();
        if (found == null) answer(player, false, "no_door");
        else if (found.scannerBound) answer(player, false, "scanner_door");
        else if (found.keypadCode == null) {
            found.keypadCode = code;
            found.setChanged();
            answer(player, true, "code_set");
        } else if (!found.keypadCode.equals(code)) answer(player, false, "wrong_code");
        else if (resetArmed) {
            found.keypadCode = null;
            found.setChanged();
            resetArmed = false;
            answer(player, true, "code_reset");
        } else {
            found.toggleByDevice();
            answer(player, true, "granted");
        }
    }

    /** R on the pad: closes an open keypad door, no code needed. */
    public void closeDoor(Player player) {
        BlockEntityDoorGeneric found = door();
        if (found == null || found.keypadCode == null) answer(player, false, "no_door");
        else if (found.state == BlockEntityDoorGeneric.STATE_OPEN) {
            found.close();
            answer(player, true, "closed");
        }
    }

    /** The screwdriver: the next correct code clears the door's code instead of opening it. */
    public void armReset(Player player) {
        BlockEntityDoorGeneric found = door();
        if (found == null || found.keypadCode == null) {
            answer(player, false, "no_code");
            return;
        }
        resetArmed = !resetArmed;
        answer(player, true, resetArmed ? "reset_armed" : "reset_cancelled");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        door = input.read("door", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("door", BlockPos.CODEC, door);
    }
}
