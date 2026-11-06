// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * main's flow lines as components: a coloured arrow ({@code GREEN "-> "} in, {@code RED "<- "} out)
 * with the text after it left in the line's own colour, the same as main's {@code arrow + RESET +
 * text}.
 */
public final class ArrowText {

    private ArrowText() {}

    public static MutableComponent arrow(ChatFormatting colour, String arrow, Component text) {
        return Component.empty().append(Component.literal(arrow).withStyle(colour)).append(text);
    }

    public static MutableComponent in(Component text) {
        return arrow(ChatFormatting.GREEN, "-> ", text);
    }

    public static MutableComponent in(String text) {
        return in(Component.literal(text));
    }

    public static MutableComponent out(Component text) {
        return arrow(ChatFormatting.RED, "<- ", text);
    }

    public static MutableComponent out(String text) {
        return out(Component.literal(text));
    }
}
