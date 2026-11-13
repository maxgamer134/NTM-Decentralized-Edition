// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.client;

import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystemSavedData;
import com.hbm.space.SpaceToggle;
import com.hbm.space.trait.CelestialBodyTrait;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** NTM:Space's client side hooks, only while it is switched on. */
public final class SpaceClientHooks {

    private static final java.util.function.Supplier<com.hbm.render.loader.HFRWavefrontObject> AIRLINER =
            com.google.common.base.Suppliers.memoize(() -> com.hbm.client.model.Meshes.load(com.hbm.lib.Library.id("models/737.obj")));

    private static final net.minecraft.resources.ResourceLocation FLASHBANG =
            com.hbm.lib.Library.id("textures/misc/overlay_flashbang.png");

    private SpaceClientHooks() {}

    private static final net.minecraft.resources.ResourceLocation AIR = net.minecraft.resources.ResourceLocation.withDefaultNamespace("hud/air");
    private static final net.minecraft.resources.ResourceLocation AIR_BURSTING = net.minecraft.resources.ResourceLocation.withDefaultNamespace("hud/air_bursting");

    private static void renderOxygen(net.minecraft.client.gui.GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        var gui = mc.gui;
        int right = graphics.guiWidth() / 2 + 91;
        int air = com.hbm.space.SpaceOxygenPayload.clientOxygen;
        if (air < 100) {
            int top = graphics.guiHeight() - gui.rightHeight;
            int full = net.minecraft.util.Mth.ceil((air - 2) * 10.0D / 100.0D);
            int partial = net.minecraft.util.Mth.ceil(air * 10.0D / 100.0D) - full;
            for (int i = 0; i < full + partial; i++)
                graphics.blitSprite(i < full ? AIR : AIR_BURSTING, right - i * 8 - 9, top, 9, 9);
            gui.rightHeight += 10;
        }
        var tank = com.hbm.space.atmosphere.SpaceEnvironment.oxygenTank(player);
        if (tank.getItem() instanceof com.hbm.space.SpaceExtras.OxyTank oxy) {
            int capacity = oxy.capacity(tank);
            float tot = capacity <= 0 ? 0F : net.minecraft.util.Mth.clamp((float) oxy.getFuel(tank) / capacity, 0F, 1F);
            int top = graphics.guiHeight() - gui.rightHeight + 3;
            graphics.fill(right - 82, top - 1, right + 1, top + 5, 0xFF404040);
            int r = (int) ((1F - tot) * 255), gb = (int) (tot * 255);
            graphics.fill(right - (int) (81 * tot), top, right, top + 4, 0xFF000000 | r << 16 | gb << 8 | gb);
            gui.rightHeight += 6;
        }
    }

    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        if (!SpaceToggle.enabled()) return;
        // Not at construction: the first touch of RecipePages builds every NTM page, which needs the registries.
        modBus.addListener(
                (net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) ->
                        event.enqueueWork(com.hbm.integration.recipeviewer.SpaceRecipePages::register));
        com.hbm.space.client.SpaceSkyClient.register(modBus);
        com.hbm.space.satellite.SatSettings.OPEN_SCREEN = player -> Minecraft.getInstance().setScreen(new ScreenSatSettings(player));
        SpaceVacuumTorex.register();
        // NTM:Space's airliner (bomber style 9)
        com.hbm.client.render.BomberStyles.register(9,
                () -> AIRLINER.get(), com.hbm.lib.Library.id("textures/models/airliner.png"), 30F / 3.1F, 180F);
        com.hbm.space.client.SpaceSiegeClient.register();
        // ModEventHandlerRenderer: the bubbles of breath left in a vacuum, and the oxygen tank's gauge
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) ->
                        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.AIR_LEVEL,
                                com.hbm.lib.Library.id("space_oxygen"), (graphics, delta) -> renderOxygen(graphics)));
        // RenderScreenOverlay.renderTaintBar: Nitan wraps the hearts in a tainted frame
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) ->
                        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.PLAYER_HEALTH,
                                com.hbm.lib.Library.id("nitan_bar"), (graphics, delta) -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    if (mc.player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
                                    if (!mc.player.hasEffect(com.hbm.potion.HbmPotion.nitan())) return;
                                    graphics.blit(com.hbm.render.util.RenderScreenOverlay.misc,
                                            graphics.guiWidth() / 2 - 92, graphics.guiHeight() - 41, 146, 18, 81, 12);
                                }));
        // ModEventHandlerClient: the flashbang effect whites out the screen
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) ->
                        event.registerBelow(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR,
                                com.hbm.lib.Library.id("flashbang"), (graphics, delta) -> {
                                    var player = net.minecraft.client.Minecraft.getInstance().player;
                                    if (player == null || !player.hasEffect(com.hbm.potion.HbmPotion.flashbang())) return;
                                    com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                                    com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                                    graphics.blit(FLASHBANG, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0F, 0F, 256, 256, 256, 256);
                                    com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                                }));
        com.hbm.space.client.SpaceFog.register();
        com.hbm.space.client.SpacePlushieFigures.register(); com.hbm.space.client.SpaceBobbleFigures.register();
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) ->
                        event.register(
                                (state, level, pos, tint) -> com.hbm.space.BlockSpaceTint.color(state),
                                com.hbm.space.SpaceBlocks.tinted()));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerBlockEntityRenderer(
                                com.hbm.space.SpaceGeysers.ELECTRIC_BE.get(), ctx -> new RenderGeysirElectric()));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerBlockEntityRenderer(
                                com.hbm.space.SpaceAntimatter.BE.get(), ctx -> new RenderNukeAntimatter()));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerEntityRenderer(
                                com.hbm.space.rocket.SpaceRockets.ROCKET_ENTITY.get(), RenderRideableRocket::new));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerBlockEntityRenderer(
                                com.hbm.space.rocket.SpaceAssembly.BE.get(), ctx -> new RenderRocketAssembly()));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                        event.register(com.hbm.space.rocket.SpaceAssembly.MENU.get(), ScreenRocketAssembly::new));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerBlockEntityRenderer(
                                com.hbm.space.rocket.SpaceLaunchPad.BE.get(), ctx -> new RenderLaunchPadRocket()));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                        event.register(com.hbm.space.rocket.SpaceLaunchPad.MENU.get(), ScreenLaunchPadRocket::new));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerBlockEntityRenderer(
                                com.hbm.space.rocket.SpaceDriveProcessor.BE.get(), ctx -> new RenderDriveProcessor()));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                        event.register(com.hbm.space.rocket.SpaceDriveProcessor.MENU.get(), ScreenDriveProcessor::new));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                        event.register(com.hbm.space.station.SpaceStationLauncher.MENU.get(), ScreenStationLauncher::new));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                        event.register(com.hbm.space.machine.SpaceTransporter.MENU.get(), ScreenTransporterRocket::new));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerBlockEntityRenderer(
                                com.hbm.space.rocket.SpaceStardar.BE.get(), ctx -> new RenderStardar()));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                        event.register(com.hbm.space.rocket.SpaceStardar.MENU.get(), ScreenStardar::new));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) -> {
                    event.register(com.hbm.space.machine.SpaceMilkReformer.MENU.get(), ScreenMilkReformer::new);
                    event.register(com.hbm.space.machine.SpaceVacuumCircuit.MENU.get(), ScreenVacuumCircuit::new);
                });
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                        event.register(com.hbm.space.machine.SpaceHydroponic.MENU.get(), ScreenHydroponic::new));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                        event.register(com.hbm.space.machine.SpaceCryoDistiller.MENU.get(), ScreenCryoDistiller::new));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) -> {
                    event.registerBlockEntityRenderer(com.hbm.space.station.SpaceStations.PORT_BE.get(), ctx -> new RenderOrbitalStation());
                    event.registerBlockEntityRenderer(com.hbm.space.station.SpaceStations.COMPUTER_BE.get(), ctx -> new RenderOrbitalComputer());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceSolar.BE.get(), ctx -> new RenderSolarPanel());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceRadiator.BE.get(), ctx -> new RenderRadiator());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceScrubber.BE.get(), ctx -> new RenderScrubber());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceXenonThruster.BE.get(), ctx -> new RenderXenonThruster());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceHTR3.BE.get(), ctx -> new RenderHTR3());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceHTRF.BE.get(), ctx -> new com.hbm.space.client.RenderHTRF());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceTransporter.BE.get(), ctx -> new RenderTransporterRocket());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceMagma.BE.get(), ctx -> new com.hbm.space.client.RenderMagma());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceDishControl.BE.get(), ctx -> new RenderDishControl());
                    event.registerBlockEntityRenderer(com.hbm.space.SpaceInvaders.COMBAT_DROP_BE.get(), ctx -> new RenderCombatPod());
                    event.registerBlockEntityRenderer(com.hbm.space.station.SpaceStationLauncher.BE.get(), ctx -> new RenderStationLauncher());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceMilkReformer.BE.get(), ctx -> new RenderMilkReformer());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceAtmoMachines.COMPRESSOR_BE.get(), ctx -> new RenderAtmo.Compressor());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceAtmoMachines.EMITTER_BE.get(), ctx -> new RenderAtmo.Emitter());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceGasDock.BE.get(), ctx -> new RenderGasDock());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceHydroponic.BE.get(), ctx -> new RenderHydroponic());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceCryoDistiller.BE.get(), ctx -> new RenderCryoDistiller());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceAtmoTower.BE.get(), ctx -> new RenderAtmoTower());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceOrrery.BE.get(), ctx -> new RenderOrrery());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceVacuumCircuit.BE.get(), RenderVacuumCircuit::new);
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceDyson.LAUNCHER_BE.get(), ctx -> new RenderDyson.Launcher());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceDyson.RECEIVER_BE.get(), ctx -> new RenderDyson.Receiver());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceDyson.CONVERTER_HE_BE.get(), ctx -> new RenderDyson.ConverterHE());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceDyson.CONVERTER_TU_BE.get(), ctx -> new RenderDyson.ConverterTU());
                    event.registerBlockEntityRenderer(com.hbm.space.machine.SpaceDyson.CONVERTER_ANATMO_BE.get(), ctx -> new RenderDyson.ConverterAnatmo());
                    event.registerEntityRenderer(com.hbm.space.SpaceMobs.FLESH_CREEPER.get(), ctx -> new com.hbm.client.render.RenderCreeperUniversal(
                            ctx, com.hbm.lib.Library.id("textures/entity/creeper_flesh.png"), 1F));
                    event.registerEntityRenderer(com.hbm.space.SpaceRobots.TANKBOT.get(), ctx -> new RenderSpaceRobot<>(ctx, false));
                    event.registerEntityRenderer(com.hbm.space.SpaceRobots.WAR_BEHEMOTH.get(), ctx -> new RenderSpaceRobot<>(ctx, true));
                    event.registerEntityRenderer(com.hbm.space.SpaceInvaders.BF_ANGEL.get(), RenderBFA::new);
                    event.registerEntityRenderer(com.hbm.space.SpaceInvaders.SIEGE_UFO.get(), RenderSiegeUFO::new);
                    event.registerEntityRenderer(com.hbm.space.SpaceInvaders.COMBAT_POD.get(), RenderCombatPod.Falling::new);
                    event.registerEntityRenderer(com.hbm.space.SpaceSiege.SIEGE_CRAFT.get(), RenderSiegeCraft::new);
                    event.registerEntityRenderer(com.hbm.space.SpaceSeaMobs.SCUTTERFISH.get(), ctx -> new RenderSpaceFish<>(ctx, RenderSpaceFish.Kind.SCUTTER, "scutterfish", 0.3F, "scutterfish"));
                    event.registerEntityRenderer(com.hbm.space.SpaceSeaMobs.SCUTTLECRAB.get(), ctx -> new RenderSpaceFish<>(ctx, RenderSpaceFish.Kind.SCUTTLECRAB, "scuttlecrab", 0.5F, "scuttlecrab", com.hbm.space.SpaceSeaMobs.Scuttlecrab.VARIANTS));
                    event.registerEntityRenderer(com.hbm.space.SpaceSeaMobs.DEPTH_SQUID.get(), ctx -> new RenderSpaceFish<>(ctx, RenderSpaceFish.Kind.DEPTH_SQUID, "depthsquid", 0.5F, "depthsquid", com.hbm.space.SpaceSeaMobs.DepthSquid.VARIANTS));
                    event.registerEntityRenderer(com.hbm.space.SpaceSeaMobs.SCRAPFISH.get(), ctx -> new RenderSpaceFish<>(ctx, RenderSpaceFish.Kind.SCRAP_FISH, "scrapfish", 0.5F, "scrapfish", com.hbm.space.SpaceSeaMobs.ScrapFish.VARIANTS));
                    event.registerEntityRenderer(com.hbm.space.SpaceSeaMobs.SIFTER_EEL.get(), ctx -> new RenderSpaceFish<>(ctx, RenderSpaceFish.Kind.SIFTER_EEL, "siftereel", 0.5F, "siftereel", com.hbm.space.SpaceSeaMobs.SifterEel.VARIANTS));
                    event.registerEntityRenderer(com.hbm.space.SpaceMobs.MOON_COW.get(), ctx -> new net.minecraft.client.renderer.entity.CowRenderer(ctx) {
                        @Override
                        public net.minecraft.resources.ResourceLocation getTextureLocation(net.minecraft.world.entity.animal.Cow cow) {
                            return com.hbm.lib.Library.id("textures/entity/moon_cow.png");
                        }
                    });
                });
        com.hbm.space.station.SpaceStations.openComputer =
                pos -> Minecraft.getInstance().setScreen(new ScreenOrbitalComputer(pos));
        NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.tick.LevelTickEvent.Pre event) -> {
                    if (!(event.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel level)
                            || level.dimension() != com.hbm.space.SpaceDimensions.ORBIT) return;
                    com.hbm.space.station.OrbitalStation.clientStation.update(level);
                    for (net.minecraft.world.entity.Entity entity : level.entitiesForRendering())
                        com.hbm.space.station.StationEvents.drift(entity);
                });
        modBus.addListener(
                (com.hbm.backport.client.gui.render.pip.RegisterPictureInPictureRenderersEvent event) ->
                        event.register(RocketPreview.State.class, RocketPreview::new));
        modBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                        event.register(com.hbm.space.SpaceAntimatter.MENU.get(), ScreenNukeAntimatter::new));
        modBus.addListener(
                (FMLClientSetupEvent event) ->
                        event.enqueueWork(() -> {
                            if (Minecraft.getInstance().isSameThread()) SolarSystemSavedData.markClientThread();
                            com.hbm.space.machine.SpaceHTR3.HTR3BE.CONTRAIL = RenderHTR3::contrail;
                            com.hbm.space.machine.SpaceLPW2.LPW2BE.CONTRAIL = com.hbm.space.client.LPW2Contrail::spawn;
                            com.hbm.space.machine.SpaceTransporter.OPEN_LINKER = ScreenTransporterLinker::open;
                            // RenderBatterySocket: a socketed angel core is the balefire angel's body, turning
                            com.hbm.client.render.SocketCentrepieces.register(com.hbm.space.SpaceFuels.CORE_ANGEL.get(),
                                    new com.hbm.client.render.SocketCentrepieces.Centrepiece(
                                            () -> com.hbm.client.model.Meshes.load(com.hbm.lib.Library.id("models/mobs/balefire_angel.obj")),
                                            "body", com.hbm.lib.Library.id("textures/entity/bfa_body.png"), 2F, 5F));
                        }));
        // NTM:Space pulls the third-person camera out to 12 blocks while you ride a rocket
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent event) -> {
            if (event.getCamera().getEntity().getVehicle() instanceof com.hbm.space.rocket.EntityRideableRocket)
                event.setDistance(12F);
        });
        // a rocket's rider leans with it, about their own feet, as the rocket leans about its base
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                (net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?> event) -> {
                    if (!(event.getEntity().getVehicle() instanceof com.hbm.space.rocket.EntityRideableRocket rocket)) return;
                    event.getPoseStack().pushPose();
                    event.getPoseStack().mulPose(com.hbm.backport.LaunchAxis.rotation(rocket.nose(rocket.getViewXRot(event.getPartialTick()))));
                });
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,
                (net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) -> {
                    if (event.getEntity().getVehicle() instanceof com.hbm.space.rocket.EntityRideableRocket)
                        event.getPoseStack().popPose();
                });
        SpaceAmbience.register();
        SpaceOreLocations.register();
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> {
                    SolarSystemSavedData.onClientDisconnect();
                    com.hbm.space.SpaceOxygenPayload.clientOxygen = 100;
                });
        NeoForge.EVENT_BUS.addListener(
                (ClientTickEvent.Post event) -> {
                    if (Minecraft.getInstance().level == null || Minecraft.getInstance().isPaused()) return;
                    for (Map.Entry<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> entry :
                            SolarSystemSavedData.clientTraits().entrySet()) {
                        CelestialBody body = CelestialBody.getBodyOrNull(entry.getKey());
                        if (body == null) continue;
                        for (CelestialBodyTrait trait : entry.getValue().values()) trait.update(true, body);
                    }
                });
    }
}
