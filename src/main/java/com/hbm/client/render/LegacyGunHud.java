// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.items.weapon.legacy.BulletConfigSyncingUtil;
import com.hbm.items.weapon.legacy.BulletConfiguration;
import com.hbm.items.weapon.legacy.GunConfiguration;
import com.hbm.items.weapon.legacy.ItemGunBase;
import com.hbm.items.weapon.legacy.ItemGunBio;
import com.hbm.render.util.RenderScreenOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's ItemGunBase.renderHUD (RenderScreenOverlay.renderAmmo
 * and the crosshair) and the client half of ItemGunBase.onUpdate.
 */
public final class LegacyGunHud {

    private LegacyGunHud() {}

    /** main's updateClient for the held gun */
    public static void clientTick() {
        Player player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().isPaused()) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof ItemGunBio) ItemGunBio.updateSmoke(player, stack, player.getRandom());
    }

    public static boolean renderCrosshair(GuiGraphicsExtractor graphics, Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof ItemGunBase gun)) return false;
        if (!(gun.mainConfig.hasSights && player.isShiftKeyDown()) && gun.mainConfig.crosshair != null)
            RenderScreenOverlay.renderCustomCrosshairs(graphics, gun.mainConfig.crosshair);
        return true;
    }

    public static boolean renderAmmo(GuiGraphicsExtractor graphics, Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof ItemGunBase gun)) return false;
        GunConfiguration gcfg = gun.mainConfig;

        BulletConfiguration bcfg;
        int count;
        int max;
        if (gcfg.reloadType == GunConfiguration.RELOAD_NONE) {
            bcfg = ItemGunBase.getBeltType(player, stack, true);
            count = ItemGunBase.getBeltSize(player, bcfg);
            max = -1;
        } else {
            int type = ItemGunBase.getMagType(stack);
            bcfg = type < gcfg.config.size() ? BulletConfigSyncingUtil.pullConfig(gcfg.config.get(type)) : null;
            count = ItemGunBase.getMag(stack);
            max = gcfg.ammoCap;
        }
        if (bcfg == null) return true;

        int dura = gcfg.durability > 0 ? ItemGunBase.getItemWear(stack) * 50 / gcfg.durability : 0;
        dura = Math.max(0, Math.min(50, dura));

        int pX = graphics.guiWidth() / 2 + 62 + 36;
        int pZ = graphics.guiHeight() - 21;
        graphics.blit(RenderPipelines.GUI_TEXTURED, RenderScreenOverlay.misc, pX, pZ + 16, 94, 0, 52, 3, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, RenderScreenOverlay.misc, pX + 1, pZ + 16, 95, 3, 50 - dura, 3, 256, 256);

        String cap = max == -1 ? "∞" : String.valueOf(max);
        if (gcfg.showAmmo)
            graphics.text(Minecraft.getInstance().font, count + " / " + cap, pX + 16, pZ + 6, 0xFFFFFFFF, true);
        graphics.item(bcfg.ammoStack(), pX, pZ);
        return true;
    }
}
