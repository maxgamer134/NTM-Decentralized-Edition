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
import com.hbm.space.rocket.SpaceDriveProcessor;
import com.hbm.space.rocket.SpaceDriveProcessor.ProcessorBE;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The drive processor's screen: process and clone buttons, power, progress and status. */
public class ScreenDriveProcessor extends ScreenInfoContainer<SpaceDriveProcessor.Menu> {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/processing/gui_drive_processor.png");
    private static final int W = 176, H = 207;

    public ScreenDriveProcessor(SpaceDriveProcessor.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private ProcessorBE be() {
        return menu.blockEntity();
    }

    private void send(String key, boolean value) {
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, value);
        PacketWire.sendToServer(new NbtControlPayload(be().getBlockPos(), data));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();
        if (event.button() == 0) {
            if (checkClick(x, y, 38, 61, 18, 18)) {
                playClick();
                send("process", !be().isProcessing);
                return true;
            }
            if (checkClick(x, y, 49, 17, 18, 18)) {
                playClick();
                send("clone", true);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + x, topPos + y, u, v, w, h, 256, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        ProcessorBE be = be();
        blit(graphics, 0, 0, 0, 0, W, H);
        if (be.isProcessing) blit(graphics, 38, 61, 192, 28, 18, 18);
        int p = (int) (be.power * 52 / ProcessorBE.MAX_POWER);
        blit(graphics, 134, 18 + 52 - p, W, 52 - p, 16, p);
        if (be.isProcessing) blit(graphics, 59, 63, 192, 14, be.progress * 44 / ProcessorBE.MAX_PROGRESS, 14);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ProcessorBE be = be();
        graphics.text(font, be.status.line(), 41, 86, 0xFFFFFFFF, false);
        super.extractLabels(graphics, mouseX, mouseY);
        drawElectricityInfo(graphics, mouseX, mouseY, 134, 18, 16, 52, be.power, ProcessorBE.MAX_POWER);
        drawCustomInfoStat(graphics, mouseX, mouseY, 49, 17, 18, 18, List.of(Component.literal("Clone drive")));
        drawCustomInfoStat(graphics, mouseX, mouseY, 38, 61, 18, 18, List.of(Component.literal("Start drive processing")));
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }
}
