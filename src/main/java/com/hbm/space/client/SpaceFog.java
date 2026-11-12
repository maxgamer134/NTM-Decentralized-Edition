// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.client;

import com.hbm.handler.ImpactWorldHandler;
import com.hbm.particle.ParticleDust;
import com.hbm.space.CelestialBody;
import com.hbm.space.SpaceTime;
import com.hbm.space.SpaceToggle;
import com.hbm.space.SpaceWeather;
import com.hbm.space.dim.legacy.LegacyConfig;
import com.hbm.space.trait.CBT_Atmosphere;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * The planets' air seen up close: the fog's colour and thickness (WorldProviderCelestial.getFogColor
 * and fogDensity), the sky colour the fog fades into, Duna's dust storms
 * (WorldProviderDuna.updateWeather and fogDensity) and Eve's thunder (WorldProviderEve.updateWeather).
 */
public final class SpaceFog {

    /** Set while the sky asks the level for vanilla's own sky colour, so it isn't handed its own back. */
    static boolean vanillaSky;
    private static float @Nullable [] lastSky;
    private static @Nullable Level lastSkyLevel;
    /** How much of the sun the last sky drawn had eclipsed. */
    static double lastEclipse;

    /** Duna's dust storm as the client sees it, eased toward the storm blowing. */
    private static float dustStorm;
    private static float lastFlash;

    private SpaceFog() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SpaceFog::onRenderFog);
        NeoForge.EVENT_BUS.addListener(SpaceFog::onClientTick);
    }

    /** A planet the client stands on (not Kerbin, not orbit), with Space on. */
    static @Nullable CelestialBody planet(Level level) {
        if (!SpaceToggle.enabled() || level.dimension() == Level.OVERWORLD) return null;
        return CelestialBody.getBodyOrNull(level.dimension());
    }

    // ---------------------------------------------------------------- sky colour

    /** The sky drew this colour; the fog fades into it. */
    static void skyDrawn(Level level, float[] sky, double eclipse) {
        lastSky = sky.clone();
        lastSkyLevel = level;
        lastEclipse = eclipse;
    }

    /**
     * The level's sky colour on a planet: the one the celestial sky last drew
     * (WorldProviderCelestial.getSkyColor), which vanilla mixes into the fog; null for vanilla's.
     */
    public static @Nullable Vec3 skyColor(Level level) {
        float[] sky = lastSky;
        if (vanillaSky || sky == null || lastSkyLevel != level || planet(level) == null) return null;
        return new Vec3(sky[0], sky[1], sky[2]);
    }

    // ---------------------------------------------------------------- fog colour

    /**
     * WorldProviderCelestial.getFogColor: each gas's colour by its share of the air (Eve purple,
     * Duna rust, Kerbin's air blue, the rest their fluid colour), lit by the sun, with a faint night
     * glow, thinned in thin air, fading out high above the ground, darkened and reddened in an
     * eclipse and dimmed or blackened by impact dust and fires. Black in a vacuum.
     */
    static Vec3 fogColor(ClientLevel level, CelestialBody body, float partial) {
        // vanilla samples the fog colour over a 6x6x6 grid of biomes each frame; it's the same everywhere
        long time = level.getGameTime();
        if (fogCacheLevel == level && fogCacheTime == time && fogCachePartial == partial && fogCache != null) return fogCache;
        fogCache = computeFogColor(level, body, partial);
        fogCacheLevel = level;
        fogCacheTime = time;
        fogCachePartial = partial;
        return fogCache;
    }

    private static @Nullable Vec3 fogCache;
    private static @Nullable Level fogCacheLevel;
    private static long fogCacheTime;
    private static float fogCachePartial;

    private static Vec3 computeFogColor(ClientLevel level, CelestialBody body, float partial) {
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        if (atmosphere == null) return Vec3.ZERO;
        float solarAngle = SpaceTime.timeOfDay(level, partial);
        if (Float.isNaN(solarAngle)) solarAngle = level.getTimeOfDay(partial);
        float sun = Mth.clamp(Mth.cos(solarAngle * Mth.TWO_PI) * 2F + 0.5F, 0F, 1F);
        boolean hardcore = LegacyConfig.GeneralConfig.enableHardcoreDarkness;
        float sunR = hardcore ? sun : sun * 0.94F, sunG = hardcore ? sun : sun * 0.94F, sunB = hardcore ? sun : sun * 0.91F;

        float total = (float) atmosphere.getPressure();
        double r = 0, g = 0, b = 0;
        for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
            if (entry == null || entry.fluid == null || total <= 0F) continue;
            Fluid f = entry.fluid;
            double fr, fg, fb;
            if (SpaceAtmosphereRender.is(f, "EVEAIR")) {
                fr = 53F / 255F * sunR;
                fg = 32F / 255F * sunG;
                fb = 74F / 255F * sunB;
            } else if (SpaceAtmosphereRender.is(f, "DUNAAIR", "CARBONDIOXIDE")) {
                fr = 212F / 255F * sunR;
                fg = 112F / 255F * sunG;
                fb = 78F / 255F * sunB;
            } else if (SpaceAtmosphereRender.is(f, "EARTHAIR", "OXYGEN", "NITROGEN")) {
                fr = 0.7529412F * sunR;
                fg = 0.84705883F * sunG;
                fb = 1.0F * sunB;
            } else {
                int hex = SpaceEffects.fluidHex(f);
                fr = (hex >> 16 & 0xFF) / 255F * sunR * 1.4F;
                fg = (hex >> 8 & 0xFF) / 255F * sunG * 1.4F;
                fb = (hex & 0xFF) / 255F * sunB * 1.4F;
            }
            double share = entry.pressure / total;
            r += fr * share;
            g += fg * share;
            b += fb * share;
        }

        // a minimum colour, for the night-time glow
        if (!hardcore) {
            float night = Mth.clamp(total, 0F, 1F);
            r += 0.06F * night;
            g += 0.06F * night;
            b += 0.09F * night;
        }

        // the fog stays strong to make thin air look thin
        float pressureFactor = Mth.clamp(total * 10F, 0F, 1F);
        r *= pressureFactor;
        g *= pressureFactor;
        b *= pressureFactor;

        Minecraft mc = Minecraft.getInstance();
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        if (camY > 600) {
            double curvature = Mth.clamp((1000F - (float) camY) / 400F, 0F, 1F);
            r *= curvature;
            g *= curvature;
            b *= curvature;
        }

        double eclipse = lastEclipse;
        if (eclipse > 0) {
            r *= 1 - eclipse * 0.3;
            g *= 1 - eclipse * 0.3;
            b *= 1 - eclipse * 0.3;
            float[] sunset = SpaceEffects.sunriseColor(atmosphere, 0.25F);
            if (sunset != null) {
                double amount = Mth.clamp(eclipse * 0.5 - (1 - sun), 0.0, 1.0);
                r = r * (1 - amount) + sunset[0] * amount;
                g = g * (1 - amount) + sunset[1] * amount;
                b = b * (1 - amount) + sunset[2] * amount;
            }
        }

        float dust = ImpactWorldHandler.getDustForClient(level);
        float fire = ImpactWorldHandler.getFireForClient(level);
        g *= 1 - dust * 0.5F;
        b *= 1 - dust;
        float dim = fire > 0 ? Math.max(1 - dust * 2, 0) : 1 - dust;
        return new Vec3(r * dim, g * dim, b * dim);
    }

    // ---------------------------------------------------------------- fog thickness

    /**
     * WorldProviderCelestial.fogDensity: air thicker than two atmospheres fogs up with the square
     * of its pressure, and a Duna dust storm blowing at a quarter or more thickens it with its own.
     */
    static float fogDensity(Level level, CelestialBody body) {
        if (SpaceWeather.isDuna(level) && dustStorm >= 0.25F) return dustStorm * dustStorm * 0.075F;
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        if (atmosphere == null) return 0F;
        float pressure = (float) atmosphere.getPressure();
        return pressure <= 2F ? 0F : pressure * pressure * 0.002F;
    }

    /**
     * 1.7.10 drew this fog exponentially (GL_EXP); 1.21's fog is linear, so it runs from the camera
     * to where the exponential fog was nearly solid (2.5 / density: 92% fogged).
     */
    private static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        CelestialBody body = planet(mc.level);
        if (body == null) return;
        float density = fogDensity(mc.level, body);
        if (density <= 0F) return;
        float far = Math.min(event.getFarPlaneDistance(), 2.5F / density);
        event.setNearPlaneDistance(event.getMode() == FogRenderer.FogMode.FOG_SKY ? 0F : Math.min(event.getNearPlaneDistance(), 0F));
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }

    // ---------------------------------------------------------------- weather ticking

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) return;

        // Duna: the storm eases in and out, and its dust blows past the player
        if (SpaceWeather.isDuna(level)) {
            dustStorm += (SpaceWeather.dunaStorm(level.getGameTime()) - dustStorm) * 0.008F;
            if (dustStorm >= 0.05F && level.random.nextFloat() < dustStorm) {
                // Vec3(20, 0, 50) turned about Z then Y, as 1.7.10's rotateAroundZ/Y did
                double x = 20, y = 0, z = 50;
                double a = level.random.nextDouble() * Math.PI * 10;
                double cos = Math.cos(a), sin = Math.sin(a);
                double nx = x * cos + y * sin;
                y = y * cos - x * sin;
                x = nx;
                a = level.random.nextDouble() * Math.PI * 2 * 5;
                cos = Math.cos(a);
                sin = Math.sin(a);
                nx = x * cos + z * sin;
                z = z * cos - x * sin;
                x = nx;
                mc.particleEngine.add(new ParticleDust(level, player.getX() + x, player.getY(), player.getZ() + z, -4, 0, 0, 4F));
            }
        } else {
            dustStorm = 0F;
        }

        // Eve: thunder rumbles as a lightning flash begins
        CelestialBody body = planet(level);
        float flash = body != null ? SpaceAtmosphereRender.eveFlashStrength(body, level.getGameTime() / 20F) : 0F;
        if (lastFlash <= 0.01F && flash > 0.01F) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), SpaceWeather.RUMBLE.get(), SoundSource.WEATHER, 10F, 1F, false);
        }
        lastFlash = flash;
    }
}
