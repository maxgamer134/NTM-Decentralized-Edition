// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.modules.machine.ModuleMachineBase;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * The four modules of a chemical or assembly factory feed each other: what one module makes goes
 * straight into another module's inputs when that module's recipe takes it, so chained recipes run
 * inside one factory without pipes or inserters looping back.
 */
public final class FactorySharing {

    private FactorySharing() {}

    /** Fluids: any output tank tops up any input tank of the same fluid and pressure, 50 mB a pair a tick (as main). */
    public static boolean shareFluids(FluidTankNTM[] inputs, FluidTankNTM[] outputs) {
        boolean changed = false;
        for (FluidTankNTM in : inputs) {
            if (in.getTankType() == null) continue;
            for (FluidTankNTM out : outputs) {
                if (out.getFill() <= 0 || !in.accepts(out.getFluid())) continue;
                if (out.getPressure() != in.getPressure()) continue;
                int toMove = Math.min(Math.min(in.getMaxFill() - in.getFill(), out.getFill()), 50);
                if (toMove > 0) {
                    int moved = in.receive(out.getFluid(), toMove);
                    out.setFill(out.getFill() - moved);
                    changed |= moved > 0;
                }
            }
        }
        return changed;
    }

    /**
     * Items: each module's output slots offer their stacks to the other modules' input slots that
     * their recipe accepts. A module never feeds itself, so a recipe can't loop on its own output.
     */
    public static boolean shareItems(
            Container inv,
            ModuleMachineBase[] modules,
            int slotsPerModule,
            int inputOffset,
            int inputCount,
            int outputOffset,
            int outputCount) {
        boolean changed = false;
        for (int from = 0; from < modules.length; from++) {
            for (int o = 0; o < outputCount; o++) {
                int outSlot = outputOffset + o + from * slotsPerModule;
                ItemStack stack = inv.getItem(outSlot);
                if (stack.isEmpty()) continue;
                for (int to = 0; to < modules.length && !stack.isEmpty(); to++) {
                    if (to == from || modules[to].getRecipe() == null) continue;
                    for (int i = 0; i < inputCount && !stack.isEmpty(); i++) {
                        int inSlot = inputOffset + i + to * slotsPerModule;
                        ItemStack target = inv.getItem(inSlot);
                        if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, stack)) continue;
                        if (!modules[to].isItemValid(inSlot, stack)) continue;
                        int limit = Math.min(stack.getMaxStackSize(), inv.getMaxStackSize());
                        int move = Math.min(stack.getCount(), limit - target.getCount());
                        if (move <= 0) continue;
                        if (target.isEmpty()) inv.setItem(inSlot, stack.copyWithCount(move));
                        else target.grow(move);
                        stack.shrink(move);
                        changed = true;
                    }
                }
                if (stack.isEmpty()) inv.setItem(outSlot, ItemStack.EMPTY);
            }
        }
        return changed;
    }
}
