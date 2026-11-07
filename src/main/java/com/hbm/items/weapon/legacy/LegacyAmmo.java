package com.hbm.items.weapon.legacy;

import static com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo.*;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.items.weapon.sedna.factory.XFactory22lr;
import com.hbm.items.weapon.sedna.factory.XFactory357;
import com.hbm.items.weapon.sedna.factory.XFactory40mm;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.items.weapon.sedna.factory.XFactory9mm;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * How the pre-rework rounds meet the post-rework ones.
 *
 * <p>Converted: an old round with an exact post-rework twin. With pre_rework_guns_use_new_ammo on, the old guns load
 * the twin and the old item is hidden like a disabled one.
 *
 * <p>Unique: an old round with no twin. Post-rework guns of the same calibre accept it, as a clone of their standard
 * round bound to the old item.
 */
public final class LegacyAmmo {

    private static final EnumAmmo[] NONE = {};
    private static final Map<String, EnumAmmo[]> CONVERTED = new HashMap<>();

    private static void conv(String old, EnumAmmo... twins) {
        CONVERTED.put(old, twins);
    }

    static {
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

    /** the post-rework twins of an old round, the first being the one shown; empty for none */
    public static EnumAmmo[] twins(String oldPath) {
        return CONVERTED.getOrDefault(oldPath, NONE);
    }

    public static boolean isConverted(String oldPath) {
        return CONVERTED.containsKey(oldPath);
    }

    /** every accepted-bullet list of a post-rework magazine, filled as the magazines are built */
    public static final List<List<BulletConfig>> MAG_LISTS = new ArrayList<>();

    /** the old rounds with no twin, by calibre: "prefix", then suffixes ("" = the prefix itself) */
    private static final String[][] UNIQUE = {
        {"ammo_357_", "iron", "nuclear", "gold", "desh", "schrabidium", "steel", "nightmare1", "nightmare2"},
        {"ammo_22lr_", "chlorophyte"},
        {"ammo_9mm_", "du", "chlorophyte", "rocket"},
        {"ammo_556_", "gold", "phosphorus", "du", "star", "chlorophyte", "sleek", "tracer", "flechette",
            "flechette_incendiary", "flechette_phosphorus", "flechette_du", "flechette_chlorophyte", "flechette_sleek", "k"},
        {"ammo_762_", "tracer", "phosphorus", "k"},
        {"ammo_50bmg_", "incendiary", "phosphorus", "star", "chlorophyte", "sleek", "flechette", "flechette_am",
            "flechette_po"},
        {"ammo_12gauge_", "incendiary", "shrapnel", "du", "sleek", "percussion"},
        {"ammo_grenade_", "he", "toxic", "sleek", "concussion", "finned", "nuclear", "tracer", "kampf", "leadburster"},
        {"ammo_rocket_", "he", "emp", "sleek", "shrapnel", "glare", "nuclear", "toxic", "rpc", "canister", "digamma"},
        {"ammo_fuel_", "phosphorus", "vaporizer"},
        {"ammo_nuke_", "safe", "pumpkin", "barrel"},
        {"ammo_mirv", "", "_low", "_high", "_safe", "_special"},
    };

    /** the post-rework standard round(s) a calibre's unique rounds copy, matched per magazine */
    private static BulletConfig[] templates(int calibre) {
        return switch (calibre) {
            case 0 -> new BulletConfig[] {XFactory357.m357_fmj};
            case 1 -> new BulletConfig[] {XFactory22lr.p22_fmj};
            case 2 -> new BulletConfig[] {XFactory9mm.p9_fmj};
            case 3 -> new BulletConfig[] {XFactory556mm.r556_fmj};
            case 4 -> new BulletConfig[] {XFactory762mm.r762_fmj};
            case 5 -> new BulletConfig[] {XFactory50.bmg50_fmj};
            case 6 -> new BulletConfig[] {XFactory12ga.g12, XFactory12ga.g12_bp};
            case 7 -> new BulletConfig[] {XFactory40mm.g40_he};
            case 8 -> new BulletConfig[] {
                XFactoryRocket.rocket_rpzb[0], XFactoryRocket.rocket_qd[0], XFactoryRocket.rocket_ml[0],
                XFactoryRocket.rocket_ncrpa[0], XFactoryRocket.rocket_ncrpa_steer[0], XFactoryRocket.rocket_ff
            };
            case 9 -> new BulletConfig[] {XFactoryFlamer.flame_diesel};
            default -> new BulletConfig[] {XFactoryCatapult.nuke_standard};
        };
    }

    /**
     * Run once after every post-rework gun is built. Clones register in a fixed order (they're bullet ids), so this
     * never looks at the config.
     */
    public static void bridgeToNewGuns() {
        Map<BulletConfig, Map<String, BulletConfig>> clones = new IdentityHashMap<>();
        for (List<BulletConfig> accepted : MAG_LISTS) {
            List<BulletConfig> add = new ArrayList<>();
            for (int c = 0; c < UNIQUE.length; c++) {
                BulletConfig template = null;
                for (BulletConfig t : templates(c)) {
                    if (t != null && accepted.contains(t)) {
                        template = t;
                        break;
                    }
                }
                if (template == null) continue;
                final BulletConfig base = template;
                Map<String, BulletConfig> byRound = clones.computeIfAbsent(template, k -> new HashMap<>());
                for (int s = 1; s < UNIQUE[c].length; s++) {
                    String id = UNIQUE[c][0] + UNIQUE[c][s];
                    add.add(byRound.computeIfAbsent(id, k -> base.clone().setItem(item(k))));
                }
            }
            accepted.addAll(add);
        }
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
