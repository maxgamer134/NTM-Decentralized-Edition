// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.client;

import com.hbm.interfaces.ILookOverlay.LookInfo;
import com.hbm.space.CelestialBody;
import com.hbm.space.rocket.EntityRideableRocket;
import com.hbm.space.rocket.EntityRideableRocket.RocketState;
import com.hbm.space.rocket.ItemVOTVdrive;
import com.hbm.space.rocket.ItemVOTVdrive.Target;
import com.hbm.space.rocket.RocketStruct;
import com.hbm.space.station.OrbitalStation;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** NTM:Space's rocket printHook: where it's going, whether it can, and how to get in, launch or swap drives. */
public final class RocketLookOverlay {

    private RocketLookOverlay() {}

    public static void build(EntityRideableRocket entity, LookInfo info) {
        RocketState state = entity.getState();
        if (state == RocketState.LAUNCHING || state == RocketState.LANDING || state == RocketState.TIPPING
                || state == RocketState.DOCKING || state == RocketState.UNDOCKING) return;
        if (state == RocketState.TRANSFER) {
            info.title("Rocket", 0xffff00, 0x404000);
            OrbitalStation station = OrbitalStation.clientStation;
            info.line(Component.translatable("station.travelling").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(": ").withStyle(ChatFormatting.AQUA))
                    .append(Component.translatable("body." + station.target.name)));
            info.line(Component.translatable("station.progress").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(": ").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(Math.round(station.getUnscaledProgress(0) * 100) + "%")));
            return;
        }

        RocketStruct rocket = entity.getRocket();
        if (rocket.stages.isEmpty() && !CelestialBody.inOrbit(entity.level()) && !entity.isReusable()) return;
        info.title("Rocket", 0xffff00, 0x404000);

        Player player = Minecraft.getInstance().player;
        Target from = entity.here();
        Target to = entity.getTarget();
        boolean canLaunch = to.body() != null && state == RocketState.AWAITING;

        if (state == RocketState.NEEDSFUEL) {
            info.line("Rocket has no fuel!", 0xff5555);
        } else if (canLaunch && !rocket.hasSufficientFuel(from.body(), to.body(), from.inOrbit(), to.inOrbit())) {
            info.line("Rocket can't reach destination!", 0xff5555);
            canLaunch = false;
        }

        if (entity.getFirstPassenger() == null) {
            info.line("Interact to enter");
        } else if (entity.getFirstPassenger() != player) {
            info.line("OCCUPIED");
        } else {
            if (to.inOrbit()) info.line("Destination: ORBITAL STATION");
            else if (to.body() != null)
                info.line(Component.literal("Destination: ").append(Component.translatable("body." + to.body().name)));
            else info.line("Destination: NO DRIVE INSTALLED");

            if (canLaunch) info.line("JUMP TO LAUNCH");
            else if (state == RocketState.LANDED) info.line("Insert next drive to continue");

            ItemStack held = player != null ? player.getMainHandItem() : ItemStack.EMPTY;
            if ((state == RocketState.LANDED || state == RocketState.AWAITING)
                    && held.getItem() instanceof ItemVOTVdrive && ItemVOTVdrive.getProcessed(held))
                info.line("Interact to swap drive");
        }
    }
}
