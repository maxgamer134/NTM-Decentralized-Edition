// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.items.weapon.sedna.ItemGunBaseNT.SmokeNode;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationSequence;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** backport-fix: BF-161 main 1.0.27_X5000's ItemGunPip, the lil' mac: the bio revolver's smoke trail, a spin on equip and its own reload. */
public class ItemGunPip extends ItemGunBase {

    public ItemGunPip(GunConfiguration config, Properties properties) {
        super(config, properties);
    }

    /** client: when the last shot was fired, for the smoke */
    public static long lastShot;

    /** client: main's smoke trail, (x, y, z) are kept as SmokeNode (side, lift, forward) */
    public static final List<SmokeNode> smokeNodes = new ArrayList<>();

    /** client: main's updateClient, run once per client tick while the gun is drawn */
    public static void updateSmoke(Player entity, ItemStack stack, RandomSource rand) {
        boolean smoking = lastShot + 2000 > System.currentTimeMillis();

        if (!smoking && !smokeNodes.isEmpty()) smokeNodes.clear();

        if (smoking) {
            Vec3 motion = entity.getDeltaMovement();
            Vec3 prev =
                    new Vec3(-motion.x, -motion.y, -motion.z)
                            .yRot((float) (entity.getYRot() * Math.PI / 180D));
            double accel = 15D;
            double side = (entity.getYRot() - entity.yHeadRotO) * 0.1D;
            double waggle = 0.025D;

            for (SmokeNode node : smokeNodes) {
                // main's node[0] is forward here and node[2] the side, the width runs along node[2]
                node.forward += -prev.z * accel + rand.nextGaussian() * waggle;
                node.lift += prev.y + 1.5D;
                node.side += prev.x * accel + rand.nextGaussian() * waggle + side;
            }

            double alpha = (System.currentTimeMillis() - lastShot) / 2000D;
            alpha = (1 - alpha) * 0.5D;

            if (getIsReloading(stack)) alpha = 0;

            smokeNodes.add(new SmokeNode(alpha));
        }
    }

    @Override
    public @Nullable BusAnimation getAnimation(ItemStack stack, AnimType type) {
        
        if(type == AnimType.EQUIP) {
                return new BusAnimation()
                        .addBus("ROTATE", new BusAnimationSequence()
                                .addPos(-360, 0, 0, 350)
                                );
        }
        
        if(type == AnimType.CYCLE) {
            lastShot = System.currentTimeMillis();
            int s = 1;
            return new BusAnimation()
                .addBus("RECOIL", new BusAnimationSequence()
                        .addPos(0, 0, 0, 50 * s)
                        .addPos(0, 0, -3, 50 * s)
                        .addPos(0, 0, 0, 250 * s)
                        )
                .addBus("HAMMER", new BusAnimationSequence()
                        .addPos(0, 0, 1, 50 * s)
                        .addPos(0, 0, 1, 300 * s)
                        .addPos(0, 0, 0, 200 * s)
                        )
                .addBus("DRUM", new BusAnimationSequence()
                        .addPos(0, 0, 1, 50 * s)
                        );
         }
        
        if(type == AnimType.RELOAD) {
            int s = 1;
                return new BusAnimation()
                        .addBus("RELAOD_TILT", new BusAnimationSequence()
                                .addPos(-15, 0, 0, 100 * s)
                                .addPos(65, 0, 0, 100 * s) //200
                                .addPos(45, 0, 0, 50 * s) //250
                                .addPos(0, 0, 0, 200 * s) //450
                                .addPos(0, 0, 0, 1450 * s) //1900
                                .addPos(-80, 0, 0, 100 * s) //2000
                                .addPos(-80, 0, 0, 100 * s) //2100
                                .addPos(0, 0, 0, 200 * s) //2300
                                )
                        .addBus("RELOAD_CYLINDER", new BusAnimationSequence()
                                .addPos(0, 0, 0, 200 * s)
                                .addPos(90, 0, 0, 100 * s) //300
                                .addPos(90, 0, 0, 1700 * s) //2000
                                .addPos(0, 0, 0, 70 * s) //2100
                                )
                        .addBus("RELOAD_LIFT", new BusAnimationSequence()
                                .addPos(0, 0, 0, 350 * s)
                                .addPos(-45, 0, 0, 250 * s) //600
                                .addPos(-45, 0, 0, 350 * s) //950
                                .addPos(-15, 0, 0, 200 * s) //1150
                                .addPos(-15, 0, 0, 1050 * s) //2200
                                .addPos(0, 0, 0, 100 * s) //2300
                                )
                        .addBus("RELOAD_JOLT", new BusAnimationSequence()
                                .addPos(0, 0, 0, 600 * s)
                                .addPos(2, 0, 0, 50 * s) //650
                                .addPos(0, 0, 0, 100 * s) //750
                                )
                        .addBus("RELOAD_BULLETS", new BusAnimationSequence()
                                .addPos(0, 0, 0, 650 * s)
                                .addPos(10, 0, 0, 300 * s) //950
                                .addPos(10, 0, 0, 200 * s) //1150
                                .addPos(0, 0, 0, 700 * s) //1850
                                )
                        .addBus("RELOAD_BULLETS_CON", new BusAnimationSequence()
                                .addPos(1, 0, 0, 0 * s)
                                .addPos(1, 0, 0, 950 * s)
                                .addPos(0, 0, 0, 1 * s)
                                );
        }
        
        return null;
    }
}
