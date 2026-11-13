// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public final class BalanceConfig {

    public static boolean enable528 = false;
    public static boolean disableAllGuns = false;
    public static boolean disablePostReworkGuns = false;
    public static boolean disablePreReworkGuns = true;
    public static boolean disableWarExplosives = false;
    public static boolean disableWarMissiles = false;
    public static boolean disableWarNukes = false;
    public static boolean disableWarBombs = false;
    public static boolean disableWarMisc = false;
    public static boolean enablePedestalRecipe = false;
    public static boolean enableSecretPartRecipes = false;
    public static boolean enableRedRoomKeyholeRecipe = false;
    public static boolean enableSecretAugmentRecipes = false;
    public static boolean enableV1Recipe = true;
    public static boolean enableSecretSeleniumSteelRecipe = false;
    public static boolean enableSecretCanisterRecipe = false;
    public static boolean enableSecretControllerRecipe = false;
    public static boolean enableSecretFollyRecipe = false;
    public static boolean enableSecretAberratorRecipe = false;
    public static boolean disableHeavyTurrets = false;
    public static boolean disableTurretHenry = false;
    public static boolean disableTurretGreg = false;
    public static boolean preReworkGunsUseNewAmmo = true;
    public static boolean enable528ReasimBoilers = false;
    public static boolean enable528PressurizedRecipes = false;
    public static boolean enable528MachineGravity = false;
    public static boolean enable528BosniaSimulator = false;

    public static boolean enableLBSM = false;
    public static boolean enableLBSMFullSchrab = false;
    public static boolean enableLBSMShorterDecay = false;
    public static boolean enableLBSMSimpleArmorRecipes = false;
    public static boolean enableLBSMSimpleToolRecipes = false;
    public static boolean enableLBSMSimpleCrafting = false;
    public static boolean enableLBSMSimpleChemistry = false;
    public static boolean enableLBSMSimpleCentrifuge = false;
    public static boolean enableLBSMUnlockAnvil = false;
    public static boolean enableLBSMSimpleMedicineRecipes = false;
    public static boolean enableLBSMSafeCrates = false;
    public static boolean enableLBSMSafeMEDrives = false;

    public static boolean enableExpensiveMode = false;

    private BalanceConfig() {}

    public static void loadFrom(ConfigStore c) {
        enable528 = c.get(ConfigSchema.ENABLE_528);
        enable528ReasimBoilers = enable528 && c.get(ConfigSchema.ENABLE_528_FORCE_REASIM_BOILERS);
        enable528PressurizedRecipes =
                enable528 && c.get(ConfigSchema.ENABLE_528_PRESSURIZED_RECIPES);
        enable528MachineGravity = enable528 && c.get(ConfigSchema.ENABLE_528_MACHINE_GRAVITY);
        enable528BosniaSimulator = enable528 && c.get(ConfigSchema.ENABLE_528_BOSNIA_SIMULATOR);

        enableLBSM = !enable528 && c.get(ConfigSchema.ENABLE_LBSM);
        enableLBSMFullSchrab = c.get(ConfigSchema.ENABLE_LBSM_FULL_SCHRAB);
        enableLBSMShorterDecay = c.get(ConfigSchema.ENABLE_LBSM_SHORTER_DECAY);
        enableLBSMSimpleArmorRecipes = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_ARMOR_RECIPES);
        enableLBSMSimpleToolRecipes = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_TOOL_RECIPES);
        enableLBSMSimpleCrafting = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_CRAFTING);
        disableAllGuns = c.get(ConfigSchema.DISABLE_ALL_GUNS);
        disablePostReworkGuns = c.get(ConfigSchema.DISABLE_POST_REWORK_GUNS);
        disablePreReworkGuns = c.get(ConfigSchema.DISABLE_PRE_REWORK_GUNS);
        disableWarExplosives = c.get(ConfigSchema.DISABLE_WAR_EXPLOSIVES);
        disableWarMissiles = c.get(ConfigSchema.DISABLE_WAR_MISSILES);
        disableWarNukes = c.get(ConfigSchema.DISABLE_WAR_NUKES);
        disableWarBombs = c.get(ConfigSchema.DISABLE_WAR_BOMBS);
        disableWarMisc = c.get(ConfigSchema.DISABLE_WAR_MISC);
        enablePedestalRecipe = c.get(ConfigSchema.ENABLE_PEDESTAL_RECIPE);
        enableSecretPartRecipes = c.get(ConfigSchema.ENABLE_SECRET_PART_RECIPES);
        enableRedRoomKeyholeRecipe = c.get(ConfigSchema.ENABLE_RED_ROOM_KEYHOLE_RECIPE);
        enableSecretAugmentRecipes = c.get(ConfigSchema.ENABLE_SECRET_AUGMENT_RECIPES);
        enableV1Recipe = c.get(ConfigSchema.ENABLE_V1_RECIPE);
        enableSecretSeleniumSteelRecipe = c.get(ConfigSchema.ENABLE_SECRET_SELENIUM_STEEL_RECIPE);
        enableSecretCanisterRecipe = c.get(ConfigSchema.ENABLE_SECRET_CANISTER_RECIPE);
        enableSecretControllerRecipe = c.get(ConfigSchema.ENABLE_SECRET_CONTROLLER_RECIPE);
        enableSecretFollyRecipe = c.get(ConfigSchema.ENABLE_SECRET_FOLLY_RECIPE);
        enableSecretAberratorRecipe = c.get(ConfigSchema.ENABLE_SECRET_ABERRATOR_RECIPE);
        disableHeavyTurrets = c.get(ConfigSchema.DISABLE_HEAVY_TURRETS);
        disableTurretHenry = c.get(ConfigSchema.DISABLE_TURRET_HENRY);
        disableTurretGreg = c.get(ConfigSchema.DISABLE_TURRET_GREG);
        preReworkGunsUseNewAmmo = c.get(ConfigSchema.PRE_REWORK_GUNS_USE_NEW_AMMO);
        com.hbm.items.weapon.GunContent.invalidate();
        enableLBSMSimpleChemistry = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_CHEMISTRY);
        enableLBSMSimpleCentrifuge = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_CENTRIFUGE);
        enableLBSMUnlockAnvil = c.get(ConfigSchema.ENABLE_LBSM_UNLOCK_ANVIL);
        enableLBSMSimpleMedicineRecipes = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_MEDICINE);
        enableLBSMSafeCrates = c.get(ConfigSchema.ENABLE_LBSM_SAFE_CRATES);
        enableLBSMSafeMEDrives = c.get(ConfigSchema.ENABLE_LBSM_SAFE_ME_DRIVES);

        enableExpensiveMode = c.get(ConfigSchema.ENABLE_EXPENSIVE_MODE);
    }
}
