// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.recipes.FusionRecipe;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * Which of NTM:Space's plasma fluids a fusion recipe makes, read from its fuels: deuterium and
 * tritium make D-T plasma, hydrogen and deuterium H-D, hydrogen and tritium H-T, deuterium and
 * helium-3 D-He3, xenon and mercury XM, and anything burning balefire BF plasma. A recipe with
 * other fuels makes plasma energy only.
 */
public final class FusionPlasmas {
    private FusionPlasmas() {}

    private record Match(String plasma, List<String> fuels) {}

    private static final List<Match> MATCHES = List.of(
            new Match("PLASMA_DT", List.of("DEUTERIUM", "TRITIUM")),
            new Match("PLASMA_HD", List.of("HYDROGEN", "DEUTERIUM")),
            new Match("PLASMA_HT", List.of("HYDROGEN", "TRITIUM")),
            new Match("PLASMA_DH3", List.of("DEUTERIUM", "HELIUM3")),
            new Match("PLASMA_XM", List.of("XENON", "MERCURY")));

    /** Every plasma fluid, in the order the pages list them. */
    public static List<Fluid> all() {
        return java.util.stream.Stream.of("PLASMA_DT", "PLASMA_HD", "PLASMA_HT", "PLASMA_DH3", "PLASMA_XM", "PLASMA_BF")
                .map(NTMFluids::byName).filter(java.util.Objects::nonNull).toList();
    }

    public static @Nullable Fluid of(@Nullable FusionRecipe recipe) {
        if (recipe == null || recipe.inputFluid.length == 0) return null;
        Set<Fluid> fuels = new HashSet<>();
        for (FluidStackNTM stack : recipe.inputFluid) fuels.add(stack.type());
        if (fuels.contains(NTMFluids.byName("BALEFIRE_FUEL"))) return NTMFluids.byName("PLASMA_BF");
        for (Match match : MATCHES) {
            Set<Fluid> wanted = new HashSet<>();
            for (String fuel : match.fuels()) wanted.add(NTMFluids.byName(fuel));
            if (fuels.equals(wanted)) return NTMFluids.byName(match.plasma());
        }
        return null;
    }
}
