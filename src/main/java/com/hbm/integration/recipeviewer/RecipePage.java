// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public abstract class RecipePage<T> {

    private final ResourceLocation id;
    private final Class<? extends T> rowType;
    private final @Nullable ItemLike machine;
    private final @Nullable Component title;

    // pages without a single machine override title(), icon() and catalysts() themselves
    protected RecipePage(ResourceLocation id, Class<? extends T> rowType) {
        this(id, rowType, null, null);
    }

    // a page run by one machine: titled by the key, shown with and catalysed by that machine
    protected RecipePage(
            ResourceLocation id,
            Class<? extends T> rowType,
            @Nullable ItemLike machine,
            @Nullable String titleKey) {
        this.id = id;
        this.rowType = rowType;
        this.machine = machine;
        this.title = titleKey == null ? null : Component.translatable(titleKey);
    }

    public final ResourceLocation id() {
        return id;
    }

    public final Class<? extends T> rowType() {
        return rowType;
    }

    public Component title() {
        return Objects.requireNonNull(title, "page has no title");
    }

    public ItemStack icon() {
        return new ItemStack(Objects.requireNonNull(machine, "page has no machine"));
    }

    public int width() {
        return RecipePanel.WIDTH;
    }

    public int height() {
        return RecipePanel.HEIGHT;
    }

    public abstract List<T> rows();

    // rows that carry their own id need no override
    public ResourceLocation rowId(T row) {
        if (row instanceof Row identified) return identified.id();
        throw new IllegalStateException(id + " rows carry no id: " + row);
    }

    public interface Row {

        ResourceLocation id();
    }

    public List<ItemStack> catalysts() {
        return List.of(icon());
    }

    public abstract void layout(T row, PageLayout layout);

    public void draw(T row, GuiGraphicsExtractor graphics) {}

    public void tooltip(T row, double mouseX, double mouseY, Consumer<Component> output) {}

    public boolean hidden(T row) {
        return false;
    }

    public @Nullable LookupFilter lookupFilter() {
        return null;
    }

    public enum Lookup {
        USES,
        RECIPES
    }

    @FunctionalInterface
    public interface LookupFilter {

        boolean answers(ItemStack looked, Lookup lookup);
    }
}
