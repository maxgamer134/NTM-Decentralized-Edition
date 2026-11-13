// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.packet.toclient;

import com.hbm.client.ClientEffects;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

/**
 * NTM:Space's "depress" and "spinlaunch" particles: rocket flames that skip the low-pressure
 * thinning. Depress is one white flame out of a hull breach; spinlaunch is a burst of count
 * flames, each nudged up to a block off and thrown along its offset, from the Dyson launcher.
 */
public final class RocketFlameBurstPayload extends ThreadedPayload {

    public static final Type<RocketFlameBurstPayload> TYPE = new Type<>(Library.id("rocket_flame_burst"));
    public static final StreamCodec<ByteBuf, RocketFlameBurstPayload> STREAM_CODEC =
            streamCodec(RocketFlameBurstPayload::decode);

    private final double x, y, z, motionX, motionY, motionZ;
    private final float scale;
    private final int maxAge, count;
    private final boolean white;

    private RocketFlameBurstPayload(double x, double y, double z, double motionX, double motionY, double motionZ,
            float scale, int maxAge, int count, boolean white) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.scale = scale;
        this.maxAge = maxAge;
        this.count = count;
        this.white = white;
    }

    /** "depress": one white flame. */
    public static RocketFlameBurstPayload depress(double x, double y, double z, double mX, double mY, double mZ, int maxAge) {
        return new RocketFlameBurstPayload(x, y, z, mX, mY, mZ, 0.5F, maxAge, 0, true);
    }

    /** "spinlaunch": count scattered flames. */
    public static RocketFlameBurstPayload spinlaunch(double x, double y, double z, double mX, double mY, double mZ,
            float scale, int maxAge, int count) {
        return new RocketFlameBurstPayload(x, y, z, mX, mY, mZ, scale, maxAge, count, false);
    }

    private static RocketFlameBurstPayload decode(ByteBuf buf) {
        return new RocketFlameBurstPayload(
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public static void handleClient(RocketFlameBurstPayload p, IPayloadHandlerContext ctx) {
        var player = ctx.playerOrNull();
        if (player == null) return;
        var level = player.level();
        if (p.white) {
            // the custom white still darkens as the flame ages
            ClientEffects.spawnRocketFlame(level, p.x, p.y, p.z, p.scale,
                    p.motionX, p.motionY, p.motionZ, p.maxAge, 1F, 1F, 1F);
            return;
        }
        RandomSource rand = level.random;
        for (int i = 0; i < p.count; i++) {
            double ox = rand.nextDouble() * 2 - 1;
            double oy = rand.nextDouble() * 2 - 1;
            double oz = rand.nextDouble() * 2 - 1;
            double mult = 1.0 - rand.nextDouble() * 0.1;
            ClientEffects.spawnRocketFlame(level, p.x + ox, p.y + oy, p.z + oz, p.scale,
                    p.motionX * mult + ox, p.motionY * mult + oy, p.motionZ * mult + oz,
                    p.maxAge + rand.nextInt(5), com.hbm.particle.RocketFlameColor.FLAME);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(motionX);
        buf.writeDouble(motionY);
        buf.writeDouble(motionZ);
        buf.writeFloat(scale);
        buf.writeInt(maxAge);
        buf.writeInt(count);
        buf.writeBoolean(white);
    }

    @Override
    public @NotNull Type<RocketFlameBurstPayload> type() {
        return TYPE;
    }
}
