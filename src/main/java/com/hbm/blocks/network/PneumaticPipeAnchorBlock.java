// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from HBM's Nuclear Tech Mod (ExhaustPipeAnchor / PneumoPipeAnchor).

package com.hbm.blocks.network;

import com.hbm.api.ntl.IPneumaticConnector;
import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.interfaces.ILookOverlay;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * Pipe anchor for pneumatic tubes: a pneumatic graph node open only on the face it is mounted
 * on, wrench links connect it to other pneumatic anchors. Carries no fluid.
 */
public class PneumaticPipeAnchorBlock extends FluidPipeAnchorBlock implements IPneumaticConnector {

    public PneumaticPipeAnchorBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean canConnectPneumatic(Direction dir) {
        return true;
    }

    @Override
    public boolean retypable() {
        return false;
    }

    @Override
    public boolean hasFluidContact(
            Level level, BlockPos pos, BlockState state, Direction side, Fluid fluid) {
        return false;
    }

    @Override
    protected void createNodes(ServerLevel level, BlockPos pos, BlockState state) {
        PneumaticNetwork.get(level)
                .addNode(pos.asLong(), com.hbm.api.ntl.PneumaticData.INSTANCE, nodeMask(state));
    }

    @Override
    protected void destroyNodes(ServerLevel level, BlockPos pos) {
        PneumaticNetwork.removeNode(level, pos);
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
    }

    @Override
    public @Nullable List<Component> getDebugInfo(Level level, BlockPos pos) {
        return null;
    }

    @Override
    public String[] infoForDisplay(Level level, BlockPos pos) {
        return new String[0];
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        return new CompoundTag();
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {}
}
