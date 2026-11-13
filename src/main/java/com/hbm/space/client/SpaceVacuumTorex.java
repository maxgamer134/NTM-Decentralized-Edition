// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.client.TorexClientFX;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.space.CelestialBody;
import com.hbm.space.trait.CBT_Atmosphere;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * NTM:Space: a nuke with no air around it makes no mushroom cloud, only a glowing ball that
 * flings its sparks outward and fades.
 */
public final class SpaceVacuumTorex implements EntityNukeTorex.Simulation {

    private static final EntityNukeTorex.TorexType STARFISH = EntityNukeTorex.TorexType.of(new EntityNukeTorex.Behavior() {
        @Override
        public void move(EntityNukeTorex.Cloudlet cloud) {
            EntityNukeTorex torex = cloud.torex();
            double factor = Mth.clamp((cloud.posY - torex.getY()) / torex.coreHeight, 0, 1);
            cloud.motionX = Math.cos(cloud.angle) * factor;
            cloud.motionY = 0;
            cloud.motionZ = -Math.sin(cloud.angle) * factor;
        }

        @Override
        public void colour(EntityNukeTorex.Cloudlet cloud) {
            EntityNukeTorex torex = cloud.torex();
            double dx = torex.getX() - cloud.posX;
            double dy = torex.getY() + torex.coreHeight - cloud.posY;
            double dz = torex.getZ() - cloud.posZ;
            double col = 2D / Math.max((dx * dx + dy * dy + dz * dz) / torex.heat, 1);
            cloud.colorR = Math.max(col * 8, 0.5);
            cloud.colorG = Math.max(col * 1.5, 0.1);
            cloud.colorB = Math.max(col * 0.5, 0.05);
        }
    });

    private SpaceVacuumTorex() {}

    public static void register() {
        EntityNukeTorex.registerSimulation(new SpaceVacuumTorex());
    }

    @Override
    public boolean claims(EntityNukeTorex torex) {
        if (CelestialBody.inOrbit(torex.level())) return true;
        CBT_Atmosphere atmosphere = CelestialBody.getTrait(torex.level(), CBT_Atmosphere.class);
        return atmosphere == null || atmosphere.getPressure() < 0.01;
    }

    @Override
    public int tick(EntityNukeTorex torex, int age, boolean live) {
        double s = torex.getScaleVal();
        RandomSource rand = torex.getRandom();
        if (live) TorexClientFX.hudFlash(torex);

        if (age < 40) {
            double range = (age + rand.nextDouble()) * 0.6;
            int lifetime = Math.max(800 - age * 10, 40);
            for (int i = 0; i < age * 5; i++) {
                EntityNukeTorex.Cloudlet cloud = torex.new Cloudlet(
                        torex.getX() + rand.nextGaussian() * range,
                        torex.getY() + rand.nextGaussian() * range,
                        torex.getZ() + rand.nextGaussian() * range,
                        (float) (rand.nextDouble() * 2D * Math.PI), 0, lifetime, STARFISH);
                cloud.setScale(4F, 5F);
                cloud.setMotion(age < 25 && rand.nextBoolean() ? 0.5 : 0.05);
                torex.cloudlets.add(cloud);
            }
        }
        if (live && age < 150) TorexClientFX.shockwaveArrival(torex);

        for (EntityNukeTorex.Cloudlet cloud : torex.cloudlets) cloud.update();
        torex.coreHeight += 0.15 / s;
        torex.torusWidth += 0.05 / s;
        torex.rollerSize = torex.torusWidth * 0.8;
        torex.convectionHeight = torex.coreHeight + torex.rollerSize;

        int maxHeat = (int) (50 * 1.5);
        torex.heat = maxHeat - (double) (maxHeat * age) / torex.maxAge;

        torex.cloudlets.removeIf(cloud -> cloud.isDead);
        return torex.cloudlets.size();
    }
}
