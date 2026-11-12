// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.client.gui.ScreenInfoContainer;
import com.hbm.lib.Library;
import com.hbm.space.machine.SpaceHydroponic;
import com.hbm.space.machine.SpaceHydroponic.BayBE;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The hydroponic bay: carbon dioxide and oxygen gauges, the battery bar and the fertilizer level. */
public class ScreenHydroponic extends ScreenInfoContainer<SpaceHydroponic.Menu> {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/machine/gui_hydrobay.png");

    public ScreenHydroponic(SpaceHydroponic.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private BayBE be() {
        return menu.blockEntity();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        BayBE be = be();
        int power = (int) (be.power * 34 / SpaceHydroponic.MAX_POWER);
        if (power > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 147, topPos + 52 - power, 176, 34 - power, 16, power, 256, 256);
        int fert = be.fertilizer * 32 / SpaceHydroponic.MAX_FERTILIZER;
        if (fert > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 102, topPos + 69 - fert, 192, 32 - fert, 6, fert, 256, 256);
        if (be.lightsOn()) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 151, topPos + 4, 176, 34, 9, 12, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BayBE be = be();
        drawFluidBar(graphics, 13, 18, 16, 52, be.tanks[0]);
        drawFluidBar(graphics, 31, 18, 16, 52, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 13, 18, 16, 52, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 31, 18, 16, 52, be.tanks[1]);
        drawElectricityInfo(graphics, mouseX, mouseY, 147, 18, 16, 34, be.power, SpaceHydroponic.MAX_POWER);
        super.extractLabels(graphics, mouseX, mouseY);
    }
}
