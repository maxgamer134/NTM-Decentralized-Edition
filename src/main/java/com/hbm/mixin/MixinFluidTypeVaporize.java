// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.space.SpaceWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NTM:Space's isHellWorld: in air too thin to hold liquid (orbit, the Mun, Minmus, Ike...) water
 * boils away as it's poured, as in the Nether, unless it's poured into a pressurised room.
 */
@Mixin(value = FluidType.class, remap = false)
public abstract class MixinFluidTypeVaporize {

    @Inject(method = "isVaporizedOnPlacement", at = @At("HEAD"), cancellable = true)
    private void hbm$boilsInVacuum(Level level, BlockPos pos, FluidStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (SpaceWeather.boilsAway(level, pos, stack)) cir.setReturnValue(true);
    }
}
