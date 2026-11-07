// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only
package com.hbm.items.weapon.legacy;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.weapon.legacy.factory.GunEnergyFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/** main 1.0.27_X5000's ItemGunChemthrower: sprays whatever fluid fills it, 3 mB a shot, filled like a tank. */
public class ItemGunChemthrowerLegacy extends ItemGunBase implements IFluidContainerItem {

    public static final int transferSpeed = 50;

    public ItemGunChemthrowerLegacy(Properties p) {
        super(GunEnergyFactory.getChemConfig(), p);
    }

    @Override
    protected void fire(ItemStack stack, Level world, Player player) {
        if (!hasAmmo(stack, player, true)) return;
        spawnProjectile(world, player, stack, 0);
        playAnimation(player, stack, AnimType.CYCLE);
        useUpAmmo(player, stack, true);
        setItemWear(stack, getItemWear(stack) + 10);
    }

    @Override
    public boolean hasAmmo(ItemStack stack, Player player, boolean main) {
        return getMag(stack) >= getConsumption(stack);
    }

    @Override
    public void useUpAmmo(Player player, ItemStack stack, boolean main) {
        if (!main || player.getAbilities().instabuild) return;
        setMag(stack, getMag(stack) - getConsumption(stack));
    }

    @Override
    public boolean canReload(ItemStack stack, Level world, Player player) { return false; }

    @Override
    protected void spawnProjectile(Level world, Player player, ItemStack stack, int config) {
        EntityChemical chem = new EntityChemical(ModEntities.CHEMICAL.get(), world, player, 0, 0, 0);
        chem.setFluid(getFluidType(stack));
        world.addFreshEntity(chem);
    }

    @Override
    protected void reload2(ItemStack stack, Level world, Player player) {
        setIsReloading(stack, false);
    }

    public Fluid getFluidType(ItemStack stack) { return BuiltInRegistries.FLUID.byId(getMagType(stack)); }

    public int getConsumption(ItemStack stack) { return 3; }

    @Override
    public boolean machineFillable() { return true; }

    @Override
    public Fluid firstFluidType(ItemStack stack) { return getFluidType(stack); }

    @Override
    public int capacity(ItemStack stack) { return mainConfig.ammoCap; }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        int fill = getMag(stack);
        return fill <= 0 ? EMPTY_CONTENT : new FluidStackNTM(getFluidType(stack), fill, 0);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        setMagType(stack, BuiltInRegistries.FLUID.getId(content.type()));
        setMag(stack, (int) Math.min(content.amount(), capacity(stack)));
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        if (getFluidType(stack) != type && getMag(stack) != 0) return 0;
        if (getMag(stack) == 0) setMagType(stack, BuiltInRegistries.FLUID.getId(type));
        int fill = getMag(stack);
        int toFill = Math.min(Math.min(amount, capacity(stack) - fill), transferSpeed);
        setMag(stack, fill + toFill);
        return toFill;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        if (getFluidType(stack) != type) return 0;
        int fill = getMag(stack);
        int toUnload = Math.min(Math.min(fill, amount), transferSpeed);
        setMag(stack, fill - toUnload);
        return toUnload;
    }
}
