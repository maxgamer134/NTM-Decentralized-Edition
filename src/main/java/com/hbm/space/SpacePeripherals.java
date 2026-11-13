// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.compat.computercraft.ComputerCraft;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.space.machine.SpaceVents;
import com.hbm.space.rocket.ItemVOTVdrive;
import com.hbm.space.rocket.SpaceLaunchPad;
import com.hbm.space.rocket.SpaceStardar;
import com.hbm.space.trait.CBT_Atmosphere;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.ArrayList;
import java.util.List;

/**
 * NTM:Space's OpenComputers components for its own machines: the atmospheric vent
 * ("ntm_atmospheric_vent"), the Stardar ("ntm_stardar") and the rocket pad ("ntm_rocket_pad").
 * Every function runs on the server thread.
 */
public final class SpacePeripherals {
    private SpacePeripherals() {}

    static {
        ComputerCraft.addon(SpaceVents.AirVentBE.class, Vent::new, false, SpaceVents.VENT);
        ComputerCraft.addon(SpaceStardar.StardarBE.class, Stardar::new, true, SpaceStardar.BLOCK);
        ComputerCraft.addon(SpaceLaunchPad.PadBE.class, Pad::new, true, SpaceLaunchPad.BLOCK);
    }

    public static final class Vent extends SnapshotPeripheral<SpaceVents.AirVentBE> {
        public Vent(SpaceVents.AirVentBE be) {
            super(be, "ntm_atmospheric_vent", 0, 0);
        }

        @Override
        protected void capture(SpaceVents.AirVentBE be) {}

        @LuaFunction(mainThread = true)
        public final Object[] getAtmosphereDetails() {
            CBT_Atmosphere atmosphere = machine().atmosphere;
            if (atmosphere == null || atmosphere.fluids == null || atmosphere.fluids.isEmpty())
                return new Object[] {"VACUUM"};
            List<Object[]> fluids = new ArrayList<>();
            for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids)
                fluids.add(new Object[] {NTMFluids.legacyName(entry.fluid), entry.pressure});
            return fluids.toArray();
        }

        @LuaFunction(mainThread = true)
        public final Object[] getFluid() {
            FluidTankNTM tank = machine().tank;
            return new Object[] {tank.getFill(), tank.getMaxFill()};
        }

        @LuaFunction(mainThread = true)
        public final Object[] isSealed() {
            return new Object[] {machine().hasSeal()};
        }
    }

    public static final class Stardar extends SnapshotPeripheral<SpaceStardar.StardarBE> {
        public Stardar(SpaceStardar.StardarBE be) {
            super(be, "ntm_stardar", 0, 0);
        }

        @Override
        protected void capture(SpaceStardar.StardarBE be) {}

        @LuaFunction(mainThread = true)
        public final Object[] getPlanetStats(String name) {
            CelestialBody body = CelestialBody.getBodyOrNull(name);
            if (body == null) return new Object[] {null, "No body with that name found."};
            String parentName = null;
            double orbitalPeriod = 0.0;
            if (body.parent != null) {
                parentName = body.parent.name;
                orbitalPeriod = body.getOrbitalPeriod();
            }
            return new Object[] {
                body.name, parentName, body.getStar().name, body.tidallyLockedTo, body.axialTilt, body.canLand,
                body.massKg, body.getProcessingLevel(CelestialBody.getBody(machine().getLevel())), body.radiusKm,
                body.semiMajorAxisKm, body.getSunPower(), body.getSurfaceGravity(), body.getRotationalPeriod(),
                orbitalPeriod
            };
        }

        @LuaFunction(mainThread = true)
        public final Object[] getCurrentPlanet() {
            return new Object[] {CelestialBody.getBody(machine().getLevel()).name};
        }

        @LuaFunction(mainThread = true)
        public final Object[] getSatellites(String name) {
            CelestialBody body = CelestialBody.getBodyOrNull(name);
            if (body == null) return new Object[] {null, "No body with that name found."};
            List<String> names = new ArrayList<>();
            for (CelestialBody planet : body.satellites) names.add(planet.name);
            return names.toArray();
        }
    }

    public static final class Pad extends SnapshotPeripheral<SpaceLaunchPad.PadBE> {
        public Pad(SpaceLaunchPad.PadBE be) {
            super(be, "ntm_rocket_pad", 0, 0);
        }

        @Override
        protected void capture(SpaceLaunchPad.PadBE be) {}

        @LuaFunction(mainThread = true)
        public final Object[] getEnergyInfo() {
            return new Object[] {machine().power, machine().getMaxPower()};
        }

        @LuaFunction(mainThread = true)
        public final Object[] getFuel() {
            List<Object[]> out = new ArrayList<>();
            for (FluidTankNTM tank : machine().tanks) {
                if (tank.getTankType() == null || tank.getTankType() == NTMFluids.NONE) continue;
                out.add(new Object[] {tank.getFill(), tank.getMaxFill(), NTMFluids.legacyName(tank.getTankType())});
            }
            return out.toArray();
        }

        @LuaFunction(mainThread = true)
        public final Object[] getSolidFuel() {
            return new Object[] {machine().solidFuel.level, machine().solidFuel.max};
        }

        @LuaFunction(mainThread = true)
        public final Object[] canLaunch() {
            return new Object[] {machine().canLaunch()};
        }

        @LuaFunction(mainThread = true)
        public final Object[] getRocketStats() {
            SpaceLaunchPad.PadBE pad = machine();
            if (pad.hasRocket() && pad.rocket != null)
                return new Object[] {pad.rocket.stages.size(), pad.rocket.getLaunchMass(), pad.rocket.getHeight()};
            return new Object[] {null, ""};
        }

        @LuaFunction(mainThread = true)
        public final Object[] launch() {
            machine().launch();
            return new Object[] {};
        }

        @LuaFunction(mainThread = true)
        public final Object[] getDestination() {
            SpaceLaunchPad.PadBE pad = machine();
            // backport-fix: NTM:Space reported "No destination drive." when there was one
            if (!pad.hasDrive()) return new Object[] {null, "No destination drive."};
            ItemVOTVdrive.Target target = ItemVOTVdrive.getTarget(pad.getItem(SpaceLaunchPad.PadBE.DRIVE), pad.getLevel());
            if (target.body() != null) return new Object[] {target.body().name.toLowerCase(java.util.Locale.ROOT)};
            return new Object[] {null, "Drive has no destination."};
        }
    }
}
