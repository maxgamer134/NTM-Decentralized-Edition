// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.mixin;

import com.hbm.space.SpaceWeather;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * WorldProviderCelestial's isHellWorld: in a vacuum or air too thin to hold water (orbit always),
 * ice mined or melted leaves nothing, as in the Nether, unless a pressurised room holds it.
 */
@Mixin(IceBlock.class)
public abstract class MixinIceSpace {

    @ModifyExpressionValue(method = "playerDestroy", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/dimension/DimensionType;ultraWarm()Z"))
    private boolean hbm$vacuumMined(boolean ultraWarm, Level level, Player player, BlockPos pos, BlockState state,
            BlockEntity blockEntity, ItemStack tool) {
        return ultraWarm || SpaceWeather.iceVanishes(level, pos);
    }

    @ModifyExpressionValue(method = "melt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/dimension/DimensionType;ultraWarm()Z"))
    private boolean hbm$vacuumMelted(boolean ultraWarm, BlockState state, Level level, BlockPos pos) {
        return ultraWarm || SpaceWeather.iceVanishes(level, pos);
    }
}
