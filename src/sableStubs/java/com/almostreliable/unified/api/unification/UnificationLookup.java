package com.almostreliable.unified.api.unification;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/** Compile-only stand-in for AlmostUnified's UnificationLookup (only the members NTM uses). */
public interface UnificationLookup {

    @Nullable
    TagKey<Item> getRelevantItemTag(ResourceLocation item);

    @Nullable
    UnificationEntry<Item> getVariantItemTarget(ResourceLocation item);
}
