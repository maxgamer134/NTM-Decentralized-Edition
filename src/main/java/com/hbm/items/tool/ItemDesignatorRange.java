// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.bomb.LaunchPad;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class ItemDesignatorRange extends ItemDesignator {

    public ItemDesignatorRange(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        // backport-fix: BF-140 main's trace, which still names a spot when aimed at the sky
        BlockHitResult bhr = com.hbm.lib.Library.rayTrace(player, 300.0D);
        if (!(level.getBlockState(bhr.getBlockPos()).getBlock() instanceof LaunchPad)) {
            if (!level.isClientSide()) {
                // Sable compat: a block on an airship is marked where the ship is now
                BlockPos pos = com.hbm.backport.SubLevelTargets.designated(level, bhr.getBlockPos());
                player.getItemInHand(hand)
                        .set(
                                ModDataComponents.TARGET_DESIGNATOR.get(),
                                IDesignatorItem.pack(pos.getX(), pos.getZ()));
                EntitySounds.play(
                        player,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.TECH_BLEEP.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F);
                player.sendSystemMessage(
                        Component.translatable(
                                "item.hbm.designator_range.set", pos.getX(), pos.getZ()));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
