// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from susy-ntmdoors (Kyron04): https://github.com/Kyron04/susy-ntmdoors

package com.hbm.doors.client;

import com.hbm.doors.DoorAccess;
import com.hbm.doors.KeypadPayload;
import com.hbm.packet.PacketWire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** The keypad's code pad: digits, C to clear, R to close the door, E to enter. Keys work too. */
public final class KeypadScreen extends Screen {

    private final BlockPos pos;
    private String code = "";

    private KeypadScreen(BlockPos pos) {
        super(Component.translatable("block.hbm.door_keypad"));
        this.pos = pos;
    }

    public static void open(BlockPos pos) {
        Minecraft.getInstance().setScreen(new KeypadScreen(pos));
    }

    @Override
    protected void init() {
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "E"};
        int size = 24, gap = 4, left = width / 2 - (3 * size + 2 * gap) / 2, top = height / 2 - 50;
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            addRenderableWidget(Button.builder(Component.literal(key), b -> press(key))
                    .bounds(left + (i % 3) * (size + gap), top + (i / 3) * (size + gap), size, size)
                    .build());
        }
        addRenderableWidget(Button.builder(Component.literal("R"), b -> press("R"))
                .bounds(left, top + 4 * (size + gap), 3 * size + 2 * gap, 20)
                .build());
    }

    private void press(String key) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(DoorAccess.PRESS.get(), 1F));
        switch (key) {
            case "C" -> code = "";
            case "E" -> {
                PacketWire.sendToServer(new KeypadPayload(pos, false, code));
                onClose();
            }
            case "R" -> {
                PacketWire.sendToServer(new KeypadPayload(pos, true, ""));
                onClose();
            }
            default -> {
                if (code.length() < 6) code += key;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) press(String.valueOf(keyCode - GLFW.GLFW_KEY_0));
        else if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) press(String.valueOf(keyCode - GLFW.GLFW_KEY_KP_0));
        else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) press("E");
        else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !code.isEmpty()) code = code.substring(0, code.length() - 1);
        else return super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = width / 2, y = height / 2 - 72;
        g.fill(x - 46, y - 4, x + 46, y + 14, 0xFF101810);
        String shown = code.isEmpty() ? Component.translatable("hbm.door_access.input_code").getString() : "*".repeat(code.length());
        g.drawCenteredString(font, shown, x, y + 1, 0x55FF55);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
