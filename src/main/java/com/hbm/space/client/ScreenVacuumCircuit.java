// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.client.gui.ScreenInfoContainer;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.space.machine.SpaceVacuumCircuit;
import com.hbm.space.machine.SpaceVacuumCircuit.CircuitBE;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The vacuum solderer: wafers and boards on the left, the circuit out in the middle, power on the right. */
public class ScreenVacuumCircuit extends ScreenInfoContainer<SpaceVacuumCircuit.Menu> {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/processing/gui_vac_cir_station.png");

    public ScreenVacuumCircuit(SpaceVacuumCircuit.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        CircuitBE be = menu.blockEntity();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int p = (int) (be.power * 52 / Math.max(be.maxPower, 1));
        if (p > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 143, topPos + 70 - p, 176, 52 - p, 16, p, 256, 256);
        int i = be.progress * 33 / Math.max(be.processTime, 1);
        if (i > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 63, topPos + 28, 192, 0, i, 14, 256, 256);
        if (be.power >= be.consumption)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 146, topPos + 4, 176, 52, 9, 12, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        CircuitBE be = menu.blockEntity();
        drawInfoPanel(graphics, 78, 76, 8, 8, 8);
        drawElectricityInfo(graphics, mouseX, mouseY, 143, 18, 16, 52, be.getPower(), be.getMaxPower());
        drawCustomInfoStat(graphics, mouseX, mouseY, 78, 76, 8, 8, 78, 76, upgradeInfo(be, UpgradeType.SPEED, UpgradeType.POWER));
        super.extractLabels(graphics, mouseX, mouseY);
    }
}
