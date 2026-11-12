// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.space.SpaceFluidIO;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** A body's atmosphere: the partial pressure of each gas in it, in atmospheres. */
public class CBT_Atmosphere extends CelestialBodyTrait {

    public List<FluidEntry> fluids = new ArrayList<>();

    public static class FluidEntry {
        public Fluid fluid;
        public double pressure;

        public FluidEntry(Fluid fluid, double pressure) {
            this.fluid = fluid;
            this.pressure = pressure;
        }
    }

    public CBT_Atmosphere() {}

    public CBT_Atmosphere(Fluid fluid, double pressure) {
        fluids.add(new FluidEntry(fluid, pressure));
    }

    public CBT_Atmosphere and(Fluid fluid, double pressure) {
        fluids.add(new FluidEntry(fluid, pressure));
        return this;
    }

    @Override
    public CBT_Atmosphere copy() {
        CBT_Atmosphere clone = new CBT_Atmosphere();
        for (FluidEntry entry : fluids) clone.fluids.add(new FluidEntry(entry.fluid, entry.pressure));
        return clone;
    }

    /** Adds pressure of a gas, to its entry if it has one. */
    public void add(Fluid fluid, double pressure) {
        for (FluidEntry entry : fluids) {
            if (entry.fluid == fluid) {
                entry.pressure += pressure;
                return;
            }
        }
        fluids.add(new FluidEntry(fluid, pressure));
    }

    /** Takes pressure away from every gas in proportion, leaving none if it takes it all. */
    public void reduce(double pressure) {
        double total = getPressure();
        if (pressure >= total) {
            fluids = new ArrayList<>();
            return;
        }
        // NTM:Space scaled each gas by pressure / total, which kept the amount removed and dropped
        // the rest; what stays is (total - pressure) / total of each
        double keep = (total - pressure) / total;
        for (FluidEntry entry : fluids) entry.pressure *= keep;
    }

    /** A gas counts as present from 0.1 millibar. */
    public boolean hasFluid(Fluid fluid) {
        return hasFluid(fluid, 0.0001);
    }

    public boolean hasFluid(Fluid fluid, double abovePressure) {
        for (FluidEntry entry : fluids) if (entry.fluid == fluid) return entry.pressure >= abovePressure;
        return false;
    }

    public boolean hasTrait(Class<? extends FluidTrait> trait) {
        return hasTrait(trait, 0.0001);
    }

    public boolean hasTrait(Class<? extends FluidTrait> trait, double abovePressure) {
        for (FluidEntry entry : fluids)
            if (entry.pressure >= abovePressure && NTMFluidProperties.hasTrait(entry.fluid, trait))
                return true;
        return false;
    }

    /** The gas with the highest pressure, or no fluid in an emptied atmosphere. */
    public Fluid getMainFluid() {
        // NTM:Space read the first entry of an empty list here and crashed
        if (fluids.isEmpty()) return Fluids.EMPTY;
        sortDescending();
        return fluids.get(0).fluid;
    }

    public void sortDescending() {
        fluids.sort((a, b) -> Double.compare(b.pressure, a.pressure));
    }

    /** The total pressure: the entries hold partial pressures. */
    public double getPressure() {
        double pressure = 0;
        for (FluidEntry entry : fluids) pressure += entry.pressure;
        return pressure;
    }

    public double getPressure(Fluid fluid) {
        for (FluidEntry entry : fluids) if (entry.fluid == fluid) return entry.pressure;
        return 0;
    }

    public List<Integer> getFluidColors() {
        List<Integer> colors = new ArrayList<>();
        for (FluidEntry entry : fluids) {
            NTMFluidProperty property = NTMFluidProperties.get(entry.fluid);
            colors.add(property != null ? property.colorARGB() & 0xFFFFFF : 0xFFFFFF);
        }
        return colors;
    }

    // gases are saved by registry name (NTM:Space saved numeric ids, which shifted, its issue 424);
    // one no longer registered is dropped
    @Override
    public void writeToNBT(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (FluidEntry entry : fluids) {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", SpaceFluidIO.name(entry.fluid));
            tag.putDouble("percentage", entry.pressure);
            list.add(tag);
        }
        nbt.put("fluids", list);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        fluids = new ArrayList<>();
        ListTag list = nbt.getList("fluids", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Fluid fluid = SpaceFluidIO.byName(tag.getString("type"));
            if (fluid != null) fluids.add(new FluidEntry(fluid, tag.getDouble("percentage")));
        }
    }

    // client sync; the float pressure is a deliberate precision loss
    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(fluids.size());
        for (FluidEntry entry : fluids) {
            SpaceFluidIO.write(buf, entry.fluid);
            buf.writeFloat((float) entry.pressure);
        }
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        fluids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Fluid fluid = SpaceFluidIO.read(buf);
            float pressure = buf.readFloat();
            if (fluid != null) fluids.add(new FluidEntry(fluid, pressure));
        }
    }
}
