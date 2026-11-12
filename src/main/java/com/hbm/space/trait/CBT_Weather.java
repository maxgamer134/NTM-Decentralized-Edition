// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystemSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * A planet's own rain and storms: only bodies with thick enough air and a water cycle have them,
 * and lightning grows more common the denser the air. Planets otherwise shared the overworld's
 * weather.
 */
public class CBT_Weather extends CelestialBodyTrait {

    private static final RandomSource WEATHER_RANDOM = RandomSource.create();
    private static final int SAVE_INTERVAL = 200;
    private static final float LIGHTNING_CLOUD_PRESSURE = 0.5F;
    private static final float LIGHTNING_HAZE_PRESSURE = 3.0F;
    private static final float LIGHTNING_OPAQUE_PRESSURE = 5.0F;
    private static final float CLOUD_LIGHTNING_ACTIVITY = 0.55F;
    private static final float HAZE_LIGHTNING_ACTIVITY = 0.82F;
    private static final int[][] LIGHTNING_BIOME_SAMPLES = {{0, 0}, {512, 0}, {-512, 0}, {0, 512}, {0, -512}};

    public boolean raining;
    public boolean thundering;
    public boolean canSpawnLightning = true;
    public int rainTime;
    public int thunderTime;
    public float prevRainStrength;
    public float rainStrength;
    public float prevThunderStrength;
    public float thunderStrength;

    private long lastUpdateTick = Long.MIN_VALUE;

    public static boolean supportsWeather(@Nullable CelestialBody body) {
        if (body == null || body.gas != null) return false;
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        CBT_Water water = body.getTrait(CBT_Water.class);
        return atmosphere != null && atmosphere.getPressure() > 0.5D && water != null && water.fluid != null;
    }

    /** The body's weather, added on the server the first time a body that can have weather asks. */
    public static @Nullable CBT_Weather ensureTrait(@Nullable CelestialBody body) {
        if (body == null) return null;
        CBT_Weather weather = body.getTrait(CBT_Weather.class);
        if (weather == null && supportsWeather(body)) {
            body.modifyTraits(new CBT_Weather());
            weather = body.getTrait(CBT_Weather.class);
        }
        return weather;
    }

    public static void updateGlobalWeather(MinecraftServer server) {
        long tick = server.getTickCount();
        boolean dirty = false;
        for (CelestialBody body : CelestialBody.getAllBodies()) {
            if (body.dimension == null) continue;
            CBT_Weather weather = ensureTrait(body);
            if (weather == null) continue;
            ServerLevel level = server.getLevel(body.dimension);
            if (weather.updateForTick(tick, level != null ? level.random : WEATHER_RANDOM, body, level)) dirty = true;
        }
        if (dirty) SolarSystemSavedData.get(server.overworld()).changed();
    }

    /**
     * Takes a planet's weather from its trait in place of vanilla's cycle, which planets only
     * mirrored from the overworld; sends the change to the players there as vanilla does.
     */
    public static void applyTo(ServerLevel level, CelestialBody body) {
        CBT_Weather weather = body.getTrait(CBT_Weather.class);
        float rain = weather == null ? 0 : weather.rainStrength;
        float thunder = weather == null ? 0 : weather.thunderStrength;
        boolean wasRaining = level.isRaining();
        float oldRain = level.getRainLevel(1), oldThunder = level.getThunderLevel(1);
        level.oRainLevel = oldRain;
        level.oThunderLevel = oldThunder;
        level.setRainLevel(rain);
        level.setThunderLevel(thunder);
        var players = level.getServer().getPlayerList();
        if (oldRain != rain)
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rain), level.dimension());
        if (oldThunder != thunder)
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunder), level.dimension());
        if (wasRaining != level.isRaining())
            players.broadcastAll(new ClientboundGameEventPacket(wasRaining ? ClientboundGameEventPacket.STOP_RAINING
                    : ClientboundGameEventPacket.START_RAINING, 0), level.dimension());
    }

    private static float getAtmospherePressure(CelestialBody body) {
        if (body.gas != null) return 0;
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        return atmosphere != null ? Math.max(0, (float) atmosphere.getPressure()) : 0;
    }

    public static float getLightningSeverity(CelestialBody body) {
        if (!supportsWeather(body)) return 0;
        float pressure = getAtmospherePressure(body);
        if (pressure > LIGHTNING_HAZE_PRESSURE) {
            float haze = Mth.clamp((pressure - LIGHTNING_HAZE_PRESSURE) / (LIGHTNING_OPAQUE_PRESSURE - LIGHTNING_HAZE_PRESSURE), 0, 1);
            return Mth.clamp(0.65F + haze * 0.35F, 0.65F, 1);
        }
        return Mth.clamp((pressure - LIGHTNING_CLOUD_PRESSURE) / (LIGHTNING_HAZE_PRESSURE - LIGHTNING_CLOUD_PRESSURE) * 0.65F, 0, 0.65F);
    }

    public static float getLightningActivityFactor(CelestialBody body) {
        if (!supportsWeather(body)) return 0;
        float pressure = getAtmospherePressure(body);
        if (pressure > LIGHTNING_HAZE_PRESSURE) {
            float haze = Mth.clamp((pressure - LIGHTNING_HAZE_PRESSURE) / (LIGHTNING_OPAQUE_PRESSURE - LIGHTNING_HAZE_PRESSURE), 0, 1);
            return Mth.clamp(HAZE_LIGHTNING_ACTIVITY + haze * (1 - HAZE_LIGHTNING_ACTIVITY), HAZE_LIGHTNING_ACTIVITY, 1);
        }
        float cloud = Mth.clamp((pressure - LIGHTNING_CLOUD_PRESSURE) / (LIGHTNING_HAZE_PRESSURE - LIGHTNING_CLOUD_PRESSURE), 0, 1);
        return Mth.clamp(CLOUD_LIGHTNING_ACTIVITY + cloud * (HAZE_LIGHTNING_ACTIVITY - CLOUD_LIGHTNING_ACTIVITY),
                CLOUD_LIGHTNING_ACTIVITY, HAZE_LIGHTNING_ACTIVITY);
    }

    private static int getStormDuration(RandomSource rand, float severity) {
        return Math.max(1200, Mth.floor((rand.nextInt(12000) + 3600) * (0.95F + severity * 0.45F)));
    }

    private static int getRainDuration(RandomSource rand) {
        return rand.nextInt(12000) + 12000;
    }

    private static int getClearDuration(RandomSource rand) {
        return rand.nextInt(168000) + 12000;
    }

    private static int getThunderClearDuration(RandomSource rand, float severity) {
        return Math.max(2400, Mth.floor(getClearDuration(rand) * (1.05F - severity * 0.45F)));
    }

    public void forceClear(RandomSource rand, int duration) {
        raining = false;
        thundering = false;
        rainTime = Math.max(1, duration);
        thunderTime = getClearDuration(rand);
        prevRainStrength = rainStrength = prevThunderStrength = thunderStrength = 0;
    }

    public void forceRain(RandomSource rand, int duration) {
        raining = true;
        thundering = false;
        rainTime = Math.max(1, duration);
        thunderTime = getClearDuration(rand);
    }

    public void forceThunder(int duration) {
        raining = true;
        thundering = true;
        rainTime = thunderTime = Math.max(1, duration);
    }

    private static boolean sampleCanSpawnLightning(@Nullable ServerLevel level) {
        if (level == null) return true;
        for (int[] s : LIGHTNING_BIOME_SAMPLES) {
            // 1.7's canSpawnLightningBolt: biomes that rain, not snow or stay dry
            var biome = level.getBiome(new BlockPos(s[0], level.getSeaLevel(), s[1])).value();
            if (biome.hasPrecipitation() && biome.getBaseTemperature() >= 0.15F) return true;
        }
        return false;
    }

    public boolean updateForTick(long tick, RandomSource rand, CelestialBody body, @Nullable ServerLevel level) {
        if (lastUpdateTick == tick) return false;
        lastUpdateTick = tick;

        if (!supportsWeather(body)) {
            boolean had = rainTime != 0 || thunderTime != 0 || canSpawnLightning || raining || thundering
                    || prevRainStrength > 0 || rainStrength > 0 || prevThunderStrength > 0 || thunderStrength > 0;
            rainTime = thunderTime = 0;
            canSpawnLightning = raining = thundering = false;
            prevRainStrength = rainStrength = prevThunderStrength = thunderStrength = 0;
            return had;
        }

        boolean changed = false;
        float severity = getLightningSeverity(body);
        // sampling biomes is only worth it now and then, and only on a loaded planet
        if (level != null && tick % 100 == 0) {
            boolean allowed = sampleCanSpawnLightning(level);
            if (canSpawnLightning != allowed) {
                canSpawnLightning = allowed;
                changed = true;
            }
        }

        if (!canSpawnLightning && thundering) {
            thundering = false;
            thunderTime = getThunderClearDuration(rand, severity);
            changed = true;
        }

        if (thunderTime <= 0) {
            thunderTime = thundering ? getStormDuration(rand, severity) : getThunderClearDuration(rand, severity);
            changed = true;
        } else if (--thunderTime <= 0) {
            thundering = canSpawnLightning && !thundering;
            thunderTime = thundering ? getStormDuration(rand, severity) : getThunderClearDuration(rand, severity);
            changed = true;
        }

        prevThunderStrength = thunderStrength;
        thunderStrength = Mth.clamp(thunderStrength + (thundering ? 0.01F : -0.01F), 0, 1);

        if (rainTime <= 0) {
            rainTime = raining ? getRainDuration(rand) : getClearDuration(rand);
            changed = true;
        } else if (--rainTime <= 0) {
            raining = !raining;
            if (!raining) {
                thundering = false;
                thunderTime = getThunderClearDuration(rand, severity);
            }
            rainTime = raining ? getRainDuration(rand) : getClearDuration(rand);
            changed = true;
        }

        prevRainStrength = rainStrength;
        rainStrength = Mth.clamp(rainStrength + (raining ? 0.01F : -0.01F), 0, 1);

        return changed || Math.floorMod(body.name.hashCode() + (int) tick, SAVE_INTERVAL) == 0;
    }

    public float getRainStrength(float partialTicks) {
        return Mth.clamp(Mth.lerp(partialTicks, prevRainStrength, rainStrength), 0, 1);
    }

    public float getThunderStrength(float partialTicks) {
        return Mth.clamp(Mth.lerp(partialTicks, prevThunderStrength, thunderStrength), 0, 1);
    }

    public float getWeightedThunderStrength(float partialTicks) {
        return getRainStrength(partialTicks) * getThunderStrength(partialTicks);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putBoolean("raining", raining);
        nbt.putBoolean("thundering", thundering);
        nbt.putBoolean("canSpawnLightning", canSpawnLightning);
        nbt.putInt("rainTime", rainTime);
        nbt.putInt("thunderTime", thunderTime);
        nbt.putFloat("prevRainStrength", prevRainStrength);
        nbt.putFloat("rainStrength", rainStrength);
        nbt.putFloat("prevThunderStrength", prevThunderStrength);
        nbt.putFloat("thunderStrength", thunderStrength);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        raining = nbt.getBoolean("raining");
        thundering = nbt.getBoolean("thundering");
        canSpawnLightning = !nbt.contains("canSpawnLightning") || nbt.getBoolean("canSpawnLightning");
        rainTime = nbt.getInt("rainTime");
        thunderTime = nbt.getInt("thunderTime");
        prevRainStrength = nbt.getFloat("prevRainStrength");
        rainStrength = nbt.getFloat("rainStrength");
        prevThunderStrength = nbt.getFloat("prevThunderStrength");
        thunderStrength = nbt.getFloat("thunderStrength");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(raining);
        buf.writeBoolean(thundering);
        buf.writeBoolean(canSpawnLightning);
        buf.writeInt(rainTime);
        buf.writeInt(thunderTime);
        buf.writeFloat(prevRainStrength);
        buf.writeFloat(rainStrength);
        buf.writeFloat(prevThunderStrength);
        buf.writeFloat(thunderStrength);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        raining = buf.readBoolean();
        thundering = buf.readBoolean();
        canSpawnLightning = buf.readBoolean();
        rainTime = buf.readInt();
        thunderTime = buf.readInt();
        prevRainStrength = buf.readFloat();
        rainStrength = buf.readFloat();
        prevThunderStrength = buf.readFloat();
        thunderStrength = buf.readFloat();
    }
}
