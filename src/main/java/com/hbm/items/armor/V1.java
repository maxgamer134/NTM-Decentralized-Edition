// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.items.armor;

import com.hbm.entity.mob.EntityMaskMan;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.legacy.LegacyFx;
import com.hbm.lib.Library;
import com.hbm.particle.ParticleGiblet;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

/** The V1 mod behind {@code [armor] enable_v1}: its item, its Mask Man drop and its blood healing. */
public final class V1 {

    public static final RegistryHandle<ItemModV1> V1_MOD =
            Reg.item("v1", ItemModV1::new, () -> new Item.Properties().stacksTo(1)).addTo(ModItems.ARMOR_MOD_ALL);

    private V1() {}

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.CONSUMABLES, Library.id("bottled_cloud"),
                () -> new ItemStack(V1_MOD.get()));

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDeathEvent event) -> {
            LivingEntity victim = event.getEntity();
            Player wearer = wearer(event.getSource());
            if (wearer == null || !(victim.level() instanceof ServerLevel level)) return;
            ParticleCreators.giblets(level, victim, ParticleGiblet.TYPE_MEAT, 0);
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                    SoundSource.HOSTILE, 2.0F, 0.95F + level.random.nextFloat() * 0.2F);
            if (wearer.distanceToSqr(victim) < 100) wearer.heal(victim.getMaxHealth() * 0.25F);
        });

        NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> {
            LivingEntity victim = event.getEntity();
            Player wearer = wearer(event.getSource());
            if (wearer == null || !(victim.level() instanceof ServerLevel level)) return;
            LegacyFx.vanillaBurst(level, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                    (int) Math.min(victim.getMaxHealth() / 2F, 250), 0.1D);
            if (wearer.distanceToSqr(victim) < 25) wearer.heal(event.getAmount() * 0.5F);
        });

        NeoForge.EVENT_BUS.addListener((LivingDropsEvent event) -> {
            if (!(event.getEntity() instanceof EntityMaskMan man)) return;
            event.getDrops().add(new ItemEntity(man.level(), man.getX(), man.getY(), man.getZ(),
                    new ItemStack(V1_MOD.get())));
        });
    }

    /** The player behind this damage if they wear V1 on their chestplate. */
    private static @Nullable Player wearer(DamageSource source) {
        if (!(source.getEntity() instanceof Player player)) return null;
        ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!ArmorModHandler.isArmor(plate)) return null;
        var mods = ArmorModHandler.mods(plate);
        for (int socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++)
            if (mods.item(socket) instanceof ItemModV1) return player;
        return null;
    }
}
