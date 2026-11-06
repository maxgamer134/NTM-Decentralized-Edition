// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.items.weapon.GunContent;
import com.hbm.items.weapon.legacy.ItemAmmoLegacy.AmmoItemTrait;
import com.hbm.items.weapon.legacy.factory.Gun357MagnumFactory;
import com.hbm.items.weapon.legacy.factory.GunRocketHomingFactory;
import com.hbm.items.weapon.legacy.factory.GunEnergyFactory;
import com.hbm.items.weapon.legacy.factory.GunPoweredFactory;
import com.hbm.items.weapon.legacy.factory.GunOSIPRFactory;
import com.hbm.items.weapon.legacy.factory.GunGaussFactory;
import com.hbm.items.weapon.legacy.factory.GunDartFactory;
import com.hbm.items.weapon.legacy.factory.GunFatmanFactory;
import com.hbm.items.weapon.legacy.factory.GunGrenadeFactory;
import com.hbm.items.weapon.legacy.factory.GunRocketFactory;
import com.hbm.items.weapon.legacy.factory.Gun9mmFactory;
import com.hbm.items.weapon.legacy.factory.Gun762mmFactory;
import com.hbm.items.weapon.legacy.factory.Gun75BoltFactory;
import com.hbm.items.weapon.legacy.factory.Gun5mmFactory;
import com.hbm.items.weapon.legacy.factory.Gun556mmFactory;
import com.hbm.items.weapon.legacy.factory.Gun50BMGFactory;
import com.hbm.items.weapon.legacy.factory.Gun50AEFactory;
import com.hbm.items.weapon.legacy.factory.Gun45ACPFactory;
import com.hbm.items.weapon.legacy.factory.Gun44MagnumFactory;
import com.hbm.items.weapon.legacy.factory.Gun22LRFactory;
import com.hbm.items.weapon.legacy.factory.Gun4GaugeFactory;
import com.hbm.items.weapon.legacy.factory.Gun20GaugeFactory;
import com.hbm.items.weapon.legacy.factory.Gun12GaugeFactory;
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

    public static RegistryHandle<ItemAmmoLegacy> AMMO_12GAUGE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_12GAUGE_INCENDIARY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_12GAUGE_SHRAPNEL;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_12GAUGE_DU;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_12GAUGE_MARAUDER;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_12GAUGE_SLEEK;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_12GAUGE_PERCUSSION;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_20GAUGE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_20GAUGE_SLUG;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_20GAUGE_FLECHETTE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_20GAUGE_INCENDIARY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_20GAUGE_SHRAPNEL;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_20GAUGE_EXPLOSIVE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_20GAUGE_CAUSTIC;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_20GAUGE_SHOCK;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_20GAUGE_WITHER;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_20GAUGE_SLEEK;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_SLUG;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_FLECHETTE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_FLECHETTE_PHOSPHORUS;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_EXPLOSIVE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_SEMTEX;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_BALEFIRE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_KAMPF;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_CANISTER;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_SLEEK;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_CLAW;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_VAMPIRE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_VOID;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_4GAUGE_TITAN;
    public static RegistryHandle<ItemGunBase> GUN_UBOINIK;
    public static RegistryHandle<ItemGunBase> GUN_REMINGTON;
    public static RegistryHandle<ItemGunBase> GUN_SPAS12_LEGACY;
    public static RegistryHandle<ItemGunBase> GUN_BENELLI;
    public static RegistryHandle<ItemGunBase> GUN_SUPERSHOTGUN;
    public static RegistryHandle<ItemGunBase> GUN_KS23;
    public static RegistryHandle<ItemGunBase> GUN_SAUER;
    public static RegistryHandle<ItemGunBase> GUN_LEVER_ACTION;
    public static RegistryHandle<ItemGunBase> GUN_LEVER_ACTION_DARK;
    public static RegistryHandle<Item> MECHANISM_RIFLE_1;
    public static RegistryHandle<Item> MECHANISM_RIFLE_2;
    public static RegistryHandle<Item> CASING_50;
    public static RegistryHandle<Item> PELLET_FLECHETTE;

    public static RegistryHandle<Item> PELLET_CANISTER;
    public static RegistryHandle<Item> PELLET_CLAWS;
    public static RegistryHandle<Item> TOOTHPICKS;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_22LR;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_22LR_AP;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_22LR_CHLOROPHYTE;
    public static RegistryHandle<ItemGunBase> GUN_UZI_LEGACY;
    public static RegistryHandle<ItemGunBase> GUN_UZI_SILENCER;
    public static RegistryHandle<ItemGunBase> GUN_UZI_SATURNITE;
    public static RegistryHandle<ItemGunBase> GUN_UZI_SATURNITE_SILENCER;
    public static RegistryHandle<Item> ASSEMBLY_UZI;
    public static RegistryHandle<Item> CASING_9;
    public static RegistryHandle<Item> PELLET_CHLOROPHYTE;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_44;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_44_AP;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_44_DU;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_44_PHOSPHORUS;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_44_STAR;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_44_CHLOROPHYTE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_44_PIP;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_44_BJ;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_44_SILVER;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_44_ROCKET;
    public static RegistryHandle<ItemGunPip> GUN_REVOLVER_PIP;
    public static RegistryHandle<ItemGunBase> GUN_REVOLVER_NOPIP;
    public static RegistryHandle<ItemGunBase> GUN_REVOLVER_BLACKJACK;
    public static RegistryHandle<ItemGunBase> GUN_REVOLVER_SILVER;
    public static RegistryHandle<ItemGunBase> GUN_REVOLVER_RED;
    public static RegistryHandle<Item> ASSEMBLY_NOPIP;

    public static RegistryHandle<Item> CASING_44;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_45;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_45_AP;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_45_DU;
    public static RegistryHandle<ItemGunBase> GUN_THOMPSON;
    public static RegistryHandle<ItemGunBase> GUN_UAC_PISTOL;
    public static RegistryHandle<Item> ASSEMBLY_45;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_50AE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50AE_AP;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50AE_DU;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50AE_STAR;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50AE_CHLOROPHYTE;
    public static RegistryHandle<ItemGunBase> GUN_DEAGLE;
    public static RegistryHandle<Item> ASSEMBLY_ACTIONEXPRESS;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG_INCENDIARY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG_PHOSPHORUS;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG_EXPLOSIVE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG_AP;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG_DU;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG_STAR;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG_CHLOROPHYTE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG_SLEEK;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG_FLECHETTE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG_FLECHETTE_AM;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_50BMG_FLECHETTE_PO;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_LUNA;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_LUNA_INCENDIARY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_LUNA_EXPLOSIVE;
    public static RegistryHandle<ItemGunBase> GUN_AR15;
    public static RegistryHandle<ItemGunBase> GUN_LUNATIC_MARKSMAN;
    public static RegistryHandle<Item> ASSEMBLY_CALAMITY;
    public static RegistryHandle<Item> ASSEMBLY_LUNA;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_556;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_GOLD;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_PHOSPHORUS;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_AP;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_DU;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_STAR;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_CHLOROPHYTE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_SLEEK;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_TRACER;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_FLECHETTE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_FLECHETTE_INCENDIARY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_FLECHETTE_PHOSPHORUS;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_FLECHETTE_DU;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_FLECHETTE_CHLOROPHYTE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_FLECHETTE_SLEEK;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_556_K;
    public static RegistryHandle<ItemGunBase> GUN_FLECHETTE;
    public static RegistryHandle<ItemGunBase> GUN_MP;
    public static RegistryHandle<ItemGunBase> GUN_VORTEX;
    public static RegistryHandle<Item> ASSEMBLY_556;
    public static RegistryHandle<Item> MECHANISM_LAUNCHER_1;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_5MM;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_5MM_EXPLOSIVE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_5MM_DU;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_5MM_STAR;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_5MM_CHLOROPHYTE;
    public static RegistryHandle<ItemGunLacunae> GUN_MINIGUN_LEGACY;
    public static RegistryHandle<ItemGunLacunae> GUN_LACUNAE;
    public static RegistryHandle<Item> ASSEMBLY_LACUNAE;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_75BOLT;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_75BOLT_INCENDIARY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_75BOLT_HE;
    public static RegistryHandle<ItemGunBase> GUN_BOLTER_LEGACY;
    public static RegistryHandle<ItemGunBase> GUN_BOLTER_DIGAMMA;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_762;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_762_AP;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_762_DU;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_762_TRACER;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_762_PHOSPHORUS;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_762_K;
    public static RegistryHandle<ItemGunBase> GUN_CALAMITY;
    public static RegistryHandle<ItemGunBase> GUN_BOLT_ACTION;
    public static RegistryHandle<ItemGunBase> GUN_BOLT_ACTION_GREEN;
    public static RegistryHandle<ItemGunBase> GUN_BOLT_ACTION_SATURNITE;
    public static RegistryHandle<Item> ASSEMBLY_762;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_9MM;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_9MM_AP;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_9MM_DU;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_9MM_CHLOROPHYTE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_9MM_ROCKET;
    public static RegistryHandle<ItemGunBase> GUN_MP40;
    public static RegistryHandle<Item> ASSEMBLY_SMG;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_HE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_INCENDIARY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_EMP;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_SLEEK;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_SHRAPNEL;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_GLARE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_NUCLEAR;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_TOXIC;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_RPC;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_PHOSPHORUS;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_CANISTER;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_ROCKET_DIGAMMA;
    public static RegistryHandle<ItemGunBase> GUN_RPG;
    public static RegistryHandle<ItemGunBase> GUN_KARL;
    public static RegistryHandle<ItemGunBase> GUN_PANZERSCHRECK_LEGACY;
    public static RegistryHandle<ItemGunBase> GUN_QUADRO_LEGACY;
    public static RegistryHandle<Item> MECHANISM_LAUNCHER_2;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE_HE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE_INCENDIARY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE_PHOSPHORUS;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE_TOXIC;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE_SLEEK;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE_CONCUSSION;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE_FINNED;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE_NUCLEAR;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE_TRACER;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE_KAMPF;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_GRENADE_LEADBURSTER;
    public static RegistryHandle<ItemGunBase> GUN_HK69;
    public static RegistryHandle<ItemGunBase> GUN_CONGOLAKE_LEGACY;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_NUKE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_NUKE_LOW;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_NUKE_HIGH;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_NUKE_TOTS;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_NUKE_SAFE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_NUKE_PUMPKIN;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_MIRV;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_MIRV_LOW;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_MIRV_HIGH;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_MIRV_SAFE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_MIRV_SPECIAL;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_NUKE_BALEFIRE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_NUKE_BARREL;
    public static RegistryHandle<ItemGunBase> GUN_FATMAN_LEGACY;
    public static RegistryHandle<ItemGunBase> GUN_PROTO;
    public static RegistryHandle<ItemGunBase> GUN_MIRV;
    public static RegistryHandle<ItemGunBase> GUN_BF;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_DART;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_DART_NUCLEAR;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_DART_NERF;
    public static RegistryHandle<ItemGunBase> GUN_MYMY;
    public static RegistryHandle<ItemGunDart> GUN_DARTER;

    public static RegistryHandle<ItemGunGauss> GUN_XVL1456;
    public static RegistryHandle<Item> GUN_XVL1456_AMMO;

    public static RegistryHandle<ItemGunOSIPR> GUN_OSIPR;
    public static RegistryHandle<Item> GUN_OSIPR_AMMO;
    public static RegistryHandle<Item> GUN_OSIPR_AMMO2;

    public static RegistryHandle<ItemEnergyGunBase> GUN_GLASS_CANNON;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_COILGUN;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_COILGUN_DU;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_COILGUN_RUBBER;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_FIREEXT_LEGACY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_FIREEXT_FOAM_LEGACY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_FIREEXT_SAND_LEGACY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_FUEL;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_FUEL_NAPALM;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_FUEL_PHOSPHORUS;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_FUEL_VAPORIZER;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_FUEL_GAS;
    public static RegistryHandle<ItemCoilgun> GUN_COILGUN_LEGACY;
    public static RegistryHandle<ItemGunBase> GUN_FLAMER_LEGACY;
    public static RegistryHandle<ItemCryoCannon> GUN_CRYOCANNON;
    public static RegistryHandle<ItemGunBase> GUN_FIREEXT_LEGACY;
    public static RegistryHandle<ItemGunBase> GUN_ZOMG;
    public static RegistryHandle<ItemGunBase> GUN_EMP;
    public static RegistryHandle<Item> GUN_EMP_AMMO;
    public static RegistryHandle<Item> GUN_CRYOLATOR_AMMO;

    public static RegistryHandle<com.hbm.items.special.ItemCustomLore> GUN_SUPER_SHOTGUN;
    public static RegistryHandle<com.hbm.items.special.ItemCustomLore> GUN_MOIST_NUGGET;
    public static RegistryHandle<ItemGunChemthrowerLegacy> GUN_CHEMTHROWER_LEGACY;
    public static RegistryHandle<ItemGunSpecial.Suicide> GUN_REVOLVER_INVERTED;
    public static RegistryHandle<Item> GUN_JACK_AMMO;
    public static RegistryHandle<ItemGunSpecial.Jack> GUN_JACK;
    public static RegistryHandle<Item> GUN_EUTHANASIA_AMMO;
    public static RegistryHandle<ItemGunSpecial.Euthanasia> GUN_EUTHANASIA;
    public static RegistryHandle<Item> GUN_TWIGUN_AMMO;
    public static RegistryHandle<ItemGunSpecial.Euthanasia> GUN_TWIGUN;
    public static RegistryHandle<Item> GUN_DEFABRICATOR_AMMO;
    public static RegistryHandle<ItemGunSpecial.Defabricator> GUN_DEFABRICATOR;
    public static RegistryHandle<ItemClipLegacy> CLIP_JACK;
    public static RegistryHandle<ItemClipLegacy> CLIP_EUTHANASIA;
    public static RegistryHandle<ItemClipLegacy> CLIP_DEFABRICATOR;

    public static RegistryHandle<ItemAmmoLegacy> AMMO_STINGER_ROCKET;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_STINGER_ROCKET_HE;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_STINGER_ROCKET_INCENDIARY;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_STINGER_ROCKET_NUCLEAR;
    public static RegistryHandle<ItemAmmoLegacy> AMMO_STINGER_ROCKET_BONES;
    public static RegistryHandle<ItemGunBase> GUN_STINGER_LEGACY;
    public static RegistryHandle<ItemGunBase> GUN_SKYSTINGER;

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

        // Gauge
        AMMO_12GAUGE = ammo("ammo_12gauge");
        AMMO_12GAUGE_INCENDIARY = ammo("ammo_12gauge_incendiary", AmmoItemTrait.PRO_INCENDIARY, AmmoItemTrait.CON_WEAR);
        AMMO_12GAUGE_SHRAPNEL = ammo("ammo_12gauge_shrapnel", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.NEU_MORE_BOUNCY, AmmoItemTrait.CON_WEAR);
        AMMO_12GAUGE_DU = ammo("ammo_12gauge_du", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.NEU_HEAVY_METAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_12GAUGE_MARAUDER = ammo("ammo_12gauge_marauder", AmmoItemTrait.PRO_MARAUDER, AmmoItemTrait.NEU_NO_CON);
        AMMO_12GAUGE_SLEEK = ammo("ammo_12gauge_sleek", AmmoItemTrait.NEU_MASKMAN_FLECHETTE);
        AMMO_12GAUGE_PERCUSSION = ammo("ammo_12gauge_percussion", AmmoItemTrait.PRO_PERCUSSION, AmmoItemTrait.CON_NO_PROJECTILE);
        AMMO_20GAUGE = ammo("ammo_20gauge");
        AMMO_20GAUGE_SLUG = ammo("ammo_20gauge_slug", AmmoItemTrait.PRO_ACCURATE2, AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WEAR, AmmoItemTrait.CON_SING_PROJECTILE);
        AMMO_20GAUGE_FLECHETTE = ammo("ammo_20gauge_flechette", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.NEU_LESS_BOUNCY, AmmoItemTrait.CON_WEAR);
        AMMO_20GAUGE_INCENDIARY = ammo("ammo_20gauge_incendiary", AmmoItemTrait.PRO_INCENDIARY, AmmoItemTrait.CON_WEAR);
        AMMO_20GAUGE_SHRAPNEL = ammo("ammo_20gauge_shrapnel", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.NEU_MORE_BOUNCY, AmmoItemTrait.CON_WEAR);
        AMMO_20GAUGE_EXPLOSIVE = ammo("ammo_20gauge_explosive", AmmoItemTrait.PRO_EXPLOSIVE, AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_20GAUGE_CAUSTIC = ammo("ammo_20gauge_caustic", AmmoItemTrait.PRO_TOXIC, AmmoItemTrait.PRO_CAUSTIC, AmmoItemTrait.NEU_NO_BOUNCE, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_20GAUGE_SHOCK = ammo("ammo_20gauge_shock", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_STUNNING, AmmoItemTrait.PRO_EMP, AmmoItemTrait.NEU_NO_BOUNCE, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_20GAUGE_WITHER = ammo("ammo_20gauge_wither", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WITHERING);
        AMMO_20GAUGE_SLEEK = ammo("ammo_20gauge_sleek", AmmoItemTrait.NEU_MASKMAN_FLECHETTE);
        AMMO_4GAUGE = ammo("ammo_4gauge");
        AMMO_4GAUGE_SLUG = ammo("ammo_4gauge_slug", AmmoItemTrait.PRO_ACCURATE2, AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WEAR, AmmoItemTrait.CON_SING_PROJECTILE);
        AMMO_4GAUGE_FLECHETTE = ammo("ammo_4gauge_flechette", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.NEU_LESS_BOUNCY, AmmoItemTrait.CON_WEAR);
        AMMO_4GAUGE_FLECHETTE_PHOSPHORUS = ammo("ammo_4gauge_flechette_phosphorus", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_PHOSPHORUS, AmmoItemTrait.NEU_WARCRIME2, AmmoItemTrait.NEU_LESS_BOUNCY, AmmoItemTrait.CON_WEAR);
        AMMO_4GAUGE_EXPLOSIVE = ammo("ammo_4gauge_explosive", AmmoItemTrait.PRO_EXPLOSIVE, AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.NEU_40MM, AmmoItemTrait.CON_HEAVY_WEAR, AmmoItemTrait.CON_SING_PROJECTILE);
        AMMO_4GAUGE_SEMTEX = ammo("ammo_4gauge_semtex", AmmoItemTrait.PRO_EXPLOSIVE, AmmoItemTrait.PRO_MINING, AmmoItemTrait.CON_NO_EXPLODE3, AmmoItemTrait.CON_HEAVY_WEAR, AmmoItemTrait.CON_SING_PROJECTILE);
        AMMO_4GAUGE_BALEFIRE = ammo("ammo_4gauge_balefire", AmmoItemTrait.PRO_EXPLOSIVE, AmmoItemTrait.PRO_BALEFIRE, AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_HEAVY_WEAR, AmmoItemTrait.CON_SING_PROJECTILE);
        AMMO_4GAUGE_KAMPF = ammo("ammo_4gauge_kampf", AmmoItemTrait.PRO_EXPLOSIVE, AmmoItemTrait.PRO_ROCKET_PROPELLED, AmmoItemTrait.PRO_ACCURATE1, AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_WEAR, AmmoItemTrait.CON_SING_PROJECTILE);
        AMMO_4GAUGE_CANISTER = ammo("ammo_4gauge_canister");
        AMMO_4GAUGE_SLEEK = ammo("ammo_4gauge_sleek", AmmoItemTrait.NEU_MASKMAN_FLECHETTE);
        AMMO_4GAUGE_CLAW = ammo("ammo_4gauge_claw");
        AMMO_4GAUGE_VAMPIRE = ammo("ammo_4gauge_vampire");
        AMMO_4GAUGE_VOID = ammo("ammo_4gauge_void");
        AMMO_4GAUGE_TITAN = ammo("ammo_4gauge_titan", AmmoItemTrait.PRO_MARAUDER, AmmoItemTrait.NEU_NO_CON);
        GUN_UBOINIK = gun("gun_uboinik", p -> new ItemGunBase(Gun12GaugeFactory.getUboinikConfig(), p));
        GUN_REMINGTON = gun("gun_remington", p -> new ItemGunBase(Gun12GaugeFactory.getRemington870Config(), p));
        GUN_SPAS12_LEGACY = gun("gun_spas12_legacy", p -> new ItemGunBase(Gun12GaugeFactory.getSpas12Config(), Gun12GaugeFactory.getSpas12AltConfig(), p));
        GUN_BENELLI = gun("gun_benelli", p -> new ItemGunBase(Gun12GaugeFactory.getBenelliModConfig(), p));
        GUN_SUPERSHOTGUN = gun("gun_supershotgun", p -> new ItemGunBase(Gun12GaugeFactory.getShottyConfig(), p));
        GUN_KS23 = gun("gun_ks23", p -> new ItemGunBase(Gun4GaugeFactory.getKS23Config(), p));
        GUN_SAUER = gun("gun_sauer", p -> new ItemGunBase(Gun4GaugeFactory.getSauerConfig(), p));
        GUN_LEVER_ACTION = gun("gun_lever_action", p -> new ItemGunBase(Gun20GaugeFactory.getMareConfig(), p));
        GUN_LEVER_ACTION_DARK = gun("gun_lever_action_dark", p -> new ItemGunBase(Gun20GaugeFactory.getMareDarkConfig(), p));
        MECHANISM_RIFLE_1 = part("mechanism_rifle_1");
        MECHANISM_RIFLE_2 = part("mechanism_rifle_2");
        CASING_50 = part("casing_50");
        PELLET_FLECHETTE = part("pellet_flechette");

        // gauge pellets
        PELLET_CANISTER = part("pellet_canister");
        PELLET_CLAWS = part("pellet_claws");
        TOOTHPICKS = part("toothpicks");

        // 22LR
        AMMO_22LR = ammo("ammo_22lr");
        AMMO_22LR_AP = ammo("ammo_22lr_ap", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_WEAR);
        AMMO_22LR_CHLOROPHYTE = ammo("ammo_22lr_chlorophyte", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WEAR, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.NEU_CHLOROPHYTE, AmmoItemTrait.NEU_HOMING, AmmoItemTrait.CON_SPEED);
        GUN_UZI_LEGACY = gun("gun_uzi_legacy", p -> new ItemGunBase(Gun22LRFactory.getUziConfig(), p));
        GUN_UZI_SILENCER = gun("gun_uzi_silencer", p -> new ItemGunBase(Gun22LRFactory.getUziConfig().silenced(), p));
        GUN_UZI_SATURNITE = gun("gun_uzi_saturnite", p -> new ItemGunBase(Gun22LRFactory.getSaturniteConfig(), p));
        GUN_UZI_SATURNITE_SILENCER = gun("gun_uzi_saturnite_silencer", p -> new ItemGunBase(Gun22LRFactory.getSaturniteConfig().silenced(), p));
        ASSEMBLY_UZI = part("assembly_uzi");
        CASING_9 = part("casing_9");
        PELLET_CHLOROPHYTE = part("pellet_chlorophyte");

        // 44
        AMMO_44 = ammo("ammo_44");
        AMMO_44_AP = ammo("ammo_44_ap", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_WEAR);
        AMMO_44_DU = ammo("ammo_44_du", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_HEAVY_METAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_44_PHOSPHORUS = ammo("ammo_44_phosphorus", AmmoItemTrait.PRO_PHOSPHORUS, AmmoItemTrait.NEU_WARCRIME1, AmmoItemTrait.CON_WEAR, AmmoItemTrait.CON_PENETRATION);
        AMMO_44_STAR = ammo("ammo_44_star", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_STARMETAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_44_CHLOROPHYTE = ammo("ammo_44_chlorophyte", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WEAR, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.NEU_CHLOROPHYTE, AmmoItemTrait.NEU_HOMING, AmmoItemTrait.CON_SPEED);
        AMMO_44_PIP = ammo("ammo_44_pip", AmmoItemTrait.NEU_BOXCAR, AmmoItemTrait.CON_DAMAGE);
        AMMO_44_BJ = ammo("ammo_44_bj", AmmoItemTrait.NEU_BOAT, AmmoItemTrait.CON_DAMAGE);
        AMMO_44_SILVER = ammo("ammo_44_silver", AmmoItemTrait.NEU_BUILDING, AmmoItemTrait.CON_DAMAGE);
        AMMO_44_ROCKET = ammo("ammo_44_rocket", AmmoItemTrait.PRO_ROCKET, AmmoItemTrait.NEU_UHH);
        GUN_REVOLVER_PIP = gun("gun_revolver_pip", p -> new ItemGunPip(Gun44MagnumFactory.getMacintoshConfig(), p));
        GUN_REVOLVER_NOPIP = gun("gun_revolver_nopip", p -> new ItemGunBase(Gun44MagnumFactory.getNovacConfig(), p));
        GUN_REVOLVER_BLACKJACK = gun("gun_revolver_blackjack", p -> new ItemGunBase(Gun44MagnumFactory.getBlackjackConfig(), p));
        GUN_REVOLVER_SILVER = gun("gun_revolver_silver", p -> new ItemGunBase(Gun44MagnumFactory.getSilverConfig(), p));
        GUN_REVOLVER_RED = gun("gun_revolver_red", p -> new ItemGunBase(Gun44MagnumFactory.getRedConfig(), p));
        ASSEMBLY_NOPIP = part("assembly_nopip");

        // 44 casing
        CASING_44 = part("casing_44");

        // 45ACP
        AMMO_45 = ammo("ammo_45");
        AMMO_45_AP = ammo("ammo_45_ap", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_WEAR);
        AMMO_45_DU = ammo("ammo_45_du", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_HEAVY_METAL, AmmoItemTrait.CON_HEAVY_WEAR);
        GUN_THOMPSON = gun("gun_thompson", p -> new ItemGunBase(Gun45ACPFactory.getThompsonConfig(), p));
        GUN_UAC_PISTOL = gun("gun_uac_pistol", p -> new ItemGunBase(Gun45ACPFactory.getUACPistolConfig(), Gun45ACPFactory.getUACPistolBurstConfig(), p));
        ASSEMBLY_45 = part("assembly_45");

        // 50AE
        AMMO_50AE = ammo("ammo_50ae");
        AMMO_50AE_AP = ammo("ammo_50ae_ap", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_WEAR);
        AMMO_50AE_DU = ammo("ammo_50ae_du", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_HEAVY_METAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_50AE_STAR = ammo("ammo_50ae_star", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_STARMETAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_50AE_CHLOROPHYTE = ammo("ammo_50ae_chlorophyte", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WEAR, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.NEU_CHLOROPHYTE, AmmoItemTrait.NEU_HOMING, AmmoItemTrait.CON_SPEED);
        GUN_DEAGLE = gun("gun_deagle", p -> new ItemGunBase(Gun50AEFactory.getDeagleConfig(), p));
        ASSEMBLY_ACTIONEXPRESS = part("assembly_actionexpress");

        // 50BMG
        AMMO_50BMG = ammo("ammo_50bmg");
        AMMO_50BMG_INCENDIARY = ammo("ammo_50bmg_incendiary", AmmoItemTrait.PRO_INCENDIARY, AmmoItemTrait.CON_WEAR);
        AMMO_50BMG_PHOSPHORUS = ammo("ammo_50bmg_phosphorus", AmmoItemTrait.PRO_PHOSPHORUS, AmmoItemTrait.NEU_WARCRIME1, AmmoItemTrait.CON_WEAR, AmmoItemTrait.CON_PENETRATION);
        AMMO_50BMG_EXPLOSIVE = ammo("ammo_50bmg_explosive", AmmoItemTrait.PRO_EXPLOSIVE, AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_50BMG_AP = ammo("ammo_50bmg_ap", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_WEAR);
        AMMO_50BMG_DU = ammo("ammo_50bmg_du", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_HEAVY_METAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_50BMG_STAR = ammo("ammo_50bmg_star", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_STARMETAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_50BMG_CHLOROPHYTE = ammo("ammo_50bmg_chlorophyte", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WEAR, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.NEU_CHLOROPHYTE, AmmoItemTrait.NEU_HOMING, AmmoItemTrait.CON_SPEED);
        AMMO_50BMG_SLEEK = ammo("ammo_50bmg_sleek", AmmoItemTrait.NEU_MASKMAN_METEORITE);
        AMMO_50BMG_FLECHETTE = ammo("ammo_50bmg_flechette", AmmoItemTrait.PRO_DAMAGE);
        AMMO_50BMG_FLECHETTE_AM = ammo("ammo_50bmg_flechette_am", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.NEU_UHH);
        AMMO_50BMG_FLECHETTE_PO = ammo("ammo_50bmg_flechette_po", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.NEU_UHH);
        AMMO_LUNA = ammo("ammo_luna");
        AMMO_LUNA_INCENDIARY = ammo("ammo_luna_incendiary");
        AMMO_LUNA_EXPLOSIVE = ammo("ammo_luna_explosive");
        GUN_AR15 = gun("gun_ar15", p -> new ItemGunBase(Gun50BMGFactory.getAR15Config(), Gun50BMGFactory.getAR15BurstConfig(), p));
        GUN_LUNATIC_MARKSMAN = gun("gun_lunatic_marksman", p -> new ItemGunBase(Gun50BMGFactory.getLunaticMarksman(), p));
        ASSEMBLY_CALAMITY = part("assembly_calamity");
        ASSEMBLY_LUNA = part("assembly_luna");

        // 556
        AMMO_556 = ammo("ammo_556");
        AMMO_556_GOLD = ammo("ammo_556_gold");
        AMMO_556_PHOSPHORUS = ammo("ammo_556_phosphorus", AmmoItemTrait.PRO_PHOSPHORUS, AmmoItemTrait.NEU_WARCRIME1, AmmoItemTrait.CON_WEAR, AmmoItemTrait.CON_PENETRATION);
        AMMO_556_AP = ammo("ammo_556_ap", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_WEAR);
        AMMO_556_DU = ammo("ammo_556_du", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_HEAVY_METAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_556_STAR = ammo("ammo_556_star", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_STARMETAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_556_CHLOROPHYTE = ammo("ammo_556_chlorophyte", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WEAR, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.NEU_CHLOROPHYTE, AmmoItemTrait.NEU_HOMING, AmmoItemTrait.CON_SPEED);
        AMMO_556_SLEEK = ammo("ammo_556_sleek", AmmoItemTrait.NEU_MASKMAN_METEORITE);
        AMMO_556_TRACER = ammo("ammo_556_tracer", AmmoItemTrait.NEU_TRACER);
        AMMO_556_FLECHETTE = ammo("ammo_556_flechette", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.NEU_LESS_BOUNCY, AmmoItemTrait.CON_WEAR);
        AMMO_556_FLECHETTE_INCENDIARY = ammo("ammo_556_flechette_incendiary", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_INCENDIARY, AmmoItemTrait.NEU_LESS_BOUNCY, AmmoItemTrait.CON_WEAR, AmmoItemTrait.CON_PENETRATION);
        AMMO_556_FLECHETTE_PHOSPHORUS = ammo("ammo_556_flechette_phosphorus", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_PHOSPHORUS, AmmoItemTrait.NEU_WARCRIME2, AmmoItemTrait.NEU_LESS_BOUNCY, AmmoItemTrait.CON_WEAR, AmmoItemTrait.CON_PENETRATION);
        AMMO_556_FLECHETTE_DU = ammo("ammo_556_flechette_du", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.NEU_HEAVY_METAL, AmmoItemTrait.NEU_LESS_BOUNCY, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_556_FLECHETTE_CHLOROPHYTE = ammo("ammo_556_flechette_chlorophyte", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WEAR, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.NEU_CHLOROPHYTE, AmmoItemTrait.NEU_HOMING, AmmoItemTrait.CON_SPEED);
        AMMO_556_FLECHETTE_SLEEK = ammo("ammo_556_flechette_sleek", AmmoItemTrait.NEU_MASKMAN_METEORITE);
        AMMO_556_K = ammo("ammo_556_k", AmmoItemTrait.NEU_BLANK);
        GUN_FLECHETTE = gun("gun_flechette", p -> new ItemGunBase(Gun556mmFactory.getSPIWConfig(), Gun556mmFactory.getGLauncherConfig(), p));
        GUN_MP = gun("gun_mp", p -> new ItemGunBase(Gun556mmFactory.getEuphieConfig(), p));
        GUN_VORTEX = gun("gun_vortex", p -> new ItemGunBase(Gun556mmFactory.getEuphieConfig(), p));
        ASSEMBLY_556 = part("assembly_556");
        MECHANISM_LAUNCHER_1 = part("mechanism_launcher_1");

        // 5mm
        AMMO_5MM = ammo("ammo_5mm");
        AMMO_5MM_EXPLOSIVE = ammo("ammo_5mm_explosive", AmmoItemTrait.PRO_EXPLOSIVE, AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_5MM_DU = ammo("ammo_5mm_du", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_HEAVY_METAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_5MM_STAR = ammo("ammo_5mm_star", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_STARMETAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_5MM_CHLOROPHYTE = ammo("ammo_5mm_chlorophyte", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WEAR, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.NEU_CHLOROPHYTE, AmmoItemTrait.NEU_HOMING, AmmoItemTrait.CON_SPEED);
        GUN_MINIGUN_LEGACY = gun("gun_minigun_legacy", p -> new ItemGunLacunae(Gun5mmFactory.get53Config(), p));
        GUN_LACUNAE = gun("gun_lacunae", p -> new ItemGunLacunae(Gun5mmFactory.getLacunaeConfig(), p));
        ASSEMBLY_LACUNAE = part("assembly_lacunae");

        // 75Bolt
        AMMO_75BOLT = ammo("ammo_75bolt");
        AMMO_75BOLT_INCENDIARY = ammo("ammo_75bolt_incendiary");
        AMMO_75BOLT_HE = ammo("ammo_75bolt_he");
        GUN_BOLTER_LEGACY = gun("gun_bolter_legacy", p -> new ItemGunBase(Gun75BoltFactory.getBolterConfig(), p));
        GUN_BOLTER_DIGAMMA = gun("gun_bolter_digamma", p -> new ItemGunBase(Gun75BoltFactory.getBolterConfig(), p));

        // 762
        AMMO_762 = ammo("ammo_762");
        AMMO_762_AP = ammo("ammo_762_ap", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_WEAR);
        AMMO_762_DU = ammo("ammo_762_du", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_HEAVY_METAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_762_TRACER = ammo("ammo_762_tracer", AmmoItemTrait.NEU_TRACER);
        AMMO_762_PHOSPHORUS = ammo("ammo_762_phosphorus", AmmoItemTrait.PRO_PHOSPHORUS, AmmoItemTrait.NEU_WARCRIME1, AmmoItemTrait.CON_WEAR, AmmoItemTrait.CON_PENETRATION);
        AMMO_762_K = ammo("ammo_762_k", AmmoItemTrait.NEU_BLANK);
        GUN_CALAMITY = gun("gun_calamity", p -> new ItemGunBase(Gun762mmFactory.getCalamityConfig(), p));
        GUN_BOLT_ACTION = gun("gun_bolt_action", p -> new ItemGunBase(Gun762mmFactory.getBoltConfig(), p));
        GUN_BOLT_ACTION_GREEN = gun("gun_bolt_action_green", p -> new ItemGunBase(Gun762mmFactory.getBoltGreenConfig(), p));
        GUN_BOLT_ACTION_SATURNITE = gun("gun_bolt_action_saturnite", p -> new ItemGunBase(Gun762mmFactory.getBoltSaturniteConfig(), p));
        ASSEMBLY_762 = part("assembly_762");

        // 9mm
        AMMO_9MM = ammo("ammo_9mm");
        AMMO_9MM_AP = ammo("ammo_9mm_ap", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_WEAR);
        AMMO_9MM_DU = ammo("ammo_9mm_du", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_HEAVY_METAL, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_9MM_CHLOROPHYTE = ammo("ammo_9mm_chlorophyte", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WEAR, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.NEU_CHLOROPHYTE, AmmoItemTrait.NEU_HOMING, AmmoItemTrait.CON_SPEED);
        AMMO_9MM_ROCKET = ammo("ammo_9mm_rocket", AmmoItemTrait.PRO_ROCKET, AmmoItemTrait.NEU_UHH);
        GUN_MP40 = gun("gun_mp40", p -> new ItemGunBase(Gun9mmFactory.getMP40Config(), p));
        ASSEMBLY_SMG = part("assembly_smg");

        // Rocket
        AMMO_ROCKET = ammo("ammo_rocket");
        AMMO_ROCKET_HE = ammo("ammo_rocket_he", AmmoItemTrait.PRO_RADIUS, AmmoItemTrait.CON_WEAR);
        AMMO_ROCKET_INCENDIARY = ammo("ammo_rocket_incendiary", AmmoItemTrait.PRO_INCENDIARY, AmmoItemTrait.CON_WEAR);
        AMMO_ROCKET_EMP = ammo("ammo_rocket_emp", AmmoItemTrait.PRO_EMP, AmmoItemTrait.CON_RADIUS);
        AMMO_ROCKET_SLEEK = ammo("ammo_rocket_sleek", AmmoItemTrait.PRO_RADIUS_HIGH, AmmoItemTrait.PRO_NO_GRAVITY, AmmoItemTrait.CON_SPEED);
        AMMO_ROCKET_SHRAPNEL = ammo("ammo_rocket_shrapnel", AmmoItemTrait.PRO_SHRAPNEL);
        AMMO_ROCKET_GLARE = ammo("ammo_rocket_glare", AmmoItemTrait.PRO_SPEED, AmmoItemTrait.PRO_INCENDIARY, AmmoItemTrait.CON_WEAR);
        AMMO_ROCKET_NUCLEAR = ammo("ammo_rocket_nuclear", AmmoItemTrait.PRO_NUCLEAR, AmmoItemTrait.CON_SUPER_WEAR, AmmoItemTrait.CON_SPEED);
        AMMO_ROCKET_TOXIC = ammo("ammo_rocket_toxic", AmmoItemTrait.PRO_CHLORINE, AmmoItemTrait.CON_NO_EXPLODE1, AmmoItemTrait.CON_SPEED);
        AMMO_ROCKET_RPC = ammo("ammo_rocket_rpc", AmmoItemTrait.PRO_CHAINSAW, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.PRO_NO_GRAVITY, AmmoItemTrait.CON_WEAR, AmmoItemTrait.CON_NO_EXPLODE1, AmmoItemTrait.NEU_UHH);
        AMMO_ROCKET_PHOSPHORUS = ammo("ammo_rocket_phosphorus", AmmoItemTrait.PRO_PHOSPHORUS_SPLASH, AmmoItemTrait.NEU_WARCRIME1, AmmoItemTrait.CON_WEAR);
        AMMO_ROCKET_CANISTER = ammo("ammo_rocket_canister");
        AMMO_ROCKET_DIGAMMA = ammo("ammo_rocket_digamma");
        GUN_RPG = gun("gun_rpg", p -> new ItemGunBase(GunRocketFactory.getGustavConfig(), p));
        GUN_KARL = gun("gun_karl", p -> new ItemGunBase(GunRocketFactory.getKarlConfig(), p));
        GUN_PANZERSCHRECK_LEGACY = gun("gun_panzerschreck_legacy", p -> new ItemGunBase(GunRocketFactory.getPanzConfig(), p));
        GUN_QUADRO_LEGACY = gun("gun_quadro_legacy", p -> new ItemGunBase(GunRocketFactory.getQuadroConfig(), p));
        MECHANISM_LAUNCHER_2 = part("mechanism_launcher_2");

        // Grenade
        AMMO_GRENADE = ammo("ammo_grenade");
        AMMO_GRENADE_HE = ammo("ammo_grenade_he", AmmoItemTrait.PRO_RADIUS, AmmoItemTrait.CON_WEAR);
        AMMO_GRENADE_INCENDIARY = ammo("ammo_grenade_incendiary", AmmoItemTrait.PRO_INCENDIARY, AmmoItemTrait.CON_WEAR);
        AMMO_GRENADE_PHOSPHORUS = ammo("ammo_grenade_phosphorus", AmmoItemTrait.PRO_PHOSPHORUS_SPLASH, AmmoItemTrait.NEU_WARCRIME1, AmmoItemTrait.CON_WEAR);
        AMMO_GRENADE_TOXIC = ammo("ammo_grenade_toxic", AmmoItemTrait.PRO_CHLORINE, AmmoItemTrait.CON_NO_EXPLODE1);
        AMMO_GRENADE_SLEEK = ammo("ammo_grenade_sleek", AmmoItemTrait.PRO_RADIUS, AmmoItemTrait.NEU_JOLT);
        AMMO_GRENADE_CONCUSSION = ammo("ammo_grenade_concussion", AmmoItemTrait.PRO_RADIUS, AmmoItemTrait.CON_NO_EXPLODE2);
        AMMO_GRENADE_FINNED = ammo("ammo_grenade_finned", AmmoItemTrait.PRO_GRAVITY, AmmoItemTrait.CON_RADIUS);
        AMMO_GRENADE_NUCLEAR = ammo("ammo_grenade_nuclear", AmmoItemTrait.PRO_NUCLEAR, AmmoItemTrait.PRO_RANGE, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_GRENADE_TRACER = ammo("ammo_grenade_tracer", AmmoItemTrait.NEU_BLANK);
        AMMO_GRENADE_KAMPF = ammo("ammo_grenade_kampf", AmmoItemTrait.PRO_ROCKET_PROPELLED, AmmoItemTrait.PRO_RADIUS, AmmoItemTrait.PRO_ACCURATE1, AmmoItemTrait.CON_WEAR);
        AMMO_GRENADE_LEADBURSTER = ammo("ammo_grenade_leadburster", AmmoItemTrait.NEU_LEADBURSTER, AmmoItemTrait.CON_NO_EXPLODE1);
        GUN_HK69 = gun("gun_hk69", p -> new ItemGunBase(GunGrenadeFactory.getHK69Config(), p));
        GUN_CONGOLAKE_LEGACY = gun("gun_congolake_legacy", p -> new ItemGunBase(GunGrenadeFactory.getCongoConfig(), p));

        // Fatman
        AMMO_NUKE = ammo("ammo_nuke");
        AMMO_NUKE_LOW = ammo("ammo_nuke_low", AmmoItemTrait.CON_RADIUS);
        AMMO_NUKE_HIGH = ammo("ammo_nuke_high", AmmoItemTrait.PRO_RADIUS, AmmoItemTrait.PRO_FALLOUT);
        AMMO_NUKE_TOTS = ammo("ammo_nuke_tots", AmmoItemTrait.PRO_BOMB_COUNT, AmmoItemTrait.NEU_FUN, AmmoItemTrait.CON_ACCURACY2, AmmoItemTrait.CON_RADIUS, AmmoItemTrait.CON_NO_MIRV);
        AMMO_NUKE_SAFE = ammo("ammo_nuke_safe", AmmoItemTrait.CON_RADIUS, AmmoItemTrait.CON_NO_EXPLODE2);
        AMMO_NUKE_PUMPKIN = ammo("ammo_nuke_pumpkin", AmmoItemTrait.CON_NN);
        AMMO_MIRV = ammo("ammo_mirv");
        AMMO_MIRV_LOW = ammo("ammo_mirv_low", AmmoItemTrait.CON_RADIUS);
        AMMO_MIRV_HIGH = ammo("ammo_mirv_high", AmmoItemTrait.PRO_RADIUS, AmmoItemTrait.PRO_FALLOUT);
        AMMO_MIRV_SAFE = ammo("ammo_mirv_safe", AmmoItemTrait.CON_RADIUS, AmmoItemTrait.CON_NO_EXPLODE2);
        AMMO_MIRV_SPECIAL = ammo("ammo_mirv_special");
        AMMO_NUKE_BALEFIRE = ammo("ammo_nuke_balefire");
        AMMO_NUKE_BARREL = ammo("ammo_nuke_barrel");
        GUN_FATMAN_LEGACY = gun("gun_fatman_legacy", p -> new ItemGunBase(GunFatmanFactory.getFatmanConfig(), p));
        GUN_PROTO = gun("gun_proto", p -> new ItemGunBase(GunFatmanFactory.getProtoConfig(), p));
        GUN_MIRV = gun("gun_mirv", p -> new ItemGunBase(GunFatmanFactory.getMIRVConfig(), p));
        GUN_BF = gun("gun_bf", p -> new ItemGunBase(GunFatmanFactory.getBELConfig(), p));

        // Dart
        AMMO_DART = ammo("ammo_dart");
        AMMO_DART_NUCLEAR = ammo("ammo_dart_nuclear");
        AMMO_DART_NERF = ammo("ammo_dart_nerf");
        GUN_MYMY = gun("gun_mymy", p -> new ItemGunBase(GunDartFactory.getMymyConfig(), p));
        GUN_DARTER = gun("gun_darter", p -> new ItemGunDart(GunDartFactory.getDarterConfig(), p));

        // Gauss
        GUN_XVL1456 = gun("gun_xvl1456", p -> new ItemGunGauss(GunGaussFactory.getXVLConfig(), GunGaussFactory.getChargedConfig(), p));
        GUN_XVL1456_AMMO = part("gun_xvl1456_ammo");

        // OSIPR
        GUN_OSIPR = gun("gun_osipr", p -> new ItemGunOSIPR(GunOSIPRFactory.getOSIPRConfig(), GunOSIPRFactory.getAltConfig(), p));
        GUN_OSIPR_AMMO = part("gun_osipr_ammo");
        GUN_OSIPR_AMMO2 = part("gun_osipr_ammo2");

        // Powered
        GUN_GLASS_CANNON = gun("gun_glass_cannon", p -> new ItemEnergyGunBase(GunPoweredFactory.getGlassCannonConfig(), p));

        // Energy
        AMMO_COILGUN = ammo("ammo_coilgun");
        AMMO_COILGUN_DU = ammo("ammo_coilgun_du", AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_COILGUN_RUBBER = ammo("ammo_coilgun_rubber", AmmoItemTrait.NEU_MORE_BOUNCY, AmmoItemTrait.CON_DAMAGE);
        AMMO_FIREEXT_LEGACY = ammo("ammo_fireext_legacy");
        AMMO_FIREEXT_FOAM_LEGACY = ammo("ammo_fireext_foam_legacy");
        AMMO_FIREEXT_SAND_LEGACY = ammo("ammo_fireext_sand_legacy");
        AMMO_FUEL = ammo("ammo_fuel");
        AMMO_FUEL_NAPALM = ammo("ammo_fuel_napalm", AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_RANGE, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_FUEL_PHOSPHORUS = ammo("ammo_fuel_phosphorus", AmmoItemTrait.PRO_PHOSPHORUS_SPLASH, AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_RANGE, AmmoItemTrait.PRO_RANGE, AmmoItemTrait.PRO_ACCURATE1, AmmoItemTrait.NEU_WARCRIME1, AmmoItemTrait.CON_SING_PROJECTILE, AmmoItemTrait.CON_HEAVY_WEAR);
        AMMO_FUEL_VAPORIZER = ammo("ammo_fuel_vaporizer", AmmoItemTrait.PRO_PHOSPHORUS, AmmoItemTrait.PRO_FLAMES, AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.NEU_ERASER, AmmoItemTrait.CON_ACCURACY2, AmmoItemTrait.CON_RANGE2, AmmoItemTrait.CON_HEAVY_WEAR, AmmoItemTrait.CON_LING_FIRE);
        AMMO_FUEL_GAS = ammo("ammo_fuel_gas", AmmoItemTrait.PRO_NO_GRAVITY, AmmoItemTrait.PRO_POISON_GAS, AmmoItemTrait.CON_NO_DAMAGE, AmmoItemTrait.CON_NO_FIRE);
        GUN_COILGUN_LEGACY = gun("gun_coilgun_legacy", p -> new ItemCoilgun(GunEnergyFactory.getCoilgunConfig(), p));
        GUN_FLAMER_LEGACY = gun("gun_flamer_legacy", p -> new ItemGunBase(GunEnergyFactory.getFlamerConfig(), p));
        GUN_CRYOCANNON = gun("gun_cryocannon", p -> new ItemCryoCannon(GunEnergyFactory.getCryoCannonConfig(), p));
        GUN_FIREEXT_LEGACY = gun("gun_fireext_legacy", p -> new ItemGunBase(GunEnergyFactory.getExtConfig(), p));
        GUN_ZOMG = gun("gun_zomg", p -> new ItemGunBase(GunEnergyFactory.getZOMGConfig(), p));
        GUN_EMP = gun("gun_emp", p -> new ItemGunBase(GunEnergyFactory.getEMPConfig(), p));
        GUN_EMP_AMMO = part("gun_emp_ammo");
        GUN_CRYOLATOR_AMMO = part("gun_cryolator_ammo");

        // one-off guns
        GUN_SUPER_SHOTGUN = item("gun_super_shotgun", p -> new com.hbm.items.special.ItemCustomLore(p.stacksTo(1)));
        GUN_MOIST_NUGGET = item("gun_moist_nugget", p -> new com.hbm.items.special.ItemCustomLore(p.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.6F).build())));
        GUN_CHEMTHROWER_LEGACY = gun("gun_chemthrower_legacy", ItemGunChemthrowerLegacy::new);
        GUN_REVOLVER_INVERTED = item("gun_revolver_inverted", ItemGunSpecial.Suicide::new);
        GUN_JACK_AMMO = part("gun_jack_ammo");
        GUN_JACK = item("gun_jack", ItemGunSpecial.Jack::new);
        GUN_EUTHANASIA_AMMO = part("gun_euthanasia_ammo");
        GUN_EUTHANASIA = item("gun_euthanasia", ItemGunSpecial.Euthanasia::new);
        GUN_TWIGUN_AMMO = part("gun_twigun_ammo");
        GUN_TWIGUN = item("gun_twigun", ItemGunSpecial.Euthanasia::new);
        GUN_DEFABRICATOR_AMMO = part("gun_defabricator_ammo");
        GUN_DEFABRICATOR = item("gun_defabricator", ItemGunSpecial.Defabricator::new);
        CLIP_JACK = clip("clip_jack", GUN_JACK_AMMO, 12);
        CLIP_EUTHANASIA = clip("clip_euthanasia", GUN_EUTHANASIA_AMMO, 32);
        CLIP_DEFABRICATOR = clip("clip_defabricator", GUN_DEFABRICATOR_AMMO, 50);

        // RocketHoming
        AMMO_STINGER_ROCKET = ammo("ammo_stinger_rocket");
        AMMO_STINGER_ROCKET_HE = ammo("ammo_stinger_rocket_he", AmmoItemTrait.PRO_RADIUS, AmmoItemTrait.CON_WEAR);
        AMMO_STINGER_ROCKET_INCENDIARY = ammo("ammo_stinger_rocket_incendiary", AmmoItemTrait.PRO_INCENDIARY, AmmoItemTrait.CON_WEAR);
        AMMO_STINGER_ROCKET_NUCLEAR = ammo("ammo_stinger_rocket_nuclear", AmmoItemTrait.PRO_NUCLEAR, AmmoItemTrait.CON_SUPER_WEAR);
        AMMO_STINGER_ROCKET_BONES = ammo("ammo_stinger_rocket_bones");
        GUN_STINGER_LEGACY = gun("gun_stinger_legacy", p -> new ItemGunBase(GunRocketHomingFactory.getStingerConfig(), p));
        GUN_SKYSTINGER = gun("gun_skystinger", p -> new ItemGunBase(GunRocketHomingFactory.getSkyStingerConfig(), p));
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

    private static <T extends Item> RegistryHandle<T> item(String name, Function<Item.Properties, T> factory) {
        return add(NeoForgeRegistrar.registerItem(name, factory, Item.Properties::new));
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
