// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib;

import com.hbm.NuclearTech;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Library {

    private Library() {}

    /** Voxels main's ray walk crosses before it gives up (World.func_147447_a). */
    private static final int RAY_STEPS = 200;

    /**
     * Main's {@code Library.rayTrace(player, length, 1)}: the first block in the way, looking
     * through fluids, and when nothing is hit the last voxel the walk reached (at most
     * {@link #RAY_STEPS} voxels out), so aiming at the sky still names a spot. Never a miss.
     */
    // backport-fix: BF-140 main's designators, detonator and bomb caller all aim with this; a
    // plain pick() misses past the first block in view and those tools then did nothing
    public static BlockHitResult rayTrace(Player player, double length) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 delta = player.getViewVector(1.0F).scale(length);
        Vec3 end = start.add(delta);
        int x = Mth.floor(start.x), y = Mth.floor(start.y), z = Mth.floor(start.z);
        int ex = Mth.floor(end.x), ey = Mth.floor(end.y), ez = Mth.floor(end.z);
        int sx = (int) Math.signum(delta.x), sy = (int) Math.signum(delta.y), sz = (int) Math.signum(delta.z);
        double dx = sx == 0 ? Double.MAX_VALUE : sx / delta.x;
        double dy = sy == 0 ? Double.MAX_VALUE : sy / delta.y;
        double dz = sz == 0 ? Double.MAX_VALUE : sz / delta.z;
        double tx = sx == 0 ? Double.MAX_VALUE : dx * (sx > 0 ? 1 - Mth.frac(start.x) : Mth.frac(start.x));
        double ty = sy == 0 ? Double.MAX_VALUE : dy * (sy > 0 ? 1 - Mth.frac(start.y) : Mth.frac(start.y));
        double tz = sz == 0 ? Double.MAX_VALUE : dz * (sz > 0 ? 1 - Mth.frac(start.z) : Mth.frac(start.z));
        Direction face = Direction.UP;
        double t = 0;
        for (int step = 0; step <= RAY_STEPS && (x != ex || y != ey || z != ez); step++) {
            if (tx < ty && tx < tz) {
                x += sx; t = tx; tx += dx; face = sx > 0 ? Direction.WEST : Direction.EAST;
            } else if (ty < tz) {
                y += sy; t = ty; ty += dy; face = sy > 0 ? Direction.DOWN : Direction.UP;
            } else {
                z += sz; t = tz; tz += dz; face = sz > 0 ? Direction.NORTH : Direction.SOUTH;
            }
        }
        boolean capped = x != ex || y != ey || z != ez;
        double reach = capped ? Math.min(1.0D, Math.min(tx, Math.min(ty, tz))) : 1.0D;
        // the clip itself stays the level's, so Sable still lets the ray land on an airship
        BlockHitResult hit = player.level().clip(new ClipContext(
                start, start.add(delta.scale(reach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) return hit;
        return new BlockHitResult(start.add(delta.scale(t)), face, new BlockPos(x, y, z), false);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NuclearTech.MOD_ID, path);
    }

    public static ResourceLocation resolve(String reference) {
        return reference.indexOf(':') < 0 ? id(reference) : ResourceLocation.parse(reference);
    }

    public static float roundFloat(double number, int decimal) {
        double pow = Math.pow(10, decimal);
        return (float) (Math.round(number * pow) / pow);
    }

    public static int blockPosToLocal(long serialized) {
        return ((int) (serialized & 0xFL) << 8)
                | (((int) (serialized >>> 12) & 0xF) << 4)
                | ((int) (serialized >>> 38) & 0xF);
    }

    public static int packLocal(int localX, int localY, int localZ) {
        return (localY << 8) | (localZ << 4) | localX;
    }

    public static int getLocalX(int packed) {
        return packed & 0xF;
    }

    public static int getLocalY(int packed) {
        return (packed >>> 8) & 0xF;
    }

    public static int getLocalZ(int packed) {
        return (packed >>> 4) & 0xF;
    }

    public static long sectionToLong(long ck, int subY) {
        return (ck << 42) | ((ck >>> 12) & 0x0000_03FF_FFF0_0000L) | (((long) subY) & 0xFFFFFL);
    }

    public static long setSectionY(long key, int subY) {
        return (key & ~0xFFFFFL) | (((long) subY) & 0xFFFFFL);
    }

    public static long shiftSectionX(long key, int dx) {
        return (key & ~(0x3FFFFFL << 42)) | (((key >>> 42) + dx & 0x3FFFFFL) << 42);
    }

    public static long shiftSectionZ(long key, int dz) {
        return (key & ~(0x3FFFFFL << 20)) | ((((key >>> 20) & 0x3FFFFFL) + dz & 0x3FFFFFL) << 20);
    }

    public static long sectionToChunkLong(long sck) {
        return (((sck << 22) >> 10) & 0xFFFF_FFFF_0000_0000L) | ((sck >> 42) & 0xFFFF_FFFFL);
    }

    public static List<int[]> getBlockPosInPath(int x, int y, int z, int length, Vec3 vec0) {
        List<int[]> list = new ArrayList<>();

        for (int i = 0; i <= length; i++) {
            list.add(new int[] {(int) (x + (vec0.x * i)), y, (int) (z + (vec0.z * i)), i});
        }

        return list;
    }
}
