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
import com.hbm.space.rocket.RocketStruct;
import com.hbm.space.rocket.SpaceLaunchPad;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The launch pad's screen: rocket and drive, fuel, power, what's in the way, and the launch button. */
public class ScreenLaunchPadRocket extends ScreenInfoContainer<SpaceLaunchPad.Menu> {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/machine/gui_launchpad_rocket.png");
    private static final int W = 188, H = 236;

    public ScreenLaunchPadRocket(SpaceLaunchPad.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private SpaceLaunchPad.PadBE be() {
        return menu.blockEntity();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        RocketStruct rocket = be().rocket;
        if (event.button() == 0 && rocket != null && rocket.validate()
                && checkClick((int) event.x(), (int) event.y(), 56, 20, 18, 17)) {
            playClick();
            CompoundTag data = new CompoundTag();
            data.putBoolean("launch", true);
            PacketWire.sendToServer(new NbtControlPayload(be().getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + x, topPos + y, u, v, w, h, 256, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        SpaceLaunchPad.PadBE be = be();
        blit(graphics, 0, 0, 0, 0, W, H);
        int p = (int) (be.power * 52 / SpaceLaunchPad.PadBE.MAX_POWER);
        blit(graphics, 167, 36 + 52 - p, W, 8 + 52 - p, 16, p);
        RocketStruct rocket = be.rocket;
        if (rocket == null) return;
        blit(graphics, 59, 43, W + (be.canLaunch() ? 12 : 0), 0, 12, 8);
        blit(graphics, 172, 23, W + 16 + (be.power > SpaceLaunchPad.PadBE.MAX_POWER * 3 / 4 ? 0 : 6), 8, 6, 8);
        float scale = (float) (86 / Math.max(rocket.getHeight(), 6));
        graphics.guiRenderState.addPicturesInPictureState(new RocketPreview.State(rocket, scale, 0,
                System.currentTimeMillis() / 10 % 360, leftPos + 97, topPos + 18, leftPos + 147, topPos + 124));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SpaceLaunchPad.PadBE be = be();
        if (be.rocket != null) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(0.5F, 0.5F);
            List<Component> issues = be.findIssues();
            for (int i = 0; i < issues.size(); i++) graphics.text(font, issues.get(i), 6 * 2, 66 * 2 + i * 8, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }
        super.extractLabels(graphics, mouseX, mouseY);
        drawElectricityInfo(graphics, mouseX, mouseY, 167, 36, 16, 52, be.power, SpaceLaunchPad.PadBE.MAX_POWER);
        drawCustomInfoStat(graphics, mouseX, mouseY, 56, 20, 18, 17, List.of(Component.literal("COMMIT TO LAUNCH")));
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }
}
