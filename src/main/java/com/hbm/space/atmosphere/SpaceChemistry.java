// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.atmosphere;

import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.space.AstronomyUtil;
import com.hbm.space.CelestialBody;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.space.trait.CBT_Water;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;

/**
 * A body's own atmospheric chemistry (CelestialBody.updateChemistry), run once a second: thick
 * enough steam makes a water table, and the atmospheric chemistry recipes react what the air holds.
 */
public final class SpaceChemistry {

    /** Processing does 64x a recipe's amounts at once, since the chemistry runs so rarely. */
    private static final int SCALE = 64;

    /** AtmosphereRecipes: lithium carbonate from lithium hydroxide and Duna's air. */
    private record Recipe(String output, int outAmount, String inA, int amountA, String inB, int amountB) {}

    private static final List<Recipe> RECIPES = List.of(
            new Recipe("LITHCARBONATE", 32_000, "LITHYDRO", 16_000, "DUNAAIR", 256_000));

    private SpaceChemistry() {}

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0 || CelestialBody.getBodyOrNull(level.dimension()) == null) return;
        CelestialBody body = CelestialBody.getBody(level);
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        if (atmosphere == null) return;

        if (body.getTrait(CBT_Water.class) == null) {
            double steam = 0;
            for (Fluid fluid : new Fluid[] {NTMFluids.STEAM, NTMFluids.HOTSTEAM, NTMFluids.SUPERHOTSTEAM, NTMFluids.ULTRAHOTSTEAM, NTMFluids.SPENTSTEAM})
                steam += atmosphere.getPressure(fluid);
            if (steam > 0.2D) body.modifyTraits(new CBT_Water());
        }

        for (Recipe recipe : RECIPES) react(level, recipe);
    }

    private static void react(ServerLevel level, Recipe recipe) {
        Fluid a = NTMFluids.byName(recipe.inA());
        Fluid b = NTMFluids.byName(recipe.inB());
        Fluid out = NTMFluids.byName(recipe.output());
        CBT_Atmosphere atmosphere = CelestialBody.getBody(level).getTrait(CBT_Atmosphere.class);
        if (a == null || b == null || out == null || atmosphere == null) return;
        if (atmosphere.getPressure(a) * AstronomyUtil.MB_PER_ATM < recipe.amountA() * SCALE) return;
        if (atmosphere.getPressure(b) * AstronomyUtil.MB_PER_ATM < recipe.amountB() * SCALE) return;

        CelestialBody.consumeGas(level, a, recipe.amountA() * SCALE);
        CelestialBody.consumeGas(level, b, recipe.amountB() * SCALE);
        CelestialBody.emitGas(level, out, recipe.outAmount() * SCALE);
    }
}
