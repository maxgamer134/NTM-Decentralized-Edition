// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/** A body's surface temperature, in degrees Celsius. */
public class CBT_Temperature extends CelestialBodyTrait {

    public float degrees;

    public CBT_Temperature() {
        this(20);
    }

    public CBT_Temperature(float degrees) {
        this.degrees = degrees;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putFloat("c", degrees);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        degrees = nbt.getFloat("c");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeFloat(degrees);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        degrees = buf.readFloat();
    }
}
