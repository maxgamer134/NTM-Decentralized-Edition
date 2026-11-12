// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NTM:Space's furnace (TileEntityFurnaceSpace): the furnace only lights fuel where there is air to
 * burn it in, taken from the planet's atmosphere or the sealed room it stands in. Lava needs no air.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MixinFurnaceSpace {

    @ModifyExpressionValue(
            method = "serverTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration(Lnet/minecraft/world/item/ItemStack;)I"))
    private static int hbm$needsAir(int burnTime, Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace) {
        if (burnTime <= 0 || !(furnace instanceof FurnaceBlockEntity) || !com.hbm.space.SpaceToggle.enabled()) return burnTime;
        if (!(level instanceof ServerLevel server)) return burnTime;
        ItemStack fuel = furnace.getItem(1);
        if (fuel.is(Items.LAVA_BUCKET)) return burnTime;
        return com.hbm.space.atmosphere.SpaceAtmosphere.breatheAir(server, pos, 0) ? burnTime : 0;
    }
}
