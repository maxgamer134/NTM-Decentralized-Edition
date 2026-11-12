// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.special.Autogen;
import com.hbm.items.special.BedrockOreKind;
import com.hbm.items.special.BedrockOreSample;
import com.hbm.items.special.BedrockOreSet;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreOutput;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreType;
import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Upstream's CelestialBedrockOre: every body has its own bedrock ore types, each with one primary,
 * sulfuric, solvent and radsolvent output. The processing recipes for these types are data in the
 * space pack (bedrock_ore_celestial), generated from the same table.
 */
public final class SpaceBedrockOres {

    private SpaceBedrockOres() {}

    public static final NTMMaterial MAT_ZINC =
            Mats.makeSmeltable(24_007, Mats.df("zinc"), 0xD7CBDA, 0x7A7277, 0xA79DA8).m();
    public static final NTMMaterial MAT_BROMINE =
            Mats.makeNonSmeltable(24_008, OreDictManager.BR, 0xFF642B, 0x720000, 0xFF642B).m();
    public static final NTMMaterial MAT_IODINE =
            Mats.makeNonSmeltable(24_009, OreDictManager.I, 0x7A8796, 0x3F3049, 0x7A8796).m();
    public static final NTMMaterial MAT_HAFNIUM =
            Mats.makeSmeltable(24_010, Mats.df("hafnium"), 0xFFF8C7, 0x2E1600, 0xFFF8C7).m();
    public static final NTMMaterial MAT_QUARTZ =
            Mats.makeNonSmeltable(24_011, OreDictManager.NETHERQUARTZ, 0xF7F5F2, 0x6F5D5A, 0xF7F5F2).n();
    public static final NTMMaterial MAT_LAPIS =
            Mats.makeNonSmeltable(24_012, OreDictManager.LAPIS, 0x567DF9, 0x001793, 0x264DC9).m();
    public static final NTMMaterial MAT_GLOWSTONE =
            Mats.makeNonSmeltable(24_013, Mats.df("glowstone"), 0xFFFF00, 0x535300, 0xFFFF00).n();
    public static final NTMMaterial MAT_PHOSPHORUS_W =
            Mats.makeNonSmeltable(24_014, OreDictManager.P_WHITE, 0xF5F5ED, 0xC4BD9A, 0xC4BD9A).n();
    public static final NTMMaterial MAT_AUSTRALIUM =
            Mats.makeSmeltable(24_015, OreDictManager.AUSTRALIUM, 0xFFFF00, 0x935B00, 0xFFFF00).m();
    public static final NTMMaterial MAT_TASMANITE =
            Mats.makeSmeltable(24_016, Mats.df("tasmanite"), 0xFFFF00, 0x935B00, 0xFFFF00).m();
    public static final NTMMaterial MAT_AYERITE =
            Mats.makeSmeltable(24_017, Mats.df("ayerite"), 0xFFFF00, 0x935B00, 0xFFFF00).m();
    public static final NTMMaterial MAT_SEMTEX =
            Mats.makeNonSmeltable(24_018, Mats.df("semtex"), 0xEDAA28, 0x825D16, 0xF0B090).n();

    /** One body's bedrock ore type, standing in for upstream's CelestialBedrockOreType. */
    public record CelestialKind(
            SolarSystem.Body body,
            String suffix,
            BedrockOreOutput primary,
            BedrockOreOutput byproductAcid,
            BedrockOreOutput byproductSolvent,
            BedrockOreOutput byproductRad,
            int light,
            int dark)
            implements BedrockOreKind {

        @Override
        public String id() {
            return body.name + "_" + suffix;
        }

        @Override
        public ResourceLocation channel() {
            return Library.id("bedrock_ore/" + id());
        }

        @Override
        public @Nullable Component nameSuffix() {
            return Component.literal(" (")
                    .append(Component.translatable("body." + body.name))
                    .append(")");
        }
    }

    public static final Map<SolarSystem.Body, BedrockOreSet> SETS = new EnumMap<>(SolarSystem.Body.class);
    public static final List<CelestialKind> KINDS = new ArrayList<>();

    private static BedrockOreOutput o(NTMMaterial mat, int amount) {
        return new BedrockOreOutput(mat, amount);
    }

    private static CelestialKind T(SolarSystem.Body body, String suffix, BedrockOreOutput primary, BedrockOreOutput acid, BedrockOreOutput solvent, BedrockOreOutput rad, int light, int dark) {
        return new CelestialKind(body, suffix, primary, acid, solvent, rad, light, dark);
    }

    private static void register(SolarSystem.Body body, CelestialKind... kinds) {
        for (CelestialKind kind : kinds) {
            BedrockOreType.KINDS.add(kind);
            KINDS.add(kind);
        }
        BedrockOreSet set =
                new BedrockOreSet(
                        "space_" + body.name,
                        Arrays.asList(kinds),
                        Component.translatable("body." + body.name));
        BedrockOreSet.SETS.add(set);
        SETS.put(body, set);
    }

    static {
        //					primary						sulfuric					solvent						dcm
        register(
                SolarSystem.Body.KERBIN,
                T(SolarSystem.Body.KERBIN, "light", o(Mats.MAT_IRON, 18), o(Mats.MAT_COPPER, 9), o(Mats.MAT_CRYOLITE, 6), o(Mats.MAT_SODIUM, 3), 0xE6D2B2, 0x564733),
                T(SolarSystem.Body.KERBIN, "heavy", o(Mats.MAT_TUNGSTEN, 18), o(Mats.MAT_TUNGSTEN, 9), o(SpaceBedrockOres.MAT_ZINC, 6), o(SpaceBedrockOres.MAT_ZINC, 3), 0xAEA8B0, 0x3D393B),
                T(SolarSystem.Body.KERBIN, "nonmetal", o(Mats.MAT_COAL, 18), o(Mats.MAT_LIGNITE, 9), o(Mats.MAT_SULFUR, 6), o(Mats.MAT_KNO, 3), 0x968966, 0x5F5330),
                T(SolarSystem.Body.KERBIN, "crystal", o(Mats.MAT_REDSTONE, 18), o(Mats.MAT_ASBESTOS, 9), o(Mats.MAT_DIAMOND, 6), o(Mats.MAT_EMERALD, 3), 0xDDBFAB, 0x3B4932));
        register(
                SolarSystem.Body.MUN,
                T(SolarSystem.Body.MUN, "light", o(Mats.MAT_LITHIUM, 18), o(Mats.MAT_IRON, 9), o(Mats.MAT_SODIUM, 6), o(Mats.MAT_CHLOROCALCITE, 3), 0xF2E9BF, 0x4D5A59),
                T(SolarSystem.Body.MUN, "heavy", o(Mats.MAT_LEAD, 18), o(SpaceBedrockOres.MAT_ZINC, 9), o(Mats.MAT_GOLD, 6), o(Mats.MAT_BISMUTH, 3), 0xCB9CC5, 0x7C3861),
                T(SolarSystem.Body.MUN, "rare", o(Mats.MAT_COBALT, 18), o(Mats.MAT_RAREEARTH, 9), o(Mats.MAT_NEODYMIUM, 6), o(Mats.MAT_STRONTIUM, 3), 0xD6D7C6, 0x2C2D26),
                T(SolarSystem.Body.MUN, "nonmetal", o(Mats.MAT_SULFUR, 18), o(Mats.MAT_FLUORITE, 9), o(Mats.MAT_KNO, 6), o(Mats.MAT_SILICON, 3), 0xE8E6CC, 0x877C61),
                T(SolarSystem.Body.MUN, "crystal", o(SpaceBedrockOres.MAT_QUARTZ, 18), o(SpaceBedrockOres.MAT_LAPIS, 9), o(Mats.MAT_EMERALD, 6), o(Mats.MAT_CINNABAR, 3), 0xB7B8CB, 0x423747));
        register(
                SolarSystem.Body.MINMUS,
                T(SolarSystem.Body.MINMUS, "light", o(Mats.MAT_COPPER, 18), o(Mats.MAT_ILMENITE, 9), o(Mats.MAT_CHLOROCALCITE, 6), o(Mats.MAT_COPPER, 3), 0xDDC07D, 0x492D1F),
                T(SolarSystem.Body.MINMUS, "heavy", o(Mats.MAT_LEAD, 18), o(Mats.MAT_GOLD, 9), o(Mats.MAT_TUNGSTEN, 6), o(Mats.MAT_BISMUTH, 3), 0xB78AB0, 0x5D1C43),
                T(SolarSystem.Body.MINMUS, "rare", o(Mats.MAT_ZIRCONIUM, 18), o(Mats.MAT_BORON, 9), o(Mats.MAT_COBALT, 6), o(Mats.MAT_STRONTIUM, 3), 0xD4D7CE, 0x302F2A),
                T(SolarSystem.Body.MINMUS, "nonmetal", o(Mats.MAT_SULFUR, 18), o(Mats.MAT_KNO, 9), o(Mats.MAT_FLUORITE, 6), o(Mats.MAT_SILICON, 3), 0xE8E6CC, 0x877C61),
                T(SolarSystem.Body.MINMUS, "crystal", o(Mats.MAT_EMERALD, 18), o(SpaceBedrockOres.MAT_LAPIS, 9), o(Mats.MAT_DIAMOND, 6), o(Mats.MAT_EMERALD, 3), 0xB2DEE8, 0x06413F));
        register(
                SolarSystem.Body.DUNA,
                T(SolarSystem.Body.DUNA, "light", o(Mats.MAT_IRON, 18), o(SpaceParts.MAT_NICKEL, 9), o(Mats.MAT_ILMENITE, 6), o(Mats.MAT_CHLOROCALCITE, 3), 0xD9CFAB, 0x484942),
                T(SolarSystem.Body.DUNA, "heavy", o(Mats.MAT_BERYLLIUM, 18), o(Mats.MAT_TUNGSTEN, 9), o(SpaceBedrockOres.MAT_ZINC, 6), o(Mats.MAT_BISMUTH, 3), 0xB080C1, 0x4E205E),
                T(SolarSystem.Body.DUNA, "rare", o(Mats.MAT_RAREEARTH, 18), o(Mats.MAT_BORON, 9), o(Mats.MAT_ZIRCONIUM, 6), o(Mats.MAT_STRONTIUM, 3), 0xD4D2C1, 0x313327),
                T(SolarSystem.Body.DUNA, "actinide", o(Mats.MAT_THORIUM, 18), o(Mats.MAT_RADIUM, 9), o(Mats.MAT_POLONIUM, 6), o(Mats.MAT_U233, 3), 0xC4B3A4, 0x4C413A),
                T(SolarSystem.Body.DUNA, "nonmetal", o(Mats.MAT_FLUORITE, 18), o(Mats.MAT_SULFUR, 9), o(Mats.MAT_SILICON, 6), o(Mats.MAT_PHOSPHORUS, 3), 0xE5B19C, 0x79563D),
                T(SolarSystem.Body.DUNA, "crystal", o(Mats.MAT_REDSTONE, 18), o(Mats.MAT_CINNABAR, 9), o(Mats.MAT_DIAMOND, 6), o(Mats.MAT_MOLYSITE, 3), 0xEC9F7D, 0x514928));
        register(
                SolarSystem.Body.MOHO,
                T(SolarSystem.Body.MOHO, "light", o(Mats.MAT_ILMENITE, 18), o(Mats.MAT_CHLOROCALCITE, 9), o(SpaceParts.MAT_NICKEL, 6), o(Mats.MAT_LITHIUM, 3), 0xD9CFAB, 0x5B5C55),
                T(SolarSystem.Body.MOHO, "heavy", o(Mats.MAT_GOLD, 18), o(SpaceBedrockOres.MAT_ZINC, 9), o(Mats.MAT_LEAD, 6), o(Mats.MAT_BISMUTH, 3), 0xCB9CC5, 0x7C3861),
                T(SolarSystem.Body.MOHO, "rare", o(Mats.MAT_NEODYMIUM, 18), o(Mats.MAT_ZIRCONIUM, 9), o(SpaceBedrockOres.MAT_BROMINE, 6), o(Mats.MAT_STRONTIUM, 3), 0xEEC396, 0x3C1C06),
                T(SolarSystem.Body.MOHO, "actinide", o(SpaceBedrockOres.MAT_AUSTRALIUM, 18), o(SpaceBedrockOres.MAT_AUSTRALIUM, 9), o(SpaceBedrockOres.MAT_TASMANITE, 6), o(SpaceBedrockOres.MAT_AYERITE, 3), 0xFFFF00, 0x935B00),
                T(SolarSystem.Body.MOHO, "nonmetal", o(SpaceBedrockOres.MAT_GLOWSTONE, 18), o(Mats.MAT_PHOSPHORUS, 9), o(Mats.MAT_SULFUR, 6), o(SpaceBedrockOres.MAT_PHOSPHORUS_W, 3), 0xEEB960, 0x8D6C30),
                T(SolarSystem.Body.MOHO, "crystal", o(Mats.MAT_CINNABAR, 18), o(Mats.MAT_REDSTONE, 9), o(SpaceBedrockOres.MAT_QUARTZ, 6), o(Mats.MAT_MOLYSITE, 3), 0xEA9D7A, 0x664224));
        register(
                SolarSystem.Body.DRES,
                T(SolarSystem.Body.DRES, "light", o(SpaceParts.MAT_NICKEL, 18), o(Mats.MAT_ILMENITE, 9), o(Mats.MAT_CADMIUM, 6), o(SpaceParts.MAT_GALLIUM, 3), 0xAFAEAA, 0x4B3F43),
                T(SolarSystem.Body.DRES, "heavy", o(SpaceBedrockOres.MAT_ZINC, 18), o(Mats.MAT_GOLD, 9), o(Mats.MAT_BISMUTH, 6), o(Mats.MAT_ARSENIC, 3), 0xBDA1C7, 0x844166),
                T(SolarSystem.Body.DRES, "rare", o(Mats.MAT_TANTALIUM, 18), o(Mats.MAT_LANTHANIUM, 9), o(Mats.MAT_NIOBIUM, 6), o(Mats.MAT_STRONTIUM, 3), 0xDBCDD8, 0x2B2E32),
                T(SolarSystem.Body.DRES, "actinide", o(Mats.MAT_URANIUM, 18), o(Mats.MAT_RADIUM, 9), o(Mats.MAT_TECHNETIUM, 6), o(Mats.MAT_U238, 3), 0xDEE2DD, 0x56635D),
                T(SolarSystem.Body.DRES, "nonmetal", o(Mats.MAT_SILICON, 18), o(Mats.MAT_SILICON, 9), o(Mats.MAT_FLUORITE, 6), o(Mats.MAT_FLUORITE, 3), 0xE8EBEF, 0x655D67),
                T(SolarSystem.Body.DRES, "crystal", o(Mats.MAT_DIAMOND, 18), o(Mats.MAT_BORAX, 9), o(Mats.MAT_MOLYSITE, 6), o(Mats.MAT_MOLYSITE, 3), 0xFCF4BD, 0x3C7123));
        register(
                SolarSystem.Body.EVE,
                T(SolarSystem.Body.EVE, "light", o(Mats.MAT_SODIUM, 18), o(Mats.MAT_CHLOROCALCITE, 9), o(Mats.MAT_IRON, 6), o(Mats.MAT_CO60, 3), 0xE2DDBB, 0x3A474E),
                T(SolarSystem.Body.EVE, "heavy", o(Mats.MAT_TUNGSTEN, 18), o(Mats.MAT_LEAD, 9), o(Mats.MAT_ARSENIC, 6), o(Mats.MAT_PB209, 3), 0x929CA1, 0x0E0A10),
                T(SolarSystem.Body.EVE, "rare", o(Mats.MAT_NIOBIUM, 18), o(Mats.MAT_STRONTIUM, 9), o(SpaceBedrockOres.MAT_IODINE, 6), o(Mats.MAT_AU198, 3), 0xC8B7A9, 0x553A22),
                T(SolarSystem.Body.EVE, "actinide", o(Mats.MAT_PLUTONIUM, 18), o(Mats.MAT_POLONIUM, 9), o(Mats.MAT_NEPTUNIUM, 6), o(Mats.MAT_PU239, 3), 0x9C9F97, 0x18160E),
                T(SolarSystem.Body.EVE, "schrabidic", o(Mats.MAT_SCHRABIDIUM, 18), o(Mats.MAT_SOLINIUM, 9), o(Mats.MAT_GHIORSIUM, 6), o(Mats.MAT_SCHRABIDIUM, 3), 0x7EF4EF, 0x0A4B3E),
                T(SolarSystem.Body.EVE, "crystal", o(Mats.MAT_SODALITE, 18), o(Mats.MAT_MOLYSITE, 9), o(Mats.MAT_DIAMOND, 6), o(Mats.MAT_BORAX, 3), 0xF4F3DB, 0x465F50));
        register(
                SolarSystem.Body.IKE,
                T(SolarSystem.Body.IKE, "light", o(Mats.MAT_COPPER, 18), o(Mats.MAT_BAUXITE, 9), o(SpaceParts.MAT_NICKEL, 6), o(Mats.MAT_SODIUM, 3), 0xEBC587, 0x72473C),
                T(SolarSystem.Body.IKE, "heavy", o(Mats.MAT_LEAD, 18), o(SpaceBedrockOres.MAT_ZINC, 9), o(Mats.MAT_GOLD, 6), o(Mats.MAT_ARSENIC, 3), 0xBACAB4, 0x58422A),
                T(SolarSystem.Body.IKE, "rare", o(Mats.MAT_BORON, 18), o(Mats.MAT_NEODYMIUM, 9), o(Mats.MAT_STRONTIUM, 6), o(Mats.MAT_LANTHANIUM, 3), 0xD7DDC8, 0x293024),
                T(SolarSystem.Body.IKE, "hazard", o(Mats.MAT_URANIUM, 18), o(Mats.MAT_U238, 9), o(Mats.MAT_PLUTONIUM, 6), o(Mats.MAT_TECHNETIUM, 3), 0xC5CCC6, 0x2F3A34));
        register(
                SolarSystem.Body.LAYTHE,
                T(SolarSystem.Body.LAYTHE, "light", o(Mats.MAT_ALUMINIUM, 18), o(Mats.MAT_ILMENITE, 9), o(SpaceParts.MAT_GALLIUM, 6), o(SpaceBedrockOres.MAT_HAFNIUM, 3), 0xB5B9B2, 0x34383B),
                T(SolarSystem.Body.LAYTHE, "heavy", o(Mats.MAT_BERYLLIUM, 18), o(Mats.MAT_TUNGSTEN, 9), o(Mats.MAT_LEAD, 6), o(Mats.MAT_ARSENIC, 3), 0x92A6A6, 0x0D0D0D),
                T(SolarSystem.Body.LAYTHE, "rare", o(Mats.MAT_RAREEARTH, 18), o(Mats.MAT_NEODYMIUM, 9), o(Mats.MAT_STRONTIUM, 6), o(Mats.MAT_NIOBIUM, 3), 0xD3BEBD, 0x2A2B22),
                T(SolarSystem.Body.LAYTHE, "actinide", o(Mats.MAT_URANIUM, 18), o(Mats.MAT_THORIUM, 9), o(Mats.MAT_POLONIUM, 6), o(Mats.MAT_U235, 3), 0xB5A594, 0x2B1E15),
                T(SolarSystem.Body.LAYTHE, "nonmetal", o(Mats.MAT_CHLOROCALCITE, 18), o(Mats.MAT_COAL, 9), o(Mats.MAT_FLUORITE, 6), o(Mats.MAT_SILICON, 3), 0xBFBC9D, 0x454646),
                T(SolarSystem.Body.LAYTHE, "crystal", o(Mats.MAT_ASBESTOS, 18), o(Mats.MAT_SODALITE, 9), o(Mats.MAT_DIAMOND, 6), o(Mats.MAT_SODALITE, 3), 0xE3E8EE, 0x434A8A));
        register(
                SolarSystem.Body.TEKTO,
                T(SolarSystem.Body.TEKTO, "light", o(Mats.MAT_TITANIUM, 18), o(Mats.MAT_COPPER, 9), o(SpaceParts.MAT_NICKEL, 6), o(Mats.MAT_LITHIUM, 3), 0xF6E3D0, 0x6D5851),
                T(SolarSystem.Body.TEKTO, "heavy", o(Mats.MAT_BERYLLIUM, 18), o(Mats.MAT_LEAD, 9), o(SpaceBedrockOres.MAT_ZINC, 6), o(SpaceBedrockOres.MAT_ZINC, 3), 0xC1BBC3, 0x413D40),
                T(SolarSystem.Body.TEKTO, "rare", o(Mats.MAT_BORON, 18), o(Mats.MAT_RAREEARTH, 9), o(Mats.MAT_TANTALIUM, 6), o(Mats.MAT_BISMUTH, 3), 0xCBA1E3, 0x4C256E),
                T(SolarSystem.Body.TEKTO, "actinide", o(Mats.MAT_URANIUM, 18), o(Mats.MAT_NEPTUNIUM, 9), o(Mats.MAT_RADIUM, 6), o(Mats.MAT_TECHNETIUM, 3), 0xD7DDD7, 0x4C5B54),
                T(SolarSystem.Body.TEKTO, "crystal", o(Mats.MAT_EMERALD, 18), o(Mats.MAT_SILICON, 9), o(Mats.MAT_MOLYSITE, 6), o(Mats.MAT_BORAX, 3), 0xE0EFCB, 0x334B18),
                T(SolarSystem.Body.TEKTO, "plastic", o(Mats.MAT_POLYMER, 18), o(Mats.MAT_RUBBER, 9), o(SpaceBedrockOres.MAT_SEMTEX, 6), o(Mats.MAT_PVC, 3), 0xA89673, 0x4D432F));

        // the fragments these outputs extract into, which main never autogens
        for (NTMMaterial mat : new NTMMaterial[] {
            Mats.MAT_ARSENIC,
            Mats.MAT_AU198,
            SpaceBedrockOres.MAT_AUSTRALIUM,
            SpaceBedrockOres.MAT_AYERITE,
            SpaceBedrockOres.MAT_BROMINE,
            Mats.MAT_CADMIUM,
            Mats.MAT_CO60,
            SpaceParts.MAT_GALLIUM,
            Mats.MAT_GHIORSIUM,
            SpaceBedrockOres.MAT_GLOWSTONE,
            SpaceBedrockOres.MAT_HAFNIUM,
            SpaceBedrockOres.MAT_IODINE,
            SpaceBedrockOres.MAT_LAPIS,
            Mats.MAT_NEPTUNIUM,
            SpaceParts.MAT_NICKEL,
            Mats.MAT_PB209,
            SpaceBedrockOres.MAT_PHOSPHORUS_W,
            Mats.MAT_PLUTONIUM,
            Mats.MAT_POLYMER,
            Mats.MAT_PU239,
            Mats.MAT_PVC,
            SpaceBedrockOres.MAT_QUARTZ,
            Mats.MAT_RUBBER,
            Mats.MAT_SCHRABIDIUM,
            SpaceBedrockOres.MAT_SEMTEX,
            Mats.MAT_SOLINIUM,
            SpaceBedrockOres.MAT_TASMANITE,
            Mats.MAT_U233,
            Mats.MAT_U235,
            SpaceBedrockOres.MAT_ZINC
        }) mat.setAutogen(MaterialShapes.FRAGMENT);
        Autogen.register();

        BedrockOreSet.OF.install(
                "hbm_space",
                level -> {
                    CelestialBody body = CelestialBody.getBodyOrNull(level.dimension());
                    if (body == null || body.type == null) return BedrockOreSet.NTM;
                    // Read on every call, so a config reload switches the set at once. Kerbin's set replaces
                    // NTM's overworld ores only while Space's changes apply (off under LBSM, like space_changes).
                    if (body.type == SolarSystem.Body.KERBIN
                            && !(com.hbm.space.SpaceToggle.changes() && !com.hbm.config.BalanceConfig.enableLBSM))
                        return BedrockOreSet.NTM;
                    BedrockOreSet set = SETS.get(body.type);
                    return set != null ? set : BedrockOreSet.NTM;
                });
        BedrockOreSet.SAMPLES.install(
                "hbm_space",
                () -> {
                    List<ItemStack> samples = new ArrayList<>();
                    for (BedrockOreSet set : SETS.values()) {
                        double[] amounts = new double[set.kinds().size()];
                        Arrays.fill(amounts, 1D);
                        ItemStack stack = new ItemStack(ModItems.BEDROCK_ORE_BASE.get());
                        stack.set(ModDataComponents.BEDROCK_ORE_SAMPLE.get(), new BedrockOreSample(set, amounts));
                        samples.add(stack);
                    }
                    return samples;
                });
    }
}
