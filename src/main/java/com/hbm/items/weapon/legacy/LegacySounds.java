// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * backport-fix: BF-161 the pre-rework guns name their sounds by main's strings
 * ("hbm:weapon.revolverShoot"). Ids are lowercase now, and an id with no registered event still
 * plays: the packet carries it by value and the client finds it in sounds.json.
 */
public final class LegacySounds {

    private static final Map<String, String> VANILLA =
            Map.of(
                    "tile.piston.out", "minecraft:block.piston.extend",
                    "random.fizz", "minecraft:block.fire.extinguish",
                    "random.explode", "minecraft:entity.generic.explode",
                    "mob.zombie.woodbreak", "minecraft:entity.zombie.break_wooden_door");
    private static final Map<String, Holder<SoundEvent>> CACHE = new ConcurrentHashMap<>();

    private LegacySounds() {}

    public static Holder<SoundEvent> holder(String name) {
        return CACHE.computeIfAbsent(
                name,
                n -> {
                    ResourceLocation id =
                            ResourceLocation.parse(
                                    VANILLA.getOrDefault(n, n).toLowerCase(Locale.ROOT));
                    return BuiltInRegistries.SOUND_EVENT
                            .getHolder(id)
                            .<Holder<SoundEvent>>map(h -> h)
                            .orElseGet(
                                    () -> Holder.direct(SoundEvent.createVariableRangeEvent(id)));
                });
    }

    /** main's world.playSoundAtEntity: everyone near the entity hears it, the entity too. */
    public static void play(Entity at, String name, float volume, float pitch) {
        if (name == null || name.isEmpty() || at.level().isClientSide()) return;
        at.level()
                .playSeededSound(
                        null,
                        at.getX(),
                        at.getY(),
                        at.getZ(),
                        holder(name),
                        SoundSource.PLAYERS,
                        volume,
                        pitch,
                        at.level().getRandom().nextLong());
    }

    public static void play(
            net.minecraft.world.level.Level level,
            double x,
            double y,
            double z,
            String name,
            float volume,
            float pitch) {
        if (level.isClientSide()) return;
        level.playSeededSound(
                null, x, y, z, holder(name), SoundSource.PLAYERS, volume, pitch, level.getRandom().nextLong());
    }
}
