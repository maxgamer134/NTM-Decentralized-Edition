// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

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
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.CelestialBody;
import com.hbm.space.rocket.SpaceLayouts;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/** The gas dock: a rocket that fetches a gas giant's atmosphere for hydrogen and oxygen, 8 B of gas for 0.5 B of each. */
public final class SpaceGasDock {

    private SpaceGasDock() {}

    public static final int[] DIMS = {0, 0, 1, 1, 1, 1};

    public static final RegistryHandle<Dock> BLOCK = Reg.blockItem("gas_dock", Dock::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<DockBE> BE = Reg.be("gas_dock",
                    () -> new BlockEntityType<>(DockBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("air_scrubber"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Dock extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {
        public Dock(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new DockBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).fluidIn().fluidOut();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, DIMS).height(12.0 / 16.0);
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) l.port(x, 0, z);
            return l.build(this, declaredCaps, DIMS, 0, 0);
        }
    }

    public static class DockBE extends BlockEntityMachineBase
            implements IFluidHandlerMK2, FluidFlushSender, FluidTankAccess, SyncUnitSchema {
        public final FluidTankNTM[] tanks = {
            new FluidTankNTM(null, 64_000), new FluidTankNTM(NTMFluids.byName("HYDROGEN"), 32_000), new FluidTankNTM(NTMFluids.byName("OXYGEN"), 32_000)
        };
        private final FluidTankNTM[] sending = {tanks[0]};
        private final FluidFlushOutputs flush = new FluidFlushOutputs();
        public boolean hasRocket = true;
        public int launchTicks;

        public DockBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 0);
        }

        private boolean hasFuel() {
            return tanks[1].getFill() >= 500 && tanks[2].getFill() >= 500;
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            CelestialBody planet = CelestialBody.getPlanet(server);
            launchTicks = Mth.clamp(launchTicks + (hasRocket ? -1 : 1), hasRocket ? -20 : 0, 100);
            if (planet.gas != null) {
                tanks[0].setTankType(planet.gas);
                if (hasFuel() && launchTicks <= -20) {
                    hasRocket = false;
                    collect();
                } else if (launchTicks >= 100) {
                    hasRocket = true;
                }
            }
            flush.provide(server, this);
            networkPackNT(150);
        }

        @Override
        public void tickClient() {
            launchTicks = Mth.clamp(launchTicks + (hasRocket ? -1 : 1), hasRocket ? -20 : 0, 100);
        }

        private void collect() {
            if (tanks[0].getFill() + 8000 > tanks[0].getMaxFill()) return;
            tanks[1].setFill(tanks[1].getFill() - 500);
            tanks[2].setFill(tanks[2].getFill() - 500);
            tanks[0].setFill(tanks[0].getFill() + 8000);
            setChanged();
        }

        @Override
        public boolean canReceiveFluid(Fluid type) {
            return tanks[1].joins(type) || tanks[2].joins(type);
        }

        @Override
        public long getDemand(Fluid type, int pressure) {
            if (pressure != 0) return 0;
            for (int i = 1; i <= 2; i++) if (tanks[i].accepts(type)) return tanks[i].getMaxFill() - tanks[i].getFill();
            return 0;
        }

        @Override
        public long transferFluid(Fluid type, int pressure, long amount) {
            if (pressure != 0) return amount;
            for (int i = 1; i <= 2; i++) {
                if (!tanks[i].accepts(type)) continue;
                long left = amount - tanks[i].fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
                if (left != amount) setChanged();
                return left;
            }
            return amount;
        }

        @Override
        public FluidTankNTM[] getSendingTanks() {
            return sending;
        }

        @Override
        public void declareFlush(FlushLanes out) {
            out.add(tanks[0], FlushFaces.own());
        }

        @Override
        public long getFluidAvailable(Fluid type, int pressure) {
            return tanks[0].provides(type) && pressure == tanks[0].getPressure() ? tanks[0].getFill() : 0;
        }

        @Override
        public void useUpFluid(Fluid type, int pressure, long amount) {
            if (!tanks[0].provides(type) || pressure != tanks[0].getPressure()) return;
            if (tanks[0].drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
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
        public long syncUnitMask() {
            return super.syncUnitMask() | 1L;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            if (unit != 0) {
                super.writeSyncUnit(unit, output);
                return;
            }
            output.writeBoolean(hasRocket);
            for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            hasRocket = input.readBoolean();
            for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            String[] keys = {"gas", "f1", "f2"};
            for (int i = 0; i < tanks.length; i++) {
                final FluidTankNTM tank = tanks[i];
                input.child(keys[i]).ifPresent(tank::deserialize);
            }
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            String[] keys = {"gas", "f1", "f2"};
            for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child(keys[i]));
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.gasDock");
        }
    }
}
