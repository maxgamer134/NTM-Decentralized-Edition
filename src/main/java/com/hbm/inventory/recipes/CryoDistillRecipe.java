// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.hbm.backport.RecipeSerializers;

public class CryoDistillRecipe extends GenericRecipe {

    public static final MapCodec<CryoDistillRecipe> MAP_CODEC =
            GenericRecipe.mapCodec(CryoDistillRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, CryoDistillRecipe> STREAM_CODEC =
            GenericRecipe.streamCodec(CryoDistillRecipe::new);
    public static final RecipeSerializer<CryoDistillRecipe> SERIALIZER =
            RecipeSerializers.of(MAP_CODEC, STREAM_CODEC);

    public CryoDistillRecipe(String name) {
        super(name);
    }

    @Override
    public GenericRecipes<?, ?> table() {
        return CryoDistillRecipes.INSTANCE;
    }
}
