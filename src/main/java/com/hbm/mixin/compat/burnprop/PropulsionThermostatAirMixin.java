// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.burnprop;

import com.hbm.space.atmosphere.SpaceCombustion;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Propulsion's burners take no new fuel where there is no oxygen to burn it in. */
@Pseudo
@Mixin(targets = "dev.propulsionteam.propulsionsimulated.content.heat.burners.AbstractBurnerBlockEntity", remap = false)
public abstract class PropulsionThermostatAirMixin {
    @Inject(method = "shouldThermostatBurn", at = @At("HEAD"), cancellable = true, require = 0)
    private void hbm$noAir(CallbackInfoReturnable<Boolean> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() != null && !SpaceCombustion.canBurn(self.getLevel(), self.getBlockPos()))
            cir.setReturnValue(false);
    }
}
