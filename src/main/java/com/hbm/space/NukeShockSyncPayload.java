// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.PacketWire;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

/** Every nuclear flash still glowing on a body (CelestialNukeShockHandler.writeSync/readSync). */
public final class NukeShockSyncPayload extends ThreadedPayload {

    public static final Type<NukeShockSyncPayload> TYPE = new Type<>(Library.id("nuke_shock_sync"));
    public static final StreamCodec<ByteBuf, NukeShockSyncPayload> STREAM_CODEC = streamCodec(NukeShockSyncPayload::decode);

    private final byte[] data;

    private NukeShockSyncPayload(byte[] data) {
        this.data = data;
    }

    private static NukeShockSyncPayload of() {
        var server = ServerLifecycleHooks.getCurrentServer();
        long time = server == null ? 0 : server.overworld().getGameTime();
        Map<String, List<SpaceSkySync.ShockStatus>> shocks = SpaceSkySync.serverShocks(time);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(shocks.size());
        shocks.forEach((body, statuses) -> {
            buf.writeUtf(body);
            buf.writeByte(statuses.size());
            for (SpaceSkySync.ShockStatus status : statuses) {
                buf.writeLong(status.time());
                buf.writeFloat(status.centerX());
                buf.writeFloat(status.centerY());
                buf.writeFloat(status.strength());
            }
        });
        byte[] bytes = new byte[buf.writerIndex()];
        buf.getBytes(0, bytes);
        return new NukeShockSyncPayload(bytes);
    }

    static void sendToAll() {
        PacketWire.sendToAll(of());
    }

    static void send(ServerPlayer player) {
        PacketWire.sendTo(of(), player);
    }

    private static NukeShockSyncPayload decode(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new NukeShockSyncPayload(bytes);
    }

    public static void handle(NukeShockSyncPayload payload, IPayloadHandlerContext ctx) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data));
        Map<String, List<SpaceSkySync.ShockStatus>> shocks = new HashMap<>();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String body = buf.readUtf();
            int shockCount = buf.readUnsignedByte();
            List<SpaceSkySync.ShockStatus> statuses = new ArrayList<>(shockCount);
            for (int j = 0; j < shockCount; j++) {
                statuses.add(new SpaceSkySync.ShockStatus(buf.readLong(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
            }
            shocks.put(body, statuses);
        }
        SpaceSkySync.setClientShocks(shocks);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBytes(data);
    }

    @Override
    public @NotNull Type<NukeShockSyncPayload> type() {
        return TYPE;
    }
}
