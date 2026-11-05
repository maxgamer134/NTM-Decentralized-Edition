package com.almostreliable.unified.api.unification;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/** Compile-only stand-in for AlmostUnified's UnificationEntry (only the members NTM uses). */
public interface UnificationEntry<T> {

    ResourceLocation id();

    TagKey<T> tag();
}
