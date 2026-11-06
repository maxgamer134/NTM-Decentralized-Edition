// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemFertilizer;
import com.hbm.items.tool.ItemModMinecart;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LevelEvent;

public final class DispenserBehaviorHandler {

    private DispenserBehaviorHandler() {}

    public static void init() {

        DispenserBlock.registerBehavior(
                ModItems.GRENADE_UNIVERSAL.get(),
                new DefaultDispenseItemBehavior() {
                    @Override
                    protected ItemStack execute(BlockSource source, ItemStack stack) {

                        Direction facing = source.state().getValue(DispenserBlock.FACING);

                        EntityGrenadeUniversal grenade =
                                new EntityGrenadeUniversal(source.level(), stack);
                        EnumGrenadeShell shell = grenade.getShell();

                        grenade.setPos(
                                source.center().x + facing.getStepX() * 0.75,
                                source.center().y + facing.getStepY() * 0.75,
                                source.center().z + facing.getStepZ() * 0.75);
                        grenade.setDeltaMovement(
                                facing.getStepX() * shell.getYeetForce(),
                                facing.getStepY() * shell.getYeetForce(),
                                facing.getStepZ() * shell.getYeetForce());
                        source.level().addFreshEntity(grenade);

                        stack.shrink(1);
                        return stack;
                    }
                });

        DispenserBlock.registerBehavior(
                ModItems.POWDER_FERTILIZER.get(),
                new DefaultDispenseItemBehavior() {
                    private boolean grew = true;

                    @Override
                    protected ItemStack execute(BlockSource source, ItemStack stack) {
                        Direction facing = source.state().getValue(DispenserBlock.FACING);
                        grew =
                                ItemFertilizer.spread(
                                        stack, source.level(), source.pos().relative(facing));
                        return stack;
                    }

                    @Override
                    protected void playSound(BlockSource source) {
                        source.level()
                                .levelEvent(
                                        grew
                                                ? LevelEvent.SOUND_DISPENSER_DISPENSE
                                                : LevelEvent.SOUND_DISPENSER_FAIL,
                                        source.pos(),
                                        0);
                    }
                });

        // backport-fix: BF-143 main's carts come out of a dispenser onto the rail in front of it
        // (or the rail one below an empty block), like vanilla carts; ours were dropped as items
        DefaultDispenseItemBehavior cartBehavior =
                new DefaultDispenseItemBehavior() {
                    private final DefaultDispenseItemBehavior drop =
                            new DefaultDispenseItemBehavior();

                    @Override
                    protected ItemStack execute(BlockSource source, ItemStack stack) {
                        Direction facing = source.state().getValue(DispenserBlock.FACING);
                        Level level = source.level();
                        double x = source.center().x + facing.getStepX() * 1.125D;
                        double y = source.center().y + facing.getStepY() * 1.125D;
                        double z = source.center().z + facing.getStepZ() * 1.125D;
                        BlockPos front = source.pos().relative(facing);
                        double yOffset;
                        if (level.getBlockState(front).is(BlockTags.RAILS)) {
                            yOffset = 0.0D;
                        } else {
                            if (!level.getBlockState(front).isAir()
                                    || !level.getBlockState(front.below()).is(BlockTags.RAILS)) {
                                return drop.dispense(source, stack);
                            }
                            yOffset = -1.0D;
                        }
                        Entity cart =
                                ItemModMinecart.createMinecart(level, x, y + yOffset, z, stack);
                        if (stack.has(DataComponents.CUSTOM_NAME)) {
                            cart.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
                        }
                        level.addFreshEntity(cart);
                        stack.shrink(1);
                        return stack;
                    }
                };
        for (ItemModMinecart.EnumMinecart cart : ItemModMinecart.EnumMinecart.VALUES) {
            DispenserBlock.registerBehavior(ModItems.CART.get(cart), cartBehavior);
        }

        DispenserBlock.registerProjectileBehavior(ModItems.STICK_DYNAMITE.get());
        DispenserBlock.registerProjectileBehavior(ModItems.STICK_DYNAMITE_FISHING.get());
    }
}
