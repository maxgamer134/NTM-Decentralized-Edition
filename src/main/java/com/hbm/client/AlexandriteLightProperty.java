// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.backport.client.itemmodel.ItemOwner;
import com.hbm.backport.client.itemmodel.RangeSelectItemModelProperty;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import org.jspecify.annotations.Nullable;

// main: ItemAlexandrite.TextureAlexandrite, the gem's frame is the block light where the player stands
public record AlexandriteLightProperty() implements RangeSelectItemModelProperty {

    public static final MapCodec<AlexandriteLightProperty> MAP_CODEC =
            MapCodec.unit(new AlexandriteLightProperty());

    @Override
    public float get(
            ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0F;
        return player.level().getBrightness(LightLayer.BLOCK, player.blockPosition());
    }

    @Override
    public MapCodec<AlexandriteLightProperty> type() {
        return MAP_CODEC;
    }
}
