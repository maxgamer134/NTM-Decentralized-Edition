// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.hbm.lib.ModDamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's ItemGunGauss, the xvl1456 tau cannon: hold alt fire to charge (one
 * ammo every half second), let go to fire a shot worth 10 damage per ammo stored, hold past ten seconds and it
 * blows up in your hands. main also looped the charge sound on the client while charging; the repo's legacy guns
 * have no client hooks, so the charge sound plays once when charging starts.
 */
public class ItemGunGauss extends ItemGunBase {

    public ItemGunGauss(GunConfiguration config, GunConfiguration alt, Properties properties) {
        super(config, alt, properties);
    }

    private static Item ammo() {
        return LegacyGuns.GUN_XVL1456_AMMO.get();
    }

    @Override
    public void startAction(ItemStack stack, Level world, Player player, boolean main) {
        super.startAction(stack, world, player, main);
        if (!main && getItemWear(stack) < mainConfig.durability && player.getInventory().contains(new ItemStack(ammo())))
            LegacySounds.play(player, "hbm:weapon.tauChargeLoop2", 1.0F, 0.75F);
    }

    @Override
    public void endAction(ItemStack stack, Level world, Player player, boolean main) {
        if (getHasShot(stack)) {
            LegacySounds.play(player, "hbm:weapon.sparkShoot", 2.0F, 1.0F);
            setHasShot(stack, false);
        }
        if (!main && getStored(stack) > 0) {
            EntityBulletBaseNT bullet = new EntityBulletBaseNT(world, altConfig.config.get(0), player);
            bullet.overrideDamage = Math.max(getStored(stack), 1) * 10F;
            world.addFreshEntity(bullet);
            LegacySounds.play(player, "hbm:weapon.tauShoot", 0.5F, 0.75F);
            setItemWear(stack, getItemWear(stack) + (getCharge(stack)) * 2);
            setCharge(stack, 0);
            playAnimation(player, stack, AnimType.CYCLE);
        }
    }

    @Override
    protected void altFire(ItemStack stack, Level world, Player player) {
        setCharge(stack, 1);
    }

    @Override
    protected void updateServer(ItemStack stack, Level world, Player player, boolean isCurrentItem) {
        super.updateServer(stack, world, player, isCurrentItem);
        if (getIsAltDown(stack) && getItemWear(stack) < mainConfig.durability) {
            int c = getCharge(stack);
            if (c > 200) {
                setCharge(stack, 0);
                setItemWear(stack, mainConfig.durability);
                player.hurt(world.damageSources().source(ModDamageTypes.TAU_BLAST), 1000);
                world.explode(player, player.getX(), player.getEyeY(), player.getZ(), 5.0F, true, Level.ExplosionInteraction.TNT);
                return;
            }
            if (c > 0) {
                setCharge(stack, c + 1);
                if (c % 10 == 1 && c < 140 && c > 2) {
                    int slot = player.getInventory().findSlotMatchingItem(new ItemStack(ammo()));
                    if (slot >= 0) {
                        player.getInventory().removeItem(slot, 1);
                        setStored(stack, getStored(stack) + 1);
                    } else {
                        setCharge(stack, 0);
                        setStored(stack, 0);
                    }
                }
            } else {
                setStored(stack, 0);
            }
        } else {
            setCharge(stack, 0);
            setStored(stack, 0);
        }
    }

    @Override
    protected void spawnProjectile(Level world, Player player, ItemStack stack, int config) {
        super.spawnProjectile(world, player, stack, config);
        setHasShot(stack, true);
    }

    public static void setHasShot(ItemStack stack, boolean b) {
        writeNBT(stack, "hasShot", b ? 1 : 0);
    }

    public static boolean getHasShot(ItemStack stack) {
        return readNBT(stack, "hasShot") == 1;
    }

    public static void setCharge(ItemStack stack, int i) {
        writeNBT(stack, "gauss_charge", i);
    }

    public static int getCharge(ItemStack stack) {
        return readNBT(stack, "gauss_charge");
    }

    public static void setStored(ItemStack stack, int i) {
        writeNBT(stack, "gauss_stored", i);
    }

    public static int getStored(ItemStack stack) {
        return readNBT(stack, "gauss_stored");
    }
}
