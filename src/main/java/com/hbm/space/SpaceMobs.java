// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.entity.mob.EntityCreeperBase;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockMutatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemSpawnEgg;
import com.hbm.lib.Library;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/** NTM:Space's flesh creeper and moon cow. */
public final class SpaceMobs {

    private SpaceMobs() {}

    public static final RegistryHandle<EntityType<FleshCreeper>> FLESH_CREEPER = Reg.entity("entity_mob_flesh_creeper",
            () -> EntityType.Builder.<FleshCreeper>of(FleshCreeper::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).clientTrackingRange(5).updateInterval(3)
                    .build(key("entity_mob_flesh_creeper")));
    public static final RegistryHandle<EntityType<MoonCow>> MOON_COW = Reg.entity("entity_moon_cow",
            () -> EntityType.Builder.<MoonCow>of(MoonCow::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.4F).clientTrackingRange(10)
                    .build(key("entity_moon_cow")));

    public static final RegistryHandle<ItemSpawnEgg> FLESH_CREEPER_EGG = Reg.item("entity_mob_flesh_creeper_spawn_egg",
            props -> new ItemSpawnEgg(() -> FLESH_CREEPER, 0xFF0000, 0xD65337, props), Item.Properties::new);
    public static final RegistryHandle<ItemSpawnEgg> MOON_COW_EGG = Reg.item("entity_moon_cow_spawn_egg",
            props -> new ItemSpawnEgg(() -> MOON_COW, 0xECD376, 0x262A44, props), Item.Properties::new);

    static {
        NeoForgeRegistrar.registerLivingAttributes(FLESH_CREEPER, Creeper::createAttributes);
        NeoForgeRegistrar.registerSpawnPlacement(FLESH_CREEPER, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        NeoForgeRegistrar.registerLivingAttributes(MOON_COW, Cow::createAttributes);
        NeoForgeRegistrar.registerSpawnPlacement(MOON_COW, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
    }

    private static String key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Library.id(name)).location().toString();
    }

    public static void init() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(SpaceMobs::onPotentialSpawns);
    }

    /**
     * ChunkProviderLaythe.getPossibleCreatures: on Laythe, the monsters that spawn on tumour are
     * flesh creepers and nothing else.
     */
    private static void onPotentialSpawns(net.neoforged.neoforge.event.level.LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory() != MobCategory.MONSTER || !SpaceToggle.enabled()) return;
        if (!(event.getLevel() instanceof Level level) || level.dimension() != SpaceDimensions.LAYTHE) return;
        if (!level.getBlockState(event.getPos().below()).is(SpaceBlocks.TUMOR.get())) return;
        for (var data : java.util.List.copyOf(event.getSpawnerDataList())) event.removeSpawnerData(data);
        event.addSpawnerData(new net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData(FLESH_CREEPER.get(), 10, 4, 4));
    }

    /** Bursts into giblets and covers what it blows up in tumours. */
    public static class FleshCreeper extends EntityCreeperBase {
        public FleshCreeper(EntityType<? extends FleshCreeper> type, Level level) {
            super(type, level);
        }

        @Override
        protected void detonate(ServerLevel level) {
            com.hbm.particle.helper.ParticleCreators.giblets(level, this, 0, 2);
            ExplosionVNT vnt = new ExplosionVNT(level, getX(), getY(), getZ(), isPowered() ? 7 : 4, this);
            vnt.setBlockAllocator(new BlockAllocatorBulkie(30, isPowered() ? 16 : 8));
            vnt.setBlockProcessor(new BlockProcessorStandard()
                    .withBlockEffect(new BlockMutatorBulkie(SpaceBlocks.TUMOR.get().defaultBlockState())));
            vnt.setEntityProcessor(new EntityProcessorStandard().withRangeMod(0.25F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectStandard());
            vnt.explode();
        }
    }

    /** A cow in a space helmet: quiet, and drops cheese as well as leather. */
    public static class MoonCow extends Cow {
        public MoonCow(EntityType<? extends Cow> type, Level level) {
            super(type, level);
        }

        @Override
        public @Nullable Cow getBreedOffspring(ServerLevel level, AgeableMob mate) {
            return MOON_COW.get().create(level);
        }

        @Override
        protected float getSoundVolume() {
            return 0.1F;
        }

        @Override
        protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
            super.dropCustomDeathLoot(level, source, recentlyHit);
            spawnAtLocation(new net.minecraft.world.item.ItemStack(ModItems.CHEESE.get(), 1 + random.nextInt(3)));
        }
    }
}
