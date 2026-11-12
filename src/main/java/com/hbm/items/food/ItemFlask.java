// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.extprop.HbmPlayerProps;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import com.hbm.backport.item.food.Consumable;
import net.minecraft.world.level.Level;
import com.hbm.backport.compat.ItemCompat;
import com.hbm.backport.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public class ItemFlask extends ItemCompat {

    public static final Consumable CONSUMABLE =
            Consumable.builder()
                    .consumeSeconds(1.6F)
                    .animation(UseAnim.DRINK)
                    .sound(SoundEvents.GENERIC_DRINK)
                    .hasConsumeParticles(false)
                    .build();

    public final EnumInfusion type;

    public ItemFlask(Properties properties, EnumInfusion type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        if (type == EnumInfusion.SHIELD
                && HbmPlayerProps.getData(player).maxShield >= HbmPlayerProps.shieldCap) {
            return InteractionResult.PASS;
        }
        return super.backport$use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof Player player && type == EnumInfusion.NITAN) {
            // NTM:Space: three minutes of Ambrosia and Resistance IV; the third flask kills
            HbmPlayerProps.getData(player).nitanCount++;
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.hbm.potion.HbmPotion.nitan(), 60 * 20 * 3, 0));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 60 * 20 * 3, 3));
        }
        if (!level.isClientSide()
                && entity instanceof Player player
                && type == EnumInfusion.SHIELD) {
            HbmPlayerProps props = HbmPlayerProps.getData(player);
            props.maxShield = Math.min(HbmPlayerProps.shieldCap, props.maxShield + 5F);
            props.shield = Math.min(props.shield + 5F, props.getEffectiveMaxShield(player));
        }
        return result;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        String key = getDescriptionId() + ".desc";
        Component desc = Component.translatableWithFallback(key, "");
        if (!desc.getString().isEmpty()) adder.accept(desc);
    }

    public enum EnumInfusion {
        SHIELD,
        NITAN
    }
}
