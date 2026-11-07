// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorBalefire;
import com.hbm.explosion.vanillant.standard.BlockMutatorDigamma;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's ExplosionNT (a vanilla blast with attributes), as the
 * pre-rework bullets use it, built on the repo's ExplosionVNT.
 */
public final class LegacyExplosion {

    private LegacyExplosion() {}

    public enum ExAttrib {
        FIRE, BALEFIRE, DIGAMMA, DIGAMMA_CIRCUIT, ALLDROP, NODROP, NOPARTICLE, NOSOUND, NOHURT
    }

    /** new ExplosionNT(level, exploder, x, y, z, size) + attributes + doExplosionA/B */
    public static void explode(Level level, @Nullable Entity exploder, double x, double y, double z, float size, ExAttrib... attribs) {
        if (level.isClientSide()) return;
        EnumSet<ExAttrib> a = EnumSet.noneOf(ExAttrib.class);
        for (ExAttrib e : attribs) a.add(e);
        BlockProcessorStandard blocks = new BlockProcessorStandard();
        if (a.contains(ExAttrib.ALLDROP)) blocks.setAllDrop();
        if (a.contains(ExAttrib.NODROP)) blocks.setNoDrop();
        if (a.contains(ExAttrib.FIRE)) blocks.withBlockEffect(new BlockMutatorFire());
        if (a.contains(ExAttrib.BALEFIRE)) blocks.withBlockEffect(new BlockMutatorBalefire());
        if (a.contains(ExAttrib.DIGAMMA) || a.contains(ExAttrib.DIGAMMA_CIRCUIT))
            blocks.withBlockEffect(new BlockMutatorDigamma(a.contains(ExAttrib.DIGAMMA_CIRCUIT)));
        ExplosionVNT vnt = new ExplosionVNT(level, x, y, z, size, exploder)
                .setBlockAllocator(new BlockAllocatorStandard())
                .setBlockProcessor(blocks);
        if (!a.contains(ExAttrib.NOHURT))
            vnt.setEntityProcessor(new EntityProcessorStandard()).setPlayerProcessor(new PlayerProcessorStandard());
        if (!a.contains(ExAttrib.NOPARTICLE) || !a.contains(ExAttrib.NOSOUND)) vnt.setSFX(new ExplosionEffectStandard());
        vnt.explode();
    }

    /** main's ExplosionChaos.explodeZOMG: a sphere of air, sparing the bedrock floor */
    public static void zomg(Level level, int x, int y, int z, int r) {
        int r22 = r * r / 2;
        for (int xx = -r; xx < r; xx++)
            for (int yy = -r; yy < r; yy++)
                for (int zz = -r; zz < r; zz++) {
                    if (xx * xx + yy * yy + zz * zz >= r22) continue;
                    net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(x + xx, y + yy, z + zz);
                    if (!(level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.BEDROCK) && p.getY() <= level.getMinBuildHeight()))
                        level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
    }
}
