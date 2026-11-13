// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.api.client.CustomSkies;
import com.hbm.backport.client.core.SkyRenderState;
import com.hbm.client.render.NTMSkybox;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ImpactWorldHandler;
import com.hbm.lib.Library;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.space.AstronomyUtil;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.SolarSystem.AstroMetric;
import com.hbm.space.SpaceSkySync;
import com.hbm.space.SpaceTime;
import com.hbm.space.SpaceToggle;
import com.hbm.space.station.OrbitalStation;
import com.hbm.space.station.OrbitalStation.StationState;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.space.trait.CBT_Destroyed;
import com.hbm.space.trait.CBT_Dyson;
import com.hbm.space.trait.CBT_Impact;
import com.hbm.space.trait.CBT_Lights;
import com.hbm.space.trait.CBT_War;
import com.hbm.space.trait.CelestialBodyTrait.CBT_Compromised;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * NTM:Space's skies. On a body's surface (SkyProviderCelestial, Kerbin included): the vanilla-style
 * fogged sky disc in the colour of the air, the sunset fan and in a vacuum its charged dust, the
 * night.png star box turned by the body's sidereal angle, the local star in the air's colour with
 * its Dyson swarm (or Kerbol's black hole once destroyed), every other body at its apparent size,
 * angle and inclination, with its atmosphere glow, cloud shader, crescent shadow, city lights,
 * lightning, impact scars, nuclear flashes, rings and the daylight wash (in the air's colour
 * lerped into thick fog), or as a coloured point when too small to see; the satellites, the held
 * satellite's orbit and the orbital stations; the CBT_War projectiles' shockwaves, the invasion
 * meteors, the observer's own rings, the flesh over a compromised body, the dark lower discs and
 * the planet seen from high up. From a station (SkyProviderOrbit): the stars and star along the
 * station's orbit and the body below it, turning beneath, with everything else in the system.
 */
public final class SpaceSky implements CustomSkies.Renderer {

    private static final ResourceLocation PLANET = Library.id("textures/misc/space/planet.png");
    private static final ResourceLocation FLARE = Library.id("textures/misc/space/sunspike.png");
    private static final ResourceLocation NIGHT = Library.id("textures/misc/space/night.png");
    private static final ResourceLocation DIGAMMA_STAR = Library.id("textures/misc/space/star_digamma.png");
    private static final ResourceLocation LODE_STAR = Library.id("textures/misc/star_lode.png");
    private static final ResourceLocation STATION = Library.id("textures/misc/space/station.png");
    private static final ResourceLocation SATELLITE = Library.id("textures/misc/space/satellite.png");
    private static final ResourceLocation IMPACT = Library.id("textures/misc/space/impact.png");
    private static final ResourceLocation SHOCKWAVE = Library.id("textures/particle/shockwave.png");
    private static final ResourceLocation SHOCK_FLARE = Library.id("textures/particle/flare.png");
    private static final ResourceLocation THATMO_SHIELD = Library.id("textures/particle/cens.png");
    private static final ResourceLocation PARTICLE_BASE = Library.id("textures/particle/particle_base.png");
    private static final ResourceLocation RINGS = Library.id("textures/misc/space/rings.png");
    private static final ResourceLocation DESTROYED = Library.id("textures/misc/space/destroyed.png");
    private static final ResourceLocation NOISE = Library.id("textures/misc/space/noise.png");
    private static final ResourceLocation DEFAULT_MASK = Library.id("textures/misc/space/default_mask.png");
    private static final ResourceLocation[] CITY_LIGHTS = {
        Library.id("textures/misc/space/citylights_0.png"),
        Library.id("textures/misc/space/citylights_1.png"),
        Library.id("textures/misc/space/citylights_2.png"),
        Library.id("textures/misc/space/citylights_3.png"),
    };

    private static final double TRANSITION_MIN_SIZE = 0.01D;
    private static final double TRANSITION_MAX_SIZE = 0.5D;
    private static final float BLEND_DARKEN = 0.1F;

    // SatelliteBase's defaults, which Kerbin's delivery satellite keeps; the others carry their own settings
    private static final float SAT_INCLINATION = 0F;
    private static final float SAT_ALTITUDE_KM = AstronomyUtil.DEFAULT_ALTITUDE_KM;
    private static final float SAT_PHASE_OFFSET = 0F;
    private static final boolean SAT_BLINKING = false;
    private static final float SAT_BLINK_PERIOD = 0.3F;


    private static boolean registered;

    private final Random lodeRandom = new Random();
    private float partial;
    private @Nullable CelestialBody lastBody;
    private static float @Nullable [] sphere;

    public static void register() {
        if (registered) return;
        registered = true;
        CustomSkies.register(new SpaceSky());
    }

    /** The body whose sky this is: the orbited one from a station, Kerbin in the overworld. */
    static @Nullable CelestialBody observer(ClientLevel level) {
        if (!SpaceToggle.enabled()) return null;
        if (level.dimension() == Level.OVERWORLD && "off".equalsIgnoreCase(com.hbm.config.RenderConfig.spaceOverworldSky)) return null;
        if (CelestialBody.inOrbit(level)) return OrbitalStation.clientStation.orbiting;
        CelestialBody body = SpaceTime.body(level);
        // Kerbin keeps vanilla time, so SpaceTime has no body for it, but it gets the celestial sky
        if (body == null && level.dimension() == Level.OVERWORLD) body = CelestialBody.getBodyOrNull(Level.OVERWORLD);
        return body;
    }

    private static @Nullable Boolean otherSkyMods;

    /**
     * Whether the overworld keeps its vanilla sky with the Space bodies drawn over it, instead of
     * the full celestial sky. Mods that draw into the vanilla sky pass (Create: Rocketnautics'
     * moon, Exposure: Space's sky layer, No Man's Land's Friend Moon, shader packs' sun and moon)
     * would vanish under the full sky, which replaces that pass.
     */
    static boolean layered(ClientLevel level) {
        if (level.dimension() != Level.OVERWORLD) return false;
        String mode = com.hbm.config.RenderConfig.spaceOverworldSky;
        if ("layered".equalsIgnoreCase(mode)) return true;
        if ("full".equalsIgnoreCase(mode)) return false;
        if (com.hbm.api.client.ShaderPacks.inUse()) return true;
        if (otherSkyMods == null) {
            boolean found = false;
            for (var mod : net.neoforged.fml.ModList.get().getMods()) {
                String id = mod.getModId().toLowerCase(java.util.Locale.ROOT);
                if (id.contains("rocketnautics") || id.contains("exposure_space") || id.contains("exposurespace")
                        || id.equals("nomansland")) {
                    found = true;
                    break;
                }
            }
            otherSkyMods = found;
        }
        return otherSkyMods;
    }

    @Override
    public boolean extract(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state) {
        partial = partialTicks;
        return observer(level) != null && !layered(level);
    }

    @Override
    public void overlay(LevelRenderer vanilla, Matrix4f viewRotation, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !layered(level)) return;
        CelestialBody body = observer(level);
        if (body == null) return;
        partial = partialTicks;
        Camera camera = mc.gameRenderer.getMainCamera();

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf())));
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        try {
            renderLayered(mc, level, camera, body);
        } finally {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /**
     * The layered overworld sky: only what vanilla's sky lacks. The planets (the vanilla moon, or
     * whatever replaces it, stands in for the Mun), satellites, stations, war flashes and meteors.
     */
    private void renderLayered(Minecraft mc, ClientLevel level, Camera camera, CelestialBody body) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        float solarAngle = level.getTimeOfDay(partial);
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        float pressure = atmosphere != null ? (float) atmosphere.getPressure() : 0F;
        float visibility = (atmosphere != null ? Mth.clamp(2F - pressure, 0.1F, 1F) : 1F) * (1F - level.getRainLevel(partial));
        if (visibility <= 0.01F) return;

        List<AstroMetric> metrics = new java.util.ArrayList<>(SolarSystem.calculateMetricsFromBody(level, partial, body, solarAngle));
        metrics.removeIf(m -> "mun".equals(m.body.name));
        Vec3 skyColor = level.getSkyColor(camera.getPosition(), partial);
        float[] tint = {(float) skyColor.x, (float) skyColor.y, (float) skyColor.z};
        float sunFactor = sunBrightnessFactor(level, solarAngle, partial);
        float blendAmount = atmosphere != null ? Mth.clamp(1F - sunFactor, 0.25F, 1F) : 1F;

        modelView.pushMatrix();
        modelView.rotateX(rad(body.axialTilt));
        modelView.rotateY(rad(-90F));
        modelView.rotateX(solarAngle * Mth.TWO_PI);
        additive();
        renderCelestials(level, partial, metrics, tint, visibility, blendAmount, null, SolarSystem.MAX_APPARENT_SIZE_SURFACE);
        RenderSystem.enableBlend();
        additive();
        if (visibility > 0.2F) renderOrbiters(mc, level, body, solarAngle);
        modelView.popMatrix();

        RenderSystem.enableBlend();
        additive();
        CBT_War war = body.getTrait(CBT_War.class);
        if (war != null) renderWar(war);
        renderMeteors(mc, camera);
    }

    @Override
    public void render(LevelRenderer vanilla, SkyRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        CelestialBody body = observer(level);
        if (body == null) return;
        Camera camera = mc.gameRenderer.getMainCamera();

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf())));
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        if (CelestialBody.inOrbit(level) && OrbitalStation.clientStation.orbiting != null) {
            renderOrbit(mc, level);
        } else {
            renderSurface(mc, level, camera, body);
        }

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    // ---------------------------------------------------------------- surface (SkyProviderCelestial)

    private void renderSurface(Minecraft mc, ClientLevel level, Camera camera, CelestialBody body) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        float solarAngle = SpaceTime.timeOfDay(level, partial);
        if (Float.isNaN(solarAngle)) solarAngle = level.getTimeOfDay(partial);

        CelestialBody sun = body.getStar();
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        boolean hasAtmosphere = atmosphere != null;
        float pressure = hasAtmosphere ? (float) atmosphere.getPressure() : 0F;
        float visibility = hasAtmosphere ? Mth.clamp(2F - pressure, 0.1F, 1F) : 1F;
        float dust = ImpactWorldHandler.getDustForClient(level);
        float fire = ImpactWorldHandler.getFireForClient(level);
        CBT_War war = body.getTrait(CBT_War.class);

        List<AstroMetric> metrics = SolarSystem.calculateMetricsFromBody(level, partial, body, solarAngle);
        double trueSunSize = SolarSystem.calculateSunSize(body);
        double eclipse = eclipseFactor(metrics, trueSunSize, SolarSystem.MAX_APPARENT_SIZE_SURFACE);
        float sunFactor = sunBrightnessFactor(level, solarAngle, partial);

        Vec3 cam = camera.getPosition();
        SpaceFog.vanillaSky = true;
        float[] sky = skyColor(level, cam, partial, atmosphere, sunFactor, eclipse, war, dust, fire);
        SpaceFog.vanillaSky = false;
        // diminish the sky colour when leaving the atmosphere
        if (cam.y > 300) {
            float curvature = Mth.clamp((800F - (float) cam.y) / 500F, 0F, 1F);
            for (int i = 0; i < 3; i++) sky[i] *= curvature;
        }
        // the fog fades into this sky, and darkens with this eclipse
        SpaceFog.skyDrawn(level, sky, eclipse);

        // the daylight wash over the bodies sinks into thick fog (ModEventHandlerRenderer.lastFogDensity * 30)
        float[] tint = sky.clone();
        float fogIntensity = fogDensity(atmosphere) * 30F;
        if (fogIntensity > 0.01F) {
            float[] fog = RenderSystem.getShaderFogColor();
            for (int i = 0; i < 3; i++) tint[i] = clampedLerp(sky[i], fog[i], fogIntensity);
        }

        skyDisc(mc, sky);

        RenderSystem.defaultBlendFunc();
        float starBrightness = starBrightness(body, solarAngle) * (1F - dust) * visibility;
        // SolarSystem.calculateSiderealAngle is in degrees; NTM:Space added it to the solar angle (in
        // turns) unconverted, which spun the stars 360 times too fast
        float siderealTurns = (float) (SolarSystem.calculateSiderealAngle(level, partial, body) / 360D);

        renderSunset(mc, level, atmosphere, solarAngle, pressure, dust);
        renderStars(mc, starBrightness, solarAngle + siderealTurns, body.axialTilt);

        modelView.pushMatrix();
        modelView.rotateX(rad(body.axialTilt));
        modelView.rotateY(rad(-90F));
        modelView.rotateX(solarAngle * Mth.TWO_PI);

        additive();
        double sunSize = trueSunSize * SolarSystem.SUN_RENDER_SCALE;
        double coronaSize = sunSize * (3 - Mth.clamp(pressure, 0F, 1F));
        renderSun(mc, level, partial, sun, atmosphere, sunSize, coronaSize, visibility, pressure);

        float blendAmount = hasAtmosphere ? Mth.clamp(1F - sunFactor, 0.25F, 1F) : 1F;
        renderCelestials(level, partial, metrics, tint, visibility, blendAmount, null, SolarSystem.MAX_APPARENT_SIZE_SURFACE);

        RenderSystem.enableBlend();
        additive();
        if (visibility > 0.2F) renderOrbiters(mc, level, body, solarAngle);
        modelView.popMatrix();

        RenderSystem.enableBlend();
        additive();
        if (war != null) renderWar(war);
        renderMeteors(mc, camera);

        if (body.hasRings) {
            modelView.pushMatrix();
            modelView.rotateX(rad(body.axialTilt - body.ringTilt));
            modelView.translate(0F, -100F, 0F);
            modelView.rotateY(rad(-90F));
            renderRings(body.ringColor, 200F, visibility);
            modelView.popMatrix();
        }

        if (body.hasTrait(CBT_Compromised.class)) renderFlesh(level);

        renderLowerDiscs(level, cam, sky);

        // the body below, seen from high above its surface
        if (cam.y > 200 && body.texture != null) {
            double sc = 1D / (cam.y / 1000D);
            float uvOffset = (float) ((cam.x / 1024D) % 1D);
            float sunBrightness = sunBrightness(level, sun, atmosphere, solarAngle, eclipse, war, dust);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(sunBrightness, sunBrightness, sunBrightness, Mth.clamp(((float) cam.y - 200F) / 300F, 0F, 1F));
            modelView.pushMatrix();
            modelView.rotateX(rad(180F));
            texQuad(body.texture, sc * 115D, 100D, uvOffset, 0F, 1F + uvOffset, 1F);
            modelView.popMatrix();
        }
    }

    // ---------------------------------------------------------------- orbit (SkyProviderOrbit)

    private void renderOrbit(Minecraft mc, ClientLevel level) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        OrbitalStation station = OrbitalStation.clientStation;
        CelestialBody target = station.target != null ? station.target : station.orbiting;
        double progress = station.getTransferProgress(partial);
        float orbitalTilt = 80F;

        // WorldProviderOrbit.updateSky: the system seen from the station's orbit, or between two orbits
        double trueSunSize = SolarSystem.calculateSunSize(station.orbiting);
        double fromAltitude = OrbitalStation.getOrbitalAltitude(station.orbiting);
        List<AstroMetric> metrics;
        if (station.state == StationState.ORBIT) {
            metrics = SolarSystem.calculateMetricsFromSatellite(level, partial, station.orbiting, fromAltitude);
        } else {
            double toAltitude = OrbitalStation.getOrbitalAltitude(target);
            metrics = SolarSystem.calculateMetricsBetweenSatelliteOrbits(level, partial, station.orbiting, target, fromAltitude, toAltitude, progress);
            trueSunSize = Mth.lerp(progress, trueSunSize, SolarSystem.calculateSunSize(target));
        }
        float angle = (float) SolarSystem.calculateSingleAngle(level, partial, metrics, station.orbiting, fromAltitude);
        if (progress > 0) {
            angle = (float) clerp(progress, angle,
                    SolarSystem.calculateSingleAngle(level, partial, metrics, target, OrbitalStation.getOrbitalAltitude(target)));
        }
        float worldAngle = 0.5F - angle / 360F;
        double eclipse = eclipseFactor(metrics, trueSunSize, SolarSystem.MAX_APPARENT_SIZE_ORBIT);
        SpaceFog.orbitEclipse = eclipse;

        // the backdrop is black: NTM:Space's orbit had black fog and sky
        dome(0F, 0F, 0F);

        float solarAngle = orbitCelestialAngle(metrics, worldAngle, station, target);
        float siderealTurns = (float) (SolarSystem.calculateSiderealAngle(level, partial, station.orbiting) / 360D);
        float celestialPhase = (1 - (solarAngle + 0.5F) % 1) * 2 - 1;

        // WorldProviderOrbit.getStarBrightness: day-side stars just before Kerbin, fully from beyond Duna
        float semiMajorAxisKm = station.orbiting.getPlanet().semiMajorAxisKm;
        if (progress > 0) semiMajorAxisKm = (float) Mth.lerp(progress, semiMajorAxisKm, target.getPlanet().semiMajorAxisKm);
        float distanceFactor = Mth.clamp((semiMajorAxisKm - 9_000_000F) / (30_000_000F - 9_000_000F), 0F, 1F);
        float starBrightness = Mth.clamp((float) eclipse, distanceFactor, 1F);

        RenderSystem.defaultBlendFunc();
        renderStars(mc, starBrightness, solarAngle + siderealTurns, orbitalTilt);

        modelView.pushMatrix();
        modelView.rotateX(rad(orbitalTilt));
        modelView.rotateY(rad(-90F));
        modelView.rotateX(solarAngle * Mth.TWO_PI);

        additive();
        double sunSize = SolarSystem.calculateSunSize(station.orbiting) * SolarSystem.SUN_RENDER_SCALE;
        if (station.state != StationState.ORBIT) {
            sunSize = Mth.lerp(progress, sunSize, SolarSystem.calculateSunSize(target) * SolarSystem.SUN_RENDER_SCALE);
        }
        double coronaSize = sunSize * (3 - smoothstep(Math.abs(celestialPhase), 0.7, 0.8));
        renderSun(mc, level, partial, station.orbiting.getStar(), null, sunSize, coronaSize, 1F, 0F);

        CelestialBody orbiting = station.state != StationState.ORBIT && progress > 0.5 ? target : station.orbiting;
        renderCelestials(level, partial, metrics, new float[] {0F, 0F, 0F}, 1F, 1F, orbiting, SolarSystem.MAX_APPARENT_SIZE_ORBIT);
        modelView.popMatrix();
    }

    /** SkyProviderOrbit.getCelestialAngle: during a transfer the star swings round to where the station heads. */
    private float orbitCelestialAngle(List<AstroMetric> metrics, float worldAngle, OrbitalStation station, CelestialBody target) {
        if (station.state == StationState.ORBIT) return worldAngle;
        float solarAngle = worldAngle * 360F - 180F;
        if (station.state != StationState.ARRIVING || lastBody == null) lastBody = station.orbiting;
        double progress = station.getUnscaledProgress(partial);
        float travelAngle = -(float) SolarSystem.calculateSingleAngle(metrics, lastBody, target);
        travelAngle = Mth.wrapDegrees(travelAngle + 90F);
        if (station.state == StationState.TRANSFER) return (travelAngle + 180F) / 360F;
        if (station.state == StationState.LEAVING) return ((float) clerp(progress, solarAngle, travelAngle) + 180F) / 360F;
        return ((float) clerp(progress, travelAngle, solarAngle) + 180F) / 360F;
    }

    // ---------------------------------------------------------------- sky values

    /** 1.7.10 World.getSunBrightnessFactor at this body's time of day. */
    private static float sunBrightnessFactor(ClientLevel level, float solarAngle, float partial) {
        float f = 1F - Mth.clamp(1F - (Mth.cos(solarAngle * Mth.TWO_PI) * 2F + 0.5F), 0F, 1F);
        f *= 1F - level.getRainLevel(partial) * 5F / 16F;
        f *= 1F - level.getThunderLevel(partial) * level.getRainLevel(partial) * 5F / 16F;
        return f;
    }

    /**
     * WorldProviderCelestial.getSunBrightness: 1.7.10's, dimmed by eclipses, impact dust and thick
     * air, gone with the star, and lit up by war projectiles bursting in the air.
     */
    private static float sunBrightness(ClientLevel level, CelestialBody star, @Nullable CBT_Atmosphere atmosphere, float solarAngle,
            double eclipse, @Nullable CBT_War war, float dust) {
        if (star.hasTrait(CBT_Destroyed.class)) return 0F;
        float f = 1F - Mth.clamp(1F - (Mth.cos(solarAngle * Mth.TWO_PI) * 2F + 0.2F), 0F, 1F);
        f *= 1F - level.getRainLevel(1F) * 5F / 16F;
        f *= 1F - level.getThunderLevel(1F) * level.getRainLevel(1F) * 5F / 16F;
        float sunBrightness = (f * 0.8F + 0.2F) * (float) (1D - eclipse * 0.6D) * (1F - dust);
        float inside = 0F;
        if (war != null) {
            for (CBT_War.Projectile projectile : war.getProjectiles()) {
                if (projectile.getAnimtime() > 0) inside += 100 - projectile.getFlashtime();
            }
        }
        if (atmosphere == null) return sunBrightness + inside;
        return sunBrightness * Mth.clamp(1F - ((float) atmosphere.getPressure() - 1.5F) * 0.2F, 0.25F, 1F) + inside;
    }

    /** WorldProviderCelestial.getStarBrightness: stars shine by day beyond Duna, fully beyond Jool. */
    private static float starBrightness(CelestialBody body, float solarAngle) {
        float distanceFactor = Mth.clamp((body.getPlanet().semiMajorAxisKm - 20_000_000F) / 60_000_000F, 0F, 1F);
        float f = Mth.clamp(1F - (Mth.cos(solarAngle * Mth.TWO_PI) * 2F + 0.25F), 0F, 1F);
        return Mth.clamp(f * f * 0.5F, distanceFactor, 1F);
    }

    /** WorldProviderCelestial.fogDensity: only air thicker than two atmospheres fogs up. */
    private static float fogDensity(@Nullable CBT_Atmosphere atmosphere) {
        if (atmosphere == null) return 0F;
        float pressure = (float) atmosphere.getPressure();
        return pressure <= 2F ? 0F : pressure * pressure * 0.002F;
    }

    /**
     * WorldProviderCelestial.getSkyColor: each gas's colour by its share of the air, flashing white
     * as war projectiles burst, thinner in thin air, darker in an eclipse and dimmed or reddened by
     * impact dust and fires.
     */
    private static float[] skyColor(ClientLevel level, Vec3 cam, float partial, @Nullable CBT_Atmosphere atmosphere, float sun, double eclipse,
            @Nullable CBT_War war, float dust, float fire) {
        float[] color = new float[3];
        if (atmosphere == null) return color;
        float total = (float) atmosphere.getPressure();
        if (total <= 0F) return color;
        for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
            if (entry == null || entry.fluid == null) continue;
            float[] c;
            Fluid f = entry.fluid;
            if (SpaceAtmosphereRender.is(f, "EVEAIR")) {
                c = new float[] {53F / 255F * sun, 32F / 255F * sun, 74F / 255F * sun};
            } else if (SpaceAtmosphereRender.is(f, "DUNAAIR", "CARBONDIOXIDE")) {
                c = new float[] {212F / 255F * sun, 112F / 255F * sun, 78F / 255F * sun};
            } else if (SpaceAtmosphereRender.is(f, "EARTHAIR", "OXYGEN", "NITROGEN")) {
                Vec3 vanilla = level.getSkyColor(cam, partial);
                c = new float[] {(float) vanilla.x, (float) vanilla.y, (float) vanilla.z};
            } else {
                int hex = SpaceEffects.fluidHex(f);
                c = new float[] {(hex >> 16 & 0xFF) / 255F * sun, (hex >> 8 & 0xFF) / 255F * sun, (hex & 0xFF) / 255F * sun};
            }
            float share = (float) entry.pressure / total;
            for (int i = 0; i < 3; i++) color[i] += c[i] * share;
        }
        if (war != null) {
            for (CBT_War.Projectile projectile : war.getProjectiles()) {
                if (projectile.getAnimtime() <= 0) continue;
                float flash = (100 - projectile.getFlashtime()) * 0.5F;
                for (int i = 0; i < 3; i++) color[i] += flash;
            }
        }
        float pressureFactor = Mth.clamp(total, 0F, 1F);
        for (int i = 0; i < 3; i++) color[i] *= pressureFactor;
        if (eclipse > 0) {
            color[0] *= (float) (1 - eclipse * 0.6);
            color[1] *= (float) (1 - eclipse * 0.6);
            color[2] *= (float) (1 - eclipse * 0.5);
        }
        if (dust > 0) {
            if (fire > 0) {
                color[0] *= 1.3F;
                color[1] *= Math.max(1 - dust * 1.4F, 0);
                color[2] *= Math.max(1 - dust * 4, 0);
            } else {
                color[1] *= 1 - dust * 0.5F;
                color[2] *= Math.max(1 - dust * 4, 0);
            }
            for (int i = 0; i < 3; i++) color[i] *= fire + (1 - dust);
        }
        return color;
    }

    /** WorldProviderCelestial.getSunColor: chlorine-like gases redden the star by absorbing blue and green. */
    private static float[] sunColor(@Nullable CBT_Atmosphere atmosphere) {
        float[] color = {1F, 1F, 1F};
        if (atmosphere == null) return color;
        for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
            if (entry == null || !SpaceAtmosphereRender.is(entry.fluid, "TEKTOAIR", "CHLORINE", "CHLOROMETHANE", "RADIOSOLVENT", "CCL")) continue;
            float absorption = Mth.clamp(1F - (float) entry.pressure * 0.5F, 0F, 1F);
            color[1] *= absorption;
            color[2] *= absorption;
        }
        return color;
    }

    /** WorldProviderCelestial.getEclipseFactor: how much of the star the nearest large body covers. */
    static double eclipseFactor(List<AstroMetric> metrics, double sunSize, double maxSize) {
        double factor = 0;
        double sunArc = arc(sunSize);
        for (AstroMetric metric : metrics) {
            if (metric.apparentSize < 1) continue;
            double planetArc = arc(Mth.clamp(metric.apparentSize, 0, maxSize));
            double minPhase = 1 - (planetArc + sunArc);
            double maxPhase = 1 - (planetArc - sunArc);
            if (metric.phaseObscure < minPhase) continue;
            double thisFactor = 1 - (metric.phaseObscure - maxPhase) / (minPhase - maxPhase);
            factor = Math.min(Math.max(factor, thisFactor), 1.0);
        }
        return factor;
    }

    // apparentSize to 0-1 of the sky; the flat quads warp the larger they get
    private static double arc(double apparentSize) {
        return apparentSize * 0.0017D + Math.sqrt(apparentSize * 0.00003D);
    }

    // ---------------------------------------------------------------- passes

    /**
     * The sky disc above, vanilla's (1.7.10's glSkyList) in the sky colour, fogged out to at most
     * twelve chunks as NTM:Space did so the sky looks the same at any render distance.
     */
    private static void skyDisc(Minecraft mc, float[] sky) {
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogStart(0F);
        RenderSystem.setShaderFogEnd(Math.min(12F, mc.options.getEffectiveRenderDistance()) * 16F);
        FogRenderer.levelFogColor();
        RenderSystem.setShaderColor(sky[0], sky[1], sky[2], 1F);
        disc(16F);
        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    /** LevelRenderer.buildSkyDisc: a fan 512 across at height y. */
    private static void disc(float y) {
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        float f = Math.signum(y) * 512F;
        builder.addVertex(0F, y, 0F);
        for (int i = -180; i <= 180; i += 45) {
            builder.addVertex(f * Mth.cos(i * Mth.DEG_TO_RAD), y, 512F * Mth.sin(i * Mth.DEG_TO_RAD));
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    /**
     * Below the horizon: a black disc when under it, then 1.7.10's second sky disc at the horizon in
     * the sky's darkened colour (World.isSkyColored), both fogged as the vanilla sky.
     */
    private static void renderLowerDiscs(ClientLevel level, Vec3 cam, float[] sky) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(0F, 0F, 0F, 1F);
        double heightAboveHorizon = cam.y - level.getLevelData().getHorizonHeight(level);
        if (heightAboveHorizon < 0) {
            modelView.pushMatrix();
            modelView.translate(0F, 12F, 0F);
            disc(-16F);
            modelView.popMatrix();

            float s = 1F, bottom = -(float) (heightAboveHorizon + 65D), top = -s;
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShader(GameRenderer::getPositionShader);
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            b.addVertex(-s, bottom, s);
            b.addVertex(s, bottom, s);
            b.addVertex(s, top, s);
            b.addVertex(-s, top, s);
            b.addVertex(-s, top, -s);
            b.addVertex(s, top, -s);
            b.addVertex(s, bottom, -s);
            b.addVertex(-s, bottom, -s);
            b.addVertex(s, top, -s);
            b.addVertex(s, top, s);
            b.addVertex(s, bottom, s);
            b.addVertex(s, bottom, -s);
            b.addVertex(-s, bottom, -s);
            b.addVertex(-s, bottom, s);
            b.addVertex(-s, top, s);
            b.addVertex(-s, top, -s);
            b.addVertex(-s, top, -s);
            b.addVertex(-s, top, s);
            b.addVertex(s, top, s);
            b.addVertex(s, top, -s);
            BufferUploader.drawWithShader(b.buildOrThrow());
        }
        RenderSystem.setShaderColor(sky[0] * 0.2F + 0.04F, sky[1] * 0.2F + 0.04F, sky[2] * 0.6F + 0.1F, 1F);
        modelView.pushMatrix();
        modelView.translate(0F, -(float) (heightAboveHorizon - 16D), 0F);
        disc(-16F);
        modelView.popMatrix();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.enableBlend();
    }

    /** A black box around the camera, for orbit, where NTM:Space's sky and fog were black. */
    private static void dome(float r, float g, float b) {
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float d = 120F;
        float[][] faces = {
            {-d, -d, -d, d, -d, -d, d, -d, d, -d, -d, d}, {-d, d, -d, -d, d, d, d, d, d, d, d, -d},
            {-d, -d, -d, -d, d, -d, d, d, -d, d, -d, -d}, {-d, -d, d, d, -d, d, d, d, d, -d, d, d},
            {-d, -d, -d, -d, -d, d, -d, d, d, -d, d, -d}, {d, -d, -d, d, d, -d, d, d, d, d, -d, d}
        };
        for (float[] f : faces) {
            for (int i = 0; i < 12; i += 3) builder.addVertex(f[i], f[i + 1], f[i + 2]).setColor(r, g, b, 1F);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    /**
     * The sunrise fan (dimmed by impact dust); in a vacuum a faint cold glow that NTM:Space kept out
     * of the fog colour, with the body's charged dust drifting through it.
     */
    private void renderSunset(Minecraft mc, ClientLevel level, @Nullable CBT_Atmosphere atmosphere, float solarAngle, float pressure, float dust) {
        float[] color;
        if (pressure < 0.05F) {
            color = vacuumGlow(solarAngle);
        } else {
            color = SpaceEffects.sunriseColor(atmosphere, solarAngle);
            if (color != null) {
                color = color.clone();
                for (int i = 0; i < 4; i++) color[i] *= 1F - dust;
            }
        }
        if (color == null) return;
        float direction = Mth.sin(solarAngle * Mth.TWO_PI) < 0F ? 180F : 0F;
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.rotateX(rad(90F));
        modelView.rotateZ(rad(direction));
        modelView.rotateZ(rad(90F));
        RenderSystem.applyModelViewMatrix();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(0F, 100F, 0F).setColor(color[0], color[1], color[2], color[3]);
        int segments = 16;
        for (int j = 0; j <= segments; j++) {
            float angle = j * Mth.TWO_PI / segments;
            float sin = Mth.sin(angle), cos = Mth.cos(angle);
            builder.addVertex(sin * 120F, cos * 120F, -cos * 40F * color[3]).setColor(color[0], color[1], color[2], 0F);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());

        if (pressure < 0.05F) chargedDust(mc, level, color[3]);
        modelView.popMatrix();
    }

    /**
     * The vacuum's charged dust: 1024 grains in the sunrise glow, drifting away from the star.
     * NTM:Space drew them as one-pixel GL points tinted by the surface texture; 1.21.1 has no point
     * primitive, so each grain is a quad one pixel across, in the grain's own colour.
     */
    private void chargedDust(Minecraft mc, ClientLevel level, float alpha) {
        Random rand = new Random(0);
        double time = (level.getDayTime() + (double) partial) * 0.002D;
        float r = 0.5F + rand.nextFloat() * 0.5F;
        float g = 0.5F + rand.nextFloat() * 0.5F;
        float b = 0.5F + rand.nextFloat() * 0.5F;
        float a = Mth.clamp(rand.nextFloat() * alpha * 4F, 0F, 1F);
        if (a <= 0F) return;
        float half = pixel(mc);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < 1024; i++) {
            float x = (float) (rand.nextGaussian() * 50);
            double zA = Math.abs(rand.nextGaussian() * 20) + time;
            double zB = Math.abs(rand.nextGaussian());
            float z = (float) -(zA % zB * 20);
            builder.addVertex(x - half, 100F, z - half).setColor(r, g, b, a);
            builder.addVertex(x + half, 100F, z - half).setColor(r, g, b, a);
            builder.addVertex(x + half, 100F, z + half).setColor(r, g, b, a);
            builder.addVertex(x - half, 100F, z + half).setColor(r, g, b, a);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    /** Half a screen pixel at a distance of 100. */
    private static float pixel(Minecraft mc) {
        float fov = mc.options.fov().get();
        int height = Math.max(1, mc.getWindow().getHeight());
        return (float) (100D * Math.tan(Math.toRadians(fov) * 0.5D) / height);
    }

    private static float @Nullable [] vacuumGlow(float solarAngle) {
        float cutoff = 0.4F;
        float angle = Mth.cos(solarAngle * Mth.TWO_PI);
        if (angle < -cutoff || angle > cutoff) return null;
        float intensity = angle / cutoff * 0.5F + 0.5F;
        float alpha = 1F - (1F - Mth.sin(intensity * Mth.PI)) * 0.99F;
        alpha *= alpha;
        return new float[] {0.9F, 1F, 1F, alpha * 0.2F};
    }

    private void renderStars(Minecraft mc, float starBrightness, float siderealTurns, float axialTilt) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        additive();
        if (NTMSkybox.lodeStarVisible()) {
            float lodeSize = 1F + lodeRandom.nextFloat() * 0.5F;
            modelView.pushMatrix();
            modelView.rotateX(rad(-75F));
            modelView.rotateY(rad(10F));
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            texQuad(LODE_STAR, lodeSize, 100D, 0F, 0F, 1F, 1F, true);
            modelView.popMatrix();
        }

        modelView.pushMatrix();
        modelView.rotateX(rad(axialTilt));
        modelView.rotateY(rad(-90F));
        modelView.rotateX(siderealTurns * Mth.TWO_PI);
        modelView.rotateX(rad(-90F));

        if (starBrightness > 0F) {
            modelView.pushMatrix();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(1F, 1F, 1F, starBrightness * 0.6F);
            modelView.rotateX(rad(90F));
            modelView.rotateZ(rad(-90F));
            skyboxSide(4);
            modelView.pushMatrix();
            modelView.rotateX(rad(90F));
            skyboxSide(1);
            modelView.popMatrix();
            modelView.pushMatrix();
            modelView.rotateX(rad(-90F));
            skyboxSide(0);
            modelView.popMatrix();
            modelView.rotateZ(rad(90F));
            skyboxSide(5);
            modelView.rotateZ(rad(90F));
            skyboxSide(2);
            modelView.rotateZ(rad(90F));
            skyboxSide(3);
            modelView.popMatrix();
        }

        // demeter
        modelView.pushMatrix();
        additive();
        float brightness = Math.max(0.5F, starBrightness * 1.6F);
        RenderSystem.setShaderColor(brightness, brightness, brightness, Math.min(brightness, 1F));
        float digamma = mc.player != null ? (float) HbmLivingProps.getDigamma(mc.player) : 0F;
        float digammaSize = 1F + digamma * 0.25F;
        float digammaDist = 100F - digamma * 2.5F;
        modelView.rotateX(rad(140F));
        modelView.rotateZ(rad(-40F));
        texQuad(DIGAMMA_STAR, digammaSize, digammaDist, 0F, 0F, 1F, 1F, true);
        modelView.popMatrix();

        modelView.popMatrix();
        RenderSystem.defaultBlendFunc();
    }

    /** One face of the star box: night.png is a 3 by 2 atlas of faces. */
    private static void skyboxSide(int side) {
        float u = side % 3 / 3F;
        float v = side / 3 / 2F;
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, NIGHT);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(-100F, -100F, -100F).setUv(u, v);
        builder.addVertex(-100F, -100F, 100F).setUv(u, v + 0.5F);
        builder.addVertex(100F, -100F, 100F).setUv(u + 1F / 3F, v + 0.5F);
        builder.addVertex(100F, -100F, -100F).setUv(u + 1F / 3F, v);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void renderSun(Minecraft mc, ClientLevel level, float partial, CelestialBody sun, @Nullable CBT_Atmosphere atmosphere,
            double sunSize, double coronaSize, float visibility, float pressure) {
        ShaderInstance blackhole = SpaceSkyClient.blackhole;
        if (sun.shader != null && sun.hasTrait(CBT_Destroyed.class)) {
            // BLACK HOLE SUN, WON'T YOU COME
            if (blackhole == null) return;
            double shaderSize = sunSize * sun.shaderScale;
            RenderSystem.defaultBlendFunc();
            Matrix4fStack modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix();
            modelView.rotateY(rad(-90F));
            RenderSystem.setShaderTexture(0, NOISE);
            blackhole.safeGetUniform("iTime").set((float) ((level.getDayTime() + (double) partial) / 20D));
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            shaderQuad(blackhole, shaderSize);
            modelView.popMatrix();
            additive();
            return;
        }

        float[] sunColor = sunColor(atmosphere);
        ResourceLocation texture = SolarSystem.kerbol.texture != null ? SolarSystem.kerbol.texture : FLARE;
        RenderSystem.setShaderColor(sunColor[0], sunColor[1], sunColor[2], visibility);
        texQuad(texture, sunSize, 100D, 0F, 0F, 1F, 1F);

        // a big ol' spiky flare, less so in an atmosphere
        RenderSystem.setShaderColor(sunColor[0], sunColor[1], sunColor[2], 1F - Mth.clamp(pressure, 0F, 1F) * 0.75F);
        texQuad(FLARE, coronaSize, 99.9D, 0F, 0F, 1F, 1F);

        CBT_Dyson dyson = sun.getTrait(CBT_Dyson.class);
        int swarmCount = dyson != null ? dyson.size() : 0;
        if (swarmCount > 0) swarm(mc, level.getDayTime() + (double) partial, (float) sunSize, swarmCount);
        additive();
    }

    private static void renderCelestials(ClientLevel level, float partial, List<AstroMetric> metrics, float[] tint, float visibility,
            float blendAmount, @Nullable CelestialBody orbiting, float maxSize) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        double worldTime = level.getGameTime() + (double) partial;
        double dayTime = level.getDayTime() + (double) partial;

        for (AstroMetric metric : metrics) {
            if (metric.distance == 0) continue;
            CelestialBody other = metric.body;
            // the body a station circles turns beneath it, its tilt lost to the orbit's
            boolean orbitingThis = other == orbiting;
            float uvOffset = orbitingThis ? (float) (1 - ((dayTime / 1024D) % 1D)) : 0F;
            float patternOffset = orbitingThis ? (float) -(dayTime / 1024D) : 0F;
            float axialTilt = orbitingThis ? 0F : other.axialTilt;

            modelView.pushMatrix();
            double size = Mth.clamp(metric.apparentSize, 0, maxSize);
            boolean renderPoint = size < TRANSITION_MAX_SIZE;
            boolean renderBody = size > TRANSITION_MIN_SIZE && other.texture != null;

            modelView.rotateX((float) Math.toRadians(metric.angle));
            modelView.rotateZ((float) Math.toRadians(metric.inclination));
            modelView.rotateY(rad(axialTilt + 90F));

            if (renderBody) {
                float phase = (float) -metric.phase;
                List<SpaceSkySync.ShockStatus> shocks = SpaceSkySync.getClientShocks(other, level.getGameTime());

                // the back half of the ring, hidden by the body
                if (other.hasRings) ringHalf(metric, size, visibility, false);

                CBT_Destroyed destroyed = other.getTrait(CBT_Destroyed.class);
                if (destroyed != null) {
                    renderDestroyed(other, destroyed, size, uvOffset);
                } else {
                    atmosphereGlow(other, size, 1F);

                    RenderSystem.disableBlend();
                    RenderSystem.setShaderColor(1F, 1F, 1F, visibility);
                    texQuad(other.texture, size, 100D, uvOffset, 0F, 1F + uvOffset, 1F);

                    CBT_Impact impact = other.getTrait(CBT_Impact.class);
                    CBT_Lights light = other.getTrait(CBT_Lights.class);
                    double impactTime = impact != null ? level.getGameTime() - impact.time + (double) partial : 0;
                    float impactAnimationTime = impact != null ? (float) impactTime : -1F;
                    int lightIntensity = light != null && impactTime < 40 ? Mth.clamp(light.getIntensity(), 0, CITY_LIGHTS.length - 1) : 0;
                    int blackouts = Math.min((int) (impactTime / 8), 5);
                    float atmosphereTime = (float) (worldTime / 20D);

                    atmosphereSurface(other, size, atmosphereTime, impactAnimationTime, partial, uvOffset, patternOffset, shocks, worldTime);
                    crescent(phase, size, uvOffset);
                    cityGlow(SpaceSkyClient.atmosphereEmissive, true, other, phase, size, lightIntensity, blackouts, atmosphereTime,
                            impactAnimationTime, uvOffset, patternOffset, shocks, worldTime);
                    cityGlow(SpaceSkyClient.nightLights, false, other, phase, size, lightIntensity, blackouts, atmosphereTime,
                            impactAnimationTime, uvOffset, patternOffset, shocks, worldTime);
                    lightning(other, phase, size, atmosphereTime, impactAnimationTime, partial, uvOffset, patternOffset, shocks, worldTime);

                    RenderSystem.enableBlend();
                    additive();
                    if (impact != null) impactScars(impactTime, size, uvOffset);

                    // another layer on top to blend with the atmosphere
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                    colorQuad(size, tint[0] - BLEND_DARKEN, tint[1] - BLEND_DARKEN, tint[2] - BLEND_DARKEN, 1F - blendAmount * visibility);
                }

                // the front half of the ring, unobscured
                if (other.hasRings) ringHalf(metric, size, visibility, true);

                nukeFlashes(size, phase, shocks, worldTime);
            }

            if (renderPoint) {
                float alpha = Mth.clamp((float) size * 100F, 0F, 1F);
                alpha *= 1F - Mth.clamp((float) ((size - TRANSITION_MIN_SIZE) / (TRANSITION_MAX_SIZE - TRANSITION_MIN_SIZE)), 0F, 1F);
                RenderSystem.enableBlend();
                additive();
                RenderSystem.setShaderColor(other.color[0], other.color[1], other.color[2], alpha * visibility);
                texQuad(PLANET, 1D, 100D, 0F, 0F, 1F, 1F);
            }
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            modelView.popMatrix();
        }
        RenderSystem.enableBlend();
    }

    private static void ringHalf(AstroMetric metric, double size, float visibility, boolean front) {
        CelestialBody body = metric.body;
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(body.ringColor[0], body.ringColor[1], body.ringColor[2], visibility);
        float ringSize = (float) (size * body.ringSize);
        modelView.translate(0F, 100F, 0F);
        modelView.rotateZ((float) Math.toRadians(-metric.angle));
        modelView.rotateX(rad(90F - body.ringTilt));
        modelView.rotateY((float) Math.toRadians(metric.angle));
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, RINGS);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        if (front) {
            builder.addVertex(-ringSize, 0F, 0F).setUv(0F, 0.5F);
            builder.addVertex(ringSize, 0F, 0F).setUv(1F, 0.5F);
            builder.addVertex(ringSize, 0F, ringSize).setUv(1F, 1F);
            builder.addVertex(-ringSize, 0F, ringSize).setUv(0F, 1F);
        } else {
            builder.addVertex(-ringSize, 0F, -ringSize).setUv(0F, 0F);
            builder.addVertex(ringSize, 0F, -ringSize).setUv(1F, 0F);
            builder.addVertex(ringSize, 0F, 0F).setUv(1F, 0.5F);
            builder.addVertex(-ringSize, 0F, 0F).setUv(0F, 0.5F);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        modelView.popMatrix();
    }

    /** A body torn apart: thirty shards of its surface and of rubble drifting out, a shockwave and a flash. */
    private static void renderDestroyed(CelestialBody body, CBT_Destroyed d, double size, float uvOffset) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        RenderSystem.enableBlend();
        double progress = d.destProgress + size * 0.5;
        float alpha = (float) (1F - Math.min(1F, progress / 100));
        Random random = new Random(12);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        for (int i = 0; i < 30; i++) {
            double radius = random.nextDouble() * size * d.destProgress;
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI;
            double x = radius * Math.sin(phi) * Math.cos(theta) * 0.7;
            double z = radius * Math.cos(phi) * 0.7;
            float rotation = random.nextFloat() * 360F;
            float uMin = (float) random.nextDouble();
            float vMin = (float) random.nextDouble();
            float uMax = (float) Math.min(uMin + random.nextDouble() * 0.2, 1.0);
            float vMax = (float) Math.min(vMin + random.nextDouble() * 0.2, 1.0);

            modelView.pushMatrix();
            modelView.translate((float) (x * -0.05), 0F, (float) (z * -0.05));
            modelView.rotateY(rad(rotation * d.destProgress * 0.05F));
            texQuad(body.texture, size * random.nextDouble() * 0.1, 100D, uMin, vMin, uMax, vMax);
            modelView.popMatrix();

            modelView.pushMatrix();
            modelView.translate((float) (x * 0.04), 0F, (float) (z * 0.04));
            modelView.rotateY(rad(rotation * d.destProgress * 0.05F));
            texQuad(DESTROYED, size * random.nextDouble() * 0.07, 100D, uMin, vMin, uMax, vMax);
            modelView.popMatrix();
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
        texQuad(SHOCKWAVE, d.destProgress * 0.5 * size * 0.1, 100D, uvOffset, 0F, 1F + uvOffset, 1F);
        RenderSystem.setShaderColor(1F, 1F, 1F, Math.min(1F, alpha * 2));
        texQuad(SHOCK_FLARE, size * 3, 100D, uvOffset, 0F, 1F + uvOffset, 1F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    /** The glowing crater of an impactor, its spreading shockwave and its flash. */
    private static void impactScars(double impactTime, double size, float uvOffset) {
        double lavaAlpha = Math.min(impactTime * 0.1, 1.0);
        double impactSize = impactTime * 0.1 * size * 0.035;
        double impactAlpha = 1.0 - Math.min(1.0, impactTime * 0.0015);
        double flareSize = size * 1.5;
        double flareAlpha = 1.0 - Math.min(1.0, impactTime * 0.002);
        if (lavaAlpha > 0) {
            RenderSystem.setShaderColor(1F, 1F, 1F, (float) lavaAlpha);
            texQuad(IMPACT, size, 100D, uvOffset, 0F, 1F + uvOffset, 1F);
        }
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.translate((float) (-size * 0.5), 0F, (float) (size * 0.4));
        if (impactAlpha > 0) {
            RenderSystem.setShaderColor(1F, 1F, 1F, (float) impactAlpha);
            texQuad(SHOCKWAVE, impactSize, 100D, 0F, 0F, 1F, 1F);
        }
        if (flareAlpha > 0) {
            RenderSystem.setShaderColor(1F, 1F, 1F, (float) flareAlpha);
            texQuad(SHOCK_FLARE, flareSize, 100D, 0F, 0F, 1F, 1F);
        }
        modelView.popMatrix();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    /** The atmosphere's rim: three bands fading out from just inside the body's edge. */
    private static void atmosphereGlow(CelestialBody body, double size, float visibility) {
        float glow = SpaceAtmosphereRender.glowAlpha(body) * visibility;
        if (glow <= 0.001F) return;
        float[] atmo = SpaceAtmosphereRender.atmosphereColor(body);
        float r = Mth.clamp(atmo[0] * 1.15F, 0F, 1F);
        float g = Mth.clamp(atmo[1] * 1.15F, 0F, 1F);
        float b = Mth.clamp(atmo[2] * 1.15F, 0F, 1F);
        float inner = (float) (size * 0.98D);
        float middle = (float) (size * 1.075D);
        float outer = (float) (size * 1.15D * (1.0D + glow * 0.25D));
        float half = glow / 2F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float y = 100F;
        // top band
        bb.addVertex(-outer, y, -outer).setColor(r, g, b, 0F);
        bb.addVertex(outer, y, -outer).setColor(r, g, b, 0F);
        bb.addVertex(middle, y, -middle).setColor(r, g, b, half);
        bb.addVertex(-middle, y, -middle).setColor(r, g, b, half);
        bb.addVertex(-middle, y, -middle).setColor(r, g, b, half);
        bb.addVertex(middle, y, -middle).setColor(r, g, b, half);
        bb.addVertex(inner, y, -inner).setColor(r, g, b, glow);
        bb.addVertex(-inner, y, -inner).setColor(r, g, b, glow);
        // left band
        bb.addVertex(outer, y, -outer).setColor(r, g, b, 0F);
        bb.addVertex(outer, y, outer).setColor(r, g, b, 0F);
        bb.addVertex(middle, y, middle).setColor(r, g, b, half);
        bb.addVertex(middle, y, -middle).setColor(r, g, b, half);
        bb.addVertex(middle, y, -middle).setColor(r, g, b, half);
        bb.addVertex(middle, y, middle).setColor(r, g, b, half);
        bb.addVertex(inner, y, inner).setColor(r, g, b, glow);
        bb.addVertex(inner, y, -inner).setColor(r, g, b, glow);
        // bottom band
        bb.addVertex(outer, y, outer).setColor(r, g, b, 0F);
        bb.addVertex(-outer, y, outer).setColor(r, g, b, 0F);
        bb.addVertex(-middle, y, middle).setColor(r, g, b, half);
        bb.addVertex(middle, y, middle).setColor(r, g, b, half);
        bb.addVertex(middle, y, middle).setColor(r, g, b, half);
        bb.addVertex(-middle, y, middle).setColor(r, g, b, half);
        bb.addVertex(-inner, y, inner).setColor(r, g, b, glow);
        bb.addVertex(inner, y, inner).setColor(r, g, b, glow);
        // right band
        bb.addVertex(-outer, y, outer).setColor(r, g, b, 0F);
        bb.addVertex(-outer, y, -outer).setColor(r, g, b, 0F);
        bb.addVertex(-middle, y, -middle).setColor(r, g, b, half);
        bb.addVertex(-middle, y, middle).setColor(r, g, b, half);
        bb.addVertex(-middle, y, middle).setColor(r, g, b, half);
        bb.addVertex(-middle, y, -middle).setColor(r, g, b, half);
        bb.addVertex(-inner, y, -inner).setColor(r, g, b, glow);
        bb.addVertex(-inner, y, inner).setColor(r, g, b, glow);
        BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    /** AtmosphereRenderUtil.applyNukeShockUniforms: up to four flashes, times since each went off. */
    private static void shockUniforms(ShaderInstance shader, List<SpaceSkySync.ShockStatus> shocks, double time) {
        int count = Math.min(shocks.size(), SpaceSkySync.MAX_ACTIVE_SHOCKS);
        float[] t = {-1F, -1F, -1F, -1F}, cx = {0.5F, 0.5F, 0.5F, 0.5F}, cy = {0.5F, 0.5F, 0.5F, 0.5F}, st = new float[4];
        for (int i = 0; i < count; i++) {
            SpaceSkySync.ShockStatus shock = shocks.get(i);
            t[i] = (float) (time - shock.time());
            cx[i] = shock.centerX();
            cy[i] = shock.centerY();
            st[i] = shock.strength();
        }
        shader.safeGetUniform("nukeShockCount").set(count);
        shader.safeGetUniform("nukeShockTime").set(t[0], t[1], t[2], t[3]);
        shader.safeGetUniform("nukeShockCenterX").set(cx[0], cx[1], cx[2], cx[3]);
        shader.safeGetUniform("nukeShockCenterY").set(cy[0], cy[1], cy[2], cy[3]);
        shader.safeGetUniform("nukeShockStrength").set(st[0], st[1], st[2], st[3]);
    }

    /** The air over the body: clouds, haze or gas-giant bands, by pressure. */
    private static void atmosphereSurface(CelestialBody body, double size, float time, float impactTime, float partial, float uvOffset,
            float patternOffset, List<SpaceSkySync.ShockStatus> shocks, double shockTime) {
        ShaderInstance shader = SpaceSkyClient.atmosphere;
        float alpha = SpaceAtmosphereRender.density(body);
        if (shader == null || alpha <= 0.001F) return;
        float[] atmo = SpaceAtmosphereRender.atmosphereColor(body);
        float[] cloud = SpaceAtmosphereRender.cloudColor(body);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        shader.safeGetUniform("offset").set(uvOffset);
        shader.safeGetUniform("patternOffset").set(patternOffset);
        shader.safeGetUniform("atmosphereColorR").set(atmo[0]);
        shader.safeGetUniform("atmosphereColorG").set(atmo[1]);
        shader.safeGetUniform("atmosphereColorB").set(atmo[2]);
        shader.safeGetUniform("cloudColorR").set(cloud[0]);
        shader.safeGetUniform("cloudColorG").set(cloud[1]);
        shader.safeGetUniform("cloudColorB").set(cloud[2]);
        shader.safeGetUniform("cloudTintStrength").set(SpaceAtmosphereRender.cloudTintStrength(body));
        shader.safeGetUniform("cloudStormDarkness").set(SpaceAtmosphereRender.cloudStormDarkness(body, partial));
        shader.safeGetUniform("atmosphereAlpha").set(alpha);
        shader.safeGetUniform("atmosphereTime").set(time);
        shader.safeGetUniform("atmosphereStyle").set(SpaceAtmosphereRender.style(body));
        shader.safeGetUniform("impactTime").set(impactTime);
        shockUniforms(shader, shocks, shockTime);
        RenderSystem.setShaderTexture(0, body.texture);
        shaderQuad(shader, size);
    }

    /**
     * NTM:Space's crescent shader over a body: black, opaque on the side turned from the star,
     * quantized to 16 by 16 texels, with a thin lit rim near new and a plain disc near full.
     */
    private static void crescent(float phase, double size, float uvOffset) {
        ShaderInstance shader = SpaceSkyClient.crescent;
        if (shader == null) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        shader.safeGetUniform("Phase").set(phase);
        shader.safeGetUniform("Offset").set(uvOffset);
        shaderQuad(shader, size);
    }

    /** City lights on the night side (nightlights), and their glow through thick air (atmosphere_emissive). */
    private static void cityGlow(@Nullable ShaderInstance shader, boolean emissive, CelestialBody body, float phase, double size,
            int lightIntensity, int blackouts, float time, float impactTime, float uvOffset, float patternOffset,
            List<SpaceSkySync.ShockStatus> shocks, double shockTime) {
        if (shader == null || lightIntensity <= 0) return;
        float density = SpaceAtmosphereRender.density(body);
        int style = SpaceAtmosphereRender.style(body);
        if (emissive && (density <= 0.001F
                || (style != SpaceAtmosphereRender.ATMOSPHERE_STYLE_CLOUDS && style != SpaceAtmosphereRender.ATMOSPHERE_STYLE_HAZE))) {
            return;
        }
        RenderSystem.enableBlend();
        additive();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        shader.safeGetUniform("phase").set(phase);
        shader.safeGetUniform("offset").set(uvOffset);
        shader.safeGetUniform("atmosphereDensity").set(density);
        shader.safeGetUniform("patternOffset").set(patternOffset);
        shader.safeGetUniform("atmosphereTime").set(time);
        shader.safeGetUniform("atmosphereStyle").set(style);
        shader.safeGetUniform("impactTime").set(impactTime);
        shader.safeGetUniform("blackouts").set(blackouts);
        shockUniforms(shader, shocks, shockTime);
        RenderSystem.setShaderTexture(0, CITY_LIGHTS[lightIntensity]);
        RenderSystem.setShaderTexture(1, body.cityMask != null ? body.cityMask : DEFAULT_MASK);
        shaderQuad(shader, size);
    }

    private static void lightning(CelestialBody body, float phase, double size, float time, float impactTime, float partial, float uvOffset,
            float patternOffset, List<SpaceSkySync.ShockStatus> shocks, double shockTime) {
        ShaderInstance shader = SpaceSkyClient.lightning;
        float alpha = SpaceAtmosphereRender.density(body);
        float strength = SpaceAtmosphereRender.cloudLightningStrength(body, partial);
        int style = SpaceAtmosphereRender.style(body);
        if (shader == null || alpha <= 0.001F || strength <= 0.001F
                || (style != SpaceAtmosphereRender.ATMOSPHERE_STYLE_CLOUDS && style != SpaceAtmosphereRender.ATMOSPHERE_STYLE_HAZE)) {
            return;
        }
        RenderSystem.enableBlend();
        additive();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        shader.safeGetUniform("phase").set(phase);
        shader.safeGetUniform("offset").set(uvOffset);
        shader.safeGetUniform("patternOffset").set(patternOffset);
        shader.safeGetUniform("cloudTintStrength").set(SpaceAtmosphereRender.cloudTintStrength(body));
        shader.safeGetUniform("cloudLightningStrength").set(strength);
        shader.safeGetUniform("atmosphereAlpha").set(alpha);
        shader.safeGetUniform("atmosphereTime").set(time);
        shader.safeGetUniform("eveFlashStrength").set(SpaceAtmosphereRender.eveFlashStrength(body, time));
        shader.safeGetUniform("atmosphereStyle").set(style);
        shader.safeGetUniform("lightningMode").set(SpaceAtmosphereRender.lightningMode(body));
        shader.safeGetUniform("impactTime").set(impactTime);
        shockUniforms(shader, shocks, shockTime);
        RenderSystem.setShaderTexture(0, body.texture);
        RenderSystem.setShaderTexture(1, body.cityMask != null ? body.cityMask : DEFAULT_MASK);
        shaderQuad(shader, size);
    }

    /** SkyProviderCelestial.renderNukeImpactOverlays: the flash, afterglow and blast ring of recent nukes. */
    private static void nukeFlashes(double size, float phase, List<SpaceSkySync.ShockStatus> shocks, double shockTime) {
        ShaderInstance shader = SpaceSkyClient.nuke;
        if (shader == null || shocks.isEmpty()) return;
        RenderSystem.enableBlend();
        additive();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        shader.safeGetUniform("phase").set(phase);
        shockUniforms(shader, shocks, shockTime);
        shaderQuad(shader, size);
    }

    private static void renderRings(float[] color, float ringSize, float visibility) {
        RenderSystem.setShaderColor(color[0], color[1], color[2], visibility);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, RINGS);
        float offset = -20F;
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(offset, -ringSize, -ringSize).setUv(0F, 0F);
        builder.addVertex(offset, ringSize, -ringSize).setUv(1F, 0F);
        builder.addVertex(offset, ringSize, ringSize).setUv(1F, 1F);
        builder.addVertex(offset, -ringSize, ringSize).setUv(0F, 1F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    // ---------------------------------------------------------------- satellites and stations

    /** What circles the body: Kerbin's delivery satellite, the launched satellites, the held one's orbit, the stations. */
    private static void renderOrbiters(Minecraft mc, ClientLevel level, CelestialBody body, float solarAngle) {
        // JEFF BOZOS WOULD LIKE TO KNOW YOUR LOCATION
        if (level.dimension() == Level.OVERWORLD) {
            renderSatellite(solarAngle, 1.0F, 0.534F, 0.385F, SAT_INCLINATION, SAT_ALTITUDE_KM, SAT_PHASE_OFFSET, SAT_BLINKING, SAT_BLINK_PERIOD);
        }
        for (SpaceSkySync.SkySat sat : SpaceSkySync.clientSatellites.values()) {
            com.hbm.space.satellite.SatSettings c = sat.settings();
            renderSatellite(solarAngle, c.r(), c.g(), c.b(), c.inclination(), c.altitude(), c.phaseOffset(), c.blinking(), c.blinkPeriod());
        }
        renderHeldSatellitePreview(mc, body, solarAngle);
        for (int[] station : SpaceSkySync.clientStations) renderStation(station[0], station[1], solarAngle);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    /** Holding a satellite bound for this body shows the orbit it would take. */
    private static void renderHeldSatellitePreview(Minecraft mc, CelestialBody body, float solarAngle) {
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) return;
        SatelliteType type = SatelliteType.fromItem(held.getItem());
        if (type == null) return;
        // XSatelliteRegistry.getTargetDimensionId: relays orbit Duna, asteroid miners Dres, lunar miners the Mun
        String target = switch (type.getSerializedName()) {
            case "relay" -> "duna";
            case "miner" -> "dres";
            case "lunar_miner" -> "mun";
            default -> body.name;
        };
        if (!target.equals(body.name)) return;
        com.hbm.space.satellite.SatSettings c = com.hbm.space.satellite.SatSettings.of(held, type.getSerializedName());
        renderOrbitLine(solarAngle, c.r(), c.g(), c.b(), c.inclination(), c.altitude(), c.blinking(), c.blinkPeriod());
        renderSatellite(solarAngle, c.r(), c.g(), c.b(), c.inclination(), c.altitude(), c.phaseOffset(), c.blinking(), c.blinkPeriod());
    }

    /** SatelliteBase.renderDefault: a satellite going round every thirty seconds at the default altitude, faster lower. */
    private static void renderSatellite(float solarAngle, float r, float g, float b, float inclination, float altitude, float phaseOffset,
            boolean blinking, float blinkPeriod) {
        double ticks = System.currentTimeMillis() / 50.0D;
        float orbitAngle = orbitAngle(phaseOffset, altitude, ticks / 600.0D * -360.0D, 360F);
        float renderAltitude = Math.max(1F, altitude);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.rotateX(rad(solarAngle * -360F));
        modelView.rotateZ(rad(inclination));
        modelView.rotateX(rad(orbitAngle));
        RenderSystem.setShaderColor(r, g, b, blinkAlpha(blinking, blinkPeriod));
        texQuad(SATELLITE, 0.5D, renderAltitude, 0F, 0F, 1F, 1F, true);
        modelView.popMatrix();
    }

    /** SatelliteBase.renderOrbitLine: the orbit as a faint ring. */
    private static void renderOrbitLine(float solarAngle, float r, float g, float b, float inclination, float altitude, boolean blinking, float blinkPeriod) {
        float renderAltitude = Math.max(1F, altitude);
        float alpha = 0.35F * blinkAlpha(blinking, blinkPeriod);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.rotateX(rad(solarAngle * -360F));
        modelView.rotateZ(rad(inclination));
        RenderSystem.applyModelViewMatrix();
        RenderSystem.lineWidth(1F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= 72; i++) {
            double angle = Math.PI * 2.0D * (i % 72) / 72.0D;
            builder.addVertex(0F, (float) (renderAltitude * Math.cos(angle)), (float) (renderAltitude * Math.sin(angle))).setColor(r, g, b, alpha);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        modelView.popMatrix();
    }

    private static float blinkAlpha(boolean blinking, float blinkPeriod) {
        if (!blinking) return 1F;
        long cycle = (long) (Mth.clamp(blinkPeriod, 0.3F, 1.0F) * 1000F);
        if (cycle <= 0L) return 1F;
        return 1F - (float) (System.currentTimeMillis() % cycle) / cycle;
    }

    /** SatelliteBase.applyPhaseOffsetToOrbitAngle: higher satellites go round slower, as Kepler says. */
    private static float orbitAngle(float phaseOffset, float altitude, double baseAngle, float fullRotation) {
        double orbitSpeed = Math.pow((double) SAT_ALTITUDE_KM / Math.max(1D, altitude), 1.5D);
        float wrappedPhase = phaseOffset % 360F;
        if (wrappedPhase < 0F) wrappedPhase += 360F;
        double angle = baseAngle * orbitSpeed + wrappedPhase / 360D * fullRotation;
        double wrapped = angle % fullRotation;
        if (wrapped < 0D) wrapped += fullRotation;
        return (float) wrapped;
    }

    /** SkyProviderCelestial.renderStation: each station on its own tilted orbit, a lap every 80 seconds. */
    private static void renderStation(int dX, int dZ, float solarAngle) {
        long seed = dX * 1024L + dZ;
        double ticks = (double) (System.currentTimeMillis() % (1600 * 50)) / 50D;
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.rotateX(rad(solarAngle * -360F));
        modelView.rotateX(rad(-40F + (float) (seed % 800) * 0.1F - 5F));
        modelView.rotateY(rad((float) (seed % 50) * 0.1F - 20F));
        modelView.rotateZ(rad((float) (seed % 80) * 0.1F - 2.5F));
        modelView.rotateX((float) Math.toRadians(ticks / 1600D * -360D));
        RenderSystem.setShaderColor(0.8F, 1F, 1F, 1F);
        texQuad(STATION, 0.8D, 100D, 0F, 0F, 1F, 1F, true);
        modelView.popMatrix();
    }

    // ---------------------------------------------------------------- war, meteors, flesh

    /** The war projectiles that arrived: a growing shockwave and Thatmo's shield flaring, fading over 100 ticks. */
    private void renderWar(CBT_War war) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        Vector3f axisA = new Vector3f(-10F, -1F, 50F).normalize();
        Vector3f axisB = new Vector3f(0F, -1F, 1F).normalize();
        for (CBT_War.Projectile projectile : war.getProjectiles()) {
            if (projectile.getTravel() > 0) continue;
            float thing = projectile.getFlashtime() + partial;
            float alpha = 1F - Math.min(1F, thing / 100F);
            for (int pass = 0; pass < 2; pass++) {
                float scale = pass == 0 ? thing : thing * 0.4F;
                modelView.pushMatrix();
                modelView.translate((float) projectile.getTranslateX() + 70F, (float) projectile.getTranslateY(), (float) projectile.getTranslateZ() + 50F);
                modelView.scale(scale);
                modelView.rotate(rad(90F), axisA);
                modelView.rotate(rad(20F), axisB);
                RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
                plane(pass == 0 ? SHOCKWAVE : THATMO_SHIELD);
                modelView.popMatrix();
            }
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    /** ResourceManager.plane: a two by two square in the xz plane. */
    private static void plane(ResourceLocation texture) {
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(-1F, 0F, 1F).setUv(0F, 0F);
        builder.addVertex(1F, 0F, 1F).setUv(1F, 0F);
        builder.addVertex(1F, 0F, -1F).setUv(1F, 1F);
        builder.addVertex(-1F, 0F, -1F).setUv(0F, 1F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    /** The invasion meteors: glowing streaks and their smoke, pulled in to the render distance and scaled to match. */
    private static void renderMeteors(Minecraft mc, Camera camera) {
        if (SpaceSiegeClient.METEORS.isEmpty()) return;
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        Vec3 pos = camera.getPosition();
        double maxDistance = mc.options.getEffectiveRenderDistance() * 16D;
        for (SpaceSiegeClient.Meteor meteor : SpaceSiegeClient.METEORS) {
            double ox = meteor.posX - pos.x, oy = meteor.posY - pos.y, oz = meteor.posZ - pos.z;
            double length = Math.sqrt(ox * ox + oy * oy + oz * oz);
            if (length < 1.0E-4D) continue;
            double distance = Math.min(maxDistance, length);
            double descent = 2017D - meteor.posY;
            float scalar = (float) ((-(descent * descent) + 1517 * descent) / 41 / length);
            modelView.pushMatrix();
            modelView.translate((float) (ox / length * distance), (float) (oy / length * distance), (float) (oz / length * distance));
            modelView.scale(scalar);
            modelView.rotate(camera.rotation());
            if (meteor.type == SpaceSiegeClient.MeteorType.SMOKE) {
                float dark = 1F - Math.min(meteor.age / (100F * 0.35F), 1F);
                RenderSystem.setShaderColor(0.6F * dark, 0.6F * dark, dark, 1F);
                billboard(PARTICLE_BASE);
            } else {
                // renderGlow(1, 1, 1, rain) always comes out at full brightness
                RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                billboard(SHOCK_FLARE);
            }
            modelView.popMatrix();
        }
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private static void billboard(ResourceLocation texture) {
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(-0.5F, -0.25F, 0F).setUv(1F, 0F);
        builder.addVertex(0.5F, -0.25F, 0F).setUv(0F, 0F);
        builder.addVertex(0.5F, 0.75F, 0F).setUv(0F, 1F);
        builder.addVertex(-0.5F, 0.75F, 0F).setUv(1F, 1F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    /** A compromised body's sky is living flesh: the flesh shader on a squashed sphere around the observer. */
    private void renderFlesh(ClientLevel level) {
        ShaderInstance shader = SpaceSkyClient.flesh;
        if (shader == null) return;
        float time = ((float) level.getDayTime() + partial) * 0.2F;
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.scale(194.5F, 70.5F, 94.5F);
        modelView.rotateZ(rad(90F));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, NOISE);
        shader.safeGetUniform("iTime").set(time * 0.05F);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(() -> shader);
        float[] verts = sphere();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (int i = 0; i < verts.length; i += 3) builder.addVertex(verts[i], verts[i + 1], verts[i + 2]);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        modelView.popMatrix();
        additive();
    }

    /** NTM:Space's sphere_v2 mesh as bare quad positions. */
    private static float[] sphere() {
        if (sphere != null) return sphere;
        int n = 0;
        for (com.hbm.render.loader.GroupObject g : com.hbm.main.ResourceManager.sphere_v2.groups)
            n += g.quads(false).length / com.hbm.render.loader.GroupObject.STRIDE * 3;
        float[] v = new float[n];
        int k = 0;
        for (com.hbm.render.loader.GroupObject g : com.hbm.main.ResourceManager.sphere_v2.groups) {
            float[] q = g.quads(false);
            for (int i = 0; i < q.length; i += com.hbm.render.loader.GroupObject.STRIDE) {
                v[k++] = q[i];
                v[k++] = q[i + 1];
                v[k++] = q[i + 2];
            }
        }
        sphere = v;
        return v;
    }

    /**
     * The Dyson swarm: three rings of members, tilted 80 degrees and turned 60 degrees apart, around
     * the star at half its drawn size. NTM:Space drew each member as a one-pixel GL point moved by
     * its swarm vertex shader (keyed by gl_VertexID) and hid the ones behind the star with a depth
     * write of its middle quarter; 1.21.1 has no point primitive, so the same motion is worked out
     * here and each member is a quad one pixel across, with the occlusion tested directly.
     */
    private static void swarm(Minecraft mc, double time, float sunSize, int count) {
        float radius = sunSize * 0.5F;
        float fov = mc.options.fov().get();
        int height = Math.max(1, mc.getWindow().getHeight());
        // members fade with their apparent size, which depends on the resolution and the field of view
        float alpha = Mth.clamp(height / fov * radius * 0.002F, 0F, 1F);
        if (alpha <= 0F) return;
        float t0 = (float) (time / 800D);
        float half = pixel(mc);
        float occluder = sunSize * 0.25F;

        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.defaultBlendFunc();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float[] turns = {0F, 60F, -60F};
        Vector3f p = new Vector3f();
        for (int ring = 0; ring < 3; ring++) {
            Matrix4f m = new Matrix4f().translation(0F, 100F, 0F).scale(radius)
                    .rotateY((float) Math.toRadians(turns[ring])).rotateX((float) Math.toRadians(80));
            for (int i = ring, v = 0; i < count; i += 3, v++) {
                float t = v + t0;
                float r = hash(v), r2 = hash(v + 0.5F), r3 = hash(v + 0.75F);
                float y = ((float) Math.cos(t) * r + (float) Math.sin(t) * (1F - r)) * 0.2F * r2 * r2;
                float o = (float) Math.cos(y * 2F) * (1F - (r3 - 0.5F) * 0.05F);
                float ti = i + t0;
                m.transformPosition((float) Math.cos(ti) * o, y, (float) Math.sin(ti) * o, p);
                if (p.y > 100.1F) {
                    float s = 100.1F / p.y;
                    if (Math.abs(p.x * s) < occluder && Math.abs(p.z * s) < occluder) continue;
                }
                builder.addVertex(p.x - half, p.y, p.z - half).setColor(0F, 0F, 0F, alpha);
                builder.addVertex(p.x + half, p.y, p.z - half).setColor(0F, 0F, 0F, alpha);
                builder.addVertex(p.x + half, p.y, p.z + half).setColor(0F, 0F, 0F, alpha);
                builder.addVertex(p.x - half, p.y, p.z + half).setColor(0F, 0F, 0F, alpha);
            }
        }
        var mesh = builder.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    /** The swarm shader's hash: fract(cos(x * 124.123) * 412). */
    private static float hash(float x) {
        float h = (float) Math.cos(x * 124.123F) * 412F;
        return h - (float) Math.floor(h);
    }

    // ---------------------------------------------------------------- drawing helpers

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    /** BobMathUtil.clerp: an angle lerp the short way round. */
    private static double clerp(double delta, double start, double end) {
        double angle = ((((end - start) % 360) + 540) % 360) - 180;
        return start + angle * delta;
    }

    private static float clampedLerp(float start, float end, float delta) {
        if (delta < 0F) return start;
        return delta > 1F ? end : start + (end - start) * delta;
    }

    private static double smoothstep(double x, double edge0, double edge1) {
        double t = Mth.clamp((x - edge0) / (edge1 - edge0), 0D, 1D);
        return t * t * (3 - 2 * t);
    }

    private static void additive() {
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    private static void texQuad(ResourceLocation texture, double size, double y, float u0, float v0, float u1, float v1) {
        texQuad(texture, size, y, u0, v0, u1, v1, false);
    }

    /** A flat square at height y in the current frame; NTM:Space's star quads ran their v along x. */
    private static void texQuad(ResourceLocation texture, double size, double y, float u0, float v0, float u1, float v1, boolean starUv) {
        float s = (float) size, h = (float) y;
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        if (starUv) {
            builder.addVertex(-s, h, -s).setUv(0F, 0F);
            builder.addVertex(s, h, -s).setUv(0F, 1F);
            builder.addVertex(s, h, s).setUv(1F, 1F);
            builder.addVertex(-s, h, s).setUv(1F, 0F);
        } else {
            builder.addVertex(-s, h, -s).setUv(u0, v0);
            builder.addVertex(s, h, -s).setUv(u1, v0);
            builder.addVertex(s, h, s).setUv(u1, v1);
            builder.addVertex(-s, h, s).setUv(u0, v1);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void colorQuad(double size, float r, float g, float b, float a) {
        float s = (float) size;
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(-s, 100F, -s).setColor(r, g, b, a);
        builder.addVertex(s, 100F, -s).setColor(r, g, b, a);
        builder.addVertex(s, 100F, s).setColor(r, g, b, a);
        builder.addVertex(-s, 100F, s).setColor(r, g, b, a);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void shaderQuad(ShaderInstance shader, double size) {
        float s = (float) size;
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(() -> shader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(-s, 100F, -s).setUv(0F, 0F);
        builder.addVertex(s, 100F, -s).setUv(1F, 0F);
        builder.addVertex(s, 100F, s).setUv(1F, 1F);
        builder.addVertex(-s, 100F, s).setUv(0F, 1F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }
}
