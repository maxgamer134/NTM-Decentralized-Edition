// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.items.armor;

import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.SpaceToggle;
import net.minecraft.sounds.SoundEvent;

/**
 * NTM:Space's dash and recharge sounds. Registered with Space or V1 on: Space's changes give every
 * dash these sounds, and V1 always uses them.
 */
public final class DashSounds {

    public static final RegistryHandle<SoundEvent> DASH = NeoForgeRegistrar.registerSound("player.dash");
    public static final RegistryHandle<SoundEvent> RECHARGE = NeoForgeRegistrar.registerSound("player.dash_recharge");

    private static volatile boolean loaded;

    private DashSounds() {}

    public static void loaded() {
        loaded = true;
    }

    /** True if dashes should sound like Space's for this wearer. */
    static boolean apply(boolean wearsV1) {
        return loaded && (wearsV1 || SpaceToggle.changes());
    }
}
