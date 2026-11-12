// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.space.SolarSystem.Body;
import com.hbm.space.dim.Ike.*;
import com.hbm.space.dim.dres.*;
import com.hbm.space.dim.dres.biome.*;
import com.hbm.space.dim.dres.genlayer.*;
import com.hbm.space.dim.duna.*;
import com.hbm.space.dim.duna.biome.*;
import com.hbm.space.dim.duna.genlayer.*;
import com.hbm.space.dim.eve.*;
import com.hbm.space.dim.eve.biome.*;
import com.hbm.space.dim.eve.genlayer.*;
import com.hbm.space.dim.laythe.*;
import com.hbm.space.dim.laythe.biome.*;
import com.hbm.space.dim.laythe.genlayer.*;
import com.hbm.space.dim.legacy.BiomeGenBase;
import com.hbm.space.dim.legacy.GenLayer;
import com.hbm.space.dim.legacy.Layers.*;
import com.hbm.space.dim.legacy.LegacyBlocks;
import com.hbm.space.dim.legacy.LegacyConfig.SpaceConfig;
import com.hbm.space.dim.legacy.WorldChunkManager;
import com.hbm.space.dim.minmus.*;
import com.hbm.space.dim.minmus.biome.*;
import com.hbm.space.dim.minmus.genlayer.*;
import com.hbm.space.dim.moho.*;
import com.hbm.space.dim.moho.biome.*;
import com.hbm.space.dim.moho.genlayer.*;
import com.hbm.space.dim.moon.*;
import com.hbm.space.dim.tekto.*;
import com.hbm.space.dim.tekto.GenLayerTekto.*;
import com.hbm.space.dim.tekto.biome.*;
import com.hbm.space.dim.thatmo.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Blocks;

/**
 * NTM:Space's planets as their WorldProviders set them up: biome layers (createBiomeGenerators),
 * terrain, stone, bedrock ice and acid, and the world generators PlanetGen registered, the
 * planet's own first and WorldGeneratorCelestial's shared ores after, as their weights ran them.
 */
final class SpacePlanets {

    private static boolean done;

    private SpacePlanets() {}

    private static List<Planet.Decorator> decorators(Planet.Decorator own) {
        return own == null ? List.of(new WorldGeneratorCelestial()) : List.of(own, new WorldGeneratorCelestial());
    }

    /** Every biome a planet's layers can give: its own, and the vanilla rivers 1.7's river mix puts in. */
    private static List<BiomeGenBase> biomes(BiomeGenBase... own) {
        List<BiomeGenBase> all = new ArrayList<>(List.of(own));
        all.add(BiomeGenBase.river);
        all.add(BiomeGenBase.frozenRiver);
        return all;
    }

    private static FluidStackNTM fluid(String name, int amount) {
        return new FluidStackNTM(NTMFluids.byName(name), amount);
    }

    static synchronized void bootstrap() {
        if (done) return;
        done = true;

        Planets.register(new Planet(Body.MUN, () -> LegacyBlocks.b("moon_rock"),
                seed -> new WorldChunkManager(BiomeGenMoon.biome), ChunkProviderMoon::new,
                biomes(BiomeGenMoon.biome), decorators(new WorldGeneratorMoon())).ice());

        Planets.register(new Planet(Body.IKE, () -> LegacyBlocks.b("ike_stone"),
                seed -> new WorldChunkManager(BiomeGenIke.biome), ChunkProviderIke::new,
                biomes(BiomeGenIke.biome), decorators(new WorldGeneratorIke())).ice());

        Planets.register(new Planet(Body.DRES, () -> LegacyBlocks.b("dres_rock"), SpacePlanets::dres, ChunkProviderDres::new,
                biomes(BiomeGenBaseDres.dresPlains, BiomeGenBaseDres.dresCanyon), decorators(new WorldGeneratorDres()))
                .ice().acid(() -> fluid("NITRIC_ACID", 500)));

        Planets.register(new Planet(Body.DUNA, () -> LegacyBlocks.b("duna_rock"), SpacePlanets::duna, ChunkProviderDuna::new,
                biomes(BiomeGenBaseDuna.dunaPlains, BiomeGenBaseDuna.dunaLowlands, BiomeGenBaseDuna.dunaPolar,
                        BiomeGenBaseDuna.dunaHills, BiomeGenBaseDuna.dunaPolarHills),
                decorators(new WorldGeneratorDuna())).ice());

        Planets.register(new Planet(Body.EVE, () -> LegacyBlocks.b("eve_rock"), SpacePlanets::eve, ChunkProviderEve::new,
                biomes(BiomeGenBaseEve.evePlains, BiomeGenBaseEve.eveOcean, BiomeGenBaseEve.eveMountains,
                        BiomeGenBaseEve.eveSeismicPlains, BiomeGenBaseEve.eveRiver),
                decorators(new WorldGeneratorEve())).acid(() -> fluid("KMNO4", 500)));

        Planets.register(new Planet(Body.LAYTHE, () -> Blocks.STONE, SpacePlanets::laythe, ChunkProviderLaythe::new,
                biomes(BiomeGenBaseLaythe.laytheIsland, BiomeGenBaseLaythe.laytheOcean, BiomeGenBaseLaythe.laythePolar,
                        BiomeGenBaseLaythe.laytheCoast),
                decorators(new WorldGeneratorLaythe())));

        Planets.register(new Planet(Body.MINMUS, () -> LegacyBlocks.b("minmus_stone"), SpacePlanets::minmus, ChunkProviderMinmus::new,
                biomes(BiomeGenBaseMinmus.minmusPlains, BiomeGenBaseMinmus.minmusCanyon), decorators(new WorldGeneratorMinmus())).ice());

        Planets.register(new Planet(Body.MOHO, () -> LegacyBlocks.b("moho_stone"), SpacePlanets::moho, ChunkProviderMoho::new,
                biomes(BiomeGenBaseMoho.mohoCrag, BiomeGenBaseMoho.mohoBasalt, BiomeGenBaseMoho.mohoLavaSea,
                        BiomeGenBaseMoho.mohoPlateau),
                decorators(new WorldGeneratorMoho())));

        Planets.register(new Planet(Body.TEKTO, () -> LegacyBlocks.b("basalt"), SpacePlanets::tekto, ChunkProviderTekto::new,
                biomes(BiomeGenBaseTekto.polyvinylPlains, BiomeGenBaseTekto.halogenHills, BiomeGenBaseTekto.tetrachloricRiver,
                        BiomeGenBaseTekto.forest, BiomeGenBaseTekto.vinylsands),
                decorators(new WorldGeneratorTekto())).ice());

        // Thatmo sat out NTM:Space's body list, and PlanetGen gave it only the shared ores
        BiomeGenBase thatmo = new BiomeGenThatmo(SpaceConfig.thatmoBiome);
        Planets.register(new Planet("thatmo", 0, () -> LegacyBlocks.b("sellafield_slaked"),
                seed -> new WorldChunkManager(thatmo), ChunkProviderThatmo::new, biomes(thatmo), decorators(null)).ice());
    }

    private static WorldChunkManager dres(long seed) {
        GenLayer biomes = new GenLayerDresBiomes(seed);
        biomes = new FuzzyZoom(2000L, biomes);
        biomes = new Zoom(2001L, biomes);
        biomes = new GenLayerDiversifyDres(1000L, biomes);
        biomes = new Zoom(1000L, biomes);
        biomes = new GenLayerDiversifyDres(1001L, biomes);
        biomes = new Zoom(1001L, biomes);
        biomes = new GenLayerDresBasins(3000L, biomes);
        biomes = new Zoom(1003L, biomes);
        biomes = new Smooth(700L, biomes);
        biomes = new GenLayerDresPlains(200L, biomes);
        biomes = new Zoom(1006L, biomes);
        return new WorldChunkManager(biomes, new VoronoiZoom(10L, biomes), seed);
    }

    private static WorldChunkManager duna(long seed) {
        GenLayer biomes = new GenLayerDunaBiomes(seed);
        biomes = new FuzzyZoom(2000L, biomes);
        biomes = new Zoom(2001L, biomes);
        biomes = new GenLayerDiversifyDuna(1000L, biomes);
        biomes = new Zoom(1000L, biomes);
        biomes = new GenLayerDiversifyDuna(1001L, biomes);
        biomes = new Zoom(1001L, biomes);
        biomes = new GenLayerDunaLowlands(1300L, biomes);
        biomes = new Zoom(1003L, biomes);
        biomes = new Smooth(700L, biomes);
        biomes = new Zoom(1005L, biomes);
        biomes = new Smooth(703L, biomes);
        biomes = new FuzzyZoom(1000L, biomes);
        biomes = new Smooth(705L, biomes);
        biomes = new FuzzyZoom(1001L, biomes);
        biomes = new Zoom(1006L, biomes);
        GenLayer voronoi = new VoronoiZoom(10L, biomes);
        GenLayer river = new River(1004L, new Zoom(1000L, biomes));
        river = new Zoom(105L, river);
        return new WorldChunkManager(new RiverMix(100L, biomes, river), voronoi, seed);
    }

    private static WorldChunkManager eve(long seed) {
        GenLayer biomes = new GenLayerEveBiomes(seed);
        for (long s = 1000L; s <= 1005L; s++) biomes = new Zoom(s, biomes);
        GenLayer river = new River(1001L, new Zoom(1000L, biomes));
        GenLayer mix = new GenLayerEveRiverMix(100L, new Smooth(1000L, biomes), new Smooth(1000L, river));
        return new WorldChunkManager(mix, new VoronoiZoom(10L, mix), seed);
    }

    private static WorldChunkManager laythe(long seed) {
        GenLayer biomes = new GenLayerLaytheBiomes(seed);
        biomes = new GenLayerLaythePolar(1000L, biomes);
        biomes = new Zoom(1040L, biomes);
        biomes = Zoom.magnify(1000L, biomes, 1);
        biomes = new GenLayerLaythePolar(1300L, biomes);
        biomes = new GenLayerDiversifyLaythe(1000L, biomes);
        biomes = new GenLayerLaytheOceans(56000L, biomes);
        biomes = new GenLayerLaytheIslands(200L, biomes);
        biomes = new Zoom(1200L, biomes);
        biomes = new Zoom(1001L, biomes);
        biomes = new GenLayerLaytheOceans(4000L, biomes);
        biomes = new GenLayerLaytheOceans(4001L, biomes, BiomeGenBaseLaythe.laytheCoast.biomeID);
        biomes = new Zoom(1003L, biomes);
        biomes = new GenLayerLaytheOceans(4342L, biomes, BiomeGenBaseLaythe.laytheCoast.biomeID);
        biomes = new Smooth(700L, biomes);
        biomes = new Zoom(1006L, biomes);
        return new WorldChunkManager(biomes, new VoronoiZoom(10L, biomes), seed);
    }

    private static WorldChunkManager minmus(long seed) {
        GenLayer biomes = new GenLayerMinmusBiomes(seed);
        biomes = new FuzzyZoom(2000L, biomes);
        biomes = new Zoom(2001L, biomes);
        biomes = new GenLayerDiversify(1000L, biomes, 2, BiomeGenBaseMinmus.minmusCanyon, BiomeGenBaseMinmus.minmusPlains);
        biomes = new Zoom(1000L, biomes);
        biomes = new GenLayerDiversify(1001L, biomes, 2, BiomeGenBaseMinmus.minmusCanyon, BiomeGenBaseMinmus.minmusPlains);
        biomes = new Zoom(1001L, biomes);
        biomes = new GenLayerMinmusBasins(3000L, biomes);
        biomes = new Zoom(1003L, biomes);
        biomes = new Smooth(700L, biomes);
        biomes = new GenLayerMinmusPlains(200L, biomes);
        biomes = new Zoom(1006L, biomes);
        return new WorldChunkManager(biomes, new VoronoiZoom(10L, biomes), seed);
    }

    private static WorldChunkManager moho(long seed) {
        GenLayer biomes = new GenLayerMohoBiomes(seed);
        biomes = new FuzzyZoom(2000L, biomes);
        biomes = new GenLayerDiversify(1234L, biomes, 6, BiomeGenBaseMoho.mohoBasalt);
        biomes = new Zoom(2001L, biomes);
        biomes = new GenLayerMohoPlateauExtend(9944L, biomes);
        biomes = new Zoom(1006L, biomes);
        biomes = new Smooth(700L, biomes);
        biomes = new Zoom(1000L, biomes);
        biomes = new Zoom(107L, biomes);
        GenLayer voronoi = new VoronoiZoom(10L, biomes);
        GenLayer river = new River(1004L, biomes);
        river = new Zoom(105L, river);
        river = new Zoom(106L, river);
        return new WorldChunkManager(new RiverMix(100L, biomes, river), voronoi, seed);
    }

    private static WorldChunkManager tekto(long seed) {
        GenLayer biomes = new GenlayerTektoBiomes(seed);
        for (long s = 1000L; s <= 1005L; s++) biomes = new Zoom(s, biomes);
        GenLayer river = new River(1001L, new Zoom(1000L, biomes));
        GenLayer mix = new GenLayerTektoRiverMix(100L, new Smooth(1000L, biomes), new Smooth(1000L, river));
        return new WorldChunkManager(mix, new VoronoiZoom(10L, mix), seed);
    }
}
