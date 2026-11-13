// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Hooks NTM:Space into the game, only while it is switched on. */
public final class SpaceHooks {

    private SpaceHooks() {}

    /** After common setup, once the fluids the bodies name are bound. */
    public static void commonSetup() {
        if (!SpaceToggle.enabled()) return;
        SolarSystem.init();
        SpaceTools.commonSetup();
    }

    public static void register(IEventBus modBus) {
        if (!SpaceToggle.enabled()) return;
        com.hbm.space.dim.SpaceWorldgen.register(modBus);
        com.hbm.space.satellite.SpaceSatellites.install();
        // the impact's extinction reaches every body
        if (SpaceToggle.changes()) com.hbm.handler.ImpactWorldHandler.EXTINCTION_LEVELS.install("hbm_space", level -> true);
        com.hbm.space.satellite.SatSettings.install();
        SpaceRespawn.init();
        // riders lean back with the rocket as it pitches east (ModEventHandlerRenderer.onRenderRidingPlayerPre)
        com.hbm.render.util.RiderTilt.POINT.install("hbm_space", (vehicle, partialTick) ->
                vehicle instanceof com.hbm.space.rocket.EntityRideableRocket rocket
                        ? -net.minecraft.util.Mth.lerp(partialTick, rocket.xRotO, rocket.getXRot()) : 0F);
        com.hbm.api.environment.Environment.PROVIDER.install("hbm_space", new com.hbm.space.atmosphere.SpaceEnvironment());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
            {
                com.hbm.space.atmosphere.SpaceAtmosphere.tick(level);
                com.hbm.space.atmosphere.SpaceChemistry.tick(level);
                SpaceTime.tick(level);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.SleepFinishedTimeEvent event) -> {
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
            {
                event.setTimeAddition(SpaceTime.morning(level, event.getNewTime()));
                // sleeping through the night clears the planet's sky too
                var weather = SpaceTime.body(level) == null ? null
                        : SpaceTime.body(level).getTrait(com.hbm.space.trait.CBT_Weather.class);
                if (weather != null) {
                    weather.forceClear(level.random, level.random.nextInt(168000) + 12000);
                    SolarSystemSavedData.get(level).changed();
                }
            }
        });
        // a bed only becomes your spawn where you could breathe waking up in it
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent event) -> {
            if (event.getNewSpawn() == null || event.getEntity().level().dimension() != event.getSpawnLevel()) return;
            if (SpaceTime.body(event.getEntity().level()) == null && !CelestialBody.inOrbit(event.getEntity().level())) return;
            if (event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level
                    && !com.hbm.space.atmosphere.SpaceAtmosphere.canBreathe(
                            com.hbm.space.atmosphere.SpaceAtmosphere.getAtmosphere(level, event.getNewSpawn())))
                event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.BlockGrowFeatureEvent event) -> {
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                com.hbm.space.atmosphere.SpaceAtmosphere.onTreeGrow(level, event.getPos());
        });
        // gravity runs on both sides, so motion stays in step with the client's prediction
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) -> {
            if (event.getEntity() instanceof ServerPlayer player && SpaceToggle.changes())
                com.hbm.space.atmosphere.SpaceEnvironment.tickReentry(player);
            if (event.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
                com.hbm.space.atmosphere.SpaceEnvironment.tickGravity(living);
                com.hbm.space.SpaceTools.tickBoots(living);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingFallEvent event) ->
                event.setDistance(com.hbm.space.atmosphere.SpaceEnvironment.fallDistance(event.getEntity(), event.getDistance())));
        NeoForge.EVENT_BUS.addListener(
                (ServerTickEvent.Post event) -> {
                    SolarSystemSavedData.get(event.getServer().overworld()).tick(event.getServer());
                    com.hbm.space.trait.CBT_Weather.updateGlobalWeather(event.getServer());
                });
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerLoggedInEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player)
                        SolarSystemSyncPayload.sendTo(player, SolarSystemSavedData.get(player.serverLevel()));
                });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> SolarSystemSavedData.onServerStopped());

        // NTM:Space city lights: bright blocks built above the horizon show on the night side from orbit
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) -> {
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
                light(level, event.getPos(), event.getPlacedBlock(), true);
                netherPortal(level, event.getPos(), event.getPlacedBlock());
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) -> {
            // placing onto a spotlight beam counts as a break upstream; beams never count as lights here
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
                    && !(event.getState().getBlock() instanceof com.hbm.blocks.machine.BlockSpotlightBeam))
                light(level, event.getPos(), event.getState(), false);
        });
        // NTM:Space logs who placed a bomb when extended logging is on
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player) || !SpaceToggle.changes()) return;
            net.minecraft.world.level.block.Block block = event.getPlacedBlock().getBlock();
            if (!isLoggedBomb(block) || !com.hbm.config.NeoForgeHbmConfig.runtime().extendedLogging()) return;
            com.hbm.NuclearTech.LOGGER.info("[BOMBPL] {} placed at {} / {} / {}! by {}", block.getName().getString(),
                    event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), player.getName().getString());
        });
        com.hbm.api.block.BlockPlacements.onRepaired((level, pos) -> light(level, pos, level.getBlockState(pos), true));
    }

    /** SpaceConfig.allowNetherPortals: fire lights a portal frame on any body, not just the overworld and nether. */
    private static void netherPortal(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state) {
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock)
                || !com.hbm.config.NeoForgeHbmConfig.runtime().spaceNetherPortals()) return;
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD || level.dimension() == net.minecraft.world.level.Level.NETHER
                || level.dimension() == net.minecraft.world.level.Level.END) return;
        net.minecraft.world.level.portal.PortalShape.findEmptyPortalShape(level, pos, net.minecraft.core.Direction.Axis.X)
                .ifPresent(net.minecraft.world.level.portal.PortalShape::createPortalBlocks);
    }

    private static final Class<?>[] LOGGED_BOMBS = {
            com.hbm.blocks.bomb.BlockChargeBase.class, com.hbm.blocks.bomb.BlockDynamite.class,
            com.hbm.blocks.bomb.BlockFireworks.class, com.hbm.blocks.bomb.BlockFissureBomb.class,
            com.hbm.blocks.bomb.BlockSemtex.class, com.hbm.blocks.bomb.BlockTNT.class,
            com.hbm.blocks.bomb.BlockVolcano.class, com.hbm.blocks.bomb.BombFlameWar.class,
            com.hbm.blocks.bomb.BlockFloatBomb.class, com.hbm.blocks.bomb.BombMulti.class,
            com.hbm.blocks.bomb.BlockThermoBomb.class, com.hbm.blocks.bomb.BlockExplosiveCharge.class,
            com.hbm.blocks.bomb.BlockLandmine.class, com.hbm.blocks.bomb.NukeBalefire.class,
            com.hbm.blocks.bomb.NukeBoy.class, com.hbm.blocks.bomb.NukeFleija.class,
            com.hbm.blocks.bomb.NukeGadget.class, com.hbm.blocks.bomb.NukeMan.class,
            com.hbm.blocks.bomb.NukeMike.class, com.hbm.blocks.bomb.NukeN2.class,
            com.hbm.blocks.bomb.NukePrototype.class, com.hbm.blocks.bomb.NukeSolinium.class,
            com.hbm.blocks.bomb.NukeTsar.class};

    private static boolean isLoggedBomb(net.minecraft.world.level.block.Block block) {
        for (Class<?> type : LOGGED_BOMBS) if (type.isInstance(block)) return true;
        return false;
    }

    private static void light(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state, boolean add) {
        if (pos.getY() < SpaceWeather.horizon(level) || state.getLightEmission() <= 10) return;
        CelestialBody body = CelestialBody.getBody(level);
        com.hbm.space.trait.CBT_Lights lights = body.getTrait(com.hbm.space.trait.CBT_Lights.class);
        if (lights == null) lights = new com.hbm.space.trait.CBT_Lights();
        if (add) lights.addLight(state);
        else lights.removeLight(state);
        body.modifyTraits(lights);
    }
}
