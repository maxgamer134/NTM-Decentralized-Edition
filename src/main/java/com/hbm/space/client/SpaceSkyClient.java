// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.lib.Library;
import com.hbm.space.SpaceSkySync;
import com.hbm.space.trait.CBT_War;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Hooks the Space sky, clouds and weather into the client: the sky through {@link SpaceSky}'s
 * custom sky renderer, the clouds and rain through {@link SpaceEffects} (the {@code hbm:space}
 * effects the Space dimension types name), and the sky's core shaders.
 */
public final class SpaceSkyClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    static @Nullable ShaderInstance crescent;
    static @Nullable ShaderInstance weather;
    static @Nullable ShaderInstance atmosphere;
    static @Nullable ShaderInstance atmosphereEmissive;
    static @Nullable ShaderInstance nightLights;
    static @Nullable ShaderInstance lightning;
    static @Nullable ShaderInstance blackhole;
    static @Nullable ShaderInstance nuke;
    static @Nullable ShaderInstance flesh;

    private SpaceSkyClient() {}

    /** Called once from the client entry point with the mod event bus. */
    public static void register(IEventBus modBus) {
        SpaceSky.register();
        modBus.addListener(SpaceSkyClient::onRegisterEffects);
        modBus.addListener(SpaceSkyClient::onRegisterShaders);
        // the sky's synced stations, satellites and flashes belong to the server just left
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> SpaceSkySync.clearClient());
        // CBT_War's projectiles thud as they hit (SkyProviderCelestial played it as travel reached 18)
        CBT_War.clientTick = (war, body) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            for (CBT_War.Projectile projectile : war.getProjectiles()) {
                if (projectile.getTravel() == 18) {
                    mc.player.playSound(SpaceSkySync.WAR_IMPACT.get(), 10F, 1F);
                }
            }
        };
    }

    private static void onRegisterEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(SpaceEffects.ID, new SpaceEffects());
    }

    private static void onRegisterShaders(RegisterShadersEvent event) {
        load(event, "space_crescent", DefaultVertexFormat.POSITION_TEX, s -> crescent = s);
        load(event, "space_weather", DefaultVertexFormat.PARTICLE, s -> weather = s);
        load(event, "space_atmosphere", DefaultVertexFormat.POSITION_TEX, s -> atmosphere = s);
        load(event, "space_atmosphere_emissive", DefaultVertexFormat.POSITION_TEX, s -> atmosphereEmissive = s);
        load(event, "space_nightlights", DefaultVertexFormat.POSITION_TEX, s -> nightLights = s);
        load(event, "space_lightning", DefaultVertexFormat.POSITION_TEX, s -> lightning = s);
        load(event, "space_blackhole", DefaultVertexFormat.POSITION_TEX, s -> blackhole = s);
        load(event, "space_nuke", DefaultVertexFormat.POSITION_TEX, s -> nuke = s);
        load(event, "space_flesh", DefaultVertexFormat.POSITION, s -> flesh = s);
    }

    private static void load(RegisterShadersEvent event, String name, VertexFormat format, Consumer<ShaderInstance> sink) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), Library.id(name), format), sink);
        } catch (IOException e) {
            LOGGER.error("Failed to load Space sky shader {}", name, e);
        }
    }
}
