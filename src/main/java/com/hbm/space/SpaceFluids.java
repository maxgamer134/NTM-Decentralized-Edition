// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space;

import com.hbm.inventory.fluid.NTMFluids;

/**
 * The fluids NTM:Space adds, registered through the addon fluid API only while Space is on. They
 * take legacy ids in the order Space's Fluids defined them, and each sits in the display order
 * after the fluid Space's metaOrder listed before it. Their properties are JSON in the Space data
 * pack ({@code data/hbm/fluid_property}).
 */
public final class SpaceFluids {

    /** Name, then the fluid it follows in the display order. */
    private static final String[] FLUIDS = {
            "NITROGEN", "MERCURY",
            "AMMONIA", "HCL",
            "HYDRAZINE", "NITAN",
            "BLOODGAS", "BLOOD",
            "EARTHAIR", "CHLOROETHANE",
            "HCL", "NITRIC_ACID",
            "MINSOL", "ENDERJUICE",
            "NEON", "ARGON",
            "ARGON", "XENON",
            "KRYPTON", "PLASMA_BF",
            "COFFEE", "HONEY",
            "TEA", "NEON",
            "HONEY", "TEA",
            "MILK", "COFFEE",
            "SMILK", "MILK",
            "CCL", "SMILK",
            "EVEAIR", "DUNAAIR",
            "CHLOROMETHANE", "LITHCARBONATE",
            "METHANOL", "CHLOROMETHANE",
            "KMNO4", "HTCO4",
            "BROMINE", "SULFURIC_ACID",
            "CHLOROETHANE", "POLYTHYLENE",
            "POLYTHYLENE", "METHANOL",
            "FLUORINE", "CHLORINE",
            "TEKTOAIR", "NGAS",
            "ELBOWGREASE", "CCL",
            "NMASSTETRANOL", "ETHANOL",
            "NMASS", "MINSOL",
            "SCUTTERBLOOD", "BLOOD_HOT",
            "HTCO4", "RADIOSOLVENT",
            "JOOLGAS", "EVEAIR",
            "SARNUSGAS", "JOOLGAS",
            "UGAS", "SARNUSGAS",
            "NGAS", "UGAS",
            "EMILK", "ELBOWGREASE",
            "CMILK", "EMILK",
            "CREAM", "CMILK",
            "MAGMA", "BALEFIRE_FUEL",
            "DICYANOACETYLENE", "CONGLOMERA",
            "BRINE", "MAGMA",
            "CONGLOMERA", "BRINE",
            "DUNAAIR", "EARTHAIR",
            "SUPERHEATED_HYDROGEN", "MUG_HOT",
            "GAS_WATZ", "SUPERHEATED_HYDROGEN",
            "URANIUM_BROMIDE", "BROMINE",
            "AQUEOUS_COPPER", "THORIUM_BROMIDE",
            "COPPERSULFATE", "AQUEOUS_COPPER",
            "THORIUM_BROMIDE", "URANIUM_BROMIDE",
            "GASEOUS_URANIUM_BROMIDE", "GAS_WATZ",
            "GASEOUS_PLUTONIUM_BROMIDE", "GASEOUS_URANIUM_BROMIDE",
            "GASEOUS_SCHRABIDIUM_BROMIDE", "GASEOUS_PLUTONIUM_BROMIDE",
            "GASEOUS_THORIUM_BROMIDE", "GASEOUS_SCHRABIDIUM_BROMIDE",
            "HGAS", "DICYANOACETYLENE",
            "AQUEOUS_NICKEL", "ALUMINA",
            "VINYL", "HGAS",
            "TCRUDE", "VINYL",
            "CBENZ", "TCRUDE",
            "HALOLIGHT", "CBENZ",
            "LITHYDRO", "COPPERSULFATE",
            "LITHCARBONATE", "LITHYDRO"
    };

    private SpaceFluids() {}

    public static void init() {
        if (!SpaceToggle.enabled()) return;
        for (int i = 0; i < FLUIDS.length; i += 2) NTMFluids.register("hbm", FLUIDS[i], FLUIDS[i + 1]);
    }
}
