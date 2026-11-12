// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.rocket;

import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.FuelType;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * A rocket as built: a capsule (or station core) on top of up to five stages, each a stack of
 * fuselages over a cluster of thrusters, with landing legs. Parts are saved by item id, where
 * NTM:Space saved hash codes that changed between runs.
 */
public class RocketStruct {

    public static final int MAX_STAGES = 5;

    public @Nullable ItemCustomMissilePart capsule;
    public final List<RocketStage> stages = new ArrayList<>();
    public int satFreq = 0;
    public final List<Component> extraIssues = new ArrayList<>();

    public RocketStruct() {}

    /** A satellite riding under the fairing, by item id; empty for crewed rockets. */
    public String payload = "";

    public RocketStruct(ItemStack capsule) {
        this.capsule = ItemCustomMissilePart.part(capsule);
        if (com.hbm.saveddata.satellites.SatelliteType.fromStack(capsule) != null) {
            this.capsule = SpaceRocketParts.RP_SATELLITE_20.get();
            payload = BuiltInRegistries.ITEM.getKey(capsule.getItem()).toString();
            satFreq = com.hbm.items.ISatChip.getFreqS(capsule);
        }
    }

    public RocketStruct(@Nullable ItemCustomMissilePart capsule) {
        this.capsule = capsule;
    }

    public void addStage(ItemStack fuselage, ItemStack fins, ItemStack thruster) {
        addStage(ItemCustomMissilePart.part(fuselage), ItemCustomMissilePart.part(fins), ItemCustomMissilePart.part(thruster),
                fuselage.isEmpty() ? 1 : fuselage.getCount(), thruster.isEmpty() ? 1 : thruster.getCount());
    }

    public void addStage(@Nullable ItemCustomMissilePart fuselage, @Nullable ItemCustomMissilePart fins,
            @Nullable ItemCustomMissilePart thruster, int fuselageCount, int thrusterCount) {
        RocketStage stage = new RocketStage();
        stage.fuselage = fuselage;
        stage.fins = fins;
        stage.thruster = thruster;
        stage.fuselageCount = fuselageCount;
        stage.thrusterCount = thrusterCount;
        stages.add(0, stage);
    }

    /** The capsule as an item: the satellite it carries, or the capsule part. */
    public ItemStack capsuleStack() {
        if (!payload.isEmpty()) {
            ResourceLocation key = ResourceLocation.tryParse(payload);
            if (key != null && BuiltInRegistries.ITEM.containsKey(key)) {
                ItemStack sat = new ItemStack(BuiltInRegistries.ITEM.get(key));
                com.hbm.items.ISatChip.setFreqS(sat, satFreq);
                return sat;
            }
        }
        return capsule != null ? new ItemStack(capsule) : ItemStack.EMPTY;
    }

    public static boolean isCapsule(@Nullable ItemCustomMissilePart part) {
        return part != null && part.type == PartType.WARHEAD
                && (part.warheadType() == SpaceRocketParts.APOLLO || part.warheadType() == SpaceRocketParts.SATELLITE);
    }

    private static boolean fuelMatches(RocketStage stage) {
        return stage.fuselage.fuelType() == SpaceRocketParts.ANY || stage.fuselage.fuelType() == stage.thruster.fuelType();
    }

    public boolean validate() {
        if (!extraIssues.isEmpty() || !isCapsule(capsule) || stages.isEmpty()) return false;
        for (RocketStage stage : stages) {
            if (stage.fuselage == null || stage.fuselage.type != PartType.FUSELAGE) return false;
            if (stage.fins != null && stage.fins.type != PartType.FINS) return false;
            if (stage.thruster == null || stage.thruster.type != PartType.THRUSTER) return false;
            if (stage.thrusterCount > stage.fuselageCount || stage.fuselageCount % stage.thrusterCount != 0) return false;
            if (!fuelMatches(stage)) return false;
        }
        return true;
    }

    public void addIssue(Component issue) {
        extraIssues.add(issue);
    }

    private static Component line(ChatFormatting color, String text) {
        return Component.literal(text).withStyle(color);
    }

    /** What's wrong with the rocket, and the current stage's numbers, for the assembler and pad. */
    public List<Component> findIssues(int stageNum, @Nullable CelestialBody from, @Nullable CelestialBody to, boolean fromOrbit, boolean toOrbit) {
        List<Component> issues = new ArrayList<>();
        if (capsule == null && stages.isEmpty()) return issues;
        if (!isCapsule(capsule)) issues.add(line(ChatFormatting.RED, "Invalid Capsule/Satellite"));

        if (stageNum < stages.size()) {
            RocketStage stage = stages.get(stageNum);
            issues.add(Component.literal("Dry mass: " + getLaunchMass(stageNum) + "kg"));
            issues.add(Component.literal("Wet mass: " + getWetMass(stageNum) + "kg"));
            if (stage.thruster != null) {
                issues.add(Component.literal("Thrust: " + getThrust(stage) + "N"));
                issues.add(Component.literal("ISP: " + getISP(stage) + "s"));
            }
        }

        for (int i = 0; i < stages.size(); i++) {
            RocketStage stage = stages.get(i);
            if (stage.fuselage == null) issues.add(line(ChatFormatting.RED, "Stage " + (i + 1) + " missing fuselage"));
            if (stage.thruster == null) issues.add(line(ChatFormatting.RED, "Stage " + (i + 1) + " missing thruster"));
            if (stage.fuselage == null || stage.thruster == null) continue;
            if (stage.thrusterCount > stage.fuselageCount) issues.add(line(ChatFormatting.RED, "Stage " + (i + 1) + " too many thrusters"));
            if (stage.fuselageCount % stage.thrusterCount != 0) issues.add(line(ChatFormatting.RED, "Stage " + (i + 1) + " uneven thrusters"));
            if (!fuelMatches(stage)) issues.add(line(ChatFormatting.RED, "Stage " + (i + 1) + " fuel mismatch"));
            if (i > 0 && stage.fins == null) issues.add(line(ChatFormatting.YELLOW, "Stage " + (i + 1) + " lacks landing legs"));
        }

        if (from != null && to != null) {
            int required = getFuelRequired(stageNum, from, to, fromOrbit, toOrbit);
            int capacity = getFuelCapacity(stageNum);
            if (required == Integer.MAX_VALUE) issues.add(line(ChatFormatting.YELLOW, "Insufficient thrust"));
            else if (capacity < required) issues.add(line(ChatFormatting.YELLOW, "Insufficient fuel: " + capacity + "/" + required + "mB"));
            else if (capacity > 0 && required > 0) issues.add(line(ChatFormatting.GREEN, "Trip possible! " + capacity + "/" + required + "mB"));
        }

        issues.addAll(extraIssues);
        return issues;
    }

    /** The propellant a thruster burns; {@link NTMFluids#NONE} for solid fuel, null if none. */
    public static @Nullable Fluid fuel(ItemCustomMissilePart thruster) {
        FuelType type = thruster.fuelType();
        if (type == FuelType.KEROSENE || type == SpaceRocketParts.KEROLOX) return NTMFluids.KEROSENE;
        if (type == SpaceRocketParts.METHALOX) return NTMFluids.GAS;
        if (type == FuelType.HYDROGEN) return NTMFluids.HYDROGEN;
        if (type == FuelType.XENON) return NTMFluids.XENON;
        if (type == FuelType.BALEFIRE) return NTMFluids.byName("BALEFIRE");
        if (type == SpaceRocketParts.HYDRAZINE) return NTMFluids.byName("HYDRAZINE");
        if (type == FuelType.SOLID) return NTMFluids.NONE;
        return null;
    }

    public static @Nullable Fluid oxidizer(ItemCustomMissilePart thruster) {
        FuelType type = thruster.fuelType();
        if (type == SpaceRocketParts.KEROLOX || type == FuelType.HYDROGEN || type == SpaceRocketParts.METHALOX) return NTMFluids.OXYGEN;
        if (type == FuelType.KEROSENE || type == FuelType.BALEFIRE) return NTMFluids.PEROXIDE;
        return null;
    }

    private static int tank(ItemCustomMissilePart part) {
        return SpaceRocketParts.stats(part).tank();
    }

    private static int mass(ItemCustomMissilePart part) {
        return SpaceRocketParts.stats(part).mass();
    }

    /** What every stage's tanks hold, by fluid; solid fuel is {@link NTMFluids#NONE}. */
    public Map<Fluid, Integer> getFillRequirement() {
        Map<Fluid, Integer> tanks = new LinkedHashMap<>();
        for (RocketStage stage : stages) {
            if (stage.thruster == null || stage.fuselage == null) continue;
            int amount = tank(stage.fuselage) * stage.fuselageCount;
            Fluid fuel = fuel(stage.thruster), oxidizer = oxidizer(stage.thruster);
            if (fuel != null) tanks.merge(fuel, amount, Integer::sum);
            if (oxidizer != null) tanks.merge(oxidizer, amount, Integer::sum);
        }
        return tanks;
    }

    public boolean hasSufficientFuel(CelestialBody from, CelestialBody to, boolean fromOrbit, boolean toOrbit) {
        // pods transfer, fall to the body they orbit and return to a station, but can't hop the surface
        if (capsule == SpaceRocketParts.RP_POD_20.get()) return from == to && (fromOrbit || toOrbit);
        // a bare capsule only returns to the body it orbits
        if (stages.isEmpty()) return from == to && fromOrbit && !toOrbit;
        return getFuelCapacity(0) >= getFuelRequired(0, from, to, fromOrbit, toOrbit);
    }

    private int getFuelCapacity(int stageNum) {
        if (stageNum >= stages.size()) return -1;
        RocketStage stage = stages.get(stageNum);
        return stage.fuselage == null ? -1 : tank(stage.fuselage) * stage.fuselageCount;
    }

    private int getFuelRequired(int stageNum, CelestialBody from, CelestialBody to, boolean fromOrbit, boolean toOrbit) {
        if (stageNum >= stages.size()) return -1;
        RocketStage stage = stages.get(stageNum);
        if (stage.fuselage == null || stage.thruster == null) return -1;
        return SolarSystem.getCostBetween(from, to, getLaunchMass(stageNum), getThrust(stage), getISP(stage), fromOrbit, toOrbit);
    }

    public int getThrust() {
        return getThrust(stages.get(0));
    }

    private static int getThrust(RocketStage stage) {
        return SpaceRocketParts.stats(stage.thruster).thrust() * stage.thrusterCount;
    }

    private static int getISP(RocketStage stage) {
        return SpaceRocketParts.stats(stage.thruster).isp();
    }

    /** Dry mass of the active stage plus the wet mass of the stages above it. */
    public int getLaunchMass() {
        return getMass(0, false);
    }

    public int getLaunchMass(int stageNum) {
        return getMass(stageNum, false);
    }

    public int getWetMass(int stageNum) {
        return getMass(stageNum, true);
    }

    private int getMass(int stageNum, boolean wet) {
        int total = !payload.isEmpty() ? com.hbm.space.satellite.SpaceSatellites.mass(payload) : capsule != null ? mass(capsule) : 0;
        for (int i = stageNum; i < stages.size(); i++) {
            RocketStage stage = stages.get(i);
            if (stage.fuselage != null) total += mass(stage.fuselage) * stage.fuselageCount;
            if (stage.thruster != null) total += mass(stage.thruster) * stage.thrusterCount;
            // fuel weighs a quarter, to make multi-stage rockets worth building
            if (stage.fuselage != null && (i > stageNum || wet)) total += tank(stage.fuselage) * stage.fuselageCount / 4;
        }
        return Mth.ceil(total);
    }

    public double getHeight() {
        double height = capsule != null ? capsule.height : 0;
        boolean deployed = true;
        for (RocketStage stage : stages) {
            if (stage.fuselage != null) height += stage.fuselage.height * stage.getStack();
            height += Math.max(stage.thruster != null ? stage.thruster.height : 0, deployed && stage.fins != null ? stage.fins.height : 0);
            deployed = false;
        }
        return height;
    }

    public double getHeight(int stageNum) {
        double height = 0;
        if (!stages.isEmpty()) {
            RocketStage stage = stages.get(Math.min(stageNum, stages.size() - 1));
            if (stage.fuselage != null) height += stage.fuselage.height * stage.getStack();
            height += Math.max(stage.thruster != null ? stage.thruster.height : 0, stageNum == 0 && stage.fins != null ? stage.fins.height : 0);
        }
        if ((stages.isEmpty() || stageNum == stages.size() - 1) && capsule != null) height += capsule.height;
        return height;
    }

    public double getOffset(int stageNum) {
        double height = 0;
        for (int i = 0; i < Math.min(stageNum, stages.size() - 1); i++) {
            RocketStage stage = stages.get(i);
            if (stage.fuselage != null) height += stage.fuselage.height * stage.getStack();
            height += Math.max(stage.thruster != null ? stage.thruster.height : 0, i == 0 && stage.fins != null ? stage.fins.height : 0);
        }
        return height;
    }

    private static String id(@Nullable ItemCustomMissilePart part) {
        return part == null ? "" : BuiltInRegistries.ITEM.getKey(part).toString();
    }

    private static @Nullable ItemCustomMissilePart part(String id) {
        if (id.isEmpty()) return null;
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && BuiltInRegistries.ITEM.get(key) instanceof ItemCustomMissilePart p ? p : null;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(id(capsule));
        buf.writeVarInt(stages.size());
        for (RocketStage stage : stages) {
            buf.writeUtf(id(stage.fuselage));
            buf.writeUtf(id(stage.fins));
            buf.writeUtf(id(stage.thruster));
            buf.writeByte(stage.fuselageCount);
            buf.writeByte(stage.thrusterCount);
        }
        buf.writeVarInt(satFreq);
        buf.writeUtf(payload);
        buf.writeVarInt(extraIssues.size());
        for (Component issue : extraIssues) buf.writeUtf(issue.getString());
    }

    public static RocketStruct read(FriendlyByteBuf buf) {
        RocketStruct rocket = new RocketStruct(part(buf.readUtf()));
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            RocketStage stage = new RocketStage();
            stage.fuselage = part(buf.readUtf());
            stage.fins = part(buf.readUtf());
            stage.thruster = part(buf.readUtf());
            stage.fuselageCount = Math.max(buf.readByte(), 1);
            stage.thrusterCount = Math.max(buf.readByte(), 1);
            rocket.stages.add(stage);
        }
        rocket.satFreq = buf.readVarInt();
        rocket.payload = buf.readUtf();
        count = buf.readVarInt();
        for (int i = 0; i < count; i++) rocket.extraIssues.add(Component.literal(buf.readUtf()));
        return rocket;
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("capsule", id(capsule));
        ListTag list = new ListTag();
        for (RocketStage stage : stages) {
            CompoundTag tag = new CompoundTag();
            tag.putString("fuselage", id(stage.fuselage));
            tag.putString("fins", id(stage.fins));
            tag.putString("thruster", id(stage.thruster));
            tag.putInt("fc", stage.fuselageCount);
            tag.putInt("tc", stage.thrusterCount);
            list.add(tag);
        }
        nbt.put("stages", list);
        nbt.putInt("freq", satFreq);
        if (!payload.isEmpty()) nbt.putString("payload", payload);
        return nbt;
    }

    public static RocketStruct load(CompoundTag nbt) {
        RocketStruct rocket = new RocketStruct(part(nbt.getString("capsule")));
        ListTag list = nbt.getList("stages", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            RocketStage stage = new RocketStage();
            stage.fuselage = part(tag.getString("fuselage"));
            stage.fins = part(tag.getString("fins"));
            stage.thruster = part(tag.getString("thruster"));
            stage.fuselageCount = Math.max(tag.getInt("fc"), 1);
            stage.thrusterCount = Math.max(tag.getInt("tc"), 1);
            rocket.stages.add(stage);
        }
        rocket.satFreq = nbt.getInt("freq");
        rocket.payload = nbt.getString("payload");
        // in case of emergency, a capsule
        if (rocket.capsule == null) rocket.capsule = SpaceRocketParts.RP_CAPSULE_20.get();
        return rocket;
    }

    public static class RocketStage {
        public @Nullable ItemCustomMissilePart fuselage;
        public @Nullable ItemCustomMissilePart fins;
        public @Nullable ItemCustomMissilePart thruster;
        public int fuselageCount = 1;
        public int thrusterCount = 1;

        /** Fuselages stacked above each thruster. */
        public int getStack() {
            return Math.max(fuselageCount / thrusterCount, 1);
        }

        /** Side-by-side columns. */
        public int getCluster() {
            return Math.max(fuselageCount / getStack(), 1);
        }
    }
}
