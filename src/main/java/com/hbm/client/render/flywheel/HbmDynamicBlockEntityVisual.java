// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class HbmDynamicBlockEntityVisual<T extends BlockEntity>
        extends HbmBlockEntityVisual<T> implements SimpleDynamicVisual {

    protected HbmDynamicBlockEntityVisual(
            VisualizationContext context, T blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @Override
    public final void beginFrame(Context context) {
        trackExtent();
        // backport: embedded (Sable sub-level) visuals skip the culling tests, see HbmBlockEntityVisual
        if (!embedded && (doDistanceLimitThisFrame(context) || !isVisible(context.frustum()))) return;
        // backport: machines past MACHINE_CULL_DISTANCE blocks stop their per-frame work; pylon
        // wires are not dynamic visuals, so they are never skipped
        if (!embedded
                && context.camera().getPosition().distanceToSqr(
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                        > MACHINE_CULL_DISTANCE * MACHINE_CULL_DISTANCE) {
            return;
        }
        frame(context);
    }

    private static final double MACHINE_CULL_DISTANCE = 64.0;

    protected void trackExtent() {}

    protected abstract void frame(Context context);
}
