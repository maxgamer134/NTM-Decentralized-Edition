// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** A held item that counts what its holder kills (NTMain's stat track). */
public interface IStatTrack {

    void tickUp(ItemStack stack, LivingEntity target);
}
