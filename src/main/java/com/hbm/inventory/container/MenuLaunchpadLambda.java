// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.blockentity.machine.BlockEntityLaunchpadLambda;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuLaunchpadLambda extends BlockEntityMenu<BlockEntityLaunchpadLambda> {

    public MenuLaunchpadLambda(int containerId, Inventory playerInv, BlockEntityLaunchpadLambda pad) {
        super(ModMenus.LAUNCHPAD_LAMBDA.get(), containerId, pad);
        checkContainerSize(pad, BlockEntityLaunchpadLambda.SLOT_COUNT);

        // lambda, satellite
        addSlot(new ValidatedSlot(pad, 0, 35, 17));
        addSlot(new ValidatedSlot(pad, 1, 53, 17));
        // fuel in/out, oxidizer in/out, battery
        addSlot(new Slot(container(), 2, 107, 80));
        addSlot(new Slot(container(), 3, 107, 98));
        addSlot(new Slot(container(), 4, 125, 80));
        addSlot(new Slot(container(), 5, 125, 98));
        addSlot(new Slot(container(), 6, 89, 80));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 144 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 202));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMove(
                player,
                index,
                false,
                stack -> {
                    if (index < BlockEntityLaunchpadLambda.SLOT_COUNT) {
                        return moveItemStackTo(
                                stack, BlockEntityLaunchpadLambda.SLOT_COUNT, slots.size(), true);
                    }
                    return moveItemStackTo(stack, 0, BlockEntityLaunchpadLambda.SLOT_COUNT, false);
                });
    }

    private static final class ValidatedSlot extends Slot {

        private final BlockEntityLaunchpadLambda pad;

        private ValidatedSlot(BlockEntityLaunchpadLambda pad, int index, int x, int y) {
            super(pad, index, x, y);
            this.pad = pad;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return pad.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
