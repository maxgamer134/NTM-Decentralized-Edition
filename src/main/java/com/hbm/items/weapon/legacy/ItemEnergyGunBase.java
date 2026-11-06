// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.backport.Nbt;
import com.hbm.backport.TooltipDisplay;
import com.hbm.data.ItemData;
import com.hbm.items.weapon.GunContent;
import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's ItemEnergyGunBase: guns that run on HE instead of ammo. each shot
 * draws the mode's discharge, alt fire cycles through the modes, and any HE charger fills them like a battery.
 * main's crosshair override stays with the shared legacy gun hud.
 */
public class ItemEnergyGunBase extends ItemGunBase implements IBatteryItem {

    public ItemEnergyGunBase(GunConfiguration config, Properties properties) {
        super(config, properties);
    }

    public ItemEnergyGunBase(GunConfiguration config, GunConfiguration alt, Properties properties) {
        super(config, alt, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> list, TooltipFlag flag) {
        list.accept(Component.literal("Energy Stored: " + BobMathUtil.getShortNumber(getCharge(stack)) + "/"
                + BobMathUtil.getShortNumber(mainConfig.maxCharge) + "HE"));
        list.accept(Component.literal("Charge rate: " + BobMathUtil.getShortNumber(mainConfig.chargeRate) + "HE/t"));
        addAdditionalInformation(stack, list);
    }

    @Override
    protected void updateServer(ItemStack stack, Level world, Player player, boolean isCurrentItem) {
        if (getDelay(stack) > 0 && isCurrentItem) setDelay(stack, getDelay(stack) - 1);
        if (getIsMouseDown(stack) && player.getMainHandItem() != stack) setIsMouseDown(stack, false);
        if (getIsAltDown(stack) && !isCurrentItem) setIsAltDown(stack, false);
        if (ItemData.ENABLE_GUNS.get()
                && !GunContent.isDisabled(this)
                && mainConfig.firingMode == 1
                && getIsMouseDown(stack)
                && tryShoot(stack, world, player, isCurrentItem)) {
            fire(stack, world, player);
            setDelay(stack, getConfig(stack).firingRate);
        }
    }

    @Override
    protected boolean tryShoot(ItemStack stack, Level world, Player player, boolean main) {
        if (main && getDelay(stack) == 0) return getConfig(stack).dischargePerShot <= getCharge(stack);
        return false;
    }

    @Override
    protected void fire(ItemStack stack, Level world, Player player) {
        BulletConfiguration config = getConfig(stack);
        int bullets = config.bulletsMin;
        for (int k = 0; k < mainConfig.roundsPerCycle; k++) {
            if (config.bulletsMax > config.bulletsMin) bullets += world.random.nextInt(config.bulletsMax - config.bulletsMin);
            for (int i = 0; i < bullets; i++) spawnProjectile(world, player, stack, BulletConfigSyncingUtil.getKey(config));
            playAnimation(player, stack, AnimType.CYCLE);
            setCharge(stack, getCharge(stack) - config.dischargePerShot);
        }
        LegacySounds.play(player, mainConfig.firingSound, 1.0F, mainConfig.firingPitch);
    }

    @Override
    public void startAction(ItemStack stack, Level world, Player player, boolean main) {
        if (mainConfig.firingMode == GunConfiguration.FIRE_MANUAL && main && tryShoot(stack, world, player, main)) {
            fire(stack, world, player);
            setDelay(stack, mainConfig.rateOfFire);
        }
        if (!main) {
            int mode = getMode(stack) + 1;
            if (mode >= mainConfig.config.size()) mode = 0;
            int m = mode;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putByte("mode", (byte) m));
            if (!world.isClientSide()) {
                BulletConfiguration config = BulletConfigSyncingUtil.pullConfig(mainConfig.config.get(mode));
                player.displayClientMessage(Component.translatable("weapon.elecGun.modeChange").withStyle(ChatFormatting.WHITE)
                        .append(" ").append(Component.translatable(config.modeName).withStyle(config.chatColour)), false);
            }
        }
    }

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static int getMode(ItemStack stack) {
        return Nbt.getByteOr(tag(stack), "mode", (byte) 0);
    }

    public BulletConfiguration getConfig(ItemStack stack) {
        return BulletConfigSyncingUtil.pullConfig(mainConfig.config.get(getMode(stack)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCharge(stack) / Math.max(1, mainConfig.maxCharge));
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {
        setCharge(stack, getCharge(stack) + i);
    }

    @Override
    public void setCharge(ItemStack stack, long i) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong("charge", i));
    }

    @Override
    public void dischargeBattery(ItemStack stack, long i) {
        setCharge(stack, getCharge(stack) - i);
    }

    /** main filled a gun with no tag to the brim (the creative tab one comes full) */
    @Override
    public long getCharge(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag.contains("charge") ? Nbt.getLongOr(tag, "charge", 0) : mainConfig.maxCharge;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return mainConfig.maxCharge;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return mainConfig.chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return 0;
    }
}
