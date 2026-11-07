package com.hbm.items.weapon.legacy;

import static com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo.*;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * How the pre-rework rounds meet the post-rework ones, by calibre.
 *
 * <p>Old guns: with pre_rework_guns_use_new_ammo on, every old config of a calibre also loads every post-rework round
 * of it. An old round with an exact post-rework twin is shown as that twin and hidden like a disabled item.
 *
 * <p>New guns: every post-rework magazine also takes every old round of its calibre, as a copy of the round the
 * magazine already has that matches it (the old round's twin, else the calibre's standard round) bound to the old item.
 */
public final class LegacyAmmo {

    private static final EnumAmmo[] NONE = {};

    /** post-rework calibre (EnumAmmo name prefix) -> its standard round, and every old round of it */
    private static final Map<String, EnumAmmo> STANDARD = new LinkedHashMap<>();
    private static final Map<String, List<String>> OLD = new LinkedHashMap<>();
    /** old round -> calibre */
    private static final Map<String, String> CALIBRE = new HashMap<>();
    /** old round -> its exact post-rework twins, the first being the one shown */
    private static final Map<String, EnumAmmo[]> CONVERTED = new HashMap<>();

    private static void calibre(String prefix, EnumAmmo standard, String oldBase, String... suffixes) {
        STANDARD.put(prefix, standard);
        List<String> old = OLD.computeIfAbsent(prefix, k -> new ArrayList<>());
        for (String s : suffixes) {
            old.add(oldBase + s);
            CALIBRE.put(oldBase + s, prefix);
        }
    }

    private static void conv(String old, EnumAmmo... twins) {
        CONVERTED.put(old, twins);
    }

    static {
        calibre("M357_", M357_FMJ, "ammo_357_", "lead", "iron", "steel", "nuclear", "gold", "desh", "schrabidium",
                "nightmare1", "nightmare2");
        calibre("P22_", P22_FMJ, "ammo_22lr", "", "_ap", "_chlorophyte");
        calibre("P9_", P9_FMJ, "ammo_9mm", "", "_ap", "_du", "_chlorophyte", "_rocket");
        calibre("P45_", P45_FMJ, "ammo_45", "", "_ap", "_du");
        calibre("R556_", R556_FMJ, "ammo_556", "", "_ap", "_gold", "_phosphorus", "_du", "_star", "_chlorophyte",
                "_sleek", "_tracer", "_flechette", "_flechette_incendiary", "_flechette_phosphorus", "_flechette_du",
                "_flechette_chlorophyte", "_flechette_sleek", "_k");
        calibre("R762_", R762_FMJ, "ammo_762", "", "_ap", "_du", "_tracer", "_phosphorus", "_k");
        calibre("BMG50_", BMG50_FMJ, "ammo_50bmg", "", "_ap", "_du", "_explosive", "_incendiary", "_phosphorus",
                "_star", "_chlorophyte", "_sleek", "_flechette", "_flechette_am", "_flechette_po");
        calibre("B75", B75, "ammo_75bolt", "", "_incendiary", "_he");
        calibre("G12", G12, "ammo_12gauge", "", "_incendiary", "_shrapnel", "_du", "_sleek", "_percussion");
        calibre("G40_", G40_HE, "ammo_grenade", "", "_he", "_incendiary", "_phosphorus", "_toxic", "_sleek",
                "_concussion", "_finned", "_nuclear", "_tracer", "_kampf", "_leadburster");
        calibre("ROCKET_", ROCKET_HE, "ammo_rocket", "", "_he", "_incendiary", "_phosphorus", "_emp", "_sleek",
                "_shrapnel", "_glare", "_nuclear", "_toxic", "_rpc", "_canister", "_digamma");
        calibre("FLAME_", FLAME_DIESEL, "ammo_fuel", "", "_napalm", "_gas", "_phosphorus", "_vaporizer");
        calibre("NUKE_", NUKE_STANDARD, "ammo_nuke", "", "_low", "_high", "_tots", "_balefire", "_safe", "_pumpkin",
                "_barrel");
        calibre("NUKE_", NUKE_STANDARD, "ammo_mirv", "", "_low", "_high", "_safe", "_special");

        conv("ammo_357_lead", M357_SP, M357_BP, M357_FMJ, M357_JHP, M357_EXPRESS);
        conv("ammo_22lr", P22_FMJ, P22_SP, P22_JHP);
        conv("ammo_22lr_ap", P22_AP);
        conv("ammo_9mm", P9_FMJ, P9_SP, P9_JHP);
        conv("ammo_9mm_ap", P9_AP);
        conv("ammo_45", P45_FMJ, P45_SP, P45_JHP);
        conv("ammo_45_ap", P45_AP);
        conv("ammo_45_du", P45_DU);
        conv("ammo_556", R556_FMJ, R556_SP, R556_JHP);
        conv("ammo_556_ap", R556_AP);
        conv("ammo_762", R762_FMJ, R762_SP, R762_JHP, R762_HE);
        conv("ammo_762_ap", R762_AP);
        conv("ammo_762_du", R762_DU);
        conv("ammo_50bmg", BMG50_FMJ, BMG50_SP, BMG50_JHP, BMG50_SM);
        conv("ammo_50bmg_ap", BMG50_AP);
        conv("ammo_50bmg_du", BMG50_DU);
        conv("ammo_50bmg_explosive", BMG50_HE);
        conv("ammo_75bolt", B75);
        conv("ammo_75bolt_incendiary", B75_INC);
        conv("ammo_75bolt_he", B75_EXP);
        conv("ammo_12gauge", G12, G12_BP, G12_MAGNUM, G12_BP_MAGNUM);
        conv("ammo_grenade", G40_HE);
        conv("ammo_grenade_incendiary", G40_INC);
        conv("ammo_grenade_phosphorus", G40_PHOSPHORUS);
        conv("ammo_rocket", ROCKET_HE);
        conv("ammo_rocket_incendiary", ROCKET_INC);
        conv("ammo_rocket_phosphorus", ROCKET_PHOSPHORUS);
        conv("ammo_fuel", FLAME_DIESEL);
        conv("ammo_fuel_napalm", FLAME_NAPALM);
        conv("ammo_fuel_gas", FLAME_GAS);
        conv("ammo_nuke", NUKE_STANDARD);
        conv("ammo_nuke_low", NUKE_DEMO);
        conv("ammo_nuke_high", NUKE_HIGH);
        conv("ammo_nuke_tots", NUKE_TOTS);
        conv("ammo_nuke_balefire", NUKE_BALEFIRE);
    }

    /** the exact post-rework twins of an old round, the first being the one shown; empty for none */
    public static EnumAmmo[] twins(String oldPath) {
        return CONVERTED.getOrDefault(oldPath, NONE);
    }

    public static boolean isConverted(String oldPath) {
        return CONVERTED.containsKey(oldPath);
    }

    /** whether a post-rework round is the same calibre as an old round */
    public static boolean sameCalibre(String oldPath, EnumAmmo type) {
        String prefix = CALIBRE.get(oldPath);
        return prefix != null && type.name().startsWith(prefix);
    }

    // ---- new guns ----

    /** the copies made so far: template -> old round -> copy, and copy -> old round */
    private static final Map<BulletConfig, Map<String, BulletConfig>> COPIES = new IdentityHashMap<>();
    private static final Map<BulletConfig, String> COPY_OF = new IdentityHashMap<>();

    /**
     * Called by a post-rework magazine whenever rounds are added to it. Guns are built at item registration, in the
     * same order on both sides, so the copies (which are bullet ids) register in the same order too. This never looks
     * at the config, for the same reason.
     */
    public static void extend(List<BulletConfig> accepted) {
        List<BulletConfig> add = new ArrayList<>();
        for (Map.Entry<String, List<String>> cal : OLD.entrySet()) {
            String prefix = cal.getKey();
            if (accepted.stream().noneMatch(c -> isOfCalibre(c, prefix))) continue;
            for (String old : cal.getValue()) {
                if (has(accepted, old) || has(add, old)) continue;
                BulletConfig template = pick(accepted, prefix, old);
                add.add(COPIES.computeIfAbsent(template, k -> new HashMap<>()).computeIfAbsent(old, k -> {
                    BulletConfig copy = template.clone().setItem(item(k));
                    COPY_OF.put(copy, k);
                    return copy;
                }));
            }
        }
        accepted.addAll(add);
    }

    private static boolean isOfCalibre(BulletConfig c, String prefix) {
        return c.ammoType != null && !COPY_OF.containsKey(c) && c.ammoType.name().startsWith(prefix);
    }

    private static boolean has(List<BulletConfig> list, String old) {
        for (BulletConfig c : list) if (old.equals(COPY_OF.get(c))) return true;
        return false;
    }

    /** the round to copy for an old one: its twin, else the calibre standard, else the first of the calibre */
    private static BulletConfig pick(List<BulletConfig> accepted, String prefix, String old) {
        for (EnumAmmo twin : twins(old))
            for (BulletConfig c : accepted) if (isOfCalibre(c, prefix) && c.ammoType == twin) return c;
        // the post-rework round with the nearest effect, so the old round keeps its fire, blast or penetration
        for (String effect : effects(old))
            for (BulletConfig c : accepted)
                if (isOfCalibre(c, prefix)
                        && java.util.Arrays.asList(c.ammoType.name().substring(prefix.length()).split("_"))
                                .contains(effect)) return c;
        EnumAmmo standard = STANDARD.get(prefix);
        for (BulletConfig c : accepted) if (isOfCalibre(c, prefix) && c.ammoType == standard) return c;
        for (BulletConfig c : accepted) if (isOfCalibre(c, prefix)) return c;
        throw new IllegalStateException("no " + prefix + " round to copy for " + old);
    }

    /** an old round's effect as post-rework round name parts, nearest first */
    private static String[] effects(String old) {
        if (old.contains("phosphorus")) return new String[] {"PHOSPHORUS", "INC"};
        if (old.contains("incendiary")) return new String[] {"INC", "PHOSPHORUS"};
        if (old.contains("explosive") || old.endsWith("_he") || old.endsWith("_rocket") || old.endsWith("_kampf")
                || old.endsWith("_semtex")) return new String[] {"HE", "EXPLOSIVE", "EXP", "DEMO"};
        if (old.contains("flechette")) return new String[] {"FLECHETTE", "AP"};
        if (old.endsWith("_du")) return new String[] {"DU", "AP"};
        if (old.endsWith("_ap") || old.endsWith("_star") || old.endsWith("_chlorophyte") || old.endsWith("_desh")
                || old.endsWith("_schrabidium") || old.endsWith("_sleek")) return new String[] {"AP", "DU"};
        if (old.endsWith("_slug")) return new String[] {"SLUG"};
        return new String[0];
    }

    private static Supplier<Item> item(String path) {
        return new Supplier<>() {
            private Item cached;

            @Override
            public Item get() {
                if (cached == null) cached = BuiltInRegistries.ITEM.get(Library.id(path));
                return cached;
            }
        };
    }

    private LegacyAmmo() {}
}
