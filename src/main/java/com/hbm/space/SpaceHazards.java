// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import net.minecraft.world.item.Item;

/** The radiation of NTM:Space's actinides and fuels, which would otherwise be as harmless as stone. */
public final class SpaceHazards {

    private SpaceHazards() {}

    private static final double CMRG = 6.0D;
    private static final double CMF = 2.2D;
    private static final double WST = HazardRegistry.wst;
    private static final double BILLET = HazardRegistry.mult_billet;
    private static final double ROD = HazardRegistry.mult_rod_rbmk;

    public static void init() {
        HazardSystem.addon(SpaceHazards::register);
    }

    private static void register() {
        metal("cm242", 9.3D);
        metal("cm243", 5.6D);
        metal("cm244", 2.0D);
        metal("cm245", 0.8D);
        metal("cm246", 2.5D);
        metal("cm247", 0.2D);
        metal("bk247", 10.5D);
        metal("cf251", 14.3D);
        metal("cf252", 15.3D);
        metal("es253", 18.3D);
        metal("es255", 19.3D);
        metal("cn989", 89.0D);
        metal("cm_mix", CMRG);
        metal("cm_fuel", CMF);
        plain("powder_cn989", 89.0D * HazardRegistry.mult_powder);
        plain("plate_cn989", 89.0D * HazardRegistry.mult_plate);

        HazardRegistry.addonRBMK(SpaceFuels.ROD_BK247, SpaceFuels.PELLET_BK247,
                10.5D * ROD, WST * ROD * 38D, 10.5D * BILLET, WST * BILLET * 37D);
        HazardRegistry.addonRBMK(SpaceFuels.ROD_LECM, SpaceFuels.PELLET_LECM,
                CMRG * 2.2D * ROD / 5D, WST * ROD, CMRG * BILLET, WST * 15.3D * 2.5D);
        HazardRegistry.addonRBMK(SpaceFuels.ROD_MECM, SpaceFuels.PELLET_MECM,
                CMRG * 2.2D * ROD / 3D, WST * ROD * 3D, CMRG * BILLET, WST * 15.3D * 2.6D);
        HazardRegistry.addonRBMK(SpaceFuels.ROD_HECM, SpaceFuels.PELLET_HECM,
                CMRG * 2.2D * ROD / 1.5D, WST * ROD * 5D, CMRG * BILLET, WST * 15.3D * 2.7D);
        HazardRegistry.addonRTG(SpaceFuels.RTG_CF251, 14.3D * HazardRegistry.mult_rtg, 0D, 0D, 0D);
        HazardRegistry.addonRTG(SpaceFuels.RTG_CF252, 15.3D * HazardRegistry.mult_rtg, 0D, 0D, 0D);

        watz(SpaceExtras.SpaceWatz.PU241, 25.0D);
        watz(SpaceExtras.SpaceWatz.AMRG, 9.0D);
        watz(SpaceExtras.SpaceWatz.CMRG, CMRG);
        watz(SpaceExtras.SpaceWatz.CMF, CMF);
        watz(SpaceExtras.SpaceWatz.BK247, 10.5D);
        watz(SpaceExtras.SpaceWatz.CF252, 15.3D);
        watz(SpaceExtras.SpaceWatz.ES253, 18.3D);
        // upstream registers the short-lived wastes of every class alike
        waste(SpaceExtras.WASTE_SHORT, () -> new com.hbm.hazard.HazardData().addEntry(HazardRegistry.RADIATION, 30D).addEntry(HazardRegistry.HOT, 5D));
        waste(SpaceExtras.WASTE_SHORT_TINY, () -> new com.hbm.hazard.HazardData().addEntry(HazardRegistry.RADIATION, 3D).addEntry(HazardRegistry.HOT, 5D));
        waste(SpaceExtras.WASTE_SHORT_DEPLETED, () -> HazardRegistry.makeData(HazardRegistry.RADIATION, 3D));
        waste(SpaceExtras.WASTE_SHORT_DEPLETED_TINY, () -> HazardRegistry.makeData(HazardRegistry.RADIATION, 0.3D));
        HazardSystem.register(SpaceExtras.RTG_DEPLETED_AMERICIUM.get(),
                HazardRegistry.makeData(HazardRegistry.RADIATION, 9.0D * HazardRegistry.mult_rtg));

        plainHazard("ammonium_nitrate", HazardRegistry.EXPLOSIVE, 4D);
        plainHazard("powder_wd2004", HazardRegistry.DIGAMMA, 1D);
        plainHazard("powder_wd2004_tiny", HazardRegistry.DIGAMMA, 0.05D);
        HazardSystem.register(com.hbm.items.ModItems.SAT_GERALD.get(), HazardRegistry.makeData(AUTISM, 1D));
        HazardSystem.register(SpaceExtras.BOBBLEHEAD_JUICE.get(), HazardRegistry.makeData(HazardRegistry.ASBESTOS, 1D));
        HazardSystem.register(com.hbm.blocks.ModBlocks.BOBBLEHEADS.get(com.hbm.blocks.generic.BlockBobble.BobbleType.MICROWAVE),
                HazardRegistry.makeData(GLITCH, 1D));
    }

    /** NTM:Space's AUTISM: confuses whoever holds it without eye protection. */
    public static final com.hbm.hazard.type.IHazardType AUTISM = new Confusing(false);
    /** NTM:Space's GLITCH: confuses too, and its tooltip names a random item. */
    public static final com.hbm.hazard.type.IHazardType GLITCH = new Confusing(true);

    private record Confusing(boolean glitch) implements com.hbm.hazard.type.IHazardType {
        @Override
        public void onUpdate(net.minecraft.world.entity.LivingEntity target, double level, net.minecraft.world.item.ItemStack stack) {
            if (com.hbm.data.RadiationData.DISABLE_BLINDING.get()) return;
            if (!com.hbm.util.ArmorRegistry.hasProtection(target, net.minecraft.world.entity.EquipmentSlot.HEAD, com.hbm.hazard.HazardClass.LIGHT))
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION,
                        (int) Math.ceil(level) * com.hbm.config.RadiationConfig.hazardRate, 0));
        }

        @Override
        public void updateEntity(net.minecraft.world.entity.item.ItemEntity item, double level) {}

        @Override
        public void addHazardInformation(net.minecraft.world.entity.player.Player player, java.util.List<net.minecraft.network.chat.Component> list,
                double level, net.minecraft.world.item.ItemStack stack, java.util.List<com.hbm.hazard.modifier.IHazardModifier> modifiers) {
            if (!glitch) {
                list.add(net.minecraft.network.chat.Component.literal("[").append(net.minecraft.network.chat.Component.translatable("trait.confusion"))
                        .append("]").withStyle(net.minecraft.ChatFormatting.GOLD));
                return;
            }
            if (player != null && player.getRandom().nextInt(10) == 0) {
                list.add(net.minecraft.network.chat.Component.literal("WORLD").withStyle(net.minecraft.ChatFormatting.DARK_RED));
                return;
            }
            var items = net.minecraft.core.registries.BuiltInRegistries.ITEM;
            int r = new java.util.Random(System.currentTimeMillis() / 500).nextInt(items.size() + 16);
            Item item = r < items.size() ? items.byId(r) : null;
            if (item != null) list.add(new net.minecraft.world.item.ItemStack(item).getHoverName());
            else list.add(net.minecraft.network.chat.Component.literal("STACKTRACE0x00" + r).withStyle(net.minecraft.ChatFormatting.RED));
        }
    }

    private static void plainHazard(String id, com.hbm.hazard.type.IHazardType type, double level) {
        for (var handle : SpaceParts.ITEMS)
            if (handle.id().getPath().equals(id)) HazardSystem.register(handle.get(), HazardRegistry.makeData(type, level));
    }

    private static void metal(String name, double base) {
        plain("ingot_" + name, base * HazardRegistry.mult_ingot);
        plain("nugget_" + name, base * HazardRegistry.mult_nugget);
        plain("billet_" + name, base * BILLET);
    }

    private static void watz(SpaceExtras.SpaceWatz type, double base) {
        HazardSystem.register(SpaceExtras.WATZ_PELLET.get(type),
                HazardRegistry.makeData(HazardRegistry.RADIATION, base * HazardRegistry.mult_ingot * 4D));
    }

    private static void plain(String id, double radiation) {
        for (var handle : SpaceParts.ITEMS) {
            if (handle.id().getPath().equals(id)) {
                Item item = handle.get();
                HazardSystem.register(item, HazardRegistry.makeData(HazardRegistry.RADIATION, radiation));
            }
        }
    }

    private static void waste(com.hbm.registration.ItemFamily<?, ? extends net.minecraft.world.item.Item> family, java.util.function.Supplier<com.hbm.hazard.HazardData> data) {
        for (var handle : family) HazardSystem.register(handle.get(), data.get());
    }
}
