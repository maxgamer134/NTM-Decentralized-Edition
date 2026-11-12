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
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
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
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.recipes.CryoDistillRecipe;
import com.hbm.inventory.recipes.CryoDistillRecipes;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
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

/** The cryogenic distiller: cools a gas into its four fractions, as its cryo_distiller recipes say. */
public final class SpaceCryoDistiller {

    private SpaceCryoDistiller() {}

    public static final int[] DIMS = {3, 2, 3, 3, 2, 2};
    public static final long MAX_POWER = 1_000_000;

    public static final RegistryHandle<Distiller> BLOCK = Reg.blockItem("machine_cryo_distill", Distiller::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<DistillerBE> BE = Reg.be("machine_cryo_distill",
                    () -> new BlockEntityType<>(DistillerBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);
    public static final RegistryHandle<MenuType<Menu>> MENU = NeoForgeRegistrar.registerBlockEntityMenu("machine_cryo_distill", DistillerBE.class);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("hydrobay"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Distiller extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {
        public Distiller(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new DistillerBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().fluidIn().fluidOut().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            // DIMS only clears the space; the blocks follow the model, as NTM:Space's fillSpace:
            // the core's plane, the two sides of the walkway under it and the two tank stacks
            SpaceLayouts l = new SpaceLayouts()
                    .box(0, 0, 0, 0, 0, 3, 3, 2, 2)
                    .box(0, 0, 0, 0, 2, 3, 0, 2, 2)
                    .box(0, 0, 0, 0, 2, -2, 3, 2, 2)
                    .box(0, 0, 0, 2, 0, 3, -1, 2, 2)
                    .box(0, 0, 0, 3, 0, -2, 3, 2, 2);
            int noDown = MASK_ALL & ~MASK_DOWN;
            // the inputs sit at NTM:Space's -rot, rot being the facing turned clockwise: local +x
            l.port(2, -2, -1, noDown);
            l.port(2, -2, -2, noDown);
            for (int x : new int[] {-2, -1, 1, 2}) l.port(x, -2, 3, noDown);
            return l.build(this, declaredCaps, DIMS, 3, 2);
        }
    }

    public static class DistillerBE extends BlockEntityMachineBase
            implements IGUIProvider, IEnergyHandlerMK2, IFluidHandlerMK2, FluidFlushSender, FluidTankAccess, SyncUnitSchema {
        public final FluidTankNTM[] tanks = {
            new FluidTankNTM(NTMFluids.byName("EARTHAIR"), 64_000), new FluidTankNTM(null, 24_000), new FluidTankNTM(null, 24_000),
            new FluidTankNTM(null, 24_000), new FluidTankNTM(null, 24_000)
        };
        private final FluidTankNTM[] sending = {tanks[1], tanks[2], tanks[3], tanks[4]};
        private final FluidFlushOutputs flush = new FluidFlushOutputs();
        public long power;

        public DistillerBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 10);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot != 2 && slot != 4 && slot != 6 && slot != 9;
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            power += ItemEnergyTransfer.extract(this, 0, MAX_POWER - power, false);
            FluidIdentifierData id = getItem(7).get(ModDataComponents.FLUID_IDENTIFIER.get());
            if (id != null && id.primary() != null && id.primary() != tanks[0].getFluid() && CryoDistillRecipes.INSTANCE.getRecipeForInput(id.primary()) != null) {
                tanks[0].setTankType(id.primary());
            }
            boolean changed = distill();
            changed |= tanks[1].unloadTank(1, 2, inventory);
            changed |= tanks[2].unloadTank(3, 4, inventory);
            changed |= tanks[3].unloadTank(5, 6, inventory);
            changed |= tanks[4].unloadTank(8, 9, inventory);
            if (changed) setChanged();
            flush.provide(server, this);
            networkPackNT(15);
        }

        private boolean distill() {
            CryoDistillRecipe recipe = CryoDistillRecipes.INSTANCE.getRecipeForInput(tanks[0].getTankType());
            if (recipe == null) {
                for (int i = 1; i <= 4; i++) tanks[i].setTankType(null);
                return false;
            }
            FluidStackNTM in = recipe.inputFluid[0];
            for (int i = 0; i < 4; i++) tanks[i + 1].setTankType(recipe.outputFluid[i].type());
            if (power < recipe.power || tanks[0].getFill() < in.amount()) return false;
            for (int i = 0; i < 4; i++) {
                if (tanks[i + 1].getFill() + recipe.outputFluid[i].amount() > tanks[i + 1].getMaxFill()) return false;
            }
            tanks[0].setFill((int) (tanks[0].getFill() - in.amount()));
            for (int i = 0; i < 4; i++) tanks[i + 1].setFill((int) (tanks[i + 1].getFill() + recipe.outputFluid[i].amount()));
            power -= recipe.power;
            return true;
        }

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
            return Component.translatable("container.cryoDistillator");
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
            String[] keys = {"input", "o1", "o2", "o3", "o4"};
            for (int i = 0; i < tanks.length; i++) {
                final FluidTankNTM tank = tanks[i];
                input.child(keys[i]).ifPresent(tank::deserialize);
            }
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("power", power);
            String[] keys = {"input", "o1", "o2", "o3", "o4"};
            for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child(keys[i]));
        }
    }

    public static class Menu extends BlockEntityMenu<DistillerBE> {
        public Menu(int containerId, Inventory playerInv, DistillerBE be) {
            super(MENU.get(), containerId, be);
            checkContainerSize(be, 10);
            int[][] at = {{26, 90}, {80, 90}, {80, 108}, {98, 90}, {98, 108}, {116, 90}, {116, 108}, {26, 108}, {134, 90}, {134, 108}};
            for (int i = 0; i < at.length; i++) {
                final boolean out = i == 2 || i == 4 || i == 6 || i == 9;
                addSlot(new Slot(be, i, at[i][0], at[i][1]) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !out;
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
            return quickMove(player, index, false, stack -> index < 10
                    ? moveItemStackTo(stack, 10, slots.size(), true)
                    : moveItemStackTo(stack, 0, 10, false));
        }
    }
}
