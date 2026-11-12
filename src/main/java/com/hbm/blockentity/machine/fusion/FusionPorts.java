// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine.fusion;

import com.hbm.blocks.machine.fusion.BlockFusionMachine;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.lib.Library;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.NeoForgeCaps;
import com.hbm.registration.RegistryHandle;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

public final class FusionPorts {

    public static BlockCapability<BlockEntity, Direction> KLYSTRON;
    public static BlockCapability<BlockEntity, Direction> PLASMA;

    private FusionPorts() {}

    public static void register() {
        KLYSTRON =
                BlockCapability.create(
                        Library.id("fusion_klystron"), BlockEntity.class, Direction.class);
        PLASMA =
                BlockCapability.create(
                        Library.id("fusion_plasma"), BlockEntity.class, Direction.class);
    }

    public static BlockCapability<BlockEntity, Direction> token(Kind kind) {
        return kind == Kind.KLYSTRON ? KLYSTRON : PLASMA;
    }

    public static @Nullable BlockEntity peer(ServerLevel level, Port port) {
        BlockEntity near =
                token(port.kind())
                        .getCapability(
                                (Level) level,
                                port.cell().relative(port.face()),
                                null,
                                null,
                                port.face().getOpposite());
        return near != null ? near : farPeer(level, port);
    }

    public static BlockLookupCache<BlockEntity> cache(ServerLevel level, Port port) {
        BlockLookupCache<BlockEntity> near =
                new NeoForgeCaps.CapCache<>(
                        token(port.kind()),
                        level,
                        port.cell().relative(port.face()),
                        port.face().getOpposite());
        return () -> {
            BlockEntity found = near.find();
            return found != null ? found : farPeer(level, port);
        };
    }

    /**
     * A peer whose own port reaches two blocks: NTM:Space's HTR-F4 NEO has its plasma node one
     * block outside the engine, so a torus one block away from its end still links up.
     */
    private static @Nullable BlockEntity farPeer(ServerLevel level, Port port) {
        BlockPos far = port.cell().relative(port.face(), 2);
        if (!level.isLoaded(far)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, far);
        if (core == null) return null;
        BlockState coreState = level.getBlockState(core);
        if (!(coreState.getBlock() instanceof BlockFusionMachine machine)) return null;
        BlockEntity coreBe = level.getBlockEntity(core);
        if (coreBe == null || coreBe.isRemoved()) return null;
        Direction facing = coreState.getValue(BlockMultiblockCore.FACING);
        Direction side = port.face().getOpposite();
        for (Port other : machine.links(core, facing)) {
            if (other.kind() == port.kind()
                    && other.reach() == 2
                    && other.face() == side
                    && other.cell().equals(far)) return coreBe;
        }
        return null;
    }

    public static <T> @Nullable T peer(ServerLevel level, Port port, Class<T> type) {
        BlockEntity be = peer(level, port);
        return type.isInstance(be) ? type.cast(be) : null;
    }

    public static void declareAtCell(RegistryHandle<? extends Block> cell) {
        for (Kind kind : Kind.values()) {
            NeoForgeCaps.pendingBlocks.add(
                    new NeoForgeCaps.BlockRegistration<>(
                            token(kind),
                            (Supplier<? extends Block>) cell,
                            (level, pos, state, be, side) -> portCap(level, pos, side, kind)));
        }
    }

    private static @Nullable BlockEntity portCap(
            Level level, BlockPos pos, @Nullable Direction side, Kind kind) {
        if (side == null) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return null;
        BlockState coreState = level.getBlockState(core);
        if (!(coreState.getBlock() instanceof BlockFusionMachine machine)) return null;
        BlockEntity coreBe = level.getBlockEntity(core);
        if (coreBe == null || coreBe.isRemoved()) return null;
        Direction facing = coreState.getValue(BlockMultiblockCore.FACING);
        return exposes(machine.links(core, facing), kind, pos, side) ? coreBe : null;
    }

    public static boolean exposes(
            Iterable<Port> ports, Kind kind, BlockPos pos, @Nullable Direction side) {
        if (side == null) return false;
        for (Port port : ports) {
            if (port.kind() == kind
                    && port.reach() == 1
                    && port.face() == side
                    && port.cell().equals(pos)) return true;
        }
        return false;
    }

    public enum Kind {
        KLYSTRON,
        PLASMA
    }

    /** {@code reach} is how far out the peer sits: 1 touches the cell, 2 leaves a one-block gap. */
    public record Port(Kind kind, BlockPos cell, Direction face, int reach) {
        public Port(Kind kind, BlockPos cell, Direction face) {
            this(kind, cell, face, 1);
        }
    }
}
