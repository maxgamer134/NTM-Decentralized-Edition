// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** backport-fix: BF-161 main 1.0.27_X5000's ItemGunLacunae: spin-up and spin-down sounds, and the barrel spin */
public class ItemGunLacunae extends ItemGunBase {

    public ItemGunLacunae(GunConfiguration config, Properties properties) {
        super(config, properties);
    }

    @Override
    public void startAction(ItemStack stack, Level world, Player player, boolean main) {
        if (main) {
            setDelay(stack, 20);
            LegacySounds.play(player, "hbm:weapon.lacunaeSpinup", 1.0F, 1.0F);
        }
    }

    @Override
    public void endAction(ItemStack stack, Level world, Player player, boolean main) {
        if (main) LegacySounds.play(player, "hbm:weapon.lacunaeSpindown", 1.0F, 1.0F);
    }

    @Override
    protected void updateServer(ItemStack stack, Level world, Player player, boolean isCurrentItem) {
        super.updateServer(stack, world, player, isCurrentItem);
        if (getIsMouseDown(stack)) {
            int rot = readNBT(stack, "rot") % 360;
            rot += 25;
            writeNBT(stack, "rot", rot);
        }
    }
}
