// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.blockmodel.ResolvableModel;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.itemmodel.ItemModel;
import com.hbm.backport.client.itemmodel.SpecialModelRenderer;
import com.hbm.client.render.DynamicSpecialWrapper;
import com.hbm.client.render.MeshItemRenderer;
import com.hbm.space.rocket.ItemCustomRocket;
import com.hbm.space.rocket.RocketStruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** The custom rocket as an item: the rocket it holds, drawn stage by stage, and turning in the inventory. */
public final class RocketCustomItemRenderer implements SpecialModelRenderer<RocketCustomItemRenderer.Argument> {

    private static final MeshItemRenderer.Spin CLOCK = new MeshItemRenderer.Spin(25L, 360L, 1F, Optional.empty());
    private static final float EMPTY_HEIGHT = 4F;
    private static final float GUI_SIZE = 20F;

    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        return null;
    }

    @Override
    public void submit(@Nullable Argument argument, PoseStack pose, SubmitNodeCollector collector,
            int light, int overlay, boolean foil, int outline) {
        if (argument == null) return;
        RocketPronter.pront(argument.rocket(), pose, collector, light, false);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) output.accept(new Vector3f(x * 4F - 2F, y * 20F, z * 4F - 2F));
    }

    private @Nullable Matrix4fc poseFor(Argument argument, ItemDisplayContext context, @Nullable Matrix4fc prefix) {
        if (context != ItemDisplayContext.GUI) return prefix;
        float height = (float) argument.rocket().getHeight();
        if (height == 0F) height = EMPTY_HEIGHT;
        float scale = GUI_SIZE / height;
        return (prefix != null ? new Matrix4f(prefix) : new Matrix4f())
                .scale(-scale, -scale, -scale)
                .rotate((float) Math.toRadians(argument.spin()), 0F, -1F, 0F);
    }

    public record Argument(RocketStruct rocket, float spin) {}

    public record Unbaked(ResourceLocation base) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(ResourceLocation.CODEC.fieldOf("base").forGetter(Unbaked::base)).apply(instance, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            RocketCustomItemRenderer renderer = new RocketCustomItemRenderer();
            return new DynamicSpecialWrapper<>(
                    renderer,
                    (stack, display, owner) -> {
                        RocketStruct rocket = ItemCustomRocket.get(stack);
                        return rocket == null ? null : new Argument(rocket, CLOCK.degrees());
                    },
                    renderer::poseFor,
                    true,
                    DynamicSpecialWrapper.properties(context, base),
                    Map.of());
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
