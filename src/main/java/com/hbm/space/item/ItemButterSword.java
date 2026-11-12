// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

/** NTM:Space's BSword: a wooden sword made of butter, and squid it hits blow up. */
public class ItemButterSword extends SwordItem {

    public ItemButterSword(Properties properties) {
        super(Tiers.WOOD, properties.attributes(SwordItem.createAttributes(Tiers.WOOD, 3, -2.4F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof Squid && !target.level().isClientSide()) {
            target.level().explode(attacker, target.getX(), target.getY(), target.getZ(), 2F, true, Level.ExplosionInteraction.MOB);
        }
        return super.hurtEnemy(stack, target, attacker);
    }
}
