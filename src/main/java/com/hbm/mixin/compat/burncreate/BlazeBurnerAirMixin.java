// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.burncreate;

import com.hbm.space.atmosphere.SpaceCombustion;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A fuelled blaze burner goes out, and takes no fuel, where there is no oxygen. Creative fuel
 * still works. Applied only with Create installed.
 */
@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public abstract class BlazeBurnerAirMixin {
    @Shadow protected BlazeBurnerBlockEntity.FuelType activeFuel;
    @Shadow protected int remainingBurnTime;
    @Shadow public boolean isCreative;

    @Shadow
    protected abstract void setBlockHeat(BlazeBurnerBlock.HeatLevel heat);

    @Shadow
    protected abstract BlazeBurnerBlock.HeatLevel getHeatLevel();

    @Shadow
    public abstract boolean isCreativeFuel(ItemStack stack);

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void hbm$smother(CallbackInfo ci) {
        if (isCreative || activeFuel == BlazeBurnerBlockEntity.FuelType.NONE) return;
        if (!SpaceCombustion.outOfAir((BlockEntity) (Object) this)) return;
        activeFuel = BlazeBurnerBlockEntity.FuelType.NONE;
        remainingBurnTime = 0;
        setBlockHeat(getHeatLevel());
    }

    @Inject(method = "tryUpdateFuel", at = @At("HEAD"), cancellable = true, require = 0)
    private void hbm$noAir(ItemStack stack, boolean forceOverflow, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() == null || isCreativeFuel(stack)) return;
        if (!SpaceCombustion.canBurn(self.getLevel(), self.getBlockPos())) cir.setReturnValue(false);
    }
}
