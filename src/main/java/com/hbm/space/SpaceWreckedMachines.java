// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.backport.StructureProcessorCompat;
import com.hbm.blocks.machine.DamageableMachine;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * NTM:Space's transformTE for the Leviathan turbine and the powered condenser: the ones a
 * planet structure places (Dres's breeder hall) come wrecked.
 */
public final class SpaceWreckedMachines extends StructureProcessorCompat {

    public static final SpaceWreckedMachines INSTANCE = new SpaceWreckedMachines();
    public static final MapCodec<SpaceWreckedMachines> MAP_CODEC = MapCodec.unit(() -> INSTANCE);
    public static final RegistryHandle<MapCodec<SpaceWreckedMachines>> TYPE =
            NeoForgeRegistrar.registerStructureProcessorType("wrecked_machines", MAP_CODEC);

    private SpaceWreckedMachines() {}

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processed,
            StructurePlaceSettings settings) {
        if (!SpaceToggle.changes() || !processed.state().hasProperty(DamageableMachine.DAMAGED))
            return processed;
        return new StructureTemplate.StructureBlockInfo(
                processed.pos(), processed.state().setValue(DamageableMachine.DAMAGED, true), processed.nbt());
    }

    @Override
    public MapCodec<SpaceWreckedMachines> codec() {
        return MAP_CODEC;
    }
}
