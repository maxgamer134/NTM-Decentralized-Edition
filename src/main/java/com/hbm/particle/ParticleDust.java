// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.backport.client.particle.ParticleCompat;
import com.hbm.backport.client.particle.ParticleRenderType;
import com.hbm.client.particle.QuadParticleVisual;
import com.hbm.client.particle.VisualParticle;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space's ParticleDust: a big translucent rust-brown puff blowing straight through the world,
 * the dust of a Duna storm streaking past.
 */
public class ParticleDust extends ParticleCompat implements IQuadParticle, VisualParticle {

    public static final ParticleRenderType DUST_GROUP = new ParticleRenderType("hbm:duststorm", "HDS");

    private static final int COLOR = 0x80000000 | (int) (0.4F * 255) << 16 | (int) (0.2F * 255) << 8 | (int) (0.1F * 255);

    private final float scale;
    private @Nullable VisualizationManager visualManager;

    public ParticleDust(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ, float scale) {
        super(level, x, y, z);
        this.scale = scale;
        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;
        this.age = 1;
        this.lifetime = 50 + this.random.nextInt(50);
        this.gravity = 0F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        // 1.7.10's EntityFX.onUpdate: drifts on, slowing 2% a tick
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.98D;
        this.yd *= 0.98D;
        this.zd *= 0.98D;
    }

    @Override
    public int argb(float pt) {
        return COLOR;
    }

    @Override
    public float size(float pt) {
        return scale;
    }

    @Override
    public int light(float pt) {
        return getLightCoords(pt);
    }

    @Override
    public double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    @Override
    public double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

    @Override
    public double interpZ(float pt) {
        return this.zo + (this.z - this.zo) * pt;
    }

    @Override
    public ClientLevel level() {
        return level;
    }

    @Override
    public @Nullable VisualizationManager visualManager() {
        return visualManager;
    }

    @Override
    public void visualManager(@Nullable VisualizationManager manager) {
        visualManager = manager;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext context, float partialTick) {
        return QuadParticleVisual.particleBase(context, this, partialTick);
    }

    @Override
    public ParticleRenderType getGroup() {
        return DUST_GROUP;
    }
}
