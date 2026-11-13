// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.network;

import com.hbm.backport.storage.ContainerStorage;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.space.SpaceToggle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityCranePartitioner extends BlockEntityMachineBase {

    private static final int SLOT_COUNT = 45;

    /** Space's partitioner holds 96 queued stacks and only 8 finished ones. */
    public static int inputCount() {
        return SpaceToggle.changes() ? 96 : SLOT_COUNT;
    }

    public static int outputCount() {
        return SpaceToggle.changes() ? 8 : SLOT_COUNT;
    }

    private static final Comparator<ItemStack> BY_STACK_SIZE =
            (a, b) -> (int) Math.signum(a.getCount() - b.getCount());

    private static final int[] ACCESS = accessAll();

    public BlockEntityCranePartitioner(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_PARTITIONER.get(), pos, state, inputCount() + outputCount());
    }

    private static int[] accessAll() {
        int[] access = new int[inputCount() + outputCount()];
        for (int i = 0; i < access.length; i++) access[i] = i;
        return access;
    }

    @Override
    public void tickServer() {
        if ((!overflowIn.isEmpty() || !overflowOut.isEmpty()) && refill()) setChanged();
        List<ItemStack> queued = new ArrayList<>();
        for (int i = 0; i < inputCount(); i++) if (!getItem(i).isEmpty()) queued.add(getItem(i));
        queued.sort(BY_STACK_SIZE);

        boolean changed = false;

        for (ItemStack stack : queued) {
            int amount = CrystallizerRecipes.INSTANCE.getAmount(stack);
            if (amount == 0) amount = stack.getCount();

            while (stack.getCount() >= amount) {
                EntityMovingItem item = new EntityMovingItem(level);
                item.setItemStack(stack.copyWithCount(amount));
                item.moveTo(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.25,
                        worldPosition.getZ() + 0.5,
                        0F,
                        0F);
                level.addFreshEntity(item);

                stack.shrink(amount);
                changed = true;
            }
        }

        for (int i = 0; i < inputCount(); i++) {
            if (getItem(i).isEmpty()) inventory.set(i, ItemStack.EMPTY);
        }

        if (changed) setChanged();
    }

    /** Stacks a changed layout had no room for, by role; they move back in as slots free up. */
    private final List<ItemStack> overflowIn = new ArrayList<>();
    private final List<ItemStack> overflowOut = new ArrayList<>();

    private static final String KEY_INPUTS = "partitionerInputs", KEY_OUTPUTS = "partitionerOutputs";

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        overflowIn.clear();
        overflowOut.clear();
        input.child("overflowIn").ifPresent(in -> readList(in, overflowIn));
        input.child("overflowOut").ifPresent(in -> readList(in, overflowOut));
        input.child("inventory").ifPresent(in -> remap(input, in));
    }

    /**
     * The slots as saved, whatever the layout then (45 queued and 45 finished before Space's
     * 96 and 8): queued stacks stay queued and finished ones finished, in the same order.
     */
    private void remap(ValueInput root, ValueInput saved) {
        NonNullList<ItemStack> all = NonNullList.withSize(256, ItemStack.EMPTY);
        ContainerStorage.loadAllItems(saved, all);
        int savedIn = root.getIntOr(KEY_INPUTS, -1), savedOut = root.getIntOr(KEY_OUTPUTS, -1);
        if (savedIn < 0 || savedOut < 0) {
            // saves from before the marker: 45 and 45, unless they used slots only the 96 and 8 had
            boolean wide = false;
            for (int i = SLOT_COUNT * 2; i < all.size(); i++) wide |= !all.get(i).isEmpty();
            savedIn = wide ? 96 : SLOT_COUNT;
            savedOut = wide ? 8 : SLOT_COUNT;
        }
        int in = inputCount(), out = inventory.size() - in;
        if (savedIn == in && savedOut == out) return;
        for (int i = 0; i < inventory.size(); i++) inventory.set(i, ItemStack.EMPTY);
        for (int i = 0; i < all.size(); i++) {
            ItemStack stack = all.get(i);
            if (stack.isEmpty()) continue;
            boolean queued = i < savedIn;
            int slot = queued ? i : i - savedIn;
            if (queued && slot < in) inventory.set(slot, stack);
            else if (!queued && i < savedIn + savedOut && slot < out) inventory.set(in + slot, stack);
            else (queued ? overflowIn : overflowOut).add(stack);
        }
        refill();
    }

    /** Overflow into any empty slot of its role. */
    private boolean refill() {
        boolean moved = false;
        int in = inputCount();
        for (int i = 0; i < inventory.size() && !(overflowIn.isEmpty() && overflowOut.isEmpty()); i++) {
            List<ItemStack> from = i < in ? overflowIn : overflowOut;
            if (from.isEmpty() || !inventory.get(i).isEmpty()) continue;
            inventory.set(i, from.removeFirst());
            moved = true;
        }
        return moved;
    }

    private static void readList(ValueInput in, List<ItemStack> into) {
        NonNullList<ItemStack> list = NonNullList.withSize(256, ItemStack.EMPTY);
        ContainerStorage.loadAllItems(in, list);
        for (ItemStack stack : list) if (!stack.isEmpty()) into.add(stack);
    }

    private static void writeList(ValueOutput out, List<ItemStack> from) {
        NonNullList<ItemStack> list = NonNullList.withSize(Math.min(256, from.size()), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) list.set(i, from.get(i));
        ContainerStorage.saveAllItems(out, list);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(KEY_INPUTS, inputCount());
        output.putInt(KEY_OUTPUTS, inventory.size() - inputCount());
        if (!overflowIn.isEmpty()) writeList(output.child("overflowIn"), overflowIn);
        if (!overflowOut.isEmpty()) writeList(output.child("overflowOut"), overflowOut);
    }

    @Override
    public void spillInventory(BlockPos pos) {
        super.spillInventory(pos);
        if (level == null) return;
        for (ItemStack stack : overflowIn) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        for (ItemStack stack : overflowOut) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        overflowIn.clear();
        overflowOut.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < inputCount() && CrystallizerRecipes.INSTANCE.getAmount(stack) >= 1;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= inputCount();
    }
}
