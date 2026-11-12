// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.items.CreativeContents;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** NTM:Space's plain crafting items: Space-only metals, billets, powders, crystals and odds and ends. */
public final class SpaceParts {

    private SpaceParts() {}

    public static final List<RegistryHandle<? extends Item>> ITEMS = new ArrayList<>();

    /** Space's stainless steel, so the foundry melts its ingots and casts its plates (upstream MAT_STAINLESS). */
    public static final com.hbm.inventory.material.NTMMaterial MAT_STAINLESS =
            com.hbm.inventory.material.Mats.makeSmeltable(24_003, com.hbm.inventory.material.Mats.df("stainless"), 0xD8D8D8, 0x474747, 0x4A4A4A).m();

    /** Space's nickel, gallium and gallium arsenide, so the crucible melts them and alloys stainless steel and GaAs. */
    public static final com.hbm.inventory.material.NTMMaterial MAT_NICKEL =
            com.hbm.inventory.material.Mats.makeSmeltable(24_004, com.hbm.inventory.material.Mats.df("nickel"), 0xE8D1C7, 0x87756E, 0xAE9572).m();
    public static final com.hbm.inventory.material.NTMMaterial MAT_GALLIUM =
            com.hbm.inventory.material.Mats.makeSmeltable(24_005, com.hbm.inventory.material.Mats.df("gallium"), 0x52687F, 0x52687F, 0x52687F).m();
    public static final com.hbm.inventory.material.NTMMaterial MAT_GAAS =
            com.hbm.inventory.material.Mats.makeSmeltable(24_006, com.hbm.inventory.material.Mats.df("gaas"), 0x6F4A57, 0x6F4A57, 0x6F4A57).m();

    private static void add(String name, Function<Item.Properties, ? extends Item> factory, Supplier<Item.Properties> props) {
        ITEMS.add(Reg.item(name, factory, props));
    }

    static {
        add("ammonium_nitrate", Item::new, Item.Properties::new);
        add("ball_ferric_clay", Item::new, Item.Properties::new);
        add("bean_roast", Item::new, Item.Properties::new);
        add("beryllium_mirror", Item::new, Item.Properties::new);
        add("billet_bk247", Item::new, Item.Properties::new);
        add("billet_cf251", Item::new, Item.Properties::new);
        add("billet_cf252", Item::new, Item.Properties::new);
        add("billet_cm242", Item::new, Item.Properties::new);
        add("billet_cm243", Item::new, Item.Properties::new);
        add("billet_cm244", Item::new, Item.Properties::new);
        add("billet_cm245", Item::new, Item.Properties::new);
        add("billet_cm246", Item::new, Item.Properties::new);
        add("billet_cm247", Item::new, Item.Properties::new);
        add("billet_cm_fuel", Item::new, Item.Properties::new);
        add("billet_cm_mix", Item::new, Item.Properties::new);
        add("billet_cn989", ItemCustomLore::new, Item.Properties::new);
        add("billet_es253", Item::new, Item.Properties::new);
        add("billet_gaas", Item::new, Item.Properties::new);
        add("billet_menthol", Item::new, Item.Properties::new);
        add("billet_red_copper", Item::new, Item.Properties::new);
        add("blade_syngas", Item::new, Item.Properties::new);
        add("butter", Item::new, Item.Properties::new);
        add("cmug_empty", Item::new, Item.Properties::new);
        add("coin_airliner", ItemCustomLore::new, Item.Properties::new);
        add("circuit_aero", Item::new, Item.Properties::new);
        add("circuit_avionics", Item::new, Item.Properties::new);
        add("circuit_molychip", Item::new, Item.Properties::new);
        add("item_expensive_stainless_beam", com.hbm.items.ItemExpensive::new, Item.Properties::new);
        add("item_expensive_avionics", com.hbm.items.ItemExpensive::new, Item.Properties::new);
        add("item_expensive_astro", com.hbm.items.ItemExpensive::new, Item.Properties::new);
        add("crystal_cleaned", ItemCustomLore::new, Item.Properties::new);
        add("crystal_mineral", ItemCustomLore::new, Item.Properties::new);
        add("crystal_nickel", Item::new, Item.Properties::new);
        add("crystal_niobium", Item::new, Item.Properties::new);
        add("crystal_zinc", Item::new, Item.Properties::new);
        add("divine_shard", Item::new, Item.Properties::new);
        add("flour", Item::new, Item.Properties::new);
        add("glass_empty", Item::new, Item.Properties::new);
        add("ingot_australium_greater", ItemCustomLore::new, Item.Properties::new);
        add("ingot_australium_lesser", ItemCustomLore::new, Item.Properties::new);
        add("ingot_bk247", Item::new, Item.Properties::new);
        add("ingot_cf251", Item::new, Item.Properties::new);
        add("ingot_cf252", Item::new, Item.Properties::new);
        add("ingot_cm242", Item::new, Item.Properties::new);
        add("ingot_cm243", Item::new, Item.Properties::new);
        add("ingot_cm244", Item::new, Item.Properties::new);
        add("ingot_cm245", Item::new, Item.Properties::new);
        add("ingot_cm246", Item::new, Item.Properties::new);
        add("ingot_cm247", Item::new, Item.Properties::new);
        add("ingot_cm_fuel", Item::new, Item.Properties::new);
        add("ingot_cm_mix", Item::new, Item.Properties::new);
        add("ingot_cn989", ItemCustomLore::new, Item.Properties::new);
        add("ingot_es253", Item::new, Item.Properties::new);
        add("ingot_es255", Item::new, Item.Properties::new);
        add("ingot_gaas", ItemCustomLore::new, Item.Properties::new);
        add("ingot_gallium", Item::new, Item.Properties::new);
        add("ingot_gwenium", ItemCustomLore::new, Item.Properties::new);
        add("ingot_iridium", ItemCustomLore::new, Item.Properties::new);
        add("ingot_magma", Item::new, Item.Properties::new);
        add("ingot_menthol", Item::new, Item.Properties::new);
        add("ingot_nickel", Item::new, Item.Properties::new);
        add("ingot_palladium", ItemCustomLore::new, Item.Properties::new);
        add("ingot_stainless", Item::new, Item.Properties::new);
        add("ingot_tt", ItemCustomLore::new, Item.Properties::new);
        add("ingot_ttas", ItemCustomLore::new, Item.Properties::new);
        add("ingot_zinc", Item::new, Item.Properties::new);
        add("mineral_dust", ItemCustomLore::new, Item.Properties::new);
        add("mineral_fragment_peroxide", ItemCustomLore::new, Item.Properties::new);
        add("mineral_fragment_nitric", ItemCustomLore::new, Item.Properties::new);
        add("mineral_fragment_sulfuric", ItemCustomLore::new, Item.Properties::new);
        add("mineral_fragment_solvent", ItemCustomLore::new, Item.Properties::new);
        add("mineral_fragment_hydrochloric", ItemCustomLore::new, Item.Properties::new);
        add("mineral_fragment_schrabidic", ItemCustomLore::new, Item.Properties::new);
        add("nickel_salts", Item::new, Item.Properties::new);
        add("nugget_bk247", Item::new, Item.Properties::new);
        add("nugget_cf251", Item::new, Item.Properties::new);
        add("nugget_cf252", Item::new, Item.Properties::new);
        add("nugget_cm242", Item::new, Item.Properties::new);
        add("nugget_cm243", Item::new, Item.Properties::new);
        add("nugget_cm244", Item::new, Item.Properties::new);
        add("nugget_cm245", Item::new, Item.Properties::new);
        add("nugget_cm246", Item::new, Item.Properties::new);
        add("nugget_cm247", Item::new, Item.Properties::new);
        add("nugget_cm_fuel", Item::new, Item.Properties::new);
        add("nugget_cm_mix", Item::new, Item.Properties::new);
        add("nugget_cn989", ItemCustomLore::new, Item.Properties::new);
        add("nugget_es253", Item::new, Item.Properties::new);
        add("nugget_gaas", Item::new, Item.Properties::new);
        add("nugget_gallium", Item::new, Item.Properties::new);
        add("nugget_hafnium", ItemCustomLore::new, Item.Properties::new);
        add("nugget_lanthanium", Item::new, Item.Properties::new);
        add("nugget_menthol", Item::new, Item.Properties::new);
        add("nugget_nickel", ItemCustomLore::new, Item.Properties::new);
        add("nugget_zinc", Item::new, Item.Properties::new);
        add("plate_cn989", ItemCustomLore::new, Item.Properties::new);
        add("plate_nickel", Item::new, Item.Properties::new);
        add("plate_stainless", Item::new, Item.Properties::new);
        add("plate_cast_stainless", Item::new, Item.Properties::new);
        add("plate_welded_stainless", Item::new, Item.Properties::new);
        add("powder_cn989", ItemCustomLore::new, Item.Properties::new);
        add("powder_coffee", ItemCustomLore::new, Item.Properties::new);
        add("powder_gallium", Item::new, Item.Properties::new);
        add("powder_gallium_tiny", Item::new, Item.Properties::new);
        add("powder_nickel", Item::new, Item.Properties::new);
        add("powder_pvc", ItemCustomLore::new, Item.Properties::new);
        add("powder_rubber", ItemCustomLore::new, Item.Properties::new);
        add("powder_wd2004", ItemCustomLore::new, Item.Properties::new);
        add("powder_wd2004_tiny", ItemCustomLore::new, Item.Properties::new);
        add("powder_zinc", Item::new, Item.Properties::new);
        add("rag_blood", Item::new, Item.Properties::new);
        add("scuttertail", Item::new, Item.Properties::new);
        add("stick_pvc", ItemCustomLore::new, Item.Properties::new);
        add("stick_vinyl", ItemCustomLore::new, Item.Properties::new);
        add("swarm_member", Item::new, Item.Properties::new);
        add("teacup_empty", Item::new, Item.Properties::new);
        add("turbine_syngas", Item::new, Item.Properties::new);
        add("woodemium_briquette", Item::new, Item.Properties::new);
        add("flesh_wafer", ItemCustomLore::new, () -> food(5, 0.6F));
        add("flesh_burger", ItemCustomLore::new, () -> food(7, 1.0F));
        add("chocolate_mint_billet", ItemCustomLore::new, () -> food(5, 5.0F));
        add("s_cream", ItemCustomLore::new, () -> food(8, 1.0F));
        add("min_cream", ItemCustomLore::new, () -> food(10, 1.0F));
        add("chunk_ore_pentlandite", Item::new, Item.Properties::new);
    }

    private static Item.Properties food(int nutrition, float saturation) {
        return new Item.Properties().food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build());
    }

    public static void init() {
        String anchor = "ingot_hafnium";
        for (RegistryHandle<? extends Item> item : ITEMS) {
            CreativeContents.insertAfter(CreativeContents.Tab.PARTS, Library.id(anchor), () -> new ItemStack(item.get()));
            anchor = item.id().getPath();
        }
    }
}
