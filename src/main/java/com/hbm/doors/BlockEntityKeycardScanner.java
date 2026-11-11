// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from susy-ntmdoors (Kyron04): https://github.com/Kyron04/susy-ntmdoors

package com.hbm.doors;

import com.hbm.backport.BlockEntityCompat;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityDoorGeneric;
import com.mojang.serialization.Codec;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The scanner's owner, its door and the cards it accepts. Placing it next to a door makes that a
 * security door; removing it makes the door an ordinary one again.
 */
public class BlockEntityKeycardScanner extends BlockEntityCompat implements BlockDoorDevice.DoorDevice {

    private @Nullable BlockPos door;
    private @Nullable UUID owner;
    private final Set<String> cards = new LinkedHashSet<>();
    private int statusTicks;

    public BlockEntityKeycardScanner(BlockPos pos, BlockState state) {
        super(DoorAccess.SCANNER_BE.get(), pos, state);
    }

    public void tickServer() {
        if (statusTicks > 0 && --statusTicks == 0) light(0);
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

    private boolean owns(Player player) {
        return owner == null || owner.equals(player.getUUID()) || player.getAbilities().instabuild;
    }

    /** The door this scanner runs, claiming it if it is free. */
    private @Nullable BlockEntityDoorGeneric door(@Nullable Player player) {
        BlockEntityDoorGeneric found = DoorAccess.touchingDoor(level, worldPosition, door);
        if (found == null) {
            answer(player, false, "no_door");
            return null;
        }
        if (found.keypadCode != null) {
            answer(player, false, "keypad_door");
            return null;
        }
        if (found.scannerBound && found.scannerOwner != null && owner != null && !found.scannerOwner.equals(owner)) {
            answer(player, false, "other_owner");
            return null;
        }
        if (!found.scannerBound || !found.getBlockPos().equals(door)) {
            found.scannerBound = true;
            found.scannerOwner = owner;
            found.setChanged();
            door = found.getBlockPos();
            setChanged();
        }
        return found;
    }

    @Override
    public void placedBy(Player player) {
        owner = player.getUUID();
        setChanged();
        if (door(player) != null) answer(player, true, "scanner_bound");
    }

    @Override
    public void removed() {
        if (door != null && level.getBlockEntity(door) instanceof BlockEntityDoorGeneric found && found.scannerBound) {
            found.scannerBound = false;
            found.scannerRedstone = false;
            found.scannerOwner = null;
            found.setChanged();
        }
    }

    /** A card on the reader: the owner sneaking adds or removes it, anyone else is let in or not. */
    public void swipe(Player player, ItemStack stack) {
        BlockEntityDoorGeneric found = door(player);
        if (found == null) return;
        if (ItemKeycard.isCreative(stack)) {
            found.toggleByDevice();
            answer(player, true, "granted");
            return;
        }
        String id = ItemKeycard.id(stack);
        if (player.isShiftKeyDown() && owns(player)) {
            boolean added = cards.add(id);
            if (!added) cards.remove(id);
            setChanged();
            answer(player, true, added ? "card_added" : "card_removed");
        } else if (cards.contains(id)) {
            found.toggleByDevice();
            answer(player, true, "granted");
        } else answer(player, false, "denied");
    }

    /** The owner sneaking with an empty hand lets redstone run the door, or stops it again. */
    public void toggleRedstone(Player player) {
        BlockEntityDoorGeneric found = door(player);
        if (found == null) return;
        if (!owns(player)) {
            answer(player, false, "not_owner");
            return;
        }
        found.scannerRedstone = !found.scannerRedstone;
        found.setChanged();
        answer(player, true, found.scannerRedstone ? "redstone_on" : "redstone_off");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        door = input.read("door", BlockPos.CODEC).orElse(null);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        cards.clear();
        input.read("cards", Codec.STRING.listOf()).ifPresent(cards::addAll);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("door", BlockPos.CODEC, door);
        output.storeNullable("owner", UUIDUtil.CODEC, owner);
        output.store("cards", Codec.STRING.listOf(), List.copyOf(cards));
    }
}
