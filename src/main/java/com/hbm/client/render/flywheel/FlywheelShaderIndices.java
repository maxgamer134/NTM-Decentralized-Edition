// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.client.render.flywheel;

import com.hbm.NuclearTech;
import dev.engine_room.flywheel.backend.MaterialShaderIndices;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * backport-fix: BF-134 the game froze the first time each new effect showed up. Flywheel builds
 * every cutout and fog shader a material names into each of its shader programs, and the first
 * time a material brings one it has not seen, it throws away every program it has built
 * ({@code MaterialShaderIndices.Index.index} calls {@code PipelineCompiler.deleteAll}); everything
 * on screen then compiles its programs again in that frame, up to seconds. HBM's particles, flashes
 * and clouds each bring their own (cutout/particle, cutout/zero, fog/fade, ...) and appear long
 * after the world, so each new kind of effect cost one of those rebuilds. Main draws with the
 * fixed-function pipeline and has no shaders to build, so its effects never stall.
 *
 * <p>Every cutout and fog source HBM and Flywheel ship is handed to Flywheel while resources load,
 * before anything is drawn, so the set is complete from the start and never grows mid-game.
 */
final class FlywheelShaderIndices {

    private FlywheelShaderIndices() {}

    static void prime(ResourceManager resources) {
        try {
            prime(resources, "cutout", MaterialShaderIndices.cutoutSources());
            prime(resources, "fog", MaterialShaderIndices.fogSources());
        } catch (LinkageError error) {
            // a Flywheel whose backend changed shape: effects only lose this head start
            NuclearTech.LOGGER.warn("Couldn't pre-register Flywheel shader sources", error);
        }
    }

    private static void prime(
            ResourceManager resources, String kind, MaterialShaderIndices.Index index) {
        String dir = "flywheel/" + kind;
        for (ResourceLocation file :
                resources.listResources(dir, id -> id.getPath().endsWith(".glsl")).keySet()) {
            String namespace = file.getNamespace();
            if (!namespace.equals(NuclearTech.MOD_ID) && !namespace.equals("flywheel")) continue;
            // a shader names its source without the flywheel/ folder: hbm:cutout/zero.glsl
            index.index(
                    ResourceLocation.fromNamespaceAndPath(
                            namespace, file.getPath().substring("flywheel/".length())));
        }
    }
}
