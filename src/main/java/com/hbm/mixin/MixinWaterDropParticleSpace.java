// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.WaterDropParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raindrops and splashes on a body take its rain's colour while it rains (SpaceEffects.rainParticleColor). */
@Mixin(WaterDropParticle.class)
public abstract class MixinWaterDropParticleSpace {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hbm$tintRain(ClientLevel level, double x, double y, double z, CallbackInfo ci) {
        float[] color = com.hbm.space.client.SpaceEffects.rainParticleColor(level);
        if (color != null) ((Particle) (Object) this).setColor(color[0], color[1], color[2]);
    }
}
