// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.hbm.config.ConfigEntry.Domain;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.neoforged.fml.loading.FMLPaths;

/**
 * A content-config switch read straight from disk, for registration that runs before NeoForge
 * loads the content config.
 */
public final class EarlyContentConfig {

    private EarlyContentConfig() {}

    public static boolean get(ConfigEntry<Boolean> entry) {
        Path file = FMLPaths.CONFIGDIR.get().resolve(ConfigFiles.neoForgeFile(Domain.CONTENT));
        if (!Files.isRegularFile(file)) return entry.defaultValue();
        try (CommentedFileConfig config = CommentedFileConfig.of(file)) {
            config.load();
            Object value = config.get(List.of(entry.section(), entry.key()));
            return value instanceof Boolean b ? b : entry.defaultValue();
        } catch (RuntimeException e) {
            return entry.defaultValue();
        }
    }
}
