// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from susy-ntmdoors (Kyron04): https://github.com/Kyron04/susy-ntmdoors

package com.hbm.doors;

import com.hbm.blocks.ITickingBlock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A wall-mounted door device. FACING points into the wall it hangs on, as in susy-ntmdoors; STATUS
 * lights it: 0 idle, 1 granted, 2 denied.
 */
public abstract class BlockDoorDevice extends Block implements ITickingBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty STATUS = IntegerProperty.create("status", 0, 2);

    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    /** The plate for a NORTH device, in block-centred coordinates (x and z from -0.5 to 0.5). */
    protected BlockDoorDevice(Properties props, double x0, double y0, double z0, double x1, double y1, double z1) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(STATUS, 0));
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            double[] a = turn(dir, x0, z0), b = turn(dir, x1, z1);
            shapes.put(dir, Shapes.box(
                    Math.min(a[0], b[0]) + 0.5, y0, Math.min(a[1], b[1]) + 0.5,
                    Math.max(a[0], b[0]) + 0.5, y1, Math.max(a[1], b[1]) + 0.5));
        }
    }

    private static double[] turn(Direction dir, double x, double z) {
        return switch (dir) {
            case EAST -> new double[] {-z, x};
            case SOUTH -> new double[] {-x, -z};
            case WEST -> new double[] {z, -x};
            default -> new double[] {x, z};
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATUS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face.getOpposite() : ctx.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof DoorDevice device)
            device.placedBy(player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DoorDevice device) device.removed();
        super.onRemove(state, level, pos, newState, moved);
    }

    /** What a door device's block entity does when placed and removed. */
    interface DoorDevice {
        void placedBy(Player player);

        default void removed() {}
    }
}
