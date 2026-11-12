// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim;

import com.hbm.lib.Library;
import com.hbm.space.SpaceToggle;
import com.hbm.space.dim.legacy.Features;
import com.hbm.space.dim.legacy.LegacyBlocks;
import com.hbm.space.dim.legacy.World;
import com.hbm.world.ore.OreGenerationProfile;
import com.hbm.world.ore.OreHeightProfile;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The ores NTM:Space's planet generators place, as ore generation page rows. Each planet's
 * decorators run once on a world with no level behind it, with every ore placement written down
 * instead of placed.
 */
public final class SpaceOreProfiles {

    private static final ResourceLocation FEATURE = Library.id("space_planet_ore");
    private static volatile List<OreGenerationProfile> profiles;

    private SpaceOreProfiles() {}

    public static List<OreGenerationProfile> profiles() {
        if (!SpaceToggle.enabled()) return List.of();
        List<OreGenerationProfile> built = profiles;
        if (built == null) profiles = built = build();
        return built;
    }

    private static List<OreGenerationProfile> build() {
        List<OreGenerationProfile> rows = new ArrayList<>();
        List<Planet> planets = new ArrayList<>(Planets.all());
        planets.sort(Comparator.comparing(planet -> planet.name));
        for (Planet planet : planets) {
            World world = planet.world(0L);
            List<Features.Vein> veins =
                    Features.record(
                            () -> {
                                for (Planet.Decorator decorator : planet.decorators) {
                                    try {
                                        // chunk 1,1: chunk 0,0 of Duna builds the martian base
                                        decorator.generate(new Random(0L), 1, 1, world);
                                    } catch (RuntimeException e) {
                                        LogUtils.getLogger()
                                                .warn("Could not list {}'s ores", planet.name, e);
                                    }
                                }
                            });
            ResourceLocation dimension = Library.id(planet.name);
            int index = 0;
            for (Features.Vein vein : veins) {
                Block block = LegacyBlocks.state(vein.ore(), vein.meta()).getBlock();
                if (block == Blocks.AIR) continue;
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                Block target = vein.target() == null ? world.stone() : vein.target();
                List<Component> conditions = new ArrayList<>();
                conditions.add(
                        Component.translatable(
                                "ore.hbm.host", block.getName(), target.getName()));
                conditions.add(Component.translatable("ore.hbm." + vein.kind()));
                rows.add(
                        new OreGenerationProfile(
                                Library.id("space/" + planet.name + "/" + index++ + "_" + id.getPath()),
                                dimension,
                                FEATURE,
                                List.of(),
                                List.of(id),
                                vein.kind().equals("stratum_3d")
                                        ? OreHeightProfile.band(vein.minY(), vein.maxY())
                                        : OreHeightProfile.uniform(vein.minY(), vein.maxY()),
                                vein.attempts(),
                                vein.size(),
                                conditions));
            }
        }
        return List.copyOf(rows);
    }
}
