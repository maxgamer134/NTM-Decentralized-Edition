// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.space;

import com.hbm.blocks.generic.BlockPlushie;
import com.hbm.space.atmosphere.SpaceAtmosphere;
import com.hbm.registration.RegistryHandle;
import com.hbm.registration.NeoForgeRegistrar;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space's planet weather and its odds and ends that both sides need: Duna's dust storms
 * (WorldProviderDuna.updateWeather), the thunder sounds, and what the planets' waters fish up
 * (WorldProviderCelestial.getFish and WorldProviderIke's plushie).
 */
public final class SpaceWeather {

    /** Eve's thunder as a lightning flash starts (hbm:misc.rumble). */
    public static final RegistryHandle<SoundEvent> RUMBLE = NeoForgeRegistrar.registerSound("misc.rumble");

    private SpaceWeather() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(SpaceWeather::onFished);
    }

    // ---------------------------------------------------------------- Duna's dust storms

    /** Each stretch of this many ticks holds one storm. */
    private static final int STORM_PERIOD = 120_000;

    /**
     * How hard a dust storm blows on Duna at a game time, 0 when clear. NTM:Space rolled the storms
     * on the server (a clear spell of 12,000 to 180,000 ticks, then a storm of 12,000 to 24,000 ticks
     * blowing at 0.25 to 1) and kept the timer in the world; here each 120,000-tick stretch rolls its
     * one storm from its own number, so the server and every client see the same storms with nothing
     * to send or save.
     */
    public static float dunaStorm(long gameTime) {
        long period = Math.floorDiv(gameTime, STORM_PERIOD);
        Random rand = new Random(period * 0x9E3779B97F4A7C15L ^ 0x5DEECE66DL);
        int length = 12_000 + rand.nextInt(12_000);
        int start = rand.nextInt(STORM_PERIOD - length);
        float intensity = rand.nextFloat() * 0.75F + 0.25F;
        long into = gameTime - period * STORM_PERIOD;
        return into >= start && into < start + length ? intensity : 0F;
    }

    public static boolean isDuna(Level level) {
        return SpaceToggle.enabled() && level.dimension() == SpaceDimensions.DUNA;
    }

    // ---------------------------------------------------------------- boiling water

    /**
     * WorldProviderCelestial's isHellWorld: water poured in a vacuum or air under 0.19 atmospheres
     * (orbit, the Mun, Minmus, Ike, Dres, Moho...) boils away as in the Nether, unless a pressurised
     * room holds it. Only the server decides, as in NTM:Space.
     */
    public static boolean boilsAway(Level level, BlockPos pos, FluidStack stack) {
        if (!SpaceToggle.enabled() || !(level instanceof ServerLevel)) return false;
        if (level.dimension() == Level.OVERWORLD || !stack.getFluid().is(FluidTags.WATER)) return false;
        if (!CelestialBody.inOrbit(level) && CelestialBody.getBodyOrNull(level.dimension()) == null) return false;
        return !SpaceAtmosphere.hasLiquidPressure(SpaceAtmosphere.getAtmosphere(level, pos));
    }

    // ---------------------------------------------------------------- fishing

    /** A planet with water of its own, not Kerbin; orbit fishes as Kerbin does. */
    private static @Nullable CelestialBody planet(Level level) {
        if (!SpaceToggle.enabled() || level.dimension() == Level.OVERWORLD) return null;
        return CelestialBody.getBodyOrNull(level.dimension());
    }

    /**
     * NTM:Space's fishing: the planets' waters hold no fish, so a fish comes up as vanilla junk
     * instead (WorldProviderCelestial.getFish), and on Ike everything that bites is a plushie
     * (WorldProviderIke). Junk and treasure stay Kerbin's.
     */
    private static void onFished(ItemFishedEvent event) {
        Level level = event.getHookEntity().level();
        CelestialBody body = planet(level);
        if (body == null) return;
        List<ItemStack> drops = event.getDrops();
        for (int i = 0; i < drops.size(); i++) drops.set(i, swap(level, drops.get(i)));
    }

    private static ItemStack swap(Level level, ItemStack drop) {
        if (level.dimension() == SpaceDimensions.IKE) return BlockPlushie.PlushieType.YOMI.stack();
        return drop.is(ItemTags.FISHES) ? junk(level.getRandom()) : drop;
    }

    /** What a fish caught here becomes: the fishing grenade's catch, by the same rules as a rod's. */
    public static ItemStack planetCatch(Level level, ItemStack fish) {
        return fish.isEmpty() || planet(level) == null ? fish : swap(level, fish);
    }

    /** 1.7.10's junk table, as NTM:Space trimmed it (no boots, leather, bones or rotten flesh). */
    private static ItemStack junk(RandomSource rand) {
        // weights: water bottle 10, string 5, fishing rod 2, bowl 10, stick 5, ink sacs 1, tripwire hook 10
        int roll = rand.nextInt(43);
        if ((roll -= 10) < 0) return PotionContents.createItemStack(Items.POTION, Potions.WATER);
        if ((roll -= 5) < 0) return new ItemStack(Items.STRING);
        if ((roll -= 2) < 0) {
            ItemStack rod = new ItemStack(Items.FISHING_ROD);
            // func_150709_a(0.9F): up to 90% worn
            rod.set(DataComponents.DAMAGE, (int) (rod.getMaxDamage() * rand.nextFloat() * 0.9F));
            return rod;
        }
        if ((roll -= 10) < 0) return new ItemStack(Items.BOWL);
        if ((roll -= 5) < 0) return new ItemStack(Items.STICK);
        if ((roll -= 1) < 0) return new ItemStack(Items.INK_SAC, 10);
        return new ItemStack(Items.TRIPWIRE_HOOK);
    }
}
