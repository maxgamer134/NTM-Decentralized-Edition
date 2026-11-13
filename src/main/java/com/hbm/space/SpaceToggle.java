// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.hbm.config.ConfigEntry.Domain;
import com.hbm.config.ConfigFiles;
import com.hbm.config.ConfigSchema;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The NTM:Space switch ({@link ConfigSchema#ENABLE_SPACE}). Registration runs before NeoForge
 * loads the content config, so this reads the toml straight from disk the first time it is asked
 * and keeps the answer for the session: every Space registration is wrapped in
 * {@code if (SpaceToggle.enabled())}, and with it off nothing of Space reaches a registry.
 */
public final class SpaceToggle {

    private static volatile Boolean enabled;
    private static volatile Boolean changes;

    private SpaceToggle() {}

    public static boolean enabled() {
        Boolean value = enabled;
        if (value == null) enabled = value = read();
        return value;
    }

    /** Space's changes to existing content ({@link ConfigSchema#SPACE_CHANGES}); only with Space on. */
    public static boolean changes() {
        Boolean value = changes;
        if (value == null) changes = value = enabled() && read(ConfigSchema.SPACE_CHANGES);
        return value;
    }

    private static volatile Boolean overworldMeteorDungeons;
    private static volatile Boolean overworldAustralium;

    /** Whether the overworld keeps its meteor dungeons: always without Space's changes, else by config. */
    public static boolean overworldMeteorDungeons() {
        Boolean value = overworldMeteorDungeons;
        if (value == null)
            overworldMeteorDungeons = value = !changes() || read(ConfigSchema.SPACE_OVERWORLD_METEOR_DUNGEONS);
        return value;
    }

    /** Whether the overworld keeps its Australium deposit: always without Space's changes, else by config. */
    public static boolean overworldAustralium() {
        Boolean value = overworldAustralium;
        if (value == null)
            overworldAustralium = value = !changes() || read(ConfigSchema.SPACE_OVERWORLD_AUSTRALIUM);
        return value;
    }

    private static boolean read() {
        return read(ConfigSchema.ENABLE_SPACE);
    }

    private static boolean read(com.hbm.config.ConfigEntry<Boolean> entry) {
        Path file = FMLPaths.CONFIGDIR.get().resolve(ConfigFiles.neoForgeFile(Domain.CONTENT));
        if (!Files.isRegularFile(file)) return entry.defaultValue();
        try (CommentedFileConfig config = CommentedFileConfig.of(file)) {
            config.load();
            Object value =
                    config.get(
                            java.util.List.of(
                                    entry.section(),
                                    entry.key()));
            return value instanceof Boolean b ? b : entry.defaultValue();
        } catch (RuntimeException e) {
            return entry.defaultValue();
        }
    }
}
