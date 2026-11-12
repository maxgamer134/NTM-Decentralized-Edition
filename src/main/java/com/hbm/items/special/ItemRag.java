// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.items.ModItems;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemRag extends ItemCustomLore implements IItemEntityUpdate {

    public ItemRag(Properties properties) {
        super(properties);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.level().getFluidState(entity.blockPosition()).is(FluidTags.WATER)) return false;

        entity.setItem(new ItemStack(ModItems.RAG_DAMP.get(), entity.getItem().getCount()));
        return true;
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            // NTM:Space: badly irradiated players bleed into the rag instead
            net.minecraft.world.item.Item out = com.hbm.space.SpaceToggle.enabled()
                            && com.hbm.extprop.HbmLivingProps.getRadiation(player) > 500
                    ? net.minecraft.core.registries.BuiltInRegistries.ITEM.get(com.hbm.lib.Library.id("rag_blood"))
                    : ModItems.RAG_PISS.get();
            player.getInventory().placeItemBackInInventory(new ItemStack(out));
            player.getItemInHand(hand).shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
