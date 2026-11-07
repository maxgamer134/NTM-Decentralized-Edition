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

/** backport-fix: BF-161 main 1.0.27_X5000's bio revolver (items/weapon/ItemGunBio), the "Atlas". */
public class ItemGunBio extends ItemGunBase {

    public ItemGunBio(GunConfiguration config, Properties properties) {
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
                node.side += prev.x * accel + rand.nextGaussian() * waggle + side;
                node.lift += prev.y + 1.5D;
                node.forward += prev.z * accel + rand.nextGaussian() * waggle;
            }

            double alpha = (System.currentTimeMillis() - lastShot) / 2000D;
            alpha = (1 - alpha) * 0.5D;

            if (getIsReloading(stack)) alpha = 0;

            smokeNodes.add(new SmokeNode(alpha));
        }
    }

    @Override
    public @Nullable BusAnimation getAnimation(ItemStack stack, AnimType type) {

        if (type == AnimType.CYCLE) {
            lastShot = System.currentTimeMillis();
            return new BusAnimation()
                    .addBus(
                            "RECOIL",
                            new BusAnimationSequence()
                                    .addPos(0, 0, 0, 50)
                                    .addPos(0, 0, -3, 50)
                                    .addPos(0, 0, 0, 250))
                    .addBus(
                            "HAMMER",
                            new BusAnimationSequence()
                                    .addPos(0, 0, 1, 50)
                                    .addPos(0, 0, 1, 300)
                                    .addPos(0, 0, 0, 200))
                    .addBus("DRUM", new BusAnimationSequence().addPos(0, 0, 1, 50));
        }

        if (type == AnimType.RELOAD) {
            return new BusAnimation()
                    .addBus(
                            "LATCH",
                            new BusAnimationSequence()
                                    .addPos(0, 0, 90, 300)
                                    .addPos(0, 0, 90, 2000)
                                    .addPos(0, 0, 0, 150))
                    .addBus(
                            "FRONT",
                            new BusAnimationSequence()
                                    .addPos(0, 0, 0, 200)
                                    .addPos(0, 0, 45, 150)
                                    .addPos(0, 0, 45, 2000)
                                    .addPos(0, 0, 0, 75))
                    .addBus(
                            "RELOAD_ROT",
                            new BusAnimationSequence()
                                    .addPos(0, 0, 0, 300)
                                    .addPos(60, 0, 0, 500)
                                    .addPos(60, 0, 0, 500)
                                    .addPos(0, -90, -90, 0)
                                    .addPos(0, -90, -90, 600)
                                    .addPos(0, 0, 0, 300)
                                    .addPos(0, 0, 0, 100)
                                    .addPos(-45, 0, 0, 50)
                                    .addPos(-45, 0, 0, 100)
                                    .addPos(0, 0, 0, 300))
                    .addBus(
                            "RELOAD_MOVE",
                            new BusAnimationSequence()
                                    .addPos(0, 0, 0, 300)
                                    .addPos(0, -15, 0, 1000)
                                    .addPos(0, 0, 0, 450))
                    .addBus(
                            "DRUM_PUSH",
                            new BusAnimationSequence()
                                    .addPos(0, 0, 0, 1600)
                                    .addPos(0, 0, -5, 0)
                                    .addPos(0, 0, 0, 300));
        }

        return null;
    }
}
