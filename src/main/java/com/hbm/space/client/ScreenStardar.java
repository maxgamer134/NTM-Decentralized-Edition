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
import com.hbm.space.SolarSystem;
import com.hbm.space.rocket.ItemVOTVdrive;
import com.hbm.space.rocket.SpaceRockets;
import com.hbm.space.rocket.SpaceStardar;
import com.hbm.space.rocket.SpaceStardar.StardarBE;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The StarDar's screen. The system map shows the bodies on their orbits (drag to pan, scroll to
 * zoom, click a body to focus it, click it again with an empty drive in to program it); with a
 * programmed drive for a landable body it shows that body's surface, where a click on flat
 * ground sets the landing point. NTM:Space drew the bodies with shaders; here they are flat discs.
 */
public class ScreenStardar extends ScreenInfoContainer<SpaceStardar.Menu> {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/machine/gui_stardar.png");
    private static final ResourceLocation STARMAP = Library.id("textures/gui/starmap3.png");
    private static final ResourceLocation GROUND = Library.id("stardar_ground_map");
    private static final int W = 176, H = 256;
    private static final int MAP_X = 9, MAP_Y = 9, MAP_W = 158, MAP_H = 108;
    private static final float ISO = 0.5F;

    private record Drawn(CelestialBody body, float x, float y, float r) {}

    private final List<Drawn> drawn = new ArrayList<>();
    private @Nullable CelestialBody focused, hovered, landingBody;
    private boolean landingMode;
    private float panX, panY, zoom = 1;
    private float surfaceX, surfaceY;
    private double pressX, pressY;
    private boolean pressedInMap;
    private @Nullable DynamicTexture ground;
    private int groundVersion = -1;

    public ScreenStardar(SpaceStardar.Menu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private StardarBE be() {
        return menu.blockEntity();
    }

    private ItemStack drive() {
        return menu.getSlot(0).getItem();
    }

    private boolean inMap(double x, double y) {
        return x >= leftPos + MAP_X && x < leftPos + MAP_X + MAP_W && y >= topPos + MAP_Y && y < topPos + MAP_Y + MAP_H;
    }

    private void send(CompoundTag data) {
        PacketWire.sendToServer(new NbtControlPayload(be().getBlockPos(), data));
    }

    private void program(CelestialBody body) {
        CompoundTag data = new CompoundTag();
        data.putString("pid", body.name);
        if (body == CelestialBody.getBody(minecraft.level) && !CelestialBody.inOrbit(minecraft.level)) {
            data.putInt("ix", be().getBlockPos().getX());
            data.putInt("iz", be().getBlockPos().getZ());
        }
        send(data);
    }

    private void enterLanding(CelestialBody body) {
        landingMode = true;
        landingBody = body;
        surfaceX = surfaceY = 0;
    }

    private void updateLandingMode() {
        ItemStack drive = drive();
        if (drive.getItem() instanceof ItemVOTVdrive) {
            CelestialBody body = ItemVOTVdrive.getBody(drive).getBody();
            if (body != null && body.canLand) {
                if (!landingMode || landingBody != body) enterLanding(body);
            } else {
                landingMode = false;
            }
        } else if (!drive.is(SpaceRockets.HARD_DRIVE.get())) {
            landingMode = false;
        }
    }

    // input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();
        pressX = event.x();
        pressY = event.y();
        pressedInMap = false;
        if (event.button() == 0) {
            if (checkClick(x, y, 129, 124, 18, 18)) {
                playClick();
                landingMode = false;
                focused = CelestialBody.getBody(minecraft.level);
                zoom = 4;
                return true;
            }
            if (checkClick(x, y, 129, 143, 18, 18)) {
                playClick();
                CompoundTag data = new CompoundTag();
                data.putString("pid", "");
                data.putBoolean("orbit", true);
                send(data);
                return true;
            }
            if (checkClick(x, y, 149, 143, 18, 18)) {
                playClick();
                program(CelestialBody.getBody(minecraft.level));
                return true;
            }
        }
        if (!inMap(x, y)) return super.mouseClicked(event, doubleClick);
        pressedInMap = true;
        if (event.button() == 1) {
            if (landingMode) landingMode = false;
            else {
                focused = null;
                zoom = 1;
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (pressedInMap && event.button() == 0) {
            if (landingMode) {
                surfaceX = Mth.clamp(surfaceX + (float) dx, -256 + MAP_W, 256);
                surfaceY = Mth.clamp(surfaceY + (float) dy, -256 + MAP_H, 256);
            } else if (focused == null) {
                panX += (float) dx / zoom;
                panY += (float) dy / zoom;
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean click = pressedInMap && event.button() == 0 && Math.abs(event.x() - pressX) <= 2 && Math.abs(event.y() - pressY) <= 2;
        pressedInMap = false;
        if (!click) return super.mouseReleased(event);
        if (landingMode) {
            int[] map = be().heightmap;
            int hx = (int) (event.x() - surfaceX - leftPos - MAP_X + 256) / 2;
            int hz = (int) (event.y() - surfaceY - topPos - MAP_Y + 256) / 2;
            if (map != null && landingInfo(map, hx, hz) == null) {
                playClick();
                CompoundTag data = new CompoundTag();
                data.putInt("px", hx);
                data.putInt("pz", hz);
                send(data);
            }
            return true;
        }
        if (hovered != null) {
            if (hovered == focused) {
                if (hovered.canLand && drive().is(SpaceRockets.HARD_DRIVE.get())) {
                    playClick();
                    program(hovered);
                    enterLanding(hovered);
                }
            } else if (hovered.parent != null) {
                focused = hovered;
                zoom = Math.max(zoom, 4);
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!landingMode && inMap(mouseX, mouseY) && scrollY != 0) {
            zoom = Mth.clamp(zoom * (scrollY > 0 ? 1.1F : 1 / 1.1F), 0.5F, 180F);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // drawing

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + x, topPos + y, u, v, w, h, 256, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        updateLandingMode();
        blit(graphics, 0, 0, 0, 0, W, H);
        int x0 = leftPos + MAP_X, y0 = topPos + MAP_Y;
        graphics.enableScissor(x0, y0, x0 + MAP_W, y0 + MAP_H);
        graphics.blit(STARMAP, x0, y0, x0 + MAP_W, y0 + MAP_H, 0F, MAP_W / 1024F, 0F, MAP_H / 1024F);
        if (landingMode) drawSurface(graphics, mouseX, mouseY);
        else drawSystem(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();
    }

    private void drawSystem(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawn.clear();
        hovered = null;
        List<SolarSystem.OrreryMetric> metrics = SolarSystem.calculatePositionsOrrery(minecraft.level, partialTick);
        double reach = 1;
        for (SolarSystem.OrreryMetric m : metrics) reach = Math.max(reach, Math.hypot(m.position.x, m.position.z));
        float scale = (float) ((MAP_W / 2F - 6) / reach) * zoom;

        Vec3 centre = Vec3.ZERO;
        if (focused != null)
            for (SolarSystem.OrreryMetric m : metrics) if (m.body == focused) centre = m.position;
        float cx = leftPos + MAP_X + MAP_W / 2F + (focused == null ? panX * zoom : 0);
        float cy = topPos + MAP_Y + MAP_H / 2F + (focused == null ? panY * zoom : 0);
        final Vec3 c = centre;
        java.util.function.Function<Vec3, float[]> project = p -> new float[] {
            cx + (float) (p.x - c.x) * scale, cy + (float) ((p.z - c.z) * ISO - (p.y - c.y)) * scale};

        for (SolarSystem.OrreryMetric m : metrics)
            for (Vec3 point : m.orbitalPath) {
                if (point == null) continue;
                float[] s = project.apply(point);
                graphics.fill((int) s[0], (int) s[1], (int) s[0] + 1, (int) s[1] + 1, 0x50FFFFFF);
            }

        CelestialBody star = CelestialBody.getBody(minecraft.level).getStar();
        float[] starAt = project.apply(Vec3.ZERO);
        drawBody(graphics, star, starAt[0], starAt[1], 8 * (float) Math.sqrt(zoom));
        for (SolarSystem.OrreryMetric m : metrics) {
            float[] s = project.apply(m.position);
            float size = Mth.clamp((float) Math.cbrt(m.body.radiusKm) / 2.5F, 2F, 10F) * (float) Math.sqrt(Math.min(zoom, 16F));
            drawBody(graphics, m.body, s[0], s[1], size);
        }
        for (int i = drawn.size() - 1; i >= 0; i--) {
            Drawn d = drawn.get(i);
            if (inMap(mouseX, mouseY) && Math.hypot(mouseX - d.x, mouseY - d.y) <= d.r + 2) {
                hovered = d.body;
                break;
            }
        }
    }

    private void drawBody(GuiGraphicsExtractor graphics, CelestialBody body, float x, float y, float size) {
        float r = size / 2;
        int x0 = Math.round(x - r), y0 = Math.round(y - r), s = Math.max(2, Math.round(size));
        graphics.blit(body.texture, x0, y0, x0 + s, y0 + s, 0F, 1F, 0F, 1F);
        drawn.add(new Drawn(body, x, y, r));
    }

    private void drawSurface(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int[] map = be().heightmap;
        if (map == null) return;
        if (ground == null) {
            ground = new DynamicTexture(SpaceStardar.MAP, SpaceStardar.MAP, false);
            minecraft.getTextureManager().register(GROUND, ground);
        }
        if (groundVersion != be().heightmapVersion) {
            NativeImage pixels = ground.getPixels();
            if (pixels != null)
                for (int z = 0; z < SpaceStardar.MAP; z++)
                    for (int x = 0; x < SpaceStardar.MAP; x++)
                        pixels.setPixelRGBA(x, z, 0xFF000000 | Math.floorMod(map[z * SpaceStardar.MAP + x], 16) * 16 << 8);
            ground.upload();
            groundVersion = be().heightmapVersion;
        }
        int gx = leftPos + MAP_X + (int) surfaceX - 256, gy = topPos + MAP_Y + (int) surfaceY - 256;
        graphics.blit(GROUND, gx, gy, gx + 512, gy + 512, 0F, 1F, 0F, 1F);
        if (inMap(mouseX, mouseY)) {
            int hx = (mouseX - (int) surfaceX - leftPos - MAP_X + 256) / 2;
            int hz = (mouseY - (int) surfaceY - topPos - MAP_Y + 256) / 2;
            boolean canLand = landingInfo(map, hx, hz) == null;
            int sx = mouseX - Math.floorMod(mouseX + (int) surfaceX, 2), sy = mouseY - Math.floorMod(mouseY + (int) surfaceY, 2);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, sx - 6, sy - 6, W + (canLand ? 14 : 0), 28, 14, 14, 256, 256);
        }
    }

    private static @Nullable String landingInfo(int[] map, int x, int z) {
        if (x < 3 || x > 252 || z < 3 || z > 252) return "gui.stardar.landing.outside_bounds";
        int h = map[256 * z + x];
        for (int ox = x - 2; ox <= x + 2; ox++)
            for (int oz = z - 2; oz <= z + 2; oz++)
                if (map[256 * oz + ox] != h) return "gui.stardar.landing.area_not_flat";
        return null;
    }

    private static Component bodyName(CelestialBody body) {
        return Component.translatable("body." + body.name);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int green = 0xFF00FF00, red = 0xFFFF4040;
        if (landingMode) {
            int[] map = be().heightmap;
            if (map == null) {
                graphics.text(font, Component.translatable("gui.stardar.label.loading_surface_map"), 10, 148, green, false);
            } else if (!inMap(mouseX, mouseY)) {
                graphics.text(font, Component.translatable("gui.stardar.label.select_landing_zone"), 10, 128, green, false);
            } else {
                int hx = (mouseX - (int) surfaceX - leftPos - MAP_X + 256) / 2;
                int hz = (mouseY - (int) surfaceY - topPos - MAP_Y + 256) / 2;
                String info = landingInfo(map, hx, hz);
                graphics.text(font, Component.translatable(info == null ? "gui.stardar.label.valid_location" : info), 10, 128, info == null ? green : red, false);
                if (hx >= 0 && hx < 256 && hz >= 0 && hz < 256)
                    graphics.text(font, Component.translatable("gui.stardar.label.target_altitude", map[256 * hz + hx]), 10, 148, green, false);
            }
        } else {
            CelestialBody display = hovered != null ? hovered : focused;
            if (display == null) {
                graphics.text(font, Component.translatable("gui.stardar.label.drag_map"), 10, 128, green, false);
                graphics.text(font, Component.translatable("gui.stardar.label.click_body_focus"), 10, 148, green, false);
            } else {
                graphics.text(font, Component.translatable("gui.stardar.label.focus_body", bodyName(display)), 10, 128, green, false);
                Component second;
                int color = green;
                if (display != focused) second = Component.translatable("gui.stardar.label.click_body_focus");
                else if (!display.canLand) {
                    second = Component.translatable("gui.stardar.label.cant_land");
                    color = red;
                } else if (drive().is(SpaceRockets.HARD_DRIVE.get())) second = Component.translatable("gui.stardar.label.click_again_land");
                else second = Component.translatable("gui.stardar.label.insert_drive");
                graphics.text(font, second, 10, 148, color, false);
            }
        }
        super.extractLabels(graphics, mouseX, mouseY);
        drawCustomInfoStat(graphics, mouseX, mouseY, 129, 124, 18, 18, List.of(Component.translatable("gui.stardar.tooltip.focus_current_body")));
        drawCustomInfoStat(graphics, mouseX, mouseY, 129, 143, 18, 18, List.of(Component.translatable("gui.stardar.tooltip.program_new_station")));
        drawCustomInfoStat(graphics, mouseX, mouseY, 149, 143, 18, 18, List.of(Component.translatable("gui.stardar.tooltip.program_current_body")));
    }

    @Override
    public void removed() {
        super.removed();
        if (ground != null) {
            minecraft.getTextureManager().release(GROUND);
            ground = null;
        }
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }
}
