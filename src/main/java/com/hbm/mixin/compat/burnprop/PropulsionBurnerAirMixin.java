// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.burnprop;

import com.hbm.space.atmosphere.SpaceCombustion;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Propulsion's solid and liquid burners go out where there is no oxygen. Its thrusters are left alone. */
@Pseudo
@Mixin(
        targets = {
            "dev.propulsionteam.propulsionsimulated.content.heat.burners.solid.SolidBurnerBlockEntity",
            "dev.propulsionteam.propulsionsimulated.content.heat.burners.liquid.LiquidBurnerBlockEntity"
        },
        remap = false)
public abstract class PropulsionBurnerAirMixin {
    @Shadow
    private int burnTime;

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void hbm$smother(CallbackInfo ci) {
        if (burnTime > 0 && SpaceCombustion.outOfAir((BlockEntity) (Object) this)) burnTime = 0;
    }
}
