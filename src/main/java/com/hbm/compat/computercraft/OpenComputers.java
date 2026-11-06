// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

import net.neoforged.fml.ModList;

/** The OpenComputers: Rebooted gate; OcBridge (and with it OC's API) is only linked when loaded. */
public final class OpenComputers {

    public static final String MOD_ID = "opencomputers";
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    private OpenComputers() {}

    /** Adds the machines to OC's Adapter block; run on the main thread during common setup. */
    public static void init() {
        if (LOADED) OcBridge.register();
    }

    public static Object[] unknownError() {
        return new Object[] {null, "unknown error"};
    }
}
