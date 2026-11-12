// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * How Space's saved data names fluids: by registry name on disk (stable when fluids are added or
 * removed) and by synced registry id on the wire. A fluid no longer registered reads as null.
 */
public final class SpaceFluidIO {

    private SpaceFluidIO() {}

    public static String name(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid).toString();
    }

    public static @Nullable Fluid byName(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null) return null;
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        return fluid == Fluids.EMPTY ? null : fluid;
    }

    public static void write(FriendlyByteBuf buf, Fluid fluid) {
        buf.writeVarInt(BuiltInRegistries.FLUID.getId(fluid));
    }

    public static @Nullable Fluid read(FriendlyByteBuf buf) {
        Fluid fluid = BuiltInRegistries.FLUID.byId(buf.readVarInt());
        return fluid == Fluids.EMPTY ? null : fluid;
    }
}
