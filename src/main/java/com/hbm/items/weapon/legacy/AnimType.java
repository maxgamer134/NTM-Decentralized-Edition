// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

/** main 1.0.27_X5000's animation slots of the pre-rework guns. */
public enum AnimType {
    RELOAD,
    RELOAD_EMPTY,
    RELOAD_CYCLE,
    RELOAD_END,
    CYCLE,
    CYCLE_EMPTY,
    ALT_CYCLE,
    SPINUP,
    SPINDOWN,
    EQUIP;

    public static final AnimType[] VALUES = values();
}
