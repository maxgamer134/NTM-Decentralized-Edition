// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.google.gson.JsonElement;
import com.hbm.NuclearTech;
import com.hbm.blocks.bomb.BlockChargeDynamite;
import com.hbm.blocks.bomb.BlockChargeMiner;
import com.hbm.blocks.bomb.BlockDetMiner;
import com.hbm.config.BalanceConfig;
import com.hbm.interfaces.IBomb;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * backport: the gun content switches (disable_all_guns, disable_post_rework_guns,
 * disable_pre_rework_guns, disable_war_explosives and its four parts in the content config). A disabled item stays registered, so worlds and
 * servers keep loading, but it leaves the creative tabs and recipe viewers, every recipe naming it
 * is dropped, and a disabled gun does not fire.
 */
public final class GunContent {

    /** main's current guns and what only they use: parts, casings, ammo, mods and the ammo press. */
    private static final Pattern POST_REWORK =
            Pattern.compile(
                    "gun_b92(_ammo)?|ammo_(standard|secret)_.+|ammo_bag(_infinite)?|casing_.+|"
                            + "weapon_mod_.+|part_(barrel|receiver|mechanism|stock|grip)_.+|machine_ammo_press");

    /** War explosives the block/item classes below don't catch: kits, bomb and grenade parts, custom nuke charges, launch codes. */
    private static final Pattern WAR =
            Pattern.compile(
                    "nuke_.+_kit|tsar_(kit|core)|missile_(kit|assembly)|machine_missile_assembly|"
                            + "grenade_(filling|fuze|shell|extra)_.+|bomb_caller(_.+)?|launch_(code|code_piece|key)|"
                            + "gadget_(core|explosive|wireing)|boy_(bullet|igniter|propellant|shielding|target)|"
                            + "man_(core|explosive|igniter)|mike_(cooling_unit|core|deut)|"
                            + "(fleija|solinium)_(core|igniter|propellant)|n2_charge|"
                            + "(boy|custom|fleija|gadget|man|mike|multi|prototype|solinium)_kit|"
                            + "custom_(amat|dirty|fall|hydro|nuke|schrab|tnt)");

    /**
     * Tools that only look like guns: the Charge Thrower (grappling hook, demolition charges) and the
     * fire extinguishers, their ammo, and the parts and ammo press the Charge Thrower is made with.
     * The gun switches leave them alone.
     */
    private static final Pattern UTILITY =
            Pattern.compile(
                    "gun_charge_thrower|gun_fireext(_legacy)?|ammo_fireext(_foam|_sand)?(_legacy)?|"
                            + "ammo_standard_ct_(hook|mortar|mortar_charge)|"
                            + "part_barrel_heavy_steel|part_grip_steel|part_mechanism_gun_metal|machine_ammo_press");

    /** Not a weapon, so the gun switches (enable_guns and the disable_*_guns ones) leave it alone. */
    public static boolean isUtility(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals(NuclearTech.MOD_ID) && UTILITY.matcher(id.getPath()).matches();
    }

    /** Rockets for space, not war. */
    private static final Pattern SPACE = Pattern.compile("missile_(soyuz.*|shuttle)");

    /** Items from main's guns before the rework; a port of them adds its items here. */
    private static final Set<ResourceLocation> PRE_REWORK = new HashSet<>();

    private static volatile Set<Item> disabled;

    private GunContent() {}

    public static synchronized void markPreRework(ResourceLocation item) {
        PRE_REWORK.add(item);
        disabled = null;
    }

    /** Called when the content config is (re)loaded. */
    public static void invalidate() {
        disabled = null;
    }

    /** switched off: hidden, and guns, launchers and bombs refuse to work. A duplicate is only hidden. */
    public static boolean isDisabled(Item item) {
        return disabled().contains(item) && !hiddenOnly.contains(item);
    }

    /** gone from the creative tabs, the recipe viewers and recipes */
    public static boolean isHidden(Item item) {
        return disabled().contains(item);
    }

    public static boolean isHidden(ItemStack stack) {
        return !stack.isEmpty() && isHidden(stack.getItem());
    }

    /**
     * Pre-rework guns with a post-rework twin of the same gun. While the post-rework guns are on, the old copy is hidden
     * (copies people own still work).
     */
    private static final Set<String> DUPLICATES = Set.of(
            "gun_bio_revolver", "gun_revolver_nopip", "gun_spas12_legacy", "gun_uzi_legacy", "gun_minigun_legacy",
            "gun_bolter_legacy", "gun_panzerschreck_legacy", "gun_quadro_legacy", "gun_congolake_legacy",
            "gun_fatman_legacy", "gun_coilgun_legacy", "gun_flamer_legacy", "gun_chemthrower_legacy",
            "gun_stinger_legacy", "gun_fireext_legacy", "gun_xvl1456");

    /** the duplicates hidden by the last compute(), which still work */
    private static Set<Item> hiddenOnly = Set.of();

    public static boolean isDisabled(ItemStack stack) {
        return !stack.isEmpty() && isDisabled(stack.getItem());
    }

    public static Set<Item> disabled() {
        Set<Item> set = disabled;
        if (set == null) disabled = set = compute();
        return set;
    }

    private static Set<Item> compute() {
        boolean post = BalanceConfig.disableAllGuns || BalanceConfig.disablePostReworkGuns;
        boolean pre = BalanceConfig.disableAllGuns || BalanceConfig.disablePreReworkGuns;
        boolean all = BalanceConfig.disableWarExplosives;
        boolean[] war = {all || BalanceConfig.disableWarMissiles, all || BalanceConfig.disableWarNukes,
                all || BalanceConfig.disableWarBombs, all || BalanceConfig.disableWarMisc};
        boolean conv = BalanceConfig.preReworkGunsUseNewAmmo;
        Set<Item> out = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Item> dup = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Map.Entry<net.minecraft.resources.ResourceKey<Item>, Item> e :
                BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation id = e.getKey().location();
            if (id.getNamespace().equals(NuclearTech.MOD_ID) && id.getPath().startsWith("turret_")) {
                if (turretOff(id.getPath(), post)) out.add(e.getValue());
                continue;
            }
            if (id.getNamespace().equals(NuclearTech.MOD_ID) && UTILITY.matcher(id.getPath()).matches()) {
                // the gun switches leave tools alone; the old extinguisher still gives way to the new one
                if (DUPLICATES.contains(id.getPath())) {
                    out.add(e.getValue());
                    dup.add(e.getValue());
                }
                continue;
            }
            if (pre && PRE_REWORK.contains(id)) out.add(e.getValue());
            else if (post
                    && (e.getValue() instanceof ItemGunBaseNT
                            || (id.getNamespace().equals(NuclearTech.MOD_ID)
                                    && POST_REWORK.matcher(id.getPath()).matches())))
                out.add(e.getValue());
            else if (warOff(war, war(id, e.getValue()))) out.add(e.getValue());
            else if (!post && id.getNamespace().equals(NuclearTech.MOD_ID) && DUPLICATES.contains(id.getPath())) {
                out.add(e.getValue());
                dup.add(e.getValue());
            }
            // pre_rework_guns_use_new_ammo: an old round with a post-rework twin gives way to it
            else if (conv
                    && id.getNamespace().equals(NuclearTech.MOD_ID)
                    && com.hbm.items.weapon.legacy.LegacyAmmo.isConverted(id.getPath()))
                out.add(e.getValue());
        }
        hiddenOnly = dup;
        return Collections.unmodifiableSet(out);
    }

    /** Missiles, bombs, nukes, launch pads, grenades, landmines and combat charges; not mining ones. */
    /**
     * The gun switches take the turrets that need pressed ammo. The five heavy turrets (Jeremy, Maxwell, Fritz, Greg,
     * Henry) have their own switch, and Henry and Greg one each. The chip goes once every turret is off.
     */
    private static boolean turretOff(String path, boolean guns) {
        boolean heavy = BalanceConfig.disableHeavyTurrets;
        return switch (path) {
            case "turret_himars" -> heavy || BalanceConfig.disableTurretHenry;
            case "turret_arty" -> heavy || BalanceConfig.disableTurretGreg;
            case "turret_jeremy", "turret_maxwell", "turret_fritz" -> heavy;
            case "turret_chip" -> guns && heavy;
            default -> guns;
        };
    }

    /** Which war switch takes an item: 0 missiles, 1 nukes, 2 conventional bombs, 3 the rest, -1 none. */
    private static final int NONE = -1, MISSILES = 0, NUKES = 1, BOMBS = 2, MISC = 3;

    private static final Pattern WAR_MISSILE =
            Pattern.compile("missile_(kit|assembly)|machine_missile_assembly|launch_(code|code_piece|key)");
    private static final Pattern WAR_MISC = Pattern.compile("grenade_(filling|fuze|shell|extra)_.+|bomb_caller(_.+)?");

    private static boolean warOff(boolean[] off, int kind) {
        return kind != NONE && off[kind];
    }

    private static int war(ResourceLocation id, Item item) {
        if (!id.getNamespace().equals(NuclearTech.MOD_ID) || SPACE.matcher(id.getPath()).matches()) {
            return NONE;
        }
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            // NTM:Space's bombs (the antimatter nuke, salted det cord) live in its own package
            String pkg = block.getClass().getPackageName();
            if (!(block instanceof IBomb)
                    || !(pkg.equals("com.hbm.blocks.bomb") || pkg.equals("com.hbm.space"))
                    || block instanceof BlockChargeDynamite
                    || block instanceof BlockChargeMiner
                    || block instanceof BlockDetMiner) return NONE;
            if (block instanceof com.hbm.blocks.bomb.LaunchPad
                    || block instanceof com.hbm.blocks.bomb.LaunchPadLarge
                    || block instanceof com.hbm.blocks.bomb.LaunchPadRusted
                    || block instanceof com.hbm.blocks.bomb.LaunchTable
                    || block instanceof com.hbm.blocks.bomb.CompactLauncher) return MISSILES;
            String name = block.getClass().getName();
            if (name.contains("Nuke") || name.contains("Balefire") || name.contains("CrashedBomb")
                    || name.contains("Antimatter")) return NUKES;
            return BOMBS;
        }
        if (item instanceof ItemMissile || item instanceof ItemCustomMissile || item instanceof ItemCustomMissilePart)
            return MISSILES;
        if (item instanceof ItemGrenadeUniversal
                || (item instanceof ItemGrenade && !(item instanceof ItemGrenadeDynamite || item instanceof ItemGrenadeFishing)))
            return MISC;
        String path = id.getPath();
        if (!WAR.matcher(path).matches()) return NONE;
        if (WAR_MISSILE.matcher(path).matches()) return MISSILES;
        if (WAR_MISC.matcher(path).matches()) return MISC;
        if (path.equals("multi_kit")) return BOMBS;
        return NUKES; // the nuke kits, cores, parts and custom nuke charges
    }

    private static boolean secretPartOn(String part) {
        if (BalanceConfig.enableSecretPartRecipes) return true;
        return switch (part) {
            case "selenium_steel" -> BalanceConfig.enableSecretSeleniumSteelRecipe;
            case "canister" -> BalanceConfig.enableSecretCanisterRecipe;
            case "controller" -> BalanceConfig.enableSecretControllerRecipe;
            case "folly" -> BalanceConfig.enableSecretFollyRecipe;
            case "aberrator" -> BalanceConfig.enableSecretAberratorRecipe;
            default -> false;
        };
    }

    /** Ids of the recipes whose json names a disabled item anywhere (output, input, catalyst). */
    public static Set<ResourceLocation> recipesToDrop(Map<ResourceLocation, JsonElement> recipes) {
        Set<ResourceLocation> drop = new HashSet<>();
        // the Pedestal and the secret gun parts are only craftable with their switches on
        recipes.keySet().forEach(id -> {
            if (!id.getNamespace().equals(NuclearTech.MOD_ID)) return;
            String path = id.getPath();
            if (!BalanceConfig.enablePedestalRecipe && path.equals("pedestal_block")) drop.add(id);
            if (path.startsWith("assembly_machine/ass.item_secret_")
                    && !secretPartOn(path.substring("assembly_machine/ass.item_secret_".length()))) drop.add(id);
            if (path.equals("assembly_machine/ass.secret_augment.v1")) {
                if (!BalanceConfig.enableV1Recipe
                        || !com.hbm.config.EarlyContentConfig.get(com.hbm.config.ConfigSchema.ENABLE_V1)) drop.add(id);
            } else if (!BalanceConfig.enableSecretAugmentRecipes && path.startsWith("assembly_machine/ass.secret_augment.")) drop.add(id);
            if (!BalanceConfig.enableRedRoomKeyholeRecipe && path.equals("stone_keyhole_meta")) drop.add(id);
            // each secret blueprint is craftable while anything in its pool is
            if (path.equals("assembly_machine/ass.secret_blueprint.parts")
                    && !java.util.stream.Stream.of("selenium_steel", "canister", "controller", "folly", "aberrator")
                            .anyMatch(GunContent::secretPartOn)) drop.add(id);
            if (path.equals("assembly_machine/ass.secret_blueprint.augments")
                    && !BalanceConfig.enableSecretAugmentRecipes
                    && !(BalanceConfig.enableV1Recipe
                            && com.hbm.config.EarlyContentConfig.get(com.hbm.config.ConfigSchema.ENABLE_V1))) drop.add(id);
        });
        Set<Item> items = disabled();
        if (items.isEmpty()) return drop;
        Set<String> ids = new HashSet<>();
        for (Item item : items) ids.add(BuiltInRegistries.ITEM.getKey(item).toString());
        recipes.forEach((id, json) -> {
            if (names(json, ids)) drop.add(id);
        });
        return drop;
    }

    private static boolean names(JsonElement json, Set<String> ids) {
        if (json.isJsonPrimitive()) {
            return json.getAsJsonPrimitive().isString() && ids.contains(json.getAsString());
        }
        if (json.isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray()) if (names(e, ids)) return true;
        } else if (json.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject().entrySet())
                if (names(e.getValue(), ids)) return true;
        }
        return false;
    }
}
