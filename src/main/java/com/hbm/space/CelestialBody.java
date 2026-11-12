// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.lib.Library;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.space.trait.CBT_Atmosphere.FluidEntry;
import com.hbm.space.trait.CelestialBodyTrait;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * A star, planet or moon, in a tree of bodies orbiting bodies. Orbital elements are in SI units
 * unless the field name says otherwise. A landable body has a dimension; the overworld is Kerbin.
 * Traits that can change (atmosphere, temperature, ...) start as the body's defaults and, once
 * changed, are kept in {@link SolarSystemSavedData}.
 */
public class CelestialBody {

    public final String name;
    public @Nullable ResourceKey<Level> dimension;
    /** Has a dimension and a solid surface. */
    public boolean canLand;

    public float massKg;
    public float radiusKm;
    /** Distance to the parent body. */
    public float semiMajorAxisKm;
    /** sqrt(1 - e^2), worked out ahead of time. */
    public float semiMinorAxisFactor;
    public float eccentricity;
    public float inclination;
    public float ascendingNode;
    public float argumentPeriapsis;
    /** Day length in seconds. */
    private int rotationalPeriod = 6 * 60 * 60;
    public float axialTilt;
    /** The lowest technology level that can locate this body, raised by where the stardar is. */
    private int minProcessingLevel;

    public ResourceLocation texture;
    public @Nullable ResourceLocation biomeMask;
    public @Nullable ResourceLocation cityMask;
    /** Drawn when the body is too small for its texture. */
    public float[] color = {0.4F, 0.4F, 0.4F};
    public @Nullable String tidallyLockedTo;
    public boolean hasRings;
    public float ringTilt;
    public float[] ringColor = {0.5F, 0.5F, 0.5F};
    public float ringSize = 2;
    /** Has bedrock ice. */
    public boolean hasIce;
    public @Nullable Fluid gas;

    public final List<CelestialBody> satellites = new ArrayList<>();
    public @Nullable CelestialBody parent;
    private final Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = new LinkedHashMap<>();

    public @Nullable ResourceLocation stoneTexture;
    public @Nullable ResourceLocation surfaceTexture;
    public @Nullable SolarSystem.Body type;

    /** The fragment shader drawn for this body (Kerbol's black hole), loaded by the client. */
    public @Nullable ResourceLocation shader;
    /** Scales the quad up when the shader draws the body smaller than the quad. */
    public float shaderScale = 1;

    private static final Map<ResourceKey<Level>, CelestialBody> DIM_TO_BODY = new HashMap<>();
    private static final Map<String, CelestialBody> NAME_TO_BODY = new LinkedHashMap<>();

    public CelestialBody(String name) {
        this.name = name;
        this.texture = Library.id("textures/misc/space/" + name + ".png");
        NAME_TO_BODY.put(name, this);
    }

    public CelestialBody(String name, ResourceKey<Level> dimension, SolarSystem.Body type) {
        this(name);
        this.dimension = dimension;
        this.canLand = true;
        this.type = type;
        DIM_TO_BODY.put(dimension, this);
    }

    // construction

    public CelestialBody withMassRadius(float kg, float km) {
        this.massKg = kg;
        this.radiusKm = km;
        return this;
    }

    public CelestialBody withOrbitalParameters(
            float semiMajorAxisKm,
            float eccentricity,
            float argumentPeriapsisDegrees,
            float inclinationDegrees,
            float ascendingNodeDegrees) {
        this.semiMajorAxisKm = semiMajorAxisKm;
        this.semiMinorAxisFactor = (float) Math.sqrt(1 - eccentricity * eccentricity);
        this.eccentricity = eccentricity;
        this.argumentPeriapsis = (float) Math.toRadians(argumentPeriapsisDegrees);
        this.inclination = (float) Math.toRadians(inclinationDegrees);
        this.ascendingNode = (float) Math.toRadians(ascendingNodeDegrees);
        return this;
    }

    public CelestialBody withRotationalPeriod(int seconds) {
        this.rotationalPeriod = seconds;
        return this;
    }

    public CelestialBody withAxialTilt(float degrees) {
        this.axialTilt = degrees;
        return this;
    }

    public CelestialBody withMinProcessingLevel(int level) {
        this.minProcessingLevel = level;
        return this;
    }

    public CelestialBody withTexture(ResourceLocation location) {
        this.texture = location;
        return this;
    }

    public CelestialBody withCityMask(ResourceLocation location) {
        this.cityMask = location;
        return this;
    }

    public CelestialBody withBiomeMask(ResourceLocation location) {
        this.biomeMask = location;
        return this;
    }

    /** The block textures the body is drawn with from orbit, as texture paths. */
    public CelestialBody withBlockTextures(ResourceLocation stone, ResourceLocation surface) {
        this.stoneTexture = stone;
        this.surfaceTexture = surface;
        return this;
    }

    public CelestialBody withColor(float... color) {
        this.color = color;
        return this;
    }

    public CelestialBody withTidalLockingTo(String name) {
        this.tidallyLockedTo = name;
        return this;
    }

    public CelestialBody withRings(float tilt, float size, float... color) {
        this.hasRings = true;
        this.ringTilt = tilt;
        this.ringSize = size;
        this.ringColor = color;
        return this;
    }

    public CelestialBody withGas(Fluid gas) {
        this.gas = gas;
        return this;
    }

    public CelestialBody withSatellites(CelestialBody... bodies) {
        Collections.addAll(satellites, bodies);
        for (CelestialBody body : bodies) body.parent = this;
        return this;
    }

    public CelestialBody withTraits(CelestialBodyTrait... traits) {
        for (CelestialBodyTrait trait : traits) this.traits.put(trait.getClass(), trait);
        return this;
    }

    public CelestialBody withShader(ResourceLocation fragmentShader) {
        return withShader(fragmentShader, 1);
    }

    public CelestialBody withShader(ResourceLocation fragmentShader, float scale) {
        this.shader = fragmentShader;
        this.shaderScale = scale;
        return this;
    }

    public CelestialBody withIce(boolean hasIce) {
        this.hasIce = hasIce;
        return this;
    }

    // terraforming: saved traits replace the body's defaults

    public static void setTraits(Level level, CelestialBodyTrait... traits) {
        getBody(level).setTraits(traits);
    }

    public void setTraits(CelestialBodyTrait... traits) {
        SolarSystemSavedData.get().setTraits(name, traits);
    }

    public void setTraits(Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits) {
        setTraits(traits.values().toArray(new CelestialBodyTrait[0]));
    }

    /**
     * The body's traits as copies safe to modify and pass back to {@link #setTraits}. NTM:Space
     * handed out the live instances here, so an edit made without saving changed the body anyway
     * (and edits to the defaults stuck until restart).
     */
    public static Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits(Level level) {
        return getBody(level).getTraits();
    }

    public Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits() {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> copy = new LinkedHashMap<>();
        for (CelestialBodyTrait trait : getTraitsUnsafe().values()) copy.put(trait.getClass(), trait.copy());
        return copy;
    }

    public static void modifyTraits(Level level, CelestialBodyTrait... traits) {
        getBody(level).modifyTraits(traits);
    }

    public void modifyTraits(CelestialBodyTrait... traits) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> current = getTraits();
        for (CelestialBodyTrait trait : traits) current.put(trait.getClass(), trait);
        setTraits(current);
    }

    public static void clearTraits(Level level) {
        getBody(level).clearTraits();
    }

    public void clearTraits() {
        SolarSystemSavedData.get().clearTraits(name);
    }

    /** He has ceased to be: strips the body's atmosphere. */
    public static void degas(Level level) {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> current = getTraits(level);
        current.remove(CBT_Atmosphere.class);
        getBody(level).setTraits(current);
    }

    /** Takes {@code amount} millibuckets of a gas out of the atmosphere; false if it has none. */
    public static boolean consumeGas(Level level, Fluid fluid, double amount) {
        CelestialBody body = getBody(level);
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> current = body.getTraits();
        CBT_Atmosphere atmosphere = (CBT_Atmosphere) current.get(CBT_Atmosphere.class);
        if (atmosphere == null) return false;

        boolean consumed = false;
        for (int i = 0; i < atmosphere.fluids.size(); i++) {
            FluidEntry entry = atmosphere.fluids.get(i);
            if (entry.fluid != fluid) continue;
            entry.pressure -= amount / AstronomyUtil.MB_PER_ATM;
            consumed = true;
            if (entry.pressure <= 0) {
                atmosphere.fluids.remove(i);
                if (atmosphere.fluids.isEmpty()) current.remove(CBT_Atmosphere.class);
            }
            break;
        }
        // NTM:Space saved here even when nothing was taken
        if (consumed) body.setTraits(current);
        return consumed;
    }

    /** Releases {@code amount} millibuckets of a gas; at most eight gases, the thinnest giving way. */
    public static void emitGas(Level level, Fluid fluid, double amount) {
        CelestialBody body = getBody(level);
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> current = body.getTraits();
        CBT_Atmosphere atmosphere =
                (CBT_Atmosphere) current.computeIfAbsent(CBT_Atmosphere.class, k -> new CBT_Atmosphere());

        boolean found = false;
        for (FluidEntry entry : atmosphere.fluids) {
            if (entry.fluid == fluid) {
                entry.pressure += amount / AstronomyUtil.MB_PER_ATM;
                found = true;
                break;
            }
        }
        if (!found) {
            if (atmosphere.fluids.size() >= 8) {
                atmosphere.sortDescending();
                atmosphere.fluids.remove(atmosphere.fluids.size() - 1);
            }
            atmosphere.fluids.add(new FluidEntry(fluid, amount / AstronomyUtil.MB_PER_ATM));
        }
        body.setTraits(current);
    }

    // lookups; an unknown dimension reads as the overworld (Kerbin), as in NTM:Space

    public static Collection<CelestialBody> getAllBodies() {
        return Collections.unmodifiableCollection(NAME_TO_BODY.values());
    }

    public static Collection<CelestialBody> getLandableBodies() {
        return Collections.unmodifiableCollection(DIM_TO_BODY.values());
    }

    public static CelestialBody getBody(String name) {
        CelestialBody body = NAME_TO_BODY.get(name);
        return body != null ? body : DIM_TO_BODY.get(Level.OVERWORLD);
    }

    public static CelestialBody getBody(ResourceKey<Level> dimension) {
        CelestialBody body = DIM_TO_BODY.get(dimension);
        return body != null ? body : DIM_TO_BODY.get(Level.OVERWORLD);
    }

    public static @Nullable CelestialBody getBodyOrNull(String name) {
        return NAME_TO_BODY.get(name);
    }

    public static @Nullable CelestialBody getBodyOrNull(ResourceKey<Level> dimension) {
        return DIM_TO_BODY.get(dimension);
    }

    public static CelestialBody getBody(Level level) {
        return getBody(level.dimension());
    }

    public static CelestialBody getStar(Level level) {
        return getBody(level).getStar();
    }

    public static CelestialBody getPlanet(Level level) {
        return getBody(level).getPlanet();
    }

    public static boolean inOrbit(Level level) {
        return level.dimension() == SpaceDimensions.ORBIT;
    }

    public static @Nullable SolarSystem.Body getEnum(Level level) {
        return getBody(level).type;
    }

    public static double getRotationalPeriod(Level level) {
        return getBody(level).getRotationalPeriod();
    }

    public static float getSemiMajorAxis(Level level) {
        return getBody(level).semiMajorAxisKm;
    }

    public static boolean hasTrait(Level level, Class<? extends CelestialBodyTrait> trait) {
        return getBody(level).hasTrait(trait);
    }

    public static <T extends CelestialBodyTrait> @Nullable T getTrait(Level level, Class<? extends T> trait) {
        return getBody(level).getTrait(trait);
    }

    public static boolean hasDefaultTrait(Level level, Class<? extends CelestialBodyTrait> trait) {
        return getBody(level).hasDefaultTrait(trait);
    }

    public static <T extends CelestialBodyTrait> @Nullable T getDefaultTrait(
            Level level, Class<? extends T> trait) {
        return getBody(level).getDefaultTrait(trait);
    }

    // members

    public String getUnlocalizedName() {
        return name;
    }

    public CelestialBody getStar() {
        CelestialBody body = this;
        while (body.parent != null) body = body.parent;
        return body;
    }

    public CelestialBody getPlanet() {
        if (parent == null) return this;
        CelestialBody body = this;
        while (body.parent.parent != null) body = body.parent;
        return body;
    }

    /** Day length in ticks, scaled to the 20 minute Minecraft day. */
    public double getRotationalPeriod() {
        return rotationalPeriod * (AstronomyUtil.DAY_FACTOR / AstronomyUtil.TIME_MULTIPLIER) * 20;
    }

    /** Year length in days, from the semi-major axis. */
    public double getOrbitalPeriod() {
        if (parent == null) return Double.POSITIVE_INFINITY; // a star never comes round
        double semiMajorAxis = semiMajorAxisKm * 1_000D;
        double period =
                2 * Math.PI
                        * Math.sqrt(
                                semiMajorAxis * semiMajorAxis * semiMajorAxis
                                        / (AstronomyUtil.GRAVITATIONAL_CONSTANT * (double) parent.massKg));
        return period / AstronomyUtil.SECONDS_IN_KSP_DAY;
    }

    /** Surface gravity in m/s/s, from mass and radius. */
    public float getSurfaceGravity() {
        float radius = radiusKm * 1000;
        return AstronomyUtil.GRAVITATIONAL_CONSTANT * massKg / (radius * radius);
    }

    /** The power multiplier for sun-driven machines: falls off with the square of the distance. */
    public float getSunPower() {
        float distanceAU = getPlanet().semiMajorAxisKm / AstronomyUtil.KM_IN_AU;
        return 1 / (distanceAU * distanceAU);
    }

    /** Technology level needed to process this body from another: 0 for itself and its moons. */
    public int getProcessingLevel(CelestialBody from) {
        int level = this == from || this == from.parent || this.parent == from ? 0 : 1;
        return Math.max(level, minProcessingLevel);
    }

    public boolean hasTrait(Class<? extends CelestialBodyTrait> trait) {
        return getTraitsUnsafe().containsKey(trait);
    }

    @SuppressWarnings("unchecked")
    public <T extends CelestialBodyTrait> @Nullable T getTrait(Class<? extends T> trait) {
        return (T) getTraitsUnsafe().get(trait);
    }

    /** The live traits: never modify these, take {@link #getTraits()} for that. */
    Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraitsUnsafe() {
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> saved = SolarSystemSavedData.liveTraits(name);
        return saved != null ? saved : traits;
    }

    public boolean hasDefaultTrait(Class<? extends CelestialBodyTrait> trait) {
        return traits.containsKey(trait);
    }

    @SuppressWarnings("unchecked")
    public <T extends CelestialBodyTrait> @Nullable T getDefaultTrait(Class<? extends T> trait) {
        return (T) traits.get(trait);
    }

    Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> defaultTraits() {
        return traits;
    }
}
