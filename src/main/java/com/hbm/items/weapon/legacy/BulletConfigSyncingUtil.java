// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.items.weapon.legacy.factory.Gun357MagnumFactory;
import com.hbm.items.weapon.legacy.factory.GunRocketFactory;
import com.hbm.items.weapon.legacy.factory.Gun9mmFactory;
import com.hbm.items.weapon.legacy.factory.Gun762mmFactory;
import com.hbm.items.weapon.legacy.factory.Gun75BoltFactory;
import com.hbm.items.weapon.legacy.factory.Gun5mmFactory;
import com.hbm.items.weapon.legacy.factory.Gun556mmFactory;
import com.hbm.items.weapon.legacy.factory.Gun50BMGFactory;
import com.hbm.items.weapon.legacy.factory.Gun50AEFactory;
import com.hbm.items.weapon.legacy.factory.Gun45ACPFactory;
import com.hbm.items.weapon.legacy.factory.Gun44MagnumFactory;
import com.hbm.items.weapon.legacy.factory.Gun22LRFactory;
import com.hbm.items.weapon.legacy.factory.Gun4GaugeFactory;
import com.hbm.items.weapon.legacy.factory.Gun20GaugeFactory;
import com.hbm.items.weapon.legacy.factory.Gun12GaugeFactory;
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


        configSet.put(G20_NORMAL, Gun20GaugeFactory.get20GaugeConfig());
        configSet.put(G20_SLUG, Gun20GaugeFactory.get20GaugeSlugConfig());
        configSet.put(G20_FLECHETTE, Gun20GaugeFactory.get20GaugeFlechetteConfig());
        configSet.put(G20_FIRE, Gun20GaugeFactory.get20GaugeFireConfig());
        configSet.put(G20_SHRAPNEL, Gun20GaugeFactory.get20GaugeShrapnelConfig());
        configSet.put(G20_EXPLOSIVE, Gun20GaugeFactory.get20GaugeExplosiveConfig());
        configSet.put(G20_CAUSTIC, Gun20GaugeFactory.get20GaugeCausticConfig());
        configSet.put(G20_SHOCK, Gun20GaugeFactory.get20GaugeShockConfig());
        configSet.put(G20_WITHER, Gun20GaugeFactory.get20GaugeWitherConfig());
        configSet.put(G20_SLEEK, Gun20GaugeFactory.get20GaugeSleekConfig());
        configSet.put(G12_NORMAL, Gun12GaugeFactory.get12GaugeConfig());
        configSet.put(G12_INCENDIARY, Gun12GaugeFactory.get12GaugeFireConfig());
        configSet.put(G12_SHRAPNEL, Gun12GaugeFactory.get12GaugeShrapnelConfig());
        configSet.put(G12_DU, Gun12GaugeFactory.get12GaugeDUConfig());
        configSet.put(G12_AM, Gun12GaugeFactory.get12GaugeAMConfig());
        configSet.put(G12_SLEEK, Gun12GaugeFactory.get12GaugeSleekConfig());
        configSet.put(G12_PERCUSSION, Gun12GaugeFactory.get12GaugePercussionConfig());
        configSet.put(G12HS_NORMAL, Gun12GaugeFactory.get12GaugeConfig().setHeadshot(2F));
        configSet.put(G12HS_INCENDIARY, Gun12GaugeFactory.get12GaugeFireConfig().setHeadshot(2F));
        configSet.put(G12HS_SHRAPNEL, Gun12GaugeFactory.get12GaugeShrapnelConfig().setHeadshot(2F));
        configSet.put(G12HS_DU, Gun12GaugeFactory.get12GaugeDUConfig().setHeadshot(2F));
        configSet.put(G12HS_AM, Gun12GaugeFactory.get12GaugeAMConfig().setHeadshot(2F));
        configSet.put(G12HS_SLEEK, Gun12GaugeFactory.get12GaugeSleekConfig().setHeadshot(2F));
        configSet.put(G12HS_PERCUSSION, Gun12GaugeFactory.get12GaugePercussionConfig().setHeadshot(2F));
        configSet.put(G4_NORMAL, Gun4GaugeFactory.get4GaugeConfig());
        configSet.put(G4_SLUG, Gun4GaugeFactory.get4GaugeSlugConfig());
        configSet.put(G4_FLECHETTE, Gun4GaugeFactory.get4GaugeFlechetteConfig());
        configSet.put(G4_FLECHETTE_PHOSPHORUS, Gun4GaugeFactory.get4GaugeFlechettePhosphorusConfig());
        configSet.put(G4_EXPLOSIVE, Gun4GaugeFactory.get4GaugeExplosiveConfig());
        configSet.put(G4_SEMTEX, Gun4GaugeFactory.get4GaugeMiningConfig());
        configSet.put(G4_BALEFIRE, Gun4GaugeFactory.get4GaugeBalefireConfig());
        configSet.put(G4_KAMPF, Gun4GaugeFactory.getGrenadeKampfConfig());
        configSet.put(G4_CANISTER, Gun4GaugeFactory.getGrenadeCanisterConfig());
        configSet.put(G4_CLAW, Gun4GaugeFactory.get4GaugeClawConfig());
        configSet.put(G4_VAMPIRE, Gun4GaugeFactory.get4GaugeVampireConfig());
        configSet.put(G4_VOID, Gun4GaugeFactory.get4GaugeVoidConfig());
        configSet.put(G4_TITAN, Gun4GaugeFactory.get4GaugeQuackConfig());
        configSet.put(G4_SLEEK, Gun4GaugeFactory.get4GaugeSleekConfig());
        configSet.put(G20_NORMAL_FIRE, Gun20GaugeFactory.get20GaugeConfig().setToFire(3));
        configSet.put(G20_SHRAPNEL_FIRE, Gun20GaugeFactory.get20GaugeShrapnelConfig().setToFire(3));
        configSet.put(G20_SLUG_FIRE, Gun20GaugeFactory.get20GaugeSlugConfig().setToFire(3));
        configSet.put(G20_FLECHETTE_FIRE, Gun20GaugeFactory.get20GaugeFlechetteConfig().setToFire(3));
        configSet.put(G20_EXPLOSIVE_FIRE, Gun20GaugeFactory.get20GaugeExplosiveConfig().setToFire(3));
        configSet.put(G20_CAUSTIC_FIRE, Gun20GaugeFactory.get20GaugeCausticConfig().setToFire(3));
        configSet.put(G20_SHOCK_FIRE, Gun20GaugeFactory.get20GaugeShockConfig().setToFire(3));
        configSet.put(G20_WITHER_FIRE, Gun20GaugeFactory.get20GaugeWitherConfig().setToFire(3));


        configSet.put(LR22_NORMAL, Gun22LRFactory.get22LRConfig());
        configSet.put(LR22_AP, Gun22LRFactory.get22LRAPConfig());
        configSet.put(LR22_NORMAL_FIRE, Gun22LRFactory.get22LRConfig().setToFire(3));
        configSet.put(LR22_AP_FIRE, Gun22LRFactory.get22LRAPConfig().setToFire(3));
        configSet.put(CHL_LR22, Gun22LRFactory.get22LRConfig().setToHoming(LegacyGuns.AMMO_22LR_CHLOROPHYTE));
        configSet.put(CHL_LR22_FIRE, Gun22LRFactory.get22LRConfig().setToFire(3).setToHoming(LegacyGuns.AMMO_22LR_CHLOROPHYTE));


        configSet.put(M44_NORMAL, Gun44MagnumFactory.getNoPipConfig());
        configSet.put(M44_AP, Gun44MagnumFactory.getNoPipAPConfig());
        configSet.put(M44_DU, Gun44MagnumFactory.getNoPipDUConfig());
        configSet.put(M44_PHOSPHORUS, Gun44MagnumFactory.getPhosphorusConfig());
        configSet.put(M44_STAR, Gun44MagnumFactory.getNoPipStarConfig());
        configSet.put(M44_PIP, Gun44MagnumFactory.getPipConfig());
        configSet.put(M44_BJ, Gun44MagnumFactory.getBJConfig());
        configSet.put(M44_SILVER, Gun44MagnumFactory.getSilverStormConfig());
        configSet.put(M44_ROCKET, Gun44MagnumFactory.getRocketConfig());
        configSet.put(CHL_M44, Gun44MagnumFactory.getNoPipConfig().setToHoming(LegacyGuns.AMMO_44_CHLOROPHYTE));


        configSet.put(ACP_45, Gun45ACPFactory.get45AutoConfig());
        configSet.put(ACP_45_AP, Gun45ACPFactory.get45AutoAPConfig());
        configSet.put(ACP_45_DU, Gun45ACPFactory.get45AutoDUConfig());


        configSet.put(AE50_NORMAL, Gun50AEFactory.get50AEConfig());
        configSet.put(AE50_AP, Gun50AEFactory.get50APConfig());
        configSet.put(AE50_DU, Gun50AEFactory.get50DUConfig());
        configSet.put(AE50_STAR, Gun50AEFactory.get50StarConfig());
        configSet.put(CHL_AE50, Gun50AEFactory.get50AEConfig().setToHoming(LegacyGuns.AMMO_50AE_CHLOROPHYTE));


        configSet.put(BMG50_NORMAL, Gun50BMGFactory.get50BMGConfig());
        configSet.put(BMG50_INCENDIARY, Gun50BMGFactory.get50BMGFireConfig());
        configSet.put(BMG50_PHOSPHORUS, Gun50BMGFactory.get50BMGPhosphorusConfig());
        configSet.put(BMG50_EXPLOSIVE, Gun50BMGFactory.get50BMGExplosiveConfig());
        configSet.put(BMG50_AP, Gun50BMGFactory.get50BMGAPConfig());
        configSet.put(BMG50_DU, Gun50BMGFactory.get50BMGDUConfig());
        configSet.put(BMG50_STAR, Gun50BMGFactory.get50BMGStarConfig());
        configSet.put(BMG50_SLEEK, Gun50BMGFactory.get50BMGSleekConfig());
        configSet.put(BMG50_FLECHETTE_NORMAL, Gun50BMGFactory.get50BMGFlechetteConfig());
        configSet.put(BMG50_FLECHETTE_AM, Gun50BMGFactory.get50BMGFlechetteAMConfig());
        configSet.put(BMG50_FLECHETTE_PO, Gun50BMGFactory.get50BMGFlechettePOConfig());
        configSet.put(ROUND_LUNA_SNIPER_SABOT, Gun50BMGFactory.getLunaticSabotRound());
        configSet.put(ROUND_LUNA_SNIPER_INCENDIARY, Gun50BMGFactory.getLunaticIncendiaryRound());
        configSet.put(ROUND_LUNA_SNIPER_EXPLOSIVE, Gun50BMGFactory.getLunaticExplosiveRound());
        configSet.put(CHL_BMG50, Gun50BMGFactory.get50BMGConfig().setToHoming(LegacyGuns.AMMO_50BMG_CHLOROPHYTE));


        configSet.put(R556_NORMAL, Gun556mmFactory.get556Config());
        configSet.put(R556_GOLD, Gun556mmFactory.get556GoldConfig());
        configSet.put(R556_PHOSPHORUS, Gun556mmFactory.get556PhosphorusConfig());
        configSet.put(R556_AP, Gun556mmFactory.get556APConfig());
        configSet.put(R556_DU, Gun556mmFactory.get556DUConfig());
        configSet.put(R556_STAR, Gun556mmFactory.get556StarConfig());
        configSet.put(R556_SLEEK, Gun556mmFactory.get556SleekConfig());
        configSet.put(R556_TRACER, Gun556mmFactory.get556TracerConfig());
        configSet.put(R556_FLECHETTE, Gun556mmFactory.get556FlechetteConfig());
        configSet.put(R556_FLECHETTE_INCENDIARY, Gun556mmFactory.get556FlechetteIncendiaryConfig());
        configSet.put(R556_FLECHETTE_PHOSPHORUS, Gun556mmFactory.get556FlechettePhosphorusConfig());
        configSet.put(R556_FLECHETTE_DU, Gun556mmFactory.get556FlechetteDUConfig());
        configSet.put(R556_FLECHETTE_SLEEK, Gun556mmFactory.get556FlechetteSleekConfig());
        configSet.put(R556_K, Gun556mmFactory.get556KConfig());
        configSet.put(CHL_R556, Gun556mmFactory.get556Config().setToHoming(LegacyGuns.AMMO_556_CHLOROPHYTE));
        configSet.put(CHL_R556_FLECHETTE, Gun556mmFactory.get556FlechetteConfig().setToHoming(LegacyGuns.AMMO_556_FLECHETTE_CHLOROPHYTE));


        configSet.put(R5_NORMAL, Gun5mmFactory.get5mmConfig());
        configSet.put(R5_EXPLOSIVE, Gun5mmFactory.get5mmExplosiveConfig());
        configSet.put(R5_DU, Gun5mmFactory.get5mmDUConfig());
        configSet.put(R5_STAR, Gun5mmFactory.get5mmStarConfig());
        configSet.put(R5_NORMAL_BOLT, Gun5mmFactory.get5mmConfig().setToBolt(BulletConfiguration.BOLT_LACUNAE));
        configSet.put(R5_EXPLOSIVE_BOLT, Gun5mmFactory.get5mmExplosiveConfig().setToBolt(BulletConfiguration.BOLT_LACUNAE));
        configSet.put(R5_DU_BOLT, Gun5mmFactory.get5mmDUConfig().setToBolt(BulletConfiguration.BOLT_LACUNAE));
        configSet.put(R5_STAR_BOLT, Gun5mmFactory.get5mmStarConfig().setToBolt(BulletConfiguration.BOLT_LACUNAE));
        configSet.put(CHL_R5, Gun5mmFactory.get5mmConfig().setToHoming(LegacyGuns.AMMO_5MM_CHLOROPHYTE));
        configSet.put(CHL_R5_BOLT, Gun5mmFactory.get5mmConfig().setToBolt(BulletConfiguration.BOLT_LACUNAE).setToHoming(LegacyGuns.AMMO_5MM_CHLOROPHYTE));


        configSet.put(B75_NORMAL, Gun75BoltFactory.get75BoltConfig());
        configSet.put(B75_INCENDIARY, Gun75BoltFactory.get75BoltIncConfig());
        configSet.put(B75_HE, Gun75BoltFactory.get75BoltHEConfig());


        configSet.put(R762_NORMAL, Gun762mmFactory.get762NATOConfig());
        configSet.put(R762_PHOSPHORUS, Gun762mmFactory.get762WPConfig());
        configSet.put(R762_AP, Gun762mmFactory.get762APConfig());
        configSet.put(R762_DU, Gun762mmFactory.get762DUConfig());
        configSet.put(R762_TRACER, Gun762mmFactory.get762TracerConfig());
        configSet.put(R762_K, Gun762mmFactory.get762BlankConfig());


        configSet.put(P9_NORMAL, Gun9mmFactory.get9mmConfig());
        configSet.put(P9_AP, Gun9mmFactory.get9mmAPConfig());
        configSet.put(P9_DU, Gun9mmFactory.get9mmDUConfig());
        configSet.put(P9_ROCKET, Gun9mmFactory.get9mmRocketConfig());
        configSet.put(CHL_P9, Gun9mmFactory.get9mmConfig().setToHoming(LegacyGuns.AMMO_9MM_CHLOROPHYTE));


        configSet.put(ROCKET_NORMAL, GunRocketFactory.getRocketConfig());
        configSet.put(ROCKET_HE, GunRocketFactory.getRocketHEConfig());
        configSet.put(ROCKET_INCENDIARY, GunRocketFactory.getRocketIncendiaryConfig());
        configSet.put(ROCKET_PHOSPHORUS, GunRocketFactory.getRocketPhosphorusConfig());
        configSet.put(ROCKET_SHRAPNEL, GunRocketFactory.getRocketShrapnelConfig());
        configSet.put(ROCKET_EMP, GunRocketFactory.getRocketEMPConfig());
        configSet.put(ROCKET_GLARE, GunRocketFactory.getRocketGlareConfig());
        configSet.put(ROCKET_SLEEK, GunRocketFactory.getRocketSleekConfig());
        configSet.put(ROCKET_NUKE, GunRocketFactory.getRocketNukeConfig());
        configSet.put(ROCKET_CHAINSAW, GunRocketFactory.getRocketRPCConfig());
        configSet.put(ROCKET_TOXIC, GunRocketFactory.getRocketChlorineConfig());
        configSet.put(ROCKET_CANISTER, GunRocketFactory.getRocketCanisterConfig());
        configSet.put(ROCKET_ERROR, GunRocketFactory.getRocketErrorConfig());
        configSet.put(ROCKET_NORMAL_LASER, GunRocketFactory.getRocketConfig().setToGuided());
        configSet.put(ROCKET_HE_LASER, GunRocketFactory.getRocketHEConfig().setToGuided());
        configSet.put(ROCKET_INCENDIARY_LASER, GunRocketFactory.getRocketIncendiaryConfig().setToGuided());
        configSet.put(ROCKET_PHOSPHORUS_LASER, GunRocketFactory.getRocketPhosphorusConfig().setToGuided());
        configSet.put(ROCKET_SHRAPNEL_LASER, GunRocketFactory.getRocketShrapnelConfig().setToGuided());
        configSet.put(ROCKET_EMP_LASER, GunRocketFactory.getRocketEMPConfig().setToGuided());
        configSet.put(ROCKET_GLARE_LASER, GunRocketFactory.getRocketGlareConfig().setToGuided());
        configSet.put(ROCKET_SLEEK_LASER, GunRocketFactory.getRocketSleekConfig().setToGuided());
        configSet.put(ROCKET_NUKE_LASER, GunRocketFactory.getRocketNukeConfig().setToGuided());
        configSet.put(ROCKET_CHAINSAW_LASER, GunRocketFactory.getRocketRPCConfig().setToGuided());
        configSet.put(ROCKET_TOXIC_LASER, GunRocketFactory.getRocketChlorineConfig().setToGuided());

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
