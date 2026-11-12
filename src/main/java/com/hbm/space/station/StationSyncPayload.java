// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.station;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.PacketWire;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * The station a player in orbit is on, for their sky, gravity and the station screens. NTM:Space
 * sent it with the core port's update, and with the rocket's during a transfer; this goes
 * straight to each player in orbit.
 */
public final class StationSyncPayload extends ThreadedPayload {

    public static final Type<StationSyncPayload> TYPE = new Type<>(Library.id("station_sync"));
    public static final StreamCodec<ByteBuf, StationSyncPayload> STREAM_CODEC = streamCodec(StationSyncPayload::decode);

    private final byte[] data;

    private StationSyncPayload(byte[] data) {
        this.data = data;
    }

    public static void send(ServerPlayer player, OrbitalStation station) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        station.serialize(buf);
        byte[] bytes = new byte[buf.writerIndex()];
        buf.getBytes(0, bytes);
        PacketWire.sendTo(new StationSyncPayload(bytes), player);
    }

    private static StationSyncPayload decode(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new StationSyncPayload(bytes);
    }

    public static void handle(StationSyncPayload payload, IPayloadHandlerContext ctx) {
        OrbitalStation.clientStation = OrbitalStation.deserialize(new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data)));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBytes(data);
    }

    @Override
    public @NotNull Type<StationSyncPayload> type() {
        return TYPE;
    }
}
