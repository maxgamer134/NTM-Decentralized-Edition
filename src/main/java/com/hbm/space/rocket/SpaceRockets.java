// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.rocket;

import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.SolarSystem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The crewed rocket, the rocket item and the flight-plan drives; only loaded with Space on. */
public final class SpaceRockets {

    private SpaceRockets() {}

    public static final Reg.EntityHandle<EntityRideableRocket> ROCKET_ENTITY = Reg.entity("rocket_rideable",
            () -> EntityType.Builder.<EntityRideableRocket>of(EntityRideableRocket::new, MobCategory.MISC)
                    .sized(2F, 8F).clientTrackingRange(32).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Library.id("rocket_rideable")).location().toString()));

    public static final RegistryHandle<ItemCustomRocket> ROCKET =
            Reg.item("rocket_custom", ItemCustomRocket::new, () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<Item> HARD_DRIVE =
            Reg.item("hard_drive", Item::new, () -> new Item.Properties().stacksTo(1));

    public static final Map<SolarSystem.Body, RegistryHandle<ItemVOTVdrive>> DRIVES = new EnumMap<>(SolarSystem.Body.class);

    static {
        for (SolarSystem.Body body : SolarSystem.Body.values())
            DRIVES.put(body, Reg.item("hard_drive_full_" + driveName(body), p -> new ItemVOTVdrive(p, body),
                    () -> new Item.Properties().stacksTo(1)));
    }

    public static String driveName(SolarSystem.Body body) {
        return body == SolarSystem.Body.ORBIT ? "orbit" : body.name;
    }

    public static final RegistryHandle<SoundEvent> ROCKET_FLY_HEAVY = NeoForgeRegistrar.registerSound("entity.rocket_fly_heavy");
    public static final RegistryHandle<SoundEvent> ROCKET_FLY_LIGHT = NeoForgeRegistrar.registerSound("entity.rocket_fly_light");
    public static final RegistryHandle<SoundEvent> ROCKET_IGNITION = NeoForgeRegistrar.registerSound("entity.rocket_ignition");
    public static final RegistryHandle<SoundEvent> ROCKET_STAGE = NeoForgeRegistrar.registerSound("entity.rocket_stage");

    public static void init() {
        var anchor = Library.id("rp_pod_20");
        for (SolarSystem.Body body : SolarSystem.Body.values()) {
            RegistryHandle<ItemVOTVdrive> drive = DRIVES.get(body);
            CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, anchor, () -> ItemVOTVdrive.processed(drive.get()));
        }
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, anchor, () -> new ItemStack(HARD_DRIVE.get()));
    }
}
