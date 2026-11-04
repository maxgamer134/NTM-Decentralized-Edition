// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import com.hbm.blockentity.machine.BlockEntityMachineElectricFurnace;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public class MachineElectricFurnace extends BlockMachineHorizontal implements ICapabilityBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public MachineElectricFurnace(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    // main's randomDisplayTick: smoke and a flame at the front while the furnace runs
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        Direction dir = state.getValue(FACING);
        Direction rot = dir.getClockWise();
        double off = 0.52D;
        double var = random.nextFloat() * 0.6D - 0.3D;
        double px = pos.getX() + 0.5D + dir.getStepX() * off + rot.getStepX() * var;
        double py = pos.getY() + random.nextFloat() * 0.375D;
        double pz = pos.getZ() + 0.5D + dir.getStepZ() * off + rot.getStepZ() * var;
        level.addParticle(ParticleTypes.SMOKE, px, py, pz, 0, 0, 0);
        level.addParticle(ParticleTypes.FLAME, px, py, pz, 0, 0, 0);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineElectricFurnace(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ELECTRIC_FURNACE).powerIn().items().fe();
    }
}
