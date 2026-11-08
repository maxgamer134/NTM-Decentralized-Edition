// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.client.render.flywheel;

import com.hbm.config.RenderConfig;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Hides the Flywheel instances of machines that are farther from the camera than
 * {@code machine_instance_cull_distance}. Only drawing stops; block entities and the server keep
 * processing. Power poles opt out through {@link HbmBlockEntityVisual#cullable()}.
 */
@EventBusSubscriber(modid = "hbm", value = Dist.CLIENT)
public final class MachineInstanceCulling {

    private static final Set<HbmBlockEntityVisual<?>> LIVE = ConcurrentHashMap.newKeySet();
    private static final int INTERVAL = 10;
    private static int counter;

    private MachineInstanceCulling() {}

    static void register(HbmBlockEntityVisual<?> visual) {
        LIVE.add(visual);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (++counter < INTERVAL) return;
        counter = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null || mc.level == null) {
            LIVE.clear();
            return;
        }
        double limit = RenderConfig.machineInstanceCullDistance;
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        Iterator<HbmBlockEntityVisual<?>> it = LIVE.iterator();
        while (it.hasNext()) {
            HbmBlockEntityVisual<?> visual = it.next();
            if (visual.isDeletedVisual()) {
                it.remove();
                continue;
            }
            if (!visual.isCullable()) continue;
            var pos = visual.cullPos();
            boolean far =
                    limit > 0
                            && camera.distanceToSqr(
                                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                                    > limit * limit;
            visual.setCulled(far);
        }
    }
}
