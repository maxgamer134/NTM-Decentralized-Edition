// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.hazard.HazardSystem;
import com.hbm.particle.HbmParticles;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.BlockCompat;

public class BlockHazard extends BlockCompat {

    public static final MapCodec<BlockHazard> CODEC = simpleCodec(BlockHazard::new);
    private @Nullable ExtDisplayEffect extEffect = null;

    public BlockHazard(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BlockHazard> codec() {
        return CODEC;
    }

    public BlockHazard setDisplayEffect(@Nullable ExtDisplayEffect effect) {
        this.extEffect = effect;
        return this;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide()
                && entity instanceof LivingEntity living
                && !(entity instanceof Player p && p.getAbilities().instabuild)) {
            HazardSystem.applyHazards(this, living);
        }
        super.stepOn(level, pos, state, entity);
    }

    private boolean hot;
    private boolean townAura;

    /** Main's BlockOre display tick for trinitite and nuclear waste: a town aura on top. */
    public BlockHazard setTownAura(boolean townAura) {
        this.townAura = townAura;
        return this;
    }

    /** Main's BlockHotHazard: the block steams in the rain and wherever it touches water. */
    public BlockHazard setHot(boolean hot) {
        this.hot = hot;
        return this;
    }

    private void hotSteam(Level level, BlockPos pos, RandomSource rand) {
        if (level.isRainingAt(pos.above())) {
            level.addParticle(
                    ParticleTypes.CLOUD,
                    pos.getX() + rand.nextFloat(),
                    pos.getY() + 1,
                    pos.getZ() + rand.nextFloat(),
                    0D,
                    0D,
                    0D);
        }
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;
            if (!level.getFluidState(pos.relative(dir)).is(net.minecraft.tags.FluidTags.WATER))
                continue;
            int ox = dir.getStepX(), oy = dir.getStepY(), oz = dir.getStepZ();
            double ix = pos.getX() + 0.5D + ox + rand.nextDouble() - 0.5D;
            double iy = pos.getY() + 0.5D + oy + rand.nextDouble() - 0.5D;
            double iz = pos.getZ() + 0.5D + oz + rand.nextDouble() - 0.5D;
            if (ox != 0) ix = pos.getX() + 0.5D + ox * 0.5D + rand.nextDouble() * 0.125D * ox;
            if (oy != 0) iy = pos.getY() + 0.5D + oy * 0.5D + rand.nextDouble() * 0.125D * oy;
            if (oz != 0) iz = pos.getZ() + 0.5D + oz * 0.5D + rand.nextDouble() * 0.125D * oz;
            level.addParticle(ParticleTypes.CLOUD, ix, iy, iz, 0D, 0D, 0D);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (hot) hotSteam(level, pos, rand);
        if (townAura) {
            level.addParticle(
                    ParticleTypes.MYCELIUM,
                    pos.getX() + rand.nextFloat(),
                    pos.getY() + 1.0625D,
                    pos.getZ() + rand.nextFloat(),
                    0D,
                    0D,
                    0D);
        }
        if (extEffect == null) return;
        for (Direction dir : Direction.VALUES) {
            if (!level.getBlockState(pos.relative(dir)).isAir()) continue;
            int ox = dir.getStepX(), oy = dir.getStepY(), oz = dir.getStepZ();
            double ix = pos.getX() + 0.5 + ox + rand.nextDouble() * 3 - 1.5;
            double iy = pos.getY() + 0.5 + oy + rand.nextDouble() * 3 - 1.5;
            double iz = pos.getZ() + 0.5 + oz + rand.nextDouble() * 3 - 1.5;
            if (ox != 0) ix = pos.getX() + 0.5 + ox * 0.5 + rand.nextDouble() * ox;
            if (oy != 0) iy = pos.getY() + 0.5 + oy * 0.5 + rand.nextDouble() * oy;
            if (oz != 0) iz = pos.getZ() + 0.5 + oz * 0.5 + rand.nextDouble() * oz;
            switch (extEffect) {
                case RADFOG -> level.addParticle(ParticleTypes.MYCELIUM, ix, iy, iz, 0.0, 0.0, 0.0);
                case SCHRAB ->
                        level.addParticle(HbmParticles.SCHRAB_FOG.get(), ix, iy, iz, 0.0, 0.0, 0.0);
            }
        }
    }

    public enum ExtDisplayEffect {
        RADFOG,
        SCHRAB
    }
}
