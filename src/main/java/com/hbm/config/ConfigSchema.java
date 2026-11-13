// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import static com.hbm.config.ConfigEntry.*;

public final class ConfigSchema {

    public static final ConfigEntry<Integer> CONVEYOR_CRAM_MAX =
            integer(
                    Domain.RUNTIME,
                    "conveyor",
                    "cram_max",
                    25,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "Nearby conveyor objects required to trigger an anti-cram burst.");
    public static final ConfigEntry<Boolean> CONVEYOR_CRAM_EXPLODE =
            bool(
                    Domain.RUNTIME,
                    "conveyor",
                    "cram_breaks_belt",
                    true,
                    "Whether anti-cram bursts break the conveyor belt under objects older than 400 ticks.");
    public static final ConfigEntry<Boolean> ULTRA_LARP_MODE =
            bool(
                    Domain.RUNTIME,
                    "doors",
                    "ultra_larp_mode",
                    false,
                    "Require redstone or remote control to operate large doors by hand, except the water door.");

    public static final ConfigEntry<Boolean> ENABLE_BOMBER_SHORT_MODE =
            bool(
                    Domain.RUNTIME,
                    "entities",
                    "bomber_short_approach",
                    false,
                    "Has bomber planes spawn closer to their target for smaller render distances.");
    public static final ConfigEntry<Integer> FAR_TRACKING_RANGE =
            integer(
                    Domain.RUNTIME,
                    "entities",
                    "far_tracking_range",
                    4096,
                    0,
                    65536,
                    "Blocks within which missiles, mushroom clouds and meteors stay tracked to players whose Distant "
                            + "Horizons or Voxy terrain reaches that far, past the view distance. 0 = view distance only.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "enabled",
                    false,
                    "Enables Less Bullshit Mode's alternate recipes.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_FULL_SCHRAB =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "full_schrab",
                    true,
                    "When enabled, this will replace schraranium with full schrabidium ingots in the transmutator's output");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SHORTER_DECAY =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "shorter_decay",
                    true,
                    "When enabled, this will highly accelerate the speed at which nuclear waste disposal drums decay their "
                            + "contents. 60x faster than 528 mode and 5-12x faster than on normal mode.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_ARMOR_RECIPES =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_armor_recipes",
                    true,
                    "When enabled, simplifies the recipe for armor sets like starmetal or schrabidium.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_TOOL_RECIPES =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_tool_recipes",
                    true,
                    "Uses Less Bullshit Mode's simple tool recipe forms.");

    // backport: gun content switches (main only has 1.20_enableGuns, which stops guns firing)
    public static final ConfigEntry<Boolean> DISABLE_ALL_GUNS =
            bool(
                    Domain.CONTENT,
                    "guns",
                    "disable_all_guns",
                    false,
                    "Master switch: hides every gun, gun part, casing, ammo and the ammo press from creative and "
                            + "recipe viewers, drops their recipes and stops guns firing.");

    public static final ConfigEntry<Boolean> DISABLE_POST_REWORK_GUNS =
            bool(
                    Domain.CONTENT,
                    "guns",
                    "disable_post_rework_guns",
                    false,
                    "Does the same as disable_all_guns for main's current guns, their parts, ammo and the ammo press.");

    public static final ConfigEntry<Boolean> DISABLE_PRE_REWORK_GUNS =
            bool(
                    Domain.CONTENT,
                    "guns",
                    "disable_pre_rework_guns",
                    true,
                    "Does the same as disable_all_guns for the guns main had before its gun rework.");

    public static final ConfigEntry<Boolean> PRE_REWORK_GUNS_USE_NEW_AMMO =
            bool(
                    Domain.CONTENT,
                    "guns",
                    "pre_rework_guns_use_new_ammo",
                    true,
                    "Converts the pre-rework guns to post-rework ammo where a round has a post-rework twin"
                            + " (standard, AP and DU rifle and pistol rounds, buckshot, .75 bolts, 40mm, rockets,"
                            + " flamer fuel, mini nukes). The old twin leaves the creative tabs, recipe viewers"
                            + " and recipes; stacks of it still load. Rounds with no twin stay.");

    public static final ConfigEntry<Boolean> DISABLE_HEAVY_TURRETS =
            bool(
                    Domain.CONTENT,
                    "guns",
                    "disable_heavy_turrets",
                    false,
                    "Switches off the five turrets that need no ammo press: Jeremy (240mm), Maxwell (MASER), Fritz"
                            + " (liquids), Greg (16 inch shells) and Henry (HIMARS). The gun switches leave these on.");
    public static final ConfigEntry<Boolean> DISABLE_TURRET_HENRY =
            bool(Domain.CONTENT, "guns", "disable_turret_henry", false, "Switches off only the Henry HIMARS turret.");
    public static final ConfigEntry<Boolean> DISABLE_TURRET_GREG =
            bool(Domain.CONTENT, "guns", "disable_turret_greg", false, "Switches off only the Greg artillery turret.");

    public static final ConfigEntry<Boolean> DISABLE_WAR_EXPLOSIVES =
            bool(
                    Domain.CONTENT,
                    "war_explosives",
                    "disable_war_explosives",
                    false,
                    "Switches off every explosive made for war (all four switches below at once): missiles and launch pads, bombs and nukes,"
                            + " grenades, landmines and C4/semtex charges. They leave the creative tabs and"
                            + " recipe viewers, their recipes are dropped, and they can't be launched, thrown"
                            + " or set off. Mining explosives (dynamite, mining charges) stay. The items stay"
                            + " registered, so old worlds still load.");
    public static final ConfigEntry<Boolean> DISABLE_WAR_MISSILES =
            bool(Domain.CONTENT, "war_explosives", "disable_war_missiles", false,
                    "Switches off only the missiles: missiles, custom missiles and their parts, launch pads, the"
                            + " launch table, the compact launcher, the missile assembly and launch codes and keys.");
    public static final ConfigEntry<Boolean> DISABLE_WAR_NUKES =
            bool(Domain.CONTENT, "war_explosives", "disable_war_nukes", false,
                    "Switches off only the stationary nuclear bombs: the assembled nukes (Gadget, Little Boy, Fat"
                            + " Man, Ivy Mike, Tsar Bomba, F.L.E.I.J.A., Solinium, N2, prototype, custom nuke), the"
                            + " balefire bomb, the crashed bomb, the nuclear det charge and the antimatter bomb, with"
                            + " their kits and parts.");
    public static final ConfigEntry<Boolean> DISABLE_WAR_BOMBS =
            bool(Domain.CONTENT, "war_explosives", "disable_war_bombs", false,
                    "Switches off only the stationary conventional bombs: C4, semtex, det charges, plastic"
                            + " explosive, landmines and the EMP, thermobaric, floating, flame and multi-purpose"
                            + " bombs. Mining explosives stay.");
    public static final ConfigEntry<Boolean> DISABLE_WAR_MISC =
            bool(Domain.CONTENT, "war_explosives", "disable_war_misc", false,
                    "Switches off the other war explosives: grenades and their parts and the bomb caller.");
    public static final ConfigEntry<Boolean> ENABLE_PEDESTAL_RECIPE =
            bool(Domain.CONTENT, "secret_recipes", "enable_pedestal_recipe", false,
                    "Lets the Pedestal be crafted (blackstone brick slabs, obsidian, blackstone bricks)."
                            + " Off, it is only found, as in NTM.");
    public static final ConfigEntry<Boolean> ENABLE_SECRET_PART_RECIPES =
            bool(Domain.CONTENT, "secret_recipes", "enable_secret_part_recipes", false,
                    "Lets the assembly machine make the secret gun parts (selenium steel, the SB-26"
                            + " canister, the control unit, the folly and aberrator parts); the switches below turn on one each. Recycling broken"
                            + " ones works either way.");
    public static final ConfigEntry<Boolean> ENABLE_SECRET_AUGMENT_RECIPES =
            bool(Domain.CONTENT, "secret_recipes", "enable_secret_augment_recipes", false,
                    "Lets the assembly machine make the red room's and other secret augments that have no"
                            + " recipe: armor polish, band-aid, serum, plutonium quartz, morning glory, spider milk,"
                            + " scrumpy, Wild Pegasus Dry Whiskey, the Ace of Spades and Queen of Hearts, and"
                            + " the nickel and doubloon gun mods. V1 has its own switch.");
    public static final ConfigEntry<Boolean> ENABLE_V1_RECIPE =
            bool(Domain.CONTENT, "secret_recipes", "enable_v1_recipe", true,
                    "Lets the assembly machine make the V1 chestplate mod (secret augment blueprint)."
                            + " Does nothing while enable_v1 is off.");
    public static final ConfigEntry<Boolean> ENABLE_RED_ROOM_KEYHOLE_RECIPE =
            bool(Domain.CONTENT, "secret_recipes", "enable_red_room_keyhole_recipe", false,
                    "Lets the red room keyhole (Strange Red Room Brick) be crafted from bricks, starmetal,"
                            + " alexandrite and red dye.");
    public static final ConfigEntry<Boolean> ENABLE_SECRET_SELENIUM_STEEL_RECIPE =
            bool(Domain.CONTENT, "secret_recipes", "enable_secret_selenium_steel_recipe", false,
                    "Lets the assembly machine make the selenium steel (on with enable_secret_part_recipes too).");
    public static final ConfigEntry<Boolean> ENABLE_SECRET_CANISTER_RECIPE =
            bool(Domain.CONTENT, "secret_recipes", "enable_secret_canister_recipe", false,
                    "Lets the assembly machine make the SB-26 canister (on with enable_secret_part_recipes too).");
    public static final ConfigEntry<Boolean> ENABLE_SECRET_CONTROLLER_RECIPE =
            bool(Domain.CONTENT, "secret_recipes", "enable_secret_controller_recipe", false,
                    "Lets the assembly machine make the control unit (on with enable_secret_part_recipes too).");
    public static final ConfigEntry<Boolean> ENABLE_SECRET_FOLLY_RECIPE =
            bool(Domain.CONTENT, "secret_recipes", "enable_secret_folly_recipe", false,
                    "Lets the assembly machine make the folly part (on with enable_secret_part_recipes too).");
    public static final ConfigEntry<Boolean> ENABLE_SECRET_ABERRATOR_RECIPE =
            bool(Domain.CONTENT, "secret_recipes", "enable_secret_aberrator_recipe", false,
                    "Lets the assembly machine make the aberrator part (on with enable_secret_part_recipes too).");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_CRAFTING =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_crafting",
                    true,
                    "Uses Less Bullshit Mode's simple crafting recipe forms.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_CHEMISTRY =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_chemistry",
                    true,
                    "Uses Less Bullshit Mode's simple chemical plant recipe forms.");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_CENTRIFUGE =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_centrifuge",
                    true,
                    "When enabled, enhances centrifuge outputs to make rare materials more common");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_UNLOCK_ANVIL =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "unlock_anvil",
                    true,
                    "When enabled, all anvil recipes are available at tier 1");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SIMPLE_MEDICINE =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "simple_medicine",
                    true,
                    "When enabled, makes some medicine recipes (like ones that require bismuth) much more affordable");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SAFE_CRATES =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "safe_crates",
                    true,
                    "When enabled, prevents crates from becoming radioactive");

    public static final ConfigEntry<Boolean> ENABLE_LBSM_SAFE_ME_DRIVES =
            bool(
                    Domain.CONTENT,
                    "lbsm",
                    "safe_me_drives",
                    true,
                    "When enabled, prevents ME Drives and Portable Cells from becoming radioactive");

    public static final ConfigEntry<Boolean> ENABLE_FOREIGN_MOD_CHANGES =
            bool(
                    Domain.CONTENT,
                    "foreign_mods",
                    "enabled",
                    true,
                    "Master switch for this mod's changes to other mods' recipes; each change also has its own entry. "
                            + "Takes effect on restart.");

    public static final ConfigEntry<Boolean> ENABLE_MEKANISM_CHANGES =
            bool(
                    Domain.CONTENT,
                    "foreign_mods",
                    "mekanism",
                    true,
                    "If enabled, will change some of Mekanism's recipes.");

    public static final ConfigEntry<Boolean> ENABLE_TECHREBORN_CHANGES =
            bool(
                    Domain.CONTENT,
                    "foreign_mods",
                    "techreborn",
                    true,
                    "If enabled, TechReborn's grinder turns a vanilla cinnabar block into one small cinnabar dust, the "
                            + "mercury this mod gets per block.");

    public static final ConfigEntry<Boolean> ENABLE_EXPENSIVE_MODE =
            bool(Domain.CONTENT, "expensive", "enabled", false, "It does what the name implies.");

    public static final ConfigEntry<Boolean> ENABLE_MOTD =
            bool(
                    Domain.RUNTIME,
                    "login",
                    "motd",
                    true,
                    "If enabled, shows the 'Loaded mod!' chat message when joining a world");

    public static final ConfigEntry<Boolean> ENABLE_DEBUG_MODE =
            bool(Domain.RUNTIME, "debug", "profiling_logs", false, "Enable debugging mode");
    public static final ConfigEntry<Boolean> ENABLE_EXTENDED_LOGGING =
            bool(
                    Domain.RUNTIME,
                    "debug",
                    "extended_logging",
                    false,
                    "Logs uses of the detonator, nuclear explosions, missile launches, grenades, etc.");
    public static final ConfigEntry<Boolean> ENABLE_SKYBOXES =
            bool(
                    Domain.CLIENT,
                    "render",
                    "skyboxes",
                    true,
                    "If enabled, will try to use NTM's custom skyboxes.");
    public static final ConfigEntry<Boolean> ENABLE_528 =
            bool(
                    Domain.CONTENT,
                    "mode_528",
                    "enabled",
                    false,
                    "The central toggle for 528 mode, required TRUE for most subsequent toggles to work.");
    public static final ConfigEntry<Boolean> ENABLE_528_PRESSURIZED_RECIPES =
            bool(
                    Domain.CONTENT,
                    "mode_528",
                    "pressurized_recipes",
                    true,
                    "Sets some recipes to require pressurized input fluid");
    public static final ConfigEntry<Boolean> ENABLE_528_MACHINE_GRAVITY =
            bool(
                    Domain.CONTENT,
                    "mode_528",
                    "machine_gravity",
                    true,
                    "Requires most large machines to have a proper foundation, or else they tilt and break.");
    public static final ConfigEntry<Boolean> ENABLE_528_BOSNIA_SIMULATOR =
            bool(
                    Domain.CONTENT,
                    "mode_528",
                    "bosnia_simulator",
                    true,
                    "Enables anti tank mines spawning all over the world.");
    public static final ConfigEntry<Boolean> ENABLE_528_FORCE_REASIM_BOILERS =
            bool(
                    Domain.CONTENT,
                    "mode_528",
                    "force_reasim_boilers",
                    true,
                    "Keeps the RBMK dial for ReaSim boilers on, preventing use of non-ReaSim boiler columns and "
                            + "forcing the use of steam in-/outlets");

    public static final ConfigEntry<Integer> RAD_TICK_RATE =
            integer(
                    Domain.RUNTIME,
                    "radiation",
                    "tick_rate",
                    1,
                    1,
                    Integer.MAX_VALUE,
                    "Ticks between radiation simulation steps. Consumed once at startup.");
    public static final ConfigEntry<Integer> HAZARD_RATE =
            integer(
                    Domain.RUNTIME,
                    "radiation",
                    "hazard_rate",
                    // backport-fix: BF-144 main applies inventory and equipment hazards every
                    // tick; 1 is main's cadence, higher values trade smoothness for server time
                    1,
                    1,
                    Integer.MAX_VALUE,
                    "Ticks between item/inventory hazard applications (1 = every tick, like 1.7.10).");
    // backport-fix: BF-126 (GitHub #34) 1.7.10's HAZ_01_disableCoaldust back as a config line; the
    // hazards datapack value disable_coal keeps working, either one turns coal dust off
    public static final ConfigEntry<Boolean> DISABLE_COAL_DUST =
            bool(
                    Domain.RUNTIME,
                    "radiation",
                    "disable_coal_dust",
                    false,
                    "When enabled, all coal dust hazards are disabled (the hazards datapack value "
                            + "disable_coal does the same for one world)");

    public static final ConfigEntry<Integer> LIMIT_EXPLOSION_LIFESPAN =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "unloaded_lifespan_seconds",
                    0,
                    0,
                    Integer.MAX_VALUE,
                    "Seconds after which a reloaded MK3 explosion gives up. 0 = no limit");
    public static final ConfigEntry<Integer> BLAST_SPEED =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "mk3_columns_per_tick",
                    1024,
                    1,
                    Integer.MAX_VALUE,
                    "Columns per tick the MK3 spiral steps");
    public static final ConfigEntry<Integer> MK5_BLAST_TIME =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "mk5_millis_per_tick",
                    50,
                    1,
                    Integer.MAX_VALUE,
                    "Minimum amount of milliseconds per tick allocated for mk5 chunk processing");
    public static final ConfigEntry<Integer> FALLOUT_DELAY =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "fallout_delay_ticks",
                    4,
                    0,
                    Integer.MAX_VALUE,
                    "How many ticks to wait for the next fallout chunk computation");
    public static final ConfigEntry<Integer> BLAST_CHUNKS_IN_FLIGHT =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "chunks_in_flight",
                    0,
                    0,
                    65536,
                    "Chunks a blast/fallout may load concurrently. 0 = auto (cpu cores * 8, minimum 64)");
    public static final ConfigEntry<Integer> EXPLOSION_ALGORITHM =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "mk5_algorithm",
                    2,
                    0,
                    2,
                    "Configures the algorithm of mk5 explosion. 0 = Legacy, 1 = Threaded DDA, "
                            + "2 = Threaded DDA with damage accumulation.");
    public static final ConfigEntry<Integer> BOMB_MAX_THREADS =
            integer(
                    Domain.RUNTIME,
                    "explosions",
                    "max_threads",
                    -1,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "Bomb ForkJoinPool workers; <=0 means processors + value (default -1 = processors - 1).");

    public static final ConfigEntry<Double> FOREIGN_EXPORT_RESERVE =
            real(
                    Domain.RUNTIME,
                    "energy",
                    "foreign_export_reserve",
                    0D,
                    0D,
                    1D,
                    "Fraction of a power net's supply that foreign (FE) machines may take at normal priority, ahead of "
                            + "the leftover surplus they receive anyway. 0 keeps NTM machines strictly first.");

    public static final ConfigEntry<Boolean> WAYPOINT_DEBUG =
            bool(
                    Domain.RUNTIME,
                    "debug",
                    "waypoints",
                    false,
                    "Allows glyphid waypoints to be seen, mainly used for debugging, also useful as an aid against them");

    // backport: switch for glyphids appearing on their own (not in ntm-next; the datapack's
    // mob_config glyphids "enabled" only covers hive world generation)
    public static final ConfigEntry<Boolean> GLYPHIDS_ENABLED =
            bool(
                    Domain.RUNTIME,
                    "glyphids",
                    "enabled",
                    true,
                    "Whether glyphids and their hives appear on their own. When false: no hives generate in new"
                            + " chunks, hive blocks already in the world stop spawning glyphids, scouts no longer spawn"
                            + " from soot or build new hives. Glyphids already in the world stay; spawn eggs and"
                            + " commands still work.");

    // backport: on/off for the world objects NTM scatters over new chunks and for the nuclear
    // creeper (Discord request after a crashed balefire bomb went off in someone's base). Not in
    // ntm-next (datapack only); 1.7.10 had spawn rates (WorldConfig 4.xx_*Spawn) where 0 meant
    // "default", so nothing could be turned off. The placed features ask through
    // world/placement/ConfigSwitchPlacement ("hbm:config_switch"), when a chunk generates.
    public static final ConfigEntry<Boolean> WORLD_DUDS =
            worldObject("duds", "Crashed bombs (duds): balefire, nuclear, salted and conventional");
    public static final ConfigEntry<Boolean> WORLD_METEORITES =
            worldObject("meteorites", "Fallen meteorites (not the meteor strikes from the sky)");
    public static final ConfigEntry<Boolean> WORLD_WASTE_BARRELS =
            worldObject("waste_barrels", "Buried nuclear waste barrels");
    public static final ConfigEntry<Boolean> WORLD_LANDING_CAPSULES =
            worldObject("landing_capsules", "Landing capsules");
    public static final ConfigEntry<Boolean> WORLD_LOCKED_SAFES =
            worldObject("locked_safes", "Locked safes");
    public static final ConfigEntry<Boolean> WORLD_LANTERN_BEHEMOTHS =
            worldObject("lantern_behemoths", "Lantern behemoths");
    public static final ConfigEntry<Boolean> WORLD_ARCTIC_VAULTS =
            worldObject("arctic_vaults", "Arctic code vaults");
    public static final ConfigEntry<Boolean> WORLD_LIBRARY_DUNGEONS =
            worldObject("library_dungeons", "Library dungeons");
    public static final ConfigEntry<Boolean> WORLD_GEYSERS =
            worldObject("geysers", "Geysers in the Overworld");
    public static final ConfigEntry<Boolean> WORLD_NETHER_GEYSERS =
            worldObject("nether_geysers", "Geysers in the Nether");

    /** The [world_objects] lines, looked up by key from the placed features. */
    public static final List<ConfigEntry<Boolean>> WORLD_OBJECTS =
            List.of(
                    WORLD_DUDS,
                    WORLD_METEORITES,
                    WORLD_WASTE_BARRELS,
                    WORLD_LANDING_CAPSULES,
                    WORLD_LOCKED_SAFES,
                    WORLD_LANTERN_BEHEMOTHS,
                    WORLD_ARCTIC_VAULTS,
                    WORLD_LIBRARY_DUNGEONS,
                    WORLD_GEYSERS,
                    WORLD_NETHER_GEYSERS);

    public static final ConfigEntry<Boolean> NUCLEAR_CREEPERS =
            bool(
                    Domain.RUNTIME,
                    "mobs",
                    "nuclear_creepers",
                    true,
                    "Whether a creeper that has taken 200 RAD or more can turn into a nuclear creeper"
                            + " (1 in 3; otherwise the radiation deals it 100 damage), as in 1.7.10. When"
                            + " false, irradiated creepers stay ordinary creepers. Nuclear creepers already"
                            + " in the world stay; spawn eggs and commands still work.");

    private static ConfigEntry<Boolean> worldObject(String key, String what) {
        return bool(
                Domain.RUNTIME,
                "world_objects",
                key,
                true,
                what + " generate in new chunks. Already generated ones stay; takes effect without"
                        + " a restart.");
    }

    public static final ConfigEntry<Integer> TOOL_RECURSION_DEPTH =
            integer(
                    Domain.RUNTIME,
                    "tools",
                    "veinminer_recursion_depth",
                    1000,
                    1,
                    Integer.MAX_VALUE,
                    "Limits veinminer's recursive function. Usually not an issue, unless you're using bukkit which is especially sensitive for some reason.");

    public static final ConfigEntry<Boolean> DAMAGE_COMPATIBILITY_MODE =
            bool(Domain.RUNTIME, "damage", "compatibility_mode", false, "");
    public static final ConfigEntry<Boolean> SAFE_SC_BATTERY_SOCKET =
            bool(
                    Domain.RUNTIME,
                    "damage",
                    "safe_sc_battery_socket",
                    false,
                    "Self-charging batteries in a battery socket fire discharge beams, as in main. On: they sit there safely.");
    public static final ConfigEntry<Boolean> TRADES_BOTTLECAPS =
            bool(
                    Domain.RUNTIME,
                    "trades",
                    "bottlecap_trades",
                    false,
                    "Every villager and wandering trader trade, vanilla or modded, uses bottle caps where it used emeralds, both prices and payouts. Applies at once: villagers that already have emerald trades switch the next time a player opens trading with them.");
    public static final ConfigEntry<String> TRADES_BOTTLECAP_ITEM =
            text(
                    Domain.RUNTIME,
                    "trades",
                    "bottlecap_item",
                    "hbm:cap_nuka",
                    "The cap that replaces emeralds (hbm:cap_nuka, cap_quantum, cap_sparkle, cap_rad, cap_korl or cap_fritz, or any item id).");
    public static final ConfigEntry<Integer> TRADES_CAPS_PER_EMERALD =
            integer(
                    Domain.RUNTIME,
                    "trades",
                    "caps_per_emerald",
                    1,
                    1,
                    64,
                    "How many caps one emerald becomes. A count past the cap's stack size is cut to it.");
    public static final ConfigEntry<Boolean> TRADES_LOOSE_CHANGE =
            bool(
                    Domain.RUNTIME,
                    "trades",
                    "loose_change",
                    false,
                    "Trades take any currency listed in currencies and give change: with a quantum cap worth four nuka caps, a two-cap trade takes one quantum cap and hands back two nuka caps. Payments whose change no listed coins make exactly are refused.");
    public static final ConfigEntry<String> TRADES_CURRENCIES =
            text(
                    Domain.RUNTIME,
                    "trades",
                    "currencies",
                    "hbm:cap_nuka=1,hbm:cap_fritz=1,hbm:cap_korl=1,hbm:cap_rad=1,hbm:cap_sparkle=2,hbm:cap_quantum=4",
                    "The currencies loose_change accepts, as item=value pairs separated by commas. Any item can be one, minecraft:emerald=8 for example.");
    public static final ConfigEntry<Boolean> ENABLE_DOOR_ACCESS =
            bool(
                    Domain.CONTENT,
                    "doors",
                    "enable_door_access",
                    false,
                    "Door keypads, keycard scanners and keycards. Off, none of them are registered. Needs a game restart, and servers and clients must match.");
    public static final ConfigEntry<Boolean> ENABLE_V1 =
            bool(
                    Domain.CONTENT,
                    "armor",
                    "enable_v1",
                    true,
                    "The V1 chestplate mod, which NTMain removed and NTM:Space kept: faster running, three dashes, and every hit and kill heals the wearer. The Mask Man drops it. Off, it is not registered. Needs a game restart, and servers and clients must match.");
    public static final ConfigEntry<Boolean> ENABLE_SPACE =
            bool(
                    Domain.CONTENT,
                    "space",
                    "enable_space",
                    false,
                    "NTM:Space: planets, atmospheres, rockets and orbital stations. Off, none of its blocks, items, entities or dimensions are registered. Needs a game restart, and servers and clients must match.");
    public static final ConfigEntry<Boolean> SPACE_CHANGES =
            bool(
                    Domain.CONTENT,
                    "space",
                    "space_changes",
                    true,
                    "With NTM:Space on, also apply its changes to existing NTM content (hydrogen and Watz mud that heat into new gases, rocket-grade fuels, and the rest). Off, existing content stays as in NTMain. Less Bullshit Mode keeps NTMain's recipes either way, so any recipe Space changes or removes stays as it was.");
    public static final ConfigEntry<Boolean> SPACE_OVERWORLD_METEOR_DUNGEONS =
            bool(
                    Domain.CONTENT,
                    "space",
                    "overworld_meteor_dungeons",
                    false,
                    "With NTM:Space changes on, meteor dungeons also generate in the overworld, as in NTMain, as well as on the other bodies. Off, as in NTM:Space, they are found on the other bodies only. Affects newly generated chunks.");
    public static final ConfigEntry<Boolean> SPACE_OVERWORLD_AUSTRALIUM =
            bool(
                    Domain.CONTENT,
                    "space",
                    "overworld_australium",
                    false,
                    "With NTM:Space changes on, the overworld's Australium deposit also generates, as in NTMain, as well as Moho's Australium bedrock ores. Off, as in NTM:Space, Australium comes from Moho only. Affects newly generated chunks.");
    public static final ConfigEntry<Boolean> SPACE_NETHER_PORTALS =
            bool(
                    Domain.RUNTIME,
                    "space",
                    "allow_nether_portals",
                    false,
                    "Nether portals work on other celestial bodies.");
    public static final ConfigEntry<Boolean> SPACE_VOLCANOES =
            bool(
                    Domain.RUNTIME,
                    "space",
                    "enable_volcano_gen",
                    true,
                    "Natural volcanoes on other bodies spew lava and grow. Off, they stay as generated.");
    public static final ConfigEntry<Boolean> SPACE_ORE_LOCATIONS =
            bool(
                    Domain.RUNTIME,
                    "space",
                    "show_ore_locations",
                    true,
                    "Ore tooltips name the bodies an ore can be found on.");
    public static final ConfigEntry<Integer> SPACE_MAX_PROBE_DISTANCE =
            integer(
                    Domain.RUNTIME,
                    "space",
                    "max_probe_distance",
                    32000,
                    0,
                    Integer.MAX_VALUE,
                    "How far from a body dimension's centre a probe can pick its landing coordinates.");
    public static final ConfigEntry<Integer> SPACE_MAX_STATION_DISTANCE =
            integer(
                    Domain.RUNTIME,
                    "space",
                    "max_station_distance",
                    32000,
                    1024,
                    Integer.MAX_VALUE,
                    "How far from the orbit dimension's centre orbital stations are placed.");
    public static final ConfigEntry<Boolean> SPACE_COMBAT_POD_DESPAWN =
            bool(
                    Domain.RUNTIME,
                    "space",
                    "combat_pod_despawn",
                    false,
                    "Combat drop pods despawn after a while.");
    public static final ConfigEntry<Boolean> SPACE_ENABLE_MUN_COWS =
            bool(Domain.RUNTIME, "space", "enable_mun_cows", true, "Cows spawn on Mun.");
    public static final ConfigEntry<Boolean> SERVER_SAFETY =
            bool(
                    Domain.RUNTIME,
                    "entities",
                    "server_safety",
                    false,
                    "Lets automated entity culling kill the elder one, which is otherwise unkillable.");
    public static final ConfigEntry<Double> SPACE_CELESTIAL_RAD =
            real(
                    Domain.RUNTIME,
                    "space",
                    "celestial_rad_multiplier",
                    0.25D,
                    0D,
                    100D,
                    "Radiation from the sun under open sky on other bodies and stations, as a multiple of the sun's strength there less 4 per atmosphere of pressure. 1 = 1 rad/s on the Mun.");

    public static final ConfigEntry<Boolean> GUNS_VANILLA_DURABILITY =
            bool(
                    Domain.RUNTIME,
                    "damage",
                    "guns_vanilla_durability",
                    false,
                    "On: gun wear is kept as vanilla item damage (pre-rework and post-rework guns), with the vanilla durability bar hidden. Off: guns keep their own wear value, as in main.");
    public static final ConfigEntry<Boolean> ITEM_DURABILITY =
            bool(
                    Domain.RUNTIME,
                    "damage",
                    "item_durability",
                    true,
                    "On: NTM tools, weapons, armor and guns wear down as in main. Off: they never lose durability or gun wear.");
    public static final ConfigEntry<Boolean> NTM_ENCHANTING =
            bool(
                    Domain.RUNTIME,
                    "damage",
                    "ntm_enchanting",
                    true,
                    "On: NTM items can be enchanted as usual. Off: the enchanting table and the anvil only give them the enchantments ntm_enchanting_efficiency and ntm_enchanting_allowed let through; enchantments they already have stay.");
    public static final ConfigEntry<Boolean> ABILITY_ENCHANT_STACKING =
            bool(
                    Domain.RUNTIME,
                    "damage",
                    "ability_enchant_stacking",
                    false,
                    "Off: a tool's fortune ability and a Fortune enchantment don't stack, the higher one counts. On: equal levels add one, as in main (Fortune III with a level 3 fortune ability digs as Fortune IV).");
    public static final ConfigEntry<Boolean> NTM_ENCHANTING_EFFICIENCY =
            bool(
                    Domain.RUNTIME,
                    "damage",
                    "ntm_enchanting_efficiency",
                    true,
                    "With ntm_enchanting off, NTM tools can still take Efficiency.");
    public static final ConfigEntry<String> NTM_ENCHANTING_ALLOWED =
            text(
                    Domain.RUNTIME,
                    "damage",
                    "ntm_enchanting_allowed",
                    "",
                    "With ntm_enchanting off, these enchantments are still allowed on NTM items, separated by commas: minecraft:unbreaking,minecraft:mending for example.");
    public static final ConfigEntry<Boolean> SPIKES_INSTAKILL =
            bool(
                    Domain.RUNTIME,
                    "damage",
                    "spikes_instakill",
                    true,
                    "Spikes deal 100 damage, as in main. Off: they deal 20 (10 hearts) instead.");
    public static final ConfigEntry<Integer> ITEM_HAZARD_DROP_TICKRATE =
            integer(
                    Domain.RUNTIME,
                    "radiation",
                    "item_hazard_drop_tickrate",
                    2,
                    1,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Boolean> UNSTABLE_RECOVERY_SWEEP =
            bool(
                    Domain.RUNTIME,
                    "radiation",
                    "unstable_recovery_sweep",
                    false,
                    "Recover unreported changes in loaded entity and player inventories every 20 ticks. "
                            + "Block storage requires slot notifications and is never audited.");

    public static final ConfigEntry<Integer> TOOL_HUD_INDICATOR_X =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "tool_indicator_x",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Integer> TOOL_HUD_INDICATOR_Y =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "tool_indicator_y",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");

    public static final ConfigEntry<Boolean> CRATE_OPEN_HELD =
            bool(
                    Domain.CLIENT,
                    "interaction",
                    "crate_opens_held",
                    true,
                    "Right-click with a held crate opens it; sneak to place it.");
    public static final ConfigEntry<Boolean> ORIGINAL_STEREO_EXPLOSIONS =
            bool(
                    Domain.CLIENT,
                    "audio",
                    "original_stereo_explosions",
                    false,
                    "Preserve stereo explosion recordings instead of positioning them in the world, with distance falloff. "
                            + "Mono recordings remain positional. Applies to newly played sounds.");
    public static final ConfigEntry<String> SPACE_OVERWORLD_SKY =
            text(
                    Domain.CLIENT,
                    "render",
                    "space_overworld_sky",
                    "auto",
                    "With Space on, the overworld's sky: full (NTM:Space's celestial sky replaces it), layered (the vanilla sky, and whatever other mods or shader packs draw in it, stays; Space adds its planets, satellites and stations on top), off (vanilla only), or auto (layered when a shader pack or another sky mod, such as Create: Rocketnautics, Exposure: Space or No Man's Land, is present, otherwise full).");
    public static final ConfigEntry<Boolean> SPACE_PLSS_BREATHING =
            bool(Domain.CLIENT, "render", "space_plss_breathing", true, "NTM:Space: play the breathing sound while wearing a PLSS oxygen tank.");
    public static final ConfigEntry<Boolean> COOLING_TOWER_PARTICLES =
            bool(Domain.CLIENT, "render", "cooling_tower_particles", true, "");
    public static final ConfigEntry<Boolean> SMOOTH_MODEL_SHADING =
            bool(
                    Domain.CLIENT,
                    "render",
                    "smooth_model_shading",
                    true,
                    "On: 3D models are drawn like the original mod, with the smooth normals from the model files and soft two-light shading. Off: models are shaded per face with block-style shading. Needs a restart.");
    public static final ConfigEntry<Boolean> GUN_MODEL_FOV =
            bool(
                    Domain.CLIENT,
                    "guns",
                    "model_fov",
                    false,
                    "Render held gun models with the game's FOV setting instead of the fixed gun FOV.");
    public static final ConfigEntry<Boolean> GUN_VISUAL_RECOIL =
            bool(Domain.CLIENT, "guns", "visual_recoil", true, "");
    public static final ConfigEntry<Double> GUN_ANIMATION_SPEED =
            real(Domain.CLIENT, "guns", "animation_speed", 1D, 0.001D, 100D, "");
    public static final ConfigEntry<Boolean> GUN_ANIMS_LEGACY =
            bool(
                    Domain.CLIENT,
                    "guns",
                    "legacy_animations",
                    false,
                    "Use the hand-authored gun animations instead of the Blender-exported sets.");
    public static final ConfigEntry<Boolean> RENDER_CABLE_HANG =
            bool(
                    Domain.CLIENT,
                    "render",
                    "cable_hang",
                    true,
                    "Draw pylon wires with sag instead of straight.");
    public static final ConfigEntry<Boolean> NUKE_HUD_FLASH =
            bool(Domain.CLIENT, "hud", "nuke_flash", true, "");
    public static final ConfigEntry<Boolean> NUKE_HUD_SHAKE =
            bool(Domain.CLIENT, "hud", "nuke_shake", true, "");
    public static final ConfigEntry<Boolean> RENDER_REBAR_SIMPLE =
            bool(
                    Domain.CLIENT,
                    "render",
                    "simple_rebar",
                    false,
                    "Draw rebar as three rods instead of twelve.");
    public static final ConfigEntry<Integer> GEIGER_OFFSET_HORIZONTAL =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "geiger_offset_horizontal",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Integer> GEIGER_OFFSET_VERTICAL =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "geiger_offset_vertical",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Integer> INFO_POSITION =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "info_position",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Integer> INFO_OFFSET_HORIZONTAL =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "info_offset_horizontal",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Integer> INFO_OFFSET_VERTICAL =
            integer(
                    Domain.CLIENT,
                    "hud",
                    "info_offset_vertical",
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    "");
    public static final ConfigEntry<Boolean> TOOLTIP_CUSTOM_NUKE =
            bool(
                    Domain.CLIENT,
                    "hud",
                    "tooltip_custom_nuke",
                    true,
                    "Show what an item adds to a custom nuke.");
    public static final ConfigEntry<Boolean> DODD_RBMK_DIAGNOSTIC =
            bool(
                    Domain.CLIENT,
                    "hud",
                    "dodd_rbmk_diagnostic",
                    true,
                    "List an RBMK column's diagnostic data while looking at it.");

    private static final List<ConfigSchemaExtension> EXTENSIONS =
            ServiceLoader.load(ConfigSchemaExtension.class, ConfigSchema.class.getClassLoader())
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .toList();

    public static final List<ConfigEntry<?>> ENTRIES =
            withAddons(
                    CONVEYOR_CRAM_MAX,
                    CONVEYOR_CRAM_EXPLODE,
                    ULTRA_LARP_MODE,
                    ENABLE_BOMBER_SHORT_MODE,
                    FAR_TRACKING_RANGE,
                    ENABLE_LBSM,
                    ENABLE_LBSM_FULL_SCHRAB,
                    ENABLE_LBSM_SHORTER_DECAY,
                    ENABLE_LBSM_SIMPLE_ARMOR_RECIPES,
                    ENABLE_LBSM_SIMPLE_TOOL_RECIPES,
                    ENABLE_LBSM_SIMPLE_CRAFTING,
                    DISABLE_ALL_GUNS,
                    DISABLE_POST_REWORK_GUNS,
                    DISABLE_PRE_REWORK_GUNS,
                    DISABLE_WAR_EXPLOSIVES,
                    DISABLE_WAR_MISSILES,
                    DISABLE_WAR_NUKES,
                    DISABLE_WAR_BOMBS,
                    DISABLE_WAR_MISC,
                    ENABLE_PEDESTAL_RECIPE,
                    ENABLE_SECRET_PART_RECIPES,
                    ENABLE_RED_ROOM_KEYHOLE_RECIPE,
                    ENABLE_SECRET_AUGMENT_RECIPES,
                    ENABLE_V1_RECIPE,
                    ENABLE_SECRET_SELENIUM_STEEL_RECIPE,
                    ENABLE_SECRET_CANISTER_RECIPE,
                    ENABLE_SECRET_CONTROLLER_RECIPE,
                    ENABLE_SECRET_FOLLY_RECIPE,
                    ENABLE_SECRET_ABERRATOR_RECIPE,
                    DISABLE_HEAVY_TURRETS,
                    DISABLE_TURRET_HENRY,
                    DISABLE_TURRET_GREG,
                    PRE_REWORK_GUNS_USE_NEW_AMMO,
                    ENABLE_LBSM_SIMPLE_CHEMISTRY,
                    ENABLE_LBSM_SIMPLE_CENTRIFUGE,
                    ENABLE_LBSM_UNLOCK_ANVIL,
                    ENABLE_LBSM_SIMPLE_MEDICINE,
                    ENABLE_LBSM_SAFE_CRATES,
                    ENABLE_LBSM_SAFE_ME_DRIVES,
                    ENABLE_FOREIGN_MOD_CHANGES,
                    ENABLE_MEKANISM_CHANGES,
                    ENABLE_TECHREBORN_CHANGES,
                    ENABLE_EXPENSIVE_MODE,
                    ENABLE_MOTD,
                    ENABLE_DEBUG_MODE,
                    ENABLE_EXTENDED_LOGGING,
                    ENABLE_SKYBOXES,
                    ENABLE_528,
                    ENABLE_528_PRESSURIZED_RECIPES,
                    ENABLE_528_MACHINE_GRAVITY,
                    ENABLE_528_FORCE_REASIM_BOILERS,
                    ENABLE_528_BOSNIA_SIMULATOR,
                    RAD_TICK_RATE,
                    HAZARD_RATE,
                    DISABLE_COAL_DUST,
                    LIMIT_EXPLOSION_LIFESPAN,
                    BLAST_SPEED,
                    MK5_BLAST_TIME,
                    FALLOUT_DELAY,
                    BLAST_CHUNKS_IN_FLIGHT,
                    EXPLOSION_ALGORITHM,
                    BOMB_MAX_THREADS,
                    FOREIGN_EXPORT_RESERVE,
                    WAYPOINT_DEBUG,
                    GLYPHIDS_ENABLED,
                    WORLD_DUDS,
                    WORLD_METEORITES,
                    WORLD_WASTE_BARRELS,
                    WORLD_LANDING_CAPSULES,
                    WORLD_LOCKED_SAFES,
                    WORLD_LANTERN_BEHEMOTHS,
                    WORLD_ARCTIC_VAULTS,
                    WORLD_LIBRARY_DUNGEONS,
                    WORLD_GEYSERS,
                    WORLD_NETHER_GEYSERS,
                    NUCLEAR_CREEPERS,
                    TOOL_RECURSION_DEPTH,
                    DAMAGE_COMPATIBILITY_MODE,
                    SPIKES_INSTAKILL,
                    SAFE_SC_BATTERY_SOCKET,
                    GUNS_VANILLA_DURABILITY,
                    ITEM_DURABILITY,
                    NTM_ENCHANTING,
                    ABILITY_ENCHANT_STACKING,
                    NTM_ENCHANTING_EFFICIENCY,
                    NTM_ENCHANTING_ALLOWED,
                    TRADES_BOTTLECAPS,
                    TRADES_BOTTLECAP_ITEM,
                    TRADES_CAPS_PER_EMERALD,
                    TRADES_LOOSE_CHANGE,
                    TRADES_CURRENCIES,
                    ENABLE_DOOR_ACCESS,
                    ENABLE_SPACE,
                    SPACE_CHANGES,
                    SPACE_OVERWORLD_METEOR_DUNGEONS,
                    SPACE_OVERWORLD_AUSTRALIUM,
                    SPACE_NETHER_PORTALS,
                    SPACE_VOLCANOES,
                    SPACE_ORE_LOCATIONS,
                    SPACE_MAX_PROBE_DISTANCE,
                    SPACE_MAX_STATION_DISTANCE,
                    SPACE_COMBAT_POD_DESPAWN,
                    SPACE_ENABLE_MUN_COWS,
                    SERVER_SAFETY,
                    SPACE_CELESTIAL_RAD,
                    ITEM_HAZARD_DROP_TICKRATE,
                    UNSTABLE_RECOVERY_SWEEP,
                    TOOL_HUD_INDICATOR_X,
                    TOOL_HUD_INDICATOR_Y,
                    CRATE_OPEN_HELD,
                    ORIGINAL_STEREO_EXPLOSIONS,
                    COOLING_TOWER_PARTICLES,
                    SPACE_OVERWORLD_SKY,
                    SPACE_PLSS_BREATHING,
                    SMOOTH_MODEL_SHADING,
                    GUN_MODEL_FOV,
                    GUN_VISUAL_RECOIL,
                    GUN_ANIMATION_SPEED,
                    GUN_ANIMS_LEGACY,
                    RENDER_CABLE_HANG,
                    NUKE_HUD_FLASH,
                    NUKE_HUD_SHAKE,
                    RENDER_REBAR_SIMPLE,
                    GEIGER_OFFSET_HORIZONTAL,
                    GEIGER_OFFSET_VERTICAL,
                    INFO_POSITION,
                    INFO_OFFSET_HORIZONTAL,
                    INFO_OFFSET_VERTICAL,
                    TOOLTIP_CUSTOM_NUKE,
                    DODD_RBMK_DIAGNOSTIC);

    private ConfigSchema() {}

    private static List<ConfigEntry<?>> withAddons(ConfigEntry<?>... ntm) {
        List<ConfigEntry<?>> entries = new ArrayList<>(List.of(ntm));
        Set<String> paths = new HashSet<>();
        for (ConfigEntry<?> entry : ntm) paths.add(entry.section() + "." + entry.key());
        for (ConfigSchemaExtension extension : EXTENSIONS) {
            for (ConfigEntry<?> entry : extension.entries()) {
                if (!paths.add(entry.section() + "." + entry.key())) {
                    throw new IllegalStateException(
                            extension.getClass().getName()
                                    + " redeclares config "
                                    + entry.section()
                                    + "."
                                    + entry.key());
                }
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    public static List<ConfigEntry<?>> of(Domain domain) {
        return ENTRIES.stream().filter(e -> e.domain() == domain).toList();
    }

    public static void loadAddonClientFacades(ConfigStore client) {
        for (ConfigSchemaExtension extension : EXTENSIONS) extension.loadClient(client);
    }
}
