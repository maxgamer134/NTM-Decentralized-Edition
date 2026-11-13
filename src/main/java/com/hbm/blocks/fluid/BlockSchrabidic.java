// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.fluid;

import com.hbm.blocks.ModBlocks;
import com.hbm.particle.HbmParticles;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import com.hbm.backport.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.Orientation;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockSchrabidic extends BlockFluidClassicBase {

    public BlockSchrabidic(ClassicFluid fluid, Properties props) {
        super(fluid, props);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (com.hbm.space.SpaceToggle.changes()) {
            // NTM:Space: no longer sticky, it eats whoever swims in it
            if (entity instanceof LivingEntity) {
                Vec3 motion = entity.getDeltaMovement();
                if (motion.y < -0.2D) entity.setDeltaMovement(motion.x, motion.y * 0.5D, motion.z);
                if (!level.isClientSide())
                    entity.hurt(level.damageSources().source(com.hbm.lib.ModDamageTypes.ACID), 10F);
            }
        } else {
            entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
        }
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(
                    living, HazardType.RADIATION, ContaminationType.CREATIVE, 1.0F);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (com.hbm.space.SpaceToggle.changes()) {
            corrode(level, pos);
            return;
        }
        for (Direction dir : Direction.VALUES) {
            BlockState neighbour = level.getBlockState(pos.relative(dir));
            if (!neighbour.is(this) && BlockFluidFiniteBase.isLiquid(neighbour)) {
                level.setBlockAndUpdate(pos, ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState());
                return;
            }
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return com.hbm.space.SpaceToggle.changes() || super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (com.hbm.space.SpaceToggle.changes()) corrode(level, pos);
    }

    /** NTM:Space: water it touches slakes into sellafield, and wood rots into waste wood. */
    private void corrode(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        RandomSource rand = level.getRandom();
        for (Direction dir : Direction.VALUES) {
            BlockPos at = pos.relative(dir);
            BlockState b = level.getBlockState(at);
            BlockState into = null;
            if (!b.is(this) && b.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) into = ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState();
            else if (b.is(net.minecraft.tags.BlockTags.LOGS_THAT_BURN) && rand.nextInt(4) == 0) into = ModBlocks.WASTE_LOG.get().defaultBlockState();
            else if (b.is(net.minecraft.tags.BlockTags.PLANKS) && rand.nextInt(4) == 0) into = ModBlocks.WASTE_PLANKS.get().defaultBlockState();
            else if (rand.nextInt(4) == 0) into = eaten(level, at, b);
            if (into == null) continue;
            if (!into.is(ModBlocks.SELLAFIELD_SLAKED.get()))
                level.playSound(null, at, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 0.2F, 1F);
            level.setBlockAndUpdate(at, into);
        }
    }

    private static final net.minecraft.world.level.block.state.BlockState AIR =
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

    private static Block hbm(String path) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(com.hbm.lib.Library.id(path));
    }

    /** What the acid leaves of the rest: stone crumbles stepwise, soft and metal things dissolve. */
    private static @Nullable BlockState eaten(Level level, BlockPos at, BlockState b) {
        Block block = b.getBlock();
        net.minecraft.world.level.block.Block[] stone = {
                net.minecraft.world.level.block.Blocks.STONE, net.minecraft.world.level.block.Blocks.STONE_BRICKS,
                net.minecraft.world.level.block.Blocks.STONE_BRICK_STAIRS, net.minecraft.world.level.block.Blocks.STONE_SLAB,
                net.minecraft.world.level.block.Blocks.STONE_BRICK_SLAB};
        if (b.isAir() || !b.getFluidState().isEmpty()) return null;
        if (b.is(net.minecraft.tags.BlockTags.LEAVES) || b.getSoundType() == net.minecraft.world.level.block.SoundType.METAL
                || (b.getSoundType() == net.minecraft.world.level.block.SoundType.GLASS && !b.is(ModBlocks.REINFORCED_GLASS.get()))) return AIR;
        for (Block s : stone) if (block == s) return net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState();
        if (block == net.minecraft.world.level.block.Blocks.COBBLESTONE) return net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState();
        if (block == net.minecraft.world.level.block.Blocks.GRAVEL) return net.minecraft.world.level.block.Blocks.CLAY.defaultBlockState();
        if (block == net.minecraft.world.level.block.Blocks.OBSIDIAN || block == hbm("brick_obsidian") || block == hbm("brick_obsidian_stairs"))
            return ModBlocks.GRAVEL_OBSIDIAN.get().defaultBlockState();
        if (block == net.minecraft.world.level.block.Blocks.SANDSTONE || block == net.minecraft.world.level.block.Blocks.END_STONE)
            return net.minecraft.world.level.block.Blocks.SAND.defaultBlockState();
        if (block.getExplosionResistance() < 1.2F && block != hbm("block_polymer") && block != ModBlocks.GRAVEL_OBSIDIAN.get()
                && block != net.minecraft.world.level.block.Blocks.SAND && block != net.minecraft.world.level.block.Blocks.CLAY
                && b.getDestroySpeed(level, at) >= 0) return AIR;
        return null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        double x = pos.getX() + 0.5D + rand.nextDouble() * 2 - 1D;
        double y = pos.getY() + 0.5D + rand.nextDouble() * 2 - 1D;
        double z = pos.getZ() + 0.5D + rand.nextDouble() * 2 - 1D;
        level.addParticle(HbmParticles.SCHRAB_FOG.get(), x, y, z, 0.0D, 0.0D, 0.0D);
    }
}
