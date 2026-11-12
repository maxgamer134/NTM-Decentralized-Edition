// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
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
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.atmosphere.AtmosphereBlob;
import com.hbm.space.atmosphere.SpaceAtmosphere;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.util.BobMathUtil;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/** The CO2 scrubber: takes the waste gas out of an air vent's room, and gives it back as CO2 for the pipes. */
public final class SpaceScrubber {

    private SpaceScrubber() {}

    public static final int[] DIMS = {1, 0, 0, 0, 0, 0};

    public static final RegistryHandle<Scrubber> BLOCK = Reg.blockItem("air_scrubber", Scrubber::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<ScrubberBE> BE = Reg.be("air_scrubber",
                    () -> new BlockEntityType<>(ScrubberBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("air_vent"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Scrubber extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock, ILookOverlay {
        public Scrubber(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ScrubberBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().fluidOut().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, DIMS);
            layout.port(0, 0, 0);
            layout.port(0, 1, 0);
            return layout.build(this, declaredCaps, DIMS, 0, 0);
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            BlockPos core = findClientCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof ScrubberBE scrubber)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            info.line(Component.literal("Power: " + BobMathUtil.getShortNumber(scrubber.getPower()) + "HE"),
                    scrubber.getPower() <= 200 ? 0xFF5555 : 0x55FF55);
            info.flow(false, Component.empty().append(NTMFluids.byName("CARBONDIOXIDE").getFluidType().getDescription())
                    .append(": " + scrubber.tank.getFill() + "/" + scrubber.tank.getMaxFill() + "mB").getString());
        }
    }

    public static class ScrubberBE extends BlockEntityMachineBase
            implements IEnergyHandlerMK2, FluidFlushSender, FluidTankAccess, SyncUnitSchema {
        public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.byName("CARBONDIOXIDE"), 16_000);
        private final FluidTankNTM[] sending = {tank};
        private final FluidFlushOutputs flush = new FluidFlushOutputs();
        private long power;
        private SpaceVents.@Nullable AirVentBE vent;
        public float rot, prevRot;
        private float rotSpeed;

        public ScrubberBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 0);
        }

        public boolean canOperate() {
            return power > 200;
        }

        /** Takes waste gas from the room, as CO2, for 10 HE a millibucket; the amount taken. */
        public int scrub(int amount) {
            if (!canOperate()) return 0;
            int add = Math.min(tank.getMaxFill() - tank.getFill(), amount);
            tank.setFill(tank.getFill() + add);
            power -= add * 10L;
            return add;
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            if (canOperate() && server.getGameTime() % 5 == 0
                    && (vent == null || vent.isRemoved() || vent.getFluidPressure() == 0 || !vent.registerScrubber(this))) {
                vent = null;
                for (AtmosphereBlob blob : SpaceAtmosphere.getBlobs(server, worldPosition)) {
                    if (blob != null && server.getBlockEntity(blob.getRootPosition()) instanceof SpaceVents.AirVentBE found) {
                        if (found.registerScrubber(this)) {
                            vent = found;
                            break;
                        }
                    }
                }
            }
            flush.provide(server, this);
            networkPackNT(20);
        }

        @Override
        public void tickClient() {
            rotSpeed = canOperate() ? Math.min(30F, rotSpeed + 0.2F) : Math.max(0F, rotSpeed - 0.1F);
            prevRot = rot;
            rot += rotSpeed;
            if (rot >= 360F) {
                rot -= 360F;
                prevRot -= 360F;
            }
        }

        // power

        @Override
        public long getPower() {
            return power;
        }

        @Override
        public void setPower(long p) {
            power = Math.max(0L, Math.min(p, getMaxPower()));
        }

        @Override
        public long getMaxPower() {
            return 10_000L;
        }

        // fluids

        @Override
        public FluidTankNTM[] getSendingTanks() {
            return sending;
        }

        @Override
        public void declareFlush(FlushLanes out) {
            for (FluidTankNTM t : getSendingTanks()) out.add(t, FlushFaces.own());
        }

        @Override
        public long getFluidAvailable(Fluid type, int pressure) {
            if (!tank.provides(type) || pressure != tank.getPressure()) return 0L;
            return tank.getFill();
        }

        @Override
        public void useUpFluid(Fluid type, int pressure, long amount) {
            if (!tank.provides(type) || pressure != tank.getPressure()) return;
            if (tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
        }

        @Override
        public int tankCount() {
            return 1;
        }

        @Override
        public FluidTankNTM tank(int index) {
            return tank;
        }

        @Override
        public void forEachTank(Consumer<FluidTankNTM> visitor) {
            visitor.accept(tank);
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
            tank.packetSerialize(output);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            power = input.readLong();
            tank.packetDeserialize(input);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            power = input.getLongOr("power", 0L);
            input.child("tank").ifPresent(tank::deserialize);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("power", power);
            tank.serialize(output.child("tank"));
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.airScrubber");
        }
    }
}
