// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.almostunified;

import com.almostreliable.unified.api.AlmostUnified;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * backport-fix: BF-128 the item AlmostUnified prefers for a tag, for NTM outputs that code picks
 * from a tag (foundry molds) instead of recipe JSON. Null without AlmostUnified, before its runtime
 * is loaded, or for a tag it does not unify. AU's classes are only touched when it is installed.
 */
public final class AlmostUnifiedCompat {

    private static Boolean loaded;

    private AlmostUnifiedCompat() {}

    public static @Nullable Item tagTarget(TagKey<Item> tag) {
        if (loaded == null) loaded = ModList.get() != null && ModList.get().isLoaded("almostunified");
        return loaded ? Api.tagTarget(tag) : null;
    }

    private static final class Api {

        static @Nullable Item tagTarget(TagKey<Item> tag) {
            AlmostUnified unified = AlmostUnified.INSTANCE;
            return unified != null && unified.isRuntimeLoaded() ? unified.getTagTargetItem(tag) : null;
        }
    }
}
