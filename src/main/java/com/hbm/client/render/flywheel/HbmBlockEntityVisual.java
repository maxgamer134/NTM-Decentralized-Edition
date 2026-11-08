// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import com.hbm.backport.client.flywheel.AbstractBlockEntityVisual;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;

public abstract class HbmBlockEntityVisual<T extends BlockEntity>
        extends AbstractBlockEntityVisual<T> {
    private final float[] visible = new float[6];
    private boolean visibleRead;

    // backport: distance culling of far machines. Every instance a visual creates is recorded, and
    // MachineInstanceCulling hides them all while the camera is far away. The block entity and the
    // server keep running; only the drawing stops.
    private final java.util.List<dev.engine_room.flywheel.api.instance.Instance> tracked =
            new java.util.ArrayList<>();
    private final java.util.IdentityHashMap<dev.engine_room.flywheel.api.instance.Instance, Boolean>
            wasVisible = new java.util.IdentityHashMap<>();
    private dev.engine_room.flywheel.api.instance.InstancerProvider trackingProvider;
    private boolean culled;

    /** Power poles and anything else that must always draw return false. */
    protected boolean cullable() {
        return true;
    }

    final boolean isCullable() {
        return cullable() && !embedded && !deleted;
    }

    final boolean isDeletedVisual() {
        return deleted;
    }

    final net.minecraft.core.BlockPos cullPos() {
        return pos;
    }

    final void setCulled(boolean cull) {
        if (cull) {
            for (var instance : tracked) {
                try {
                    if (!culled) wasVisible.put(instance, instance.handle().isVisible());
                    else wasVisible.putIfAbsent(instance, instance.handle().isVisible());
                    instance.setVisible(false);
                } catch (RuntimeException ignored) {
                    // a deleted instance
                }
            }
            culled = true;
        } else if (culled) {
            for (var instance : tracked) {
                try {
                    instance.setVisible(wasVisible.getOrDefault(instance, Boolean.TRUE));
                } catch (RuntimeException ignored) {
                    // a deleted instance
                }
            }
            wasVisible.clear();
            culled = false;
        }
    }

    @Override
    protected dev.engine_room.flywheel.api.instance.InstancerProvider instancerProvider() {
        if (trackingProvider == null) {
            var delegate = super.instancerProvider();
            trackingProvider =
                    new dev.engine_room.flywheel.api.instance.InstancerProvider() {
                        @Override
                        public <I extends dev.engine_room.flywheel.api.instance.Instance>
                                dev.engine_room.flywheel.api.instance.Instancer<I> instancer(
                                        dev.engine_room.flywheel.api.instance.InstanceType<I> type,
                                        dev.engine_room.flywheel.api.model.Model model,
                                        int bias) {
                            var inner = delegate.instancer(type, model, bias);
                            return new dev.engine_room.flywheel.api.instance.Instancer<I>() {
                                @Override
                                public I createInstance() {
                                    I instance = inner.createInstance();
                                    tracked.add(instance);
                                    return instance;
                                }

                                @Override
                                public void createInstances(I[] instances) {
                                    inner.createInstances(instances);
                                    for (I instance : instances) tracked.add(instance);
                                }

                                @Override
                                public void stealInstance(I instance) {
                                    inner.stealInstance(instance);
                                    tracked.add(instance);
                                }
                            };
                        }
                    };
        }
        return trackingProvider;
    }

    /**
     * backport: Sable (Create: Aeronautics) puts the visuals of block entities on a physics
     * sub-level into a Flywheel VisualEmbedding whose positions are plot coordinates under the
     * sub-level's pose, so the world-space frustum and camera-distance tests say nothing about
     * them (NTM's dynamic visuals froze or updated at random there: tank textures, hazard
     * diamonds, animations). Embedded visuals skip both tests.
     */
    protected final boolean embedded;

    protected HbmBlockEntityVisual(VisualizationContext context, T blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        this.embedded = context instanceof dev.engine_room.flywheel.api.visualization.VisualEmbedding;
        MachineInstanceCulling.register(this);
    }

    public static boolean hasVisual(BlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        return VisualizationManager.supportsVisualization(level)
                && level.getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }

    protected AABB visibleBounds() {
        return getRenderBoundingBox();
    }

    protected final void refreshVisibleBounds() {
        visibleRead = false;
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        if (embedded) return true;
        if (!visibleRead) {
            AABB bounds = visibleBounds();
            var origin = renderOrigin();
            visible[0] = (float) (bounds.minX - origin.getX());
            visible[1] = (float) (bounds.minY - origin.getY());
            visible[2] = (float) (bounds.minZ - origin.getZ());
            visible[3] = (float) (bounds.maxX - origin.getX());
            visible[4] = (float) (bounds.maxY - origin.getY());
            visible[5] = (float) (bounds.maxZ - origin.getZ());
            visibleRead = true;
        }
        return frustum.testAab(
                visible[0], visible[1], visible[2], visible[3], visible[4], visible[5]);
    }

    @Override
    public void updateLight(float partialTick) {}
}
