// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

/** main 1.0.27_X5000's gun makers; the name key is gun.make.<constant>. */
public enum EnumGunManufacturer {
    ARMALITE,
    AUTO_ORDINANCE,
    BAE,
    BENELLI,
    BLACK_MESA,
    CERIX,
    COLT,
    COMBINE,
    CUBE,
    DRG,
    ENZINGER,
    EQUESTRIA,
    F_PRICE,
    F_STRONG,
    FLIMFLAM,
    GLORIA,
    H_AND_K,
    H_AND_R,
    HASBRO,
    IF,
    IMI,
    IMI_BIGMT,
    LANGFORD,
    MAGNUM_R_IMI,
    MANN,
    MAXIM,
    METRO,
    MWT,
    NAWS,
    ERFURT,
    NONE,
    OXFORD,
    LUNA,
    RAYTHEON,
    REMINGTON,
    ROCKWELL,
    ROCKWELL_U,
    RYAN,
    SAAB,
    SACO,
    TULSKY,
    UAC,
    UNKNOWN,
    WESTTEK,
    WGW,
    WINCHESTER,
    WINCHESTER_BIGMT;

    public String getKey() {
        return "gun.make." + name();
    }
}
