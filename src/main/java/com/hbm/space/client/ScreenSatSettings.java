// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.gui.ScreenCompat;
import com.hbm.backport.client.gui.WidgetBridge;
import com.hbm.backport.client.gui.input.KeyEvent;
import com.hbm.backport.client.gui.input.MouseButtonEvent;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.ItemControlPayload;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.space.satellite.SatSettings;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * GUIScreenSatSettings: the held satellite's inclination, altitude, phase, blink, owner and colour.
 * The scroll wheel steps the readouts, the three bars set the colour, and what changed is sent when
 * the screen closes. The preview shows the orbit and the satellite going round on a plain planet
 * instead of NTM:Space's shader-drawn body with the owner's other satellites.
 */
public class ScreenSatSettings extends ScreenCompat {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/satellites/gui_sat_settings.png");
    private static final int WIDTH = 134;
    private static final int HEIGHT = 221;
    private static final int TEX_W = 152;
    private static final int TEX_H = 221;
    private static final float SMALL = 2F / 3F;
    private static final int GREEN = 0xFF00FF00;

    private final Player player;
    private int left;
    private int top;
    private int draggedSlider = -1;
    private boolean pending;
    private String owner = SatSettings.DEFAULT_OWNER;
    private float altitude;
    private float inclination;
    private float phaseOffset;
    private boolean blinking;
    private float blinkPeriod;
    private int colorR;
    private int colorG;
    private int colorB;
    private boolean showDetails = true;
    private int detailsMode;

    public ScreenSatSettings(Player player) {
        super(Component.translatable("item.hbm.sat_scanner"));
        this.player = player;
    }

    private @Nullable ItemStack held() {
        ItemStack stack = player.getMainHandItem();
        return SatelliteType.fromStack(stack) != null ? stack : null;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        ItemStack stack = held();
        if (stack == null) return;
        SatSettings s = SatSettings.of(stack, SatelliteType.fromStack(stack).getSerializedName());
        pending = false;
        owner = s.owner();
        altitude = s.altitude();
        inclination = s.inclination();
        phaseOffset = s.phaseOffset();
        blinking = s.blinking();
        blinkPeriod = s.blinkPeriod();
        colorR = SatSettings.channel(s.r());
        colorG = SatSettings.channel(s.g());
        colorB = SatSettings.channel(s.b());
    }

    @Override
    public void tick() {
        if (held() == null) onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0F, 0F, WIDTH, HEIGHT, TEX_W, TEX_H);
        if (held() == null) return;

        if (showDetails) {
            // the battery: a slice of the sheet turned on its side
            g.pose().pushMatrix();
            g.pose().translate(left + 15 + 29, top + 200);
            g.pose().rotate((float) (Math.PI / 2D));
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 136F, 38F, 13, 29, TEX_W, TEX_H);
            g.pose().popMatrix();

            drawPreview(g);
            if (detailsMode == 0) {
                line(g, 130, 140, Component.translatable("item.sat.desc.speed").getString() + ": "
                        + number(Math.round(SatSettings.orbitSpeedKmPerSecond(altitude) * 10F) / 10F)
                        + Component.translatable("gui.sat.settings.unit.km_per_second").getString());
                line(g, 145, 155, Component.translatable("item.sat.desc.altitude").getString() + ": " + number(altitude)
                        + Component.translatable("gui.sat.settings.unit.km").getString());
                line(g, 160, 170, Component.translatable("item.sat.desc.inclination").getString() + ": " + number(inclination) + "°");
            } else {
                line(g, 130, 140, Component.translatable("gui.sat.settings.label.orbital_phase").getString() + ": "
                        + number(Math.round(SatSettings.normalizePhaseOffset(phaseOffset) * 10F) / 10F) + "°");
                line(g, 145, 155, Component.translatable("item.sat.desc.owner").getString() + ": " + owner);
            }
            g.fill(left + 81, top + 177, left + 110, top + 199, 0xFF000000 | colorR << 16 | colorG << 8 | colorB);
            String blink = number(blinkPeriod) + Component.translatable("gui.sat.settings.unit.seconds").getString();
            int bw = Math.round(font.width(blink) * SMALL);
            scaled(g, blink, left + 108 - bw, centred(204, 213), 0xFFFFFFFF);
        }

        slider(g, colorR, 180, 17);
        slider(g, colorG, 187, 24);
        slider(g, colorB, 194, 31);
        if (showDetails && blinking) g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left + 114, top + 202, 136F, 3F, 12, 12, TEX_W, TEX_H);

        if (!showDetails) return;
        if (ownerButton(mouseX, mouseY)) tip(g, "gui.sat.settings.tooltip.owner_assign", mouseX, mouseY);
        else if (modeButton(mouseX, mouseY)) tip(g, "gui.sat.settings.tooltip.switch_settings_list", mouseX, mouseY);
        else {
            switch (scrollField(mouseX, mouseY)) {
                case 0 -> tip(g, detailsMode == 0 ? "gui.sat.settings.tooltip.scroll_speed" : "gui.sat.settings.tooltip.scroll_phase", mouseX, mouseY);
                case 1 -> tip(g, "gui.sat.settings.tooltip.scroll_altitude", mouseX, mouseY);
                case 2 -> tip(g, "gui.sat.settings.tooltip.scroll_inclination", mouseX, mouseY);
                case 3 -> tip(g, "gui.sat.settings.tooltip.scroll_blink", mouseX, mouseY);
                default -> {}
            }
        }
    }

    private void tip(GuiGraphicsExtractor g, String key, int mouseX, int mouseY) {
        g.setTooltipForNextFrame(font, Component.translatable(key), mouseX, mouseY);
    }

    private static String number(float value) {
        if (value == (int) value) return Integer.toString((int) value);
        return Float.toString(value);
    }

    private int centred(int y1, int y2) {
        return top + y1 + ((y2 - y1 + 1) - Math.round(font.lineHeight * SMALL)) / 2;
    }

    private void line(GuiGraphicsExtractor g, int y1, int y2, String text) {
        scaled(g, text, left + 10, centred(y1, y2), GREEN);
    }

    private void scaled(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.pose().pushMatrix();
        g.pose().scale(SMALL, SMALL);
        g.text(font, text, Math.round(x / SMALL), Math.round(y / SMALL), color, false);
        g.pose().popMatrix();
    }

    private void slider(GuiGraphicsExtractor g, int value, int y, int v) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left + 12 + Math.round(value * 62 / 255F), top + y - 2, 136F, (float) v, 2, 6, TEX_W, TEX_H);
    }

    /** The orbit seen at an angle round a plain planet, the satellite going round it at its own speed. */
    private void drawPreview(GuiGraphicsExtractor g) {
        int cx = left + 9 + 58;
        int cy = top + 8 + 58;
        int planet = 13;
        for (int dy = -planet; dy <= planet; dy++) {
            int half = (int) Math.sqrt((double) planet * planet - (double) dy * dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, 0xFF3A5C7A);
        }
        float radius = 24F + (Mth.clamp(altitude, SatSettings.MIN_ALTITUDE_KM, SatSettings.MAX_ALTITUDE_KM) - SatSettings.MIN_ALTITUDE_KM)
                / (SatSettings.MAX_ALTITUDE_KM - SatSettings.MIN_ALTITUDE_KM) * 26F;
        double tilt = Math.toRadians(inclination);
        float alpha = blinkAlpha();
        int rgb = colorR << 16 | colorG << 8 | colorB;
        int line = (int) (0xFF * 0.45F * alpha) << 24 | rgb;
        for (int i = 0; i < 90; i++) {
            double a = Math.PI * 2D * i / 90D;
            int[] p = project(radius, a, tilt);
            g.fill(cx + p[0], cy + p[1], cx + p[0] + 1, cy + p[1] + 1, line);
        }
        double ticks = System.currentTimeMillis() / 50.0D;
        double base = ticks / 600.0D * -360.0D;
        double speed = Math.pow((double) SatSettings.DEFAULT_ALTITUDE_KM / Math.max(1D, altitude), 1.5D);
        double angle = Math.toRadians(SatSettings.normalizePhaseOffset(phaseOffset) + base * speed);
        int[] sat = project(radius, angle, tilt);
        g.fill(cx + sat[0] - 1, cy + sat[1] - 1, cx + sat[0] + 2, cy + sat[1] + 2, (int) (0xFF * alpha) << 24 | rgb);
    }

    private static int[] project(float radius, double angle, double tilt) {
        double c = Math.cos(angle);
        double s = Math.sin(angle) * 0.35D;
        return new int[] {(int) Math.round(radius * (c * Math.cos(tilt) - s * Math.sin(tilt))),
                (int) Math.round(radius * (c * Math.sin(tilt) + s * Math.cos(tilt)))};
    }

    private float blinkAlpha() {
        if (!blinking) return 1F;
        long cycle = (long) (SatSettings.clampBlinkPeriod(blinkPeriod) * 1000F);
        if (cycle <= 0L) return 1F;
        return 1F - (float) (System.currentTimeMillis() % cycle) / cycle;
    }

    // ---------------------------------------------------------------- hit areas

    private int sliderAt(double mouseX, double mouseY) {
        int x = (int) mouseX - left;
        int y = (int) mouseY - top;
        if (x < 12 || x >= 12 + 64) return -1;
        if (y >= 178 && y < 184) return 0;
        if (y >= 185 && y < 191) return 1;
        if (y >= 192 && y < 198) return 2;
        return -1;
    }

    private int scrollField(double mouseX, double mouseY) {
        int x = (int) mouseX - left;
        int y = (int) mouseY - top;
        if (x >= 9 && x < 111) {
            if (y >= 130 && y <= 140) return 0;
            if (detailsMode == 0) {
                if (y >= 145 && y <= 155) return 1;
                if (y >= 160 && y <= 170) return 2;
            }
        }
        if (x >= 81 && x < 111 && y >= 204 && y <= 213) return 3;
        return -1;
    }

    private boolean in(double mouseX, double mouseY, int x0, int y0, int x1, int y1) {
        int x = (int) mouseX - left;
        int y = (int) mouseY - top;
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    private boolean blinkButton(double mx, double my) {
        return in(mx, my, 114, 202, 127, 215);
    }

    private boolean batteryToggle(double mx, double my) {
        return in(mx, my, 15, 200, 44, 213);
    }

    private boolean ownerButton(double mx, double my) {
        return in(mx, my, 113, 128, 126, 141);
    }

    private boolean modeButton(double mx, double my) {
        return in(mx, my, 113, 143, 126, 156);
    }

    // ---------------------------------------------------------------- input

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || held() == null) return super.mouseClicked(event, doubleClick);
        double mx = event.x();
        double my = event.y();
        if (batteryToggle(mx, my)) {
            click();
            showDetails = !showDetails;
            return true;
        }
        if (blinkButton(mx, my)) {
            click();
            blinking = !blinking;
            pending = true;
            return true;
        }
        if (ownerButton(mx, my)) {
            click();
            String name = player.getName().getString();
            if (!name.equals(owner)) {
                owner = name;
                pending = true;
            }
            return true;
        }
        if (modeButton(mx, my)) {
            click();
            detailsMode = detailsMode == 0 ? 1 : 0;
            return true;
        }
        draggedSlider = sliderAt(mx, my);
        if (draggedSlider >= 0) {
            updateSlider(draggedSlider, mx);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && draggedSlider >= 0) {
            updateSlider(draggedSlider, event.x());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggedSlider >= 0) draggedSlider = -1;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int field = scrollField(mouseX, mouseY);
        if (field < 0 || scrollY == 0 || held() == null) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        adjust(field, scrollY > 0 ? 1 : -1);
        return true;
    }

    private void updateSlider(int slider, double mouseX) {
        int value = Math.round(Mth.clamp((int) mouseX - left - 12, 0, 62) * 255F / 62);
        switch (slider) {
            case 0 -> {
                if (colorR != value) { colorR = value; pending = true; }
            }
            case 1 -> {
                if (colorG != value) { colorG = value; pending = true; }
            }
            default -> {
                if (colorB != value) { colorB = value; pending = true; }
            }
        }
    }

    private void adjust(int field, int delta) {
        if (detailsMode != 0) {
            if (field == 0) adjustPhase(delta);
            return;
        }
        switch (field) {
            case 0 -> adjustAltitude(-delta); // speed is the inverse of the altitude
            case 1 -> adjustAltitude(delta);
            case 2 -> adjustInclination(delta);
            case 3 -> adjustBlink(delta);
            default -> {}
        }
    }

    private void adjustAltitude(int delta) {
        float step = hasShiftDown() ? 5F : 1F;
        float value = Mth.clamp(altitude + (delta > 0 ? step : -step), SatSettings.MIN_ALTITUDE_KM, SatSettings.MAX_ALTITUDE_KM);
        if (value == altitude) return;
        altitude = value;
        pending = true;
    }

    private void adjustInclination(int delta) {
        float step = hasShiftDown() ? 15F : 1F;
        float value = Mth.clamp(inclination + (delta > 0 ? step : -step), SatSettings.MIN_INCLINATION, SatSettings.MAX_INCLINATION);
        if (value == inclination) return;
        inclination = value;
        pending = true;
    }

    private void adjustPhase(int delta) {
        float step = hasShiftDown() ? 15F : 1F;
        float value = SatSettings.normalizePhaseOffset(phaseOffset + (delta > 0 ? step : -step));
        if (Math.abs(value - phaseOffset) < 0.0005F) return;
        phaseOffset = value;
        pending = true;
    }

    private void adjustBlink(int delta) {
        float value = SatSettings.clampBlinkPeriod(Math.round((blinkPeriod + (delta > 0 ? 0.1F : -0.1F)) * 10F) / 10F);
        if (value == blinkPeriod) return;
        blinkPeriod = value;
        pending = true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft != null && WidgetBridge.matches(minecraft.options.keyInventory, event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    /** The changes go to the server, and show on the item here at once, when the screen goes. */
    @Override
    public void removed() {
        if (pending) {
            ItemStack stack = held();
            if (stack != null) {
                CompoundTag data = new CompoundTag();
                data.putString("satOwner", owner);
                data.putFloat("satAltitude", altitude);
                data.putFloat("satInclination", inclination);
                data.putFloat("satPhaseOffset", phaseOffset);
                data.putBoolean("satIsBlinking", blinking);
                data.putFloat("satBlink", blinkPeriod);
                data.putInt("satColorR", colorR);
                data.putInt("satColorG", colorG);
                data.putInt("satColorB", colorB);
                stack.set(SatSettings.COMPONENT.get(), new SatSettings(inclination, altitude, phaseOffset, blinking, blinkPeriod, owner,
                        colorR / 255F, colorG / 255F, colorB / 255F).sanitized());
                PacketWire.sendToServer(new ItemControlPayload(data));
            }
            pending = false;
        }
        super.removed();
    }
}
