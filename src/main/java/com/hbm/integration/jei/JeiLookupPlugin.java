// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.jei;

import com.hbm.integration.recipeviewer.RecipePage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.world.item.ItemStack;

final class JeiLookupPlugin<T> implements ISimpleRecipeManagerPlugin<T> {

    private final RecipePage<T> page;
    private final JeiPageCategory<T> category;
    private final RecipePage.LookupFilter filter;
    private final IIngredientManager ingredients;
    private final IPlatformFluidHelper<?> fluids;
    private Index<T> index = new Index<>(List.of(), Map.of(), Map.of());

    JeiLookupPlugin(
            RecipePage<T> page,
            JeiPageCategory<T> category,
            RecipePage.LookupFilter filter,
            IIngredientManager ingredients,
            IPlatformFluidHelper<?> fluids) {
        this.page = page;
        this.category = category;
        this.filter = filter;
        this.ingredients = ingredients;
        this.fluids = fluids;
    }

    RecipeType<T> type() {
        return category.getRecipeType();
    }

    void fill() {
        List<T> rows = page.rows().stream().filter(row -> !page.hidden(row)).toList();
        Map<Object, List<T>> inputs = new HashMap<>();
        Map<Object, List<T>> outputs = new HashMap<>();
        for (T row : rows) {
            JeiPageCategory.RowIngredients slots = category.ingredients(row, ingredients, fluids);
            index(inputs, row, slots.inputs());
            index(outputs, row, slots.outputs());
        }
        inputs.replaceAll((uid, found) -> List.copyOf(found));
        outputs.replaceAll((uid, found) -> List.copyOf(found));
        index = new Index<>(rows, inputs, outputs);
    }

    // uid -> rows in page order, each row once per uid, so a lookup is one map hit instead of a
    // scan over every row
    private void index(Map<Object, List<T>> into, T row, List<ITypedIngredient<?>> slots) {
        Set<Object> seen = new HashSet<>();
        for (ITypedIngredient<?> ingredient : slots) {
            Object uid = uid(ingredient);
            if (seen.add(uid)) into.computeIfAbsent(uid, key -> new ArrayList<>()).add(row);
        }
    }

    private <V> Object uid(ITypedIngredient<V> ingredient) {
        return ingredients
                .getIngredientHelper(ingredient.getType())
                .getUid(ingredient, UidContext.Recipe);
    }

    private List<T> matching(boolean input, ITypedIngredient<?> looked, RecipePage.Lookup lookup) {
        ItemStack stack = looked.getItemStack().orElse(null);
        if (stack != null && !filter.answers(stack, lookup)) return List.of();
        Index<T> current = index;
        List<T> found = (input ? current.inputs() : current.outputs()).get(uid(looked));
        return found == null ? List.of() : found;
    }

    @Override
    public boolean isHandledInput(ITypedIngredient<?> input) {
        return !matching(true, input, RecipePage.Lookup.USES).isEmpty();
    }

    @Override
    public boolean isHandledOutput(ITypedIngredient<?> output) {
        return !matching(false, output, RecipePage.Lookup.RECIPES).isEmpty();
    }

    @Override
    public List<T> getRecipesForInput(ITypedIngredient<?> input) {
        return matching(true, input, RecipePage.Lookup.USES);
    }

    @Override
    public List<T> getRecipesForOutput(ITypedIngredient<?> output) {
        return matching(false, output, RecipePage.Lookup.RECIPES);
    }

    @Override
    public List<T> getAllRecipes() {
        return index.rows();
    }

    private record Index<T>(
            List<T> rows, Map<Object, List<T>> inputs, Map<Object, List<T>> outputs) {}
}
