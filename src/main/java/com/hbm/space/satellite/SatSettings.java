// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.satellite;

import com.hbm.backport.InteractionResult;
import com.hbm.backport.Nbt;
import com.hbm.items.ISatChip;
import com.hbm.items.special.ItemSatellite;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.space.AstronomyUtil;
import com.hbm.space.CelestialBody;
import com.hbm.space.station.OrbitalStation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.Nullable;

/**
 * SatelliteBase's per-satellite orbit settings: inclination, altitude, phase, blink, owner and
 * colour. NTM:Space kept them in the item's NBT and copied them onto the satellite when it
 * orbited; here they are a data component of the item that Satellite.CARRIED copies onto the
 * satellite, from which the sky reads them.
 */
public record SatSettings(float inclination, float altitude, float phaseOffset, boolean blinking, float blinkPeriod,
        String owner, float r, float g, float b) {

    public static final float DEFAULT_INCLINATION = 0F;
    public static final float MIN_INCLINATION = -180F;
    public static final float MAX_INCLINATION = 180F;
    public static final float DEFAULT_ALTITUDE_KM = AstronomyUtil.DEFAULT_ALTITUDE_KM;
    public static final float MIN_ALTITUDE_KM = 80F;
    public static final float MAX_ALTITUDE_KM = 125F;
    public static final float MIN_BLINK_PERIOD = 0.3F;
    public static final float MAX_BLINK_PERIOD = 1.0F;
    public static final float DEFAULT_PHASE_OFFSET = 0F;
    public static final boolean DEFAULT_IS_BLINKING = false;
    public static final float DEFAULT_BLINK_PERIOD = MIN_BLINK_PERIOD;
    public static final String DEFAULT_OWNER = "None";

    /** The heaviest satellite that can be deployed by hand (the rest need a rocket). */
    public static final int HAND_LAUNCH_MASS = 16_000;

    /** XSatelliteRegistry's colour for each satellite, by the 1.21.1 satellite type's name. */
    public static final Map<String, float[]> COLORS = Map.ofEntries(
            Map.entry("mapper", new float[] {0.538F, 1.0F, 0.523F}),
            Map.entry("scanner", new float[] {0.544F, 0.680F, 1.0F}),
            Map.entry("radar", new float[] {0.134F, 1.0F, 0.134F}),
            Map.entry("death_ray", new float[] {0.221F, 0.663F, 1.0F}),
            Map.entry("resonator", new float[] {1.0F, 0.646F, 0.181F}),
            Map.entry("relay", new float[] {1.0F, 0.15F, 0.15F}),
            Map.entry("miner", new float[] {0.46F, 0.56F, 0.68F}),
            Map.entry("lunar_miner", new float[] {0.42F, 0.54F, 0.82F}),
            Map.entry("dyson_relay", new float[] {1.0F, 0.9F, 0.8F}),
            Map.entry("horizons", new float[] {0.0F, 0.0F, 0.0F}),
            Map.entry("war", new float[] {0.0F, 0.0F, 0.0F}),
            Map.entry("precision_laser", new float[] {0.221F, 1.0F, 0.663F}),
            Map.entry("detector", new float[] {0.1F, 0.2F, 0.3F}),
            Map.entry("ray_scan", new float[] {0.1F, 0.2F, 0.3F}),
            Map.entry("science", new float[] {0.2F, 0.64F, 0.8F}));
    public static final float[] WHITE = {1F, 1F, 1F};

    public static final Codec<SatSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("inclination", DEFAULT_INCLINATION).forGetter(SatSettings::inclination),
            Codec.FLOAT.optionalFieldOf("altitude", DEFAULT_ALTITUDE_KM).forGetter(SatSettings::altitude),
            Codec.FLOAT.optionalFieldOf("phase", DEFAULT_PHASE_OFFSET).forGetter(SatSettings::phaseOffset),
            Codec.BOOL.optionalFieldOf("blinking", DEFAULT_IS_BLINKING).forGetter(SatSettings::blinking),
            Codec.FLOAT.optionalFieldOf("blink", DEFAULT_BLINK_PERIOD).forGetter(SatSettings::blinkPeriod),
            Codec.STRING.optionalFieldOf("owner", DEFAULT_OWNER).forGetter(SatSettings::owner),
            Codec.FLOAT.optionalFieldOf("r", 1F).forGetter(SatSettings::r),
            Codec.FLOAT.optionalFieldOf("g", 1F).forGetter(SatSettings::g),
            Codec.FLOAT.optionalFieldOf("b", 1F).forGetter(SatSettings::b)).apply(i, SatSettings::new));

    public static final StreamCodec<ByteBuf, SatSettings> STREAM_CODEC = StreamCodec.of(
            (buf, s) -> s.write(buf), SatSettings::read);

    public static final RegistryHandle<DataComponentType<SatSettings>> COMPONENT = NeoForgeRegistrar.registerDataComponent(
            "sat_settings",
            DataComponentType.<SatSettings>builder().persistent(CODEC).networkSynchronized(STREAM_CODEC).build());

    /** The GUI of a satellite item's settings; set on the client. */
    public static Consumer<Player> OPEN_SCREEN = player -> {};

    // ---------------------------------------------------------------- values

    public static SatSettings defaults(String type) {
        float[] c = COLORS.getOrDefault(type, WHITE);
        return new SatSettings(DEFAULT_INCLINATION, DEFAULT_ALTITUDE_KM, DEFAULT_PHASE_OFFSET, DEFAULT_IS_BLINKING,
                DEFAULT_BLINK_PERIOD, DEFAULT_OWNER, c[0], c[1], c[2]);
    }

    /** A satellite item's settings, the type's defaults while nobody has changed them. */
    public static SatSettings of(ItemStack stack, String type) {
        SatSettings settings = stack.get(COMPONENT.get());
        return settings == null ? defaults(type) : settings;
    }

    /** What a satellite in orbit shows in the sky. */
    public static SatSettings of(Satellite satellite) {
        SatSettings settings = satellite.components.get(COMPONENT.get());
        return settings == null ? defaults(satellite.type().getSerializedName()) : settings;
    }

    public static float clampBlinkPeriod(float period) {
        return Math.max(MIN_BLINK_PERIOD, Math.min(MAX_BLINK_PERIOD, period));
    }

    public static float normalizePhaseOffset(float phase) {
        float wrapped = phase % 360F;
        if (wrapped < 0F) wrapped += 360F;
        return wrapped;
    }

    private static double altitudeOrbitSpeed(float altitude) {
        return Math.pow((double) DEFAULT_ALTITUDE_KM / Math.max(1D, altitude), 1.5D);
    }

    /** SatelliteBase.getOrbitSpeedKmPerSecond. */
    public static float orbitSpeedKmPerSecond(float altitude) {
        double radiusKm = Math.max(1D, altitude);
        double turnsPerSecond = altitudeOrbitSpeed(altitude) / 30D;
        return (float) (2D * Math.PI * radiusKm * turnsPerSecond);
    }

    public SatSettings sanitized() {
        return new SatSettings(Mth.clamp(inclination, MIN_INCLINATION, MAX_INCLINATION),
                Mth.clamp(altitude, MIN_ALTITUDE_KM, MAX_ALTITUDE_KM), normalizePhaseOffset(phaseOffset), blinking,
                clampBlinkPeriod(blinkPeriod), owner.length() > 32 ? owner.substring(0, 32) : owner,
                Mth.clamp(r, 0F, 1F), Mth.clamp(g, 0F, 1F), Mth.clamp(b, 0F, 1F));
    }

    private void write(ByteBuf buf) {
        buf.writeFloat(inclination);
        buf.writeFloat(altitude);
        buf.writeFloat(normalizePhaseOffset(phaseOffset));
        buf.writeBoolean(blinking);
        buf.writeFloat(blinkPeriod);
        ByteBufCodecs.STRING_UTF8.encode(buf, owner);
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
    }

    private static SatSettings read(ByteBuf buf) {
        float inclination = buf.readFloat();
        float altitude = buf.readFloat();
        float phase = normalizePhaseOffset(buf.readFloat());
        boolean blinking = buf.readBoolean();
        float blink = clampBlinkPeriod(buf.readFloat());
        String owner = ByteBufCodecs.STRING_UTF8.decode(buf);
        return new SatSettings(inclination, altitude, phase, blinking, blink, owner, buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public String hexColor() {
        return String.format(Locale.ROOT, "#%02X%02X%02X", channel(r), channel(g), channel(b));
    }

    public static int channel(float value) {
        return Mth.clamp(Math.round(value * 255F), 0, 255);
    }

    // ---------------------------------------------------------------- hooks

    public static void install() {
        Satellite.CARRIED.install("hbm_space", List.of(COMPONENT::get));
        ItemSatellite.DESCRIBE.install("hbm_space", SatSettings::describe);
        ItemSatellite.USE.install("hbm_space", SatSettings::use);
    }

    /** SatelliteBase.copyItemData: a satellite part keeps its settings when the rocket carries it. */
    public static void copy(ItemStack from, ItemStack to) {
        SatSettings settings = from.get(COMPONENT.get());
        if (settings != null) to.set(COMPONENT.get(), settings);
    }

    /** ItemSatellite.receiveControl: the settings screen's values, clamped. */
    public static void receive(ServerPlayer player, ItemStack stack, CompoundTag data) {
        SatelliteType type = SatelliteType.fromStack(stack);
        if (type == null) return;
        SatSettings settings = new SatSettings(
                Nbt.getFloatOr(data, "satInclination", DEFAULT_INCLINATION),
                Nbt.getFloatOr(data, "satAltitude", DEFAULT_ALTITUDE_KM),
                Nbt.getFloatOr(data, "satPhaseOffset", DEFAULT_PHASE_OFFSET),
                Nbt.getBooleanOr(data, "satIsBlinking", DEFAULT_IS_BLINKING),
                Nbt.getFloatOr(data, "satBlink", DEFAULT_BLINK_PERIOD),
                Nbt.getStringOr(data, "satOwner", DEFAULT_OWNER),
                Mth.clamp(Nbt.getIntOr(data, "satColorR", 255), 0, 255) / 255F,
                Mth.clamp(Nbt.getIntOr(data, "satColorG", 255), 0, 255) / 255F,
                Mth.clamp(Nbt.getIntOr(data, "satColorB", 255), 0, 255) / 255F).sanitized();
        stack.set(COMPONENT.get(), settings);
    }

    private static Component entry(String key, String value) {
        return Component.translatable(key).withStyle(ChatFormatting.AQUA).append(Component.literal(": " + value).withStyle(ChatFormatting.GRAY));
    }

    private static String number(float value) {
        if (value == (int) value) return Integer.toString((int) value);
        return Float.toString(value);
    }

    /** ItemSatellite.addInformation. */
    private static void describe(ItemStack stack, SatelliteType type, TooltipContext context, TooltipFlag flag,
            Consumer<Component> adder, Runnable ntm) {
        String name = type.getSerializedName();
        SatSettings s = of(stack, name);
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            ntm.run();
            adder.accept(entry("item.sat.desc.owner", s.owner()));
            adder.accept(entry("item.sat.desc.speed", number(Math.round(orbitSpeedKmPerSecond(s.altitude()) * 10F) / 10F) + "km/s"));
            adder.accept(entry("item.sat.desc.altitude", number(s.altitude()) + "km"));
            adder.accept(entry("item.sat.desc.inclination", number(s.inclination()) + "°"));
            adder.accept(entry("item.sat.desc.phase", number(Math.round(normalizePhaseOffset(s.phaseOffset()) * 10F) / 10F) + "°"));
            adder.accept(entry("item.sat.desc.color", s.hexColor()));
        } else {
            adder.accept(Component.literal("Hold <").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                    .append(Component.literal("LSHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                    .append(Component.literal("> to display more info").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }

        String desc = switch (name) {
            case "death_ray" -> "satchip.laser";
            case "mapper" -> "satchip.mapper";
            case "miner" -> "satchip.miner";
            case "lunar_miner" -> "satchip.lunar_miner";
            case "radar" -> "satchip.radar";
            case "resonator" -> "satchip.resonator";
            case "scanner" -> "satchip.scanner";
            case "dyson_relay" -> "item.sat.desc.dyson_relay";
            default -> null;
        };
        if (desc != null) adder.accept(Component.translatable(desc));

        if (canLaunchByHand(stack)) {
            adder.accept(Component.translatable("item.sat.desc.launch_by_hand").withStyle(ChatFormatting.GOLD));
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && CelestialBody.inOrbit(player.level()))
                adder.accept(Component.translatable("item.sat.desc.deploy_orbit").withStyle(ChatFormatting.BOLD));
        }
    }

    private static boolean canLaunchByHand(ItemStack stack) {
        return SpaceSatellites.mass(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()) <= HAND_LAUNCH_MASS;
    }

    /** ItemSatellite.onItemRightClick: sneaking opens the settings, in orbit a light satellite is deployed by hand. */
    private static InteractionResult use(ItemStack stack, @Nullable SatelliteType type, Level level, Player player, InteractionHand hand) {
        if (type == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (level.isClientSide() && hand == InteractionHand.MAIN_HAND) OPEN_SCREEN.accept(player);
            return InteractionResult.CONSUME;
        }
        if (!canLaunchByHand(stack) || !CelestialBody.inOrbit(level)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        OrbitalStation station = OrbitalStation.getStationFromPosition(server, player.getBlockX(), player.getBlockZ());
        if (station == null || station.orbiting == null || station.orbiting.dimension == null) return InteractionResult.PASS;
        ServerLevel target = server.getServer().getLevel(station.orbiting.dimension);
        if (target == null) return InteractionResult.PASS;

        Satellite.orbit(target, stack, ISatChip.getFreqS(stack), player.getX(), player.getY(), player.getZ());
        player.sendSystemMessage(Component.literal("Satellite launched successfully!"));
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
