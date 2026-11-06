// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.entity.projectile.EntityBulletBaseNT;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's ItemGunOSIPR, the pulse rifle: alt fire charges for a second, then
 * throws a combine ball (1000 damage, a small blast when it dies) for one dark energy ball.
 */
public class ItemGunOSIPR extends ItemGunBase {

    public ItemGunOSIPR(GunConfiguration config, GunConfiguration alt, Properties properties) {
        super(config, alt, properties);
    }

    @Override
    protected void altFire(ItemStack stack, Level world, Player player) {
        setCharge(stack, 1);
        LegacySounds.play(player, "hbm:weapon.osiprCharging", 1.0F, 1F);
    }

    @Override
    protected void updateServer(ItemStack stack, Level world, Player player, boolean isCurrentItem) {
        super.updateServer(stack, world, player, isCurrentItem);
        if (!isCurrentItem) {
            setCharge(stack, 0);
            return;
        }
        int i = getCharge(stack);
        if (i >= 20) {
            world.addFreshEntity(new CombineBall(world, BulletConfigSyncingUtil.SPECIAL_OSIPR_CHARGED, player));
            LegacySounds.play(player, altConfig.firingSound, 1.0F, 1F);
            setCharge(stack, 0);
            setDelay(stack, altConfig.rateOfFire);
            int slot = player.getInventory().findSlotMatchingItem(new ItemStack(LegacyGuns.GUN_OSIPR_AMMO2.get()));
            if (slot >= 0) player.getInventory().removeItem(slot, 1);
        } else if (i > 0) setCharge(stack, i + 1);
    }

    @Override
    protected boolean tryShoot(ItemStack stack, Level world, Player player, boolean main) {
        return super.tryShoot(stack, world, player, main) && getCharge(stack) == 0;
    }

    public static void setCharge(ItemStack stack, int i) {
        writeNBT(stack, "cmb_charge", i);
    }

    public static int getCharge(ItemStack stack) {
        return readNBT(stack, "cmb_charge");
    }

    /**
     * main's legacy EntityCombineBallNT: a bullet with 1000 damage that leaves a size 2 blast when it dies. the
     * repo's own EntityCombineBallNT is the reworked one, so this rides on the legacy bullet entity.
     */
    public static class CombineBall extends EntityBulletBaseNT {

        public CombineBall(Level world, int config, LivingEntity shooter) {
            super(world, config, shooter);
            overrideDamage = 1000;
        }

        @Override
        public void remove(RemovalReason reason) {
            if (!level().isClientSide() && !isRemoved() && reason.shouldDestroy())
                level().explode(getThrower(), getX(), getY(), getZ(), 2, false, Level.ExplosionInteraction.TNT);
            super.remove(reason);
        }
    }
}
