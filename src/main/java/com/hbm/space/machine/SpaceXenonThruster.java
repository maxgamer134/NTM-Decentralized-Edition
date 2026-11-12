// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Rocket;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.space.station.IPropulsion;
import com.hbm.util.BobMathUtil;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/** The xenon ion thruster: a station engine that burns xenon and a great deal of power for a long, slow burn. */
public final class SpaceXenonThruster {

    private SpaceXenonThruster() {}

    public static final int[] DIMS = {1, 1, 0, 1, 1, 1};
    public static final long MAX_POWER = 20_000_000L;
    private static final int POWER_COST_MULTIPLIER = 5_000;

    public static final RegistryHandle<Thruster> BLOCK = Reg.blockItem("machine_xenon_thruster", Thruster::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<ThrusterBE> BE = Reg.be("machine_xenon_thruster",
                    () -> new BlockEntityType<>(ThrusterBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_radiator"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Thruster extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock, ILookOverlay {
        public Thruster(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ThrusterBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().fluidIn().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, DIMS);
            layout.port(-1, 0, 0);
            layout.port(0, 0, 0);
            layout.port(1, 0, 0);
            return layout.build(this, declaredCaps, DIMS, 1, 1);
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            if (!CelestialBody.inOrbit(level)) return;
            BlockPos core = findClientCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof ThrusterBE thruster)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            if (!thruster.isFacingPrograde()) {
                info.line(Component.literal("! ! ! ").append(Component.translatable("atmosphere.engineFacing")).append(" ! ! !"),
                        BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
                return;
            }
            info.line(Component.literal(BobMathUtil.getShortNumber(thruster.power) + "HE"), thruster.power == 0 ? 0xFF5555 : 0x55FF55);
            FluidTankNTM tank = thruster.tank;
            info.input(Component.empty().append(tank.getFluid() == null ? Component.empty() : tank.getFluid().getFluidType().getDescription())
                    .append(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB"));
        }
    }

    public static class ThrusterBE extends BlockEntityMachineBase
            implements IPropulsion, IEnergyHandlerMK2, IFluidHandlerMK2, FluidTankAccess, SyncUnitSchema {
        public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.byName("XENON"), 4_000);
        public long power;
        private boolean isOn;
        private boolean registered;
        private int fuelCost;
        public float thrustAmount;

        public ThrusterBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 0);
        }

        /** The engine has to point along the station's direction of travel, which is west. */
        public boolean isFacingPrograde() {
            return getBlockState().getValue(BlockMultiblockCore.FACING) == Direction.WEST;
        }

        @Override
        public void tickServer() {
            if (level == null || !CelestialBody.inOrbit(level)) return;
            if (!registered) {
                if (isFacingPrograde()) registerPropulsion();
                registered = true;
                isOn = false;
            }
            networkPackNT(250);
        }

        @Override
        public void tickClient() {
            thrustAmount = Mth.clamp(thrustAmount + (isOn ? 0.01F : -0.01F), 0F, 1F);
        }

        @Override
        public void setRemoved() {
            if (registered && level != null) {
                if (isFacingPrograde()) unregisterPropulsion();
                registered = false;
            }
            super.setRemoved();
        }

        // IPropulsion

        @Override
        public BlockEntity getBlockEntity() {
            return this;
        }

        @Override
        public boolean canPerformBurn(int shipMass, double deltaV) {
            FT_Rocket trait = tank.getFluid() == null ? null : NTMFluidProperties.getTrait(tank.getFluid(), FT_Rocket.class);
            int isp = trait != null ? trait.getISP() : 300;
            fuelCost = SolarSystem.getFuelCost(deltaV, shipMass, isp);
            return power >= (long) fuelCost * POWER_COST_MULTIPLIER && tank.getFill() >= fuelCost;
        }

        @Override
        public void addErrors(List<Component> errors) {
            long needed = (long) fuelCost * POWER_COST_MULTIPLIER;
            if (power < needed) {
                errors.add(Component.literal(" - Insufficient power: needs " + BobMathUtil.getShortNumber(needed) + "HE").withStyle(ChatFormatting.RED));
            }
            if (tank.getFill() < fuelCost) {
                errors.add(Component.literal(" - Insufficient fuel: needs " + fuelCost + "mB").withStyle(ChatFormatting.RED));
            }
        }

        @Override
        public float getThrust() {
            return 1_400_000;
        }

        @Override
        public int startBurn() {
            isOn = true;
            power -= (long) fuelCost * POWER_COST_MULTIPLIER;
            tank.setFill(tank.getFill() - fuelCost);
            setChanged();
            return 200; // ten second prewarm
        }

        @Override
        public int endBurn() {
            isOn = false;
            return 200; // ten second recharge time
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
            return tank.joins(type);
        }

        @Override
        public void forEachTank(Consumer<FluidTankNTM> visitor) {
            visitor.accept(tank);
        }

        @Override
        public long getDemand(Fluid type, int pressure) {
            return pressure == 0 && tank.accepts(type) ? tank.getMaxFill() - tank.getFill() : 0;
        }

        @Override
        public long transferFluid(Fluid type, int pressure, long amount) {
            if (pressure != 0 || !tank.accepts(type)) return amount;
            long left = amount - tank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
            if (left != amount) setChanged();
            return left;
        }

        @Override
        public int tankCount() {
            return 1;
        }

        @Override
        public FluidTankNTM tank(int index) {
            return tank;
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
            output.writeBoolean(isOn);
            output.writeLong(power);
            output.writeInt(fuelCost);
            tank.packetSerialize(output);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            isOn = input.readBoolean();
            power = input.readLong();
            fuelCost = input.readInt();
            tank.packetDeserialize(input);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            isOn = input.getBooleanOr("on", false);
            power = input.getLongOr("power", 0L);
            input.child("tank").ifPresent(tank::deserialize);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putBoolean("on", isOn);
            output.putLong("power", power);
            tank.serialize(output.child("tank"));
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.xenonThruster");
        }
    }
}
