// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.backport.Nbt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's ItemGunDart: a gps dart tags the player it hits for an hour,
 * the alt fire tells you where they are, and the darter designates them for missiles.
 */
public class ItemGunDart extends ItemGunBase implements IDesignatorItem {

    public ItemGunDart(GunConfiguration config, Properties properties) {
        super(config, properties);
    }

    private static long now() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? 0 : server.overworld().getGameTime();
    }

    public static void writePlayer(ItemStack stack, Player player) {
        String name = player.getGameProfile().getName();
        long lease = player.level().getGameTime() + 60 * 60 * 20;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("player", name);
            tag.putLong("lease", lease);
        });
    }

    public static @Nullable Player readPlayer(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        var tag = data.copyTag();
        if (!tag.contains("player")) return null;
        if (Nbt.getLongOr(tag, "lease", 0) < now()) return null;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getPlayerList().getPlayerByName(Nbt.getStringOr(tag, "player", ""));
    }

    @Override
    public void startAction(ItemStack stack, Level world, Player player, boolean main) {
        if (main) {
            super.startAction(stack, world, player, main);
        } else {
            Player target = readPlayer(stack);
            if (target != null) {
                String dim = target.level().dimension().location().toString();
                int x = (int) target.getX();
                int y = (int) target.getY();
                int z = (int) target.getZ();
                int dist = (int) target.distanceTo(player);
                player.displayClientMessage(target.getDisplayName().copy().withStyle(ChatFormatting.YELLOW), false);
                player.displayClientMessage(Component.literal("Dim: " + dim + " X:" + x + " Y:" + y + " Z:" + z + " (" + dist + " blocks away)").withStyle(ChatFormatting.YELLOW), false);
            } else {
                player.displayClientMessage(Component.literal("No Target").withStyle(ChatFormatting.RED), false);
            }
        }
    }

    /** main also checked the target is in the designator's dimension; the repo's designators carry no level */
    @Override
    public boolean isReady(ItemStack stack) {
        return readPlayer(stack) != null;
    }

    @Override
    public int getTargetX(ItemStack stack) {
        Player target = readPlayer(stack);
        return target == null ? 0 : (int) target.getX();
    }

    @Override
    public int getTargetZ(ItemStack stack) {
        Player target = readPlayer(stack);
        return target == null ? 0 : (int) target.getZ();
    }
}
