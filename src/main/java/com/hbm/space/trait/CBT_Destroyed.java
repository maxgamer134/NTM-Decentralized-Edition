// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import com.hbm.space.CelestialBody;
import net.minecraft.nbt.CompoundTag;

/** A body being torn apart; clients ease the progress towards 200 and loop it for the effect. */
public class CBT_Destroyed extends CelestialBodyTrait {

    public float destProgress;

    public CBT_Destroyed() {}

    public CBT_Destroyed(float destProgress) {
        this.destProgress = destProgress;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putFloat("destProgress", destProgress);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        destProgress = nbt.getFloat("destProgress");
    }

    @Override
    public void update(boolean isRemote, CelestialBody body) {
        if (!isRemote) return;
        destProgress = Math.min(201F, destProgress + 0.0025F * (201F - destProgress) * 0.15F);
        if (destProgress >= 200) destProgress = 0;
    }
}
