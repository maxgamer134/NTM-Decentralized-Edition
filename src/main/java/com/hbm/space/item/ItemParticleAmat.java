// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.item;

import com.hbm.data.ItemData;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidCell;
import net.minecraft.world.entity.item.ItemEntity;

/** NTM:Space's antimatter particle capsule annihilates when dropped, like the cell. */
public class ItemParticleAmat extends ItemFluidCell implements IItemEntityUpdate {

    public ItemParticleAmat(Properties properties) {
        super(properties, () -> NTMFluids.AMAT, ModItems.PARTICLE_EMPTY);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.onGround()) return false;
        if (ItemData.DROP_ANTIMATTER_CELL.get()) {
            new ExplosionVNT(entity.level(), entity.getX(), entity.getY(), entity.getZ(), 5F).makeAmat().explode();
        }
        entity.discard();
        return true;
    }
}
