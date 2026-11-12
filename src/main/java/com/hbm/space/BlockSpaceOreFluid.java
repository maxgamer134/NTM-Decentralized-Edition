// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.blocks.generic.FluidReserveBlock;
import com.hbm.inventory.fluid.NTMFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * NTM:Space's BlockOreFluid: a per-body oil, gas or brine reserve that derricks, pumpjacks and
 * fracking towers drain. The body picks the fluids and how much each suck yields, with Space's
 * WorldConfig defaults; a drained block turns into its empty counterpart on the same body.
 */
public class BlockSpaceOreFluid extends BlockSpaceOre implements FluidReserveBlock {

    public enum Reserve { OIL, GAS, BRINE }

    private final Supplier<? extends Block> empty;
    private final Reserve type;

    public BlockSpaceOreFluid(Properties props, Supplier<? extends Block> empty, Reserve type) {
        super(props);
        this.empty = empty;
        this.type = type;
    }

    /** The empty ore the reserves leave behind, which drills look through for more. */
    public static class Empty extends BlockSpaceOre implements FluidReserveBlock.Drained {
        public Empty(Properties props) {
            super(props);
        }
    }

    private static OreBody body(BlockState state) {
        return state.hasProperty(BODY) ? state.getValue(BODY) : OreBody.KERBIN;
    }

    private BlockState kerbin() {
        return defaultBlockState().setValue(BODY, OreBody.KERBIN);
    }

    @Override
    public Fluid primaryFluid(BlockState state) {
        return NTMFluids.byName(switch (type) {
            case OIL -> body(state) == OreBody.LAYTHE ? "OIL_DS" : body(state) == OreBody.TEKTO ? "TCRUDE" : "OIL";
            case GAS -> "GAS";
            case BRINE -> "BRINE";
        });
    }

    @Override
    public @Nullable Fluid secondaryFluid(BlockState state) {
        return switch (type) {
            case OIL -> NTMFluids.byName(body(state) == OreBody.TEKTO ? "HGAS" : "GAS");
            case GAS -> NTMFluids.byName("PETROLEUM");
            case BRINE -> null;
        };
    }

    @Override
    public int primaryAmount(BlockState state) {
        return switch (body(state)) {
            case DUNA -> 200;
            case MUN, MINMUS, IKE -> 300;
            default -> 500; // Kerbin, Laythe, Tekto oil and Eve gas
        };
    }

    @Override
    public int secondaryAmount(BlockState state, RandomSource random) {
        return body(state) == OreBody.EVE ? 20 + random.nextInt(81) : 100 + random.nextInt(401);
    }

    @Override
    public int blockFluidAmount(BlockState state) {
        return switch (type) {
            case OIL -> 250;
            case GAS -> 100;
            case BRINE -> 0;
        };
    }

    private static double drainChance(BlockState state) {
        return body(state) == OreBody.DUNA ? 0.1D : 0.05D;
    }

    @Override
    public void drain(Level level, BlockPos pos, RandomSource random, double chanceMultiplier) {
        BlockState state = level.getBlockState(pos);
        if (random.nextDouble() < drainChance(state) * chanceMultiplier)
            level.setBlock(pos, empty.get().defaultBlockState().setValue(BODY, body(state)), 3);
    }

    @Override
    public Fluid primaryFluid() {
        return primaryFluid(kerbin());
    }

    @Override
    public @Nullable Fluid secondaryFluid() {
        return secondaryFluid(kerbin());
    }

    @Override
    public int primaryAmount() {
        return primaryAmount(kerbin());
    }

    @Override
    public int secondaryAmount(RandomSource random) {
        return secondaryAmount(kerbin(), random);
    }

    @Override
    public int blockFluidAmount() {
        return blockFluidAmount(kerbin());
    }

    @Override
    public boolean requiresFracking() {
        return false;
    }

    /** Reserves sink: a full block over its empty kind swaps places with it, as in NTM:Space. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos from, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, from, moved);
        BlockState below = level.getBlockState(pos.below());
        if (below.is(empty.get())) {
            level.setBlock(pos, below, 3);
            level.setBlock(pos.below(), state, 3);
        }
    }
}
