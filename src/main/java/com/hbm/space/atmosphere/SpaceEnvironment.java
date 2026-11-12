// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.atmosphere;

import com.hbm.api.environment.EnvironmentProvider;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageTypes;
import com.hbm.space.AstronomyUtil;
import com.hbm.space.CelestialBody;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.space.trait.CBT_Water;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.TriState;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space's environment: breathing, corrosive air, gravity and what vacuum does to machines,
 * through the hooks NTM gives Space ({@link EnvironmentProvider}).
 */
public final class SpaceEnvironment implements EnvironmentProvider {

    public static final ResourceKey<DamageType> OXYPRIME = ResourceKey.create(Registries.DAMAGE_TYPE, Library.id("oxyprime"));

    /** Suits that seal, as NTM:Space's setSealed(true) sets marked them. */
    private static final EnumSet<ModArmorItem.Suit> SEALED = EnumSet.of(ModArmorItem.Suit.DNT, ModArmorItem.Suit.DESH,
            ModArmorItem.Suit.AJR, ModArmorItem.Suit.AJRO, ModArmorItem.Suit.RPA, ModArmorItem.Suit.NCRPA,
            ModArmorItem.Suit.ENVSUIT, ModArmorItem.Suit.HEV, ModArmorItem.Suit.DIGAMMA, ModArmorItem.Suit.DNS,
            ModArmorItem.Suit.TAURUN, ModArmorItem.Suit.TRENCHMASTER);

    /** An oxygen tank armor mod, NTM:Space's ItemModOxy. */
    public interface OxygenTank {
        /** True if the wearer breathes, from the air or the tank; uses the tank's gas if from it. */
        boolean attemptBreathing(LivingEntity entity, ItemStack tank, @Nullable CBT_Atmosphere atmosphere, boolean force);
    }

    /** Ticks of breath left in a vacuum, NTM:Space's HbmLivingProps oxygen. */
    private static final Map<LivingEntity, int[]> OXYGEN = new WeakHashMap<>();
    private static final Map<LivingEntity, CBT_Atmosphere> CACHE = new WeakHashMap<>();

    @Override
    public boolean breatheAir(ServerLevel level, BlockPos pos, int amount) {
        return SpaceAtmosphere.breatheAir(level, pos, amount);
    }

    @Override
    public TriState canBreathe(LivingEntity entity) {
        return SpaceAtmosphere.canBreathe(atmosphere(entity)) ? TriState.TRUE : TriState.FALSE;
    }

    private static @Nullable CBT_Atmosphere atmosphere(LivingEntity entity) {
        // players every tick, everything else once a second
        if (entity instanceof Player || entity.tickCount % 20 == 0 || !CACHE.containsKey(entity)) {
            CBT_Atmosphere atmosphere = SpaceAtmosphere.getAtmosphere(entity);
            CACHE.put(entity, atmosphere);
            return atmosphere;
        }
        return CACHE.get(entity);
    }

    @Override
    public void tickLiving(LivingEntity entity) {
        if (entity.getVehicle() != null && entity.getVehicle().getType().builtInRegistryHolder().key().location()
                .getPath().contains("rocket")) return; // breathe easy in your ship
        CBT_Atmosphere atmosphere = atmosphere(entity);
        handleOxy(entity, atmosphere);
        if (checkForCorrosion(entity, atmosphere))
            entity.hurt(entity.damageSources().source(ModDamageTypes.ACID), 1);
    }

    private static void handleOxy(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
        if (entity instanceof SuffocationImmune) return;
        int[] oxygen = OXYGEN.computeIfAbsent(entity, e -> new int[] {100});
        if (checkForOxy(entity, atmosphere)) {
            oxygen[0] = 100; // 5 seconds until vacuum damage
            return;
        }
        int o = --oxygen[0];
        if (o > 0) return;
        if (entity.tickCount < 20) o = oxygen[0] = 0; // limit overdamage after relogging
        int interval = Mth.clamp(o / 60 + 8, 1, 8);
        int amount = Math.max(-o / 40 - 10, 1);
        if (entity.tickCount % interval == 0) entity.hurt(entity.damageSources().source(OXYPRIME), amount);
    }

    /** Marks a mob that needs no air. */
    public interface SuffocationImmune {}

    public static boolean checkForOxy(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
        if (!(entity instanceof Player player)) return SpaceAtmosphere.canBreathe(atmosphere);
        if (player.isCreative() || player.isSpectator()) return true;
        if (ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.DNS, false)) return true;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack tank = sealedSuit(player) ? ArmorModHandler.pryMod(helmet, ArmorModHandler.PLATE_ONLY) : ItemStack.EMPTY;
        if (!(tank.getItem() instanceof OxygenTank oxy)) return SpaceAtmosphere.canBreathe(atmosphere);
        // a tank also keeps you from drowning
        boolean underwater = entity.getAirSupply() < entity.getMaxAirSupply();
        boolean breathes = oxy.attemptBreathing(entity, tank, atmosphere, underwater);
        ArmorModHandler.applyMod(helmet, tank, ArmorModHandler.PLATE_ONLY);
        if (underwater && breathes) entity.setAirSupply(entity.getMaxAirSupply());
        return breathes;
    }

    private static boolean sealedSuit(LivingEntity entity) {
        if (!(entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem plate)) return false;
        return SEALED.contains(plate.suit()) && ArmorSuitEffects.hasFullSet(entity, plate.suit(), false);
    }

    /**
     * Corrosive air eats a sealed suit first, then whoever wears it. Without NTM:Space's
     * corrosion inserts, an unsealed wearer is hurt at once.
     */
    private static boolean checkForCorrosion(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
        if (!SpaceAtmosphere.willCorrode(atmosphere)) return false;
        if (!(entity instanceof Player player) || player.isCreative() || player.isSpectator()) return !(entity instanceof Player);
        boolean sealed = true;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ModArmorItem item) || !SEALED.contains(item.suit())) sealed = false;
            if (stack.isEmpty()) continue;
            if (stack.isDamageableItem() && player.getRandom().nextInt(2) == 0) stack.hurtAndBreak(1, player, slot);
        }
        return !sealed;
    }

    @Override
    public void blockChanged(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState oldState,
            net.minecraft.world.level.block.state.BlockState newState) {
        SpaceAtmosphere.blockChanged(level, pos, oldState, newState);
    }

    private static @Nullable CBT_Atmosphere bodyAtmosphere(Level level) {
        if (CelestialBody.inOrbit(level)) return null;
        CelestialBody body = CelestialBody.getBodyOrNull(level.dimension());
        if (body == null) return new CBT_Atmosphere(com.hbm.inventory.fluid.NTMFluids.byName("EARTHAIR"), 1.0);
        return body.getTrait(CBT_Atmosphere.class);
    }

    @Override
    public boolean vacuum(Level level) {
        CBT_Atmosphere atmosphere = bodyAtmosphere(level);
        return atmosphere == null || atmosphere.getPressure() < 0.01;
    }

    @Override
    public boolean evaporates(ServerLevel level, BlockPos pos, boolean vacuumProof) {
        if (vacuumProof) return false;
        CBT_Atmosphere atmosphere = SpaceAtmosphere.getAtmosphere(level, pos);
        return atmosphere == null || atmosphere.getPressure() < 0.01;
    }

    @Override
    public boolean hasAir(ServerLevel level, BlockPos pos) {
        CBT_Atmosphere atmosphere = SpaceAtmosphere.getAtmosphere(level, pos);
        return atmosphere != null && atmosphere.getPressure() > 0.01D;
    }

    /** NTM:Space's intake: draws a pressurized room down, or takes from the planet's air outside one. */
    @Override
    public boolean compressAir(ServerLevel level, BlockPos pos, int deficit) {
        CBT_Atmosphere atmosphere = SpaceAtmosphere.getAtmosphere(level, pos);
        if (atmosphere == null || atmosphere.getPressure() <= 0.01D) return false;
        if (deficit <= 0) return true;
        int consumption = Math.max(1, deficit / 100);
        if (!SpaceAtmosphere.hasAtmosphere(level, pos)) {
            CelestialBody.consumeGas(level, atmosphere.getMainFluid(), consumption);
            return true;
        }
        for (AtmosphereBlob blob : SpaceAtmosphere.getBlobs(level, pos)) {
            if (blob.handler.getFluidType() != com.hbm.inventory.fluid.NTMFluids.byName("AIR") && blob.handler.getFluidPressure() >= 0.1) {
                blob.consume(consumption);
                return true;
            }
        }
        return false;
    }

    @Override
    public void releaseGas(ServerLevel level, Fluid fluid, double mB) {
        if (!CelestialBody.inOrbit(level)) CelestialBody.emitGas(level, fluid, mB);
    }

    @Override
    public @Nullable Fluid groundwater(ServerLevel level) {
        if (CelestialBody.inOrbit(level)) return null;
        CelestialBody body = CelestialBody.getBodyOrNull(level.dimension());
        if (body == null) return com.hbm.inventory.fluid.NTMFluids.WATER;
        CBT_Water table = body.getTrait(CBT_Water.class);
        return table == null ? null : table.fluid;
    }

    @Override
    public int sunHeat(ServerLevel level, BlockPos pos, int heat) {
        CelestialBody body = CelestialBody.getBodyOrNull(level.dimension());
        return body == null ? heat : (int) (heat * body.getSunPower());
    }

    @Override
    public float gravity(LivingEntity entity) {
        return gravityOf(entity);
    }

    public static float gravityOf(LivingEntity entity) {
        if (entity instanceof WaterAnimal) return AstronomyUtil.STANDARD_GRAVITY;
        if (CelestialBody.inOrbit(entity.level())) return orbitalGravity(entity);
        CelestialBody body = CelestialBody.getBodyOrNull(entity.level().dimension());
        if (body == null) return AstronomyUtil.STANDARD_GRAVITY;
        return body.getSurfaceGravity() * AstronomyUtil.PLAYER_GRAVITY_MODIFIER;
    }

    /**
     * A station's artificial gravity, for entities over its deck. NTM:Space gave it to entities in
     * a sealed room; the rooms are only known on the server, so the deck stands in for them here,
     * keeping the client's prediction in step.
     */
    private static float orbitalGravity(LivingEntity entity) {
        float gravity = AstronomyUtil.STANDARD_GRAVITY
                * com.hbm.space.station.OrbitalStation.getStationFromPosition(entity.level(), entity.getBlockX(), entity.getBlockZ()).gravityMultiplier;
        if (gravity < 0.2F) return 0;
        BlockPos.MutableBlockPos pos = entity.blockPosition().mutable();
        for (int i = 0; i < 32 && pos.getY() > entity.level().getMinBuildHeight(); i++) {
            pos.move(0, -1, 0);
            if (!entity.level().getBlockState(pos).isAir()) return gravity;
        }
        return 0;
    }

    @Override
    public boolean orbital(Level level) {
        return CelestialBody.inOrbit(level);
    }

    /** Seed slurry greens dirt only in air plants can use, and turns the planet's CO2 to air. */
    @Override
    public boolean fertilize(ServerLevel level, BlockPos pos) {
        CBT_Atmosphere atmosphere = bodyAtmosphere(level);
        var fluids = com.hbm.inventory.fluid.NTMFluids.class;
        if (atmosphere == null) return false;
        boolean air = atmosphere.hasFluid(com.hbm.inventory.fluid.NTMFluids.byName("EARTHAIR"))
                || atmosphere.hasFluid(com.hbm.inventory.fluid.NTMFluids.byName("DUNAAIR"))
                || atmosphere.hasFluid(com.hbm.inventory.fluid.NTMFluids.byName("CARBONDIOXIDE"))
                || atmosphere.hasFluid(com.hbm.inventory.fluid.NTMFluids.OXYGEN);
        if (!air) return false;
        SpaceAtmosphere.addGrowth(level, null, net.minecraft.world.level.block.Blocks.GRASS_BLOCK, pos, 4, 6);
        int conversion = SpaceAtmosphere.GRASS_GROWTH_CONVERSION * 4 * 6;
        if (CelestialBody.consumeGas(level, com.hbm.inventory.fluid.NTMFluids.byName("CARBONDIOXIDE"), conversion)
                || CelestialBody.consumeGas(level, com.hbm.inventory.fluid.NTMFluids.byName("DUNAAIR"), conversion))
            CelestialBody.emitGas(level, com.hbm.inventory.fluid.NTMFluids.byName("EARTHAIR"), conversion);
        level.levelEvent(2005, pos.above(), 0);
        return true;
    }

    @Override
    public double particlePressure(Level level) {
        CBT_Atmosphere atmosphere = bodyAtmosphere(level);
        return atmosphere == null ? 0 : atmosphere.getPressure();
    }

    /**
     * NTM:Space's gravity, run on both sides for every living thing: weightless in orbit (sneak
     * and jump to move), lighter or heavier on other bodies. High gravity only pulls on the way
     * down, so single blocks can still be jumped.
     */
    public static void tickGravity(LivingEntity entity) {
        if (entity.isNoGravity() || entity instanceof net.minecraft.world.entity.FlyingMob) return;
        float gravity = gravityOf(entity);
        Vec3 m = entity.getDeltaMovement();
        double my = m.y;
        if (gravity == 0) {
            my /= 0.98F;
            my += AstronomyUtil.STANDARD_GRAVITY / 20F;
            if (entity instanceof Player player) {
                if (player.isShiftKeyDown()) my -= 0.01F;
                if (player.jumping) my += 0.01F;
            } else if (entity instanceof Chicken) {
                my = 0;
            }
            my *= 0.91F;
        } else if (!entity.isInWater() && entity.tickCount > 20 && (gravity < 1.5F || gravity > 1.7F)) {
            if (gravity < 0.2F) gravity = 0.2F;
            if ((gravity < 1.5F || my < 0) && !(entity instanceof Chicken)) {
                my /= 0.98F;
                my += AstronomyUtil.STANDARD_GRAVITY / 20F;
                my -= gravity / 20F;
                my *= 0.98F;
            }
        } else {
            return;
        }
        entity.setDeltaMovement(m.x, my, m.z);
    }

    /** Low gravity softens falls, and below 0.3 you can't hurt yourself falling at all. */
    public static float fallDistance(LivingEntity entity, float distance) {
        float gravity = gravityOf(entity);
        if (gravity < 0.3F) return 0;
        if (gravity < 1.5F) return distance * gravity / AstronomyUtil.STANDARD_GRAVITY;
        return distance;
    }
}
