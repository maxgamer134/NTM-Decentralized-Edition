// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.structure;

import com.hbm.world.gen.WorldgenHeight;
import com.hbm.world.gen.component.*;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class NtmFeaturesStructure extends Structure implements ColumnBiome.SelfTested {

    public static final MapCodec<NtmFeaturesStructure> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            settingsCodec(i),
                                            com.mojang.serialization.Codec.STRING
                                                    .optionalFieldOf("piece", "")
                                                    .forGetter(s -> s.piece))
                                    .apply(i, NtmFeaturesStructure::new));

    private static final int FLATNESS_SAMPLE_RADIUS = 21;
    private static final int FLATNESS_TOLERANCE_BLOCKS = 4;

    /** the one piece this structure places (silo, desert_house_1, ...); empty picks one at random as before */
    private final String piece;

    public NtmFeaturesStructure(Structure.StructureSettings settings, String piece) {
        super(settings);
        this.piece = piece;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getBlockX(8);
        int z = chunkPos.getBlockZ(8);
        RandomSource random = context.random();

        int centerHeight =
                context.chunkGenerator()
                        .getFirstOccupiedHeight(
                                x,
                                z,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState());
        if (!ColumnBiome.valid(context, x, centerHeight + 1, z)) return Optional.empty();
        int meanHeight =
                getMeanFirstOccupiedHeight(
                        context,
                        x - FLATNESS_SAMPLE_RADIUS,
                        FLATNESS_SAMPLE_RADIUS * 2,
                        z - FLATNESS_SAMPLE_RADIUS,
                        FLATNESS_SAMPLE_RADIUS * 2);
        boolean flat = Math.abs(meanHeight - centerHeight) <= FLATNESS_TOLERANCE_BLOCKS;

        Holder<Biome> biomeHolder =
                context.chunkGenerator()
                        .getBiomeSource()
                        .getNoiseBiome(
                                QuartPos.fromBlock(x),
                                QuartPos.fromBlock(centerHeight),
                                QuartPos.fromBlock(z),
                                context.randomState().sampler());
        Biome biome = biomeHolder.value();
        boolean hotDryNonMesa =
                biome.getBaseTemperature() >= 1.0F
                        && !biome.hasPrecipitation()
                        && !biomeHolder.is(BiomeTags.IS_BADLANDS);

        BlockPos origin = new BlockPos(x, centerHeight, z);

        String want = piece;
        if (want.isEmpty()) {
            if (flat && random.nextInt(10) == 0) want = "silo";
            else if (hotDryNonMesa) want = random.nextBoolean() ? "desert_house_1" : "desert_house_2";
            else
                want =
                        switch (random.nextInt(6)) {
                            case 0 -> "lab_2";
                            case 1 -> "lab_1";
                            case 2 -> "large_office";
                            case 3 -> "large_office_corner";
                            default -> "rural_house";
                        };
        } else if (want.equals("silo") ? !flat : want.startsWith("desert_house") != hotDryNonMesa) {
            // each split structure keeps the ground and biome its piece needed inside hbm:features
            return Optional.empty();
        }

        if (want.equals("silo")) {
            return Optional.of(
                    new Structure.GenerationStub(
                            origin,
                            builder ->
                                    builder.addPiece(SiloComponentPiece.create(context, origin))));
        }
        NtmComponentPiece part =
                switch (want) {
                    case "desert_house_1" -> new NTMHouse1Piece(origin, random);
                    case "desert_house_2" -> new NTMHouse2Piece(origin, random);
                    case "lab_1" -> new NTMLab1Piece(origin, random);
                    case "lab_2" -> new NTMLab2Piece(origin, random);
                    case "large_office" -> new LargeOfficePiece(origin, random);
                    case "large_office_corner" -> new LargeOfficeCornerPiece(origin, random);
                    default -> new RuralHouse1Piece(origin, random);
                };
        return grounded(context, origin, part);
    }

    private static Optional<GenerationStub> grounded(
            GenerationContext context, BlockPos origin, NtmComponentPiece piece) {
        int surface = WorldgenHeight.averageGround(context, piece.getBoundingBox());
        piece.move(0, surface - origin.getY(), 0);
        return Optional.of(
                new GenerationStub(
                        new BlockPos(origin.getX(), surface, origin.getZ()),
                        builder -> builder.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return HbmStructureTypes.NTM_FEATURES.get();
    }
}
