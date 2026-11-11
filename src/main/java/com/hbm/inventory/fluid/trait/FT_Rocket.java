// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * NTM:Space's rocket fuel grade. The specific impulse sets how far a bucket takes a rocket (its
 * delta-v, in transfer burns and station engines); the thrust sets how much a bucket lifts off a
 * body's surface (its thrust-to-weight, which decides gravity losses on the way to orbit).
 */
public class FT_Rocket extends FluidTrait {

    private final int isp;
    private final long thrust;

    public FT_Rocket(int isp, long thrust) {
        this.isp = isp;
        this.thrust = thrust;
    }

    public int getISP() {
        return isp;
    }

    public long getThrust() {
        return thrust;
    }

    @Override
    public void addInfo(List<Component> info) {
        info.add(Component.translatable("hbmfluid.trait.rocket").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (isp > 0) info.add(line("hbmfluid.trait.rocket.isp", BobMathUtil.getShortNumber(isp) + " ISP"));
        info.add(Component.translatable("hbmfluid.trait.rocket.thrust").withStyle(ChatFormatting.RED));
        if (thrust > 0) info.add(line("hbmfluid.trait.rocket.thrust_per", BobMathUtil.getShortNumber(thrust) + " N"));
    }

    private static Component line(String key, String value) {
        return Component.translatable(key, Component.literal(value).withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.YELLOW);
    }
}
