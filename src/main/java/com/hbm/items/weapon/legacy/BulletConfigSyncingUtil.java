// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.items.weapon.legacy.factory.Gun357MagnumFactory;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's bullet config registry. The ids are main's, in main's
 * order; a family's configs join {@link #loadConfigsForSync} once that family is ported. Built on
 * first use, after the items they name are registered.
 */
public final class BulletConfigSyncingUtil {

    private static final Int2ObjectOpenHashMap<BulletConfiguration> configSet =
            new Int2ObjectOpenHashMap<>();
    private static final Reference2IntOpenHashMap<BulletConfiguration> keys =
            new Reference2IntOpenHashMap<>();
    private static volatile boolean loaded;

    static int i = 0;

    public static final int TEST_CONFIG = i++;
    public static final int IRON_REVOLVER = i++;
    public static final int STEEL_REVOLVER = i++;
    public static final int LEAD_REVOLVER = i++;
    public static final int GOLD_REVOLVER = i++;
    public static final int CURSED_REVOLVER = i++;
    public static final int SCHRABIDIUM_REVOLVER = i++;
    public static final int NIGHT_REVOLVER = i++;
    public static final int NIGHT2_REVOLVER = i++;
    public static final int SATURNITE_REVOLVER = i++;
    public static final int DESH_REVOLVER = i++;
    public static final int IRON_HS = i++;
    public static final int STEEL_HS = i++;
    public static final int GOLD_HS = i++;
    public static final int LEAD_HS = i++;
    public static final int DESH_HS = i++;
    public static final int G20_NORMAL = i++;
    public static final int G20_SLUG = i++;
    public static final int G20_FLECHETTE = i++;
    public static final int G20_FIRE = i++;
    public static final int G20_SHRAPNEL = i++;
    public static final int G20_EXPLOSIVE = i++;
    public static final int G20_CAUSTIC = i++;
    public static final int G20_SHOCK = i++;
    public static final int G20_WITHER = i++;
    public static final int G20_SLEEK = i++;
    public static final int ROCKET_NORMAL = i++;
    public static final int ROCKET_HE = i++;
    public static final int ROCKET_INCENDIARY = i++;
    public static final int ROCKET_SHRAPNEL = i++;
    public static final int ROCKET_EMP = i++;
    public static final int ROCKET_GLARE = i++;
    public static final int ROCKET_SLEEK = i++;
    public static final int ROCKET_NUKE = i++;
    public static final int ROCKET_CHAINSAW = i++;
    public static final int ROCKET_TOXIC = i++;
    public static final int ROCKET_PHOSPHORUS = i++;
    public static final int ROCKET_CANISTER = i++;
    public static final int ROCKET_ERROR = i++;
    public static final int GRENADE_NORMAL = i++;
    public static final int GRENADE_HE = i++;
    public static final int GRENADE_INCENDIARY = i++;
    public static final int GRENADE_CHEMICAL = i++;
    public static final int GRENADE_SLEEK = i++;
    public static final int GRENADE_CONCUSSION = i++;
    public static final int GRENADE_FINNED = i++;
    public static final int GRENADE_NUCLEAR = i++;
    public static final int GRENADE_PHOSPHORUS = i++;
    public static final int GRENADE_TRACER = i++;
    public static final int GRENADE_KAMPF = i++;
    public static final int GRENADE_LEADBURSTER = i++;
    public static final int G12_NORMAL = i++;
    public static final int G12_INCENDIARY = i++;
    public static final int G12_SHRAPNEL = i++;
    public static final int G12_DU = i++;
    public static final int G12_AM = i++;
    public static final int G12_SLEEK = i++;
    public static final int G12_PERCUSSION = i++;
    public static final int G12HS_NORMAL = i++;
    public static final int G12HS_INCENDIARY = i++;
    public static final int G12HS_SHRAPNEL = i++;
    public static final int G12HS_DU = i++;
    public static final int G12HS_AM = i++;
    public static final int G12HS_SLEEK = i++;
    public static final int G12HS_PERCUSSION = i++;
    public static final int LR22_NORMAL = i++;
    public static final int LR22_AP = i++;
    public static final int LR22_NORMAL_FIRE = i++;
    public static final int LR22_AP_FIRE = i++;
    public static final int M44_NORMAL = i++;
    public static final int M44_AP = i++;
    public static final int M44_DU = i++;
    public static final int M44_STAR = i++;
    public static final int M44_PIP = i++;
    public static final int M44_BJ = i++;
    public static final int M44_SILVER = i++;
    public static final int M44_ROCKET = i++;
    public static final int M44_PHOSPHORUS = i++;
    public static final int P9_NORMAL = i++;
    public static final int P9_AP = i++;
    public static final int P9_DU = i++;
    public static final int P9_ROCKET = i++;
    public static final int ACP_45 = i++;
    public static final int ACP_45_AP = i++;
    public static final int ACP_45_DU = i++;
    public static final int BMG50_NORMAL = i++;
    public static final int BMG50_INCENDIARY = i++;
    public static final int BMG50_EXPLOSIVE = i++;
    public static final int BMG50_AP = i++;
    public static final int BMG50_DU = i++;
    public static final int BMG50_STAR = i++;
    public static final int BMG50_PHOSPHORUS = i++;
    public static final int BMG50_SLEEK = i++;
    public static final int BMG50_FLECHETTE_NORMAL = i++;
    public static final int BMG50_FLECHETTE_AM = i++;
    public static final int BMG50_FLECHETTE_PO = i++;
    public static final int ROUND_LUNA_SNIPER_SABOT = i++;
    public static final int ROUND_LUNA_SNIPER_INCENDIARY = i++;
    public static final int ROUND_LUNA_SNIPER_EXPLOSIVE = i++;
    public static final int R5_NORMAL = i++;
    public static final int R5_EXPLOSIVE = i++;
    public static final int R5_DU = i++;
    public static final int R5_STAR = i++;
    public static final int R5_NORMAL_BOLT = i++;
    public static final int R5_EXPLOSIVE_BOLT = i++;
    public static final int R5_DU_BOLT = i++;
    public static final int R5_STAR_BOLT = i++;
    public static final int AE50_NORMAL = i++;
    public static final int AE50_AP = i++;
    public static final int AE50_DU = i++;
    public static final int AE50_STAR = i++;
    public static final int G4_NORMAL = i++;
    public static final int G4_SLUG = i++;
    public static final int G4_FLECHETTE = i++;
    public static final int G4_FLECHETTE_PHOSPHORUS = i++;
    public static final int G4_EXPLOSIVE = i++;
    public static final int G4_SEMTEX = i++;
    public static final int G4_BALEFIRE = i++;
    public static final int G4_KAMPF = i++;
    public static final int G4_CANISTER = i++;
    public static final int G4_CLAW = i++;
    public static final int G4_VAMPIRE = i++;
    public static final int G4_VOID = i++;
    public static final int G4_TITAN = i++;
    public static final int G4_SLEEK = i++;
    public static final int SPECIAL_OSIPR = i++;
    public static final int SPECIAL_OSIPR_CHARGED = i++;
    public static final int SPECIAL_GAUSS = i++;
    public static final int SPECIAL_GAUSS_CHARGED = i++;
    public static final int SPECIAL_EMP = i++;
    public static final int COIL_NORMAL = i++;
    public static final int COIL_DU = i++;
    public static final int COIL_RUBBER = i++;
    public static final int FLAMER_NORMAL = i++;
    public static final int FLAMER_NAPALM = i++;
    public static final int FLAMER_WP = i++;
    public static final int FLAMER_VAPORIZER = i++;
    public static final int FLAMER_GAS = i++;
    public static final int CRYO_NORMAL = i++;
    public static final int FEXT_NORMAL = i++;
    public static final int FEXT_FOAM = i++;
    public static final int FEXT_SAND = i++;
    public static final int R556_NORMAL = i++;
    public static final int R556_GOLD = i++;
    public static final int R556_PHOSPHORUS = i++;
    public static final int R556_AP = i++;
    public static final int R556_DU = i++;
    public static final int R556_STAR = i++;
    public static final int R556_SLEEK = i++;
    public static final int R556_TRACER = i++;
    public static final int R556_FLECHETTE = i++;
    public static final int R556_FLECHETTE_INCENDIARY = i++;
    public static final int R556_FLECHETTE_PHOSPHORUS = i++;
    public static final int R556_FLECHETTE_DU = i++;
    public static final int R556_FLECHETTE_SLEEK = i++;
    public static final int R556_K = i++;
    public static final int R762_NORMAL = i++;
    public static final int R762_PHOSPHORUS = i++;
    public static final int R762_AP = i++;
    public static final int R762_DU = i++;
    public static final int R762_TRACER = i++;
    public static final int R762_K = i++;
    public static final int B75_NORMAL = i++;
    public static final int B75_INCENDIARY = i++;
    public static final int B75_HE = i++;
    public static final int NEEDLE_GPS = i++;
    public static final int NEEDLE_NUKE = i++;
    public static final int DART_NORMAL = i++;
    public static final int G20_NORMAL_FIRE = i++;
    public static final int G20_SHRAPNEL_FIRE = i++;
    public static final int G20_SLUG_FIRE = i++;
    public static final int G20_FLECHETTE_FIRE = i++;
    public static final int G20_EXPLOSIVE_FIRE = i++;
    public static final int G20_CAUSTIC_FIRE = i++;
    public static final int G20_SHOCK_FIRE = i++;
    public static final int G20_WITHER_FIRE = i++;
    public static final int ROCKET_NORMAL_LASER = i++;
    public static final int ROCKET_HE_LASER = i++;
    public static final int ROCKET_INCENDIARY_LASER = i++;
    public static final int ROCKET_SHRAPNEL_LASER = i++;
    public static final int ROCKET_EMP_LASER = i++;
    public static final int ROCKET_GLARE_LASER = i++;
    public static final int ROCKET_SLEEK_LASER = i++;
    public static final int ROCKET_NUKE_LASER = i++;
    public static final int ROCKET_CHAINSAW_LASER = i++;
    public static final int ROCKET_TOXIC_LASER = i++;
    public static final int ROCKET_PHOSPHORUS_LASER = i++;
    public static final int ROCKET_STINGER = i++;
    public static final int ROCKET_STINGER_HE = i++;
    public static final int ROCKET_STINGER_INCENDIARY = i++;
    public static final int ROCKET_STINGER_NUCLEAR = i++;
    public static final int ROCKET_STINGER_BONES = i++;
    public static final int SHELL_NORMAL = i++;
    public static final int SHELL_EXPLOSIVE = i++;
    public static final int SHELL_AP = i++;
    public static final int SHELL_DU = i++;
    public static final int SHELL_W9 = i++;
    public static final int DGK_NORMAL = i++;
    public static final int FLA_NORMAL = i++;
    public static final int NUKE_NORMAL = i++;
    public static final int NUKE_LOW = i++;
    public static final int NUKE_HIGH = i++;
    public static final int NUKE_TOTS = i++;
    public static final int NUKE_SAFE = i++;
    public static final int NUKE_PUMPKIN = i++;
    public static final int NUKE_BARREL = i++;
    public static final int NUKE_PROTO_NORMAL = i++;
    public static final int NUKE_PROTO_LOW = i++;
    public static final int NUKE_PROTO_HIGH = i++;
    public static final int NUKE_PROTO_TOTS = i++;
    public static final int NUKE_PROTO_SAFE = i++;
    public static final int NUKE_PROTO_PUMPKIN = i++;
    public static final int NUKE_MIRV_NORMAL = i++;
    public static final int NUKE_MIRV_LOW = i++;
    public static final int NUKE_MIRV_HIGH = i++;
    public static final int NUKE_MIRV_SAFE = i++;
    public static final int NUKE_MIRV_SPECIAL = i++;
    public static final int NUKE_AMAT = i++;
    public static final int TWR_RAY = i++;
    public static final int HLR_NORMAL = i++;
    public static final int HLR_ALT = i++;
    public static final int ZOMG_BOLT = i++;
    public static final int DET_BOLT = i++;
    public static final int TURBINE = i++;
    public static final int GLASS_EMRADIO = i++;
    public static final int GLASS_EMMICRO = i++;
    public static final int GLASS_EMIR = i++;
    public static final int GLASS_EMVISIBLE = i++;
    public static final int GLASS_EMUV = i++;
    public static final int GLASS_EMXRAY = i++;
    public static final int GLASS_EMGAMMA = i++;
    public static final int CHL_LR22 = i++;
    public static final int CHL_LR22_FIRE = i++;
    public static final int CHL_M44 = i++;
    public static final int CHL_P9 = i++;
    public static final int CHL_BMG50 = i++;
    public static final int CHL_R5 = i++;
    public static final int CHL_R5_BOLT = i++;
    public static final int CHL_AE50 = i++;
    public static final int CHL_R556 = i++;
    public static final int CHL_R556_FLECHETTE = i++;
    public static final int MASKMAN_BULLET = i++;
    public static final int MASKMAN_ORB = i++;
    public static final int MASKMAN_BOLT = i++;
    public static final int MASKMAN_ROCKET = i++;
    public static final int MASKMAN_TRACER = i++;
    public static final int MASKMAN_METEOR = i++;
    public static final int WORM_BOLT = i++;
    public static final int WORM_LASER = i++;
    public static final int UFO_ROCKET = i++;

    private BulletConfigSyncingUtil() {}

    private static synchronized void loadConfigsForSync() {
        if (loaded) return;
        keys.defaultReturnValue(-1);

        configSet.put(IRON_REVOLVER, Gun357MagnumFactory.getRevIronConfig());
        configSet.put(STEEL_REVOLVER, Gun357MagnumFactory.getRevLeadConfig());
        configSet.put(LEAD_REVOLVER, Gun357MagnumFactory.getRevNuclearConfig());
        configSet.put(GOLD_REVOLVER, Gun357MagnumFactory.getRevGoldConfig());
        configSet.put(CURSED_REVOLVER, Gun357MagnumFactory.getRevCursedConfig());
        configSet.put(SCHRABIDIUM_REVOLVER, Gun357MagnumFactory.getRevSchrabidiumConfig());
        configSet.put(NIGHT_REVOLVER, Gun357MagnumFactory.getRevNightmare1Config());
        configSet.put(NIGHT2_REVOLVER, Gun357MagnumFactory.getRevNightmare2Config());
        configSet.put(SATURNITE_REVOLVER, Gun357MagnumFactory.getRevLeadConfig().setToFire(3));
        configSet.put(DESH_REVOLVER, Gun357MagnumFactory.getRevDeshConfig());

        configSet.put(IRON_HS, Gun357MagnumFactory.getRevIronConfig().setHeadshot(3F));
        configSet.put(STEEL_HS, Gun357MagnumFactory.getRevCursedConfig().setHeadshot(3F));
        configSet.put(GOLD_HS, Gun357MagnumFactory.getRevGoldConfig().setHeadshot(3F));
        configSet.put(LEAD_HS, Gun357MagnumFactory.getRevLeadConfig().setHeadshot(3F));
        configSet.put(DESH_HS, Gun357MagnumFactory.getRevDeshConfig().setHeadshot(3F));

        configSet.int2ObjectEntrySet().forEach(e -> keys.put(e.getValue(), e.getIntKey()));
        loaded = true;
    }

    public static BulletConfiguration pullConfig(int key) {
        if (!loaded) loadConfigsForSync();
        return configSet.get(key);
    }

    public static int getKey(BulletConfiguration config) {
        if (!loaded) loadConfigsForSync();
        return keys.getInt(config);
    }
}
