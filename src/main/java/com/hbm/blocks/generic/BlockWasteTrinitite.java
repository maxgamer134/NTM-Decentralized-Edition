// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import com.hbm.potion.HbmPotion;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import com.hbm.backport.compat.BlockCompat;

public class BlockWasteTrinitite extends BlockCompat {

    public static final MapCodec<BlockWasteTrinitite> CODEC = simpleCodec(BlockWasteTrinitite::new);

    public BlockWasteTrinitite(BlockBehaviour.Properties props) {
        super(props);
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND)
                .strength(0.5F, 1.5F)
                .sound(SoundType.SAND)
                .noLootTable();
    }

    @Override
    protected MapCodec<BlockWasteTrinitite> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(HbmPotion.radiation(), 5 * 20, 2));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }

    // main's BlockOre display tick: trinitite ground gives off a town aura one tick in five
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);
        if (rand.nextInt(5) != 0) return;
        level.addParticle(
                ParticleTypes.MYCELIUM,
                pos.getX() + rand.nextFloat(),
                pos.getY() + 1.0625D,
                pos.getZ() + rand.nextFloat(),
                0D,
                0D,
                0D);
    }
}
