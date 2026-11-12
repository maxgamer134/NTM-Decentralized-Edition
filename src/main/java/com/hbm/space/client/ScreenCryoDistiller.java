// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.client.gui.ScreenInfoContainer;
import com.hbm.lib.Library;
import com.hbm.space.machine.SpaceCryoDistiller;
import com.hbm.space.machine.SpaceCryoDistiller.DistillerBE;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The cryogenic distiller: the gas in, its four fractions out. */
public class ScreenCryoDistiller extends ScreenInfoContainer<SpaceCryoDistiller.Menu> {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/processing/gui_cryodistill.png");
    private static final int[] TANK_X = {44, 80, 98, 116, 134};

    public ScreenCryoDistiller(SpaceCryoDistiller.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 238);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private DistillerBE be() {
        return menu.blockEntity();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int bar = (int) (be().power * 52 / SpaceCryoDistiller.MAX_POWER);
        if (bar > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 26, topPos + 70 - bar, 176, 52 - bar, 16, bar, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        DistillerBE be = be();
        for (int i = 0; i < TANK_X.length; i++) {
            drawFluidBar(graphics, TANK_X[i], 18, 16, 52, be.tanks[i]);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X[i], 18, 16, 52, be.tanks[i]);
        }
        drawElectricityInfo(graphics, mouseX, mouseY, 26, 18, 16, 52, be.power, SpaceCryoDistiller.MAX_POWER);
        super.extractLabels(graphics, mouseX, mouseY);
    }
}
