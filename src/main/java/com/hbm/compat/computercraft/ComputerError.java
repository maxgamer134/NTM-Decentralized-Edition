// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

/**
 * An error a computer method raises: CC's LuaException with CC loaded, so CC shows the same message
 * as before, else an IllegalArgumentException, whose message OpenComputers shows as the error.
 */
public final class ComputerError {

    private ComputerError() {}

    public static RuntimeException of(String message) {
        return ComputerCraft.LOADED ? CcBridge.error(message) : new IllegalArgumentException(message);
    }
}
