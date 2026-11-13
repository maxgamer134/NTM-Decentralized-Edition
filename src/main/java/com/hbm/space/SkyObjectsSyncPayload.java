// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/** The stations orbiting the player's body and the satellites around it, for the sky. */
public final class SkyObjectsSyncPayload extends ThreadedPayload {

    public static final Type<SkyObjectsSyncPayload> TYPE = new Type<>(Library.id("sky_objects_sync"));
    public static final StreamCodec<ByteBuf, SkyObjectsSyncPayload> STREAM_CODEC = streamCodec(SkyObjectsSyncPayload::decode);

    private final byte[] data;

    private SkyObjectsSyncPayload(byte[] data) {
        this.data = data;
    }

    byte[] data() {
        return data;
    }

    static SkyObjectsSyncPayload of(List<int[]> stations, Map<Integer, SpaceSkySync.SkySat> satellites) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(stations.size());
        for (int[] station : stations) {
            buf.writeInt(station[0]);
            buf.writeInt(station[1]);
        }
        buf.writeVarInt(satellites.size());
        satellites.forEach((freq, sat) -> {
            buf.writeInt(freq);
            buf.writeUtf(sat.type());
            com.hbm.space.satellite.SatSettings.STREAM_CODEC.encode(buf, sat.settings());
        });
        byte[] bytes = new byte[buf.writerIndex()];
        buf.getBytes(0, bytes);
        return new SkyObjectsSyncPayload(bytes);
    }

    private static SkyObjectsSyncPayload decode(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new SkyObjectsSyncPayload(bytes);
    }

    public static void handle(SkyObjectsSyncPayload payload, IPayloadHandlerContext ctx) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data));
        int stationCount = buf.readVarInt();
        List<int[]> stations = new ArrayList<>(stationCount);
        for (int i = 0; i < stationCount; i++) stations.add(new int[] {buf.readInt(), buf.readInt()});
        int satCount = buf.readVarInt();
        Map<Integer, SpaceSkySync.SkySat> sats = new LinkedHashMap<>();
        for (int i = 0; i < satCount; i++) sats.put(buf.readInt(), new SpaceSkySync.SkySat(buf.readUtf(), com.hbm.space.satellite.SatSettings.STREAM_CODEC.decode(buf)));
        SpaceSkySync.clientStations = List.copyOf(stations);
        SpaceSkySync.clientSatellites = Map.copyOf(sats);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBytes(data);
    }

    @Override
    public @NotNull Type<SkyObjectsSyncPayload> type() {
        return TYPE;
    }
}
