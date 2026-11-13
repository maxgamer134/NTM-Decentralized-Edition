// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.lib.Library;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * NTM:Space's ore tooltips: which celestial bodies an ore can be found on, from the bodies each
 * planet's world generator lists for it (BlockOre.addValidBody). Vanilla ores show their NTM:Space
 * counterpart's bodies; coal only exists on Earth.
 */
public final class SpaceOreLocations {

    private record Where(Kind kind, List<String> bodies) {}

    private enum Kind { ANYWHERE, EXCEPT, ON }

    private static final Map<ResourceLocation, Where> ORES = new HashMap<>();

    private SpaceOreLocations() {}

    private static void all(String ore) {
        ORES.put(Library.id(ore), new Where(Kind.ANYWHERE, List.of()));
    }

    private static void except(String ore, String... bodies) {
        ORES.put(Library.id(ore), new Where(Kind.EXCEPT, List.of(bodies)));
    }

    private static void on(String ore, String... bodies) {
        ORES.put(Library.id(ore), new Where(Kind.ON, List.of(bodies)));
    }

    private static void vanilla(String block, String ore) {
        ORES.put(ResourceLocation.withDefaultNamespace(block), ORES.get(Library.id(ore)));
    }

    static {
        all("cluster_aluminium");
        all("cluster_copper");
        all("cluster_iron");
        all("cluster_titanium");
        all("ore_aluminium");
        all("ore_beryllium");
        all("ore_cinnabar");
        all("ore_cobalt");
        all("ore_copper");
        all("ore_diamond");
        all("ore_fluorite");
        all("ore_gold");
        all("ore_iron");
        all("ore_lead");
        all("ore_niter");
        all("ore_rare");
        all("ore_redstone");
        all("ore_sulfur");
        all("ore_thorium");
        all("ore_titanium");
        all("ore_tungsten");
        all("ore_uranium");
        all("ore_zinc");
        except("ore_nickel", "kerbin");
        on("ore_asbestos", "ike", "kerbin", "laythe");
        on("ore_australium", "moho");
        on("ore_bedrock_oil", "kerbin");
        on("ore_brine", "ike", "minmus", "mun");
        on("ore_coltan", "dres", "ike", "kerbin");
        on("ore_emerald", "kerbin", "laythe");
        on("ore_fire", "moho");
        on("ore_gas", "eve");
        on("ore_glowstone", "moho");
        on("ore_iodine", "eve");
        on("ore_lanthanium", "dres");
        on("ore_lapis", "kerbin", "laythe");
        on("ore_lignite", "kerbin");
        on("ore_lithium", "ike", "mun");
        on("ore_mineral", "ike", "moho");
        on("ore_niobium", "dres", "eve");
        on("ore_oil", "duna", "kerbin", "laythe");
        on("ore_quartz", "mun");
        on("ore_schrabidium", "eve");
        on("ore_shale", "dres", "moho", "mun");
        on("ore_tekto", "tekto");
        vanilla("iron_ore", "ore_iron");
        vanilla("deepslate_iron_ore", "ore_iron");
        vanilla("gold_ore", "ore_gold");
        vanilla("deepslate_gold_ore", "ore_gold");
        vanilla("redstone_ore", "ore_redstone");
        vanilla("deepslate_redstone_ore", "ore_redstone");
        vanilla("lapis_ore", "ore_lapis");
        vanilla("deepslate_lapis_ore", "ore_lapis");
        vanilla("emerald_ore", "ore_emerald");
        vanilla("deepslate_emerald_ore", "ore_emerald");
        vanilla("nether_quartz_ore", "ore_quartz");
        vanilla("diamond_ore", "ore_diamond");
        vanilla("deepslate_diamond_ore", "ore_diamond");
        ORES.put(ResourceLocation.withDefaultNamespace("coal_ore"), new Where(Kind.ON, List.of("kerbin")));
        ORES.put(ResourceLocation.withDefaultNamespace("deepslate_coal_ore"), new Where(Kind.ON, List.of("kerbin")));
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> {
            Where where = ORES.get(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()));
            if (where == null) return;
            List<Component> tooltip = event.getToolTip();
            switch (where.kind()) {
                case ANYWHERE -> tooltip.add(Component.translatable("desc.ore.space.anywhere").withStyle(ChatFormatting.GOLD));
                case EXCEPT -> {
                    tooltip.add(Component.translatable("desc.ore.space.except").withStyle(ChatFormatting.GOLD));
                    for (String body : where.bodies())
                        tooltip.add(Component.literal(" - ").append(Component.translatable("body." + body)).withStyle(ChatFormatting.RED));
                }
                case ON -> {
                    tooltip.add(Component.translatable("desc.ore.space.on").withStyle(ChatFormatting.GOLD));
                    for (String body : where.bodies())
                        tooltip.add(Component.literal(" - ").append(Component.translatable("body." + body)).withStyle(ChatFormatting.AQUA));
                }
            }
        });
    }
}
