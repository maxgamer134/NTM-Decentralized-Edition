// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.blockentity.network.BlockEntityCableDiode;
import com.hbm.blockentity.network.BlockEntityNetworkSeparator;
import net.minecraft.core.BlockPos;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import com.hbm.backport.client.gui.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import com.hbm.backport.client.gui.ScreenCompat;
import com.hbm.backport.client.gui.WidgetBridge;

public final class ScreenCableDiode extends ScreenCompat {

    private final BlockPos pos;
    private final long limit;
    private EditBox throughput;
    private int priority;

    public ScreenCableDiode(BlockEntityCableDiode diode) {
        this(diode.getBlockPos(), diode.limit, diode.priority);
    }

    /** main's GUIDiode belongs to the network separator; the cable diode shares it */
    public ScreenCableDiode(BlockEntityNetworkSeparator separator) {
        this(separator.getBlockPos(), separator.configuredLimit, separator.priority);
    }

    private ScreenCableDiode(BlockPos pos, long limit, ConnectionPriority priority) {
        super(Component.empty());
        this.pos = pos;
        this.limit = limit;
        this.priority = priority.ordinal();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        throughput =
                new EditBox(
                        font,
                        cx - 150,
                        100,
                        90,
                        20,
                        Component.translatable("gui.diode.throughput"));
        throughput.setMaxLength(11);
        throughput.setValue(Long.toString(limit));
        addRenderableWidget(throughput);
        addRenderableWidget(
                Button.builder(
                                Component.literal(ConnectionPriority.VALUES[priority].name()),
                                button -> {
                                    priority = (priority + 1) % ConnectionPriority.VALUES.length;
                                    button.setMessage(
                                            Component.literal(
                                                    ConnectionPriority.VALUES[priority].name()));
                                })
                        .bounds(cx + 20, 100, 90, 20)
                        .build());
        setInitialFocus(throughput);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        graphics.text(
                font,
                Component.translatable("gui.diode.throughput"),
                cx - 150,
                80,
                0xFFA0A0A0,
                false);
        graphics.text(
                font,
                Component.translatable("gui.diode.throughputMax"),
                cx - 150,
                90,
                0xFFA0A0A0,
                false);
        graphics.text(
                font, Component.translatable("gui.diode.priority"), cx + 20, 80, 0xFFA0A0A0, false);
    }

    @Override
    public void onClose() {
        CompoundTag data = new CompoundTag();
        data.putByte("priority", (byte) priority);
        try {
            data.putLong("limit", Long.parseLong(throughput.getValue()));
        } catch (NumberFormatException ignored) {
        }
        PacketWire.sendToServer(new NbtControlPayload(pos, data));
        super.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft != null && WidgetBridge.matches(minecraft.options.keyInventory, event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
