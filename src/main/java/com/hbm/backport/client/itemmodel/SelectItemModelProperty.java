// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.itemmodel;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 26.x {@code net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty}. */
public interface SelectItemModelProperty<T> {
    @Nullable T get(
            ItemStack stack,
            @Nullable ClientLevel level,
            @Nullable LivingEntity owner,
            int seed,
            ItemDisplayContext displayContext);

    Codec<T> valueCodec();

    Type<? extends SelectItemModelProperty<T>, T> type();

    record Type<P extends SelectItemModelProperty<T>, T>(
            MapCodec<SelectItemModel.UnbakedSwitch<P, T>> switchCodec) {

        public static <P extends SelectItemModelProperty<T>, T> Type<P, T> create(
                MapCodec<P> propertyMapCodec, Codec<T> valueCodec) {
            Codec<SelectItemModel.SwitchCase<T>> caseCodec = SelectItemModel.SwitchCase.codec(valueCodec);
            MapCodec<SelectItemModel.UnbakedSwitch<P, T>> switchCodec =
                    RecordCodecBuilder.mapCodec(
                            i ->
                                    i.group(
                                                    propertyMapCodec.forGetter(
                                                            SelectItemModel.UnbakedSwitch::property),
                                                    lenientCases(caseCodec)
                                                            .fieldOf("cases")
                                                            .forGetter(SelectItemModel.UnbakedSwitch::cases))
                                            .apply(i, SelectItemModel.UnbakedSwitch::new));
            return new Type<>(switchCodec);
        }

        // backport-fix: a case whose value does not decode (a bedrock ore type of NTM:Space while
        // Space is off) is skipped instead of failing the whole item definition, so the cases that
        // do exist keep their models
        private static <C> Codec<List<C>> lenientCases(Codec<C> caseCodec) {
            Codec<Optional<C>> optional =
                    new Codec<>() {
                        @Override
                        public <D> DataResult<Pair<Optional<C>, D>> decode(DynamicOps<D> ops, D input) {
                            return caseCodec
                                    .decode(ops, input)
                                    .result()
                                    .map(p -> DataResult.success(p.mapFirst(Optional::of)))
                                    .orElseGet(() -> DataResult.success(Pair.of(Optional.empty(), ops.empty())));
                        }

                        @Override
                        public <D> DataResult<D> encode(Optional<C> input, DynamicOps<D> ops, D prefix) {
                            return input.map(c -> caseCodec.encode(c, ops, prefix))
                                    .orElseGet(() -> DataResult.success(prefix));
                        }
                    };
            return optional.listOf()
                    .xmap(
                            list -> list.stream().flatMap(Optional::stream).toList(),
                            list -> list.stream().map(Optional::of).toList());
        }
    }
}
