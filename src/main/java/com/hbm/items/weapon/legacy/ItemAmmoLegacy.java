// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.backport.TooltipDisplay;
import com.hbm.backport.compat.ItemCompat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * backport-fix: BF-161 one variant of main 1.0.27_X5000's ItemAmmo (an ItemEnumMulti there, one
 * item per variant here), with main's trait lines under the name.
 */
public class ItemAmmoLegacy extends ItemCompat {

    private final AmmoItemTrait[] traits;

    public ItemAmmoLegacy(Properties properties, AmmoItemTrait... traits) {
        super(properties);
        this.traits = traits.clone();
        Arrays.sort(this.traits, Comparator.reverseOrder());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        for (AmmoItemTrait trait : traits) {
            ChatFormatting color =
                    switch (trait.name().substring(0, 3)) {
                        case "PRO" -> ChatFormatting.BLUE;
                        case "NEU" -> ChatFormatting.YELLOW;
                        case "CON" -> ChatFormatting.RED;
                        default -> ChatFormatting.DARK_GRAY;
                    };
            tooltip.accept(Component.translatable(trait.key).withStyle(color));
        }
    }

    public enum AmmoItemTrait {
        CON_ACCURACY2,
        CON_DAMAGE,
        CON_HEAVY_WEAR,
        CON_LING_FIRE,
        CON_NN,
        CON_NO_DAMAGE,
        CON_NO_EXPLODE1,
        CON_NO_EXPLODE2,
        CON_NO_EXPLODE3,
        CON_NO_FIRE,
        CON_NO_MIRV,
        CON_NO_PROJECTILE,
        CON_PENETRATION,
        CON_RADIUS,
        CON_RANGE2,
        CON_SING_PROJECTILE,
        CON_SPEED,
        CON_SUPER_WEAR,
        CON_WEAR,
        NEU_40MM,
        NEU_BLANK,
        NEU_BOAT,
        NEU_BOXCAR,
        NEU_BUILDING,
        NEU_CHLOROPHYTE,
        NEU_ERASER,
        NEU_FUN,
        NEU_HEAVY_METAL,
        NEU_HOMING,
        NEU_JOLT,
        NEU_LESS_BOUNCY,
        NEU_MASKMAN_FLECHETTE,
        NEU_MASKMAN_METEORITE,
        NEU_MORE_BOUNCY,
        NEU_NO_BOUNCE,
        NEU_NO_CON,
        NEU_STARMETAL,
        NEU_TRACER,
        NEU_UHH,
        NEU_LEADBURSTER,
        NEU_WARCRIME1,
        NEU_WARCRIME2,
        PRO_ACCURATE1,
        PRO_ACCURATE2,
        PRO_BALEFIRE,
        PRO_BOMB_COUNT,
        PRO_CAUSTIC,
        PRO_CHAINSAW,
        PRO_CHLORINE,
        PRO_DAMAGE,
        PRO_DAMAGE_SLIGHT,
        PRO_EMP,
        PRO_EXPLOSIVE,
        PRO_FALLOUT,
        PRO_FIT_357,
        PRO_FLAMES,
        PRO_GRAVITY,
        PRO_HEAVY_DAMAGE,
        PRO_INCENDIARY,
        PRO_LUNATIC,
        PRO_MARAUDER,
        PRO_MINING,
        PRO_NO_GRAVITY,
        PRO_NUCLEAR,
        PRO_PENETRATION,
        PRO_PERCUSSION,
        PRO_PHOSPHORUS,
        PRO_PHOSPHORUS_SPLASH,
        PRO_POISON_GAS,
        PRO_RADIUS,
        PRO_RADIUS_HIGH,
        PRO_RANGE,
        PRO_ROCKET,
        PRO_ROCKET_PROPELLED,
        PRO_SHRAPNEL,
        PRO_SPEED,
        PRO_STUNNING,
        PRO_TOXIC,
        PRO_WEAR,
        PRO_WITHERING;

        public final String key = "desc.item.ammo." + name().toLowerCase(Locale.US);
    }
}
