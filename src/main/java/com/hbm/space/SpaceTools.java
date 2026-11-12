// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.backport.TooltipDisplay;
import com.hbm.backport.compat.ItemCompat;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.atmosphere.SpaceAtmosphere;
import com.hbm.space.trait.CBT_Atmosphere;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space's handheld tools and boot mods: the atmosphere scanner, the creative time wand, the
 * flippers and the occasionally heavy boots (a boot mod that can also be worn on its own).
 */
public final class SpaceTools {

    private SpaceTools() {}

    public static final RegistryHandle<Item> ATMOSPHERE_SCANNER =
            Reg.item("atmosphere_scanner", AtmosphereScanner::new, () -> new Item.Properties().stacksTo(1));
    public static final RegistryHandle<Item> WAND_TIME =
            Reg.item("wand_time", WandTime::new, () -> new Item.Properties().stacksTo(1));

    public static final RegistryHandle<Item> FLIPPERS =
            Reg.item("flippers", Flippers::new, Item.Properties::new);
    private static final com.hbm.backport.item.armor.ArmorMaterial HEAVY_BOOTS_MAT = com.hbm.items.ModItems.material(
            "heavy_boots", 0, 0, 0, 0, 0, 0, com.hbm.inventory.material.Mats.MAT_STEEL.dict.ingot());
    public static final RegistryHandle<Item> HEAVY_BOOTS = Reg.item("heavy_boots", HeavyBoots::new,
            () -> com.hbm.items.ModItems.humanoidArmorProps(HEAVY_BOOTS_MAT,
                    com.hbm.backport.item.armor.ArmorType.BOOTS, com.hbm.items.armor.ModArmorItem.Suit.NONE));

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.CONSUMABLES, Library.id("pads_slime"),
                () -> new ItemStack(HEAVY_BOOTS.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.CONSUMABLES, Library.id("pads_slime"),
                () -> new ItemStack(FLIPPERS.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.CONSUMABLES, Library.id("geiger_counter"),
                () -> new ItemStack(ATMOSPHERE_SCANNER.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.CONSUMABLES, Library.id("geiger_counter"),
                () -> new ItemStack(WAND_TIME.get()));
    }

    /** Common setup, once the items exist: the heavy boots also go into other boots' boot slot. */
    public static void commonSetup() {
        com.hbm.handler.ArmorModHandler.registerForeignMod(HEAVY_BOOTS.get(),
                1 << com.hbm.handler.ArmorModHandler.BOOTS_ONLY, EquipmentSlot.FEET);
    }

    /** Whether the feet carry this item, worn there or socketed into the boots' mod slot. */
    private static boolean onFeet(LivingEntity entity, Item item) {
        ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty()) return false;
        if (boots.is(item)) return true;
        return com.hbm.handler.ArmorModHandler.hasMods(boots)
                && com.hbm.handler.ArmorModHandler.modItem(boots, com.hbm.handler.ArmorModHandler.BOOTS_ONLY) == item;
    }

    /**
     * Runs every tick on both sides with the gravity tick. Player motion is the client's, so this
     * acts where the movement is predicted; on the server it only keeps the server's guess close.
     */
    public static void tickBoots(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        if (player.isInWater() && onFeet(player, FLIPPERS.get())) {
            // a full envsuit set already swims faster
            if (com.hbm.items.armor.ArmorSuitEffects.hasEnvsuitSwim(player)) return;
            player.setDeltaMovement(player.getDeltaMovement().add(player.getLookAngle().scale(0.08 * player.zza)));
        }
        // crouching in the air, extra gravity pulls you down until it matches the overworld's
        if (player.isShiftKeyDown() && !player.onGround() && !player.isInWater() && onFeet(player, HEAVY_BOOTS.get())) {
            float gravity = com.hbm.space.atmosphere.SpaceEnvironment.gravityOf(player);
            if (gravity > 1.5F || gravity == 0) return;
            if (gravity < 0.2F) gravity = 0.2F;
            Vec3 m = player.getDeltaMovement();
            double my = m.y / 0.98F + gravity / 20F - AstronomyUtil.STANDARD_GRAVITY / 20F;
            player.setDeltaMovement(m.x, my * 0.98F, m.z);
        }
    }

    public static class Flippers extends com.hbm.items.armor.ItemArmorMod {
        public Flippers(Properties props) {
            super(props, com.hbm.handler.ArmorModHandler.BOOTS_ONLY, false, false, false, true);
        }

        @Override
        protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
            adder.accept(Component.literal("Increases swim speed").withStyle(ChatFormatting.BLUE));
        }

        @Override
        public void addDescription(java.util.List<Component> tooltip, ItemStack stack, ItemStack armor) {
            tooltip.add(Component.literal("  ").append(stack.getHoverName()).append(" (increased swim speed)")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /** No protection of its own: worn alone, or socketed into other boots as their boot mod. */
    public static class HeavyBoots extends com.hbm.items.armor.ModArmorItem {
        public HeavyBoots(Properties props) {
            super(props, com.hbm.items.armor.ModArmorItem.Suit.NONE);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                Consumer<Component> adder, TooltipFlag flag) {
            adder.accept(Component.literal("Increases fall speed in low gravity").withStyle(ChatFormatting.BLUE));
            adder.accept(Component.literal("Activated by crouching").withStyle(ChatFormatting.BLUE));
            adder.accept(Component.empty());
            adder.accept(Component.translatable("armorMod.applicableTo").withStyle(ChatFormatting.DARK_PURPLE));
            adder.accept(Component.literal("  ").append(Component.translatable("armorMod.boots")));
            adder.accept(Component.translatable("desc.item.armorMod.slot").withStyle(ChatFormatting.DARK_PURPLE));
            adder.accept(Component.literal("  ").append(Component.translatable("armorMod.type.boots")));
            adder.accept(Component.literal("Can be worn on its own!").withStyle(ChatFormatting.GOLD));
            adder.accept(Component.literal("We take no responsibility for any deaths that may")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            adder.accept(Component.literal("occur while using these boots without a space suit.")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    /** Shows the partial pressure of every gas around its holder, or "near vacuum". */
    public static class AtmosphereScanner extends ItemCompat {
        public AtmosphereScanner(Properties props) {
            super(props);
        }

        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
            if (!(owner instanceof ServerPlayer player) || level.getGameTime() % 5 != 0) return;
            CBT_Atmosphere atmosphere = SpaceAtmosphere.getAtmosphere(owner);
            boolean any = false;
            if (atmosphere != null) {
                for (int i = 0; i < atmosphere.fluids.size(); i++) {
                    CBT_Atmosphere.FluidEntry entry = atmosphere.fluids.get(i);
                    if (entry.pressure <= 0.0001) continue;
                    Component line = Component.empty()
                            .append(entry.fluid.getFluidType().getDescription().copy().withStyle(ChatFormatting.AQUA))
                            .append(String.format(": %.4fatm", entry.pressure));
                    PacketWire.sendTo(new PlayerInformPayload(line, 969 + i, 4000), player);
                    any = true;
                }
            }
            if (!any) PacketWire.sendTo(new PlayerInformPayload(
                    Component.literal("NEAR VACUUM").withStyle(ChatFormatting.YELLOW), 969, 4000), player);
        }
    }

    /** Creative-only: doubles the celestial time multiplier, or halves it while sneaking. */
    public static class WandTime extends ItemCompat {
        public WandTime(Properties props) {
            super(props);
        }

        @Override
        public com.hbm.backport.InteractionResult backport$useOn(UseOnContext context) {
            Player player = context.getPlayer();
            if (context.getLevel().isClientSide() || player == null) return com.hbm.backport.InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) AstronomyUtil.TIME_MULTIPLIER = Math.max(1, AstronomyUtil.TIME_MULTIPLIER / 2);
            else AstronomyUtil.TIME_MULTIPLIER *= 2;
            player.sendSystemMessage(Component.literal("Celestial Time Multiplier set to: " + AstronomyUtil.TIME_MULTIPLIER));
            return com.hbm.backport.InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                Consumer<Component> adder, TooltipFlag flag) {
            adder.accept(Component.literal("Creative-only item"));
            adder.accept(Component.literal("\"Wibbly wobbly, timey-wimey... stuff\"").withStyle(ChatFormatting.ITALIC));
        }
    }
}
