// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.impl;

import com.hbm.items.IStatTrack;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** NTMain's corrupted Henry: its name is the number of kills made with it. */
public class ItemGunCounter extends ItemGunBaseNT implements IStatTrack {

    public static final String KEY_STATTRACK = "stattrack";

    public ItemGunCounter(WeaponQuality quality, Properties properties, GunConfig... cfg) {
        super(quality, properties, cfg);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(Integer.toString(getScore(stack)));
    }

    @Override
    public void tickUp(ItemStack stack, LivingEntity target) {
        setScore(stack, getScore(stack) + 1);
    }

    public static int getScore(ItemStack stack) {
        return getValueInt(stack, KEY_STATTRACK);
    }

    public static void setScore(ItemStack stack, int value) {
        setValueInt(stack, KEY_STATTRACK, value);
    }
}
