// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.lib.Library;
import com.hbm.space.CelestialBody;
import com.hbm.space.SpaceToggle;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.space.trait.CBT_Water;
import com.hbm.space.trait.CBT_Weather;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The Space dimensions' effects: overworld-like fog and sky, with NTM:Space's cloud renderer
 * (CloudProviderCelestial: one to three cloud layers by air pressure, tinted by the coloured gases
 * in the air, none in thin air or in orbit) and weather renderer (WeatherProviderCelestial: rain
 * and snow in the colour of the body's liquid, only on bodies with a weather cycle).
 */
public final class SpaceEffects extends DimensionSpecialEffects {

    public static final ResourceLocation ID = Library.id("space");

    // NTM:Space left the cloud height at 1.7.10's 128; its worlds are 256 tall like 1.7.10's
    private static final float CLOUD_HEIGHT = 128F;

    private static final ResourceLocation RAIN = ResourceLocation.withDefaultNamespace("textures/environment/rain.png");
    private static final ResourceLocation SNOW = ResourceLocation.withDefaultNamespace("textures/environment/snow.png");

    private static final float CLOUD_BASE_ALPHA = 0.8F;
    private static final float CLOUD_HEIGHT_SHIFT = 0.33F;
    private static final float CLOUD_SCROLL_SPEED = 0.03F;
    private static final float FAST_CLOUD_TILE_SIZE = 32.0F;
    private static final int FAST_CLOUD_RADIUS = 256 / (int) FAST_CLOUD_TILE_SIZE;
    private static final float FANCY_CLOUD_SCALE = 12.0F;
    private static final float FANCY_CLOUD_DEPTH = 4.0F;
    private static final int FANCY_CLOUD_TILE_SIZE = 8;
    private static final int FANCY_CLOUD_RADIUS = 4;
    private static final double LAYER_POSITION_RANGE = 16384.0D;
    private static final float[] LAYER_HEIGHT_OFFSETS = {0.0F, 12.0F, 24.0F};
    private static final float[] LAYER_ALPHA_MULTIPLIERS = {1.0F, 0.84F, 0.76F};
    private static final double[] LAYER_SCROLL_MULTIPLIERS = {1.0D, 0.84D, 0.7D};

    private final float[] rainSizeX = new float[1024];
    private final float[] rainSizeZ = new float[1024];
    private @Nullable VertexBuffer cloudBuffer;

    public SpaceEffects() {
        super(CLOUD_HEIGHT, true, SkyType.NORMAL, false, false);
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 32; ++j) {
                float x = j - 16, z = i - 16;
                float len = Mth.sqrt(x * x + z * z);
                rainSizeX[i << 5 | j] = -z / len;
                rainSizeZ[i << 5 | j] = x / len;
            }
        }
    }

    /** On a planet the fog is its air's colour (SpaceFog.fogColor), lit by the sun there; elsewhere vanilla's. */
    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) {
        Minecraft mc = Minecraft.getInstance();
        CelestialBody planet = mc.level != null ? SpaceFog.planet(mc.level) : null;
        if (planet != null) return SpaceFog.fogColor(mc.level, planet, mc.getTimer().getGameTimeDeltaPartialTick(false));
        return fog.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    /**
     * WorldProviderLaythe and WorldProviderIke.updateLightmap: where the sun doesn't reach, the
     * gas giant overhead lights the night: Jool green on Laythe, Duna red on Ike.
     */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
            float skyLight, int pixelX, int pixelY, org.joml.Vector3f colors) {
        boolean laythe = level.dimension() == com.hbm.space.SpaceDimensions.LAYTHE;
        boolean ike = level.dimension() == com.hbm.space.SpaceDimensions.IKE;
        if (!SpaceToggle.enabled() || (!laythe && !ike)) return;
        CelestialBody body = CelestialBody.getBodyOrNull(level.dimension());
        if (body == null) return;
        float sun = sunBrightness(level, body) - (ike ? 0.1F : 0F);
        float sky = net.minecraft.client.renderer.LightTexture.getBrightness(level.dimensionType(), pixelY);
        float glow = Math.max(sky - sun, 0F);
        if (laythe) colors.y = Math.min(colors.y + glow * 60F / 255F, 1F);
        else colors.x = Math.min(colors.x + glow * 20F / 255F, 1F);
    }

    /** WorldProviderCelestial.getSunBrightness(1): the daylight the world is lit by ({@link SpaceFog#skyDarken}). */
    private static float sunBrightness(ClientLevel level, CelestialBody body) {
        return level.getSkyDarken(1F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    private static @Nullable CelestialBody body(ClientLevel level) {
        if (!SpaceToggle.enabled() || CelestialBody.inOrbit(level)) return null;
        return SpaceSky.observer(level);
    }

    // ---------------------------------------------------------------- sunrise

    /**
     * WorldProviderCelestial.calcSunriseSunsetColors: none in thin air, and the vanilla glow with
     * its colours swizzled by the air (blue Martian sunsets for carbon dioxide, Eve's own green
     * glow, green for chlorine-like gases). This also tints the fog, as in NTM:Space; the vacuum
     * glow, which must not touch the fog, is drawn by the sky alone.
     */
    @Override
    public float @Nullable [] getSunriseColor(float timeOfDay, float partialTicks) {
        if (!SpaceToggle.enabled()) return super.getSunriseColor(timeOfDay, partialTicks);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return super.getSunriseColor(timeOfDay, partialTicks);
        // in orbit there is no air to glow
        if (CelestialBody.inOrbit(mc.level)) return null;
        CelestialBody body = body(mc.level);
        if (body == null) return super.getSunriseColor(timeOfDay, partialTicks);
        float[] colors = sunriseColor(body.getTrait(CBT_Atmosphere.class), timeOfDay);
        if (colors == null) return null;
        // the impact dust dims it
        float dust = com.hbm.handler.ImpactWorldHandler.getDustForClient(mc.level);
        colors = colors.clone();
        for (int i = 0; i < 4; i++) colors[i] *= 1F - dust;
        return colors;
    }

    static float @Nullable [] sunriseColor(@Nullable CBT_Atmosphere atmosphere, float timeOfDay) {
        if (atmosphere == null || atmosphere.getPressure() < 0.05F) return null;
        float cos = Mth.cos(timeOfDay * Mth.TWO_PI);
        if (cos < -0.4F || cos > 0.4F) return null;
        float f = cos / 0.4F * 0.5F + 0.5F;
        float alpha = 1.0F - (1.0F - Mth.sin(f * Mth.PI)) * 0.99F;
        alpha *= alpha;
        float[] colors = {f * 0.3F + 0.7F, f * f * 0.7F + 0.2F, 0.2F, alpha};
        if (has(atmosphere, "DUNAAIR") || has(atmosphere, "CARBONDIOXIDE")) {
            float tmp = colors[0];
            colors[0] = colors[2];
            colors[2] = tmp;
        } else if (has(atmosphere, "EVEAIR")) {
            colors[0] = f * 0.01F;
            colors[1] = f * f * 0.9F + 0.3F;
            colors[2] = f * f;
        } else if (has(atmosphere, "TEKTOAIR") || has(atmosphere, "JOOLGAS") || has(atmosphere, "CHLORINE")) {
            float tmp = colors[1];
            colors[1] = colors[2];
            colors[2] = tmp;
        }
        // getSunriseColor dims it by the impact dust
        return colors;
    }

    private static boolean has(CBT_Atmosphere atmosphere, String fluid) {
        Fluid f = SpaceAtmosphereRender.fluid(fluid);
        return f != null && atmosphere.hasFluid(f);
    }

    // ---------------------------------------------------------------- clouds

    public static int cloudLayerCount(@Nullable CBT_Atmosphere atmosphere) {
        if (atmosphere == null || atmosphere.getPressure() < 0.5F) return 0;
        if (atmosphere.getPressure() >= 5.0F) return 3;
        if (atmosphere.getPressure() >= 2.5F) return 2;
        return 1;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
            double camX, double camY, double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        if (!SpaceToggle.enabled()) return false;
        // in orbit there are no clouds at all, as NTM:Space's orbit sets the cloud height to -99999
        if (CelestialBody.inOrbit(level)) return true;
        CelestialBody body = body(level);
        if (body == null) return false;
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        int layers = cloudLayerCount(atmosphere);
        if (layers <= 0) return true;

        Minecraft mc = Minecraft.getInstance();
        CloudStatus status = mc.options.getCloudsType();
        if (status == CloudStatus.OFF) return true;
        Vec3 color = tintedCloudColor(atmosphere, level.getCloudColor(partialTick));
        double cloudTime = (mc.player != null ? mc.player.tickCount : (int) (level.getGameTime() & Integer.MAX_VALUE)) + partialTick;

        if (cloudBuffer == null) cloudBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        Matrix4f view = new Matrix4f(poseStack.last().pose()).mul(modelViewMatrix);
        FogRenderer.levelFogColor();

        for (int layer = 0; layer < layers && layer < LAYER_HEIGHT_OFFSETS.length; layer++) {
            float height = getCloudHeight() + LAYER_HEIGHT_OFFSETS[layer];
            float alpha = CLOUD_BASE_ALPHA * LAYER_ALPHA_MULTIPLIERS[layer];
            double offsetX = layerOffset(level, layer, 0x4F1BBCDCBFA54001L);
            double offsetZ = layerOffset(level, layer, 0x2C1B3C6D5E7F8103L);
            double scroll = cloudTime * CLOUD_SCROLL_SPEED * LAYER_SCROLL_MULTIPLIERS[layer];
            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
            Matrix4f pose = new Matrix4f(view);
            boolean fancy = status == CloudStatus.FANCY;
            if (fancy) {
                buildFancyLayer(builder, color, height, alpha, camX + scroll + offsetX, camY, camZ + offsetZ);
                pose.scale(FANCY_CLOUD_SCALE, 1F, FANCY_CLOUD_SCALE);
            } else {
                buildFlatLayer(builder, color, height, alpha, camX + scroll + offsetX, camY, camZ + offsetZ);
            }
            var mesh = builder.build();
            if (mesh == null) continue;
            cloudBuffer.bind();
            cloudBuffer.upload(mesh);
            // fancy clouds first fill the depth buffer so the layer's own faces hide each other, as the 1.7.10 colour-masked pass
            for (int pass = fancy ? 0 : 1; pass < 2; pass++) {
                RenderType type = pass == 0 ? RenderType.cloudsDepthOnly() : RenderType.clouds();
                type.setupRenderState();
                ShaderInstance shader = RenderSystem.getShader();
                if (shader != null) cloudBuffer.drawWithShader(pose, projectionMatrix, shader);
                type.clearRenderState();
            }
            VertexBuffer.unbind();
        }
        return true;
    }

    private static void buildFlatLayer(BufferBuilder b, Vec3 color, float cloudHeight, float alpha, double motionX, double camY, double motionZ) {
        float layerY = (float) (cloudHeight - camY + CLOUD_HEIGHT_SHIFT);
        motionX -= Mth.floor(motionX / 2048.0D) * 2048.0D;
        motionZ -= Mth.floor(motionZ / 2048.0D) * 2048.0D;
        float uvX = (float) (motionX * 4.8828125E-4F);
        float uvZ = (float) (motionZ * 4.8828125E-4F);
        float r = (float) color.x, g = (float) color.y, bl = (float) color.z;
        float s = 4.8828125E-4F;
        for (int tileX = -FAST_CLOUD_RADIUS; tileX < FAST_CLOUD_RADIUS; tileX++) {
            for (int tileZ = -FAST_CLOUD_RADIUS; tileZ < FAST_CLOUD_RADIUS; tileZ++) {
                float minX = tileX * FAST_CLOUD_TILE_SIZE, minZ = tileZ * FAST_CLOUD_TILE_SIZE;
                float maxX = minX + FAST_CLOUD_TILE_SIZE, maxZ = minZ + FAST_CLOUD_TILE_SIZE;
                v(b, minX, layerY, maxZ, minX * s + uvX, maxZ * s + uvZ, r, g, bl, alpha, 0F, 1F, 0F);
                v(b, maxX, layerY, maxZ, maxX * s + uvX, maxZ * s + uvZ, r, g, bl, alpha, 0F, 1F, 0F);
                v(b, maxX, layerY, minZ, maxX * s + uvX, minZ * s + uvZ, r, g, bl, alpha, 0F, 1F, 0F);
                v(b, minX, layerY, minZ, minX * s + uvX, minZ * s + uvZ, r, g, bl, alpha, 0F, 1F, 0F);
            }
        }
    }

    private static void buildFancyLayer(BufferBuilder b, Vec3 color, float cloudHeight, float alpha, double camX, double camY, double camZ) {
        double motionX = camX / FANCY_CLOUD_SCALE;
        double motionZ = camZ / FANCY_CLOUD_SCALE + 0.33000001311302185D;
        float y = (float) (cloudHeight - camY + CLOUD_HEIGHT_SHIFT);
        motionX -= Mth.floor(motionX / 2048.0D) * 2048.0D;
        motionZ -= Mth.floor(motionZ / 2048.0D) * 2048.0D;
        float r = (float) color.x, g = (float) color.y, bl = (float) color.z;
        float uBase = Mth.floor(motionX) * 0.00390625F;
        float vBase = Mth.floor(motionZ) * 0.00390625F;
        float xOff = (float) (motionX - Mth.floor(motionX));
        float zOff = (float) (motionZ - Mth.floor(motionZ));
        float t = FANCY_CLOUD_TILE_SIZE, d = FANCY_CLOUD_DEPTH, k = 0.00390625F;

        for (int tileX = -FANCY_CLOUD_RADIUS + 1; tileX <= FANCY_CLOUD_RADIUS; tileX++) {
            for (int tileZ = -FANCY_CLOUD_RADIUS + 1; tileZ <= FANCY_CLOUD_RADIUS; tileZ++) {
                float minX = tileX * t, minZ = tileZ * t;
                float x = minX - xOff, z = minZ - zOff;
                float u0 = minX * k + uBase, u1 = (minX + t) * k + uBase;
                float v0 = minZ * k + vBase, v1 = (minZ + t) * k + vBase;

                if (y > -d - 1.0F) {
                    float c = 0.7F;
                    v(b, x, y, z + t, u0, v1, r * c, g * c, bl * c, alpha, 0F, -1F, 0F);
                    v(b, x + t, y, z + t, u1, v1, r * c, g * c, bl * c, alpha, 0F, -1F, 0F);
                    v(b, x + t, y, z, u1, v0, r * c, g * c, bl * c, alpha, 0F, -1F, 0F);
                    v(b, x, y, z, u0, v0, r * c, g * c, bl * c, alpha, 0F, -1F, 0F);
                }
                if (y <= d + 1.0F) {
                    float top = y + d - 9.765625E-4F;
                    v(b, x, top, z, u0, v0, r, g, bl, alpha, 0F, 1F, 0F);
                    v(b, x + t, top, z, u1, v0, r, g, bl, alpha, 0F, 1F, 0F);
                    v(b, x + t, top, z + t, u1, v1, r, g, bl, alpha, 0F, 1F, 0F);
                    v(b, x, top, z + t, u0, v1, r, g, bl, alpha, 0F, 1F, 0F);
                }
                float cx = 0.9F;
                if (tileX > -1) xFaces(b, x, y, z, minX, uBase, v0, v1, 0.0F, r * cx, g * cx, bl * cx, alpha, -1F);
                if (tileX <= 1) xFaces(b, x, y, z, minX, uBase, v0, v1, 1.0F - 9.765625E-4F, r * cx, g * cx, bl * cx, alpha, 1F);
                float cz = 0.8F;
                if (tileZ > -1) zFaces(b, x, y, z, minZ, vBase, u0, u1, 0.0F, r * cz, g * cz, bl * cz, alpha, -1F);
                if (tileZ <= 1) zFaces(b, x, y, z, minZ, vBase, u0, u1, 1.0F - 9.765625E-4F, r * cz, g * cz, bl * cz, alpha, 1F);
            }
        }
    }

    private static void xFaces(BufferBuilder b, float x, float y, float z, float minX, float uBase, float v0, float v1,
            float offset, float r, float g, float bl, float a, float nx) {
        for (int face = 0; face < FANCY_CLOUD_TILE_SIZE; face++) {
            float fx = x + face + offset;
            float u = (minX + face + 0.5F) * 0.00390625F + uBase;
            v(b, fx, y, z + FANCY_CLOUD_TILE_SIZE, u, v1, r, g, bl, a, nx, 0F, 0F);
            v(b, fx, y + FANCY_CLOUD_DEPTH, z + FANCY_CLOUD_TILE_SIZE, u, v1, r, g, bl, a, nx, 0F, 0F);
            v(b, fx, y + FANCY_CLOUD_DEPTH, z, u, v0, r, g, bl, a, nx, 0F, 0F);
            v(b, fx, y, z, u, v0, r, g, bl, a, nx, 0F, 0F);
        }
    }

    private static void zFaces(BufferBuilder b, float x, float y, float z, float minZ, float vBase, float u0, float u1,
            float offset, float r, float g, float bl, float a, float nz) {
        for (int face = 0; face < FANCY_CLOUD_TILE_SIZE; face++) {
            float fz = z + face + offset;
            float vv = (minZ + face + 0.5F) * 0.00390625F + vBase;
            v(b, x, y + FANCY_CLOUD_DEPTH, fz, u0, vv, r, g, bl, a, 0F, 0F, nz);
            v(b, x + FANCY_CLOUD_TILE_SIZE, y + FANCY_CLOUD_DEPTH, fz, u1, vv, r, g, bl, a, 0F, 0F, nz);
            v(b, x + FANCY_CLOUD_TILE_SIZE, y, fz, u1, vv, r, g, bl, a, 0F, 0F, nz);
            v(b, x, y, fz, u0, vv, r, g, bl, a, 0F, 0F, nz);
        }
    }

    private static void v(BufferBuilder b, float x, float y, float z, float u, float vv,
            float r, float g, float bl, float a, float nx, float ny, float nz) {
        b.addVertex(x, y, z).setUv(u, vv).setColor(r, g, bl, a).setNormal(nx, ny, nz);
    }

    // NTM:Space salted the world seed, which 1.21.1 clients do not know; the dimension id stands in for it
    private static double layerOffset(ClientLevel level, int layer, long salt) {
        long dimension = level.dimension().location().hashCode();
        long mixed = mix((dimension * 0x9E3779B97F4A7C15L) ^ ((long) (layer + 1) * 0xC2B2AE3D27D4EB4FL) ^ salt);
        double normalized = (double) (mixed & ((1L << 53) - 1)) / (double) (1L << 53);
        return (normalized - 0.5D) * LAYER_POSITION_RANGE;
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static boolean isNeutralCloudFluid(Fluid fluid) {
        return fluid == NTMFluids.byName("EARTHAIR") || fluid == NTMFluids.byName("OXYGEN")
                || fluid == NTMFluids.byName("NITROGEN") || fluid == NTMFluids.byName("DUNAAIR")
                || fluid == NTMFluids.byName("CARBONDIOXIDE");
    }

    /** WorldProviderCelestial.getAtmosphereFluidColor for the gases that tint clouds (the neutral ones never reach it). */
    private static double[] fluidColor(Fluid fluid) {
        if (fluid == NTMFluids.byName("EVEAIR")) return new double[] {53F / 255F, 32F / 255F, 74F / 255F};
        return rgb(fluidHex(fluid));
    }

    static int fluidHex(@Nullable Fluid fluid) {
        NTMFluidProperty property = fluid == null ? null : NTMFluidProperties.get(fluid);
        return property != null ? property.colorARGB() & 0xFFFFFF : 0xFFFFFF;
    }

    private static double[] rgb(int hex) {
        return new double[] {(hex >> 16 & 0xFF) / 255D, (hex >> 8 & 0xFF) / 255D, (hex & 0xFF) / 255D};
    }

    /** CloudProviderCelestial.getCloudTintStrength: how much the coloured gases tint the clouds. */
    static float cloudTintStrength(@Nullable CBT_Atmosphere atmosphere) {
        if (atmosphere == null || atmosphere.fluids.isEmpty()) return 0F;
        double total = 0, tint = 0, tr = 0, tg = 0, tb = 0;
        for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
            if (entry == null || entry.fluid == null || entry.pressure <= 0) continue;
            total += entry.pressure;
            if (isNeutralCloudFluid(entry.fluid)) continue;
            double[] c = fluidColor(entry.fluid);
            tint += entry.pressure;
            tr += c[0] * entry.pressure;
            tg += c[1] * entry.pressure;
            tb += c[2] * entry.pressure;
        }
        if (total <= 0 || tint <= 0 || Math.max(tr, Math.max(tg, tb)) <= 0) return 0F;
        return (float) Mth.clamp(tint / total, 0D, 0.65D);
    }

    static Vec3 tintedCloudColor(@Nullable CBT_Atmosphere atmosphere, Vec3 clouds) {
        if (atmosphere == null || atmosphere.fluids.isEmpty()) return clouds;
        double total = 0, tint = 0, tr = 0, tg = 0, tb = 0;
        for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
            if (entry == null || entry.fluid == null || entry.pressure <= 0) continue;
            total += entry.pressure;
            if (isNeutralCloudFluid(entry.fluid)) continue;
            double[] c = fluidColor(entry.fluid);
            tint += entry.pressure;
            tr += c[0] * entry.pressure;
            tg += c[1] * entry.pressure;
            tb += c[2] * entry.pressure;
        }
        if (total <= 0 || tint <= 0) return clouds;
        tr /= tint;
        tg /= tint;
        tb /= tint;
        double peak = Math.max(tr, Math.max(tg, tb));
        if (peak <= 0) return clouds;
        double strength = Mth.clamp(tint / total, 0D, 0.65D);
        double luma = clouds.x * 0.299D + clouds.y * 0.587D + clouds.z * 0.114D;
        double brightness = Mth.clamp(luma * 0.9D, 0D, 1D);
        double cr = Mth.clamp(tr / peak * brightness, 0D, 1D);
        double cg = Mth.clamp(tg / peak * brightness, 0D, 1D);
        double cb = Mth.clamp(tb / peak * brightness, 0D, 1D);
        return new Vec3(
                Mth.clamp(clouds.x + (cr - clouds.x) * strength, 0D, 1D),
                Mth.clamp(clouds.y + (cg - clouds.y) * strength, 0D, 1D),
                Mth.clamp(clouds.z + (cb - clouds.z) * strength, 0D, 1D));
    }

    // ---------------------------------------------------------------- weather

    /** WorldProviderCelestial.getWeatherColor: the body's liquid, desaturated to 35%. */
    private static float[] rainColor(CelestialBody body) {
        CBT_Water water = body.getTrait(CBT_Water.class);
        if (water == null || water.fluid == null) return new float[] {1F, 1F, 1F};
        double[] base = rgb(fluidHex(water.fluid));
        double luma = base[0] * 0.299D + base[1] * 0.587D + base[2] * 0.114D;
        float[] out = new float[3];
        for (int i = 0; i < 3; i++) out[i] = (float) Mth.clamp(luma + (base[i] - luma) * 0.35D, 0D, 1D);
        return out;
    }

    /**
     * ModEventHandlerRenderer.tintWeatherParticles: while it rains on a body, raindrops and their
     * splashes take the rain's colour. Null to leave them be.
     */
    @Nullable
    public static float[] rainParticleColor(ClientLevel level) {
        if (!SpaceToggle.enabled()) return null;
        CelestialBody body = SpaceFog.planet(level);
        if (body == null || !CBT_Weather.supportsWeather(body) || level.getRainLevel(1F) <= 0F) return null;
        return rainColor(body);
    }

    /** WorldProviderCelestial.getSnowColor: white over water, the rain colour over other liquids. */
    private static float[] snowColor(CelestialBody body) {
        CBT_Water water = body.getTrait(CBT_Water.class);
        if (water == null || water.fluid == null || water.fluid == net.minecraft.world.level.material.Fluids.WATER
                || water.fluid == NTMFluids.WATER) {
            return new float[] {1F, 1F, 1F};
        }
        return rainColor(body);
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture,
            double camX, double camY, double camZ) {
        if (!SpaceToggle.enabled()) return false;
        if (CelestialBody.inOrbit(level)) return true;
        CelestialBody body = body(level);
        if (body == null) return false;
        if (!CBT_Weather.supportsWeather(body)) return true;
        float intensity = level.getRainLevel(partialTick);
        if (intensity <= 0.0F) return true;
        ShaderInstance shader = SpaceSkyClient.weather;

        float[] rain = rainColor(body);
        float[] snow = snowColor(body);
        lightTexture.turnOnLightLayer();
        int px = Mth.floor(camX), py = Mth.floor(camY), pz = Mth.floor(camZ);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = null;
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        int range = Minecraft.useFancyGraphics() ? 10 : 5;
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        int layer = -1;
        float time = ticks + partialTick;
        if (shader != null) RenderSystem.setShader(() -> shader);
        else RenderSystem.setShader(GameRenderer::getParticleShader);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int z = pz - range; z <= pz + range; z++) {
            for (int x = px - range; x <= px + range; x++) {
                int idx = (z - pz + 16) * 32 + x - px + 16;
                double sx = rainSizeX[idx] * 0.5D, sz = rainSizeZ[idx] * 0.5D;
                pos.set(x, camY, z);
                Biome biome = level.getBiome(pos).value();
                if (!biome.hasPrecipitation()) continue;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int minY = Math.max(py - range, top);
                int maxY = Math.max(py + range, top);
                int lightY = Math.max(top, py);
                if (minY == maxY) continue;
                RandomSource random = RandomSource.create(x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761);
                pos.set(x, minY, z);
                Biome.Precipitation precipitation = biome.getPrecipitationAt(pos);
                if (precipitation == Biome.Precipitation.NONE) continue;
                boolean isRain = precipitation == Biome.Precipitation.RAIN;
                if (layer != (isRain ? 0 : 1)) {
                    if (layer >= 0 && builder != null) BufferUploader.drawWithShader(builder.buildOrThrow());
                    layer = isRain ? 0 : 1;
                    RenderSystem.setShaderTexture(0, isRain ? RAIN : SNOW);
                    builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                }
                double dx = x + 0.5D - camX, dz = z + 0.5D - camZ;
                float dist = (float) Math.sqrt(dx * dx + dz * dz) / range;
                pos.set(x, lightY, z);
                int light = LevelRenderer.getLightColor(level, pos);
                float x0 = (float) (x - camX - sx + 0.5D), z0 = (float) (z - camZ - sz + 0.5D);
                float x1 = (float) (x - camX + sx + 0.5D), z1 = (float) (z - camZ + sz + 0.5D);
                float yLow = (float) (minY - camY), yHigh = (float) (maxY - camY);
                if (isRain) {
                    int seed = x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 0xFF;
                    float speed = 3.0F + random.nextFloat();
                    float scroll = -((float) ((ticks & 131071) + seed) + partialTick) / 32.0F * speed % 32.0F;
                    float alpha = ((1.0F - dist * dist) * 0.5F + 0.5F) * intensity;
                    builder.addVertex(x0, yHigh, z0).setUv(0F, minY * 0.25F + scroll).setColor(rain[0], rain[1], rain[2], alpha).setLight(light);
                    builder.addVertex(x1, yHigh, z1).setUv(1F, minY * 0.25F + scroll).setColor(rain[0], rain[1], rain[2], alpha).setLight(light);
                    builder.addVertex(x1, yLow, z1).setUv(1F, maxY * 0.25F + scroll).setColor(rain[0], rain[1], rain[2], alpha).setLight(light);
                    builder.addVertex(x0, yLow, z0).setUv(0F, maxY * 0.25F + scroll).setColor(rain[0], rain[1], rain[2], alpha).setLight(light);
                } else {
                    float sway = -((float) (ticks & 511) + partialTick) / 512.0F;
                    float fall = (float) (random.nextDouble() + time * 0.01D * (float) random.nextGaussian());
                    float drift = (float) (random.nextDouble() + (time * (float) random.nextGaussian()) * 0.001D);
                    float alpha = ((1.0F - dist * dist) * 0.3F + 0.5F) * intensity;
                    int sky = (light >> 16 & 0xFFFF) * 3 + 240 >> 2;
                    int block = (light & 0xFFFF) * 3 + 240 >> 2;
                    builder.addVertex(x0, yHigh, z0).setUv(0F + fall, minY * 0.25F + sway + drift).setColor(snow[0], snow[1], snow[2], alpha).setUv2(block, sky);
                    builder.addVertex(x1, yHigh, z1).setUv(1F + fall, minY * 0.25F + sway + drift).setColor(snow[0], snow[1], snow[2], alpha).setUv2(block, sky);
                    builder.addVertex(x1, yLow, z1).setUv(1F + fall, maxY * 0.25F + sway + drift).setColor(snow[0], snow[1], snow[2], alpha).setUv2(block, sky);
                    builder.addVertex(x0, yLow, z0).setUv(0F + fall, maxY * 0.25F + sway + drift).setColor(snow[0], snow[1], snow[2], alpha).setUv2(block, sky);
                }
            }
        }
        if (layer >= 0 && builder != null) BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
        return true;
    }
}
