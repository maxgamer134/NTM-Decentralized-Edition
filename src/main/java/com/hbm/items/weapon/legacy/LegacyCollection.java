// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import static com.hbm.items.weapon.legacy.BulletConfigSyncingUtil.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hbm.items.weapon.legacy.ItemAmmoLegacy.AmmoItemTrait;
import java.util.List;
import java.util.Set;

/** backport-fix: BF-161 main 1.0.27_X5000's lib/HbmCollection: the trait sets and the bullet config lists. */
public final class LegacyCollection {

    private LegacyCollection() {}

    public static final Set<AmmoItemTrait> APType = ImmutableSet.of(AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_WEAR);
    public static final Set<AmmoItemTrait> FlechetteType = ImmutableSet.of(AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.NEU_LESS_BOUNCY, AmmoItemTrait.CON_WEAR);
    public static final Set<AmmoItemTrait> IncendiaryType = ImmutableSet.of(AmmoItemTrait.PRO_INCENDIARY, AmmoItemTrait.CON_WEAR);
    public static final Set<AmmoItemTrait> PhosphorusType = ImmutableSet.of(AmmoItemTrait.PRO_PHOSPHORUS, AmmoItemTrait.NEU_WARCRIME1, AmmoItemTrait.CON_WEAR, AmmoItemTrait.CON_PENETRATION);
    public static final Set<AmmoItemTrait> PhosphorusTypeSpecial = ImmutableSet.of(AmmoItemTrait.PRO_PHOSPHORUS_SPLASH, AmmoItemTrait.NEU_WARCRIME1, AmmoItemTrait.CON_WEAR);
    public static final Set<AmmoItemTrait> ExplosiveType = ImmutableSet.of(AmmoItemTrait.PRO_EXPLOSIVE, AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.CON_HEAVY_WEAR);
    public static final Set<AmmoItemTrait> DUType = ImmutableSet.of(AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_HEAVY_METAL, AmmoItemTrait.CON_HEAVY_WEAR);
    public static final Set<AmmoItemTrait> StarmetalType = ImmutableSet.of(AmmoItemTrait.PRO_HEAVY_DAMAGE, AmmoItemTrait.NEU_STARMETAL, AmmoItemTrait.CON_HEAVY_WEAR);
    public static final Set<AmmoItemTrait> ChlorophyteType = ImmutableSet.of(AmmoItemTrait.PRO_DAMAGE, AmmoItemTrait.PRO_WEAR, AmmoItemTrait.PRO_PENETRATION, AmmoItemTrait.NEU_CHLOROPHYTE, AmmoItemTrait.NEU_HOMING, AmmoItemTrait.CON_SPEED);
    public static final List<Integer> g12 = ImmutableList.of(G12_NORMAL, G12_INCENDIARY, G12_SHRAPNEL, G12_DU, G12_AM, G12_SLEEK, G12_PERCUSSION);
    public static final List<Integer> g12hs = ImmutableList.of(G12HS_NORMAL, G12HS_INCENDIARY, G12HS_SHRAPNEL, G12HS_DU, G12HS_AM, G12HS_SLEEK, G12HS_PERCUSSION);
    public static final List<Integer> g20 = ImmutableList.of(G20_NORMAL, G20_SLUG, G20_FLECHETTE, G20_FIRE, G20_SHRAPNEL, G20_EXPLOSIVE, G20_CAUSTIC, G20_SHOCK, G20_WITHER, G20_SLEEK);
    public static final List<Integer> g4 = ImmutableList.of(G4_NORMAL, G4_SLUG, G4_FLECHETTE, G4_FLECHETTE_PHOSPHORUS, G4_EXPLOSIVE, G4_SEMTEX, G4_BALEFIRE, G4_KAMPF, G4_CANISTER, G4_CLAW, G4_VAMPIRE, G4_VOID, G4_TITAN, G4_SLEEK);
    public static final List<Integer> lr22 = ImmutableList.of(LR22_NORMAL, LR22_AP, CHL_LR22);
    public static final List<Integer> lr22Inc = ImmutableList.of(LR22_NORMAL_FIRE, LR22_AP_FIRE, CHL_LR22_FIRE);
    public static final List<Integer> m44Normal = ImmutableList.of(M44_NORMAL, M44_AP, M44_DU, M44_PHOSPHORUS, M44_STAR, CHL_M44, M44_ROCKET);
    public static final List<Integer> m44All = ImmutableList.of(M44_NORMAL, M44_AP, M44_DU, M44_PHOSPHORUS, M44_STAR, CHL_M44, M44_ROCKET, M44_PIP, M44_BJ, M44_SILVER);
    public static final List<Integer> ae50 = ImmutableList.of(AE50_NORMAL, AE50_AP, AE50_DU, AE50_STAR, CHL_AE50);
    public static final List<Integer> p9 = ImmutableList.of(P9_NORMAL, P9_AP, P9_DU, CHL_P9, P9_ROCKET);
    public static final List<Integer> acp45 = ImmutableList.of(ACP_45, ACP_45_AP, ACP_45_DU);
    public static final List<Integer> bmg50 = ImmutableList.of(BMG50_NORMAL, BMG50_INCENDIARY, BMG50_PHOSPHORUS, BMG50_EXPLOSIVE, BMG50_AP, BMG50_DU, BMG50_STAR, CHL_BMG50, BMG50_SLEEK);
    public static final List<Integer> bmg50Flechette = ImmutableList.of(BMG50_FLECHETTE_AM, BMG50_FLECHETTE_NORMAL, BMG50_FLECHETTE_PO);
    public static final List<Integer> r556 = ImmutableList.of(R556_NORMAL, R556_TRACER, R556_PHOSPHORUS, R556_AP, R556_DU, R556_STAR, CHL_R556, R556_SLEEK, R556_K, R556_GOLD);
    public static final List<Integer> r556Flechette = ImmutableList.of(R556_FLECHETTE, R556_FLECHETTE_INCENDIARY, R556_FLECHETTE_PHOSPHORUS, R556_FLECHETTE_DU, CHL_R556_FLECHETTE, R556_FLECHETTE_SLEEK, R556_K);
    public static final List<Integer> r762 = ImmutableList.of(R762_NORMAL, R762_PHOSPHORUS, R762_AP, R762_DU, R762_TRACER, R762_K);
    public static final List<Integer> r5 = ImmutableList.of(R5_NORMAL, R5_EXPLOSIVE, R5_DU, R5_STAR, CHL_R5);
    public static final List<Integer> r5Bolt = ImmutableList.of(R5_NORMAL_BOLT, R5_EXPLOSIVE_BOLT, R5_DU_BOLT, R5_STAR_BOLT, CHL_R5_BOLT);
    public static final List<Integer> b75 = ImmutableList.of(B75_NORMAL, B75_INCENDIARY, B75_HE);
    public static final List<Integer> cannon = ImmutableList.of(SHELL_NORMAL, SHELL_EXPLOSIVE, SHELL_AP, SHELL_DU, SHELL_W9);
    public static final List<Integer> flamer = ImmutableList.of(FLAMER_NORMAL, FLAMER_NAPALM, FLAMER_WP, FLAMER_VAPORIZER, FLAMER_GAS);
    public static final List<Integer> fatman = ImmutableList.of(NUKE_NORMAL, NUKE_LOW, NUKE_HIGH, NUKE_TOTS, NUKE_SAFE, NUKE_PUMPKIN, NUKE_BARREL);
    public static final List<Integer> fatmanMIRV = ImmutableList.of(NUKE_MIRV_NORMAL, NUKE_MIRV_LOW, NUKE_MIRV_HIGH, NUKE_MIRV_SAFE, NUKE_MIRV_SPECIAL);
    public static final List<Integer> grenade = ImmutableList.of(GRENADE_NORMAL, GRENADE_HE, GRENADE_INCENDIARY, GRENADE_PHOSPHORUS, GRENADE_CHEMICAL, GRENADE_CONCUSSION, GRENADE_FINNED, GRENADE_SLEEK, GRENADE_NUCLEAR, GRENADE_TRACER, GRENADE_KAMPF, GRENADE_LEADBURSTER);
    public static final List<Integer> rocket = ImmutableList.of(ROCKET_NORMAL, ROCKET_HE, ROCKET_INCENDIARY, ROCKET_PHOSPHORUS, ROCKET_SHRAPNEL, ROCKET_EMP, ROCKET_GLARE, ROCKET_TOXIC, ROCKET_CANISTER, ROCKET_SLEEK, ROCKET_NUKE, ROCKET_CHAINSAW);
}
