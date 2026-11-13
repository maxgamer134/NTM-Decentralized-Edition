// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.backport.client.core.BlockOutlineRenderState;
import com.hbm.backport.client.core.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class MachinePlacementOutline {
    private MachinePlacementOutline() {}

    public static @Nullable State extract(
            Level level, BlockHitResult hit, @Nullable Player player) {
        if (player == null
                || hit.getType() != HitResult.Type.BLOCK
                || !(player.getMainHandItem().getItem() instanceof BlockItem item)
                || !(item.getBlock() instanceof BlockMultiblockCore block)) return null;

        Direction face = hit.getDirection();
        Direction facing = block.placementFacing(player.getYRot(), face);
        Direction direction = block.getDirModified(facing);
        BlockPos core = block.placementCore(hit.getBlockPos().relative(face), face, facing, direction);
        return new State(
                core,
                block.placementOutline(direction),
                block.placementExtraOutlines(direction),
                block.placementClear(level, core, direction));
    }

    public static void submit(
            State state,
            BlockOutlineRenderState outline,
            SubmitNodeCollector collector,
            PoseStack poses,
            LevelRenderState levelState) {
        int pulse =
                (int)
                        (255
                                * (Math.sin((GameTime.now() % (1000D * Math.PI)) / 250D) * 0.25D
                                        + 0.75D));
        int color = state.placeable ? 0xFF000000 | pulse << 8 : 0xFF000000 | pulse << 16;
        Vec3 camera = levelState.cameraRenderState.pos;
        poses.pushPose();
        poses.translate(
                state.core.getX() - camera.x,
                state.core.getY() - camera.y,
                state.core.getZ() - camera.z);
        collector.submitShapeOutline(
                poses,
                state.shape,
                RenderTypes.linesTranslucent(),
                color,
                2F,
                outline.isTranslucent());
        for (VoxelShape extra : state.extras) {
            collector.submitShapeOutline(
                    poses,
                    extra,
                    RenderTypes.linesTranslucent(),
                    0xFF000000 | pulse,
                    2F,
                    outline.isTranslucent());
        }
        poses.popPose();
    }

    public record State(BlockPos core, VoxelShape shape, VoxelShape[] extras, boolean placeable) {}
}
