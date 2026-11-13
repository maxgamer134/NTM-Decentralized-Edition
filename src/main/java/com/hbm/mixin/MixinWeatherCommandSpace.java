// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.mixin;

import com.hbm.command.SpaceCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WeatherCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** On another body or in orbit, /weather sets that body's weather (SpaceCommands.planetWeather), not the overworld's. */
@Mixin(WeatherCommand.class)
public abstract class MixinWeatherCommandSpace {

    @Inject(method = "setClear", at = @At("HEAD"), cancellable = true)
    private static void hbm$planetClear(CommandSourceStack source, int time, CallbackInfoReturnable<Integer> cir) {
        int result = SpaceCommands.planetWeather(source, SpaceCommands.Weather.CLEAR, time);
        if (result != SpaceCommands.NOT_A_BODY) cir.setReturnValue(result);
    }

    @Inject(method = "setRain", at = @At("HEAD"), cancellable = true)
    private static void hbm$planetRain(CommandSourceStack source, int time, CallbackInfoReturnable<Integer> cir) {
        int result = SpaceCommands.planetWeather(source, SpaceCommands.Weather.RAIN, time);
        if (result != SpaceCommands.NOT_A_BODY) cir.setReturnValue(result);
    }

    @Inject(method = "setThunder", at = @At("HEAD"), cancellable = true)
    private static void hbm$planetThunder(CommandSourceStack source, int time, CallbackInfoReturnable<Integer> cir) {
        int result = SpaceCommands.planetWeather(source, SpaceCommands.Weather.THUNDER, time);
        if (result != SpaceCommands.NOT_A_BODY) cir.setReturnValue(result);
    }
}
