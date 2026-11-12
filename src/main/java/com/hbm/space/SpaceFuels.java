// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.NuclearTech;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.ItemRBMKRod.DepletionFunction;
import com.hbm.items.machine.ItemRBMKRod.BurnFunction;
import com.hbm.items.machine.ItemRBMKRod.EnumBurnFunc;
import com.hbm.interfaces.HalfLifeType;
import com.hbm.items.CreativeContents;
import com.hbm.items.armor.ItemModInsert;
import com.hbm.items.food.ItemDrinkEnergy;
import com.hbm.items.machine.DepletedRTGMaterial;
import com.hbm.items.machine.ItemBattery;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.RTGUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** NTM:Space's RBMK fuels: berkelium-247 and the curium-245 rods, whose californium buildup peaks mid-burn. */
public final class SpaceFuels {

    private SpaceFuels() {}

    /** Space's SLOW_LINEAR flux function. */
    static final BurnFunction SLOW_LINEAR = new BurnFunction() {
        @Override
        public String title() {
            return "trait.rbmx.flux.slowlinear";
        }

        @Override
        public double reactivity(ItemRBMKRod rod, double flux, double enrichment) {
            return Math.sqrt(2 * flux + 30) / 10 * rod.reactivity / 2.5;
        }

        @Override
        public String formula(ItemRBMKRod rod) {
            return "sqrt(2 * %1$s + 30) / 10 * %2$s / 2.5";
        }
    };

    /** Space's CF_SLOPE: caps at about 193 percent around 60 percent depletion. */
    static final DepletionFunction CF_SLOPE = e -> e + Math.sin(e * Math.PI) * 1.4;

    public static final Reg.Handle<ItemRBMKRod> ROD_BK247 = ModItems.rbmkFuel(Reg.in(NuclearTech.MOD_ID), "rbmk_fuel_bk247", "Highly Enriched Berkelium-247", rod -> rod.setYield(100_000_000D).setStats(50).setFunction(EnumBurnFunc.LINEAR).setHeat(2D).setMeltingPoint(2993).setTint(0xC8A57E));
    public static final Reg.Handle<ItemRBMKPellet> PELLET_BK247 = ModItems.rbmkPellet(Reg.in(NuclearTech.MOD_ID), ROD_BK247, "rbmk_pellet_bk247", "Highly Enriched Berkelium-247", false);
    public static final Reg.Handle<ItemRBMKRod> ROD_LECM = ModItems.rbmkFuel(Reg.in(NuclearTech.MOD_ID), "rbmk_fuel_lecm", "Low Enriched Curium-245", rod -> rod.setYield(30_000_000D).setStats(20, 5).setFunction(SLOW_LINEAR).setDepletionFunction(CF_SLOPE).setHeat(1.25D).setMeltingPoint(1340).setDiffusion(0.4D).setTint(0x9C8B8E));
    public static final Reg.Handle<ItemRBMKPellet> PELLET_LECM = ModItems.rbmkPellet(Reg.in(NuclearTech.MOD_ID), ROD_LECM, "rbmk_pellet_lecm", "Low Enriched Curium-245", false);
    public static final Reg.Handle<ItemRBMKRod> ROD_MECM = ModItems.rbmkFuel(Reg.in(NuclearTech.MOD_ID), "rbmk_fuel_mecm", "Medium Enriched Curium-245", rod -> rod.setYield(27_000_000D).setStats(30, 10).setFunction(SLOW_LINEAR).setDepletionFunction(CF_SLOPE).setHeat(1.25D).setMeltingPoint(1720).setDiffusion(0.3D).setTint(0x9C8B8E));
    public static final Reg.Handle<ItemRBMKPellet> PELLET_MECM = ModItems.rbmkPellet(Reg.in(NuclearTech.MOD_ID), ROD_MECM, "rbmk_pellet_mecm", "Medium Enriched Curium-245", false);
    public static final Reg.Handle<ItemRBMKRod> ROD_HECM = ModItems.rbmkFuel(Reg.in(NuclearTech.MOD_ID), "rbmk_fuel_hecm", "Highly Enriched Curium-245", rod -> rod.setYield(24_000_000D).setStats(60, 25).setFunction(SLOW_LINEAR).setDepletionFunction(CF_SLOPE).setHeat(1.3D).setMeltingPoint(1880).setDiffusion(0.3D).setTint(0x9C8B8E));
    public static final Reg.Handle<ItemRBMKPellet> PELLET_HECM = ModItems.rbmkPellet(Reg.in(NuclearTech.MOD_ID), ROD_HECM, "rbmk_pellet_hecm", "Highly Enriched Curium-245", false);

    public static final Reg.Handle<ItemRTGPellet> RTG_CF251 = rtg("pellet_rtg_cf251");
    public static final Reg.Handle<ItemRTGPellet> RTG_CF252 = rtg("pellet_rtg_cf252");

    public static final Reg.Handle<ItemModInsert> INSERT_CMB = Reg.item("insert_cmb",
            props -> new ItemModInsert(props, 9999, 0.7F, 0.9F, 0.4F, 1F).withCorrosionProtection(), Item.Properties::new);
    public static final Reg.Handle<com.hbm.space.item.ItemButterSword> BUTTER_SWORD = Reg.item("butter_sword",
            com.hbm.space.item.ItemButterSword::new, Item.Properties::new);
    public static final Reg.Handle<ItemBattery> CORE_ANGEL = Reg.item("core_angel",
            props -> new ItemBattery(props, 16_000_000L, 16_000_000L, 800_000L), () -> new Item.Properties().stacksTo(1));
    public static final Reg.Handle<ItemDrinkEnergy> TEACUP = drink("teacup");
    public static final Reg.Handle<ItemDrinkEnergy> BOTTLE_HONEY = drink("bottle_honey");
    public static final Reg.Handle<ItemDrinkEnergy> GLASS_SMILK = drink("glass_smilk");

    public static final RegistryHandle<net.minecraft.sounds.SoundEvent> SOUND_GS = com.hbm.registration.NeoForgeRegistrar.registerSound("music.record_gs");
    public static final RegistryHandle<net.minecraft.sounds.SoundEvent> SOUND_GP = com.hbm.registration.NeoForgeRegistrar.registerSound("music.record_gp");
    public static final RegistryHandle<net.minecraft.sounds.SoundEvent> SOUND_EL = com.hbm.registration.NeoForgeRegistrar.registerSound("music.record_el");
    public static final Reg.Handle<Item> RECORD_GS = record("gs");
    public static final Reg.Handle<Item> RECORD_GP = record("gp");
    public static final Reg.Handle<Item> RECORD_EL = record("el");
    public static final Reg.Handle<com.hbm.items.machine.ItemFELCrystal> LASER_CRYSTAL_IRON = Reg.item("laser_crystal_iron",
            props -> new com.hbm.items.machine.ItemFELCrystal(props, com.hbm.items.machine.EnumWavelengths.XRAY), () -> new Item.Properties().stacksTo(1));

    private static Reg.Handle<Item> record(String song) {
        return Reg.item("record_" + song, Item::new, () -> new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)
                .jukeboxPlayable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG, Library.id(song))));
    }

    private static Reg.Handle<ItemDrinkEnergy> drink(String name) {
        return Reg.item(name, props -> new ItemDrinkEnergy(props, null, false, player -> {}), Item.Properties::new);
    }

    private static Reg.Handle<ItemRTGPellet> rtg(String name) {
        long halflife = (long) (RTGUtil.getLifespan(1F, HalfLifeType.SHORT, false) * 2.5);
        return Reg.item(name, props -> new ItemRTGPellet(props, 200).setDecays(DepletedRTGMaterial.LEAD, halflife, 1.5D),
                () -> new Item.Properties().stacksTo(1));
    }

    private static void after(CreativeContents.Tab tab, String anchor, RegistryHandle<? extends Item>... items) {
        for (RegistryHandle<? extends Item> item : items) {
            CreativeContents.insertAfter(tab, Library.id(anchor), () -> new ItemStack(item.get()));
            anchor = item.id().getPath();
        }
    }

    @SuppressWarnings("unchecked")
    public static void init() {
        after(CreativeContents.Tab.CONTROL, "rbmk_fuel_hea242", ROD_BK247, ROD_LECM, ROD_MECM, ROD_HECM);
        after(CreativeContents.Tab.CONTROL, "rbmk_pellet_hea242", PELLET_BK247, PELLET_LECM, PELLET_MECM, PELLET_HECM);
        after(CreativeContents.Tab.CONTROL, "pellet_rtg_americium", RTG_CF251, RTG_CF252);
        after(CreativeContents.Tab.CONTROL, "battery_creative", CORE_ANGEL);
        after(CreativeContents.Tab.CONSUMABLES, "chocolate_milk", TEACUP, BOTTLE_HONEY, GLASS_SMILK, INSERT_CMB);
        after(CreativeContents.Tab.CONSUMABLES, "insert_cmb", RECORD_GS, RECORD_GP, RECORD_EL);
        after(CreativeContents.Tab.CONTROL, "laser_crystal_bismuth", LASER_CRYSTAL_IRON);
        // NTM:Space put it in vanilla's Combat tab
        com.hbm.registration.NeoForgeRegistrar.vanillaTabEntries.add(java.util.Map.entry(
                net.minecraft.world.item.CreativeModeTabs.COMBAT, () -> new ItemStack(BUTTER_SWORD.get())));
    }
}
