// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With NTM:Space on, max health may go past vanilla's 1024, as in 1.7.10: the Radioactive Angel
 * has 10,000 and the stronger siege craft tiers several thousand.
 */
@Mixin(RangedAttribute.class)
public abstract class MixinRangedAttribute extends Attribute {

    @Shadow @Final private double minValue;

    private MixinRangedAttribute() {
        super("", 0);
    }

    @Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
    private void hbm$uncapMaxHealth(double value, CallbackInfoReturnable<Double> cir) {
        if (!"attribute.name.generic.max_health".equals(getDescriptionId())) return;
        if (!com.hbm.space.SpaceToggle.enabled()) return;
        cir.setReturnValue(Double.isNaN(value) ? minValue : Mth.clamp(value, minValue, 1_000_000D));
    }
}
