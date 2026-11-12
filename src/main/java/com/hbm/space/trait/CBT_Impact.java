// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/** The world time an impactor struck the body. */
public class CBT_Impact extends CelestialBodyTrait {

    public long time;

    public CBT_Impact() {}

    public CBT_Impact(long time) {
        this.time = time;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putLong("time", time);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        time = nbt.getLong("time");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeLong(time);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        time = buf.readLong();
    }
}
