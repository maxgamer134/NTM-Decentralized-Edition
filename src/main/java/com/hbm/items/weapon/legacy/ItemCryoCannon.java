// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.fluid.NTMFluids;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's ItemCryoCannon: holding fire spins the turbine up, it only sprays
 * once the turbine is at full speed, each puff of oxygen builds pressure and at full pressure it stops.
 */
public class ItemCryoCannon extends ItemGunBase {

    public ItemCryoCannon(GunConfiguration config, Properties properties) {
        super(config, properties);
    }

    @Override
    protected void fire(ItemStack stack, Level world, Player player) {
        if (getPressure(stack) >= 1000) return;
        if (getTurbine(stack) < 100) return;
        super.fire(stack, world, player);
    }

    @Override
    protected void spawnProjectile(Level world, Player player, ItemStack stack, int config) {
        EntityChemical chem = new EntityChemical(ModEntities.CHEMICAL.get(), world, player, 0, 0, 0);
        chem.setFluid(NTMFluids.OXYGEN);
        world.addFreshEntity(chem);
        setPressure(stack, Mth.clamp(getPressure(stack) + 5, 0, 1000));
    }

    @Override
    protected void updateServer(ItemStack stack, Level world, Player player, boolean isCurrentItem) {
        int turbine = getTurbine(stack);
        int pressure = getPressure(stack);
        if (getIsMouseDown(stack)) {
            turbine += 10;
        } else {
            turbine -= 5;
            pressure -= 5;
        }
        setTurbine(stack, Mth.clamp(turbine, 0, 100));
        setPressure(stack, Mth.clamp(pressure, 0, 1000));
        super.updateServer(stack, world, player, isCurrentItem);
    }

    public static void setTurbine(ItemStack stack, int i) {
        writeNBT(stack, "turbine", i);
    }

    public static int getTurbine(ItemStack stack) {
        return readNBT(stack, "turbine");
    }

    public static void setPressure(ItemStack stack, int i) {
        writeNBT(stack, "pressure", i);
    }

    public static int getPressure(ItemStack stack) {
        return readNBT(stack, "pressure");
    }
}
