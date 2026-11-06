// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy.factory;

import com.hbm.items.weapon.legacy.BulletConfiguration;

/** backport-fix: BF-161 main 1.0.27_X5000's handler/guncfg/BulletConfigFactory: the base bullets. */
public final class BulletConfigFactory {

    public static final float defaultSpread = 0.005F;

    private BulletConfigFactory() {}

    public static BulletConfiguration standardBulletConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 5.0F;
        bullet.spread = defaultSpread;
        bullet.wear = 10;
        bullet.bulletsMin = 1;
        bullet.bulletsMax = 1;
        bullet.gravity = 0D;
        bullet.maxAge = 100;
        bullet.doesRicochet = true;
        bullet.ricochetAngle = 5;
        bullet.HBRC = 2;
        bullet.LBRC = 95;
        bullet.bounceMod = 0.8;
        bullet.doesPenetrate = true;
        bullet.doesBreakGlass = true;
        bullet.destroysBlocks = false;
        bullet.style = BulletConfiguration.STYLE_NORMAL;
        bullet.plink = BulletConfiguration.PLINK_BULLET;
        bullet.leadChance = 5;
        return bullet;
    }

    public static BulletConfiguration standardPistolConfig() {
        BulletConfiguration bullet = standardBulletConfig();
        bullet.style = BulletConfiguration.STYLE_PISTOL;
        bullet.plink = BulletConfiguration.PLINK_BULLET;
        return bullet;
    }

    public static BulletConfiguration standardBuckshotConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 5.0F;
        bullet.spread = defaultSpread * 10F;
        bullet.wear = 10;
        bullet.bulletsMin = 6;
        bullet.bulletsMax = 8;
        bullet.gravity = 0D;
        bullet.maxAge = 100;
        bullet.doesRicochet = true;
        bullet.ricochetAngle = 5;
        bullet.HBRC = 10;
        bullet.LBRC = 95;
        bullet.bounceMod = 0.8;
        bullet.doesPenetrate = false;
        bullet.doesBreakGlass = true;
        bullet.style = BulletConfiguration.STYLE_PELLET;
        bullet.plink = BulletConfiguration.PLINK_BULLET;
        bullet.leadChance = 10;
        return bullet;
    }
}
