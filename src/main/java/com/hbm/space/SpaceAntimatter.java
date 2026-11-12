// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.api.control.IControlReceiver;
import com.hbm.backport.Nbt;
import com.hbm.backport.Orientation;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.interfaces.IBomb;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.GunContent;
import com.hbm.lib.Library;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import java.awt.Color;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space's antimatter bomb: an antimatter pellet held between two lead particles and two
 * hafnium ingots. Lit by redstone or its timer, it digs a balefire-sized crater of its own
 * ({@link com.hbm.space.explosion.ExplosionAntimatter}) under a rainbow mushroom cloud.
 */
public final class SpaceAntimatter {

    private SpaceAntimatter() {}

    public static final int TORREX_TYPE = 2;

    /** NTM:Space's hafnium ingot, until the material system brings the rest of hafnium. */
    public static final RegistryHandle<Item> INGOT_HAFNIUM = Reg.item("ingot_hafnium", Item::new, Item.Properties::new);

    public static final RegistryHandle<Bomb> NUKE_ANTIMATTER = Reg.blockItem("nuke_antimatter", Bomb::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 6000F).noOcclusion().requiresCorrectToolForDrops());

    public static final Reg.BeHandle<BombBE> BE = Reg.be("nuke_antimatter",
                    () -> new BlockEntityType<>(BombBE::new, Set.of(NUKE_ANTIMATTER.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static final RegistryHandle<MenuType<Menu>> MENU = NeoForgeRegistrar.registerBlockEntityMenu("nuke_antimatter", BombBE.class);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.NUKES, Library.id("nuke_fstbmb"), () -> new ItemStack(NUKE_ANTIMATTER.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.PARTS, Library.id("ingot_zirconium"), () -> new ItemStack(INGOT_HAFNIUM.get()));
        // NTM:Space's antimatter cloud: every cloudlet a hue of its angle, the ring grey
        EntityNukeTorex.registerPalette(TORREX_TYPE, (cloud, col) -> {
            if (cloud.type == EntityNukeTorex.TorexType.RING) {
                cloud.colorR = cloud.colorG = cloud.colorB = Math.max(col, 0.25);
                return;
            }
            Color c = Color.getHSBColor(cloud.angle / 2F / (float) Math.PI, 1F, 1F);
            cloud.colorR = c.getRed() / 255D;
            cloud.colorG = c.getGreen() / 255D;
            cloud.colorB = c.getBlue() / 255D;
        });
    }

    public static class Bomb extends BlockMachineHorizontal implements IBomb {
        public Bomb(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BombBE(pos, state);
        }

        @Override
        protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                @Nullable Orientation orientation, boolean movedByPiston) {
            super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
            if (!level.isClientSide() && level.hasNeighborSignal(pos)) explode(level, pos, null);
        }

        @Override
        public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
            if (GunContent.isDisabled(asItem()) || level.isClientSide()) return BombReturnCode.UNDEFINED;
            if (!(level.getBlockEntity(pos) instanceof BombBE bomb)) return BombReturnCode.UNDEFINED;
            if (!bomb.isLoaded()) return BombReturnCode.ERROR_MISSING_COMPONENT;
            bomb.explode();
            return BombReturnCode.DETONATED;
        }
    }

    public static class BombBE extends BlockEntityMachineBase implements IControlReceiver, IGUIProvider, SyncUnitSchema {
        public static final int LEAD_A = 0, LEAD_B = 1, CORE = 2, HAFNIUM_A = 3, HAFNIUM_B = 4, SLOTS = 5;
        public static final int DEFAULT_TIMER = 18_000;

        @SyncField(units = 1L << 2)
        public boolean loaded;
        @SyncField(units = 1L << 1)
        public boolean started;
        @SyncField(units = 1L << 0)
        public int timer = DEFAULT_TIMER;
        /** The pellet the renderer floats over the bomb. */
        public boolean core;

        public BombBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, SLOTS);
        }

        @Override
        public void tickServer() {
            loaded = isLoaded();
            if (!loaded && started) {
                started = false;
                markChanged();
            }
            if (started) {
                timer--;
                markChanged();
                if (timer % 20 == 0)
                    level.playSound(null, worldPosition, ModSounds.FSTBMB_PING.get(), SoundSource.BLOCKS, 5F, 1F);
                if (timer <= 0) explode();
            }
            networkPackNT(250);
        }

        public boolean hasCore() {
            return level != null && level.isClientSide() ? core : getItem(CORE).is(ModItems.PELLET_ANTIMATTER.get());
        }

        public boolean isLoaded() {
            return hasCore() && getItem(LEAD_A).is(ModItems.PARTICLE_LEAD.get()) && getItem(LEAD_B).is(ModItems.PARTICLE_LEAD.get())
                    && getItem(HAFNIUM_A).is(INGOT_HAFNIUM.get()) && getItem(HAFNIUM_B).is(INGOT_HAFNIUM.get());
        }

        public void explode() {
            if (!(level instanceof ServerLevel server)) return;
            clearContent();
            server.destroyBlock(worldPosition, false);
            double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
            EntityBalefire blast = new EntityBalefire(ModEntities.BALEFIRE.get(), server).antimatter();
            blast.setPos(x, y, z);
            blast.destructionRange = 150;
            server.addFreshEntity(blast);
            // EntityNukeTorex.startFacAnti: a scale of 1000 clamped to the largest cloud
            EntityNukeTorex.statFac(server, x, y + 4.5, z, 500).setTorexType(TORREX_TYPE);
        }

        public String getMinutes() {
            return pad(timer / 1200);
        }

        public String getSeconds() {
            return pad((timer / 20) % 60);
        }

        private static String pad(int value) {
            return value < 10 ? "0" + value : Integer.toString(value);
        }

        @Override
        public boolean hasPermission(Player player) {
            return true;
        }

        @Override
        public void receiveControl(CompoundTag data) {
            if (data.contains("start") && isLoaded()) {
                level.playSound(null, worldPosition, ModSounds.FSTBMB_START.get(), SoundSource.BLOCKS, 5F, 1F);
                started = true;
            }
            if (data.contains("timer")) {
                int value = Nbt.getIntOr(data, "timer", timer / 20);
                if (value >= 1 && value <= 999) timer = value * 20;
            }
            setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public long syncUnitMask() {
            return super.syncUnitMask() | 15L;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            switch (unit) {
                case 0 -> output.writeInt(timer);
                case 1 -> output.writeBoolean(started);
                case 2 -> output.writeBoolean(loaded);
                case 3 -> output.writeBoolean(getItem(CORE).is(ModItems.PELLET_ANTIMATTER.get()));
                default -> super.writeSyncUnit(unit, output);
            }
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            switch (unit) {
                case 0 -> timer = input.readInt();
                case 1 -> started = input.readBoolean();
                case 2 -> loaded = input.readBoolean();
                case 3 -> core = input.readBoolean();
                default -> super.readSyncUnit(unit, input);
            }
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            started = input.getBooleanOr("started", false);
            timer = input.getIntOr("timer", DEFAULT_TIMER);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putBoolean("started", started);
            output.putInt("timer", timer);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.nukeAntimatter");
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

    public static class Menu extends BlockEntityMenu<BombBE> {
        public Menu(int containerId, Inventory playerInv, BombBE bomb) {
            super(MENU.get(), containerId, bomb);
            checkContainerSize(bomb, BombBE.SLOTS);
            slot(BombBE.LEAD_A, 26, 15, () -> ModItems.PARTICLE_LEAD.get());
            slot(BombBE.LEAD_B, 134, 14, () -> ModItems.PARTICLE_LEAD.get());
            slot(BombBE.CORE, 81, 15, () -> ModItems.PELLET_ANTIMATTER.get());
            slot(BombBE.HAFNIUM_A, 26, 44, INGOT_HAFNIUM);
            slot(BombBE.HAFNIUM_B, 134, 43, INGOT_HAFNIUM);
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++)
                    addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInv, col, 8 + col * 18, 198));
        }

        private void slot(int index, int x, int y, Supplier<? extends Item> item) {
            addSlot(new SlotFiltered(container(), index, x, y, stack -> stack.is(item.get())));
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return quickMove(player, index, false, stack -> index < BombBE.SLOTS
                    ? moveItemStackTo(stack, BombBE.SLOTS, slots.size(), true)
                    : moveItemStackTo(stack, 0, BombBE.SLOTS, false));
        }
    }
}
