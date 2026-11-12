// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid;

import com.google.gson.JsonElement;
import com.hbm.lib.Library;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class FluidPropertyReloadListener
        extends SimpleJsonResourceReloadListener {

    public static final ResourceLocation ID = Library.id("fluid_property");

    public FluidPropertyReloadListener() {
        // backport: 1.21.1's listener hands over raw JSON by directory (26.x decodes with a codec)
        super(new com.google.gson.Gson(), "fluid_property");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> entries,
            ResourceManager manager,
            ProfilerFiller profiler) {
        List<String> failures = new ArrayList<>();
        int loaded = 0;

        entries = withSpace(entries, manager);
        Map<Fluid, NTMFluidProperty> replacement = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            Fluid fluid = BuiltInRegistries.FLUID.get(id);
            if (fluid == Fluids.EMPTY) {
                failures.add(id + " names no registered fluid");
                continue;
            }
            DataResult<NTMFluidProperty> parsed =
                    NTMFluidPropertyCodec.CODEC.parse(JsonOps.INSTANCE, entry.getValue());
            if (parsed.error().isPresent()) {
                failures.add(id + ": " + parsed.error().get().message());
                continue;
            }
            replacement.put(fluid, parsed.result().orElseThrow());
            loaded++;
        }

        if (!failures.isEmpty()) {
            throw new IllegalStateException(
                    failures.size()
                            + " fluid property file(s) failed to load: "
                            + failures.subList(0, Math.min(5, failures.size())));
        }
        if (loaded == 0) {
            throw new IllegalStateException(
                    "no fluid property loaded - every machine, gauge and tooltip "
                            + "would read as though no fluid had properties at all");
        }
        NTMFluidProperties.replace(replacement);
        NTMFluidProperties.markResyncPending();
    }

    /**
     * NTM:Space's fluid data, read only while Space is on. Its own fluids ({@code space/fluid_property})
     * go in first, so a data pack's {@code fluid_property} file for one still wins. Its edits to
     * existing fluids ({@code space_changes/fluid_property}, with {@code space_changes} on) replace a
     * fluid's file only while that file still comes from the same pack as the edit, NTM's own jar:
     * a fluid a data pack has redefined keeps the data pack's version (NTM:Space let its edits
     * override custom fluid data, its issue 512).
     */
    private static Map<ResourceLocation, JsonElement> withSpace(
            Map<ResourceLocation, JsonElement> entries, ResourceManager manager) {
        if (!com.hbm.space.SpaceToggle.enabled()) return entries;
        com.google.gson.Gson gson = new com.google.gson.Gson();
        Map<ResourceLocation, JsonElement> merged = new java.util.HashMap<>();
        scanDirectory(manager, "space/fluid_property", gson, merged);
        merged.putAll(entries);
        if (!com.hbm.space.SpaceToggle.changes()) return merged;
        Map<ResourceLocation, JsonElement> changes = new java.util.HashMap<>();
        scanDirectory(manager, "space_changes/fluid_property", gson, changes);
        changes.forEach(
                (id, json) -> {
                    String base = pack(manager, id.withPath("fluid_property/" + id.getPath() + ".json"));
                    String edit =
                            pack(manager, id.withPath("space_changes/fluid_property/" + id.getPath() + ".json"));
                    if (base == null || base.equals(edit)) merged.put(id, json);
                });
        return merged;
    }

    private static @org.jetbrains.annotations.Nullable String pack(
            ResourceManager manager, ResourceLocation file) {
        return manager.getResource(file).map(net.minecraft.server.packs.resources.Resource::sourcePackId).orElse(null);
    }
}
