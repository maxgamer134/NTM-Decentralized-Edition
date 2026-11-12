// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.space.machine.SpaceTransporter;
import com.hbm.space.machine.SpaceTransporter.TransporterInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * The transporter pad linker: the pad that was clicked at the top, and below it the saved pads it
 * can link to (searchable, scrolled five at a time). Clicking a pad's button links the two, or
 * unlinks them if they are already linked; the top button unlinks the clicked pad.
 */
public class ScreenTransporterLinker extends Screen {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/tool/gui_transport_linker.png");
    private static final int W = 218, H = 201;
    private static final int GREEN = 0xFF00FF00;

    private final TransporterInfo linkFrom;
    private final @Nullable TransporterInfo linkTo;
    private final List<TransporterInfo> transporters;
    private List<TransporterInfo> visible = new ArrayList<>();
    private int left, top, index;
    private @Nullable EditBox search;

    public ScreenTransporterLinker(TransporterInfo linkFrom, List<TransporterInfo> transporters) {
        super(Component.translatable("item.hbm.transporter_linker"));
        this.linkFrom = linkFrom;
        this.linkTo = linkFrom.linkedTo;
        this.transporters = transporters;
    }

    /** The hook {@link SpaceTransporter#OPEN_LINKER} is set to on the client. */
    public static void open(TransporterInfo from, List<TransporterInfo> saved) {
        Minecraft.getInstance().setScreen(new ScreenTransporterLinker(from, saved));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        search = new EditBox(font, left + 59, top + 46, 86, 12, Component.empty());
        search.setTextColor(0xFFFFFFFF);
        search.setBordered(false);
        search.setResponder(text -> updateSearch());
        addRenderableWidget(search);
        setInitialFocus(search);
        updateSearch();
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft != null && minecraft.player != null && !minecraft.player.getMainHandItem().is(SpaceTransporter.LINKER.get())) onClose();
    }

    private void updateSearch() {
        visible = new ArrayList<>();
        index = 0;
        String sub = search == null ? "" : search.getValue().toLowerCase(Locale.US);
        for (TransporterInfo t : transporters) {
            if (t.equals(linkFrom)) continue;
            if (t.linkedTo != null && !t.linkedTo.equals(linkFrom)) continue;
            if (t.name.toLowerCase(Locale.US).contains(sub)) visible.add(t);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawForeground(graphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int hideScroll = visible.size() <= 5 ? 20 : 0;
        graphics.blit(TEXTURE, left, top, 0, 0, W - hideScroll, H);
        if (search != null && search.isFocused()) graphics.blit(TEXTURE, left + 55, top + 41, 0, H, 107, 18);

        // the pad being linked
        int x = left + 37, textLeft = left + 62, textRight = left + 188;
        entry(graphics, linkFrom, x, top + 14, textLeft, textRight, top + 13);
        for (int i = index; i < Math.min(index + 5, visible.size()); i++) {
            int y = top + 70 + (i - index) * 26;
            entry(graphics, visible.get(i), x, y, textLeft, textRight, y - 1);
        }
    }

    private void entry(GuiGraphics graphics, TransporterInfo t, int iconX, int iconY, int textLeft, int textRight, int textY) {
        graphics.blit(t.planet(), iconX, iconY, 0F, 0F, 16, 16, 16, 16);
        String coords = "x: " + t.pos.getX() + ", z: " + t.pos.getZ();
        graphics.drawString(font, t.name, textLeft, textY, GREEN, true);
        graphics.drawString(font, coords, textRight - font.width(coords), textY + 10, GREEN, true);
    }

    private void drawForeground(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = left + 10;
        if (in(mouseX, mouseY, x, top + 13, 18, 18)) graphics.blit(TEXTURE, x, top + 13, W + 18, 0, 18, 18);
        for (int i = index; i < Math.min(index + 5, visible.size()); i++) {
            int y = top + 69 + (i - index) * 26;
            boolean hover = in(mouseX, mouseY, x, y, 18, 18);
            if (linkTo != null && linkTo.equals(visible.get(i))) {
                graphics.blit(TEXTURE, x, y, W, 18, 18, 18);
                if (hover) graphics.blit(TEXTURE, x, y, W + 18, 0, 18, 18);
            } else if (hover) {
                graphics.blit(TEXTURE, x, y, W, 0, 18, 18);
            }
        }
        if (visible.size() > 5) {
            int y = top + 68 + Mth.floor((float) index / (float) (visible.size() - 5) * 114.0F);
            graphics.blit(TEXTURE, left + 202, y, W, 54, 14, 15);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = left + 10;
        if (in(mouseX, mouseY, x, top + 13, 18, 18)) {
            if (select(null)) onClose();
            return true;
        }
        for (int i = index; i < Math.min(index + 5, visible.size()); i++) {
            int y = top + 69 + (i - index) * 26;
            if (in(mouseX, mouseY, x, y, 18, 18)) {
                if (select(visible.get(i))) onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (visible.size() > 5 && in(mouseX, mouseY, left + 198, top + 64, 22, 137)) {
            index = Mth.clamp(Mth.floor((float) (mouseY - (top + 68)) / 114.0F * (visible.size() - 5)), 0, visible.size() - 5);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && index > 0) index--;
        if (scrollY < 0 && index < visible.size() - 5) index++;
        return true;
    }

    /** Links commutatively; clicking the linked pad again unlinks. */
    private boolean select(@Nullable TransporterInfo t) {
        if (linkFrom.equals(t)) return false;
        CompoundTag data = new CompoundTag();
        if (t == null || linkTo != null && linkTo.equals(t)) {
            data.putBoolean("unlink", true);
        } else {
            data.putString("dimension", t.dimension.location().toString());
            data.putIntArray("linkedTo", new int[] {t.pos.getX(), t.pos.getY(), t.pos.getZ()});
        }
        PacketWire.sendToServer(new NbtControlPayload(linkFrom.pos, data));
        return true;
    }

    private static boolean in(double mouseX, double mouseY, int x, int y, int width, int height) {
        return x <= mouseX && x + width > mouseX && y <= mouseY && y + height > mouseY;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
