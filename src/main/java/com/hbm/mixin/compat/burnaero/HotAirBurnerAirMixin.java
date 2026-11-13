// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.burnaero;

import com.hbm.space.atmosphere.SpaceCombustion;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aeronautics' hot air burner puts out no hot air where there is no oxygen to burn in. */
@Pseudo
@Mixin(targets = "dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity", remap = false)
public abstract class HotAirBurnerAirMixin {
    @Unique
    private boolean hbm$noAir;

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void hbm$check(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide
                || (self.getLevel().getGameTime() + self.getBlockPos().asLong()) % 20 != 0) return;
        hbm$noAir = !SpaceCombustion.canBurn(self.getLevel(), self.getBlockPos());
    }

    @Inject(method = "canOutputGas", at = @At("HEAD"), cancellable = true, require = 0)
    private void hbm$smothered(CallbackInfoReturnable<Boolean> cir) {
        if (hbm$noAir) cir.setReturnValue(false);
    }
}
