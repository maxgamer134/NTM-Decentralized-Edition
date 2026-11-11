// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from susy-ntmdoors (Kyron04): https://github.com/Kyron04/susy-ntmdoors

package com.hbm.doors;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/** A code entered, or R pressed, on a keypad screen. */
public final class KeypadPayload extends ThreadedPayload {

    public static final Type<KeypadPayload> TYPE = new Type<>(Library.id("door_keypad"));
    public static final StreamCodec<ByteBuf, KeypadPayload> STREAM_CODEC =
            streamCodec(buf -> new KeypadPayload(BlockPos.of(buf.readLong()), buf.readBoolean(), ByteBufCodecs.STRING_UTF8.decode(buf)));

    private final BlockPos pos;
    private final boolean close;
    private final String code;

    public KeypadPayload(BlockPos pos, boolean close, String code) {
        this.pos = pos;
        this.close = close;
        this.code = code.length() > 6 ? code.substring(0, 6) : code;
    }

    public static void handle(KeypadPayload payload, IPayloadHandlerContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (player.distanceToSqr(payload.pos.getCenter()) > 64) return;
        if (!(player.level().getBlockEntity(payload.pos) instanceof BlockEntityDoorKeypad keypad)) return;
        if (payload.close) keypad.closeDoor(player);
        else keypad.enter(player, payload.code);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.asLong());
        buf.writeBoolean(close);
        ByteBufCodecs.STRING_UTF8.encode(buf, code);
    }

    @Override
    public @NotNull Type<KeypadPayload> type() {
        return TYPE;
    }
}
