// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)
// Generated from NTM:Space's ModItems by the port's rocketparts.py; edit the table there.

package com.hbm.space.rocket;

import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.FuelType;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.items.weapon.ItemCustomMissilePart.WarheadType;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** NTM:Space's rocket parts, and the mass, tank, thrust and ISP every part gets for spaceflight. */
public final class SpaceRocketParts {

    private SpaceRocketParts() {}

    public static final WarheadType APOLLO = new WarheadType("APOLLO", () -> Component.translatable("item.hbm.warhead.desc.apollo").withStyle(System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.GOLD : ChatFormatting.RED));
    public static final WarheadType SATELLITE = new WarheadType("SATELLITE", () -> Component.translatable("item.hbm.warhead.desc.satellite").withStyle(System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.GOLD : ChatFormatting.RED));
    /** Space-grade fuselages hold whatever their thruster burns. */
    public static final FuelType ANY = new FuelType("ANY", "item.hbm.missile.fuel.any", ChatFormatting.GRAY, null);
    public static final FuelType HYDRAZINE = new FuelType("HYDRAZINE", "item.hbm.missile.fuel.hydrazine", ChatFormatting.AQUA, com.hbm.client.ClientEffects.Contrail.KEROSENE);
    public static final FuelType METHALOX = new FuelType("METHALOX", "item.hbm.missile.fuel.methalox", ChatFormatting.YELLOW, com.hbm.client.ClientEffects.Contrail.KEROSENE);
    public static final FuelType KEROLOX = new FuelType("KEROLOX", "item.hbm.missile.fuel.kerolox", ChatFormatting.LIGHT_PURPLE, null);
    public static final PartSize SIZE_25 = PartSize.register("SIZE_25", () -> Component.literal("2.5m"));
    public static final PartSize SIZE_30 = PartSize.register("SIZE_30", () -> Component.literal("3.0m"));

    /** A part's spaceflight numbers: kg dry, mB of propellant, newtons, seconds. */
    public record Stats(int mass, int tank, int thrust, int isp) {}

    private static final Map<ResourceLocation, Stats> STATS = new LinkedHashMap<>();

    public static Stats stats(ItemCustomMissilePart part) {
        Stats s = STATS.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(part));
        return s != null ? s : new Stats(0, (int) part.fuelAmount(), 0, 0);
    }

    private static RegistryHandle<ItemCustomMissilePart> part(String name, java.util.function.UnaryOperator<ItemCustomMissilePart> build,
            String mesh, String skin, float height, float guiHeight) {
        int stack = name.startsWith("rp_legs") || name.startsWith("rp_capsule") || name.startsWith("rp_station") || name.startsWith("rp_pod") ? 1 : 64;
        return Reg.item(name, props -> build.apply(new ItemCustomMissilePart(props)).setAssembly(height, guiHeight,
                Library.id("models/missile_parts/" + mesh + ".obj"), Library.id("textures/models/missile_parts/" + skin + ".png")),
                () -> new Item.Properties().stacksTo(stack));
    }

    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_METHALOX =
            part("mp_thruster_20_methalox", p -> p.makeThruster(METHALOX, 1F, 100F, PartSize.SIZE_20).setHealth(30F), "mp_t_20_kerosene", "thrusters/mp_t_20_methalox", 2F, 2F);
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_METHALOX_DUAL =
            part("mp_thruster_20_methalox_dual", p -> p.makeThruster(METHALOX, 1F, 100F, PartSize.SIZE_20).setHealth(30F), "mp_t_20_kerosene_dual", "thrusters/mp_t_20_methalox_dual", 2F, 2F);
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_METHALOX_TRIPLE =
            part("mp_thruster_20_methalox_triple", p -> p.makeThruster(METHALOX, 1F, 100F, PartSize.SIZE_20).setHealth(30F), "mp_t_20_kerosene_triple", "thrusters/mp_t_20_methalox_dual", 2F, 2F);
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_HYDROGEN =
            part("mp_thruster_20_hydrogen", p -> p.makeThruster(FuelType.HYDROGEN, 1F, 100F, PartSize.SIZE_20).setHealth(30F), "mp_t_20_kerosene", "thrusters/mp_t_20_hydrogen", 2F, 2F);
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_HYDROGEN_DUAL =
            part("mp_thruster_20_hydrogen_dual", p -> p.makeThruster(FuelType.HYDROGEN, 1F, 100F, PartSize.SIZE_20).setHealth(30F), "mp_t_20_kerosene_dual", "thrusters/mp_t_20_hydrogen_dual", 2F, 2F);
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_HYDROGEN_TRIPLE =
            part("mp_thruster_20_hydrogen_triple", p -> p.makeThruster(FuelType.HYDROGEN, 1F, 100F, PartSize.SIZE_20).setHealth(30F), "mp_t_20_kerosene_triple", "thrusters/mp_t_20_hydrogen_dual", 2F, 2F);
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_20_HYDRAZINE =
            part("mp_thruster_20_hydrazine", p -> p.makeThruster(HYDRAZINE, 1F, 100F, PartSize.SIZE_20).setHealth(35F), "mp_t_20_hydrazine", "thrusters/mp_t_20_hydrazine", 3F, 2.5F);
    public static final RegistryHandle<ItemCustomMissilePart> MP_THRUSTER_10_HYDRAZINE =
            part("mp_thruster_10_hydrazine", p -> p.makeThruster(HYDRAZINE, 1F, 1.5F, PartSize.SIZE_10).setHealth(15F), "mp_t_10_solid", "thrusters/mp_t_10_solid", 0.5F, 1F);
    public static final RegistryHandle<ItemCustomMissilePart> MP_FUSELAGE_10_HYDRAZINE =
            part("mp_fuselage_10_hydrazine", p -> p.makeFuselage(HYDRAZINE, 2000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F), "mp_f_10_kerosene", "fuselages/mp_f_10_hydrazine", 4F, 3F);
    public static final RegistryHandle<ItemCustomMissilePart> RP_FUSELAGE_20_12_HYDRAZINE =
            part("rp_fuselage_20_12_hydrazine", p -> p.makeFuselage(HYDRAZINE, 12500F, PartSize.SIZE_20, PartSize.SIZE_20).setHealth(25F), "mp_f_20_neo", "fuselages/mp_f_20_hydrazine", 10F, 8F);
    public static final RegistryHandle<ItemCustomMissilePart> RP_FUSELAGE_20_12 =
            part("rp_fuselage_20_12", p -> p.makeFuselage(ANY, 64000F, PartSize.SIZE_20, PartSize.SIZE_20), "mp_f_20_usa", "fuselages/mp_f_20_kerosene_usa", 12F, 8F);
    public static final RegistryHandle<ItemCustomMissilePart> RP_FUSELAGE_20_6 =
            part("rp_fuselage_20_6", p -> p.makeFuselage(ANY, 32000F, PartSize.SIZE_20, PartSize.SIZE_20), "mp_f_20_6_usa", "fuselages/mp_f_20_kerosene_usa", 6F, 4.5F);
    public static final RegistryHandle<ItemCustomMissilePart> RP_FUSELAGE_20_3 =
            part("rp_fuselage_20_3", p -> p.makeFuselage(ANY, 16000F, PartSize.SIZE_20, PartSize.SIZE_20), "mp_f_20_3_usa", "fuselages/mp_f_20_kerosene", 3F, 2.5F);
    public static final RegistryHandle<ItemCustomMissilePart> RP_FUSELAGE_20_1 =
            part("rp_fuselage_20_1", p -> p.makeFuselage(ANY, 6000F, PartSize.SIZE_20, PartSize.SIZE_20), "mp_f_20_1_usa", "fuselages/mp_f_20_kerosene", 1F, 1.5F);
    public static final RegistryHandle<ItemCustomMissilePart> RP_LEGS_20 =
            part("rp_legs_20", p -> p.makeStability(0F, PartSize.SIZE_20), "rp_s_20_leggy", "stability/rp_s_20_leggy", 2.4F, 3F);
    public static final RegistryHandle<ItemCustomMissilePart> RP_CAPSULE_20 =
            part("rp_capsule_20", p -> p.makeWarhead(APOLLO, 15F, 8F, PartSize.SIZE_20), "rp_landing_capsule", "warheads/rp_landing_capsule", 3.5F, 2.25F);
    public static final RegistryHandle<ItemCustomMissilePart> RP_STATION_CORE_20 =
            part("rp_station_core_20", p -> p.makeWarhead(SATELLITE, 15F, 64F, PartSize.SIZE_20), "mp_w_fairing", "warheads/mp_w_fairing", 7F, 5F);
    /** The fairing a satellite payload flies under; never an item of its own in play. */
    public static final RegistryHandle<ItemCustomMissilePart> RP_SATELLITE_20 =
            part("rp_satellite_20", p -> p.makeWarhead(SATELLITE, 15F, 16F, PartSize.SIZE_20), "mp_w_fairing", "warheads/mp_w_fairing", 7F, 5F);
    public static final RegistryHandle<ItemCustomMissilePart> RP_POD_20 =
            part("rp_pod_20", p -> p.makeWarhead(APOLLO, 15F, 4F, PartSize.SIZE_20), "rp_drop_pod", "warheads/rp_drop_pod", 3.0F, 2.25F);

    static {
        STATS.put(Library.id("mp_thruster_10_kerosene"), new Stats(180, 0, 16000, 308));
        STATS.put(Library.id("mp_thruster_10_solid"), new Stats(400, 0, 60000, 195));
        STATS.put(Library.id("mp_thruster_10_xenon"), new Stats(200, 0, 2000, 4200));
        STATS.put(Library.id("mp_thruster_15_kerosene"), new Stats(900, 0, 120000, 308));
        STATS.put(Library.id("mp_thruster_15_kerosene_dual"), new Stats(1600, 0, 200000, 308));
        STATS.put(Library.id("mp_thruster_15_kerosene_triple"), new Stats(2200, 0, 280000, 308));
        STATS.put(Library.id("mp_thruster_15_solid"), new Stats(1000, 0, 220000, 195));
        STATS.put(Library.id("mp_thruster_15_solid_hexdecuple"), new Stats(1200, 0, 260000, 195));
        STATS.put(Library.id("mp_thruster_15_hydrogen"), new Stats(800, 0, 100000, 380));
        STATS.put(Library.id("mp_thruster_15_hydrogen_dual"), new Stats(1400, 0, 200000, 380));
        STATS.put(Library.id("mp_thruster_15_balefire_short"), new Stats(1600, 0, 800000, 666));
        STATS.put(Library.id("mp_thruster_15_balefire"), new Stats(2000, 0, 1000000, 666));
        STATS.put(Library.id("mp_thruster_15_balefire_large"), new Stats(2400, 0, 1200000, 666));
        STATS.put(Library.id("mp_thruster_15_balefire_large_rad"), new Stats(2400, 0, 1200000, 666));
        STATS.put(Library.id("mp_thruster_20_kerosene"), new Stats(2400, 0, 1536000, 308));
        STATS.put(Library.id("mp_thruster_20_kerosene_dual"), new Stats(3200, 0, 1934000, 308));
        STATS.put(Library.id("mp_thruster_20_kerosene_triple"), new Stats(4400, 0, 2542000, 308));
        STATS.put(Library.id("mp_thruster_20_methalox"), new Stats(2400, 0, 890000, 320));
        STATS.put(Library.id("mp_thruster_20_methalox_dual"), new Stats(3200, 0, 1184000, 320));
        STATS.put(Library.id("mp_thruster_20_methalox_triple"), new Stats(4400, 0, 1456000, 320));
        STATS.put(Library.id("mp_thruster_20_hydrogen"), new Stats(2600, 0, 480000, 380));
        STATS.put(Library.id("mp_thruster_20_hydrogen_dual"), new Stats(3400, 0, 640000, 380));
        STATS.put(Library.id("mp_thruster_20_hydrogen_triple"), new Stats(4500, 0, 938000, 380));
        STATS.put(Library.id("mp_thruster_20_solid"), new Stats(2400, 0, 1400000, 195));
        STATS.put(Library.id("mp_thruster_20_solid_multi"), new Stats(3200, 0, 1830000, 195));
        STATS.put(Library.id("mp_thruster_20_solid_multier"), new Stats(4000, 0, 2320000, 195));
        STATS.put(Library.id("mp_thruster_20_hydrazine"), new Stats(1200, 0, 600000, 220));
        STATS.put(Library.id("mp_thruster_10_hydrazine"), new Stats(120, 0, 14000, 220));
        STATS.put(Library.id("mp_fuselage_10_kerosene"), new Stats(1000, 2500, 0, 0));
        STATS.put(Library.id("mp_fuselage_10_solid"), new Stats(1000, 2500, 0, 0));
        STATS.put(Library.id("mp_fuselage_10_xenon"), new Stats(1000, 5000, 0, 0));
        STATS.put(Library.id("mp_fuselage_10_hydrazine"), new Stats(1000, 2000, 0, 0));
        STATS.put(Library.id("rp_fuselage_20_12_hydrazine"), new Stats(1000, 12500, 0, 0));
        STATS.put(Library.id("mp_fuselage_10_long_kerosene"), new Stats(1000, 5000, 0, 0));
        STATS.put(Library.id("mp_fuselage_10_long_solid"), new Stats(1000, 5000, 0, 0));
        STATS.put(Library.id("mp_fuselage_10_15_kerosene"), new Stats(1000, 10000, 0, 0));
        STATS.put(Library.id("mp_fuselage_10_15_solid"), new Stats(1000, 10000, 0, 0));
        STATS.put(Library.id("mp_fuselage_10_15_hydrogen"), new Stats(1000, 10000, 0, 0));
        STATS.put(Library.id("mp_fuselage_10_15_balefire"), new Stats(1000, 10000, 0, 0));
        STATS.put(Library.id("mp_fuselage_15_kerosene"), new Stats(1000, 15000, 0, 0));
        STATS.put(Library.id("mp_fuselage_15_solid"), new Stats(1000, 15000, 0, 0));
        STATS.put(Library.id("mp_fuselage_15_hydrogen"), new Stats(1000, 15000, 0, 0));
        STATS.put(Library.id("mp_fuselage_15_balefire"), new Stats(1000, 15000, 0, 0));
        STATS.put(Library.id("mp_fuselage_15_20_kerosene"), new Stats(1000, 20000, 0, 0));
        STATS.put(Library.id("mp_fuselage_15_20_solid"), new Stats(1000, 20000, 0, 0));
        STATS.put(Library.id("mp_warhead_10_he"), new Stats(1500, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_10_incendiary"), new Stats(1500, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_10_buster"), new Stats(1500, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_10_nuclear"), new Stats(1500, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_10_nuclear_large"), new Stats(2500, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_10_taint"), new Stats(1500, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_10_cloud"), new Stats(1500, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_15_he"), new Stats(2500, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_15_incendiary"), new Stats(2500, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_15_nuclear"), new Stats(5000, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_15_boxcar"), new Stats(7500, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_15_n2"), new Stats(5000, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_15_balefire"), new Stats(7500, 0, 0, 0));
        STATS.put(Library.id("mp_warhead_15_turbine"), new Stats(5000, 0, 0, 0));
        STATS.put(Library.id("rp_fuselage_20_12"), new Stats(4000, 64000, 0, 0));
        STATS.put(Library.id("rp_fuselage_20_6"), new Stats(2100, 32000, 0, 0));
        STATS.put(Library.id("rp_fuselage_20_3"), new Stats(1200, 16000, 0, 0));
        STATS.put(Library.id("rp_fuselage_20_1"), new Stats(500, 6000, 0, 0));
        STATS.put(Library.id("rp_capsule_20"), new Stats(8000, 0, 0, 0));
        STATS.put(Library.id("rp_station_core_20"), new Stats(64000, 0, 0, 0));
        STATS.put(Library.id("rp_pod_20"), new Stats(4000, 0, 0, 0));
        STATS.put(Library.id("rp_satellite_20"), new Stats(16000, 0, 0, 0));
    }

    /** Every new part, in the order NTM:Space listed them, for the creative tab. */
    public static final java.util.List<RegistryHandle<ItemCustomMissilePart>> ALL = java.util.List.of(
            MP_THRUSTER_20_METHALOX,
            MP_THRUSTER_20_METHALOX_DUAL,
            MP_THRUSTER_20_METHALOX_TRIPLE,
            MP_THRUSTER_20_HYDROGEN,
            MP_THRUSTER_20_HYDROGEN_DUAL,
            MP_THRUSTER_20_HYDROGEN_TRIPLE,
            MP_THRUSTER_20_HYDRAZINE,
            MP_THRUSTER_10_HYDRAZINE,
            MP_FUSELAGE_10_HYDRAZINE,
            RP_FUSELAGE_20_12_HYDRAZINE,
            RP_FUSELAGE_20_12,
            RP_FUSELAGE_20_6,
            RP_FUSELAGE_20_3,
            RP_FUSELAGE_20_1,
            RP_LEGS_20,
            RP_CAPSULE_20,
            RP_STATION_CORE_20,
            RP_POD_20);

    /** Puts the parts in the missile tab and adds the spaceflight numbers to every part's tooltip. */
    public static void init() {
        // NTM:Space's compact launcher fuels hydrazine missiles from its fuel tank alone
        com.hbm.blockentity.bomb.BlockEntityCompactLauncher.addFuel(new com.hbm.blockentity.bomb.BlockEntityCompactLauncher.AddonFuel(
                HYDRAZINE, () -> com.hbm.inventory.fluid.NTMFluids.byName("HYDRAZINE"), null));
        ResourceLocation anchor = Library.id("mp_thruster_20_kerosene");
        for (RegistryHandle<ItemCustomMissilePart> part : ALL.reversed())
            com.hbm.items.CreativeContents.insertAfter(com.hbm.items.CreativeContents.Tab.MISSILES, anchor,
                    () -> new net.minecraft.world.item.ItemStack(part.get()));
        ItemCustomMissilePart.DESCRIBE.install("hbm_space", (part, adder) -> {
            // upstream: the drop pod has no tooltip, and capsule/satellite payloads no strength
            if (part == RP_POD_20.get()) return;
            if (part.type == ItemCustomMissilePart.PartType.WARHEAD
                    && (part.warheadType() == APOLLO || part.warheadType() == SATELLITE)) {
                Component strength = ItemCustomMissilePart.stat("strength", Component.literal(String.valueOf(part.warheadStrength())));
                part.describe(line -> {
                    if (!line.equals(strength)) adder.accept(line);
                });
            } else {
                part.describe(adder);
            }
            if (part.type == null) return;
            Stats s = stats(part);
            switch (part.type) {
                case WARHEAD, FUSELAGE -> adder.accept(ItemCustomMissilePart.stat("mass", Component.literal(s.mass() + "kg")));
                case THRUSTER -> {
                    adder.accept(ItemCustomMissilePart.stat("mass", Component.literal(s.mass() + "kg")));
                    adder.accept(ItemCustomMissilePart.stat("thrust", Component.literal(s.thrust() + "N")));
                    adder.accept(ItemCustomMissilePart.stat("isp", Component.literal(s.isp() + "s")));
                }
                default -> {}
            }
        });
    }
}
