// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.mixin;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Under the full celestial sky, Kerbin's fog is its air's (SpaceFog.kerbinFogColor), darkened in eclipses. */
@Mixin(DimensionSpecialEffects.OverworldEffects.class)
public abstract class MixinOverworldEffectsSpace {

    @Inject(method = "getBrightnessDependentFogColor", at = @At("HEAD"), cancellable = true)
    private void hbm$kerbinFog(Vec3 fog, float brightness, CallbackInfoReturnable<Vec3> cir) {
        Vec3 kerbin = com.hbm.space.client.SpaceFog.kerbinFogColor();
        if (kerbin != null) cir.setReturnValue(kerbin);
    }
}
