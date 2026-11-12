// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.rocket;

import com.hbm.backport.compat.EntityCompat;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.rocket.ItemVOTVdrive.Destination;
import com.hbm.space.rocket.ItemVOTVdrive.Target;
import com.hbm.space.rocket.RocketStruct.RocketStage;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A crewed rocket: ride it, load a processed drive, jump to launch. It climbs out of the
 * atmosphere, drops its spent stage and comes down on the drive's landing spot, burning to a
 * stop on its legs; without legs, or in liquid, it tips over (and in lava, blows up). A pod can
 * fly again and again; anything else needs a new stage per trip.
 *
 * <p>Crewed trips between bodies spend the transfer in the orbit dimension, in a temporary
 * station cell; a station drive docks at a station's port, and a rocket leaving a port drops
 * out of its bottom before climbing away.
 */
public class EntityRideableRocket extends EntityCompat implements com.hbm.interfaces.IEntityLookOverlay {

    public enum RocketState {
        AWAITING, LAUNCHING, LANDING, LANDED, TIPPING, DOCKING, UNDOCKING, NEEDSFUEL, TRANSFER
    }

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TIMER = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DRIVE = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<CompoundTag> ROCKET = SynchedEntityData.defineId(EntityRideableRocket.class, EntityDataSerializers.COMPOUND_TAG);

    public ItemStack navDrive = ItemStack.EMPTY;
    private int stateTimer;
    public int decoupleTimer;
    public int forceExitTimer;
    private long lastExitAttempt = Long.MIN_VALUE;

    /** A rider sneaking to get out mid-flight: true once they have held it for three seconds. */
    public boolean tryForcedExit() {
        long now = level().getGameTime();
        if (now - lastExitAttempt > 2) forceExitTimer = 0; // they let go
        lastExitAttempt = now;
        return forceExitTimer++ >= 60;
    }
    private double rocketVelocity;
    private boolean willExplode;
    private @Nullable Player thrower;
    private @Nullable RocketStruct cached;
    private @Nullable CompoundTag cachedTag;
    private RocketState lastState = RocketState.AWAITING;
    private @Nullable AudioWrapper audio;
    private @Nullable Destination destinationOverride;
    private com.hbm.space.station.SpaceStations.@Nullable PortBE targetPort;
    private boolean dockChecked;
    /** The frequency a satellite payload is launched on. */
    public int satFreq;

    public EntityRideableRocket(EntityType<? extends EntityRideableRocket> type, Level level) {
        super(type, level);
    }

    public static EntityRideableRocket of(Level level, double x, double y, double z, ItemStack rocketItem) {
        EntityRideableRocket rocket = new EntityRideableRocket(SpaceRockets.ROCKET_ENTITY.get(), level);
        rocket.setPos(x, y, z);
        RocketStruct struct = ItemCustomRocket.get(rocketItem);
        if (struct != null) rocket.setRocket(struct);
        return rocket;
    }

    public EntityRideableRocket withProgram(ItemStack drive) {
        navDrive = drive.copyWithCount(1);
        return this;
    }

    /** The player who launched it from a pad or launcher, who it seats once it lands. */
    public EntityRideableRocket launchedBy(@Nullable Player player) {
        thrower = player;
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, RocketState.AWAITING.ordinal());
        builder.define(TIMER, 0);
        builder.define(DRIVE, ItemStack.EMPTY);
        builder.define(ROCKET, new RocketStruct(SpaceRocketParts.RP_CAPSULE_20.get()).save());
    }

    public RocketStruct getRocket() {
        CompoundTag tag = entityData.get(ROCKET);
        if (tag != cachedTag || cached == null) {
            cached = RocketStruct.load(tag);
            cachedTag = tag;
        }
        return cached;
    }

    public void setRocket(RocketStruct rocket) {
        entityData.set(ROCKET, rocket.save());
        refreshDimensions();
    }

    public RocketState getState() {
        return RocketState.values()[Mth.clamp(entityData.get(STATE), 0, RocketState.values().length - 1)];
    }

    public void setState(RocketState state) {
        entityData.set(STATE, state.ordinal());
        entityData.set(TIMER, 0);
        stateTimer = 0;
    }

    public int getStateTimer() {
        return entityData.get(TIMER);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(2F, (float) getRocket().getHeight() + 1F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ROCKET.equals(key)) refreshDimensions();
    }

    public boolean canRide() {
        RocketStruct rocket = getRocket();
        return rocket.capsule != null && rocket.capsule.warheadType() == SpaceRocketParts.APOLLO;
    }

    public boolean isReusable() {
        return getRocket().capsule == SpaceRocketParts.RP_POD_20.get();
    }

    public boolean canExitCapsule() {
        RocketState s = getState();
        return s != RocketState.LANDING && s != RocketState.LAUNCHING && s != RocketState.DOCKING
                && s != RocketState.UNDOCKING && s != RocketState.TRANSFER;
    }

    public Target getTarget() {
        return ItemVOTVdrive.getTarget(entityData.get(DRIVE), level());
    }

    public @Nullable Destination getDestination() {
        if (destinationOverride != null) return destinationOverride;
        return ItemVOTVdrive.getDestination(entityData.get(DRIVE));
    }

    /** Where the rocket stands now: the body under it, or the station it's at. */
    public Target here() {
        if (CelestialBody.inOrbit(level())) {
            com.hbm.space.station.OrbitalStation station =
                    com.hbm.space.station.OrbitalStation.getStationFromPosition(level(), getBlockX(), getBlockZ());
            return new Target(station.orbiting, true, station.hasStation);
        }
        return new Target(CelestialBody.getBody(level()), false, true);
    }

    /** Sends a docked pod down to a destination, as a recall from the ground does. */
    public void recallPod(Destination destination) {
        thrower = null;
        destinationOverride = destination;
        attemptLaunch();
    }

    public void attemptLaunch() {
        Target from = here(), to = getTarget();
        if (to.body() == null || !to.isValid()) return;
        if (getRocket().hasSufficientFuel(from.body(), to.body(), from.inOrbit(), to.inOrbit()))
            setState(from.inOrbit() ? RocketState.UNDOCKING : RocketState.LAUNCHING);
    }

    private void beginLandingSequence(Target from, Target to) {
        setDeltaMovement(Vec3.ZERO);
        RocketStruct rocket = getRocket();
        // undocking to the body below doesn't spend a stage
        if (!rocket.stages.isEmpty() && !(getState() == RocketState.UNDOCKING && from.body() == to.body())) {
            rocket.stages.remove(0);
            setRocket(rocket);
        }
        setState(RocketState.LANDING);
        Destination destination = getDestination();
        if (destination == null || !(level() instanceof ServerLevel server)) return;
        boolean toStation = destination.body() == SolarSystem.Body.ORBIT;
        int x = destination.x(), z = destination.z();
        double y = 800;
        if (!canRide()) {
            if (rocket.capsule == SpaceRocketParts.RP_STATION_CORE_20.get()) {
                // the cell is marked; the core itself appears when someone first flies there
                com.hbm.space.station.OrbitalStation.markLaunched(com.hbm.space.SolarSystemSavedData.get(server), x, z, CelestialBody.getBody(server));
            } else if (!toStation && rocket.capsule != null) {
                ServerLevel target = server.getServer().getLevel(destination.body().getDimension());
                if (target != null)
                    com.hbm.space.satellite.SpaceSatellites.orbitFromRocket(target, rocket.capsuleStack(), satFreq, getX(), getY(), getZ());
            }
            discard();
            return;
        }
        if (toStation) {
            setState(RocketState.DOCKING);
            // the docking ring is always in the middle of the cell
            x = com.hbm.space.station.OrbitalStation.centre(x);
            z = com.hbm.space.station.OrbitalStation.centre(z);
            y = 0;
        }
        ServerLevel target = server.getServer().getLevel(destination.body().getDimension());
        if (toStation && target != null) com.hbm.space.station.OrbitalStation.spawn(target, x, z);
        Vec3 at = new Vec3(x + 0.5, y, z + 0.5);
        if (target == null || target == server) {
            setPos(at);
            return;
        }
        // passengers go along, and climb back in on arrival
        changeDimension(new DimensionTransition(target, at, Vec3.ZERO, getYRot(), getXRot(), DimensionTransition.PLACE_PORTAL_TICKET));
    }

    /** A crewed trip between bodies: coast in a temporary station cell until the transfer is done. */
    private void beginCelestialTransfer(Target from, Target to) {
        setDeltaMovement(Vec3.ZERO);
        setState(RocketState.TRANSFER);
        if (!(level() instanceof ServerLevel server)) return;
        com.hbm.space.SolarSystemSavedData data = com.hbm.space.SolarSystemSavedData.get(server);
        com.hbm.space.station.OrbitalStation station = com.hbm.space.station.OrbitalStation.addStation(data, from.body());
        RocketStruct rocket = getRocket();
        float thrust = rocket.stages.isEmpty() ? 600_000 : rocket.getThrust();
        double distance = SolarSystem.calculateDistanceBetweenTwoBodies(server, from.body(), to.body());
        station.setState(com.hbm.space.station.OrbitalStation.StationState.TRANSFER,
                com.hbm.space.station.OrbitalStation.calculateTransferTime(distance, 10, thrust));
        station.orbiting = from.body();
        station.target = to.body();
        Vec3 at = new Vec3(com.hbm.space.station.OrbitalStation.centre(station.dX) + 0.5, 128,
                com.hbm.space.station.OrbitalStation.centre(station.dZ) + 0.5);
        ServerLevel orbit = server.getServer().getLevel(com.hbm.space.SpaceDimensions.ORBIT);
        if (orbit == null || orbit == server) {
            setPos(at);
            return;
        }
        changeDimension(new DimensionTransition(orbit, at, Vec3.ZERO, getYRot(), getXRot(), DimensionTransition.PLACE_PORTAL_TICKET));
    }

    private static final double MOTION_MULT = 4;

    /** Steps toward each position the server sends instead of jumping to it, as boats do. */
    private final com.hbm.backport.InterpolationHandler interpolation = new com.hbm.backport.InterpolationHandler(this);

    @Override
    public com.hbm.backport.InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public void tick() {
        interpolation.interpolate();
        super.tick();
        RocketState state = getState();
        Player rider = getFirstPassenger() instanceof Player p ? p : null;

        if (!level().isClientSide()) {
            if (!dockChecked) {
                dockChecked = true;
                // a rocket loaded hanging under a port docks with it again
                if (state == RocketState.LANDED || state == RocketState.AWAITING || state == RocketState.NEEDSFUEL) {
                    BlockEntity under = level().getBlockEntity(BlockPos.containing(getX(), getY() + getBbHeight() - 1, getZ()));
                    if (under instanceof com.hbm.space.station.SpaceStations.PortBE port) port.dockRocket(this);
                    else if (under instanceof com.hbm.space.station.SpaceStationLauncher.LauncherBE launcher) launcher.dockRocket(this);
                }
            }
            setYRot(-90F);
            if (navDrive.getItem() instanceof ItemVOTVdrive) entityData.set(DRIVE, navDrive.copy());
            if (thrower == null && rider != null) thrower = rider;
            if (state == RocketState.AWAITING && ((rider != null && rider.jumping) || !canRide())) {
                attemptLaunch();
                thrower = rider;
            }
            if (thrower != null && rider == null && !canExitCapsule() && forceExitTimer < 60 && thrower.isAlive()
                    && thrower.level() == level()) thrower.startRiding(this, true);

            switch (state) {
                case LAUNCHING -> {
                    double acceleration = stateTimer / 120D;
                    if (isReusable()) {
                        setXRot(Mth.clamp((stateTimer - 60) * 0.3F, 0F, 45F));
                        if (rocketVelocity < 4) rocketVelocity += Mth.clamp(acceleration * 0.05D, 0, 0.05);
                    } else {
                        setXRot(Mth.clamp((stateTimer - 80) * 0.3F, 0F, 45F));
                        if (rocketVelocity < 4) rocketVelocity += Mth.clamp(acceleration * acceleration * 0.05D, 0, 0.05);
                    }
                }
                case LANDING -> {
                    double ground = groundHeight();
                    setXRot(0);
                    rocketVelocity = isReusable() ? Mth.clamp((ground - getY()) * 0.01, -1.0, -0.005)
                            : Mth.clamp((ground - getY()) * 0.005, -0.5, -0.005);
                    Destination destination = getDestination();
                    if (destination != null) {
                        int x = destination.x(), z = destination.z();
                        // someone parked on our spot: land beside them
                        if (!level().getEntities(this, getBoundingBox().setMinY(ground), e -> e instanceof EntityRideableRocket).isEmpty()) {
                            int d = random.nextBoolean() ? -5 : 5;
                            if (random.nextBoolean()) x += d;
                            else z += d;
                            ItemVOTVdrive.setCoordinates(navDrive, x, z);
                        }
                        setPos(x + 0.5, getY(), z + 0.5);
                    }
                    BlockPos at = blockPosition();
                    FluidState fluid = level().getFluidState(at);
                    if (!fluid.isEmpty()) {
                        setState(RocketState.TIPPING);
                        if (fluid.is(net.minecraft.tags.FluidTags.LAVA)) {
                            igniteForSeconds(15);
                            willExplode = true;
                        }
                    } else if (getY() <= ground + 0.05) {
                        land(ground);
                    }
                }
                case TIPPING -> {
                    float tip = stateTimer * 0.1F;
                    setXRot(Math.min(tip * tip, 90F));
                    if (getXRot() >= 90F && willExplode) {
                        dropNDie();
                        level().explode(this, getX(), getY(), getZ(), 5F, true, Level.ExplosionInteraction.TNT);
                    }
                    rocketVelocity = 0;
                }
                case DOCKING -> {
                    setXRot(0);
                    rocketVelocity = 0;
                    // the ports need a moment to load and register
                    if (stateTimer > 20) {
                        if (targetPort == null || targetPort.isRemoved())
                            targetPort = com.hbm.space.station.OrbitalStation.getPort(level(), getBlockX(), getBlockZ());
                        if (targetPort != null) {
                            rocketVelocity = 0.1;
                            BlockPos port = targetPort.getBlockPos();
                            setPos(port.getX() + 0.5, getY(), port.getZ() + 0.5);
                            if (targetPort.getDocked() != this) targetPort.despawnRocket();
                            targetPort.reservePort();
                            if (getY() + getBbHeight() > port.getY() + 1.5) {
                                setState(isReusable() ? RocketState.NEEDSFUEL : RocketState.LANDED);
                                destinationOverride = null;
                                rocketVelocity = 0;
                                setPos(getX(), port.getY() + 1.5 - getBbHeight(), getZ());
                                targetPort.dockRocket(this);
                                targetPort = null;
                            }
                        }
                    }
                }
                case UNDOCKING -> {
                    setXRot(0);
                    rocketVelocity = -0.1;
                }
                case TRANSFER -> {
                    rocketVelocity = 0;
                    setXRot(90);
                    forceExitTimer = 0; // never any way out mid-transfer
                    com.hbm.space.station.OrbitalStation station =
                            com.hbm.space.station.OrbitalStation.getStationFromPosition(level(), getBlockX(), getBlockZ());
                    station.update(level());
                    if (station.getUnscaledProgress(0) > 0.99 || station.state == com.hbm.space.station.OrbitalStation.StationState.ARRIVING) {
                        Target from = here(), to = getTarget();
                        if (level() instanceof ServerLevel server)
                            com.hbm.space.station.OrbitalStation.removeStation(com.hbm.space.SolarSystemSavedData.get(server), station);
                        beginLandingSequence(from, to);
                    }
                }
                default -> {
                    rocketVelocity = 0;
                    setXRot(0);
                }
            }

            if (state == RocketState.LAUNCHING) {
                // pitching over toward the east, as NTM:Space's ascent did
                float pitch = (float) Math.toRadians(getXRot());
                setDeltaMovement(Math.sin(pitch) * rocketVelocity, Math.cos(pitch) * rocketVelocity, 0);
            } else {
                setDeltaMovement(0, rocketVelocity, 0);
            }
            // NTM:Space's motionMult: a moving rocket covers four times its velocity each tick
            Vec3 step = getDeltaMovement().scale(MOTION_MULT);
            setPos(getX() + step.x, getY() + step.y, getZ() + step.z);

            if ((state == RocketState.LAUNCHING && getY() > 900) || (state == RocketState.UNDOCKING && getY() < 32)) {
                Target from = here(), to = getTarget();
                if (!canRide() || from.body() == to.body()) beginLandingSequence(from, to);
                else beginCelestialTransfer(from, to);
            }
            entityData.set(TIMER, ++stateTimer);
        } else {
            clientTick(state);
            stateTimer = getStateTimer();
        }
    }

    private double groundHeight() {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(getX()), Mth.floor(getZ()));
    }

    private void land(double ground) {
        setDeltaMovement(Vec3.ZERO);
        rocketVelocity = 0;
        RocketStruct rocket = getRocket();
        if (!rocket.stages.isEmpty() && rocket.stages.get(0).fins == null) {
            setState(RocketState.TIPPING);
            willExplode = true;
        } else {
            setState(RocketState.LANDED);
        }
        setPos(getX(), ground, getZ());
    }

    private void clientTick(RocketState state) {
        if (state != lastState) {
            if (state == RocketState.LAUNCHING) {
                AudioWrapper ignition = AudioSystem.getLoopedSound(SpaceRockets.ROCKET_IGNITION.get(), SoundSource.PLAYERS,
                        (float) getX(), (float) getY(), (float) getZ(), 1F, 250F, 1F, 5);
                if (ignition != null) {
                    ignition.setDoesRepeat(false);
                    ignition.startSound();
                }
            }
            lastState = state;
        }
        boolean burning = state == RocketState.LAUNCHING || (state == RocketState.LANDING && getDeltaMovement().y > -0.4);
        if (burning) {
            if (audio == null || !audio.isPlaying()) {
                audio = AudioSystem.getLoopedSound(getRocket().stages.size() <= 1 ? SpaceRockets.ROCKET_FLY_LIGHT.get()
                        : SpaceRockets.ROCKET_FLY_HEAVY.get(), SoundSource.PLAYERS, (float) getX(), (float) getY(), (float) getZ(), 1F, 250F, 1F, 5);
                if (audio != null) audio.startSound();
            }
            if (audio != null) {
                audio.updatePosition((float) getX(), (float) getY(), (float) getZ());
                audio.keepAlive();
            }
            spawnContrail(state);
        } else if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    @Override
    public void buildLookOverlay(Level level, com.hbm.interfaces.ILookOverlay.LookInfo info) {
        com.hbm.space.client.RocketLookOverlay.build(this, info);
    }

    /** NTM:Space's gasfire flames were drawn at 6.5 unless told otherwise. */
    private static final float GAS_FLAME_SCALE = 6.5F;

    private void spawnContrail(RocketState state) {
        RocketStruct rocket = getRocket();
        double x = getX(), y = getY(), z = getZ();
        // going up, from last tick's spot, so fast ascents don't spawn them inside the capsule
        if (y > yo) {
            x = xo;
            y = yo;
            z = zo;
        }
        if (rocket.stages.isEmpty()) {
            if (state == RocketState.TIPPING) return;
            if (isReusable()) {
                gasFlame(x + 0.5, y, z, 0, -1, 0);
                gasFlame(x - 0.5, y, z, 0, -1, 0);
                gasFlame(x, y, z + 0.5, 0, -1, 0);
                gasFlame(x, y, z - 0.5, 0, -1, 0);
            } else {
                double r = rocket.capsule != null ? radius(rocket.capsule.bottom) * 0.5 : 0.5;
                gasFlame(x + r, y, z + r, 0.25, -0.75, 0.25);
                gasFlame(x - r, y, z + r, -0.25, -0.75, 0.25);
                gasFlame(x + r, y, z - r, 0.25, -0.75, -0.25);
                gasFlame(x - r, y, z - r, -0.25, -0.75, -0.25);
            }
            return;
        }
        RocketStage stage = rocket.stages.get(0);
        if (state == RocketState.LANDING) {
            gasFlame(x, y, z, 0, -1, 0);
        } else if (state == RocketState.LAUNCHING || getStateTimer() < 200) {
            missileContrail(0, 0);
            int cluster = stage.getCluster();
            double r = stage.fuselage != null ? radius(stage.fuselage.bottom) : 1;
            for (int c = 1; c < cluster; c++) {
                double spin = (double) c / (cluster - 1) * Math.PI * 2;
                missileContrail(Math.cos(spin) * r, Math.sin(spin) * r);
            }
        }
    }

    private void gasFlame(double x, double y, double z, double mx, double my, double mz) {
        com.hbm.client.ClientEffects.spawnGasFlame(level(), x, y, z, mx, my, mz, GAS_FLAME_SCALE);
    }

    /** EntityMissileBaseNT's contrail: up to ten puffs a tick along the path, blown out the nozzle. */
    private void missileContrail(double ox, double oz) {
        Vec3 back = new Vec3(xo - getX(), yo - getY(), zo - getZ());
        double len = back.length();
        back = len < 1.0e-4 ? Vec3.ZERO : back.scale(1 / len);
        double pitch = Math.toRadians(getXRot());
        Vec3 thrust = new Vec3(Math.sin(pitch), Math.cos(pitch), 0);
        double pressure = com.hbm.api.environment.Environment.particlePressure(level());
        for (int i = 0; i < Math.max(Math.min(len, 10), 1); i++) {
            double j = i - len;
            com.hbm.client.ClientEffects.spawnRocketFlameInAir(level(),
                    getX() - back.x * j + ox, getY() - back.y * j, getZ() - back.z * j + oz,
                    1F, -thrust.x, -thrust.y, -thrust.z, 60 + random.nextInt(20), pressure);
        }
    }

    /** A part size's radius in blocks: half its diameter in metres, 1 for any size. */
    private static double radius(com.hbm.items.weapon.ItemCustomMissilePart.@Nullable PartSize size) {
        if (size == null) return 1;
        String name = size.name();
        if (!name.startsWith("SIZE_")) return 1;
        try {
            return Integer.parseInt(name.substring(5)) / 20D;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @Override
    public com.hbm.backport.InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!canRide()) return com.hbm.backport.InteractionResult.PASS;
        if (player.getItemInHand(hand).getItem() instanceof ItemVOTVdrive) return com.hbm.backport.InteractionResult.PASS;
        if (!level().isClientSide() && (getFirstPassenger() == null || getFirstPassenger() == player)) {
            player.startRiding(this);
            return com.hbm.backport.InteractionResult.SUCCESS;
        }
        return com.hbm.backport.InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && canRide();
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        // along the rocket's axis, which leans east as it pitches over (NTM:Space's updateRiderPosition)
        double length = isReusable() ? getBbHeight() - 2.5 : getBbHeight() - 3.0;
        double pitch = Math.toRadians(getXRot());
        return position().add(Math.sin(pitch) * length, Math.cos(pitch) * length, 0);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isRemoved() || !getPassengers().isEmpty() || !(source.getEntity() instanceof Player player)) return true;
        RocketStruct rocket = getRocket();
        boolean bare = rocket.stages.isEmpty() && !isReusable();
        // a capsule breaks by hand, a whole rocket needs a pickaxe
        if (bare || getState() == RocketState.TIPPING
                || player.getMainHandItem().isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()))
            dropNDie();
        return true;
    }

    public void dropNDie() {
        RocketStruct rocket = getRocket();
        if (rocket.stages.isEmpty()) {
            if (rocket.capsule != null) spawnAtLocation(rocket.capsuleStack());
        } else {
            spawnAtLocation(ItemCustomRocket.build(rocket, true));
        }
        if (!navDrive.isEmpty()) spawnAtLocation(navDrive);
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500_000;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("rocket", CompoundTag.CODEC).ifPresent(tag -> setRocket(RocketStruct.load(tag)));
        setState(RocketState.values()[Mth.clamp(input.getIntOr("state", 0), 0, RocketState.values().length - 1)]);
        stateTimer = input.getIntOr("timer", 0);
        entityData.set(TIMER, stateTimer);
        navDrive = input.read("drive", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("rocket", CompoundTag.CODEC, getRocket().save());
        output.putInt("state", getState().ordinal());
        output.putInt("timer", stateTimer);
        output.store("drive", ItemStack.OPTIONAL_CODEC, navDrive);
    }
}
