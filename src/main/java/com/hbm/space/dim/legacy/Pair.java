// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

/** NTM's Tuple.Pair, which NTM:Space's noise samplers return. */
public class Pair<X, Y> {
    public X key;
    public Y value;

    public Pair(X key, Y value) {
        this.key = key;
        this.value = value;
    }

    public X getKey() {
        return key;
    }

    public Y getValue() {
        return value;
    }
}
