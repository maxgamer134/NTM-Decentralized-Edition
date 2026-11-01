// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

public abstract class TablePage<T extends GenericRecipe> extends RecipePage<T> {

    protected TablePage(ResourceLocation id, Class<? extends T> rowType) {
        super(id, rowType);
    }

    protected TablePage(
            ResourceLocation id, Class<? extends T> rowType, ItemLike machine, String titleKey) {
        super(id, rowType, machine, titleKey);
    }

    @Override
    public ResourceLocation rowId(T row) {
        return row.recipeId().location();
    }
}
