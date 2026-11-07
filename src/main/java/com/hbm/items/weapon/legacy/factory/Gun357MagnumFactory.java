// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy.factory;

import com.hbm.items.weapon.legacy.AnimType;
import com.hbm.items.weapon.legacy.BulletConfigSyncingUtil;
import com.hbm.items.weapon.legacy.BulletConfiguration;
import com.hbm.items.weapon.legacy.EnumGunManufacturer;
import com.hbm.items.weapon.legacy.GunConfiguration;
import com.hbm.items.weapon.legacy.LegacyGuns;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.SpentCasing.CasingType;
import com.hbm.potion.HbmPotion;
import com.hbm.render.item.weapon.legacy.LegacyResources;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/Gun357MagnumFactory, value for value. */
public final class Gun357MagnumFactory {

    private static final GunConfiguration.Ejector EJECTOR_REVOLVER =
            new GunConfiguration.Ejector()
                    .setMotion(0, 0, -0.03)
                    .setOffset(0, -0.15, 0)
                    .setAngleRange(0.01F, 0.05F)
                    .setAfterReload()
                    .setAmount(6);

    // backport: casing names carry "old" so they don't replace the post-rework .357 casings
    private static final SpentCasing CASING357 =
            new SpentCasing(CasingType.STRAIGHT).setBounceMotion(0.01F, 0.05F);
    private static final SpentCasing CASINGNM =
            new SpentCasing(CasingType.SHOTGUN)
                    .setScale(1.25F)
                    .setBounceMotion(0.01F, 0.05F)
                    .setColor(0xC7AB1C, 0x6D63A6)
                    .register("old357N2");

    private Gun357MagnumFactory() {}

    public static GunConfiguration getBaseConfig() {
        GunConfiguration config = new GunConfiguration();
        config.rateOfFire = 10;
        config.roundsPerCycle = 1;
        config.gunMode = GunConfiguration.MODE_NORMAL;
        config.firingMode = GunConfiguration.FIRE_MANUAL;
        config.reloadDuration = 30;
        config.firingDuration = 0;
        config.ammoCap = 6;
        config.reloadType = GunConfiguration.RELOAD_FULL;
        config.allowsInfinity = true;
        config.crosshair = Crosshair.L_CLASSIC;
        config.reloadSound = GunConfiguration.RSOUND_REVOLVER;
        config.firingSound = "hbm:weapon.revolverShoot";
        config.reloadSoundEnd = false;
        config.ejector = EJECTOR_REVOLVER;
        config.loadAnimations =
                () -> {
                    config.animations.put(AnimType.CYCLE, LegacyResources.python_anim.get("Fire"));
                    config.animations.put(
                            AnimType.RELOAD, LegacyResources.python_anim.get("Reload"));
                };
        return config;
    }

    private static GunConfiguration revolver(
            int durability, String name, EnumGunManufacturer maker, int... configs) {
        GunConfiguration config = getBaseConfig();
        config.durability = durability;
        config.name = name;
        config.manufacturer = maker;
        config.config = new ArrayList<>();
        for (int c : configs) config.config.add(c);
        return config;
    }

    public static GunConfiguration getRevolverConfig() {
        return revolver(
                3500,
                "ffiVInox",
                EnumGunManufacturer.FLIMFLAM,
                BulletConfigSyncingUtil.STEEL_REVOLVER,
                BulletConfigSyncingUtil.IRON_REVOLVER,
                BulletConfigSyncingUtil.LEAD_REVOLVER,
                BulletConfigSyncingUtil.DESH_REVOLVER);
    }

    public static GunConfiguration getRevolverSaturniteConfig() {
        return revolver(
                3500,
                "ffivSatur",
                EnumGunManufacturer.FLIMFLAM,
                BulletConfigSyncingUtil.SATURNITE_REVOLVER,
                BulletConfigSyncingUtil.DESH_REVOLVER);
    }

    public static GunConfiguration getRevolverGoldConfig() {
        return revolver(
                2500,
                "ffivBling",
                EnumGunManufacturer.FLIMFLAM,
                BulletConfigSyncingUtil.GOLD_REVOLVER,
                BulletConfigSyncingUtil.STEEL_REVOLVER,
                BulletConfigSyncingUtil.IRON_REVOLVER,
                BulletConfigSyncingUtil.LEAD_REVOLVER,
                BulletConfigSyncingUtil.DESH_REVOLVER);
    }

    public static GunConfiguration getRevolverCursedConfig() {
        GunConfiguration config =
                revolver(
                        5000,
                        "revolverCursed",
                        EnumGunManufacturer.BAE,
                        BulletConfigSyncingUtil.CURSED_REVOLVER,
                        BulletConfigSyncingUtil.DESH_REVOLVER);
        config.rateOfFire = 7;
        config.ammoCap = 17;
        config.firingSound = "hbm:weapon.heavyShoot";
        config.loadAnimations =
                () -> {
                    config.animations.put(AnimType.CYCLE, LegacyResources.cursed_anim.get("Fire"));
                    config.animations.put(
                            AnimType.RELOAD, LegacyResources.cursed_anim.get("Reload"));
                };
        return config;
    }

    public static GunConfiguration getRevolverSchrabidiumConfig() {
        GunConfiguration config =
                revolver(
                        7500,
                        "ffiVUltra",
                        EnumGunManufacturer.FLIMFLAM,
                        BulletConfigSyncingUtil.SCHRABIDIUM_REVOLVER,
                        BulletConfigSyncingUtil.GOLD_REVOLVER,
                        BulletConfigSyncingUtil.STEEL_REVOLVER,
                        BulletConfigSyncingUtil.IRON_REVOLVER,
                        BulletConfigSyncingUtil.LEAD_REVOLVER,
                        BulletConfigSyncingUtil.DESH_REVOLVER);
        config.firingSound = "hbm:weapon.schrabidiumShoot";
        return config;
    }

    public static GunConfiguration getRevolverNightmareConfig() {
        GunConfiguration config =
                revolver(
                        4000,
                        "ffiVN1",
                        EnumGunManufacturer.FLIMFLAM,
                        BulletConfigSyncingUtil.NIGHT_REVOLVER,
                        BulletConfigSyncingUtil.DESH_REVOLVER);
        config.firingSound = "hbm:weapon.schrabidiumShoot";
        return config;
    }

    public static GunConfiguration getRevolverNightmare2Config() {
        GunConfiguration config =
                revolver(
                        4000,
                        "ffiVN2",
                        EnumGunManufacturer.FLIMFLAM,
                        BulletConfigSyncingUtil.NIGHT2_REVOLVER);
        config.firingSound = "hbm:weapon.schrabidiumShoot";
        config.crosshair = Crosshair.NONE;
        return config;
    }

    public static GunConfiguration getRevolverBioConfig() {
        GunConfiguration config =
                revolver(
                        100000,
                        "bio",
                        EnumGunManufacturer.RYAN,
                        BulletConfigSyncingUtil.STEEL_HS,
                        BulletConfigSyncingUtil.GOLD_HS,
                        BulletConfigSyncingUtil.IRON_HS,
                        BulletConfigSyncingUtil.LEAD_HS,
                        BulletConfigSyncingUtil.DESH_HS);
        config.firingSound = "hbm:weapon.deagleShoot";
        config.reloadDuration = 53;
        config.crosshair = Crosshair.CIRCLE;
        // main's bio revolver plays its own animations (ItemGunBio), not python_anim
        config.loadAnimations = null;
        return config;
    }

    private static BulletConfiguration pistol(
            java.util.function.Supplier<? extends net.minecraft.world.item.Item> ammo,
            float min,
            float max,
            String casing,
            int color) {
        BulletConfiguration bullet = BulletConfigFactory.standardPistolConfig();
        bullet.ammo = ammo;
        bullet.dmgMin = min;
        bullet.dmgMax = max;
        bullet.spentCasing = CASING357.clone().register(casing).setColor(color);
        return bullet;
    }

    public static BulletConfiguration getRevIronConfig() {
        return pistol(LegacyGuns.AMMO_357_IRON, 8, 10, "old357Iron", 0xA8A8A8);
    }

    public static BulletConfiguration getRevLeadConfig() {
        return pistol(LegacyGuns.AMMO_357_LEAD, 18, 22, "old357Lead", 0x646470);
    }

    public static BulletConfiguration getRevNuclearConfig() {
        BulletConfiguration bullet =
                pistol(LegacyGuns.AMMO_357_NUCLEAR, 10, 15, "old357Nuc", 0xFEFEFE);
        bullet.effects =
                List.of(
                        new MobEffectInstance(
                                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HbmPotion.radiation.get()),
                                10 * 20,
                                4));
        return bullet;
    }

    public static BulletConfiguration getRevGoldConfig() {
        return pistol(LegacyGuns.AMMO_357_GOLD, 25, 28, "old357Gold", 0xF9FF3E);
    }

    public static BulletConfiguration getRevDeshConfig() {
        return pistol(LegacyGuns.AMMO_357_DESH, 30, 33, "old357Desh", 0xF22929);
    }

    public static BulletConfiguration getRevSchrabidiumConfig() {
        BulletConfiguration bullet =
                pistol(LegacyGuns.AMMO_357_SCHRABIDIUM, 10000, 100000, "old357Schrab", 0x32FFFF);
        bullet.instakill = true;
        return bullet;
    }

    public static BulletConfiguration getRevCursedConfig() {
        return pistol(LegacyGuns.AMMO_357_STEEL, 18, 25, "old357Cursed", 0x565656);
    }

    public static BulletConfiguration getRevNightmare1Config() {
        return pistol(LegacyGuns.AMMO_357_NIGHTMARE1, 1, 100, "old357N1", 0x3A3A3A);
    }

    public static BulletConfiguration getRevNightmare2Config() {
        BulletConfiguration bullet = BulletConfigFactory.standardBulletConfig();
        bullet.ammo = LegacyGuns.AMMO_357_NIGHTMARE2;
        bullet.spread *= 10;
        bullet.bulletsMin = 4;
        bullet.bulletsMax = 6;
        bullet.dmgMin = 25;
        bullet.dmgMax = 100;
        bullet.doesRicochet = false;
        bullet.destroysBlocks = true;
        bullet.style = BulletConfiguration.STYLE_BOLT;
        bullet.trail = BulletConfiguration.BOLT_NIGHTMARE;
        bullet.damageType = ModDamageTypes.LASER;
        bullet.spentCasing = CASINGNM;
        return bullet;
    }
}
