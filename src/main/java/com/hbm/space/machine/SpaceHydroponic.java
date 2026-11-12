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
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.SpaceCrops;
import com.hbm.space.atmosphere.SpaceAtmosphere;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/** The hydroponic bay: three plots under grow lights that turn carbon dioxide into oxygen as the crops grow. */
public final class SpaceHydroponic {

    private SpaceHydroponic() {}

    public static final int[] DIMS = {2, 0, 0, 0, 2, 2};
    public static final long MAX_POWER = 2_000;
    public static final int MAX_FERTILIZER = 200;
    public static final long LIGHT_POWER = 200;

    public static final RegistryHandle<Bay> BLOCK = Reg.blockItem("hydrobay", Bay::new,
            () -> BlockBehaviour.Properties.of().strength(3F, 3F).requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> 15));
    public static final Reg.BeHandle<BayBE> BE = Reg.be("hydrobay",
                    () -> new BlockEntityType<>(BayBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);
    public static final RegistryHandle<MenuType<Menu>> MENU = NeoForgeRegistrar.registerBlockEntityMenu("hydrobay", BayBE.class);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("gas_dock"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Bay extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {
        public Bay(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BayBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().fluidIn().fluidOut().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            // DIMS is only the base row; as NTM:Space's fillSpace the bay is three deep above it:
            // its front and back walls and roof, with the middle of the core's row left open for the plants
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, DIMS)
                    .box(0, 1, 0, 1, 0, 1, 1, 2, 2);
            for (int x = -1; x <= 1; x++) l.remove(x, 1, 0);
            l.port(2, 0, 0);
            l.port(-2, 0, 0);
            l.port(0, 2, 0);
            return l.build(this, declaredCaps, DIMS, 0, 0);
        }
    }

    public static class BayBE extends BlockEntityMachineBase
            implements IGUIProvider, IEnergyHandlerMK2, IFluidHandlerMK2, FluidFlushSender, FluidTankAccess, SyncUnitSchema {
        public static final int SLOT_SEED = 0;
        public static final int SLOT_FERTILIZER = 1;
        public static final int SLOT_BATTERY = 2;

        public final FluidTankNTM[] tanks = {
            new FluidTankNTM(NTMFluids.byName("CARBONDIOXIDE"), 16_000), new FluidTankNTM(NTMFluids.byName("OXYGEN"), 16_000)
        };
        private final FluidTankNTM[] sending = {tanks[1]};
        private final FluidFlushOutputs flush = new FluidFlushOutputs();
        public long power;
        public int fertilizer;
        private final int[] prevAge = new int[3];

        public BayBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 6);
        }

        public boolean lightsOn() {
            return power >= LIGHT_POWER;
        }

        public static int fertilizerStrength(ItemStack stack) {
            if (stack.isEmpty()) return 0;
            if (stack.is(Items.BONE_MEAL)) return 1;
            if (stack.is(ModItems.POWDER_FERTILIZER.get())) return 9;
            return 0;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == SLOT_SEED) return stack.getItem() instanceof BlockItem item && isPlantable(item.getBlock());
            if (slot == SLOT_FERTILIZER) return fertilizerStrength(stack) > 0;
            return slot == SLOT_BATTERY;
        }

        private static boolean isPlantable(Block block) {
            if (!(block instanceof CropBlock) || block instanceof StemBlock) return false;
            return !(block instanceof SpaceCrops.Crop crop) || crop.hydroponic;
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            if (power > 0) power = Math.max(power - 25, 0);

            ItemStack fert = getItem(SLOT_FERTILIZER);
            int strength = fertilizerStrength(fert);
            if (strength > 0 && fertilizer <= MAX_FERTILIZER - strength) {
                fert.shrink(1);
                fertilizer += strength;
                setChanged();
            }
            power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

            Direction rot = getBlockState().getValue(BlockMultiblockCore.FACING).getClockWise();
            for (int i = 0; i < 3; i++) {
                BlockPos plot = worldPosition.offset(rot.getStepX() * (i - 1), 1, rot.getStepZ() * (i - 1));
                tendPlot(server, plot, i);
            }

            flush.provide(server, this);
            networkPackNT(15);
        }

        private void tendPlot(ServerLevel server, BlockPos pos, int i) {
            BlockState current = server.getBlockState(pos);
            boolean growing = power >= LIGHT_POWER && tanks[0].getFill() >= 100;
            if (!growing) {
                if (current.getBlock() instanceof CropBlock crop && crop.getAge(current) != prevAge[i]) {
                    server.setBlock(pos, crop.getStateForAge(prevAge[i]), 2);
                }
                return;
            }

            if (!(current.getBlock() instanceof CropBlock)) {
                if (!current.isAir()) return;
                ItemStack seed = getItem(SLOT_SEED);
                if (seed.isEmpty() || !(seed.getItem() instanceof BlockItem item) || !isPlantable(item.getBlock())) return;
                current = item.getBlock().defaultBlockState();
                server.setBlock(pos, current, 2);
                prevAge[i] = 0;
                seed.shrink(1);
                setChanged();
            }

            CropBlock crop = (CropBlock) current.getBlock();
            if (server.random.nextInt(120) == 0) {
                current.randomTick(server, pos, server.random);
                current = server.getBlockState(pos);
            }

            boolean fullyGrown = crop.isMaxAge(current);
            if (!fullyGrown && fertilizer > 0 && server.random.nextInt(60) == 0) {
                if (crop.isBonemealSuccess(server, server.random, pos, current)) {
                    crop.performBonemeal(server, server.random, pos, current);
                    server.levelEvent(2005, pos, 0);
                }
                current = server.getBlockState(pos);
                fertilizer--;
                fullyGrown = crop.isMaxAge(current);
            }

            int age = crop.getAge(current);
            if (age != prevAge[i]) {
                int grown = Math.max(age - prevAge[i], 0) * SpaceAtmosphere.CROP_GROWTH_CONVERSION;
                tanks[0].setFill(Math.max(tanks[0].getFill() - grown, 0));
                tanks[1].setFill(Math.min(tanks[1].getFill() + grown, tanks[1].getMaxFill()));
                prevAge[i] = age;
                setChanged();
            }

            if (fullyGrown && harvest(Block.getDrops(current, server, pos, null))) {
                server.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                prevAge[i] = 0;
                setChanged();
            }
        }

        /** Puts the drops in the three output slots, or none of them if they will not all fit. */
        private boolean harvest(List<ItemStack> drops) {
            ItemStack[] original = new ItemStack[3];
            for (int i = 0; i < 3; i++) original[i] = getItem(3 + i).copy();
            for (ItemStack drop : drops) {
                if (!InventoryUtil.tryAddItemToInventory(this, 3, 5, drop.copy()).isEmpty()) {
                    for (int i = 0; i < 3; i++) setItem(3 + i, original[i]);
                    return false;
                }
            }
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
            out.add(tanks[1], FlushFaces.own());
        }

        @Override
        public long getFluidAvailable(Fluid type, int pressure) {
            return tanks[1].provides(type) && pressure == tanks[1].getPressure() ? tanks[1].getFill() : 0;
        }

        @Override
        public void useUpFluid(Fluid type, int pressure, long amount) {
            if (!tanks[1].provides(type) || pressure != tanks[1].getPressure()) return;
            if (tanks[1].drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
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
            return Component.translatable("container.hydrobay");
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
            output.writeInt(fertilizer);
            for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            power = input.readLong();
            fertilizer = input.readInt();
            for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            power = input.getLongOr("power", 0L);
            fertilizer = input.getIntOr("fertilizer", 0);
            for (int i = 0; i < 3; i++) prevAge[i] = input.getIntOr("p" + i, 0);
            String[] keys = {"co2", "o2"};
            for (int i = 0; i < tanks.length; i++) {
                final FluidTankNTM tank = tanks[i];
                input.child(keys[i]).ifPresent(tank::deserialize);
            }
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("power", power);
            output.putInt("fertilizer", fertilizer);
            for (int i = 0; i < 3; i++) output.putInt("p" + i, prevAge[i]);
            String[] keys = {"co2", "o2"};
            for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child(keys[i]));
        }
    }

    public static class Menu extends BlockEntityMenu<BayBE> {
        public Menu(int containerId, Inventory playerInv, BayBE be) {
            super(MENU.get(), containerId, be);
            checkContainerSize(be, 6);
            addSlot(new Slot(be, 0, 67, 18));
            addSlot(new Slot(be, 1, 67, 54));
            addSlot(new Slot(be, 2, 147, 54));
            for (int i = 0; i < 3; i++) {
                addSlot(new Slot(be, 3 + i, 111, 18 + i * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++)
                    addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInv, col, 8 + col * 18, 162));
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return quickMove(player, index, false, stack -> index < 6
                    ? moveItemStackTo(stack, 6, slots.size(), true)
                    : moveItemStackTo(stack, 0, 3, false));
        }
    }
}
