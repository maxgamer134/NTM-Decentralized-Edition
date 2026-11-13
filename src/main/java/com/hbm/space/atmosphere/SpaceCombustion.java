// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.atmosphere;

import com.hbm.space.SpaceToggle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Other mods' fuel burners (Create's blaze burners, Aeronautics' hot air burner and portable
 * engine, Propulsion's solid and liquid burners) need oxygen to burn in, as NTM:Space's furnace:
 * from the body's atmosphere or a sealed room. Thrusters are left alone.
 */
public final class SpaceCombustion {
    private static final ResourceLocation LIT_BLAZE_BURNER =
            ResourceLocation.fromNamespaceAndPath("create", "lit_blaze_burner");
    private static final ResourceLocation BLAZE_BURNER =
            ResourceLocation.fromNamespaceAndPath("create", "blaze_burner");

    private SpaceCombustion() {}

    /** False only on the server, with Space's changes on, where there is no oxygen to burn in. */
    public static boolean canBurn(Level level, BlockPos pos) {
        if (!SpaceToggle.changes() || !(level instanceof ServerLevel server)) return true;
        return SpaceAtmosphere.breatheAir(server, pos, 0);
    }

    /** True when a once-a-second check finds no oxygen; false on the other ticks and the client. */
    public static boolean outOfAir(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide || (level.getGameTime() + be.getBlockPos().asLong()) % 20 != 0)
            return false;
        return !canBurn(level, be.getBlockPos());
    }

    /** Create's flint-lit blaze burner, which burns like a campfire. */
    public static boolean isLitBurner(Block block) {
        return block != Blocks.AIR && LIT_BLAZE_BURNER.equals(BuiltInRegistries.BLOCK.getKey(block));
    }

    /** The empty blaze burner a lit one goes back to when its flame goes out. */
    public static BlockState emptyBurner() {
        BlockState state = BuiltInRegistries.BLOCK.get(BLAZE_BURNER).defaultBlockState();
        Property<?> heat = state.getBlock().getStateDefinition().getProperty("blaze");
        return heat == null ? state : with(state, heat, "none");
    }

    private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, String name) {
        return property.getValue(name).map(v -> state.setValue(property, v)).orElse(state);
    }
}
