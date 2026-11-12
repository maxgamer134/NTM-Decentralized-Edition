// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.space.SpaceTime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelTimeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Planets keep their own clock and day length ({@link SpaceTime}); nothing changes while Space is off. */
@Mixin(Level.class)
public abstract class MixinLevelSpaceTime implements LevelTimeAccess {

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void hbm$localTime(CallbackInfoReturnable<Long> cir) {
        long time = SpaceTime.get((Level) (Object) this);
        if (time >= 0) cir.setReturnValue(time);
    }

    /** WorldProviderDuna.isDaytime: a dust storm blots out the day (no sleeping through it, the undead don't burn). */
    @Inject(method = "isDay", at = @At("HEAD"), cancellable = true)
    private void hbm$dunaStormNight(CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (com.hbm.space.SpaceWeather.isDuna(level) && com.hbm.space.SpaceWeather.dunaStorm(level.getGameTime()) >= 0.2F) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public float getTimeOfDay(float partialTick) {
        float angle = SpaceTime.timeOfDay((Level) (Object) this, partialTick);
        return Float.isNaN(angle) ? dimensionType().timeOfDay(dayTime()) : angle;
    }
}
