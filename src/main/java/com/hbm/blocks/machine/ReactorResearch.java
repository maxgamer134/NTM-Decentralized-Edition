// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import net.minecraft.tags.FluidTags;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import com.hbm.blockentity.machine.BlockEntityReactorResearch;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.handler.BossSpawnHandler;
import net.minecraft.core.BlockPos;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ReactorResearch extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {
    public ReactorResearch(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        BossSpawnHandler.markFBI(player);
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityReactorResearch(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.REACTOR_RESEARCH).items();
    }

    // main's randomDisplayTick: the hot core sends bubbles up through any water touching its sides
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!level.getFluidState(pos.relative(dir)).is(FluidTags.WATER)) continue;
            double ix = pos.getX() + 0.5D + dir.getStepX() + rand.nextDouble() - 0.5D;
            double iy = pos.getY() + 0.5D + rand.nextDouble() - 0.5D;
            double iz = pos.getZ() + 0.5D + dir.getStepZ() + rand.nextDouble() - 0.5D;
            if (dir.getStepX() != 0)
                ix = pos.getX() + 0.5D + dir.getStepX() * 0.5D + rand.nextDouble() * 0.125D * dir.getStepX();
            if (dir.getStepZ() != 0)
                iz = pos.getZ() + 0.5D + dir.getStepZ() * 0.5D + rand.nextDouble() * 0.125D * dir.getStepZ();
            level.addParticle(ParticleTypes.BUBBLE, ix, iy, iz, 0D, 0.2D, 0D);
        }
    }
}
