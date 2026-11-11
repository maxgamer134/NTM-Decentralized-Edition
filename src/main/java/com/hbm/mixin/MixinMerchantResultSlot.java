// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.integration.kubejs.LooseChange;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands the trading player the change {@link LooseChange} held back. */
@Mixin(MerchantResultSlot.class)
public abstract class MixinMerchantResultSlot {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void hbm$beginTake(Player player, ItemStack stack, CallbackInfo ci) {
        LooseChange.beginTake();
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void hbm$giveChange(Player player, ItemStack stack, CallbackInfo ci) {
        LooseChange.giveChange(player);
    }
}
