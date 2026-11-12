// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/** How many bees live on a body, in hundreds. */
public class CBT_Bees extends CelestialBodyTrait {

    public float bees;

    public CBT_Bees() {}

    public CBT_Bees(float bees) {
        this.bees = bees;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putFloat("bees", bees);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        bees = nbt.getFloat("bees");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeFloat(bees);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        bees = buf.readFloat();
    }
}
