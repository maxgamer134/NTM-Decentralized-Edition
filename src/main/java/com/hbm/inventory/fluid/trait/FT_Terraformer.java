// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * NTM:Space's terraforming gas: released on the body it is named after, it reshapes that body's
 * surface (Duna's sand, Laythe's silt). The body is kept by its lowercase name, the way the
 * fluid_property JSON writes it, so this trait needs nothing of the solar system to load.
 */
public class FT_Terraformer extends FluidTrait {

    private final String body;

    public FT_Terraformer(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    @Override
    public void addInfo(List<Component> info) {
        info.add(Component.translatable("hbmfluid.trait.terraformer").withStyle(ChatFormatting.AQUA));
    }
}
