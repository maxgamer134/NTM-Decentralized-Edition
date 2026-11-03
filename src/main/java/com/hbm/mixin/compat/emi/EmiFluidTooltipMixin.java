// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.mixin.compat.emi;

import com.hbm.client.gui.FluidTooltipFrame;
import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * backport-fix: EMI (and TMRV, which runs JEI plugins on EMI) shows NTM fluids as its own fluid
 * stacks with only the name, so the JEI hook (JeiFluidTooltipMixin) never runs there. Main's NEI
 * fluid icons listed the traits (temperature, flammable/combustible energy, ...) and main's tank
 * hover drew the border in the fluid's colour (GUIElements.drawHoveringTextFluid): both added here.
 */
@Pseudo
@Mixin(targets = "dev.emi.emi.api.stack.FluidEmiStack", remap = false)
public abstract class EmiFluidTooltipMixin {

    private static Method hbm$getKey;

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true, require = 0)
    private void hbm$fluidTraits(CallbackInfoReturnable<List<ClientTooltipComponent>> cir) {
        Fluid fluid;
        try {
            if (hbm$getKey == null) hbm$getKey = getClass().getMethod("getKey");
            if (!(hbm$getKey.invoke(this) instanceof Fluid f)) return;
            fluid = f;
        } catch (ReflectiveOperationException e) {
            return;
        }
        List<ClientTooltipComponent> extra = new ArrayList<>();
        FluidTraitTooltip.addInfo(
                fluid, line -> extra.add(ClientTooltipComponent.create(line.getVisualOrderText())));
        if (extra.isEmpty()) return;
        List<ClientTooltipComponent> list = new ArrayList<>(cir.getReturnValue());
        // traits go before the trailing mod name line, as main lists them under name and amount
        list.addAll(list.size() > 1 ? list.size() - 1 : list.size(), extra);
        list.add(Math.min(1, list.size()), new FluidTooltipFrame.Renderer(FluidTooltipFrame.of(fluid)));
        cir.setReturnValue(list);
    }
}
