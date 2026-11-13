// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * A 1.7 biome as NTM:Space's planets describe them: its terrain height, its surface and filler
 * blocks, how it lays them down and how it decorates a chunk. Each one is also a data pack biome
 * under its {@link #key}, which carries what modern Minecraft wants from a biome (sky and fog
 * colours, spawns, weather).
 */
public class BiomeGenBase {

    public static final int OCEAN_ID = 0, RIVER_ID = 7, FROZEN_RIVER_ID = 11, ICE_PLAINS_ID = 12, DEEP_OCEAN_ID = 24;

    private static final BiomeGenBase[] BIOMES = new BiomeGenBase[256];

    /** The vanilla river, which 1.7's river layers put on planets too. */
    public static final BiomeGenBase river = new BiomeGenBase(RIVER_ID, ResourceLocation.withDefaultNamespace("river"))
            .setHeight(new Height(-0.5F, 0.0F));
    public static final BiomeGenBase mushroomIsland = new BiomeGenBase(14, ResourceLocation.withDefaultNamespace("mushroom_fields"));
    public static final BiomeGenBase mushroomIslandShore = mushroomIsland;
    public static final BiomeGenBase frozenRiver = new BiomeGenBase(FROZEN_RIVER_ID, ResourceLocation.withDefaultNamespace("frozen_river"))
            .setHeight(new Height(-0.5F, 0.0F));

    public final int biomeID;
    public final ResourceKey<Biome> key;
    public String biomeName = "";
    public Block topBlock = Blocks.GRASS_BLOCK;
    public Block fillerBlock = Blocks.DIRT;
    public int field_150604_aj;
    public float rootHeight = 0.1F;
    public float heightVariation = 0.2F;
    public float temperature = 0.5F;
    public float rainfall = 0.5F;
    public boolean enableRain = true;
    public boolean enableSnow;
    /** The water tint, which the biome's data pack file carries. */
    public int waterColorMultiplier = 0xFFFFFF;
    public BiomeDecorator theBiomeDecorator = new BiomeDecorator();

    protected final List<SpawnListEntry> creatures = new ArrayList<>();
    protected final List<SpawnListEntry> monsters = new ArrayList<>();
    protected final List<SpawnListEntry> waterCreatures = new ArrayList<>();
    protected final List<SpawnListEntry> caveCreatures = new ArrayList<>();

    private BiomeGenBase(int id, ResourceLocation key) {
        this.biomeID = id;
        this.key = ResourceKey.create(Registries.BIOME, key);
        if (BIOMES[id] != null) throw new IllegalStateException("biome id " + id + " is taken by " + BIOMES[id].key.location());
        BIOMES[id] = this;
    }

    /** A planet biome, a data pack biome of NTM's under its id. */
    protected BiomeGenBase(int id, String name) {
        this(id, Library.id(name));
    }

    public static BiomeGenBase[] getBiomeGenArray() {
        return BIOMES;
    }

    public static BiomeGenBase getBiome(int id) {
        BiomeGenBase biome = id >= 0 && id < BIOMES.length ? BIOMES[id] : null;
        return biome != null ? biome : river;
    }

    public BiomeGenBase setBiomeName(String name) {
        this.biomeName = name;
        return this;
    }

    public BiomeGenBase setHeight(Height height) {
        this.rootHeight = height.rootHeight;
        this.heightVariation = height.variation;
        return this;
    }

    public BiomeGenBase setTemperatureRainfall(float temperature, float rainfall) {
        this.temperature = temperature;
        this.rainfall = rainfall;
        return this;
    }

    public BiomeGenBase setDisableRain() {
        this.enableRain = false;
        return this;
    }

    public BiomeGenBase setEnableSnow() {
        this.enableSnow = true;
        return this;
    }

    public BiomeGenBase setColor(int color) {
        return this;
    }

    public float getFloatTemperature(int x, int y, int z) {
        return y > 64 ? temperature - (y - 64) * 0.05F / 30F : temperature;
    }

    public float getSpawningChance() {
        return 0.1F;
    }

    public List<SpawnListEntry> creatures() {
        return creatures;
    }

    public List<SpawnListEntry> monsters() {
        return monsters;
    }

    public List<SpawnListEntry> waterCreatures() {
        return waterCreatures;
    }

    public void decorate(World world, Random rand, int x, int z) {
        theBiomeDecorator.decorateChunk(world, rand, this, x, z);
    }

    /** 1.7's genBiomeTerrain: the surface and filler over stone, bedrock below. */
    public void genTerrainBlocks(World world, Random rand, Block[] blocks, byte[] meta, int x, int z, double noise) {
        Block top = topBlock;
        byte topMeta = (byte) (field_150604_aj & 255);
        Block filler = fillerBlock;
        int run = -1;
        int depth = (int) (noise / 3.0 + 3.0 + rand.nextDouble() * 0.25);
        int lx = x & 15, lz = z & 15;
        int height = blocks.length / 256;
        for (int y = 319; y >= -64; y--) {
            int i = (lz * 16 + lx) * height + y - ChunkProviderCelestial.MIN_Y;
            if (y <= -64 + rand.nextInt(5)) {
                blocks[i] = Blocks.BEDROCK;
                continue;
            }
            Block block = blocks[i];
            if (block == null || Material.of(block) == Material.air) {
                run = -1;
                continue;
            }
            if (block != Blocks.STONE) continue;
            if (run == -1) {
                if (depth <= 0) {
                    top = null;
                    topMeta = 0;
                    filler = Blocks.STONE;
                } else if (y >= 59 && y <= 64) {
                    top = topBlock;
                    topMeta = (byte) (field_150604_aj & 255);
                    filler = fillerBlock;
                }
                if (y < 63 && (top == null || Material.of(top) == Material.air)) {
                    top = getFloatTemperature(x, y, z) < 0.15F ? Blocks.ICE : Blocks.WATER;
                    topMeta = 0;
                }
                run = depth;
                if (y >= 62) {
                    blocks[i] = top;
                    meta[i] = topMeta;
                } else if (y < 56 - depth) {
                    top = null;
                    filler = Blocks.STONE;
                    blocks[i] = Blocks.GRAVEL;
                } else {
                    blocks[i] = filler;
                }
            } else if (run > 0) {
                run--;
                blocks[i] = filler;
                if (run == 0 && filler == Blocks.SAND) {
                    run = rand.nextInt(4) + Math.max(0, y - 63);
                    filler = Blocks.SANDSTONE;
                }
            }
        }
    }

    public static class Height {
        public final float rootHeight;
        public final float variation;

        public Height(float rootHeight, float variation) {
            this.rootHeight = rootHeight;
            this.variation = variation;
        }
    }

    /** A spawn entry, which the biome's data pack file carries for modern Minecraft. */
    public record SpawnListEntry(Class<? extends Entity> entityClass, int weight, int min, int max) {}
}
