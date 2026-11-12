// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;

/** City lights: the summed light of lamps on the body, drawn on its night side from afar. */
public class CBT_Lights extends CelestialBodyTrait {

    public int lights;
    public boolean isCivilized;

    public CBT_Lights() {}

    public CBT_Lights(int lights) {
        this.lights = lights;
    }

    public int getIntensity() {
        if (lights > 10000) return 3;
        if (lights > 2000) return 2;
        if (lights > 400) return 1;
        return 0;
    }

    public void addLight(BlockState state) {
        lights += state.getLightEmission();
    }

    public void removeLight(BlockState state) {
        lights = Math.max(0, lights - state.getLightEmission());
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putInt("lights", lights);
        nbt.putBoolean("isCiv", isCivilized);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        lights = nbt.getInt("lights");
        isCivilized = nbt.getBoolean("isCiv");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeInt(lights);
        buf.writeBoolean(isCivilized);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        lights = buf.readInt();
        isCivilized = buf.readBoolean();
    }
}
