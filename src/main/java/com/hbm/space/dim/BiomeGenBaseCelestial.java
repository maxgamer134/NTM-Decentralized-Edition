// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim;

import com.hbm.space.dim.legacy.BiomeGenBase;
import com.hbm.space.dim.legacy.LegacyConfig;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A planet biome. NTM:Space numbered its biomes from its config; each one here is also the data
 * pack biome named after its config entry (the Mun's moonBiome is hbm:moon, Duna's hills
 * dunaHillsBiome are hbm:duna_hills).
 */
public abstract class BiomeGenBaseCelestial extends BiomeGenBase {

    private static Map<Integer, String> names;

    protected BiomeGenBaseCelestial(int id) {
        super(id, nameOf(id));
    }

    private static synchronized String nameOf(int id) {
        if (names == null) {
            names = new HashMap<>();
            for (Field f : LegacyConfig.SpaceConfig.class.getFields()) {
                if (!f.getName().endsWith("Biome") || f.getType() != int.class) continue;
                try {
                    String n = f.getName().substring(0, f.getName().length() - 5);
                    names.put(f.getInt(null), n.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        String name = names.get(id);
        if (name == null) throw new IllegalStateException("no NTM:Space biome has id " + id);
        return name;
    }
}
