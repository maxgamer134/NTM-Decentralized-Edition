// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

import org.jspecify.annotations.Nullable;

/**
 * The argument reads of the few free-form computer methods (main's OC versions took the whole
 * Arguments): CC hands in its IArguments, OpenComputers its Arguments, each with its own errors.
 */
public interface ComputerArgs {

    int count();

    @Nullable Object get(int index);

    int getInt(int index);

    String getString(int index);
}
