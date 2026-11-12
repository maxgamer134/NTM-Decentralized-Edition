// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.space.CelestialBody;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.space.trait.CBT_Weather;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * AtmosphereRenderUtil: how a body's air looks from afar, as the sky's atmosphere glow and
 * shaders draw it. The nuclear shockwave uniforms are left out (CelestialNukeShockHandler is not
 * ported), so the shaders see no shocks.
 */
public final class SpaceAtmosphereRender {

    private static final float OPAQUE_ATMOSPHERE_PRESSURE = 5.0F;
    private static final float CLOUDY_ATMOSPHERE_PRESSURE = 0.5F;
    private static final float DENSE_HAZE_PRESSURE = 3.0F;
    private static final float CLOUD_STORM_SMOOTHING_RATE = 6.0F;
    private static final String EVE_BODY_NAME = "eve";
    private static final float EVE_STRIKE_WINDOW = 8.0F;
    private static final float EVE_STRIKE_FADE_TIME = 5.0F;
    private static final float EVE_STRIKE_DELAY_RANGE = 2.4F;
    private static final float EVE_STRIKE_GATE_THRESHOLD = 0.82F;
    private static final double EVE_HASH_MULTIPLIER = 43758.5453123D;

    public static final int ATMOSPHERE_STYLE_CLEAR = 0;
    public static final int ATMOSPHERE_STYLE_CLOUDS = 1;
    public static final int ATMOSPHERE_STYLE_HAZE = 2;
    public static final int ATMOSPHERE_STYLE_GAS_BANDS = 3;
    public static final int LIGHTNING_MODE_STANDARD = 0;
    public static final int LIGHTNING_MODE_EVE = 1;

    private static final Map<String, double[]> CLOUD_STORM_FADE = new HashMap<>();

    private SpaceAtmosphereRender() {}

    static @Nullable Fluid fluid(String name) {
        try {
            return NTMFluids.byName(name);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    static boolean is(@Nullable Fluid fluid, String... names) {
        if (fluid == null) return false;
        for (String name : names) if (fluid == fluid(name)) return true;
        return false;
    }

    /** WorldProviderCelestial.getAtmosphereFluidColor. */
    public static float[] fluidColor(@Nullable Fluid fluid) {
        if (fluid == null) return new float[] {1F, 1F, 1F};
        if (is(fluid, "EVEAIR")) return new float[] {53F / 255F, 32F / 255F, 74F / 255F};
        // slightly redder "red sand" tint for Duna-like atmospheres
        if (is(fluid, "DUNAAIR")) return new float[] {198F / 255F, 96F / 255F, 64F / 255F};
        // neutral, desaturated CO2
        if (is(fluid, "CARBONDIOXIDE")) return new float[] {188F / 255F, 192F / 255F, 198F / 255F};
        if (is(fluid, "EARTHAIR", "OXYGEN", "NITROGEN")) return new float[] {0.7529412F, 0.84705883F, 1.0F};
        int hex = SpaceEffects.fluidHex(fluid);
        return new float[] {(hex >> 16 & 0xFF) / 255F, (hex >> 8 & 0xFF) / 255F, (hex & 0xFF) / 255F};
    }

    public static float glowAlpha(@Nullable CelestialBody body) {
        if (body == null) return 0F;
        if (body.gas != null) return 0.35F;
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        if (atmosphere == null) return 0F;
        float pressure = Mth.clamp((float) atmosphere.getPressure(), 0F, 3F);
        if (pressure <= 0.02F) return 0F;
        return Mth.clamp(0.08F + pressure * 0.16F, 0.08F, 0.5F);
    }

    public static float pressure(@Nullable CelestialBody body) {
        if (body == null) return 0F;
        if (body.gas != null) return OPAQUE_ATMOSPHERE_PRESSURE;
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        return atmosphere == null ? 0F : Math.max(0F, (float) atmosphere.getPressure());
    }

    public static float density(@Nullable CelestialBody body) {
        return Mth.clamp(pressure(body) / OPAQUE_ATMOSPHERE_PRESSURE, 0F, 1F);
    }

    public static int style(@Nullable CelestialBody body) {
        if (body == null) return ATMOSPHERE_STYLE_CLEAR;
        if (body.gas != null) return ATMOSPHERE_STYLE_GAS_BANDS;
        float pressure = pressure(body);
        if (pressure > DENSE_HAZE_PRESSURE) return ATMOSPHERE_STYLE_HAZE;
        if (pressure >= CLOUDY_ATMOSPHERE_PRESSURE) return ATMOSPHERE_STYLE_CLOUDS;
        return ATMOSPHERE_STYLE_CLEAR;
    }

    public static float[] atmosphereColor(@Nullable CelestialBody body) {
        if (body == null) return new float[] {1F, 1F, 1F};
        if (body.gas != null) return fluidColor(body.gas);
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        if (atmosphere != null && !atmosphere.fluids.isEmpty()) {
            double total = 0, r = 0, g = 0, b = 0;
            for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
                if (entry == null || entry.fluid == null || entry.pressure <= 0) continue;
                float[] c = fluidColor(entry.fluid);
                r += c[0] * entry.pressure;
                g += c[1] * entry.pressure;
                b += c[2] * entry.pressure;
                total += entry.pressure;
            }
            if (total > 0) {
                return new float[] {
                    (float) Mth.clamp(r / total, 0, 1), (float) Mth.clamp(g / total, 0, 1), (float) Mth.clamp(b / total, 0, 1)
                };
            }
        }
        return new float[] {1F, 1F, 1F};
    }

    public static float[] cloudColor(@Nullable CelestialBody body) {
        if (body == null) return new float[] {1F, 1F, 1F};
        if (body.gas != null) {
            float[] gas = atmosphereColor(body);
            float peak = Math.max(gas[0], Math.max(gas[1], gas[2]));
            if (peak <= 0) return new float[] {1F, 1F, 1F};
            return new float[] {Mth.clamp(gas[0] / peak, 0, 1), Mth.clamp(gas[1] / peak, 0, 1), Mth.clamp(gas[2] / peak, 0, 1)};
        }
        var c = SpaceEffects.tintedCloudColor(body.getTrait(CBT_Atmosphere.class), new net.minecraft.world.phys.Vec3(1, 1, 1));
        return new float[] {(float) c.x, (float) c.y, (float) c.z};
    }

    public static float cloudTintStrength(@Nullable CelestialBody body) {
        if (body == null) return 0F;
        if (body.gas != null) return 1F;
        return SpaceEffects.cloudTintStrength(body.getTrait(CBT_Atmosphere.class));
    }

    public static float cloudStormDarkness(@Nullable CelestialBody body, float partialTicks) {
        CBT_Weather weather = body != null ? body.getTrait(CBT_Weather.class) : null;
        float target = 0F;
        if (weather != null && CBT_Weather.supportsWeather(body)) {
            float rain = weather.getRainStrength(partialTicks);
            float thunder = weather.getThunderStrength(partialTicks);
            float stateDark = (weather.raining ? 0.22F : 0F) + (weather.thundering ? 0.48F : 0F);
            float animated = rain * 0.35F + thunder * 0.22F;
            target = Mth.clamp(Math.max(stateDark, animated), 0F, 0.9F);
        }
        if (body == null) return target;
        long now = Util.getMillis();
        double[] state = CLOUD_STORM_FADE.get(body.name);
        if (state == null) {
            CLOUD_STORM_FADE.put(body.name, new double[] {target, now});
            return target;
        }
        float delta = Mth.clamp((float) (now - state[1]) / 1000F, 0F, 0.5F);
        float blend = Mth.clamp(delta * CLOUD_STORM_SMOOTHING_RATE, 0F, 1F);
        state[0] += (target - state[0]) * blend;
        state[1] = now;
        return (float) state[0];
    }

    public static float cloudLightningStrength(@Nullable CelestialBody body, float partialTicks) {
        CBT_Weather weather = body != null ? body.getTrait(CBT_Weather.class) : null;
        if (weather == null || !CBT_Weather.supportsWeather(body) || !weather.canSpawnLightning) return 0F;
        float activity = CBT_Weather.getLightningActivityFactor(body);
        if (isEve(body)) return Mth.clamp(activity, 0F, 1F);
        return Mth.clamp(weather.getThunderStrength(partialTicks) * activity, 0F, 1F);
    }

    public static int lightningMode(@Nullable CelestialBody body) {
        return isEve(body) ? LIGHTNING_MODE_EVE : LIGHTNING_MODE_STANDARD;
    }

    public static float eveFlashStrength(@Nullable CelestialBody body, float time) {
        if (!isEve(body)) return 0F;
        float event = Mth.floor(time / EVE_STRIKE_WINDOW);
        float eventTime = time - event * EVE_STRIKE_WINDOW;
        float delay = eveHash(event, 41.9F) * EVE_STRIKE_DELAY_RANGE;
        if (eventTime < delay) return 0F;
        if (eveHash(event, 313.7F) < EVE_STRIKE_GATE_THRESHOLD) return 0F;
        float t = Math.max(eventTime - delay, 0F);
        return Mth.clamp(smoothstep(0F, 0.14F, t) * (1F - smoothstep(0.16F, EVE_STRIKE_FADE_TIME, t)), 0F, 1F);
    }

    private static boolean isEve(@Nullable CelestialBody body) {
        return body != null && EVE_BODY_NAME.equals(body.name);
    }

    private static float eveHash(float x, float y) {
        double value = Math.sin(x * 127.1D + y * 311.7D) * EVE_HASH_MULTIPLIER;
        return (float) (value - Math.floor(value));
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        if (edge0 == edge1) return value < edge0 ? 0F : 1F;
        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0F, 1F);
        return t * t * (3F - 2F * t);
    }
}
