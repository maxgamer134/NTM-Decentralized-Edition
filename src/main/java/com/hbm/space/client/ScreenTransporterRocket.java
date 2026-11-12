// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.gui.input.MouseButtonEvent;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.client.gui.ScreenInfoContainer;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.space.machine.SpaceTransporter;
import com.hbm.space.machine.SpaceTransporter.TransporterBE;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** The transporter pad: its name, the cargo slots and tanks both ways, the fuel, and the launch threshold slider. */
public class ScreenTransporterRocket extends ScreenInfoContainer<SpaceTransporter.Menu> {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/machine/gui_transporter.png");
    private static final int W = 230, H = 236;

    private @Nullable EditBox name;
    private boolean dragging;

    public ScreenTransporterRocket(SpaceTransporter.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private TransporterBE be() {
        return menu.blockEntity();
    }

    @Override
    protected void init() {
        super.init();
        name = new EditBox(font, leftPos + 8, topPos + 12, 122, 12, Component.empty());
        name.setTextColor(0xFF00FF00);
        name.setTextColorUneditable(0xFF00FF00);
        name.setBordered(false);
        name.setMaxLength(64);
        name.setValue(be().getTransporterName());
        name.setResponder(text -> send(data -> data.putString("name", text)));
        addRenderableWidget(name);
    }

    private void send(java.util.function.Consumer<CompoundTag> fill) {
        CompoundTag data = new CompoundTag();
        fill.accept(data);
        PacketWire.sendToServer(new NbtControlPayload(be().getBlockPos(), data));
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 100 + (int) (be().threshold * 6.78), topPos + 122, W, 0, 4, 15, 256, 256);
        String threshold = "x" + be().getThreshold();
        graphics.text(font, threshold, leftPos + 167 - font.width(threshold), topPos + 12, 0xFFFFFFFF, true);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (int i = 0; i < 4; i++) {
            bar(graphics, mouseX, mouseY, 8 + i * 18, 70, 34, i);
            bar(graphics, mouseX, mouseY, 98 + i * 18, 70, 34, i + 4);
        }
        for (int i = 0; i < 2; i++) bar(graphics, mouseX, mouseY, 188 + i * 18, 18, 70, i + 8);
    }

    private void bar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int h, int tank) {
        drawFluidBar(graphics, x, y, 16, h, be().tanks[tank]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, x, y, 16, h, be().tanks[tank]);
    }

    private boolean inSlider(double x, double y) {
        return x >= leftPos + 98 && x < leftPos + 98 + 74 && y >= topPos + 120 && y < topPos + 140;
    }

    private void slide(double mouseX) {
        int pos = Mth.clamp((int) ((mouseX - (leftPos + 98)) / 6.78), 0, 10); // 2^0 - 2^9 | 0 - 512
        if (pos == be().threshold) return;
        be().threshold = pos;
        send(data -> data.putInt("threshold", pos));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        dragging = event.button() == 0 && inSlider(event.x(), event.y());
        if (dragging) {
            slide(event.x());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging && inSlider(event.x(), event.y())) {
            slide(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }
}
