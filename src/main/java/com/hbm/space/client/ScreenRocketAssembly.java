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
import com.hbm.space.CelestialBody;
import com.hbm.space.rocket.RocketStruct;
import com.hbm.space.rocket.SpaceAssembly;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The assembly building's screen: parts per stage, the rocket turning, and what's wrong with it. */
public class ScreenRocketAssembly extends ScreenInfoContainer<SpaceAssembly.Menu> {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/machine/gui_rocket_assembly.png");
    private static final int W = 192, H = 224;

    private double currentScale = 1, currentOffset;
    private long lastTime = System.nanoTime();

    public ScreenRocketAssembly(SpaceAssembly.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private SpaceAssembly.AssemblyBE be() {
        return menu.blockEntity();
    }

    private void send(String key, int value) {
        CompoundTag data = new CompoundTag();
        if (key.equals("stage")) data.putInt(key, value);
        else data.putBoolean(key, true);
        PacketWire.sendToServer(new NbtControlPayload(be().getBlockPos(), data));
    }

    private void setStage(int stage) {
        be().currentStage = stage;
        send("stage", stage);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();
        RocketStruct rocket = be().rocket;
        if (event.button() == 0) {
            if (checkClick(x, y, 17, 34, 18, 8)) {
                playClick();
                if (be().currentStage > 0) setStage(be().currentStage - 1);
                return true;
            }
            if (checkClick(x, y, 17, 98, 18, 8)) {
                playClick();
                if (be().currentStage < Math.min(rocket.stages.size(), RocketStruct.MAX_STAGES - 1)) setStage(be().currentStage + 1);
                return true;
            }
            if (checkClick(x, y, 41, 62, 18, 18)) {
                if (rocket.validate()) {
                    playClick();
                    send("construct", 1);
                    return true;
                }
                if (be().canDeconstruct()) {
                    playClick();
                    send("deconstruct", 1);
                    return true;
                }
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
        RocketStruct rocket = be().rocket;
        blit(graphics, 0, 0, 0, 0, W, H);
        int stage = Math.max(rocket.stages.size() - 1 - be().currentStage, -1);
        blit(graphics, 47, 39, W + 18 + (stage + 1) * 6, 0, 6, 8);
        stage = Math.max(stage, 0);

        if (checkClick(mouseX, mouseY, 17, 34, 18, 8)) blit(graphics, 17, 34, W, 36, 18, 8);
        if (checkClick(mouseX, mouseY, 17, 98, 18, 8)) blit(graphics, 17, 98, W, 44, 18, 8);
        if (rocket.validate()) blit(graphics, 41, 62, W + 18, 8, 18, 18);
        if (be().canDeconstruct()) blit(graphics, 39, 52, W + 36, 8, 20, 38);

        // the rocket, eased toward the stage on show
        double dt = Math.min((System.nanoTime() - lastTime) / 1e9 * 4, 1);
        lastTime = System.nanoTime();
        double target = 86 / Math.max(rocket.getHeight(stage), 6);
        currentScale += (target - currentScale) * dt;
        currentOffset += (rocket.getOffset(stage) - currentOffset) * dt;
        if (rocket.capsule != null || !rocket.stages.isEmpty())
            graphics.guiRenderState.addPicturesInPictureState(new RocketPreview.State(rocket, (float) currentScale,
                    (float) currentOffset, System.currentTimeMillis() / 10 % 360, leftPos + 65, topPos + 5, leftPos + 155, topPos + 111));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SpaceAssembly.AssemblyBE be = be();
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        List<Component> issues = be.issues(minecraft.level);
        for (int i = 0; i < issues.size(); i++) graphics.text(font, issues.get(i), 65 * 2, 5 * 2 + i * 8, 0xFFFFFFFF, true);
        CelestialBody from = SpaceAssembly.body(be.fromDrive(be.currentStage), minecraft.level);
        CelestialBody to = SpaceAssembly.body(be.toDrive(be.currentStage), minecraft.level);
        if (from != null) graphics.text(font, Component.translatable("body." + from.name), 162 * 2, 75 * 2, 0xFF00FF00, false);
        if (to != null) graphics.text(font, Component.translatable("body." + to.name), 162 * 2, 108 * 2, 0xFF00FF00, false);
        graphics.pose().popMatrix();
        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }
}
