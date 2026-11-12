// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

/** Forge 1.7's six directions, in its order. */
public enum ForgeDirection {
    DOWN(0, -1, 0), UP(0, 1, 0), NORTH(0, 0, -1), SOUTH(0, 0, 1), WEST(-1, 0, 0), EAST(1, 0, 0), UNKNOWN(0, 0, 0);

    public static final ForgeDirection[] VALID_DIRECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};
    public final int offsetX, offsetY, offsetZ;

    ForgeDirection(int x, int y, int z) {
        offsetX = x;
        offsetY = y;
        offsetZ = z;
    }

    public static ForgeDirection getOrientation(int id) {
        return id >= 0 && id < VALID_DIRECTIONS.length ? VALID_DIRECTIONS[id] : UNKNOWN;
    }

    public ForgeDirection getOpposite() {
        return switch (this) {
            case DOWN -> UP; case UP -> DOWN; case NORTH -> SOUTH; case SOUTH -> NORTH; case WEST -> EAST; case EAST -> WEST; default -> UNKNOWN;
        };
    }
}
