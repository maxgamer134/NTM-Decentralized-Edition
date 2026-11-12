// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine.fusion;

public interface IFusionPowerReceiver {
    boolean receivesFusionPower();

    void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b);

    /** The plasma fluid the reactor's recipe makes ({@link com.hbm.space.FusionPlasmas}), sent before its power; null for none. */
    default void receivePlasmaType(net.minecraft.world.level.material.@org.jspecify.annotations.Nullable Fluid plasma) {}
}
