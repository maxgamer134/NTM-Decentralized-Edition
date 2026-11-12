// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public interface RuntimeConfig {

    ConfigStore store();

    default int conveyorCramMax() {
        return store().get(ConfigSchema.CONVEYOR_CRAM_MAX);
    }

    default boolean conveyorCramExplode() {
        return store().get(ConfigSchema.CONVEYOR_CRAM_EXPLODE);
    }

    default boolean ultraLarpMode() {
        return store().get(ConfigSchema.ULTRA_LARP_MODE);
    }

    default int farTrackingRange() {
        return store().get(ConfigSchema.FAR_TRACKING_RANGE);
    }

    default boolean damageCompatibilityMode() {
        return store().get(ConfigSchema.DAMAGE_COMPATIBILITY_MODE);
    }
    default boolean ntmEnchanting() {
        return store().get(ConfigSchema.NTM_ENCHANTING);
    }
    default boolean ntmEnchantingEfficiency() {
        return store().get(ConfigSchema.NTM_ENCHANTING_EFFICIENCY);
    }
    default String ntmEnchantingAllowed() {
        return store().get(ConfigSchema.NTM_ENCHANTING_ALLOWED);
    }
    default boolean abilityEnchantStacking() {
        return store().get(ConfigSchema.ABILITY_ENCHANT_STACKING);
    }
    default boolean itemDurability() {
        return store().get(ConfigSchema.ITEM_DURABILITY);
    }
    default boolean gunsVanillaDurability() {
        return store().get(ConfigSchema.GUNS_VANILLA_DURABILITY);
    }
    default boolean safeScBatterySocket() {
        return store().get(ConfigSchema.SAFE_SC_BATTERY_SOCKET);
    }


    default boolean spikesInstakill() {
        return store().get(ConfigSchema.SPIKES_INSTAKILL);
    }

    default int itemHazardDropTickrate() {
        return store().get(ConfigSchema.ITEM_HAZARD_DROP_TICKRATE);
    }

    default boolean unstableRecoverySweep() {
        return store().get(ConfigSchema.UNSTABLE_RECOVERY_SWEEP);
    }

    default boolean enableMOTD() {
        return store().get(ConfigSchema.ENABLE_MOTD);
    }

    default boolean extendedLogging() {
        return store().get(ConfigSchema.ENABLE_EXTENDED_LOGGING);
    }

    default boolean enableBomberShortMode() {
        return store().get(ConfigSchema.ENABLE_BOMBER_SHORT_MODE);
    }

    default double foreignExportReserve() {
        return store().get(ConfigSchema.FOREIGN_EXPORT_RESERVE);
    }

    default boolean waypointDebug() {
        return store().get(ConfigSchema.WAYPOINT_DEBUG);
    }

    // backport: see ConfigSchema.GLYPHIDS_ENABLED
    default boolean glyphidsEnabled() {
        return store().get(ConfigSchema.GLYPHIDS_ENABLED);
    }

    // backport: see ConfigSchema.NUCLEAR_CREEPERS
    default boolean nuclearCreepers() {
        return store().get(ConfigSchema.NUCLEAR_CREEPERS);
    }

    default int toolRecursionDepth() {
        return store().get(ConfigSchema.TOOL_RECURSION_DEPTH);
    }

    default boolean spaceNetherPortals() {
        return store().get(ConfigSchema.SPACE_NETHER_PORTALS);
    }

    default boolean spaceVolcanoes() {
        return store().get(ConfigSchema.SPACE_VOLCANOES);
    }

    default boolean spaceOreLocations() {
        return store().get(ConfigSchema.SPACE_ORE_LOCATIONS);
    }

    default int spaceMaxProbeDistance() {
        return store().get(ConfigSchema.SPACE_MAX_PROBE_DISTANCE);
    }

    default int spaceMaxStationDistance() {
        return store().get(ConfigSchema.SPACE_MAX_STATION_DISTANCE);
    }

    default boolean spaceCombatPodDespawn() {
        return store().get(ConfigSchema.SPACE_COMBAT_POD_DESPAWN);
    }

    default boolean bottlecapTrades() {
        return store().get(ConfigSchema.TRADES_BOTTLECAPS);
    }

    default String bottlecapItem() {
        return store().get(ConfigSchema.TRADES_BOTTLECAP_ITEM);
    }

    default int capsPerEmerald() {
        return store().get(ConfigSchema.TRADES_CAPS_PER_EMERALD);
    }

    default boolean looseChange() {
        return store().get(ConfigSchema.TRADES_LOOSE_CHANGE);
    }

    default String tradeCurrencies() {
        return store().get(ConfigSchema.TRADES_CURRENCIES);
    }
}
