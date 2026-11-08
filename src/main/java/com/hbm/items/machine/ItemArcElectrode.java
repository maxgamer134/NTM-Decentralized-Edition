// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import net.minecraft.world.item.ItemStack;
import com.hbm.backport.compat.ItemCompat;

public class ItemArcElectrode extends ItemCompat {

    public final EnumElectrodeType type;

    public ItemArcElectrode(Properties props, EnumElectrodeType type) {
        super(props.durability(type.durability));
        this.type = type;
    }

    public static int getDurability(ItemStack stack) {
        return stack.getDamageValue();
    }

    public static boolean damage(ItemStack stack) {
        stack.setDamageValue(stack.getDamageValue() + 1);
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    public static int getMaxDurability(ItemStack stack) {
        return stack.getMaxDamage();
    }

    public enum EnumElectrodeType {
        GRAPHITE(10),
        LANTHANIUM(100),
        DESH(500),
        SATURNITE(1500);

        public static final EnumElectrodeType[] VALUES = values();

        public final int durability;

        EnumElectrodeType(int dura) {
            this.durability = dura;
        }
    }
}
