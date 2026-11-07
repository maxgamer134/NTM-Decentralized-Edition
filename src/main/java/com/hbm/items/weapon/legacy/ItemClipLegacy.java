// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.backport.InteractionResult;
import com.hbm.backport.compat.ItemCompat;
import java.util.function.Supplier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** backport-fix: BF-161 main 1.0.27_X5000's ItemClip: right click unpacks the ammo it holds. */
public class ItemClipLegacy extends ItemCompat {

    private final Supplier<? extends Item> ammo;
    private final int count;

    public ItemClipLegacy(Properties properties, Supplier<? extends Item> ammo, int count) {
        super(properties.stacksTo(1));
        this.ammo = ammo;
        this.count = count;
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack give = new ItemStack(ammo.get(), count);
            if (!player.getInventory().add(give)) player.drop(give, false);
            player.getItemInHand(hand).shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
