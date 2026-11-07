// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import static com.hbm.render.item.weapon.legacy.LegacyGunRenderer.rot;

import com.hbm.backport.client.blockmodel.ResolvableModel;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.itemmodel.ItemModel;
import com.hbm.backport.client.itemmodel.SpecialModelRenderer;
import com.hbm.render.item.weapon.legacy.LegacyGunRenderer;
import com.hbm.render.item.weapon.legacy.LegacyGunRenderer.Ctx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * backport-fix: BF-161 draws the pre-rework guns through main's IItemRenderer code. The base
 * model has no display transforms, so submit starts in the modern item frame and rebuilds the
 * 1.7.10 one: ItemRenderer's first-person hold, RenderPlayer's held-item chain, the 16-pixel GUI
 * slot, and Forge's renderEquippedItem for both held cases.
 */
public final class LegacyGunItemRenderer implements SpecialModelRenderer<LegacyGunItemRenderer.Argument> {

    private static final List<Vector3fc> EXTENTS = List.of(
            new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1),
            new Vector3f(1, 1, 0), new Vector3f(1, 0, 1), new Vector3f(0, 1, 1), new Vector3f(1, 1, 1));

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        EXTENTS.forEach(output);
    }

    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        return null;
    }

    static @Nullable Argument argument(ItemStack stack, ItemDisplayContext display) {
        LegacyGunRenderer renderer = LegacyGunRenderer.get(stack.getItem());
        if (renderer == null) return null;
        Ctx ctx = switch (display) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> Ctx.FIRST_PERSON;
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> Ctx.EQUIPPED;
            case GUI -> Ctx.INVENTORY;
            default -> Ctx.ENTITY;
        };
        return new Argument(renderer, ctx, display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND, stack);
    }

    @Override
    public void submit(
            @Nullable Argument arg,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {
        if (arg == null) return;
        pose.pushPose();
        switch (arg.ctx) {
            case FIRST_PERSON -> {
                // modern applyItemArmTransform is 1.7.10's (0.56, -0.52, -0.72); the rest of the hold
                pose.translate(0.5F, 0.5F, 0.5F);
                rot(pose, 45, 0, 1, 0);
                pose.scale(0.4F, 0.4F, 0.4F);
                forgeEquipped(pose);
            }
            case EQUIPPED -> {
                pose.translate(0.5F, 0.5F, 0.5F);
                // undo ItemInHandLayer's Rx(-90) Ry(180) T(+-1/16, 0.125, -0.625)
                pose.translate(-(arg.leftHand ? -1F : 1F) / 16F, -0.125F, 0.625F);
                rot(pose, -180, 0, 1, 0);
                rot(pose, 90, 1, 0, 0);
                // RenderPlayer.renderEquippedItems for a flat (not full 3D) item
                pose.translate(-0.0625F, 0.4375F, 0.0625F);
                pose.translate(0.25F, 0.1875F, -0.1875F);
                pose.scale(0.375F, 0.375F, 0.375F);
                rot(pose, 60, 0, 0, 1);
                rot(pose, -90, 1, 0, 0);
                rot(pose, 20, 0, 0, 1);
                forgeEquipped(pose);
            }
            case INVENTORY -> {
                // 1.7.10's slot: 16 pixels, origin top left, y down
                pose.translate(0F, 1F, 0F);
                pose.scale(1F / 16F, -1F / 16F, 1F / 16F);
            }
            case ENTITY -> pose.translate(0.5F, 0.5F, 0.5F);
        }
        arg.renderer.render(arg.ctx, arg.stack, pose, collector, light);
        pose.popPose();
    }

    /** ForgeHooksClient.renderEquippedItem without the EQUIPPED_BLOCK helper */
    private static void forgeEquipped(PoseStack pose) {
        pose.translate(0.0F, -0.3F, 0.0F);
        pose.scale(1.5F, 1.5F, 1.5F);
        rot(pose, 50, 0, 1, 0);
        rot(pose, 335, 0, 0, 1);
        pose.translate(-0.9375F, -0.0625F, 0.0F);
    }

    public record Argument(LegacyGunRenderer renderer, Ctx ctx, boolean leftHand, ItemStack stack) {}

    public record Unbaked(ResourceLocation base) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(ResourceLocation.CODEC.fieldOf("base").forGetter(Unbaked::base))
                .apply(instance, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new DynamicSpecialWrapper<>(
                    new LegacyGunItemRenderer(),
                    (stack, display, owner) -> argument(stack, display),
                    true,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
