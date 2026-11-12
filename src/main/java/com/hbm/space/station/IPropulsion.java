// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.station;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

/** An engine a station can travel with; it registers itself with the station it stands on. */
public interface IPropulsion {

    default void registerPropulsion() {
        BlockEntity be = getBlockEntity();
        if (be.getLevel() != null) OrbitalStation.addPropulsion(be.getLevel(), this);
    }

    default void unregisterPropulsion() {
        BlockEntity be = getBlockEntity();
        if (be.getLevel() != null) OrbitalStation.removePropulsion(be.getLevel(), this);
    }

    BlockEntity getBlockEntity();

    /** Whether the engine has the fuel to change a mass's velocity by deltaV. */
    boolean canPerformBurn(int shipMass, double deltaV);

    /** Why {@link #canPerformBurn} failed. */
    void addErrors(List<Component> errors);

    /** Higher is faster travel. */
    float getThrust();

    /** Starts the engine; ticks until it gives thrust. */
    int startBurn();

    /** Stops the engine; ticks until another trip can start. */
    int endBurn();
}
