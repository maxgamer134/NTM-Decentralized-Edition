// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.space.SpaceTime;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Planets keep their own clock ({@link SpaceTime}) and weather ({@link com.hbm.space.trait.CBT_Weather}). */
@Mixin(ServerLevel.class)
public abstract class MixinServerLevelSpaceTime {

    @Inject(method = "setDayTime", at = @At("HEAD"), cancellable = true)
    private void hbm$localTime(long time, CallbackInfo ci) {
        if (SpaceTime.set((ServerLevel) (Object) this, time)) ci.cancel();
    }

    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void hbm$localWeather(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        var body = SpaceTime.body(level);
        if (body == null) return;
        com.hbm.space.trait.CBT_Weather.applyTo(level, body);
        ci.cancel();
    }
}
