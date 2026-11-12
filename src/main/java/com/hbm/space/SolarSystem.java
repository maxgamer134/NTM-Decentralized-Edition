// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.lib.Library;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.space.trait.CBT_Temperature;
import com.hbm.space.trait.CBT_Water;
import com.hbm.space.trait.CelestialBodyTrait.CBT_Battlefield;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NTM:Space's Kerbol system: the bodies, their orbits (Keplerian, positions from the world time)
 * and the delta-v rockets need between them. Built once, after the fluids are registered.
 */
public final class SolarSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger("NTM Space");

    public static CelestialBody kerbol;

    public static final double RENDER_SCALE = 180;
    public static final double SUN_RENDER_SCALE = 4;
    public static final float MAX_APPARENT_SIZE_SURFACE = 24;
    public static final float MAX_APPARENT_SIZE_ORBIT = 160;
    public static final double ORRERY_MAX_RADIUS = 20_000;
    public static final double ORRERY_MIN_RADIUS = 2_000;

    private SolarSystem() {}

    private static Fluid fluid(String name) {
        return NTMFluids.byName(name);
    }

    private static ResourceLocation block(String texture) {
        return Library.id("textures/block/" + texture + ".png");
    }

    private static ResourceLocation vanilla(String texture) {
        return ResourceLocation.withDefaultNamespace("textures/block/" + texture + ".png");
    }

    public static synchronized void init() {
        if (kerbol != null || !SpaceToggle.enabled()) return;
        kerbol =
                new CelestialBody("kerbol")
                        .withMassRadius(1.757e28F, 261_600)
                        .withRotationalPeriod(432_000)
                        .withColor(1.0F, 0.8667F, 0.0F)
                        // only drawn once the star is destroyed
                        .withShader(Library.id("shaders/blackhole.frag"), 3)
                        .withSatellites(
                                new CelestialBody("moho", SpaceDimensions.MOHO, Body.MOHO)
                                        .withMassRadius(2.526e21F, 250)
                                        .withOrbitalParameters(5_263_138, 0.2F, 15.0F, 7.0F, 70.0F)
                                        .withRotationalPeriod(210_000)
                                        .withColor(0.4863F, 0.4F, 0.3456F)
                                        .withBlockTextures(block("moho_stone"), block("moho_regolith"))
                                        .withAxialTilt(30F)
                                        .withTraits(new CBT_Temperature(200)),
                                new CelestialBody("eve", SpaceDimensions.EVE, Body.EVE)
                                        .withMassRadius(1.224e23F, 700)
                                        .withOrbitalParameters(9_832_684, 0.01F, 0.0F, 2.1F, 15.0F)
                                        .withRotationalPeriod(80_500)
                                        .withColor(0.408F, 0.298F, 0.553F)
                                        .withBlockTextures(block("eve_stone_2"), block("eve_silt"))
                                        .withMinProcessingLevel(2)
                                        .withTraits(
                                                new CBT_Atmosphere(fluid("EVEAIR"), 5D),
                                                new CBT_Temperature(400),
                                                new CBT_Water(fluid("MERCURY")))
                                        .withSatellites(
                                                new CelestialBody("gilly")
                                                        .withMassRadius(1.242e17F, 13)
                                                        .withOrbitalParameters(31_500, 0.55F, 10.0F, 12.0F, 80.0F)
                                                        .withRotationalPeriod(28_255)
                                                        .withColor(0.612F, 0.494F, 0.439F)),
                                new CelestialBody("kerbin", Level.OVERWORLD, Body.KERBIN)
                                        .withMassRadius(5.292e22F, 600)
                                        .withOrbitalParameters(13_599_840, 0.0F, 0.0F, 0.0F, 0.0F)
                                        .withRotationalPeriod(21_549)
                                        .withColor(0.608F, 0.914F, 1.0F)
                                        .withTraits(new CBT_Atmosphere(fluid("EARTHAIR"), 1D), new CBT_Water())
                                        .withBlockTextures(vanilla("stone"), vanilla("dirt"))
                                        .withCityMask(Library.id("textures/misc/space/kerbin_mask.png"))
                                        .withBiomeMask(Library.id("textures/misc/space/kerbin_biomes.png"))
                                        .withSatellites(
                                                new CelestialBody("mun", SpaceDimensions.MUN, Body.MUN)
                                                        .withMassRadius(9.76e20F, 200)
                                                        .withOrbitalParameters(12_000, 0.054F, 0.0F, 5.15F, 17.0F)
                                                        .withRotationalPeriod(138_984)
                                                        .withColor(0.51F, 0.51F, 0.51F)
                                                        .withTidalLockingTo("kerbin")
                                                        .withBlockTextures(block("moon_rock"), block("moon_turf"))
                                                        .withIce(true),
                                                new CelestialBody("minmus", SpaceDimensions.MINMUS, Body.MINMUS)
                                                        .withMassRadius(2.646e19F, 60)
                                                        .withOrbitalParameters(47_000, 0, 38.0F, 6.0F, 78.0F)
                                                        .withRotationalPeriod(40_400)
                                                        .withColor(0.6F, 0.882F, 0.764F)
                                                        .withBlockTextures(block("minmus_stone"), block("minmus_regolith"))
                                                        .withTraits(new CBT_Water(fluid("MILK")))
                                                        .withIce(true)),
                                new CelestialBody("duna", SpaceDimensions.DUNA, Body.DUNA)
                                        .withMassRadius(4.515e21F, 320)
                                        .withOrbitalParameters(20_726_155, 0.05F, 0.0F, 0.06F, 135.5F)
                                        .withRotationalPeriod(65_518)
                                        .withTidalLockingTo("ike")
                                        .withColor(0.6471F, 0.2824F, 0.1608F)
                                        .withBlockTextures(block("duna_rock"), block("duna_sands"))
                                        .withTraits(new CBT_Atmosphere(fluid("DUNAAIR"), 0.1D))
                                        .withCityMask(Library.id("textures/misc/space/duna_mask.png"))
                                        .withIce(true)
                                        .withSatellites(
                                                new CelestialBody("ike", SpaceDimensions.IKE, Body.IKE)
                                                        .withMassRadius(2.782e20F, 130)
                                                        .withOrbitalParameters(3_200, 0.03F, 0.0F, 0.2F, 0.0F)
                                                        .withBlockTextures(block("ike_stone"), block("ike_regolith"))
                                                        .withRotationalPeriod(65_518)
                                                        .withTidalLockingTo("duna")
                                                        .withColor(0.533F, 0.502F, 0.526F)
                                                        .withTraits(new CBT_Water(fluid("BROMINE")))
                                                        .withIce(true)),
                                new CelestialBody("dres", SpaceDimensions.DRES, Body.DRES)
                                        .withMassRadius(3.219e20F, 138)
                                        .withOrbitalParameters(40_839_348, 0.145F, 90.0F, 5.0F, 280.0F)
                                        .withRotationalPeriod(34_800)
                                        .withColor(0.318F, 0.306F, 0.318F)
                                        .withBlockTextures(block("dresbase"), block("sellafield_slaked"))
                                        .withRings(10.0F, 3, 0.4F, 0.4F, 0.4F)
                                        .withMinProcessingLevel(2)
                                        .withIce(true),
                                new CelestialBody("jool")
                                        .withMassRadius(4.233e24F, 6_000)
                                        .withOrbitalParameters(68_773_560, 0.05F, 0.0F, 1.304F, 52.0F)
                                        .withRotationalPeriod(36_000)
                                        .withColor(0.4588F, 0.6784F, 0.3059F)
                                        .withGas(fluid("JOOLGAS"))
                                        .withSatellites(
                                                new CelestialBody("laythe", SpaceDimensions.LAYTHE, Body.LAYTHE)
                                                        .withMassRadius(2.94e22F, 500)
                                                        .withOrbitalParameters(27_184, 0.0288F, 0.0F, 0.348F, 0.0F)
                                                        .withRotationalPeriod(52_981)
                                                        .withTidalLockingTo("jool")
                                                        .withColor(0.388F, 0.384F, 0.851F)
                                                        .withMinProcessingLevel(3)
                                                        .withTraits(
                                                                new CBT_Atmosphere(fluid("EARTHAIR"), 0.45D)
                                                                        .and(fluid("XENON"), 0.15D),
                                                                new CBT_Water())
                                                        .withBlockTextures(vanilla("stone"), block("laythe_silt"))
                                                        .withCityMask(Library.id("textures/misc/space/laythe_mask.png")),
                                                new CelestialBody("vall")
                                                        .withMassRadius(3.109e21F, 300)
                                                        .withOrbitalParameters(43_152, 0.111F, 342.9F, 7.48F, 128.0F)
                                                        .withRotationalPeriod(105_962)
                                                        .withColor(0.773F, 0.843F, 0.855F),
                                                new CelestialBody("tylo")
                                                        .withMassRadius(4.233e22F, 600)
                                                        .withOrbitalParameters(68_500, 0.002F, 0.0F, 0.3F, 0.0F)
                                                        .withRotationalPeriod(211_926)
                                                        .withColor(0.875F, 0.863F, 0.851F),
                                                new CelestialBody("bop")
                                                        .withMassRadius(3.726e19F, 65)
                                                        .withOrbitalParameters(128_500, 0.235F, 25.0F, 15F, 10.0F)
                                                        .withRotationalPeriod(544_507)
                                                        .withColor(0.329F, 0.302F, 0.263F),
                                                new CelestialBody("pol")
                                                        .withMassRadius(1.081e19F, 44)
                                                        .withOrbitalParameters(179_890, 0.171F, 15.0F, 4.25F, 2.0F)
                                                        .withRotationalPeriod(901_902)
                                                        .withColor(0.541F, 0.478F, 0.373F)),
                                new CelestialBody("sarnus")
                                        .withMassRadius(1.223e24F, 5_300)
                                        .withOrbitalParameters(125_798_522, 0.0534F, 0.0F, 2.02F, 184.0F)
                                        .withRotationalPeriod(28_500)
                                        .withColor(1f, 0.6862f, 0.5882f)
                                        .withRings(10.0F, 3, 0.6F, 0.4F, 0.3F)
                                        .withGas(fluid("SARNUSGAS"))
                                        .withSatellites(
                                                new CelestialBody("hale")
                                                        .withMassRadius(1.2166e16F, 6)
                                                        .withOrbitalParameters(10_488, 0, 0.0F, 1.0F, 55.0F)
                                                        .withRotationalPeriod(23_555)
                                                        .withColor(0.306F, 0.259F, 0.235F),
                                                new CelestialBody("ovok")
                                                        .withMassRadius(4.233e17F, 26)
                                                        .withOrbitalParameters(12_169, 0.01F, 0.0F, 1.5F, 55.0F)
                                                        .withRotationalPeriod(29_440)
                                                        .withColor(0.361F, 0.361F, 0.361F),
                                                new CelestialBody("eeloo")
                                                        .withMassRadius(1.115e21F, 210)
                                                        .withOrbitalParameters(19_106, 0.0034F, 0.0F, 2.3F, 55.0F)
                                                        .withRotationalPeriod(57_915)
                                                        .withColor(0.62F, 0.647F, 0.655F),
                                                new CelestialBody("slate")
                                                        .withMassRadius(2.965e22F, 540)
                                                        .withOrbitalParameters(42_593, 0.04F, 0.0F, 2.3F, 55.0F)
                                                        .withRotationalPeriod(192_771)
                                                        .withColor(0.81F, 0.62F, 0.483F),
                                                new CelestialBody("tekto", SpaceDimensions.TEKTO, Body.TEKTO)
                                                        .withMassRadius(2.883e21F, 480)
                                                        .withOrbitalParameters(67_355, 0.028F, 0.0F, 9.4F, 55.0F)
                                                        .withRotationalPeriod(57_915)
                                                        .withColor(0.357F, 0.47F, 0.313F)
                                                        .withAxialTilt(25F)
                                                        .withMinProcessingLevel(3)
                                                        .withTraits(
                                                                new CBT_Atmosphere(fluid("TEKTOAIR"), 1.5F),
                                                                new CBT_Water(fluid("CCL")))
                                                        .withBlockTextures(block("basalt"), block("rubber_silt"))),
                                new CelestialBody("urlum")
                                        .withMassRadius(1.7896e23F, 2_177)
                                        .withOrbitalParameters(254_317_012, 0.045F, 0.0F, 0.64F, 61.0F)
                                        .withRotationalPeriod(41_000)
                                        .withColor(0.67F, 0.86F, 0.922F)
                                        .withRings(6.0F, 3, 0.6F, 0.8F, 0.9F)
                                        .withGas(fluid("UGAS"))
                                        .withSatellites(
                                                new CelestialBody("polta")
                                                        .withMassRadius(1.3512e21F, 220)
                                                        .withOrbitalParameters(11_728, 0.002F, 0.60F, 2.45F, 40.0F)
                                                        .withRotationalPeriod(73_017)
                                                        .withColor(0.31F, 0.377F, 0.33F),
                                                new CelestialBody("priax")
                                                        .withMassRadius(5.0691e19F, 74)
                                                        .withOrbitalParameters(11_728, 0.002F, 0.0F, 15.5F, 40.0F)
                                                        .withRotationalPeriod(73_017)
                                                        .withColor(0.251F, 0.24F, 0.212F),
                                                new CelestialBody("wal")
                                                        .withMassRadius(7.4427e21F, 370)
                                                        .withOrbitalParameters(67_553, 0.023F, 0.0F, 1.9F, 40.0F)
                                                        .withRotationalPeriod(1_009_410)
                                                        .withColor(0.502F, 0.42F, 0.224F)
                                                        .withSatellites(
                                                                new CelestialBody("tal")
                                                                        .withMassRadius(3.2e18F, 22)
                                                                        .withOrbitalParameters(3_109, 0.0F, 0.0F, 1.9F, 40.0F)
                                                                        .withRotationalPeriod(48_874)
                                                                        .withColor(0.596F, 0.51F, 0.416F))),
                                new CelestialBody("neidon")
                                        .withMassRadius(2.1228e23F, 2_145)
                                        .withOrbitalParameters(409_355_192, 0.0534F, 0.0F, 2.02F, 184.0F)
                                        .withRotationalPeriod(40_250)
                                        .withColor(0.424F, 0.298F, 0.659F)
                                        .withGas(fluid("NGAS"))
                                        .withSatellites(
                                                new CelestialBody("thatmo")
                                                        .withMassRadius(2.788e21F, 286)
                                                        .withOrbitalParameters(32_301, 0.0534F, 0.0F, 4.02F, 284.0F)
                                                        .withRotationalPeriod(306_443)
                                                        .withColor(0.757F, 0.765F, 0.773F)
                                                        .withTraits(
                                                                new CBT_Atmosphere(fluid("NITROGEN"), 0.005F),
                                                                new CBT_Battlefield())
                                                        .withIce(true),
                                                new CelestialBody("nissee")
                                                        .withMassRadius(5.951e18F, 30)
                                                        .withOrbitalParameters(487_744, 0.0534F, 0.0F, 45.02F, 84.0F)
                                                        .withRotationalPeriod(27_924)
                                                        .withColor(0.173F, 0.149F, 0.118F)
                                                        .withMinProcessingLevel(3)),
                                new CelestialBody("plock")
                                        .withMassRadius(7.768e20F, 189)
                                        .withOrbitalParameters(535_833_706, 0.26F, 0.50F, 6.15F, 260.0F)
                                        .withRotationalPeriod(106_309)
                                        .withColor(0.737F, 0.592F, 0.443F)
                                        .withSatellites(
                                                new CelestialBody("karen")
                                                        .withMassRadius(7.014e19F, 85)
                                                        .withOrbitalParameters(2_458, 0.0F, 50.0F, 0.0F, 260.0F)
                                                        .withRotationalPeriod(106_327)
                                                        .withColor(0.483F, 0.392F, 0.341F)));
        logChecks();
    }

    /** The bodies with a dimension; ORBIT stands for the orbit dimension and has no body. */
    public enum Body {
        ORBIT(""),
        KERBIN("kerbin"),
        MUN("mun"),
        MINMUS("minmus"),
        DUNA("duna"),
        MOHO("moho"),
        DRES("dres"),
        EVE("eve"),
        IKE("ike"),
        LAYTHE("laythe"),
        TEKTO("tekto");

        public final String name;
        private @Nullable CelestialBody body;

        Body(String name) {
            this.name = name;
        }

        public @Nullable CelestialBody getBody() {
            if (this == ORBIT) return null;
            if (body == null) body = CelestialBody.getBody(name);
            return body;
        }

        public int getProcessingLevel(CelestialBody from) {
            return this == ORBIT ? 0 : getBody().getProcessingLevel(from);
        }

        public @Nullable ResourceLocation getStoneTexture() {
            return this == ORBIT ? null : getBody().stoneTexture;
        }

        public @Nullable ResourceLocation getSurfaceTexture() {
            return this == ORBIT ? null : getBody().surfaceTexture;
        }

        public ResourceKey<Level> getDimension() {
            return this == ORBIT ? SpaceDimensions.ORBIT : getBody().dimension;
        }
    }

    /** Where a body appears in the sky from somewhere else. */
    public static class AstroMetric {
        public double distance;
        public double angle;
        public double inclination;
        public double apparentSize;
        public double phase;
        public double phaseObscure;
        protected Vec3 position;
        public final CelestialBody body;

        public AstroMetric(CelestialBody body, Vec3 position) {
            this.body = body;
            this.position = position;
        }
    }

    /** A body's place on the orrery, and its orbit drawn as a loop. */
    public static class OrreryMetric {
        private static final int PATH_RESOLUTION = 32;
        public final Vec3 position;
        public final Vec3[] orbitalPath = new Vec3[PATH_RESOLUTION];
        public final CelestialBody body;

        public OrreryMetric(CelestialBody body, Vec3 position) {
            this.body = body;
            this.position = position;
        }
    }

    // celestial mechanics

    private static double ticks(Level level, double partialTicks) {
        return (level.getGameTime() + partialTicks) * AstronomyUtil.TIME_MULTIPLIER;
    }

    /** Farthest first, so the sky draws near bodies over far ones. */
    private static void sortFarthestFirst(List<AstroMetric> metrics) {
        // NTM:Space compared (int) (b.distance - a.distance), which breaks the comparator contract
        // for bodies under a kilometre apart and can make the sort throw
        metrics.sort((a, b) -> Double.compare(b.distance, a.distance));
    }

    public static List<OrreryMetric> calculatePositionsOrrery(Level level, float partialTicks) {
        List<OrreryMetric> metrics = new ArrayList<>();
        calculatePositionsRecursiveOrrery(metrics, null, CelestialBody.getBody(level).getStar(), ticks(level, partialTicks));
        return metrics;
    }

    public static List<AstroMetric> calculateMetricsFromBody(
            Level level, float partialTicks, CelestialBody body, float solarAngle) {
        List<AstroMetric> metrics = new ArrayList<>();
        double rotOffset = -120.0;
        CelestialBody tidalLockedBody =
                body.tidallyLockedTo != null ? CelestialBody.getBody(body.tidallyLockedTo) : null;
        if (tidalLockedBody != null && tidalLockedBody.getPlanet() != body) {
            tidalLockedBody = body;
            rotOffset += 180.0;
        }
        calculatePositionsRecursive(
                metrics, null, body.getStar(), ticks(level, partialTicks), tidalLockedBody, solarAngle * 360.0, rotOffset);
        calculateMetricsFromBody(metrics, body);
        sortFarthestFirst(metrics);
        return metrics;
    }

    public static List<AstroMetric> calculateMetricsFromSatellite(
            Level level, float partialTicks, CelestialBody orbiting, double altitude) {
        List<AstroMetric> metrics = new ArrayList<>();
        double ticks = ticks(level, partialTicks);
        calculatePositionsRecursive(metrics, null, orbiting.getStar(), ticks);
        Vec3 position = calculatePositionSatellite(orbiting, altitude, ticks);
        for (AstroMetric metric : metrics) {
            if (metric.body == orbiting) {
                position = position.add(metric.position);
                break;
            }
        }
        calculateMetricsFromPosition(metrics, position);
        sortFarthestFirst(metrics);
        return metrics;
    }

    public static List<AstroMetric> calculateMetricsBetweenSatelliteOrbits(
            Level level,
            float partialTicks,
            CelestialBody from,
            CelestialBody to,
            double fromAltitude,
            double toAltitude,
            double t) {
        List<AstroMetric> metrics = new ArrayList<>();
        double ticks = ticks(level, partialTicks);
        calculatePositionsRecursive(metrics, null, from.getStar(), ticks);
        Vec3 fromPos = calculatePositionSatellite(from, fromAltitude, ticks);
        Vec3 toPos = calculatePositionSatellite(to, toAltitude, ticks);
        for (AstroMetric metric : metrics) {
            if (metric.body == from) fromPos = fromPos.add(metric.position);
            if (metric.body == to) toPos = toPos.add(metric.position);
        }
        calculateMetricsFromPosition(metrics, lerp(fromPos, toPos, t));
        sortFarthestFirst(metrics);
        return metrics;
    }

    public static double calculateDistanceBetweenTwoBodies(Level level, CelestialBody from, CelestialBody to) {
        List<AstroMetric> metrics = new ArrayList<>();
        calculatePositionsRecursive(metrics, null, from.getStar(), ticks(level, 0));
        Vec3 fromPos = Vec3.ZERO;
        Vec3 toPos = Vec3.ZERO;
        for (AstroMetric metric : metrics) {
            if (metric.body == from) fromPos = metric.position;
            if (metric.body == to) toPos = metric.position;
        }
        return fromPos.distanceTo(toPos);
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, double t) {
        double c = Mth.clamp(t, 0, 1);
        return new Vec3(Mth.lerp(c, from.x, to.x), Mth.lerp(c, from.y, to.y), Mth.lerp(c, from.z, to.z));
    }

    private static void calculatePositionsRecursive(
            List<AstroMetric> metrics, @Nullable AstroMetric parentMetric, CelestialBody body, double ticks) {
        calculatePositionsRecursive(metrics, parentMetric, body, ticks, null, 0, 0);
    }

    private static void calculatePositionsRecursive(
            List<AstroMetric> metrics,
            @Nullable AstroMetric parentMetric,
            CelestialBody body,
            double ticks,
            @Nullable CelestialBody lockBody,
            double solarAngle,
            double rotOffset) {
        Vec3 parentPosition = parentMetric != null ? parentMetric.position : Vec3.ZERO;
        for (CelestialBody satellite : body.satellites) {
            Vec3 position =
                    satellite == lockBody
                            ? calculatePositionFromAngle(
                                    satellite,
                                    solarAngle
                                            + calculateSiderealAngle(satellite, ticks)
                                            - Math.toDegrees(satellite.argumentPeriapsis)
                                            - Math.toDegrees(satellite.ascendingNode)
                                            + rotOffset)
                            : calculatePositionFromTime(satellite, ticks);
            AstroMetric metric = new AstroMetric(satellite, position.add(parentPosition));
            metrics.add(metric);
            calculatePositionsRecursive(metrics, metric, satellite, ticks, lockBody, solarAngle, rotOffset);
        }
    }

    private static void calculatePositionsRecursiveOrrery(
            List<OrreryMetric> metrics, @Nullable OrreryMetric parentMetric, CelestialBody body, double ticks) {
        Vec3 parentPosition = parentMetric != null ? parentMetric.position : Vec3.ZERO;
        double distance = Math.min(body.radiusKm, ORRERY_MAX_RADIUS) * 1.42;
        for (CelestialBody satellite : body.satellites) {
            double extraDistance =
                    Mth.clamp(satellite.radiusKm, ORRERY_MIN_RADIUS, ORRERY_MAX_RADIUS)
                            * ((1 + satellite.eccentricity) * 2)
                            * 1.42;
            for (CelestialBody inner : satellite.satellites)
                extraDistance +=
                        Mth.clamp(inner.radiusKm, ORRERY_MIN_RADIUS, ORRERY_MAX_RADIUS)
                                * ((1 + inner.eccentricity) * 2)
                                * 2;
            distance += extraDistance;
            OrreryMetric metric =
                    new OrreryMetric(satellite, calculatePositionFromTime(satellite, ticks, distance).add(parentPosition));
            for (int i = 0; i < metric.orbitalPath.length; i++)
                metric.orbitalPath[i] =
                        calculatePositionFromAngle(satellite, (float) i / metric.orbitalPath.length * 360, distance)
                                .add(parentPosition);
            metrics.add(metric);
            distance += extraDistance;
            calculatePositionsRecursiveOrrery(metrics, metric, satellite, ticks);
        }
    }

    private static Vec3 calculatePositionFromTime(CelestialBody body, double ticks) {
        return calculatePositionFromTime(body, ticks, body.semiMajorAxisKm);
    }

    private static Vec3 calculatePositionFromTime(CelestialBody body, double ticks, double semiMajorAxis) {
        double yearTicks = body.getOrbitalPeriod() * AstronomyUtil.TICKS_IN_DAY;
        double meanAnomaly = 2 * Math.PI * (ticks / yearTicks);
        return calculatePosition(body, meanAnomaly, semiMajorAxis);
    }

    private static Vec3 calculatePositionFromAngle(CelestialBody body, double angle) {
        return calculatePosition(body, Math.toRadians(angle), body.semiMajorAxisKm);
    }

    private static Vec3 calculatePositionFromAngle(CelestialBody body, double angle, double semiMajorAxis) {
        return calculatePosition(body, Math.toRadians(angle), semiMajorAxis);
    }

    private static Vec3 calculatePosition(CelestialBody body, double meanAnomaly, double semiMajorAxis) {
        double eccentricAnomaly = calculateEccentricAnomaly(meanAnomaly, body.eccentricity);
        double x = semiMajorAxis * (Math.cos(eccentricAnomaly) - body.eccentricity);
        double y = semiMajorAxis * body.semiMinorAxisFactor * Math.sin(eccentricAnomaly);

        double px = x;
        x = Math.cos(body.argumentPeriapsis) * px - Math.sin(body.argumentPeriapsis) * y;
        y = Math.sin(body.argumentPeriapsis) * px + Math.cos(body.argumentPeriapsis) * y;

        double z = Math.sin(body.inclination) * y;
        y = Math.cos(body.inclination) * y;

        px = x;
        x = Math.cos(body.ascendingNode) * px - Math.sin(body.ascendingNode) * y;
        y = Math.sin(body.ascendingNode) * px + Math.cos(body.ascendingNode) * y;
        return new Vec3(x, y, z);
    }

    private static Vec3 calculatePositionSatellite(CelestialBody body, double altitude, double ticks) {
        double orbitalPeriod =
                2 * Math.PI
                        * Math.sqrt(altitude * altitude * altitude / (AstronomyUtil.GRAVITATIONAL_CONSTANT * (double) body.massKg));
        orbitalPeriod /= AstronomyUtil.SECONDS_IN_KSP_DAY;
        double orbitTicks = orbitalPeriod * AstronomyUtil.TICKS_IN_DAY;
        double meanAnomaly = 2 * Math.PI * (ticks / orbitTicks);
        return new Vec3(altitude / 1000 * Math.cos(meanAnomaly), altitude / 1000 * Math.sin(meanAnomaly), 0);
    }

    /**
     * Solves Kepler's equation M = E - e sin E. NTM:Space ran four fixed-point steps, which for
     * eccentric orbits (Gilly at 0.55, Plock at 0.26) is still off by degrees and makes those
     * bodies jitter near periapsis; Newton's method converges in a few steps at any eccentricity.
     */
    static double calculateEccentricAnomaly(double meanAnomaly, float eccentricity) {
        double e = eccentricity;
        double anomaly = e < 0.8 ? meanAnomaly : Math.PI;
        for (int i = 0; i < 8; i++) {
            double delta = (anomaly - e * Math.sin(anomaly) - meanAnomaly) / (1 - e * Math.cos(anomaly));
            anomaly -= delta;
            if (Math.abs(delta) < 1e-10) break;
        }
        return anomaly;
    }

    private static void calculateMetricsFromBody(List<AstroMetric> metrics, CelestialBody body) {
        AstroMetric from = null;
        for (AstroMetric metric : metrics) {
            if (metric.body == body) {
                from = metric;
                break;
            }
        }
        // the star is never in its own list: seen from it, everything is measured from the origin
        Vec3 origin = from != null ? from.position : Vec3.ZERO;
        for (AstroMetric to : metrics) if (from != to) calculateMetric(to, origin);
    }

    private static void calculateMetricsFromPosition(List<AstroMetric> metrics, Vec3 position) {
        for (AstroMetric to : metrics) calculateMetric(to, position);
    }

    private static void calculateMetric(AstroMetric metric, Vec3 position) {
        metric.distance = position.distanceTo(metric.position);
        metric.apparentSize = getApparentSize(Math.min(metric.body.radiusKm, 3_000), metric.distance);
        metric.angle = getApparentAngleDegrees(position, metric.position);
        metric.inclination = getApparentInclinationDegrees(position, metric.position);
        metric.phase = getPhase(position, metric.position);
        metric.phaseObscure = getPhaseObscure(position, metric.position);
    }

    private static double getApparentSize(double radius, double distance) {
        return 2D * (float) Math.atan((2D * radius) / (2D * distance)) * RENDER_SCALE;
    }

    private static double getApparentAngleDegrees(Vec3 from, Vec3 to) {
        double angleToOrigin = Math.atan2(-from.y, -from.x);
        double angleToTarget = Math.atan2(to.y - from.y, to.x - from.x);
        return Mth.wrapDegrees(Math.toDegrees(angleToOrigin - angleToTarget));
    }

    private static double getApparentInclinationDegrees(Vec3 from, Vec3 to) {
        double x = from.x - to.x;
        double y = from.y - to.y;
        double planeDistance = Math.sqrt(x * x + y * y);
        double offsetDistance = from.z - to.z;
        return Mth.wrapDegrees(Math.toDegrees(Math.atan2(offsetDistance, planeDistance)));
    }

    private static double getPhase(Vec3 from, Vec3 to) {
        return getApparentAngleDegrees(to, from) / 180.0;
    }

    private static double getPhaseObscure(Vec3 from, Vec3 to) {
        return Math.min(
                Math.abs(getPhase(from, to)), 1.0F - Math.abs(getApparentInclinationDegrees(from, to) / 180.0F));
    }

    public static double calculateSunSize(CelestialBody from) {
        if (from.parent == null) return 0;
        if (from.parent.parent != null) return calculateSunSize(from.parent);
        return getApparentSize(from.parent.radiusKm, from.semiMajorAxisKm);
    }

    public static double calculateSingleAngle(List<AstroMetric> metrics, CelestialBody from, CelestialBody to) {
        AstroMetric metricFrom = null;
        AstroMetric metricTo = null;
        for (AstroMetric metric : metrics) {
            if (metric.body == from) metricFrom = metric;
            else if (metric.body == to) metricTo = metric;
        }
        if (metricFrom == null || metricTo == null) return 0;
        return getApparentAngleDegrees(metricFrom.position, metricTo.position);
    }

    public static double calculateSingleAngle(
            Level level, double partialTicks, List<AstroMetric> metrics, CelestialBody orbiting, double altitude) {
        Vec3 from = calculatePositionSatellite(orbiting, altitude, ticks(level, partialTicks));
        Vec3 to = Vec3.ZERO;
        for (AstroMetric metric : metrics) {
            if (metric.body == orbiting) {
                to = metric.position;
                from = from.add(to);
                break;
            }
        }
        return getApparentAngleDegrees(from, to);
    }

    public static double calculateSingleAngle(Level level, CelestialBody from, CelestialBody to) {
        List<AstroMetric> metrics = new ArrayList<>();
        calculatePositionsRecursive(metrics, null, from.getStar(), ticks(level, 0));
        return calculateSingleAngle(metrics, from, to);
    }

    public static double calculateSiderealAngle(Level level, float partialTicks, CelestialBody body) {
        return calculateSiderealAngle(body, ticks(level, partialTicks));
    }

    public static double calculateSiderealAngle(CelestialBody body, double ticks) {
        Vec3 position = calculatePositionFromTime(body.getPlanet(), ticks);
        return Math.toDegrees(Math.atan2(position.y, position.x));
    }

    // delta-v

    public static int getCostBetween(
            CelestialBody from, CelestialBody to, int mass, int thrust, int isp, boolean fromOrbit, boolean toOrbit) {
        double fromDrag = getAtmosphericDrag(from.getTrait(CBT_Atmosphere.class));
        double toDrag = getAtmosphericDrag(to.getTrait(CBT_Atmosphere.class));
        double launchDV = fromOrbit ? 0 : getLiftoffDeltaV(from, mass, thrust, fromDrag);
        double travelDV = getDeltaVBetween(from, to);
        double landerDV = toOrbit ? 0 : getLandingDeltaV(to, mass, thrust, toDrag);
        return getFuelCost(launchDV + travelDV + landerDV, mass, isp);
    }

    /** Propellant for a burn in millibuckets of each of the two propellants, rounded up to 100. */
    public static int getFuelCost(double deltaV, int mass, int isp) {
        double exhaustVelocity = isp * 9.81;
        double massFraction = 1 - Math.exp(-(deltaV / exhaustVelocity));
        double totalMass = mass / (1 - massFraction);
        double propellantVolume = (totalMass - mass) / 2;
        if (!(propellantVolume + 100 <= Integer.MAX_VALUE)) return Integer.MAX_VALUE;
        return Mth.ceil(propellantVolume * 0.01D) * 100;
    }

    private static double getAtmosphericDrag(@Nullable CBT_Atmosphere atmosphere) {
        if (atmosphere == null) return 0;
        return Math.log(atmosphere.getPressure() + 1.0D) / 10.0D;
    }

    public static double getLiftoffDeltaV(CelestialBody body, float craftMassKg, float craftThrustN, double drag) {
        return calculateSurfaceToOrbitDeltaV(body, craftMassKg, craftThrustN, drag, false);
    }

    public static double getLandingDeltaV(CelestialBody body, float craftMassKg, float craftThrustN, double drag) {
        return calculateSurfaceToOrbitDeltaV(body, craftMassKg, craftThrustN, drag, drag > 0.006);
    }

    private static double calculateSurfaceToOrbitDeltaV(
            CelestialBody body, float craftMassKg, float craftThrustN, double drag, boolean lossesOnly) {
        float gravity = body.getSurfaceGravity();
        double orbitalDeltaV =
                Math.sqrt(AstronomyUtil.GRAVITATIONAL_CONSTANT * (double) body.massKg / (body.radiusKm * 1_000D));
        double thrustToWeight = craftThrustN / (craftMassKg * gravity);
        if (thrustToWeight < 1) return Double.MAX_VALUE;
        double acceleration = (thrustToWeight - 1) * gravity;
        double timeToOrbit = orbitalDeltaV / acceleration;
        double gravityLosses = gravity * timeToOrbit * 2; // no perfect burns
        if (lossesOnly) return gravityLosses * (1 - drag); // drag helps on the way down
        return orbitalDeltaV + gravityLosses * (1 + drag); // and hinders on the way up
    }

    public static double getDeltaVBetween(CelestialBody start, CelestialBody end) {
        return calculateHohmannTransfer(start, end);
    }

    private static float parkingOrbit(CelestialBody body) {
        return body.radiusKm + AstronomyUtil.DEFAULT_ALTITUDE_KM;
    }

    private static double calculateHohmannTransfer(CelestialBody start, CelestialBody end) {
        if (start == end) return 0;
        if (start.parent == null || end.parent == null)
            throw new UnsupportedOperationException("transfers to and from a star are not supported");

        if (start.parent == end.parent) {
            return calculateSingleHohmannTransfer(
                            start.parent.massKg, start.semiMajorAxisKm, end.semiMajorAxisKm, start.massKg, parkingOrbit(start))
                    + calculateSingleHohmannTransfer(
                            start.parent.massKg, end.semiMajorAxisKm, start.semiMajorAxisKm, end.massKg, parkingOrbit(end));
        }
        if (start == end.parent) {
            return calculateSingleHohmannTransfer(start.massKg, parkingOrbit(start), end.semiMajorAxisKm)
                    + calculateSingleHohmannTransfer(
                            start.massKg, end.semiMajorAxisKm, parkingOrbit(start), end.massKg, parkingOrbit(end));
        }
        if (start.parent == end) {
            return calculateSingleHohmannTransfer(
                            end.massKg, start.semiMajorAxisKm, parkingOrbit(end), start.massKg, parkingOrbit(start))
                    + calculateSingleHohmannTransfer(end.massKg, parkingOrbit(end), start.semiMajorAxisKm);
        }

        // through the common parent; as in NTM:Space, the parking orbits are the bodies' distances
        // from their parents (checked against KSP's delta-v map: Kerbin to Ike 1561 m/s, KSP 1540)
        CelestialBody commonParent = getCommonParent(start, end);
        CelestialBody fromBody = start;
        CelestialBody toBody = end;
        float fromParking = start.semiMajorAxisKm;
        float toParking = end.semiMajorAxisKm;
        double burnCost = 0;
        while (fromBody.parent != commonParent) {
            burnCost += calculateSingleHohmannTransfer(
                    fromBody.parent.massKg, fromBody.semiMajorAxisKm, fromBody.semiMajorAxisKm, fromBody.massKg, fromBody.semiMajorAxisKm);
            fromParking = fromBody.semiMajorAxisKm;
            fromBody = fromBody.parent;
        }
        while (toBody.parent != commonParent) {
            burnCost += calculateSingleHohmannTransfer(
                    toBody.parent.massKg, toBody.semiMajorAxisKm, toBody.semiMajorAxisKm, toBody.massKg, toBody.semiMajorAxisKm);
            toParking = toBody.semiMajorAxisKm;
            toBody = toBody.parent;
        }
        burnCost += calculateSingleHohmannTransfer(
                commonParent.massKg, fromBody.semiMajorAxisKm, toBody.semiMajorAxisKm, fromBody.massKg, fromParking);
        burnCost += calculateSingleHohmannTransfer(
                commonParent.massKg, toBody.semiMajorAxisKm, fromBody.semiMajorAxisKm, toBody.massKg, toParking);
        return burnCost;
    }

    private static CelestialBody getCommonParent(CelestialBody start, CelestialBody end) {
        for (CelestialBody a = start.parent; a != null; a = a.parent)
            for (CelestialBody b = end.parent; b != null; b = b.parent) if (a == b) return a;
        throw new IllegalArgumentException("bodies are not in the same solar system");
    }

    private static double calculateSingleHohmannTransfer(float parentMassKg, float fromRadiusKm, float toRadiusKm) {
        double mu = parentMassKg * (double) AstronomyUtil.GRAVITATIONAL_CONSTANT;
        double startRadius = fromRadiusKm * 1_000D;
        double endRadius = toRadiusKm * 1_000D;
        double transferSemiMajorAxis = (startRadius + endRadius) / 2;
        double currentVelocity = Math.sqrt(mu / startRadius);
        double requiredVelocity = Math.sqrt(mu * ((2 / startRadius) - (1 / transferSemiMajorAxis)));
        return Math.abs(requiredVelocity - currentVelocity);
    }

    private static double calculateSingleHohmannTransfer(
            float parentMassKg, float fromRadiusKm, float toRadiusKm, float fromMassKg, float parkingOrbitRadiusKm) {
        double hyperbolicVelocity = calculateSingleHohmannTransfer(parentMassKg, fromRadiusKm, toRadiusKm);
        double mu = fromMassKg * (double) AstronomyUtil.GRAVITATIONAL_CONSTANT;
        double parkingRadius = parkingOrbitRadiusKm * 1_000D;
        double parkingVelocity = Math.sqrt(mu / parkingRadius);
        double escapeVelocity = Math.sqrt((2 * mu) / parkingRadius);
        return Math.sqrt(hyperbolicVelocity * hyperbolicVelocity + escapeVelocity * escapeVelocity) - parkingVelocity;
    }

    /** NTM:Space's sanity checks against KSP's delta-v map, logged at debug level. */
    private static void logChecks() {
        if (!LOGGER.isDebugEnabled()) return;
        CelestialBody kerbin = CelestialBody.getBody("kerbin");
        CelestialBody eve = CelestialBody.getBody("eve");
        CelestialBody duna = CelestialBody.getBody("duna");
        CelestialBody mun = CelestialBody.getBody("mun");
        CelestialBody minmus = CelestialBody.getBody("minmus");
        CelestialBody ike = CelestialBody.getBody("ike");
        LOGGER.debug("Kerbin -> Eve {} (KSP {})", getDeltaVBetween(kerbin, eve), 950 + 90 + 80 + 1330);
        LOGGER.debug("Kerbin -> Duna {} (KSP {})", getDeltaVBetween(kerbin, duna), 950 + 130 + 250 + 360);
        LOGGER.debug("Kerbin -> Ike {} (KSP {})", getDeltaVBetween(kerbin, ike), 950 + 130 + 250 + 30 + 180);
        LOGGER.debug("Kerbin -> Mun {} (KSP {})", getDeltaVBetween(kerbin, mun), 860 + 310);
        LOGGER.debug("Kerbin -> Minmus {} (KSP {})", getDeltaVBetween(kerbin, minmus), 930 + 160);
        LOGGER.debug("Kerbin year {} days (KSP 426), Eve {} (261), Mun {} (6)",
                kerbin.getOrbitalPeriod(), eve.getOrbitalPeriod(), mun.getOrbitalPeriod());
    }
}
