// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.items.weapon.GunContent;
import com.hbm.items.weapon.legacy.ItemAmmoLegacy.AmmoItemTrait;
import com.hbm.items.weapon.legacy.factory.Gun357MagnumFactory;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's pre-rework guns and their ammo. Every item here sits
 * behind disable_pre_rework_guns (GunContent); the weapons tab lists them in main's order.
 */
public final class LegacyGuns {

    private static final List<RegistryHandle<? extends Item>> TAB = new ArrayList<>();

    public static RegistryHandle<ItemGunBase> GUN_REVOLVER;
    public static RegistryHandle<ItemGunBase> GUN_REVOLVER_SATURNITE;
    public static RegistryHandle<ItemGunBase> GUN_REVOLVER_GOLD;
    public static RegistryHandle<ItemGunBase> GUN_REVOLVER_SCHRABIDIUM;
    public static RegistryHandle<ItemGunBase> GUN_REVOLVER_CURSED;
    public static RegistryHandle<ItemGunBase> GUN_REVOLVER_NIGHTMARE;
    public static RegistryHandle<ItemGunBase> GUN_REVOLVER_NIGHTMARE2;
    public static RegistryHandle<ItemGunBio> GUN_BIO_REVOLVER;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_357_IRON;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_357_LEAD;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_357_NUCLEAR;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_357_GOLD;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_357_DESH;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_357_SCHRABIDIUM;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_357_STEEL;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_357_NIGHTMARE1;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_357_NIGHTMARE2;

    public static RegistryHandle<ItemClipLegacy> CLIP_REVOLVER_IRON;
    public static RegistryHandle<ItemClipLegacy> CLIP_REVOLVER;
    public static RegistryHandle<ItemClipLegacy> CLIP_REVOLVER_GOLD;
    public static RegistryHandle<ItemClipLegacy> CLIP_REVOLVER_LEAD;
    public static RegistryHandle<ItemClipLegacy> CLIP_REVOLVER_SCHRABIDIUM;
    public static RegistryHandle<ItemClipLegacy> CLIP_REVOLVER_CURSED;
    public static RegistryHandle<ItemClipLegacy> CLIP_REVOLVER_NIGHTMARE;
    public static RegistryHandle<ItemClipLegacy> CLIP_REVOLVER_NIGHTMARE2;

    public static RegistryHandle<Item> MECHANISM_REVOLVER_1;
    public static RegistryHandle<Item> MECHANISM_REVOLVER_2;
    public static RegistryHandle<Item> MECHANISM_SPECIAL;
    public static RegistryHandle<Item> CASING_357;
    public static RegistryHandle<Item> ASSEMBLY_IRON;
    public static RegistryHandle<Item> ASSEMBLY_STEEL;
    public static RegistryHandle<Item> ASSEMBLY_LEAD;
    public static RegistryHandle<Item> ASSEMBLY_GOLD;
    public static RegistryHandle<Item> ASSEMBLY_SCHRABIDIUM;
    public static RegistryHandle<Item> ASSEMBLY_NIGHTMARE;
    public static RegistryHandle<Item> ASSEMBLY_DESH;

    private LegacyGuns() {}

    /** Called from ModItems' registration, after the post-rework guns. */
    public static void init() {
        // main's parts tab entries the old guns and bullets are made from
        MECHANISM_REVOLVER_1 = part("mechanism_revolver_1");
        MECHANISM_REVOLVER_2 = part("mechanism_revolver_2");
        MECHANISM_SPECIAL = part("mechanism_special");
        CASING_357 = part("casing_357");
        ASSEMBLY_IRON = part("assembly_iron");
        ASSEMBLY_STEEL = part("assembly_steel");
        ASSEMBLY_LEAD = part("assembly_lead");
        ASSEMBLY_GOLD = part("assembly_gold");
        ASSEMBLY_SCHRABIDIUM = part("assembly_schrabidium");
        ASSEMBLY_NIGHTMARE = part("assembly_nightmare");
        ASSEMBLY_DESH = part("assembly_desh");

        // .357 Magnum
        GUN_REVOLVER = gun("gun_revolver", p -> new ItemGunBase(Gun357MagnumFactory.getRevolverConfig(), p));
        GUN_REVOLVER_SATURNITE =
                gun("gun_revolver_saturnite", p -> new ItemGunBase(Gun357MagnumFactory.getRevolverSaturniteConfig(), p));
        GUN_REVOLVER_GOLD = gun("gun_revolver_gold", p -> new ItemGunBase(Gun357MagnumFactory.getRevolverGoldConfig(), p));
        GUN_REVOLVER_SCHRABIDIUM =
                gun("gun_revolver_schrabidium", p -> new ItemGunBase(Gun357MagnumFactory.getRevolverSchrabidiumConfig(), p));
        GUN_REVOLVER_CURSED =
                gun("gun_revolver_cursed", p -> new ItemGunBase(Gun357MagnumFactory.getRevolverCursedConfig(), p));
        GUN_REVOLVER_NIGHTMARE =
                gun("gun_revolver_nightmare", p -> new ItemGunBase(Gun357MagnumFactory.getRevolverNightmareConfig(), p));
        GUN_REVOLVER_NIGHTMARE2 =
                gun("gun_revolver_nightmare2", p -> new ItemGunBase(Gun357MagnumFactory.getRevolverNightmare2Config(), p));
        GUN_BIO_REVOLVER = gun("gun_bio_revolver", p -> new ItemGunBio(Gun357MagnumFactory.getRevolverBioConfig(), p));

        AMMO_357_IRON = ammo("ammo_357_iron");
        AMMO_357_LEAD = ammo("ammo_357_lead");
        AMMO_357_NUCLEAR = ammo("ammo_357_nuclear");
        AMMO_357_GOLD = ammo("ammo_357_gold");
        AMMO_357_DESH = ammo("ammo_357_desh", AmmoItemTrait.PRO_FIT_357, AmmoItemTrait.PRO_DAMAGE_SLIGHT);
        AMMO_357_SCHRABIDIUM = ammo("ammo_357_schrabidium");
        AMMO_357_STEEL = ammo("ammo_357_steel");
        AMMO_357_NIGHTMARE1 = ammo("ammo_357_nightmare1");
        AMMO_357_NIGHTMARE2 = ammo("ammo_357_nightmare2");

        CLIP_REVOLVER_IRON = clip("clip_revolver_iron", AMMO_357_IRON, 20);
        CLIP_REVOLVER = clip("clip_revolver", AMMO_357_LEAD, 12);
        CLIP_REVOLVER_GOLD = clip("clip_revolver_gold", AMMO_357_GOLD, 6);
        CLIP_REVOLVER_LEAD = clip("clip_revolver_lead", AMMO_357_NUCLEAR, 6);
        CLIP_REVOLVER_SCHRABIDIUM = clip("clip_revolver_schrabidium", AMMO_357_SCHRABIDIUM, 2);
        CLIP_REVOLVER_CURSED = clip("clip_revolver_cursed", AMMO_357_STEEL, 17);
        CLIP_REVOLVER_NIGHTMARE = clip("clip_revolver_nightmare", AMMO_357_NIGHTMARE1, 6);
        CLIP_REVOLVER_NIGHTMARE2 = clip("clip_revolver_nightmare2", AMMO_357_NIGHTMARE2, 6);
    }

    private static <T extends Item> RegistryHandle<T> add(RegistryHandle<T> handle) {
        TAB.add(handle);
        GunContent.markPreRework(handle.id());
        return handle;
    }

    private static <T extends ItemGunBase> RegistryHandle<T> gun(
            String name, Function<Item.Properties, T> factory) {
        return add(NeoForgeRegistrar.registerItem(name, p -> factory.apply(p.stacksTo(1)), Item.Properties::new));
    }

    private static RegistryHandle<ItemAmmoLegacy> ammo(String name, AmmoItemTrait... traits) {
        return add(NeoForgeRegistrar.registerItem(name, p -> new ItemAmmoLegacy(p, traits), Item.Properties::new));
    }

    private static RegistryHandle<Item> part(String name) {
        return add(NeoForgeRegistrar.registerItem(name, Item::new, Item.Properties::new));
    }

    private static RegistryHandle<ItemClipLegacy> clip(String name, RegistryHandle<? extends Item> ammo, int count) {
        return add(NeoForgeRegistrar.registerItem(name, p -> new ItemClipLegacy(p, ammo::get, count), Item.Properties::new));
    }

    /** The weapons tab entries, main's order. */
    public static void acceptTab(Consumer<ItemStack> out) {
        for (RegistryHandle<? extends Item> handle : TAB) out.accept(new ItemStack(handle.get()));
    }

    /** The guns, for the client to bind their renderers. */
    public static List<RegistryHandle<? extends Item>> all() {
        return TAB;
    }
}
