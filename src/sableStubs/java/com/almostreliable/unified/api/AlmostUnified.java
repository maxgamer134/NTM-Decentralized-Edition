package com.almostreliable.unified.api;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/** Compile-only stand-in for AlmostUnified's runtime API (only the members NTM uses). */
public interface AlmostUnified {

    // not a constant: callers read AU's real field at runtime
    AlmostUnified INSTANCE = null;

    boolean isRuntimeLoaded();

    @Nullable
    Item getTagTargetItem(TagKey<Item> tag);
}
