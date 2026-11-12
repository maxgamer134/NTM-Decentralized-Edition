// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.rocket;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.CelestialBody;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The drive processor: decodes a hard drive's flight plan so a launch pad will fly it, if its
 * processor circuit is a high enough tier for the trip, and copies a drive onto an empty one.
 */
public final class SpaceDriveProcessor {

    private SpaceDriveProcessor() {}

    public static final int[] DIMS = {0, 0, 0, 0, 0, 1};

    public static final RegistryHandle<Processor> BLOCK = Reg.blockItem("machine_drive_processor", Processor::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 6F).requiresCorrectToolForDrops().noOcclusion());

    public static final Reg.BeHandle<ProcessorBE> BE = Reg.be("machine_drive_processor",
                    () -> new BlockEntityType<>(ProcessorBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static final RegistryHandle<MenuType<Menu>> MENU = NeoForgeRegistrar.registerBlockEntityMenu("machine_drive_processor", ProcessorBE.class);

    /** The processor circuits, tier 1 to 3. */
    public static final List<RegistryHandle<Item>> CIRCUITS = List.of(
            Reg.item("circuit_processt1", Item::new, Item.Properties::new),
            Reg.item("circuit_processt2", Item::new, Item.Properties::new),
            Reg.item("circuit_processt3", Item::new, Item.Properties::new));

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("launch_pad_rocket"), () -> new ItemStack(BLOCK.get()));
        for (int i = CIRCUITS.size() - 1; i >= 0; i--) {
            RegistryHandle<Item> circuit = CIRCUITS.get(i);
            CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("machine_drive_processor"), () -> new ItemStack(circuit.get()));
        }
    }

    public static int tier(ItemStack circuit) {
        for (int i = 0; i < CIRCUITS.size(); i++) if (circuit.is(CIRCUITS.get(i).get())) return i + 1;
        return 0;
    }

    public static class Processor extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {
        public Processor(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ProcessorBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().items().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            return new SpaceLayouts().port(0, 0, 0).port(1, 0, 0).build(this, declaredCaps, DIMS, 0, 0);
        }
    }

    /** What the screen says under the buttons. */
    public enum Status {
        NONE(ChatFormatting.WHITE, ""),
        NO_POWER(ChatFormatting.RED, "No power"),
        LOW_TIER(ChatFormatting.RED, "Low tier"),
        PROCESSING(ChatFormatting.GREEN, "Processing"),
        DONE(ChatFormatting.GREEN, "Done!"),
        NO_TARGET(ChatFormatting.RED, "No target"),
        CLONED(ChatFormatting.GREEN, "Drive cloned");

        public final ChatFormatting color;
        public final String text;

        Status(ChatFormatting color, String text) {
            this.color = color;
            this.text = text;
        }

        public Component line() {
            Component c = Component.literal(text).withStyle(color);
            return this == PROCESSING ? c.copy().withStyle(ChatFormatting.ITALIC) : c;
        }
    }

    public static class ProcessorBE extends BlockEntityMachineBase implements IControlReceiver, IGUIProvider, SyncUnitSchema, IEnergyHandlerMK2 {
        public static final int DRIVE = 0, TARGET = 1, CIRCUIT = 2, BATTERY = 3, SLOTS = 4;
        public static final long MAX_POWER = 2_000;
        public static final int MAX_PROGRESS = 100;

        public long power;
        public boolean isProcessing;
        public int progress;
        public Status status = Status.NONE;
        public boolean hasDrive;
        private int lastTier;

        public ProcessorBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, SLOTS);
        }

        private boolean drive() {
            return getItem(DRIVE).getItem() instanceof ItemVOTVdrive;
        }

        private int processingTier() {
            return tier(getItem(CIRCUIT));
        }

        private boolean enoughPower() {
            return power >= MAX_POWER * 3 / 4;
        }

        @Override
        public void tickServer() {
            power += ItemEnergyTransfer.extract(this, BATTERY, MAX_POWER - power, false);
            if (!enoughPower()) {
                isProcessing = false;
                status = Status.NO_POWER;
            } else if (!drive()) {
                isProcessing = false;
                status = Status.NONE;
            } else if (processingTier() < ItemVOTVdrive.getProcessingTier(getItem(DRIVE), CelestialBody.getBody(level))) {
                isProcessing = false;
                status = Status.LOW_TIER;
            }
            if (lastTier != processingTier()) status = Status.NONE;

            if (isProcessing) {
                power -= 200;
                status = Status.PROCESSING;
                if (++progress >= MAX_PROGRESS) {
                    progress = 0;
                    isProcessing = false;
                    ItemVOTVdrive.setProcessed(getItem(DRIVE), true);
                    status = Status.DONE;
                    setChanged();
                }
            } else {
                progress = 0;
            }
            lastTier = processingTier();
            hasDrive = drive();
            networkPackNT(15);
        }

        private void processDrive(boolean process) {
            if (!process) {
                isProcessing = false;
                return;
            }
            if (!enoughPower() || !drive() || ItemVOTVdrive.getProcessed(getItem(DRIVE))) return;
            if (processingTier() >= ItemVOTVdrive.getProcessingTier(getItem(DRIVE), CelestialBody.getBody(level))) isProcessing = true;
        }

        private void cloneDrive() {
            if (!enoughPower() || !drive()) return;
            if (!getItem(TARGET).is(SpaceRockets.HARD_DRIVE.get())) {
                status = Status.NO_TARGET;
                return;
            }
            ItemVOTVdrive.markCopied(getItem(DRIVE));
            setItem(TARGET, getItem(DRIVE).copy());
            status = Status.CLONED;
        }

        @Override
        public boolean hasPermission(Player player) {
            return stillValid(player);
        }

        @Override
        public void receiveControl(CompoundTag data) {
            if (data.contains("process")) processDrive(data.getBoolean("process"));
            if (data.contains("clone")) cloneDrive();
            setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public long getPower() {
            return power;
        }

        @Override
        public void setPower(long p) {
            power = Math.clamp(p, 0L, MAX_POWER);
        }

        @Override
        public long getMaxPower() {
            return MAX_POWER;
        }

        @Override
        public long syncUnitMask() {
            return super.syncUnitMask() | 1L;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            if (unit != 0) {
                super.writeSyncUnit(unit, output);
                return;
            }
            output.writeLong(power);
            output.writeBoolean(isProcessing);
            output.writeInt(progress);
            output.writeBoolean(hasDrive);
            output.writeByte(status.ordinal());
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            power = input.readLong();
            isProcessing = input.readBoolean();
            progress = input.readInt();
            hasDrive = input.readBoolean();
            int s = input.readByte();
            status = s >= 0 && s < Status.values().length ? Status.values()[s] : Status.NONE;
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            power = input.getLongOr("power", 0L);
            isProcessing = input.getBooleanOr("isProcessing", false);
            progress = input.getIntOr("progress", 0);
            lastTier = input.getIntOr("lastTier", 0);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("power", power);
            output.putBoolean("isProcessing", isProcessing);
            output.putInt("progress", progress);
            output.putInt("lastTier", lastTier);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.machineDriveProcessor");
        }

        @Override
        public Component getDisplayName() {
            return getName();
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new Menu(containerId, playerInventory, this);
        }
    }

    public static class Menu extends BlockEntityMenu<ProcessorBE> {
        public Menu(int containerId, Inventory playerInv, ProcessorBE be) {
            super(MENU.get(), containerId, be);
            checkContainerSize(be, ProcessorBE.SLOTS);
            addSlot(new Slot(be, ProcessorBE.DRIVE, 30, 18));
            addSlot(new Slot(be, ProcessorBE.TARGET, 50, 38));
            addSlot(new Slot(be, ProcessorBE.CIRCUIT, 81, 24));
            addSlot(new Slot(be, ProcessorBE.BATTERY, 134, 72));
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++)
                    addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 125 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInv, col, 8 + col * 18, 183));
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return quickMove(player, index, false, stack -> index < ProcessorBE.SLOTS
                    ? moveItemStackTo(stack, ProcessorBE.SLOTS, slots.size(), true)
                    : moveItemStackTo(stack, 0, ProcessorBE.SLOTS, false));
        }
    }
}
