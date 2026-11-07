// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** backport-fix: BF-161 main 1.0.27_X5000's ItemCoilgun: its own recoil and reload animations */
public class ItemCoilgun extends ItemGunBase {

    public ItemCoilgun(GunConfiguration config, Properties properties) {
        super(config, properties);
    }

    @Override
    public @Nullable BusAnimation getAnimation(ItemStack stack, AnimType type) {
        if (type == AnimType.CYCLE) {
            return new BusAnimation()
                    .addBus("RECOIL", new BusAnimationSequence()
                            .addPos(1, 0, 0, 100)
                            .addPos(0, 0, 0, 200));
        }
        if (type == AnimType.RELOAD) {
            return new BusAnimation()
                    .addBus("RELOAD", new BusAnimationSequence()
                            .addPos(1, 0, 0, 250)
                            .addPos(1, 0, 0, 500)
                            .addPos(0, 0, 0, 250));
        }
        return mainConfig.animations.get(type);
    }
}
