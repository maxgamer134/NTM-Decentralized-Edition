// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.explosion.ExplosionChaos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * The Jar of Corrosive Fumes and the Jar of Pink Cloud, which NTMain dropped after X5490: they shatter on
 * impact and leave a poison cloud (type 1) or a pink cloud (type 2).
 */
public class ItemGrenadeCloud extends ItemGenericGrenade {

    private final int count;
    private final double speed;
    private final int type;

    public ItemGrenadeCloud(Properties properties, int count, double speed, int type) {
        super(properties, 10);
        this.count = count;
        this.speed = speed;
        this.type = type;
    }

    @Override
    public void explode(Entity grenade, LivingEntity thrower, Level level, double x, double y, double z) {
        if (level.isClientSide()) return;
        level.levelEvent(2002, grenade.blockPosition(), 0); // the splash potion shatter
        ExplosionChaos.spawnPoisonCloud(level, x, y, z, count, speed, type);
    }

    @Override
    public boolean explodesOnImpact() {
        return true;
    }
}
