// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

/** 1.7's int array pool; plain arrays now. */
public final class IntCache {
    private IntCache() {}

    public static int[] getIntCache(int size) {
        return new int[size];
    }

    public static void resetIntCache() {}
}
