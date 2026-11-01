// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from HBM's Nuclear Tech Mod (ExhaustPipeAnchor / PneumoPipeAnchor).

package com.hbm.blocks.network;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * Pipe anchor for exhaust networks: like the exhaust duct it sits in all three smoke graphs
 * (smoke, leaded smoke, poisonous smoke) at once, and wrench links join every one of them.
 */
public class ExhaustPipeAnchorBlock extends FluidPipeAnchorBlock {

    public ExhaustPipeAnchorBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean retypable() {
        return false;
    }

    @Override
    public boolean hasFluidContact(
            Level level, BlockPos pos, BlockState state, Direction side, Fluid fluid) {
        return side == state.getValue(FACING).getOpposite() && FluidPipeGraph.isSmoke(fluid);
    }

    @Override
    protected void createNodes(ServerLevel level, BlockPos pos, BlockState state) {
        for (Fluid smoke : FluidPipeGraph.smokes()) {
            FluidPipeGraph.get(level, smoke)
                    .addNode(pos.asLong(), new PipeData(smoke), nodeMask(state));
        }
    }

    @Override
    protected void destroyNodes(ServerLevel level, BlockPos pos) {
        for (Fluid smoke : FluidPipeGraph.smokes()) {
            FluidPipeGraph.get(level, smoke).removeNode(pos.asLong());
        }
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity by,
            ItemStack stack) {}

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        info.title(getName().getString(), 0xffff00, 0x404000);
        for (Fluid smoke : FluidPipeGraph.smokes()) {
            info.line(NTMFluidProperties.getDisplayName(smoke).getString());
        }
    }
}
