// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.PacketWire;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.space.trait.CelestialBodyTrait;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Every body's saved traits, sent to clients on login and whenever one changes, so skies and
 * machines on the client see terraforming. The traits are written when the payload is made, on
 * the server thread, and travel as bytes.
 */
public final class SolarSystemSyncPayload extends ThreadedPayload {

    public static final Type<SolarSystemSyncPayload> TYPE = new Type<>(Library.id("solar_system_sync"));
    public static final StreamCodec<ByteBuf, SolarSystemSyncPayload> STREAM_CODEC =
            streamCodec(SolarSystemSyncPayload::decode);

    private final byte[] data;

    private SolarSystemSyncPayload(byte[] data) {
        this.data = data;
    }

    public static SolarSystemSyncPayload of(SolarSystemSavedData solarSystem) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> all = solarSystem.allTraits();
        buf.writeVarInt(all.size());
        all.forEach(
                (body, traits) -> {
                    buf.writeUtf(body);
                    buf.writeVarInt(traits.size());
                    for (CelestialBodyTrait trait : traits.values()) {
                        buf.writeUtf(CelestialBodyTrait.nameOf(trait));
                        FriendlyByteBuf traitBuf = new FriendlyByteBuf(Unpooled.buffer());
                        trait.writeToBytes(traitBuf);
                        // length-prefixed, so a trait the client reads differently cannot shift the rest
                        buf.writeByteArray(traitBuf.array().length == traitBuf.writerIndex()
                                ? traitBuf.array()
                                : java.util.Arrays.copyOf(traitBuf.array(), traitBuf.writerIndex()));
                    }
                });
        byte[] bytes = new byte[buf.writerIndex()];
        buf.getBytes(0, bytes);
        return new SolarSystemSyncPayload(bytes);
    }

    public static void sendToAll(SolarSystemSavedData solarSystem) {
        PacketWire.sendToAll(of(solarSystem));
    }

    public static void sendTo(ServerPlayer player, SolarSystemSavedData solarSystem) {
        PacketWire.sendTo(of(solarSystem), player);
    }

    private static SolarSystemSyncPayload decode(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new SolarSystemSyncPayload(bytes);
    }

    public static void handle(SolarSystemSyncPayload payload, IPayloadHandlerContext ctx) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data));
        Map<String, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>> all = new HashMap<>();
        int bodies = buf.readVarInt();
        for (int i = 0; i < bodies; i++) {
            String body = buf.readUtf();
            int count = buf.readVarInt();
            Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = new LinkedHashMap<>();
            for (int j = 0; j < count; j++) {
                CelestialBodyTrait trait = CelestialBodyTrait.create(buf.readUtf());
                byte[] traitData = buf.readByteArray();
                if (trait == null) continue;
                trait.readFromBytes(new FriendlyByteBuf(Unpooled.wrappedBuffer(traitData)));
                traits.put(trait.getClass(), trait);
            }
            all.put(body, traits);
        }
        SolarSystemSavedData.updateClientTraits(all);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBytes(data);
    }

    @Override
    public @NotNull Type<SolarSystemSyncPayload> type() {
        return TYPE;
    }
}
