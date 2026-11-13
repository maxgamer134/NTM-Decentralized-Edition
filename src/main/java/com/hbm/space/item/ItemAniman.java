// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.item;

import com.hbm.items.special.ItemCustomLore;
import com.hbm.packet.toclient.VomitPayload;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The Problematic Meal: the treatment gives Run and the Hunger, this adds the vomit sound and particles. */
public class ItemAniman extends ItemCustomLore {

    public ItemAniman(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (level instanceof ServerLevel) {
            ParticleCreators.vomit(level, entity, VomitPayload.MODE_NORMAL, 25);
            EntitySounds.play(entity, entity.getX(), entity.getY(), entity.getZ(),
                    ModSounds.PLAYER_VOMIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return rest;
    }
}
