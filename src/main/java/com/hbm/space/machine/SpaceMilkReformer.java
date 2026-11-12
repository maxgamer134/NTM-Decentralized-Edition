// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
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
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.rocket.SpaceLayouts;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/** The dairy processing tank: separates milk into its three reformates for 10 kHE per 100 mB. */
public final class SpaceMilkReformer {

    private SpaceMilkReformer() {}

    public static final int[] DIMS = {6, 0, 1, 1, 1, 1};
    public static final long MAX_POWER = 1_000_000L;

    public static final RegistryHandle<Reformer> BLOCK = Reg.blockItem("machine_milk_reformer", Reformer::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<ReformerBE> BE = Reg.be("machine_milk_reformer",
                    () -> new BlockEntityType<>(ReformerBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);
    public static final RegistryHandle<MenuType<Menu>> MENU = NeoForgeRegistrar.registerBlockEntityMenu("machine_milk_reformer", ReformerBE.class);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_radiator"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Reformer extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {
        public Reformer(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ReformerBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().fluidIn().fluidOut().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, DIMS);
            layout.port(1, 0, 1);
            layout.port(1, 0, -1);
            layout.port(-1, 0, 1);
            layout.port(-1, 0, -1);
            return layout.build(this, declaredCaps, DIMS, 1, 0);
        }
    }

    public static class ReformerBE extends BlockEntityMachineBase
            implements IGUIProvider, IEnergyHandlerMK2, FluidFlushSender, FluidTankAccess, SyncUnitSchema {
        public static final int SLOT_BATTERY = 0;
        public final FluidTankNTM[] tanks = {
            new FluidTankNTM(NTMFluids.byName("MILK"), 64_000),
            new FluidTankNTM(NTMFluids.byName("EMILK"), 32_000),
            new FluidTankNTM(NTMFluids.byName("CMILK"), 32_000),
            new FluidTankNTM(NTMFluids.byName("CREAM"), 32_000)
        };
        private final FluidTankNTM[] sending = {tanks[1], tanks[2], tanks[3]};
        private final FluidFlushOutputs flush = new FluidFlushOutputs();
        public long power;

        public ReformerBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 9);
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
            boolean changed = tanks[0].loadTank(1, 2, inventory);
            changed |= refine();
            changed |= tanks[1].unloadTank(3, 4, inventory);
            changed |= tanks[2].unloadTank(5, 6, inventory);
            changed |= tanks[3].unloadTank(7, 8, inventory);
            if (changed) setChanged();
            flush.provide(server, this);
            networkPackNT(150);
        }

        private boolean refine() {
            if (power < 10_000 || tanks[0].getFill() < 100) return false;
            if (tanks[1].getFill() + 50 > tanks[1].getMaxFill()) return false;
            if (tanks[2].getFill() + 35 > tanks[2].getMaxFill()) return false;
            if (tanks[3].getFill() + 15 > tanks[3].getMaxFill()) return false;
            power -= 10_000;
            tanks[0].setFill(tanks[0].getFill() - 100);
            tanks[1].setFill(tanks[1].getFill() + 50);
            tanks[2].setFill(tanks[2].getFill() + 35);
            tanks[3].setFill(tanks[3].getFill() + 15);
            return true;
        }

        // power

        @Override
        public long getPower() {
            return power;
        }

        @Override
        public void setPower(long p) {
            power = Math.max(0L, Math.min(p, MAX_POWER));
        }

        @Override
        public long getMaxPower() {
            return MAX_POWER;
        }

        // fluids

        @Override
        public boolean canReceiveFluid(Fluid type) {
            return tanks[0].joins(type);
        }

        @Override
        public long getDemand(Fluid type, int pressure) {
            return pressure == 0 && tanks[0].accepts(type) ? tanks[0].getMaxFill() - tanks[0].getFill() : 0;
        }

        @Override
        public long transferFluid(Fluid type, int pressure, long amount) {
            if (pressure != 0 || !tanks[0].accepts(type)) return amount;
            long left = amount - tanks[0].fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
            if (left != amount) setChanged();
            return left;
        }

        @Override
        public FluidTankNTM[] getSendingTanks() {
            return sending;
        }

        @Override
        public void declareFlush(FlushLanes out) {
            for (FluidTankNTM tank : sending) out.add(tank, FlushFaces.own());
        }

        @Override
        public long getFluidAvailable(Fluid type, int pressure) {
            long total = 0;
            for (FluidTankNTM tank : sending) if (tank.provides(type) && pressure == tank.getPressure()) total += tank.getFill();
            return total;
        }

        @Override
        public void useUpFluid(Fluid type, int pressure, long amount) {
            for (FluidTankNTM tank : sending) {
                if (amount <= 0) break;
                if (!tank.provides(type) || pressure != tank.getPressure()) continue;
                int drained = tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
                amount -= drained;
                if (drained > 0) setChanged();
            }
        }

        @Override
        public void forEachTank(Consumer<FluidTankNTM> visitor) {
            for (FluidTankNTM tank : tanks) visitor.accept(tank);
        }

        @Override
        public int tankCount() {
            return tanks.length;
        }

        @Override
        public FluidTankNTM tank(int index) {
            return tanks[index];
        }

        // menu

        @Override
        public Component getDisplayName() {
            return getName();
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new Menu(containerId, playerInventory, this);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.milkreformer");
        }

        // sync and saving

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
            for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            power = input.readLong();
            for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            power = input.getLongOr("power", 0L);
            String[] keys = {"input", "m1", "m2", "m3"};
            for (int i = 0; i < tanks.length; i++) {
                final FluidTankNTM tank = tanks[i];
                input.child(keys[i]).ifPresent(tank::deserialize);
            }
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("power", power);
            String[] keys = {"input", "m1", "m2", "m3"};
            for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child(keys[i]));
        }
    }

    public static class Menu extends BlockEntityMenu<ReformerBE> {
        public Menu(int containerId, Inventory playerInv, ReformerBE be) {
            super(MENU.get(), containerId, be);
            checkContainerSize(be, 9);
            addSlot(new Slot(be, 0, 79, 8));
            int[][] pairs = {{45, 88}, {95, 88}, {122, 88}, {149, 88}};
            for (int i = 0; i < pairs.length; i++) {
                addSlot(new Slot(be, 1 + i * 2, pairs[i][0], pairs[i][1]));
                addSlot(new Slot(be, 2 + i * 2, pairs[i][0], pairs[i][1] + 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++)
                    addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 156 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInv, col, 8 + col * 18, 214));
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return quickMove(player, index, false, stack -> index < 9
                    ? moveItemStackTo(stack, 9, slots.size(), true)
                    : moveItemStackTo(stack, 0, 9, false));
        }
    }
}
