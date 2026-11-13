// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.registration.OverridePoint;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.hbm.backport.TooltipDisplay;
import net.minecraft.world.level.Level;
import com.hbm.backport.compat.ItemCompat;

public class ItemOilDetector extends ItemCompat {
    public static final OverridePoint<Reading> READING =
            OverridePoint.of(Library.id("oil_detector_reading"), ItemOilDetector::scan);

    public ItemOilDetector(Properties properties) {
        super(properties);
    }

    private static boolean containsOil(Level level, int x, int z, int topY, int floorExclusive) {
        for (int y = topY; y > floorExclusive; y--) {
            if (level.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.ORE_OIL.get())) return true;
        }
        return false;
    }

    /** NTM:Space: the first oil, gas or brine reserve in the column, its key suffix, or null. */
    private static @org.jspecify.annotations.Nullable String spaceReserve(Level level, int x, int z, int topY) {
        // the planets' reserves lie deeper, down to the bedrock oil at the bottom of the world
        int floor = com.hbm.NuclearTech.MOD_ID.equals(level.dimension().location().getNamespace())
                ? level.getMinBuildHeight() : 5;
        for (int y = topY; y > floor; y--) {
            var block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
            if (block instanceof com.hbm.space.BlockSpaceOreFluid reserve) return reserve.reserveSuffix();
            if (block == ModBlocks.ORE_OIL.get()) return "";
        }
        return null;
    }

    private static Component spaceScan(Level level, int x, int y, int z) {
        String direct = spaceReserve(level, x, z, y + 15);
        String near = direct;
        int[][] offsets = {{5, 0}, {-5, 0}, {0, 5}, {0, -5}, {10, 0}, {-10, 0}, {0, 10}, {0, -10}, {5, 5}, {-5, 5}, {5, -5}, {-5, -5}};
        for (int i = 0; near == null && i < offsets.length; i++)
            near = spaceReserve(level, x + offsets[i][0], z + offsets[i][1], y + 15);
        if (direct != null)
            return Component.translatable("item.hbm.oil_detector.bullseye" + direct).withStyle(ChatFormatting.DARK_GREEN);
        if (near != null)
            return Component.translatable("item.hbm.oil_detector.detected" + near).withStyle(ChatFormatting.GOLD);
        return Component.translatable("item.hbm.oil_detector.noOil").withStyle(ChatFormatting.RED);
    }

    private static Component scan(Level level, Player player) {
        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();
        if (com.hbm.space.SpaceToggle.enabled()) return spaceScan(level, x, y, z);

        boolean direct = containsOil(level, x, z, y + 15, 5);
        boolean oil =
                containsOil(level, x + 5, z, y + 15, 5)
                        || containsOil(level, x - 5, z, y + 15, 5)
                        || containsOil(level, x, z + 5, y + 15, 5)
                        || containsOil(level, x, z - 5, y + 15, 5)
                        || containsOil(level, x + 10, z, y + 15, 10)
                        || containsOil(level, x - 10, z, y + 15, 10)
                        || containsOil(level, x, z + 10, y + 15, 10)
                        || containsOil(level, x, z - 10, y + 15, 10)
                        || containsOil(level, x + 5, z + 5, y + 15, 5)
                        || containsOil(level, x - 5, z + 5, y + 15, 5)
                        || containsOil(level, x + 5, z - 5, y + 15, 5)
                        || containsOil(level, x - 5, z - 5, y + 15, 5);

        return direct
                ? Component.translatable("item.hbm.oil_detector.bullseye")
                        .withStyle(ChatFormatting.DARK_GREEN)
                : oil
                        ? Component.translatable("item.hbm.oil_detector.detected")
                                .withStyle(ChatFormatting.GOLD)
                        : Component.translatable("item.hbm.oil_detector.noOil")
                                .withStyle(ChatFormatting.RED);
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            Component result = READING.get().read(level, player);
            PacketWire.sendTo(
                    new PlayerInformPayload(
                            result,
                            PlayerInformPayload.ID_DETONATOR,
                            PlayerInformPayload.DEFAULT_MILLIS),
                    serverPlayer);
            EntitySounds.play(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @FunctionalInterface
    public interface Reading {
        Component read(Level level, Player player);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("item.hbm.oil_detector.desc1"));
        adder.accept(Component.translatable("item.hbm.oil_detector.desc2"));
    }
}
