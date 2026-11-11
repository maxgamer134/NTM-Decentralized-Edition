// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.integration.kubejs.LooseChange;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets trades accept other currencies and give change (see {@link LooseChange}). */
@Mixin(MerchantOffer.class)
public abstract class MixinMerchantOffer {

    @Inject(method = "satisfiedBy", at = @At("HEAD"), cancellable = true)
    private void hbm$looseChangeSatisfied(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
        Boolean result = LooseChange.satisfiedBy((MerchantOffer) (Object) this, a, b);
        if (result != null) cir.setReturnValue(result);
    }

    @Inject(method = "take", at = @At("HEAD"), cancellable = true)
    private void hbm$looseChangeTake(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
        Boolean result = LooseChange.take((MerchantOffer) (Object) this, a, b);
        if (result != null) cir.setReturnValue(result);
    }
}
