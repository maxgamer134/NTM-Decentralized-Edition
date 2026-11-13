// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.rocket;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Multiblock layouts built in code, for Space's machines: the baked layout file only holds what
 * the bake tool saw, and Space must not touch it. Cells are given in the core's local frame (local
 * +z is the way the machine faces, as {@link MultiblockMaskTable#worldX} reads it); NTM's
 * {up, down, north, south, west, east} boxes map straight onto it.
 */
public final class SpaceLayouts {

    private final Set<Long> cells = new LinkedHashSet<>();
    private final Set<Long> ports = new LinkedHashSet<>();
    private final Set<Long> heat = new LinkedHashSet<>();
    private final java.util.Map<Long, Integer> portMasks = new java.util.HashMap<>();
    private double height = 1.0;

    /** Gives every cell and the core a box this tall instead of a full block, as NTM:Space's gas dock. */
    public SpaceLayouts height(double height) {
        this.height = height;
        return this;
    }

    /** Adds a box around a local origin, in NTM's {up, down, north, south, west, east} form. */
    public SpaceLayouts box(int ox, int oy, int oz, int... dims) {
        for (int x = ox - dims[4]; x <= ox + dims[5]; x++)
            for (int y = oy - dims[1]; y <= oy + dims[0]; y++)
                for (int z = oz - dims[2]; z <= oz + dims[3]; z++) cells.add(BlockPos.asLong(x, y, z));
        return this;
    }

    /** A cell pipes, cables and conveyors connect to on every side. */
    public SpaceLayouts port(int x, int y, int z) {
        long at = BlockPos.asLong(x, y, z);
        cells.add(at);
        ports.add(at);
        return this;
    }

    /** A port that connects on only some sides, as NTM's machines that refuse connections from below. */
    public SpaceLayouts port(int x, int y, int z, int mask) {
        port(x, y, z);
        portMasks.put(BlockPos.asLong(x, y, z), mask);
        return this;
    }

    /** A cell other machines can draw heat from, as the converters' heat vents. */
    public SpaceLayouts heat(int x, int y, int z) {
        long at = BlockPos.asLong(x, y, z);
        cells.add(at);
        heat.add(at);
        return this;
    }

    /** Leaves a cell out, for a part the machine places as its own block. */
    public SpaceLayouts remove(int x, int y, int z) {
        long at = BlockPos.asLong(x, y, z);
        cells.remove(at);
        ports.remove(at);
        return this;
    }

    public MultiblockLayout build(BlockMultiblockCore core, int declaredCaps, int[] dims, int offset, int heightOffset) {
        int domains = core.passiveCoreDomains(declaredCaps);
        int roles = declaredCaps & BlockMultiblockCore.ROLE_ALL;
        MultiblockMaskTable.Builder table = MultiblockMaskTable.builder()
                .cell(0, 0, 0, BlockMultiblockCore.MASK_NONE, BlockMultiblockCore.ROLE_ALL)
                .passive(0, 0, 0, core.passiveCoreMask(), domains);
        long origin = BlockPos.asLong(0, 0, 0);
        int size = 0;
        for (long cell : cells) if (cell != origin) size++;
        long[][] offsets = new long[4][size];
        int i = 0;
        for (long cell : cells) {
            if (cell == origin) continue;
            int x = BlockPos.getX(cell), y = BlockPos.getY(cell), z = BlockPos.getZ(cell);
            boolean port = ports.contains(cell) && roles != 0;
            int mask = portMasks.getOrDefault(cell, BlockMultiblockCore.MASK_ALL);
            table.cell(x, y, z, port ? mask : BlockMultiblockCore.MASK_NONE, BlockMultiblockCore.ROLE_ALL);
            if (port) table.passive(x, y, z, mask, domains);
            if (heat.contains(cell)) table.flag(x, y, z, MultiblockMaskTable.FLAG_HEAT);
            for (int f = 0; f < 4; f++) {
                Direction facing = Direction.from2DDataValue(f);
                offsets[f][i] = BlockPos.asLong(MultiblockMaskTable.worldX(x, y, z, facing), y,
                        MultiblockMaskTable.worldZ(x, y, z, facing));
            }
            i++;
        }
        MultiblockMaskTable built = table.build();
        int[] preview = new int[9];
        System.arraycopy(dims, 0, preview, 0, 6);
        return new MultiblockLayout(declaredCaps,
                new MultiblockMaskTable[] {built, built, built, built},
                offsets,
                new long[][] {MultiblockLayout.NO_CELLS, MultiblockLayout.NO_CELLS, MultiblockLayout.NO_CELLS, MultiblockLayout.NO_CELLS},
                new MultiblockLayout.Placement(dims, offset, heightOffset, preview, new double[0]),
                height >= 1.0 ? MultiblockLayout.CellGeometry.NONE : geometry(offsets[0].length));
    }

    private MultiblockLayout.CellGeometry geometry(int size) {
        double[] box = {0, 0, 0, 1, height, 1};
        long[] local = new long[size];
        int i = 0;
        long origin = BlockPos.asLong(0, 0, 0);
        for (long cell : cells) if (cell != origin) local[i++] = cell;
        double[][] core = new double[16][];
        java.util.Arrays.fill(core, box);
        return new MultiblockLayout.CellGeometry(
                new String[] {"box:" + height},
                new double[][][] {{box, box, box, box}},
                new double[1][],
                true,
                new long[][] {local, local, local, local},
                new int[4][size],
                core,
                new double[4][]);
    }
}
