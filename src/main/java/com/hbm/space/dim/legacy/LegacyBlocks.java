// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space.dim.legacy;

import com.hbm.NuclearTech;
import com.hbm.lib.Library;
import com.hbm.space.BlockSpaceOre;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * NTM's blocks by their NTM:Space names, and the block states 1.7 block and metadata pairs stand
 * for: a space ore's metadata is the body it sits on, stained clay's its colour.
 */
public final class LegacyBlocks {

    private static final Map<String, Block> BY_NAME = new ConcurrentHashMap<>();
    private static final Block[] CLAY = {Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA,
            Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA,
            Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA,
            Blocks.BLUE_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA};

    /** NTM:Space names the port spells differently, or gives to a vanilla block. */
    private static final Map<String, String> ALIASES = Map.of(
            "ore_cinnebar", "hbm:ore_cinnabar",
            "ore_depth_cinnebar", "hbm:ore_depth_cinnabar",
            "ore_copper", "minecraft:copper_ore");

    /**
     * 1.7 blocks whose metadata picked a variant that is its own block now, by metadata. Calcium
     * (stone_resource 7) never had a block; Space's limestone spawn asked for it, so it is limestone.
     */
    private static final Map<String, String[]> VARIANTS = Map.of(
            "stone_resource", new String[] {"stone_resource_sulfur", "stone_resource_asbestos", "stone_resource_hematite",
                    "stone_resource_malachite", "stone_resource_limestone", "stone_resource_bauxite", "stone_resource_limestone",
                    "stone_resource_limestone"},
            "ore_basalt", new String[] {"ore_basalt_sulfur", "ore_basalt_fluorite", "ore_basalt_asbestos", "ore_basalt_gem",
                    "ore_basalt_molysite"},
            // BlockNTMFlower's EnumFlowerType order; strawberry and mint have no block here
            "plant_flower", new String[] {"plant_flower_foxglove", "plant_flower_tobacco", "plant_flower_nightshade",
                    "plant_flower_weed", "plant_flower_cd0", "plant_flower_cd1"});

    private static final Map<Block, Block[]> VARIANT_BLOCKS = new ConcurrentHashMap<>();

    private LegacyBlocks() {}

    /** An NTM block by name; air, with a warning, for one this port has not brought over. */
    public static Block b(String name) {
        return BY_NAME.computeIfAbsent(name, n -> {
            String[] variants = VARIANTS.get(n);
            if (variants != null) {
                Block[] blocks = new Block[variants.length];
                for (int i = 0; i < variants.length; i++) blocks[i] = b(variants[i]);
                VARIANT_BLOCKS.put(blocks[0], blocks);
                return blocks[0];
            }
            var id = ALIASES.containsKey(n) ? net.minecraft.resources.ResourceLocation.parse(ALIASES.get(n)) : Library.id(n);
            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                NuclearTech.LOGGER.warn("NTM:Space world generation wants the missing block {}", id);
                return Blocks.AIR;
            }
            return BuiltInRegistries.BLOCK.get(id);
        });
    }

    public static BlockState state(Block block, int meta) {
        if (block == null) return Blocks.AIR.defaultBlockState();
        if (block == Blocks.TERRACOTTA && meta > 0) return CLAY[meta & 15].defaultBlockState();
        Block[] variants = VARIANT_BLOCKS.get(block);
        if (variants != null) return variants[Math.min(meta & 15, variants.length - 1)].defaultBlockState();
        BlockState state = block.defaultBlockState();
        if (meta == 0) return state;
        if (state.hasProperty(BlockSpaceOre.BODY)) {
            var bodies = BlockSpaceOre.OreBody.values();
            return state.setValue(BlockSpaceOre.BODY, bodies[Math.min(meta, bodies.length - 1)]);
        }
        // 1.7 metadata was the ordinal of a block's one variant property
        for (var property : state.getProperties()) {
            if (property instanceof net.minecraft.world.level.block.state.properties.EnumProperty<?> e) {
                var values = new java.util.ArrayList<>(e.getPossibleValues());
                if (meta < values.size()) return with(state, e, values.get(meta));
            }
        }
        if (state.hasProperty(BlockStateProperties.LEVEL)) {
            IntegerProperty level = BlockStateProperties.LEVEL;
            return state.setValue(level, Math.min(meta, 15));
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState with(BlockState state, net.minecraft.world.level.block.state.properties.Property property, Object value) {
        return state.setValue(property, (Comparable) value);
    }

    /** The metadata a state would have had, the inverse of {@link #state}. */
    public static int meta(BlockState state) {
        for (Block[] variants : VARIANT_BLOCKS.values())
            for (int i = 0; i < variants.length; i++) if (state.is(variants[i])) return i;
        if (state.hasProperty(BlockSpaceOre.BODY)) return state.getValue(BlockSpaceOre.BODY).ordinal();
        for (int i = 0; i < CLAY.length; i++) if (state.is(CLAY[i])) return i;
        return 0;
    }

    /** The block a state is, with stained clay folded back into terracotta. */
    public static Block block(BlockState state) {
        for (Block clay : CLAY) if (state.is(clay)) return Blocks.TERRACOTTA;
        for (Block[] variants : VARIANT_BLOCKS.values())
            for (Block variant : variants) if (state.is(variant)) return variants[0];
        return state.isAir() ? Blocks.AIR : state.getBlock();
    }
}
