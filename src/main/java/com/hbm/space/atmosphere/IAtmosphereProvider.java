// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.atmosphere;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;

/** A machine that fills a sealed volume with gas, as air vents do. */
public interface IAtmosphereProvider {

    ServerLevel getAtmosphereLevel();

    /** How far from the root the volume may reach before it counts as a leak. */
    int getMaxBlobRadius();

    /** Where the volume starts; if that is sealed off, the volume pops. */
    BlockPos getRootPosition();

    Fluid getFluidType();

    /** The gas pressure in bar; below zero, it takes that much away (a vacuum pump). */
    double getFluidPressure();

    void consume(int amount);

    void produce(int amount);

    void onBlobCreated(AtmosphereBlob blob);
}
