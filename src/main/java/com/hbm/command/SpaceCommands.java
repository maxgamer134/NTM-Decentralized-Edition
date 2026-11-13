// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.command;

import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.SolarSystemSavedData;
import com.hbm.space.SpaceDimensions;
import com.hbm.space.SpaceToggle;
import com.hbm.space.rocket.ItemVOTVdrive;
import com.hbm.space.rocket.SpaceRockets;
import com.hbm.space.station.OrbitalStation;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;

/**
 * NTM:Space's commands: {@code /ntmstations} (launch, tp, list and fetch stations by the held
 * drive) and {@code /totaltime} (set or add to every world's total time, which the planets'
 * orbits run on). Registered only with Space on.
 */
public final class SpaceCommands {
    private SpaceCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!SpaceToggle.enabled()) return;
        dispatcher.register(Commands.literal("ntmstations")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("launch").executes(SpaceCommands::launch))
                .then(Commands.literal("tp").executes(SpaceCommands::teleport))
                .then(Commands.literal("list").executes(SpaceCommands::list))
                .then(Commands.literal("fetch")
                        .executes(ctx -> fail(ctx.getSource(), "commands.station.invalid_station"))
                        .then(Commands.argument("station", StringArgumentType.greedyString())
                                .executes(SpaceCommands::fetch))));
        dispatcher.register(Commands.literal("totaltime")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("set").then(Commands.argument("time", IntegerArgumentType.integer(0))
                        .executes(ctx -> setTime(ctx, false))))
                .then(Commands.literal("add").then(Commands.argument("time", IntegerArgumentType.integer(0))
                        .executes(ctx -> setTime(ctx, true)))));
    }

    /** A station's ID as NTM:Space shows it: the hash its 1.7 chunk pair had, in hex. */
    public static String stationId(OrbitalStation station) {
        int hash = (1664525 * station.dX + 1013904223) ^ (1664525 * (station.dZ ^ -559038737) + 1013904223);
        return "0x" + Integer.toHexString(hash).toUpperCase(Locale.ROOT);
    }

    private static int launch(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = player(ctx);
        if (player == null) return 0;
        ItemVOTVdrive.Destination dest = stationDrive(ctx.getSource(), player);
        if (dest == null) return 0;
        OrbitalStation.markLaunched(SolarSystemSavedData.get(player.serverLevel()), dest.x(), dest.z(),
                CelestialBody.getBody(player.level()));
        return ok(ctx.getSource(), Component.translatable("commands.station.launched"));
    }

    private static int teleport(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = player(ctx);
        if (player == null) return 0;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemVOTVdrive)) return fail(ctx.getSource(), "commands.station.invalid_drive");
        ItemVOTVdrive.Destination dest = ItemVOTVdrive.getDestination(held);
        ServerLevel target = player.server.getLevel(dest.body().getDimension());
        if (target == null) return fail(ctx.getSource(), "commands.station.invalid_drive");
        player.stopRiding();
        if (dest.body() == SolarSystem.Body.ORBIT) {
            int x = OrbitalStation.centre(dest.x()), z = OrbitalStation.centre(dest.z());
            player.teleportTo(target, x + 0.5D, 130.0D, z + 0.5D, player.getYRot(), player.getXRot());
            OrbitalStation.spawn(target, x, z);
        } else {
            int y = target == player.level() ? target.getHeight(Heightmap.Types.MOTION_BLOCKING, dest.x(), dest.z()) + 1 : 300;
            player.teleportTo(target, dest.x() + 0.5D, y, dest.z() + 0.5D, player.getYRot(), player.getXRot());
        }
        return ok(ctx.getSource(), Component.translatable("commands.station.teleported"));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        int found = 0;
        for (OrbitalStation station : SolarSystemSavedData.get(ctx.getSource().getLevel()).stations().values()) {
            if (!station.hasStation) continue;
            String text = stationId(station) + (station.name.isEmpty() ? "" : " - " + station.name);
            ok(ctx.getSource(), Component.literal(text));
            found++;
        }
        return found > 0 ? found : fail(ctx.getSource(), "commands.station.no_stations");
    }

    private static int fetch(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = player(ctx);
        if (player == null) return 0;
        String match = StringArgumentType.getString(ctx, "station").trim();
        if (match.isEmpty()) return fail(ctx.getSource(), "commands.station.invalid_station");
        for (OrbitalStation station : SolarSystemSavedData.get(player.serverLevel()).stations().values()) {
            if (!station.name.trim().equalsIgnoreCase(match) && !stationId(station).equalsIgnoreCase(match)) continue;
            ItemStack drive = new ItemStack(SpaceRockets.DRIVES.get(SolarSystem.Body.ORBIT).get());
            CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, drive, (CompoundTag tag) -> {
                tag.putInt("x", station.dX);
                tag.putInt("z", station.dZ);
                tag.putBoolean("Processed", true);
                tag.putString("stationName", station.name);
            });
            if (!player.getInventory().add(drive)) player.drop(drive, false);
            return ok(ctx.getSource(), Component.translatable("commands.station.drive_created"));
        }
        return fail(ctx.getSource(), "commands.station.no_match");
    }

    private static int setTime(CommandContext<CommandSourceStack> ctx, boolean add) {
        int time = IntegerArgumentType.getInteger(ctx, "time");
        for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
            if (level.getLevelData() instanceof ServerLevelData data)
                data.setGameTime(add ? data.getGameTime() + time : time);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(add ? "commands.time.added" : "commands.time.set", time), true);
        return 1;
    }

    public enum Weather { CLEAR, RAIN, THUNDER }

    /** {@link #planetWeather}'s answer for the overworld and other mods' levels: vanilla's /weather runs. */
    public static final int NOT_A_BODY = Integer.MIN_VALUE;

    /**
     * ModEventHandler.handlePlanetaryWeatherCommand: /weather on another body, or in orbit (the body
     * the station circles), sets that body's weather. With no duration, 300 to 900 seconds.
     */
    public static int planetWeather(CommandSourceStack source, Weather weather, int time) {
        if (!SpaceToggle.enabled()) return NOT_A_BODY;
        ServerLevel level = source.getLevel();
        CelestialBody body;
        if (CelestialBody.inOrbit(level)) {
            var pos = source.getPosition();
            body = OrbitalStation.getStationFromPosition(level, (int) Math.floor(pos.x), (int) Math.floor(pos.z)).orbiting;
        } else {
            if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) return NOT_A_BODY;
            body = CelestialBody.getBodyOrNull(level.dimension());
            if (body == null) return NOT_A_BODY;
        }
        var trait = com.hbm.space.trait.CBT_Weather.ensureTrait(body);
        if (trait == null || !com.hbm.space.trait.CBT_Weather.supportsWeather(body)) {
            source.sendFailure(Component.translatable("commands.weather.no_cycle"));
            return 0;
        }
        int duration = time == -1 ? (300 + level.random.nextInt(600)) * 20 : time;
        switch (weather) {
            case CLEAR -> trait.forceClear(level.random, duration);
            case RAIN -> trait.forceRain(level.random, duration);
            case THUNDER -> trait.forceThunder(duration);
        }
        SolarSystemSavedData.get(level).changed();
        String key = switch (weather) {
            case CLEAR -> "commands.weather.set.clear";
            case RAIN -> "commands.weather.set.rain";
            case THUNDER -> "commands.weather.set.thunder";
        };
        source.sendSuccess(() -> Component.translatable(key), true);
        return duration;
    }

    private static ItemVOTVdrive.@org.jspecify.annotations.Nullable Destination stationDrive(CommandSourceStack source, ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemVOTVdrive) || ItemVOTVdrive.getBody(held) != SolarSystem.Body.ORBIT) {
            fail(source, "commands.station.invalid_drive");
            return null;
        }
        return ItemVOTVdrive.getDestination(held);
    }

    private static @org.jspecify.annotations.Nullable ServerPlayer player(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) return player;
        fail(ctx.getSource(), "commands.satellite.should_be_run_as_player");
        return null;
    }

    private static int ok(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message.copy().withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int fail(CommandSourceStack source, String key) {
        source.sendFailure(Component.translatable(key).withStyle(ChatFormatting.RED));
        return 0;
    }
}
