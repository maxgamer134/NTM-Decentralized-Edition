// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.rocket;

import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.rocket.EntityRideableRocket.RocketState;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import com.hbm.backport.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A hard drive holding a flight plan to one body (or to an orbital station): where to land, and
 * whether it's been processed for the trip. Used on the ground of its body it sets the landing
 * spot; used while seated in a rocket it loads the plan into the rocket.
 */
public class ItemVOTVdrive extends com.hbm.backport.compat.ItemCompat {

    public final SolarSystem.Body body;

    public ItemVOTVdrive(Properties props, SolarSystem.Body body) {
        super(props);
        this.body = body;
    }

    public record Destination(SolarSystem.Body body, int x, int z) {}

    /** Where a drive takes a rocket: the body, whether to its orbit, and whether that's possible yet. */
    public record Target(@Nullable CelestialBody body, boolean inOrbit, boolean isValid) {}

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void edit(ItemStack stack, Consumer<CompoundTag> edit) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, edit);
    }

    public static SolarSystem.Body getBody(ItemStack stack) {
        return stack.getItem() instanceof ItemVOTVdrive drive ? drive.body : SolarSystem.Body.ORBIT;
    }

    public static @Nullable Destination getDestination(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemVOTVdrive)) return null;
        CompoundTag tag = tag(stack);
        return new Destination(getBody(stack), tag.getInt("x"), tag.getInt("z"));
    }

    public static Target getTarget(ItemStack stack, Level level) {
        Destination destination = getDestination(stack);
        if (destination == null) return new Target(null, false, false);
        if (destination.body() == SolarSystem.Body.ORBIT) {
            CompoundTag tag = tag(stack);
            if (!(level instanceof net.minecraft.server.level.ServerLevel server))
                return new Target(CelestialBody.getBody(tag.getString("sBody")), true, tag.getBoolean("sHas"));
            com.hbm.space.station.OrbitalStation station = com.hbm.space.station.OrbitalStation.addStation(
                    com.hbm.space.SolarSystemSavedData.get(server), destination.x(), destination.z(), CelestialBody.getBody(level));
            if (!station.hasStation) station.orbiting = CelestialBody.getBody(level);
            // the client can't look stations up, so the drive carries what the server saw
            if (!station.name.equals(tag.getString("stationName")) || !station.orbiting.name.equals(tag.getString("sBody"))
                    || station.hasStation != tag.getBoolean("sHas"))
                edit(stack, t -> {
                    t.putString("stationName", station.name);
                    t.putString("sBody", station.orbiting.name);
                    t.putBoolean("sHas", station.hasStation);
                });
            return new Target(station.orbiting, true, station.hasStation);
        }
        return new Target(destination.body().getBody(), false, true);
    }

    public static void setCoordinates(ItemStack stack, int x, int z) {
        edit(stack, t -> {
            t.putInt("x", x);
            t.putInt("z", z);
        });
    }

    public static boolean getProcessed(ItemStack stack) {
        return tag(stack).getBoolean("Processed");
    }

    public static void setProcessed(ItemStack stack, boolean processed) {
        edit(stack, t -> t.putBoolean("Processed", processed));
    }

    public static int getProcessingTier(ItemStack stack, CelestialBody from) {
        return getBody(stack).getProcessingLevel(from);
    }

    public static void markCopied(ItemStack stack) {
        edit(stack, t -> t.putBoolean("copied", true));
    }

    public static boolean wasCopied(ItemStack stack) {
        return tag(stack).getBoolean("copied");
    }

    /** A drive ready to fly, as the creative tab hands them out. */
    public static ItemStack processed(Item drive) {
        ItemStack stack = new ItemStack(drive);
        edit(stack, t -> {
            t.putInt("x", 1);
            t.putInt("ax", 1);
            t.putBoolean("Processed", true);
        });
        return stack;
    }

    private static boolean onBody(Level level, SolarSystem.Body body) {
        return body != SolarSystem.Body.ORBIT && level.dimension() == body.getDimension();
    }

    @Override
    public InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean processed = getProcessed(stack);
        // on the body itself, or in creative, a drive processes at once
        if (!processed && (player.isCreative() || onBody(level, body))) {
            setProcessed(stack, true);
            processed = true;
        }
        if (processed && player.getVehicle() instanceof EntityRideableRocket rocket) {
            if ((!rocket.getRocket().stages.isEmpty() || CelestialBody.inOrbit(level) || rocket.isReusable())
                    && (rocket.getState() == RocketState.LANDED || rocket.getState() == RocketState.AWAITING)) {
                ItemStack old = rocket.navDrive;
                rocket.navDrive = stack.copyWithCount(1);
                player.setItemInHand(hand, old);
                if (!level.isClientSide()) rocket.setState(RocketState.AWAITING);
                level.playSound(null, player.blockPosition(), com.hbm.sound.ModSounds.UPGRADE_PLUG.get(), SoundSource.PLAYERS, 1F, 1F);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult backport$useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        int x = context.getClickedPos().getX(), z = context.getClickedPos().getZ();
        if (body == SolarSystem.Body.ORBIT) {
            // a station drive used on the ground calls a docked pod down to it
            if (CelestialBody.inOrbit(context.getLevel())) return InteractionResult.PASS;
            if (context.getLevel() instanceof net.minecraft.server.level.ServerLevel server && context.getPlayer() != null) {
                Destination destination = getDestination(stack);
                com.hbm.space.station.OrbitalStation station = com.hbm.space.station.OrbitalStation.addStation(
                        com.hbm.space.SolarSystemSavedData.get(server), destination.x(), destination.z(), CelestialBody.getBody(server));
                SolarSystem.Body here = CelestialBody.getEnum(server);
                boolean recalled = here != null && station.recallPod(new Destination(here, x, z));
                context.getPlayer().sendSystemMessage(Component.literal(recalled ? "Recalling drop pod to coordinates: " + x + ", " + z
                        : "Could not recall drop pod from station!").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            }
            return InteractionResult.SUCCESS;
        }
        if (!onBody(context.getLevel(), body)) return InteractionResult.PASS;
        setCoordinates(stack, x, z);
        setProcessed(stack, true);
        if (!context.getLevel().isClientSide() && context.getPlayer() != null)
            context.getPlayer().sendSystemMessage(Component.literal("Set landing coordinates to: " + x + ", " + z)
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> adder, TooltipFlag flag) {
        Destination destination = getDestination(stack);
        if (destination == null) return;
        if (body == SolarSystem.Body.ORBIT) {
            adder.accept(Component.literal("Destination: ORBITAL STATION"));
            String name = tag(stack).getString("stationName");
            // 1.7's ChunkCoordIntPair.hashCode, which names a station that has none yet
            if (name.isEmpty()) name = "0x" + Integer.toHexString((1664525 * destination.x() + 1013904223) ^ (1664525 * (destination.z() ^ -559038737) + 1013904223)).toUpperCase();
            adder.accept(Component.literal("Station: " + name).withStyle(ChatFormatting.AQUA));
            Level level = context.level();
            if (level != null && !CelestialBody.inOrbit(level)) {
                for (String line : Component.translatable("item.hbm.hard_drive_full.orbit.desc").getString().split("\\$"))
                    adder.accept(Component.literal(line).withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        adder.accept(Component.literal("Destination: ").append(Component.translatable("body." + body.name).withStyle(ChatFormatting.AQUA)));
        if (destination.x() == 0 && destination.z() == 0) {
            adder.accept(Component.literal("Needs destination coordinates!").withStyle(ChatFormatting.GOLD));
        } else if (!getProcessed(stack)) {
            Level level = context.level();
            if (level != null) adder.accept(Component.literal("Process requirement: Level " + body.getProcessingLevel(CelestialBody.getBody(level))));
            adder.accept(Component.literal("Needs processing!").withStyle(ChatFormatting.GOLD));
            adder.accept(Component.literal("Target coordinates: " + destination.x() + ", " + destination.z()));
        } else {
            adder.accept(Component.literal("Processed!").withStyle(ChatFormatting.GREEN));
            adder.accept(Component.literal("Target coordinates: " + destination.x() + ", " + destination.z()));
        }
        if (context.level() != null && onBody(context.level(), body)) {
            for (String line : Component.translatable("item.hbm.hard_drive_full.surface.desc").getString().split("\\$"))
                adder.accept(Component.literal(line).withStyle(ChatFormatting.GOLD));
        }
    }
}
