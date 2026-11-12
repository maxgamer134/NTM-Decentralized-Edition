// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.space.SpaceFluidIO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.material.Fluid;

/** A body's water table, or whatever fluid cycle stands in for one (Eve's mercury, Minmus' milk). */
public class CBT_Water extends CelestialBodyTrait {

    public Fluid fluid;

    public CBT_Water() {
        this(NTMFluids.WATER);
    }

    public CBT_Water(Fluid fluid) {
        this.fluid = fluid;
    }

    // saved by registry name, not by the fluid's numeric id, so the table survives fluids being
    // added or removed (NTM:Space saved ids, which shifted, its issue 424)
    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putString("fluid", SpaceFluidIO.name(fluid));
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        Fluid read = SpaceFluidIO.byName(nbt.getString("fluid"));
        fluid = read != null ? read : NTMFluids.WATER;
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        SpaceFluidIO.write(buf, fluid);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        Fluid read = SpaceFluidIO.read(buf);
        fluid = read != null ? read : NTMFluids.WATER;
    }
}
