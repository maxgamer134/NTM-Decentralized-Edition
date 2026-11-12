// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.station;

import com.hbm.space.AstronomyUtil;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.SolarSystemSavedData;
import com.hbm.space.rocket.EntityRideableRocket;
import com.hbm.space.rocket.EntityRideableRocket.RocketState;
import com.hbm.space.rocket.ItemVOTVdrive.Destination;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * A space station. The orbit dimension is cut into {@link #STATION_SIZE}-block cells, one station
 * each; which cell a player is in decides the body they orbit and what the sky shows. Leaving the
 * cell's buffer drops the player back to that body.
 */
public class OrbitalStation {

    public static final int STATION_SIZE = 1024; // total area for each station
    public static final int BUFFER_SIZE = 256; // the edge that drops you out of orbit
    public static final int WARNING_SIZE = 32; // the band that warns you first
    private static final float ORBITAL_PERIOD = 7200;

    public enum StationState {
        ORBIT, // big chillin
        LEAVING, // prepare engines for transfer
        TRANSFER, // going from A to B
        ARRIVING, // spool down engines
    }

    public String name = ""; // I dub thee
    public CelestialBody orbiting;
    public CelestialBody target;
    public StationState state = StationState.ORBIT;
    public int stateTimer;
    public int maxStateTimer = 100;
    public boolean hasStation;
    /** The station's grid cell. */
    public int dX, dZ;
    public boolean hasEngines = true;
    public List<BlockPos> errorsAt = new ArrayList<>();
    public int errorTimer;
    public float gravityMultiplier = 1;

    private SpaceStations.@Nullable PortBE mainPort;
    private final Map<BlockPos, SpaceStations.PortBE> ports = new LinkedHashMap<>();
    private int portIndex;
    private final Set<IPropulsion> engines = new HashSet<>();
    /** Where the engines are, kept while their chunks are unloaded (they register only when loaded). */
    private final Set<BlockPos> enginePositions = new HashSet<>();
    private float lastThrust;

    /** The station the client's player is on, as the server last sent it. */
    public static OrbitalStation clientStation = new OrbitalStation(null);

    public OrbitalStation(@Nullable CelestialBody orbiting) {
        this.orbiting = this.target = orbiting != null ? orbiting : SolarSystem.kerbol;
    }

    public OrbitalStation(CelestialBody orbiting, int x, int z) {
        this(orbiting);
        dX = x;
        dZ = z;
    }

    public static float getOrbitalAltitude(CelestialBody body) {
        // r = cbrt(G M T^2 / 4 pi^2): an hour-long realtime orbit
        return (float) Math.cbrt((AstronomyUtil.GRAVITATIONAL_CONSTANT * body.massKg * (ORBITAL_PERIOD * ORBITAL_PERIOD)) / (4 * Math.PI * Math.PI));
    }

    public void travelTo(CelestialBody to) {
        if (state != StationState.ORBIT || !canTravel(orbiting, to)) return;
        setState(StationState.LEAVING, getLeaveTime());
        target = to;
    }

    /** Loads the chunks the engines are in, so they register before a burn (they would otherwise leave the station stuck at 0%). */
    public void loadEngines(ServerLevel level) {
        for (BlockPos pos : enginePositions) {
            if (!level.isLoaded(pos)) level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        }
    }

    public void update(Level level) {
        if (!level.isClientSide()) {
            if (state == StationState.LEAVING) {
                if (stateTimer > maxStateTimer && level instanceof ServerLevel server) loadEngines(server);
                if (stateTimer > maxStateTimer) setState(StationState.TRANSFER, getTransferTime(level));
            } else if (state == StationState.TRANSFER) {
                if (stateTimer > maxStateTimer) {
                    setState(StationState.ARRIVING, getArriveTime());
                    orbiting = target;
                }
            } else if (state == StationState.ARRIVING) {
                if (stateTimer > maxStateTimer) setState(StationState.ORBIT, 0);
            }
            if (state != StationState.ORBIT && level instanceof ServerLevel server) SolarSystemSavedData.get(server).setDirty();
            hasEngines = !engines.isEmpty();
            if (--errorTimer <= 0) {
                errorsAt = new ArrayList<>();
                errorTimer = 0;
            }
        }
        stateTimer++;
    }

    private boolean canTravel(CelestialBody from, CelestialBody to) {
        if (engines.isEmpty()) return false;
        double deltaV = SolarSystem.getDeltaVBetween(from, to);
        int shipMass = 200_000; // always static, so big cool stations aren't punished
        float totalThrust = getTotalThrust();
        if (totalThrust > 0) lastThrust = totalThrust;
        boolean canTravel = true;
        errorsAt = new ArrayList<>();
        for (IPropulsion engine : engines) {
            float massPortion = engine.getThrust() / totalThrust;
            if (!engine.canPerformBurn(Math.round(shipMass * massPortion), deltaV)) {
                canTravel = false;
                errorsAt.add(engine.getBlockEntity().getBlockPos());
                errorTimer = 100;
            }
        }
        return canTravel;
    }

    private float getTotalThrust() {
        float thrust = 0;
        for (IPropulsion engine : engines) thrust += engine.getThrust();
        return thrust;
    }

    // has the side effect of starting the burns
    private int getLeaveTime() {
        int time = 20;
        for (IPropulsion engine : engines) time = Math.max(time, engine.startBurn());
        return time;
    }

    // and this one ends them
    private int getArriveTime() {
        int time = 20;
        for (IPropulsion engine : engines) time = Math.max(time, engine.endBurn());
        return time;
    }

    private int getTransferTime(Level level) {
        if (mainPort == null) return -1;
        float thrust = getTotalThrust();
        if (thrust <= 0) thrust = lastThrust; // the engines' chunks are unloaded
        if (thrust <= 0) thrust = 1;
        return calculateTransferTime(SolarSystem.calculateDistanceBetweenTwoBodies(level, orbiting, target), calculateSize(), thrust);
    }

    public static int calculateTransferTime(double distance, int size, float thrust) {
        return (int) (Math.log(1 + (distance * size / thrust * 100)) * 150);
    }

    public void setState(StationState state, int timeUntilNext) {
        this.state = state;
        stateTimer = 0;
        maxStateTimer = timeUntilNext;
    }

    /** Sends a free docked pod down to the destination; false when none could go. */
    public boolean recallPod(Destination destination) {
        if (!hasStation || destination.body().getBody() != orbiting) return false;
        for (SpaceStations.PortBE port : ports.values()) {
            EntityRideableRocket rocket = port.getDocked();
            if (rocket == null || !rocket.isReusable() || rocket.isVehicle()) continue;
            RocketState s = rocket.getState();
            if (s != RocketState.AWAITING && s != RocketState.LANDED) continue;
            rocket.recallPod(destination);
            return true;
        }
        return false;
    }

    public static void addPropulsion(Level level, IPropulsion propulsion) {
        BlockPos pos = propulsion.getBlockEntity().getBlockPos();
        OrbitalStation station = getStationFromPosition(level, pos.getX(), pos.getZ());
        station.engines.add(propulsion);
        station.enginePositions.add(pos.immutable());
    }

    public static void removePropulsion(Level level, IPropulsion propulsion) {
        BlockPos pos = propulsion.getBlockEntity().getBlockPos();
        OrbitalStation station = getStationFromPosition(level, pos.getX(), pos.getZ());
        station.engines.remove(propulsion);
        // an unloaded engine stays on the books; one that was broken or replaced goes
        if (level.getBlockState(pos).getBlock() != propulsion.getBlockEntity().getBlockState().getBlock()) station.enginePositions.remove(pos);
    }

    public void addPort(SpaceStations.PortBE port) {
        ports.put(port.getBlockPos(), port);
        if (port.isCore()) mainPort = port;
    }

    public void removePort(SpaceStations.PortBE port) {
        ports.remove(port.getBlockPos());
        if (mainPort == port) mainPort = null;
    }

    public SpaceStations.@Nullable PortBE getPort() {
        if (ports.isEmpty()) return null;
        // first any free port
        int index = 0;
        for (SpaceStations.PortBE port : ports.values()) {
            if (!port.hasDocked && !port.isReserved) {
                portIndex = index;
                return port;
            }
            index++;
        }
        // failing that, round robin on the occupied ones
        if (++portIndex >= ports.size()) portIndex = 0;
        return new ArrayList<>(ports.values()).get(portIndex);
    }

    public static SpaceStations.@Nullable PortBE getPort(Level level, int x, int z) {
        return getStationFromPosition(level, x, z).getPort();
    }

    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /** The station's top-down area; a flood fill, so don't call it every frame. */
    public int calculateSize() {
        if (mainPort == null || mainPort.getLevel() == null) return 0;
        Level level = mainPort.getLevel();
        BlockPos core = mainPort.getBlockPos();
        int minX = core.getX(), maxX = minX, minZ = core.getZ(), maxZ = minZ;
        ArrayDeque<BlockPos> stack = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        stack.push(core);
        visited.add(core);
        // a cap, so a runaway platform can't stall the server
        while (!stack.isEmpty() && visited.size() < 1_000_000) {
            BlockPos pos = stack.pop();
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
            for (Direction dir : HORIZONTAL) {
                BlockPos next = pos.relative(dir);
                if (!visited.contains(next) && isInStation(level, core, next)) {
                    visited.add(next);
                    stack.push(next);
                }
            }
        }
        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    private static boolean isInStation(Level level, BlockPos core, BlockPos pos) {
        if (level.isLoaded(pos) && level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) > 1) return true;
        return Math.abs(pos.getX() - core.getX()) < 5 && Math.abs(pos.getZ() - core.getZ()) < 5; // minimum station size
    }

    public double getUnscaledProgress(float partialTicks) {
        if (state == StationState.ORBIT || maxStateTimer <= 0) return 0;
        return Mth.clamp((stateTimer + partialTicks) / (double) maxStateTimer, 0, 1);
    }

    public double getTransferProgress(float partialTicks) {
        if (state != StationState.TRANSFER) return 0;
        double t = getUnscaledProgress(partialTicks);
        return t < 0.5 ? (1 - Math.sqrt(1 - Math.pow(2 * t, 3))) / 2 : (Math.sqrt(1 - Math.pow(-2 * t + 2, 3)) + 1) / 2;
    }

    /** How much sunlight the station gets; NTM:Space also dimmed it in eclipses. */
    public float getSunPower() {
        double progress = getTransferProgress(0);
        float sun = orbiting.getSunPower();
        return progress > 0 ? (float) Mth.lerp(progress, sun, target.getSunPower()) : sun;
    }

    // the station grid

    public static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int cell(double coord) {
        return Mth.floor(coord / STATION_SIZE);
    }

    /** The middle of a grid cell, where the docking ring always sits. */
    public static int centre(int cell) {
        return cell * STATION_SIZE + STATION_SIZE / 2;
    }

    /** The station a position is in; on the client, the one the server sent. */
    public static OrbitalStation getStationFromPosition(Level level, int x, int z) {
        if (!(level instanceof ServerLevel server)) return clientStation;
        return getStationFromPosition(SolarSystemSavedData.get(server), x, z);
    }

    public static OrbitalStation getStationFromPosition(SolarSystemSavedData data, int x, int z) {
        int cx = cell(x), cz = cell(z);
        OrbitalStation station = data.stations().get(key(cx, cz));
        // only a cell nobody launched to (a debug teleport, say) has none
        if (station == null) station = addStation(data, cx, cz, CelestialBody.getBody(net.minecraft.world.level.Level.OVERWORLD));
        return station;
    }

    /** A free cell within the configured distance; after 256 tries, one only reserved by a drive. */
    public static long findFreeSpace(SolarSystemSavedData data, Random rand) {
        int size = Math.max(1, com.hbm.config.NeoForgeHbmConfig.runtime().spaceMaxStationDistance() / STATION_SIZE);
        long key = 0;
        for (int i = 0; i < 512; i++) {
            key = key(rand.nextInt(size * 2) - size, rand.nextInt(size * 2) - size);
            OrbitalStation station = data.stations().get(key);
            if (station == null || (i > 256 && !station.hasStation)) return key;
        }
        return key;
    }

    public static OrbitalStation addStation(SolarSystemSavedData data, CelestialBody orbiting) {
        long key = findFreeSpace(data, new Random());
        return addStation(data, (int) (key >> 32), (int) key, orbiting);
    }

    /** Adds a station at a grid cell; an existing one stays. */
    public static OrbitalStation addStation(SolarSystemSavedData data, int x, int z, CelestialBody orbiting) {
        OrbitalStation station = data.stations().computeIfAbsent(key(x, z), k -> new OrbitalStation(orbiting, x, z));
        data.setDirty();
        return station;
    }

    /** Removes a temporary station; ones with a launched core stay. */
    public static void removeStation(SolarSystemSavedData data, OrbitalStation station) {
        if (station.hasStation) return;
        data.stations().remove(key(station.dX, station.dZ));
        data.setDirty();
    }

    /** Marks a cell's station as launched, so stations can fly to it. */
    public static void markLaunched(SolarSystemSavedData data, int x, int z, CelestialBody body) {
        OrbitalStation station = addStation(data, x, z, body);
        station.orbiting = station.target = body;
        station.hasStation = true;
        if (station.name.isEmpty()) station.name = randomName(new Random());
        data.setDirty();
    }

    private static final String[] NAME_FIRST = {
        "Kestrel", "Halcyon", "Meridian", "Corvid", "Lantern", "Bastion", "Zenith", "Aurora", "Tundra", "Harbor",
        "Cinder", "Vesper", "Orchid", "Granite", "Solstice", "Mariner", "Falcon", "Ember", "Juniper", "Atlas"
    };
    private static final String[] NAME_SECOND = {
        "Station", "Outpost", "Platform", "Habitat", "Relay", "Spire", "Ring", "Dock", "Array", "Bulwark"
    };

    /** A fresh station's name: two words and a hull number, e.g. "Kestrel Relay 417". */
    public static String randomName(Random random) {
        return NAME_FIRST[random.nextInt(NAME_FIRST.length)] + " " + NAME_SECOND[random.nextInt(NAME_SECOND.length)]
                + " " + (100 + random.nextInt(900));
    }

    /** Places the station core in the middle of a cell, if it's not there yet. */
    public static void spawn(ServerLevel level, int x, int z) {
        BlockPos core = new BlockPos(x, 127, z);
        if (level.getBlockState(core).is(SpaceStations.CORE.get())) return;
        SpaceStations.Port block = SpaceStations.CORE.get();
        level.setBlock(core, block.defaultBlockState().setValue(SpaceStations.Port.FACING, Direction.NORTH), 3);
        block.fillSpace(level, core, Direction.NORTH);
    }

    // sync and saving

    public void serialize(FriendlyByteBuf buf) {
        buf.writeUtf(orbiting.name);
        buf.writeUtf(target.name);
        buf.writeByte(state.ordinal());
        buf.writeInt(stateTimer);
        buf.writeInt(maxStateTimer);
        buf.writeBoolean(hasEngines);
        buf.writeBoolean(hasStation);
        buf.writeFloat(gravityMultiplier);
        buf.writeUtf(name);
        buf.writeVarInt(errorsAt.size());
        for (BlockPos error : errorsAt) buf.writeBlockPos(error);
    }

    public static OrbitalStation deserialize(FriendlyByteBuf buf) {
        OrbitalStation station = new OrbitalStation(CelestialBody.getBody(buf.readUtf()));
        station.target = CelestialBody.getBody(buf.readUtf());
        station.state = StationState.values()[Mth.clamp(buf.readByte(), 0, StationState.values().length - 1)];
        station.stateTimer = buf.readInt();
        station.maxStateTimer = buf.readInt();
        station.hasEngines = buf.readBoolean();
        station.hasStation = buf.readBoolean();
        station.gravityMultiplier = buf.readFloat();
        station.name = buf.readUtf();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) station.errorsAt.add(buf.readBlockPos());
        return station;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", dX);
        tag.putInt("z", dZ);
        tag.putString("orbiting", orbiting.name);
        tag.putString("target", target.name);
        tag.putInt("state", state.ordinal());
        tag.putInt("stateTimer", stateTimer);
        tag.putInt("maxStateTimer", maxStateTimer);
        tag.putBoolean("hasStation", hasStation);
        tag.putString("name", name);
        tag.putFloat("gravity", gravityMultiplier);
        tag.putFloat("lastThrust", lastThrust);
        tag.putLongArray("engines", enginePositions.stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }

    public static OrbitalStation load(CompoundTag tag) {
        OrbitalStation station = new OrbitalStation(CelestialBody.getBody(tag.getString("orbiting")), tag.getInt("x"), tag.getInt("z"));
        station.target = CelestialBody.getBody(tag.getString("target"));
        station.state = StationState.values()[Mth.clamp(tag.getInt("state"), 0, StationState.values().length - 1)];
        station.stateTimer = tag.getInt("stateTimer");
        station.maxStateTimer = tag.getInt("maxStateTimer");
        station.hasStation = tag.getBoolean("hasStation");
        station.name = tag.getString("name");
        station.gravityMultiplier = tag.contains("gravity") ? tag.getFloat("gravity") : 1;
        station.lastThrust = tag.getFloat("lastThrust");
        for (long pos : tag.getLongArray("engines")) station.enginePositions.add(BlockPos.of(pos));
        return station;
    }
}
