// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.blockentity.BlockEntityPlushie;
import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * NTM:Space: a press stamping with a plushie sat on top squeaks in pain, and one stamping under
 * Hundun blows itself up.
 */
public final class SpacePressPlushie {

    public static final RegistryHandle<SoundEvent> SQUEAKY_PAIN = NeoForgeRegistrar.registerSound("block.squeaky_pain");

    private SpacePressPlushie() {}

    /**
     * The sound for a stamp of the press whose core is at core, or the given sound with no plushie
     * on it. A Hundun on top destroys the press. Only called with Space changes on.
     */
    public static SoundEvent stamp(Level level, BlockPos core, SoundEvent normal) {
        BlockPos top = core.above();
        Block press = level.getBlockState(core).getBlock();
        for (int i = 0; i < 4 && isPart(level.getBlockState(top).getBlock(), press); i++) top = top.above();
        if (!(level.getBlockEntity(top) instanceof BlockEntityPlushie plushie)) return normal;
        if (plushie.type == PlushieType.HUNDUN) {
            level.destroyBlock(core, false);
            ExplosionVNT vnt = new ExplosionVNT(level, core.getX() + 0.5, core.getY() + 0.5, core.getZ() + 0.5, 5);
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 50));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
            vnt.explode();
        }
        return SQUEAKY_PAIN.get();
    }

    private static boolean isPart(Block block, Block press) {
        return block == press || block instanceof BlockMultiblockCell;
    }
}
