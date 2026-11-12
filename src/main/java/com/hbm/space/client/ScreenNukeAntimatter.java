// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.client.gui.ScreenInfoContainer;

import com.hbm.space.SpaceAntimatter;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import com.hbm.backport.client.gui.input.MouseButtonEvent;
import com.hbm.backport.client.rendertype.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class ScreenNukeAntimatter extends ScreenInfoContainer<SpaceAntimatter.Menu> {

    private static final ResourceLocation TEXTURE =
            Library.id("textures/gui/weapon/blomb_schematic.png");

    private static final int FIELD_X = 80, FIELD_Y = 108, FIELD_W = 29, FIELD_H = 12;
    private static final int START_X = 80, START_Y = 85, START_SIZE = 18;
    private static final float CLOCK_SCALE = 0.6F;
    private static final int CLOCK_CENTRE = 95, CLOCK_Y = 66;

    private @Nullable EditBox field;

    public ScreenNukeAntimatter(SpaceAntimatter.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
    }

    @Override
    protected void init() {
        super.init();
        field =
                new EditBox(
                        font,
                        leftPos + FIELD_X,
                        topPos + FIELD_Y,
                        FIELD_W,
                        FIELD_H,
                        Component.empty());
        field.setTextColor(0xFFFF0000);
        field.setTextColorUneditable(0xFF800000);
        field.setBordered(false);
        field.setMaxLength(3);
        field.setValue(String.valueOf(menu.blockEntity().timer / 20));
        field.setResponder(this::commit);
        addRenderableWidget(field);
    }

    private void commit(String text) {
        if (text.isEmpty() || !text.chars().allMatch(Character::isDigit)) return;
        int seconds =
                Math.clamp(
                        Integer.parseInt(text),
                        1,
                        999);
        CompoundTag data = new CompoundTag();
        data.putInt("timer", seconds);
        PacketWire.sendToServer(new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && !menu.blockEntity().started
                && checkClick(
                        (int) event.x(),
                        (int) event.y(),
                        START_X,
                        START_Y,
                        START_SIZE,
                        START_SIZE)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("start", true);
            PacketWire.sendToServer(new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        SpaceAntimatter.BombBE bomb = menu.blockEntity();

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        if (bomb.started) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + START_X,
                    topPos + START_Y,
                    176.0F,
                    26.0F,
                    START_SIZE,
                    START_SIZE,
                    256,
                    256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SpaceAntimatter.BombBE bomb = menu.blockEntity();

        graphics.text(font, title, (imageWidth - font.width(title)) / 2, 6, -12566464, false);
        graphics.text(font, playerInventoryTitle, 8, imageHeight - 96 + 2, -12566464, false);

        if (bomb.hasCore()) {
            Component clock = Component.literal(bomb.getMinutes() + ":" + bomb.getSeconds());
            graphics.pose().pushMatrix();
            graphics.pose().scale(CLOCK_SCALE, CLOCK_SCALE);
            graphics.text(
                    font,
                    clock,
                    (int) ((CLOCK_CENTRE - font.width(clock) / 2F) / CLOCK_SCALE),
                    (int) (CLOCK_Y / CLOCK_SCALE),
                    0xFFFF0000,
                    false);
            graphics.pose().popMatrix();
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
