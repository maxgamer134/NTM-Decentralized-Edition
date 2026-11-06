// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.legacy;

import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's IItemRenderer for the pre-rework guns. {@link
 * #render} runs in main's own frame for each ItemRenderType; LegacyGunItemRenderer builds that
 * frame from the modern display context.
 */
public abstract class LegacyGunRenderer {

    public enum Ctx {
        FIRST_PERSON,
        EQUIPPED,
        ENTITY,
        INVENTORY
    }

    private static final Map<Item, LegacyGunRenderer> REGISTRY = new IdentityHashMap<>();

    public static void register(Supplier<? extends Item> item, LegacyGunRenderer renderer) {
        REGISTRY.put(item.get(), renderer);
    }

    public static @Nullable LegacyGunRenderer get(Item item) {
        return REGISTRY.get(item);
    }

    /** main's renderItem, already inside its push/pop */
    public abstract void render(Ctx ctx, ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light);

    /** whether main culls back faces for this gun (the bio revolver turns culling off) */
    protected boolean cull() {
        return true;
    }

    protected RenderType type(ResourceLocation tex) {
        return cull() ? RenderTypes.entityCutoutCull(tex) : RenderTypes.entityCutoutNoCull(tex);
    }

    protected void part(
            SubmitNodeCollector collector, PoseStack pose, HFRWavefrontObject model, ResourceLocation tex, int part, int light) {
        collector.submitCustomGeometry(pose, type(tex), (p, buffer) -> model.renderPart(p, buffer, light, -1, part));
    }

    protected void all(SubmitNodeCollector collector, PoseStack pose, HFRWavefrontObject model, ResourceLocation tex, int light) {
        collector.submitCustomGeometry(pose, type(tex), (p, buffer) -> model.render(p, buffer, light, -1));
    }

    /** GL11.glRotated */
    public static void rot(PoseStack pose, double deg, float x, float y, float z) {
        pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(deg), x, y, z));
    }

    /** GL11.glScaled with one factor */
    public static void scale(PoseStack pose, double s) {
        pose.scale((float) s, (float) s, (float) s);
    }
}
