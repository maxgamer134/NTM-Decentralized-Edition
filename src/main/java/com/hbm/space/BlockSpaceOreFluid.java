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

    /** For the oil detector: "", "_gas" or "_brine" after its message keys. */
    public String reserveSuffix() {
        return switch (type) {
            case GAS -> "_gas";
            case BRINE -> "_brine";
            default -> "";
        };
    }

    private static final com.hbm.space.dim.legacy.Noise.Perlin CRACKS =
            new com.hbm.space.dim.legacy.Noise.Perlin(new java.util.Random(73470), 4);

    /** NTM:Space: fracking Tekto cracks the ground open instead of spilling oil on it. */
    @Override
    public void scarSurface(Level level, RandomSource random, int x, int z, int range) {
        if (level.dimension() != com.hbm.space.SpaceDimensions.TEKTO) {
            FluidReserveBlock.super.scarSurface(level, random, x, z, range);
            return;
        }
        Block plant = id("rubber_plant"), grass = id("rubber_grass"), silt = id("rubber_silt"), vinyl = id("vinyl_sand");
        Block slaked = id("sellafield_slaked"), basalt = id("basalt"), dead = id("plant_dead");
        for (int i = 0; i < 100; i++) { // only ~10% of spots pass the noise
            int rX = x + (int) (random.nextGaussian() * range);
            int rZ = z + (int) (random.nextGaussian() * range);
            if (CRACKS.func_151601_a(rX * 0.25, rZ * 0.25) < 0.9) continue;
            int rY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, rX, rZ);
            for (int y = rY; y > rY - 4; y--) {
                BlockPos pos = new BlockPos(rX, y, rZ), down = pos.below();
                BlockState belowState = level.getBlockState(down), groundState = level.getBlockState(pos);
                Block ground = groundState.getBlock();
                if (ground == plant && belowState.isCollisionShapeFullBlock(level, down))
                    level.setBlock(pos, dead.defaultBlockState(), 3);
                if (ground == grass) {
                    level.setBlock(pos, silt.defaultBlockState(), 3);
                    break;
                }
                if (ground == silt || ground == vinyl || ground == slaked || ground == basalt) {
                    if (belowState.getDestroySpeed(level, down) < 0) break;
                    level.setBlock(down, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    // vinyl sand falls by itself; the rest is shaken loose
                    if (ground != vinyl && level instanceof net.minecraft.server.level.ServerLevel server)
                        net.minecraft.world.entity.item.FallingBlockEntity.fall(server, pos, groundState).dropItem = false;
                    break;
                }
            }
        }
    }

    private static Block id(String path) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(com.hbm.lib.Library.id(path));
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
