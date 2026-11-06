// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only
package com.hbm.items.weapon.legacy;

import com.hbm.backport.TooltipDisplay;
import com.hbm.items.weapon.GunB92;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * main 1.0.27_X5000's GunB93, the b92 energy mod: it charges and fires like the repo's b92. main's
 * EntityModBeam (one beam whose mode is the charge) is the b92's own beams here.
 */
public class GunB93 extends GunB92 {
    public GunB93(Properties p) { super(p); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal("[LEGENDARY WEAPON]"));
    }
}
