// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.client.particle.VanillaExt;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.PacketWire;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

/** NTMain's "vanillaExt" aux particle: a vanilla particle with NTM's colour, size and motion. */
public final class VanillaExtPayload extends ThreadedPayload {
    public static final Type<VanillaExtPayload> TYPE = new Type<>(Library.id("vanilla_ext"));
    public static final StreamCodec<ByteBuf, VanillaExtPayload> STREAM_CODEC =
            streamCodec(VanillaExtPayload::decode);

    private final VanillaExt.Spec spec;

    public VanillaExtPayload(VanillaExt.Spec spec) {
        this.spec = spec;
    }

    /** Sends to every player within {@code range} blocks, as NTMain's TargetPoint did. */
    public static void send(ServerLevel level, VanillaExt.Spec spec, double range) {
        PacketWire.sendToAllAround(
                new VanillaExtPayload(spec),
                new TargetPoint(level, spec.x, spec.y, spec.z, range));
    }

    private static VanillaExtPayload decode(ByteBuf buf) {
        VanillaExt.Spec s =
                new VanillaExt.Spec(
                        VanillaExt.Mode.byId(buf.readByte()),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble());
        s.mX = buf.readDouble();
        s.mY = buf.readDouble();
        s.mZ = buf.readDouble();
        s.r = buf.readFloat();
        s.g = buf.readFloat();
        s.b = buf.readFloat();
        s.hasColor = buf.readBoolean();
        s.size = buf.readFloat();
        s.count = buf.readByte();
        s.overrideAge = buf.readInt();
        s.noclip = buf.readBoolean();
        s.blockState = buf.readInt();
        return new VanillaExtPayload(s);
    }

    public static void handle(VanillaExtPayload payload, IPayloadHandlerContext ctx) {
        if (ctx.playerOrNull() == null || payload.spec.mode == null) return;
        VanillaExt.spawn(payload.spec);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(spec.mode.ordinal());
        buf.writeDouble(spec.x);
        buf.writeDouble(spec.y);
        buf.writeDouble(spec.z);
        buf.writeDouble(spec.mX);
        buf.writeDouble(spec.mY);
        buf.writeDouble(spec.mZ);
        buf.writeFloat(spec.r);
        buf.writeFloat(spec.g);
        buf.writeFloat(spec.b);
        buf.writeBoolean(spec.hasColor);
        buf.writeFloat(spec.size);
        buf.writeByte(spec.count);
        buf.writeInt(spec.overrideAge);
        buf.writeBoolean(spec.noclip);
        buf.writeInt(spec.blockState);
    }

    @Override
    public @NotNull Type<VanillaExtPayload> type() {
        return TYPE;
    }
}
