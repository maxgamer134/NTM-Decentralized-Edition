// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.backport.ColorFamilies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * NTMain's ClientProxy "vanillaExt" effect, mode for mode: the vanilla particle each mode made in
 * 1.7.10 (mapped to its 1.21 counterpart), with the colour, scale, motion, lifetime and no-clip
 * changes NTMain applied on top. The spec can be built on either side; {@link #spawn} is client
 * only.
 */
public final class VanillaExt {

    public enum Mode {
        FLAME,
        SMOKE,
        VOLCANO,
        CLOUD,
        REDDUST,
        BLUEDUST,
        GREENDUST,
        FIREWORKS,
        LARGEEXPLODE,
        TOWNAURA,
        BLOCKDUST,
        COLORDUST;

        private static final Mode[] VALUES = values();

        public static Mode byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : null;
        }
    }

    /** The NBT fields NTMain's aux packet carried. */
    public static final class Spec {
        public final Mode mode;
        public final double x, y, z;
        public double mX, mY, mZ;
        public float r, g, b;
        public boolean hasColor;
        public float size;
        public int count;
        public int overrideAge;
        public boolean noclip;
        public int blockState;

        public Spec(Mode mode, double x, double y, double z) {
            this.mode = mode;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Spec motion(double mX, double mY, double mZ) {
            this.mX = mX;
            this.mY = mY;
            this.mZ = mZ;
            return this;
        }

        public Spec color(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.hasColor = true;
            return this;
        }

        /** largeexplode: the blast's size argument and how many grey puffs go with it. */
        public Spec explode(float size, int count) {
            this.size = size;
            this.count = count;
            return this;
        }

        public Spec age(int overrideAge) {
            this.overrideAge = overrideAge;
            return this;
        }

        public Spec noclip() {
            this.noclip = true;
            return this;
        }

        public Spec block(BlockState state) {
            this.blockState = Block.getId(state);
            return this;
        }
    }

    private VanillaExt() {}

    public static Spec of(Mode mode, double x, double y, double z) {
        return new Spec(mode, x, y, z);
    }

    private static Particle create(ParticleOptions type, Spec s, double vx, double vy, double vz) {
        return Minecraft.getInstance().particleEngine.createParticle(type, s.x, s.y, s.z, vx, vy, vz);
    }

    private static void tint(Particle fx, float r, float g, float b) {
        fx.rCol = r;
        fx.gCol = g;
        fx.bCol = b;
    }

    public static void spawn(Spec s) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        RandomSource rand = level.getRandom();
        Particle fx = null;

        switch (s.mode) {
            case FLAME -> fx = create(ParticleTypes.FLAME, s, s.mX, s.mY, s.mZ);
            case SMOKE -> fx = create(ParticleTypes.SMOKE, s, s.mX, s.mY, s.mZ);
            case VOLCANO -> {
                fx = create(ParticleTypes.SMOKE, s, s.mX, s.mY, s.mZ);
                if (fx != null) {
                    fx.scale(100F);
                    fx.setLifetime(200 + rand.nextInt(50));
                    fx.hasPhysics = false;
                    fx.setParticleSpeed(
                            rand.nextGaussian() * 0.2,
                            2.5 + rand.nextDouble(),
                            rand.nextGaussian() * 0.2);
                }
            }
            case CLOUD -> {
                fx = create(ParticleTypes.CLOUD, s, s.mX, s.mY, s.mZ);
                if (fx != null && s.hasColor) {
                    float rng = rand.nextFloat() * 0.1F;
                    tint(fx, s.r + rng, s.g + rng, s.b + rng);
                    // 1.7.10 set the cloud's base scale to 7.5, twice its 3.75 average
                    fx.scale(2F);
                    fx.setParticleSpeed(0, 0, 0);
                }
            }
            // EntityReddustFX took its colour as the motion arguments, a red of 0 meaning 1
            case REDDUST -> fx = dust(s, s.mX == 0 ? 1F : (float) s.mX, (float) s.mY, (float) s.mZ);
            case BLUEDUST -> fx = dust(s, 0.01F, 0.01F, 1F);
            case GREENDUST -> fx = dust(s, 0.01F, 0.5F, 0.1F);
            case FIREWORKS -> fx = create(ParticleTypes.FIREWORK, s, 0, 0, 0);
            case LARGEEXPLODE -> {
                // the explosion particle reads its size from the x speed, as 1.7.10's did
                fx = create(ParticleTypes.EXPLOSION, s, s.size, 0, 0);
                if (fx != null) {
                    float r = 1F - rand.nextFloat() * 0.2F;
                    tint(fx, r, 0.9F * r, 0.5F * r);
                }
                for (int i = 0; i < s.count; i++) {
                    Particle sec = create(ParticleTypes.POOF, s, 0, 0, 0);
                    if (sec == null) continue;
                    float r2 = 1F - rand.nextFloat() * 0.5F;
                    tint(sec, 0.5F * r2, 0.5F * r2, 0.5F * r2);
                    sec.scale(i + 1);
                }
            }
            case TOWNAURA -> {
                fx = create(ParticleTypes.MYCELIUM, s, 0, 0, 0);
                if (fx != null) {
                    float color = 0.5F + rand.nextFloat() * 0.5F;
                    tint(fx, 0.8F * color, 0.9F * color, color);
                    fx.setParticleSpeed(s.mX, s.mY, s.mZ);
                }
            }
            case BLOCKDUST -> {
                fx = create(
                        new BlockParticleOption(ParticleTypes.BLOCK, Block.stateById(s.blockState)),
                        s, s.mX, s.mY + 0.2, s.mZ);
                if (fx != null) {
                    fx.setParticleSpeed(s.mX, s.mY + 0.2, s.mZ);
                    fx.setLifetime(10 + rand.nextInt(20));
                    tint(fx, 0.8F, 0.8F, 0.8F);
                }
            }
            case COLORDUST -> {
                fx = create(
                        new BlockParticleOption(
                                ParticleTypes.BLOCK, ColorFamilies.WOOL.white().defaultBlockState()),
                        s, s.mX, s.mY + 0.2, s.mZ);
                if (fx != null) {
                    fx.setParticleSpeed(s.mX, s.mY + 0.2, s.mZ);
                    tint(fx, s.r, s.g, s.b);
                    fx.setLifetime(10 + rand.nextInt(20));
                }
            }
        }

        if (fx != null) {
            if (s.noclip) fx.hasPhysics = false;
            if (s.overrideAge > 0) fx.setLifetime(s.overrideAge);
        }
    }

    private static Particle dust(Spec s, float r, float g, float b) {
        return create(new DustParticleOptions(new Vector3f(r, g, b), 1F), s, 0, 0, 0);
    }
}
