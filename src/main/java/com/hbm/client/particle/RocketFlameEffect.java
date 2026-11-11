// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.particle.RocketFlameColor;
import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.world.level.Level;

public final class RocketFlameEffect implements Effect {

    private static final double DRAG = 0.91D;

    final double spawnX, spawnY, spawnZ;
    final float baseScale;
    final int maxAge;
    final RocketFlameColor color;
    private final Level level;
    private double x, y, z;
    private double prevX, prevY, prevZ;
    private double vx, vy, vz;
    int age;

    public RocketFlameEffect(
            Level level,
            double x,
            double y,
            double z,
            float baseScale,
            double vx,
            double vy,
            double vz,
            int maxAge,
            RocketFlameColor color) {
        this.level = level;
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        this.x = this.prevX = x;
        this.y = this.prevY = y;
        this.z = this.prevZ = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.baseScale = baseScale;
        this.maxAge = maxAge;
        this.color = color;
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
        return new RocketFlameVisual(ctx, this);
    }

    void tick() {
        prevX = x;
        prevY = y;
        prevZ = z;

        age++;
        if (expired()) return;

        vx *= DRAG;
        vy *= DRAG;
        vz *= DRAG;

        // NTMain: ParticleRocketFlame moves with moveEntity, so its 0.2 box stops at blocks and
        // the axis it hits loses its speed (launch flames spread over the pad, not through it)
        net.minecraft.world.phys.Vec3 wanted = new net.minecraft.world.phys.Vec3(vx, vy, vz);
        net.minecraft.world.phys.Vec3 moved =
                net.minecraft.world.entity.Entity.collideBoundingBox(
                        null,
                        wanted,
                        new net.minecraft.world.phys.AABB(
                                x - 0.1, y - 0.1, z - 0.1, x + 0.1, y + 0.1, z + 0.1),
                        level,
                        java.util.List.of());
        if (moved.x != vx) vx = 0;
        if (moved.y != vy) vy = 0;
        if (moved.z != vz) vz = 0;
        x += moved.x;
        y += moved.y;
        z += moved.z;
    }

    double interpX(float partialTick) {
        return prevX + (x - prevX) * partialTick;
    }

    double interpY(float partialTick) {
        return prevY + (y - prevY) * partialTick;
    }

    double interpZ(float partialTick) {
        return prevZ + (z - prevZ) * partialTick;
    }

    boolean expired() {
        return age >= maxAge;
    }
}
