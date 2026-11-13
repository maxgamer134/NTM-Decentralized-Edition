// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.PacketWire;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/** The breath a player has left in a vacuum (HbmLivingProps oxy), for the bubbles on the HUD. */
public final class SpaceOxygenPayload extends ThreadedPayload {

    public static final Type<SpaceOxygenPayload> TYPE = new Type<>(Library.id("space_oxygen"));
    public static final StreamCodec<ByteBuf, SpaceOxygenPayload> STREAM_CODEC =
            streamCodec(buf -> new SpaceOxygenPayload(buf.readUnsignedByte()));

    /** The local player's breath, 0 to 100; 100 is a full breath and shows no bubbles. */
    public static volatile int clientOxygen = 100;

    private final int oxygen;

    private SpaceOxygenPayload(int oxygen) {
        this.oxygen = oxygen;
    }

    public static void send(ServerPlayer player, int oxygen) {
        PacketWire.sendTo(new SpaceOxygenPayload(oxygen), player);
    }

    public static void handle(SpaceOxygenPayload payload, IPayloadHandlerContext ctx) {
        clientOxygen = payload.oxygen;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(oxygen);
    }

    @Override
    public @NotNull Type<SpaceOxygenPayload> type() {
        return TYPE;
    }
}
