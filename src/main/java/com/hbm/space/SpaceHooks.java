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
        com.hbm.api.environment.Environment.PROVIDER.install("hbm_space", new com.hbm.space.atmosphere.SpaceEnvironment());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
            {
                com.hbm.space.atmosphere.SpaceAtmosphere.tick(level);
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
            if (SpaceTime.body(event.getEntity().level()) == null) return;
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
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                light(level, event.getPos(), event.getPlacedBlock(), true);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) -> {
            // placing onto a spotlight beam counts as a break upstream; beams never count as lights here
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
                    && !(event.getState().getBlock() instanceof com.hbm.blocks.machine.BlockSpotlightBeam))
                light(level, event.getPos(), event.getState(), false);
        });
        com.hbm.api.block.BlockPlacements.onRepaired((level, pos) -> light(level, pos, level.getBlockState(pos), true));
    }

    private static void light(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state, boolean add) {
        if (pos.getY() < level.getSeaLevel() || state.getLightEmission() <= 10) return;
        CelestialBody body = CelestialBody.getBody(level);
        com.hbm.space.trait.CBT_Lights lights = body.getTrait(com.hbm.space.trait.CBT_Lights.class);
        if (lights == null) lights = new com.hbm.space.trait.CBT_Lights();
        if (add) lights.addLight(state);
        else lights.removeLight(state);
        body.modifyTraits(lights);
    }
}
