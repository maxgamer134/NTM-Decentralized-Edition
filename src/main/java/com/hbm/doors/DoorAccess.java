// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from susy-ntmdoors (Kyron04): https://github.com/Kyron04/susy-ntmdoors

package com.hbm.doors;

import com.hbm.blockentity.BlockEntityDoorGeneric;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.platform.NeoForgeNetwork;
import com.hbm.packet.PayloadHandlerThread;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * Door access hardware from susy-ntmdoors: a keypad that locks a door behind a code, a keycard
 * scanner that turns it into a security door, and the keycards that open it.
 */
public final class DoorAccess {

    public static final RegistryHandle<SoundEvent> GRANTED = NeoForgeRegistrar.registerSound("door_access.granted");
    public static final RegistryHandle<SoundEvent> DENIED = NeoForgeRegistrar.registerSound("door_access.denied");
    public static final RegistryHandle<SoundEvent> PRESS = NeoForgeRegistrar.registerSound("door_access.press");

    public static final RegistryHandle<BlockDoorKeypad> KEYPAD =
            Reg.blockItem("door_keypad", BlockDoorKeypad::new, () -> device().strength(5.0F, 6.0F));
    public static final RegistryHandle<BlockKeycardScanner> KEYCARD_SCANNER =
            Reg.blockItem("keycard_scanner", BlockKeycardScanner::new, () -> device().strength(50.0F, 1200.0F));

    public static final RegistryHandle<BlockEntityType<BlockEntityDoorKeypad>> KEYPAD_BE =
            Reg.be("door_keypad", () -> new BlockEntityType<>(BlockEntityDoorKeypad::new, Set.of(KEYPAD.get()), null))
                    .ticksServer(BlockEntityDoorKeypad::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityKeycardScanner>> SCANNER_BE =
            Reg.be("keycard_scanner", () -> new BlockEntityType<>(BlockEntityKeycardScanner::new, Set.of(KEYCARD_SCANNER.get()), null))
                    .ticksServer(BlockEntityKeycardScanner::tickServer);

    /** susy-ntmdoors' cards. The creative keycard opens every scanner; the rest are programmed on one. */
    private static final String[] CARDS = {
        "blank_rfid_card", "red_rfid_card", "violet_rfid_card", "green_rfid_card", "blue_rfid_card",
        "black_rfid_card", "orange_rfid_card", "yellow_rfid_card", "keycard_blue_marking",
        "labyrinth_keycard", "engine_room_keycard", "terminal_keycard", "terragroup_labs_access_keycard",
        "operator_keycard", "googa_security_keycard", "scp_level_0_keycard", "scp_level_1_keycard",
        "scp_level_2_keycard", "scp_level_3_keycard", "scp_level_4_keycard", "scp_level_5_keycard",
        "level_1_access_keycard", "level_2_access_keycard", "level_3_access_keycard",
        "level_4_access_keycard", "level_5_access_keycard", "level_6_access_keycard",
        "level_7_access_keycard", "level_8_access_keycard", "level_9_access_keycard",
        "level_10_access_keycard", "level_11_access_keycard", "level_12_access_keycard"
    };

    public static final List<RegistryHandle<ItemKeycard>> KEYCARDS = new ArrayList<>();
    public static final RegistryHandle<ItemKeycard> CREATIVE_KEYCARD =
            Reg.item("creative_keycard", props -> new ItemKeycard(props, true), () -> new Item.Properties().stacksTo(1));

    static {
        for (String card : CARDS)
            KEYCARDS.add(Reg.item(card, props -> new ItemKeycard(props, false), () -> new Item.Properties().stacksTo(1)));
    }

    private DoorAccess() {}

    private static BlockBehaviour.Properties device() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .noOcclusion()
                .requiresCorrectToolForDrops();
    }

    /** Registers the keypad payload and the creative tab entries; called from common init. */
    public static void init() {
        NeoForgeNetwork.serverbound.add(
                new NeoForgeNetwork.Registration<>(
                        KeypadPayload.TYPE, KeypadPayload.STREAM_CODEC, KeypadPayload::handle, PayloadHandlerThread.MAIN));
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("cargo_door"), () -> new ItemStack(KEYPAD.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("cargo_door"), () -> new ItemStack(KEYCARD_SCANNER.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.CONSUMABLES, Library.id("padlock_reinforced"), () -> new ItemStack(CREATIVE_KEYCARD.get()));
        for (RegistryHandle<ItemKeycard> card : KEYCARDS)
            CreativeContents.insertAfter(CreativeContents.Tab.CONSUMABLES, Library.id("padlock_reinforced"), () -> new ItemStack(card.get()));
    }

    /** The door a wall device touches: any door cell on one of its six sides. */
    static @Nullable BlockEntityDoorGeneric touchingDoor(Level level, BlockPos device, @Nullable BlockPos remembered) {
        if (remembered != null && level.getBlockEntity(remembered) instanceof BlockEntityDoorGeneric door) return door;
        for (Direction dir : Direction.values()) {
            BlockPos side = device.relative(dir);
            BlockPos core = MultiblockSurface.coreOfAny(level, side);
            if (core != null && level.getBlockEntity(core) instanceof BlockEntityDoorGeneric door) return door;
        }
        return null;
    }

    /** The device's answer: a chime at the device and a line over the hotbar. */
    static void answer(Level level, BlockPos pos, @Nullable Player player, boolean ok, String key) {
        level.playSound(null, pos, (ok ? GRANTED : DENIED).get(), SoundSource.BLOCKS, 1F, 1F);
        if (player != null) player.displayClientMessage(Component.translatable("hbm.door_access." + key), true);
    }
}
