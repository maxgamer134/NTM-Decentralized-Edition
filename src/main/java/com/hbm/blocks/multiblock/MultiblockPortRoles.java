// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.capability.MachineCaps;
import com.hbm.lib.Library;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

// main: machines whose power ports and fluid ports are separate lists (setupPowerPorts / setupFluidPorts).
// The baked layouts open every port to every role, so these limit each port cell to main's role.
// Cells are in the layout's local frame (core facing south).
final class MultiblockPortRoles {

    private static final int POWER = MachineCaps.POWER_IN | MachineCaps.POWER_OUT;
    private static final int FLUID = MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT;

    private static final Map<ResourceLocation, MultiblockMaskTable.CellRoles> ROLES =
            Map.of(
                    // TileEntityMachineTurbineGas: power only at the power port, fluids everywhere else
                    Library.id("machine_turbinegas"),
                    (x, y, z) -> x == -4 && y == 1 && z == 0 ? POWER : FLUID,
                    // TileEntityMachineIndustrialTurbine: power only at the back, fluids everywhere else
                    Library.id("machine_industrial_turbine"),
                    (x, y, z) -> x == 0 && y == 1 && z == -3 ? POWER : FLUID,
                    // the assembly and chemical factories take power and fluids at every port: their
                    // coolant cells (y 0, |x| 1, |z| 2) carry power and the coolant loop (main's
                    // coolant line delegate), and fluidAccess keeps recipe fluids off them
                    // battery socket: the top face cells are power only, the side cells keep all
                    Library.id("machine_battery_socket"),
                    (x, y, z) -> y == 1 ? POWER : ~0);

    private MultiblockPortRoles() {}

    static MultiblockLayout apply(ResourceLocation id, MultiblockLayout layout) {
        MultiblockMaskTable.CellRoles roles = ROLES.get(id);
        return roles == null ? layout : layout.withRoles(roles);
    }
}
