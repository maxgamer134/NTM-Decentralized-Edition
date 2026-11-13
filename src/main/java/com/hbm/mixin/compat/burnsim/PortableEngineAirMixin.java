// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.burnsim;

import com.hbm.space.atmosphere.SpaceCombustion;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Aeronautics' portable engine goes out, and takes no fuel, where there is no oxygen. Infinite
 * fuel still runs.
 */
@Pseudo
@Mixin(targets = "dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity", remap = false)
public abstract class PortableEngineAirMixin {
    @Shadow
    private int burnTime;

    @Unique
    private boolean hbm$noAir;

    @Shadow
    public abstract boolean isCurrentFuelInfinite();

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void hbm$check(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide
                || (self.getLevel().getGameTime() + self.getBlockPos().asLong()) % 20 != 0) return;
        hbm$noAir = !SpaceCombustion.canBurn(self.getLevel(), self.getBlockPos());
        if (hbm$noAir && burnTime > 0 && !isCurrentFuelInfinite()) burnTime = 0;
    }

    @Inject(method = "getNextBurnTime", at = @At("HEAD"), cancellable = true, require = 0)
    private void hbm$noFuel(CallbackInfoReturnable<Integer> cir) {
        if (hbm$noAir) cir.setReturnValue(0);
    }
}
