// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import com.hbm.items.special.ItemSpawnEgg;
import com.hbm.lib.Library;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Laythe's sea life: the scutterfish, scrapfish, sifter eel, depth squid and scuttlecrab. */
public final class SpaceSeaMobs {

    private SpaceSeaMobs() {}

    public static final RegistryHandle<EntityType<Scutterfish>> SCUTTERFISH = fish("entity_scutterfish", Scutterfish::new, 2F, 2F);
    public static final RegistryHandle<EntityType<ScrapFish>> SCRAPFISH = fish("entity_scrapfish", ScrapFish::new, 2F, 2F);
    public static final RegistryHandle<EntityType<SifterEel>> SIFTER_EEL = fish("entity_siftereel", SifterEel::new, 2F, 2F);
    public static final RegistryHandle<EntityType<DepthSquid>> DEPTH_SQUID = fish("entity_depthsquid", DepthSquid::new, 0.95F, 0.95F);
    public static final RegistryHandle<EntityType<Scuttlecrab>> SCUTTLECRAB = fish("entity_scuttlecrab", Scuttlecrab::new, 0.6F, 1.8F);

    static {
        egg("entity_scutterfish", SCUTTERFISH, 0xC8C9CD, 0x858894);
        egg("entity_scuttlecrab", SCUTTLECRAB, 0xF17951, 0xEDDABB);
        egg("entity_depthsquid", DEPTH_SQUID, 0x00B4DF, 0x016085);
        egg("entity_scrapfish", SCRAPFISH, 0xDF9835, 0x510E13);
        egg("entity_siftereel", SIFTER_EEL, 0x5B963E, 0xC0B286);
        for (RegistryHandle<? extends EntityType<? extends WaterAnimal>> type : java.util.List.of(SCUTTERFISH, SCRAPFISH, SIFTER_EEL)) {
            @SuppressWarnings("unchecked")
            RegistryHandle<EntityType<WaterAnimal>> t = (RegistryHandle<EntityType<WaterAnimal>>) (Object) type;
            NeoForgeRegistrar.registerLivingAttributes(t, Fish::createFishAttributes);
            NeoForgeRegistrar.registerSpawnPlacement(t, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    SpaceSeaMobs::checkWaterMobSpawn);
        }
        NeoForgeRegistrar.registerLivingAttributes(SCUTTLECRAB, Mob::createMobAttributes);
        NeoForgeRegistrar.registerSpawnPlacement(SCUTTLECRAB, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SpaceSeaMobs::checkWaterMobSpawn);
        NeoForgeRegistrar.registerLivingAttributes(DEPTH_SQUID, Squid::createAttributes);
        NeoForgeRegistrar.registerSpawnPlacement(DEPTH_SQUID, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SpaceSeaMobs::checkWaterMobSpawn);
    }

    private static <T extends Mob> RegistryHandle<EntityType<T>> fish(String name, EntityType.EntityFactory<T> factory, float w, float h) {
        return Reg.entity(name, () -> EntityType.Builder.of(factory, MobCategory.WATER_CREATURE).sized(w, h).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Library.id(name)).location().toString()));
    }

    private static <T extends Mob> void egg(String name, RegistryHandle<EntityType<T>> type, int bg, int fg) {
        Reg.item(name + "_spawn_egg", props -> new ItemSpawnEgg(() -> type, bg, fg, props), Item.Properties::new);
    }

    public static void init() {}

    /**
     * EntityWaterMob.getCanSpawnHere: anywhere in water with room for it, at any depth (the depth
     * squid lives deep), instead of vanilla's thirteen blocks under the surface.
     */
    static boolean checkWaterMobSpawn(EntityType<? extends Mob> type, net.minecraft.world.level.LevelAccessor level,
            net.minecraft.world.entity.MobSpawnType reason, net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                && level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER);
    }

    /** The looting level of whoever got the kill, for the 1.7 dropFewItems rolls. */
    static int looting(ServerLevel level, DamageSource source) {
        if (!(source.getEntity() instanceof net.minecraft.world.entity.LivingEntity killer)) return 0;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), killer);
    }

    /** EntityLivingBase.dropFewItems: 0-2 of the drop item, plus up to the looting level. */
    static int dropCount(ServerLevel level, DamageSource source, net.minecraft.util.RandomSource random) {
        int n = random.nextInt(3);
        int looting = looting(level, source);
        if (looting > 0) n += random.nextInt(looting + 1);
        return n;
    }

    /** A mob with one of several skins, picked when it spawns and kept from then on. */
    public interface Variant {
        int variant();
    }

    private static final EntityDataAccessor<Integer> FISH_VARIANT = SynchedEntityData.defineId(Fish.class, EntityDataSerializers.INT);

    /** Swims toward a target that changes now and then, and flops about on land. */
    public abstract static class Fish extends WaterAnimal implements Variant {
        private Vec3 swimTarget;
        private final double swimSpeed;
        private final float watchDistance;

        protected Fish(EntityType<? extends Fish> type, Level level, double swimSpeed, float watchDistance) {
            super(type, level);
            this.swimSpeed = swimSpeed;
            this.watchDistance = watchDistance;
            this.swimTarget = position();
            registerFishGoals();
        }

        public static AttributeSupplier.Builder createFishAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 15.0).add(Attributes.FOLLOW_RANGE, 10.0);
        }

        /** The goals need the speeds, which the constructor sets after registerGoals runs. */
        private void registerFishGoals() {
            goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, watchDistance));
            goalSelector.addGoal(1, new RandomLookAroundGoal(this));
            goalSelector.addGoal(2, new RandomStrollGoal(this, swimSpeed));
        }

        protected int variantCount() {
            return 1;
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(FISH_VARIANT, -1);
        }

        @Override
        public int variant() {
            int v = entityData.get(FISH_VARIANT);
            if (v < 0 && !level().isClientSide()) {
                v = random.nextInt(variantCount());
                entityData.set(FISH_VARIANT, v);
            }
            return Math.max(v, 0);
        }

        @Override
        public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag output) {
            super.addAdditionalSaveData(output);
            output.putInt("variant", variant());
        }

        @Override
        public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag input) {
            super.readAdditionalSaveData(input);
            entityData.set(FISH_VARIANT, (input.contains("variant") ? input.getInt("variant") : -1));
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep(); ServerLevel level = (ServerLevel) level();
            variant();
            if (!isInWater()) {
                setXRot(getXRot() + 0.2F);
                setYRot(0F);
                setJumping(true);
                if (onGround()) {
                    push(0.4 * random.nextDouble() - 0.4 * random.nextDouble(), 0.0, 0.4 * random.nextDouble() - 0.4 * random.nextDouble());
                }
            }
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (random.nextInt(200) == 0 || !isInWater()) {
                double tx = getX() + (random.nextDouble() - 0.5) * 20.0;
                double ty = Mth.clamp(getY() + (random.nextDouble() - 0.5) * 8.0, level().getMinBuildHeight(), level().getMaxBuildHeight() - 1);
                double tz = getZ() + (random.nextDouble() - 0.5) * 20.0;
                swimTarget = new Vec3(tx, ty, tz);
            }
            Vec3 d = swimTarget.subtract(position());
            double distance = d.length();
            if (distance > 0) {
                d = d.scale(1 / distance);
                double speed = 0.01;
                setDeltaMovement(getDeltaMovement().add(d.x * speed + (random.nextDouble() - 0.5) * 0.02,
                        d.y * speed + (random.nextDouble() - 0.5) * 0.02, d.z * speed + (random.nextDouble() - 0.5) * 0.02));
            }
            Vec3 m = getDeltaMovement();
            float targetYaw = (float) (Math.atan2(m.z, m.x) * (180D / Math.PI)) - 90F;
            float targetPitch = (float) -(Math.atan2(m.y, Math.sqrt(m.x * m.x + m.z * m.z)) * (180D / Math.PI));
            setYRot(approach(getYRot(), targetYaw, 10F));
            setXRot(approach(getXRot(), targetPitch, 10F));
            yBodyRot = getYRot();
        }

        private static float approach(float current, float target, float max) {
            return current + Mth.clamp(Mth.wrapDegrees(target - current), -max, max);
        }

        @Override
        public void travel(Vec3 input) {
            if (isInWater()) {
                Vec3 m = getDeltaMovement();
                setDeltaMovement(m.x, m.y * 0.8F, m.z);
                move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
                calculateEntityAnimation(false);
            } else {
                super.travel(input);
            }
        }
    }

    public static class Scutterfish extends Fish {
        public Scutterfish(EntityType<? extends Scutterfish> type, Level level) {
            super(type, level, 1.5, 6.0F);
        }

        @Override
        protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
            super.dropCustomDeathLoot(level, source, recentlyHit);
            int n = dropCount(level, source, random);
            if (n > 0) spawnAtLocation(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Library.id("scuttertail")), n));
        }
    }

    /** STEEL, ALUMINIUM, ISOTOPE, CADMIUM, TECH, BLOOD, HORROR. Drops machine parts. */
    public static class ScrapFish extends Fish {
        public static final String[] VARIANTS = {"steel", "aluminium", "isotope", "cadmium", "tech", "blood", "horror"};

        public ScrapFish(EntityType<? extends ScrapFish> type, Level level) {
            super(type, level, 0.8, 4.0F);
        }

        @Override
        protected int variantCount() {
            return VARIANTS.length;
        }

        @Override
        protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
            super.dropCustomDeathLoot(level, source, recentlyHit);
            int j = random.nextInt(3 + looting(level, source)) + 1;
            for (int k = 0; k < j; k++) {
                ItemStack stack = ItemPool.getStack(ItemPools.POOL_MACHINE_PARTS, ItemPool.context(level, random));
                if (!stack.isEmpty()) spawnAtLocation(stack.copy());
            }
        }
    }

    /** PLAIN, FAST, EXOTIC, PHASED, ELEMENTAL, PERFECT. */
    public static class SifterEel extends Fish {
        public static final String[] VARIANTS = {"plain", "fast", "exotic", "phased", "elemental", "perfect"};

        public SifterEel(EntityType<? extends SifterEel> type, Level level) {
            super(type, level, 1.8, 8.0F);
        }

        @Override
        protected int variantCount() {
            return VARIANTS.length;
        }
    }

    private static final EntityDataAccessor<Integer> SQUID_VARIANT = SynchedEntityData.defineId(DepthSquid.class, EntityDataSerializers.INT);

    /** AQUA, BLACK, ORANGE, OURPLE, RED, SILVER, VICIOUS. */
    public static class DepthSquid extends Squid implements Variant {
        public static final String[] VARIANTS = {"aqua", "black", "orange", "ourple", "red", "silver", "vicious"};

        public DepthSquid(EntityType<? extends Squid> type, Level level) {
            super(type, level);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(SQUID_VARIANT, -1);
        }

        @Override
        public int variant() {
            int v = entityData.get(SQUID_VARIANT);
            if (v < 0 && !level().isClientSide()) {
                v = random.nextInt(VARIANTS.length);
                entityData.set(SQUID_VARIANT, v);
            }
            return Math.max(v, 0);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (!level().isClientSide()) variant();
        }

        // vanilla's squid never updates its limb swing, which the depth squid's tentacles follow
        @Override
        public void travel(Vec3 input) {
            super.travel(input);
            calculateEntityAnimation(false);
        }

        @Override
        public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag output) {
            super.addAdditionalSaveData(output);
            output.putInt("variant", variant());
        }

        @Override
        public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag input) {
            super.readAdditionalSaveData(input);
            entityData.set(SQUID_VARIANT, (input.contains("variant") ? input.getInt("variant") : -1));
        }
    }

    private static final EntityDataAccessor<Integer> CRAB_VARIANT = SynchedEntityData.defineId(Scuttlecrab.class, EntityDataSerializers.INT);

    /** TROPICAL, or one in eight CLAYINFUSED. Walks sideways along the seafloor and drops clay. */
    public static class Scuttlecrab extends WaterAnimal implements Variant {
        public static final String[] VARIANTS = {"tropical", "clayinfused"};
        private boolean seafloorWalking;
        private float crabDirection = 1;
        private int switchTimer;

        public Scuttlecrab(EntityType<? extends Scuttlecrab> type, Level level) {
            super(type, level);
        }

        // 1.7's EntityWaterMob was an EntityCreature, which wanders to random spots when it has no AI tasks
        @Override
        protected void registerGoals() {
            goalSelector.addGoal(1, new RandomStrollGoal(this, 1.0D));
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(CRAB_VARIANT, -1);
        }

        @Override
        public int variant() {
            int v = entityData.get(CRAB_VARIANT);
            if (v < 0 && !level().isClientSide()) {
                v = random.nextInt(8) == 0 ? 1 : 0;
                entityData.set(CRAB_VARIANT, v);
            }
            return Math.max(v, 0);
        }

        // walk on the seafloor: movement doesn't see the water
        @Override
        public boolean isInWater() {
            return !seafloorWalking && super.isInWater();
        }

        @Override
        public void aiStep() {
            if (!level().isClientSide()) variant();
            seafloorWalking = true;
            super.aiStep();
            seafloorWalking = false;
        }

        // crab walk!
        @Override
        public void travel(Vec3 input) {
            if (--switchTimer <= 0) {
                crabDirection *= -1;
                switchTimer = 20 + random.nextInt(80);
            }
            super.travel(new Vec3(input.z * crabDirection, input.y, input.x));
        }

        @Override
        protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
            super.dropCustomDeathLoot(level, source, recentlyHit);
            int n = dropCount(level, source, random);
            if (n > 0) spawnAtLocation(new ItemStack(Items.CLAY_BALL, n));
        }

        @Override
        public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag output) {
            super.addAdditionalSaveData(output);
            output.putInt("variant", variant());
        }

        @Override
        public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag input) {
            super.readAdditionalSaveData(input);
            entityData.set(CRAB_VARIANT, (input.contains("variant") ? input.getInt("variant") : -1));
        }
    }
}
