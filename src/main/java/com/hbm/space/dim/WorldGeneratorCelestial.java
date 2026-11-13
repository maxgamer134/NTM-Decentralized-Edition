// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.dim;

import com.hbm.space.dim.legacy.*;
import com.hbm.space.dim.legacy.Layers.*;
import com.hbm.space.dim.legacy.Features.*;
import com.hbm.space.dim.legacy.LegacyConfig.*;
import com.hbm.space.dim.Planet;
import java.util.*;
import com.hbm.inventory.fluid.FluidStackNTM;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;
import java.util.function.*;
import java.util.stream.*;
import com.hbm.util.BobMathUtil;
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
import com.hbm.space.dim.legacy.*;
import com.hbm.space.dim.mapgen.*;
import com.hbm.space.dim.minmus.*;
import com.hbm.space.dim.minmus.biome.*;
import com.hbm.space.dim.minmus.genlayer.*;
import com.hbm.space.dim.moho.*;
import com.hbm.space.dim.moho.biome.*;
import com.hbm.space.dim.moho.genlayer.*;
import com.hbm.space.dim.moon.*;
import com.hbm.space.dim.noise.*;
import com.hbm.space.dim.tekto.*;
import com.hbm.space.dim.tekto.GenLayerTekto.*;
import com.hbm.space.dim.tekto.biome.*;
import com.hbm.space.dim.thatmo.*;





public class WorldGeneratorCelestial implements Planet.Decorator {

	// Register system-wide structures
	public WorldGeneratorCelestial() {
		// NTM:Space registered its structures here; they come with the structure port
	}

	@Override
	public void generate(Random rand, int chunkX, int chunkZ, World world) {
		Block blockToReplace = world.stone();
		FluidStackNTM drillAcid = world.bedrockAcid;
		boolean hasIce = world.hasIce;
		int meta = world.bodyMeta();

		generateStructures(world, rand, chunkX * 16, chunkZ * 16);

		// Generate vanilla ores too
		if(blockToReplace != Blocks.STONE) {
			generateVanillaOres(world, rand, chunkX * 16, chunkZ * 16, blockToReplace, meta);
		}

		generateNTMOres(world, rand, chunkX * 16, chunkZ * 16, blockToReplace, meta);
		generateBedrockOres(world, rand, chunkX * 16, chunkZ * 16, blockToReplace, hasIce, drillAcid);
	}

	public void generateStructures(World world, Random rand, int x, int z) {
		// NTM's fallen meteorites, which NTM:Space let fall on every body (1 in 200 chunks)
		if(rand.nextInt(200) == 0 && Boolean.TRUE.equals(com.hbm.config.NeoForgeHbmConfig.STORE.get(com.hbm.config.ConfigSchema.WORLD_METEORITES))) {
			var level = world.level();
			var feature = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
					.get(com.hbm.lib.Library.id("meteorite"));
			if(feature != null) {
				var at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
						new net.minecraft.core.BlockPos(x + rand.nextInt(16) + 8, 0, z + rand.nextInt(16) + 8));
				feature.place(level, level.getLevel().getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(rand.nextLong()), at);
			}
		}
	}

	public void generateNTMOres(World world, Random rand, int x, int z, Block planetStone, int meta) {

		if(WorldConfig.alexandriteSpawn > 0 && rand.nextInt(WorldConfig.alexandriteSpawn) == 0) {
			Features.generateOre(world, rand, x, z, 1, 3, 10, 5, LegacyBlocks.b("ore_alexandrite"), 0, planetStone);
		}

		Features.depthDeposit(world, x, 0, 3, z, 5, 0.6D, LegacyBlocks.b("cluster_depth_iron"), rand, 24, planetStone, LegacyBlocks.b("stone_depth"));
		Features.depthDeposit(world, x, 0, 3, z, 5, 0.6D, LegacyBlocks.b("cluster_depth_titanium"), rand, 32, planetStone, LegacyBlocks.b("stone_depth"));
		Features.depthDeposit(world, x, 0, 3, z, 5, 0.6D, LegacyBlocks.b("cluster_depth_tungsten"), rand, 32, planetStone, LegacyBlocks.b("stone_depth"));
		Features.depthDeposit(world, x, 0, 3, z, 5, 0.8D, LegacyBlocks.b("ore_depth_cinnebar"), rand, 16, planetStone, LegacyBlocks.b("stone_depth"));
		Features.depthDeposit(world, x, 0, 3, z, 5, 0.8D, LegacyBlocks.b("ore_depth_zirconium"), rand, 16, planetStone, LegacyBlocks.b("stone_depth"));
		Features.depthDeposit(world, x, 0, 3, z, 5, 0.8D, LegacyBlocks.b("ore_depth_borax"), rand, 16, planetStone, LegacyBlocks.b("stone_depth"));

		Features.generateOre(world, rand, x, z, WorldConfig.uraniumSpawn, 5, 5, 20, LegacyBlocks.b("ore_uranium"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.thoriumSpawn, 5, 5, 25, LegacyBlocks.b("ore_thorium"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.titaniumSpawn, 6, 5, 30, LegacyBlocks.b("ore_titanium"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.sulfurSpawn, 8, 5, 30, LegacyBlocks.b("ore_sulfur"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.aluminiumSpawn, 6, 5, 40, LegacyBlocks.b("ore_aluminium"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.copperSpawn, 6, 5, 45, LegacyBlocks.b("ore_copper"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.nickelSpawn, 6, 5, 10, LegacyBlocks.b("ore_nickel"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.zincSpawn, 6, 5, 32, LegacyBlocks.b("ore_zinc"), meta, planetStone);
		//Features.generateOre(world, rand, x, z, WorldConfig.mineralSpawn, 10, 12, 32, LegacyBlocks.b("ore_mineral"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.fluoriteSpawn, 4, 5, 45, LegacyBlocks.b("ore_fluorite"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.niterSpawn, 6, 5, 30, LegacyBlocks.b("ore_niter"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.tungstenSpawn, 8, 5, 30, LegacyBlocks.b("ore_tungsten"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.leadSpawn, 9, 5, 30, LegacyBlocks.b("ore_lead"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.berylliumSpawn, 4, 5, 30, LegacyBlocks.b("ore_beryllium"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.rareSpawn, 5, 5, 20, LegacyBlocks.b("ore_rare"), meta, planetStone);
		// Features.generateOre(world, rand, x, z, WorldConfig.asbestosSpawn, 4, 16, 16, LegacyBlocks.b("ore_asbestos"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.cinnebarSpawn, 4, 8, 16, LegacyBlocks.b("ore_cinnebar"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.cobaltSpawn, 4, 4, 8, LegacyBlocks.b("ore_cobalt"), meta, planetStone);

		Features.generateOre(world, rand, x, z, WorldConfig.ironClusterSpawn, 6, 15, 45, LegacyBlocks.b("cluster_iron"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.titaniumClusterSpawn, 6, 15, 30, LegacyBlocks.b("cluster_titanium"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.aluminiumClusterSpawn, 6, 15, 35, LegacyBlocks.b("cluster_aluminium"), meta, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.copperClusterSpawn, 6, 15, 20, LegacyBlocks.b("cluster_copper"), meta, planetStone);

		//Features.generateOre(world, rand, x, z, WorldConfig.malachiteSpawn, 16, 6, 40, LegacyBlocks.b("stone_resource"), 3, planetStone);
		Features.generateOre(world, rand, x, z, WorldConfig.limestoneSpawn, 12, 25, 30, LegacyBlocks.b("stone_resource"), 7, planetStone);

		if(rand.nextInt(4) == 0) {
			int rx = x + rand.nextInt(16) + 8;
			int ry = 6 + rand.nextInt(13);
			int rz = z + rand.nextInt(16) + 8;

			if(world.getBlock(rx, ry, rz) == planetStone) {
				world.setBlock(rx, ry, rz, LegacyBlocks.b("stone_keyhole"));
			}
		}
	}

	public void generateBedrockOres(World world, Random rand, int x, int z, Block planetStone, boolean hasIce, FluidStackNTM drillAcid) {
		if(world.level() == null) return; // the recipe viewer's dry run: bedrock ores have their own page
		if(rand.nextInt(10) == 0) {
			int randPosX = x + rand.nextInt(2) + 8;
			int randPosZ = z + rand.nextInt(2) + 8;

			// the port's bedrock ore field and ore set stand in for CelestialBedrockOre
			var level = world.level();
			var field = com.hbm.world.NtmWorldgenFields.get(level).bedrock();
			var set = com.hbm.items.special.BedrockOreSet.OF.get().apply(level.getLevel());
			double richness = field.richness(randPosX, randPosZ), total = 0;
			for(var kind : set.kinds()) total += field.oreLevel(richness, randPosX, randPosZ, kind);
			int tier = com.hbm.world.feature.BedrockOreFeature.getTier(total / Math.max(1, set.kinds().size()));
			FluidStackNTM acid = drillAcid != null ? drillAcid : com.hbm.world.feature.BedrockOreFeature.getBoreFluid(tier);
			bedrockOre(world, rand, randPosX, randPosZ, new net.minecraft.world.item.ItemStack(com.hbm.items.ModItems.BEDROCK_ORE_BASE.get()), acid, 0xD78A16, tier, planetStone);
		} else if(hasIce && rand.nextInt(3) == 0) {
			int randPosX = x + rand.nextInt(2) + 8;
			int randPosZ = z + rand.nextInt(2) + 8;

			bedrockOre(world, rand, randPosX, randPosZ, new net.minecraft.world.item.ItemStack(Blocks.PACKED_ICE, 8 * 4), null, 0xAAFFFF, 1, Blocks.STONE);
		}
	}

	private static void bedrockOre(World world, Random rand, int x, int z, net.minecraft.world.item.ItemStack stack, FluidStackNTM acid, int color, int tier, Block target) {
		com.hbm.world.feature.BedrockOreFeature.generate(world.level(), x, z, stack, acid, color, tier,
				LegacyBlocks.b("stone_depth").defaultBlockState(), state -> com.hbm.space.dim.DeepStone.matches(state.getBlock(), target), net.minecraft.util.RandomSource.create(rand.nextLong()));
	}

	// This will generate vanilla ores for the chunk when the biome decorator fails to find any regular stone
	public void generateVanillaOres(World world, Random rand, int x, int z, Block planetStone, int meta) {
		genVanillaOre(world, rand, x, z, 0, 64, 20, 8, LegacyBlocks.b("ore_iron"), planetStone, meta);
		genVanillaOre(world, rand, x, z, 0, 32, 2, 8, LegacyBlocks.b("ore_gold"), planetStone, meta);
		genVanillaOre(world, rand, x, z, 0, 16, 8, 7, LegacyBlocks.b("ore_redstone"), planetStone, meta);
		genVanillaOre(world, rand, x, z, 0, 16, 1, 7, LegacyBlocks.b("ore_diamond"), planetStone, meta);
		// what the fuck is a lapis lazuli
		// emeralds also spawn in a special way but... like... fuck emeralds
	}

	// A simple reimplementation of `genStandardOre1` without needing to instance a BiomeDecorator
	private void genVanillaOre(World world, Random rand, int x, int z, int yMin, int yMax, int count, int numberOfBlocks, Block ore, Block target, int meta) {
		if(Features.recorded(ore, meta, target, count, numberOfBlocks, yMin, yMax - 1, "vein")) return;
		WorldGenMinable worldGenMinable = new WorldGenMinable(ore, meta, numberOfBlocks, target);

		for (int l = 0; l < count; ++l) {
			int genX = x + rand.nextInt(16);
			int genY = Features.stretch(rand.nextInt(yMax - yMin) + yMin); // millenial supremacy
			int genZ = z + rand.nextInt(16);
			worldGenMinable.generate(world, rand, genX, genY, genZ);
		}
	}

}

