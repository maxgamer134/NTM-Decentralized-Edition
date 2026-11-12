// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.space.station.OrbitalStation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/** The station computer: the station's name, and its artificial gravity switch. */
public class ScreenOrbitalComputer extends Screen {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/machine/gui_ship_computer.png");
    private static final int W = 198, H = 135;

    private final BlockPos pos;
    private int left, top;
    private EditBox name;

    public ScreenOrbitalComputer(BlockPos pos) {
        super(Component.translatable("container.orbitalStationComputer"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        name = new EditBox(font, left + 52, top + 26, 136, 12, Component.empty());
        name.setBordered(false);
        name.setTextColor(0xffffff);
        name.setMaxLength(64);
        name.setValue(OrbitalStation.clientStation.name);
        name.setResponder(text -> send(tag -> tag.putString("name", text)));
        addRenderableWidget(name);
        setInitialFocus(name);
    }

    private void send(java.util.function.Consumer<CompoundTag> fill) {
        CompoundTag data = new CompoundTag();
        fill.accept(data);
        PacketWire.sendToServer(new NbtControlPayload(pos, data));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, left, top, 0, 0, W, H);
        if (OrbitalStation.clientStation.gravityMultiplier > 0) graphics.blit(TEXTURE, left + 36, top + 84, W, 0, 29, 5);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= left + 36 && mouseX < left + 65 && mouseY >= top + 84 && mouseY < top + 89) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
            boolean on = OrbitalStation.clientStation.gravityMultiplier == 0;
            OrbitalStation.clientStation.gravityMultiplier = on ? 1 : 0;
            send(tag -> tag.putBoolean("gravity", on));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
