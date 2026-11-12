// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.backport.StructureProcessorCompat;
import com.hbm.blockentity.network.BlockEntityPylonBase;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Red-wire links in the planet structures. NTM:Space's templates kept each connector's wires;
 * the converted templates keep the far ends as template positions under {@link #KEY}, and this
 * processor turns them into world positions (rotation and mirror included) for the connector,
 * which strings the wires when it loads.
 */
public final class SpaceWireLinks extends StructureProcessorCompat {

    public static final String KEY = "hbm_wire_local";
    public static final SpaceWireLinks INSTANCE = new SpaceWireLinks();
    public static final MapCodec<SpaceWireLinks> MAP_CODEC = MapCodec.unit(() -> INSTANCE);
    public static final RegistryHandle<MapCodec<SpaceWireLinks>> TYPE =
            NeoForgeRegistrar.registerStructureProcessorType("wire_links", MAP_CODEC);

    private SpaceWireLinks() {}

    public static void init() {}

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        CompoundTag nbt = processedBlockInfo.nbt();
        if (nbt == null || !nbt.contains(KEY)) return processedBlockInfo;
        int[] local = nbt.getIntArray(KEY);
        BlockPos self = StructureTemplate.calculateRelativePosition(settings, templateRelativePos);
        int[] world = new int[local.length / 3 * 3];
        for (int i = 0; i + 2 < local.length; i += 3) {
            BlockPos far = StructureTemplate.calculateRelativePosition(
                    settings, new BlockPos(local[i], local[i + 1], local[i + 2]));
            BlockPos at = processedBlockInfo.pos().offset(far.subtract(self));
            world[i] = at.getX();
            world[i + 1] = at.getY();
            world[i + 2] = at.getZ();
        }
        CompoundTag out = nbt.copy();
        out.remove(KEY);
        out.putIntArray(BlockEntityPylonBase.STRUCTURE_WIRES, world);
        return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), processedBlockInfo.state(), out);
    }

    @Override
    public MapCodec<SpaceWireLinks> codec() {
        return MAP_CODEC;
    }
}
