// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.client.gui.ScreenInfoContainer;
import com.hbm.lib.Library;
import com.hbm.space.machine.SpaceMilkReformer;
import com.hbm.space.machine.SpaceMilkReformer.ReformerBE;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The dairy processing tank: milk in on the left, the three reformates out along the right. */
public class ScreenMilkReformer extends ScreenInfoContainer<SpaceMilkReformer.Menu> {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/machine/gui_milk_reformer.png");

    public ScreenMilkReformer(SpaceMilkReformer.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 238);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private ReformerBE be() {
        return menu.blockEntity();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int bar = (int) (be().power * 63 / SpaceMilkReformer.MAX_POWER);
        if (bar > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 98, topPos + 13, 176, 6, bar, 6, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ReformerBE be = be();
        drawFluidBar(graphics, 50, 32, 5, 30, be.tanks[0]);
        drawFluidBar(graphics, 101, 53, 3, 10, be.tanks[1]);
        drawFluidBar(graphics, 128, 53, 3, 10, be.tanks[2]);
        drawFluidBar(graphics, 155, 53, 3, 10, be.tanks[3]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 45, 38, 16, 29, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 90, 55, 21, 10, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 117, 55, 21, 10, be.tanks[2]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 143, 55, 21, 10, be.tanks[3]);
        drawElectricityInfo(graphics, mouseX, mouseY, 98, 8, 70, 20, be.power, SpaceMilkReformer.MAX_POWER);
        super.extractLabels(graphics, mouseX, mouseY);
    }
}
