// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.backport.InteractionResult;
import com.hbm.backport.compat.BlockCompat;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ITickingBlock;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.atmosphere.AtmosphereBlob;
import com.hbm.space.atmosphere.IAtmosphereProvider;
import com.hbm.space.atmosphere.SpaceAtmosphere;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.util.BobMathUtil;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The air vent: fed oxygen (or any gas set with a fluid identifier), it fills the sealed room around
 * it, a millibucket a block, and lets the gas leak back out as the room is breathed. Only an oxygen
 * vent feeds a CO2 scrubber. The OpenComputers hooks are left out.
 */
public final class SpaceVents {

    private SpaceVents() {}

    public static final RegistryHandle<AirVent> VENT = Reg.blockItem("air_vent", AirVent::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops());
    public static final Reg.BeHandle<AirVentBE> VENT_BE = Reg.be("air_vent",
                    () -> new BlockEntityType<>(AirVentBE::new, Set.of(VENT.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_condenser_powered"), () -> new ItemStack(VENT.get()));
    }

    public static class AirVent extends BlockCompat implements ITickingBlock, ICapabilityBlock, ILookOverlay {
        public AirVent(Properties props) {
            super(props);
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new AirVentBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(VENT_BE).fluidIn();
        }

        @Override
        protected InteractionResult backport$useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                InteractionHand hand, BlockHitResult hit) {
            FluidIdentifierData data = held.get(ModDataComponents.FLUID_IDENTIFIER.get());
            if (data == null) return super.backport$useItemOn(held, state, level, pos, player, hand, hit);
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            Fluid type = data.primary();
            if (!(level.getBlockEntity(pos) instanceof AirVentBE vent) || type == null || type == Fluids.EMPTY) return InteractionResult.SUCCESS;
            if (NTMFluidProperties.hasTrait(type, FT_Gaseous.class) || NTMFluidProperties.hasTrait(type, FT_Gaseous_ART.class)) {
                vent.tank.setTankType(type);
                vent.setChanged();
                player.sendSystemMessage(Component.translatable("desc.shared.changedTypeTo").withStyle(ChatFormatting.YELLOW)
                        .append(NTMFluidProperties.getDisplayName(type)).append("!"));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            if (!(level.getBlockEntity(pos) instanceof AirVentBE vent)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            FluidTankNTM tank = vent.tank;
            info.line(Component.empty().append(tank.getFluid() == null ? Component.empty() : tank.getFluid().getFluidType().getDescription())
                    .append(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB"));
            int color = BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00;
            if (tank.getFill() <= 10) info.line(Component.literal("! ! ! ").append(Component.translatable("atmosphere.noTank")).append(" ! ! !"), color);
            else if (!vent.hasSeal()) info.line(Component.literal("! ! ! ").append(Component.translatable("atmosphere.noSeal")).append(" ! ! !"), color);
            info.line(Component.translatable("atmosphere.name").append(":"));
            boolean any = false;
            if (vent.atmosphere != null) {
                for (CBT_Atmosphere.FluidEntry entry : vent.atmosphere.fluids) {
                    if (entry.pressure <= 0.01D) continue;
                    info.line(Component.literal(" - ").append(entry.fluid.getFluidType().getDescription())
                            .append(" - " + Math.round(entry.pressure * 1000D) / 1000D + "atm"), 0x55FFFF);
                    any = true;
                }
            }
            if (!any) info.line(Component.literal(" - ").append(Component.translatable("atmosphere.vacuum")), 0x55FFFF);
            if (vent.bonusFill > 0) info.line(Component.translatable("atmosphere.recycling"), 0x55FF55);
        }
    }

    public static class AirVentBE extends BlockEntityMachineBase
            implements IAtmosphereProvider, IFluidHandlerMK2, FluidTankAccess, SyncUnitSchema {
        public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.byName("OXYGEN"), 16_000);
        public @Nullable CBT_Atmosphere atmosphere;
        private @Nullable AtmosphereBlob blob;
        private boolean registered;
        private int recovering;
        private int registerWait = 10;
        private int airFill;
        private int wasteFill;
        public int bonusFill;
        private boolean on;
        private SpaceScrubber.@Nullable ScrubberBE scrubber;

        public AirVentBE(BlockPos pos, BlockState state) {
            super(VENT_BE.get(), pos, state, 0);
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            if (registerWait > 0) registerWait--;
            if (tank.getFill() + bonusFill >= 20) {
                on = true;
                if (registerWait > 0) {
                    // let the chunks load first
                } else if (!registered) {
                    SpaceAtmosphere.registerAtmosphere(this);
                    registered = true;
                    if (airFill > 1) recovering = 100;
                } else if (recovering > 0) {
                    recovering--;
                    if (blob != null) recovering = 0;
                } else {
                    if (blob != null) {
                        int size = blob.getBlobSize();
                        if (size != 0) {
                            if (airFill > size) airFill = size;
                            if (wasteFill > size) wasteFill = size;
                            int toFill = Math.min(size - airFill, 20);
                            airFill += toFill;
                            if (toFill > 0) {
                                consumeFromTank(toFill);
                            } else if (server.random.nextBoolean()) {
                                consumeFromTank(1);
                                wasteFill++;
                            }
                            if (scrubber != null && !scrubber.isRemoved() && tank.getTankType() == NTMFluids.byName("OXYGEN")) {
                                wasteFill -= scrubber.scrub(Mth.clamp(wasteFill, 0, 20));
                            }
                        } else {
                            blob = null;
                        }
                    }
                    if (blob == null) { // venting to vacuum
                        consumeFromTank(20);
                        airFill = 0;
                        wasteFill = 0;
                    }
                }
            } else {
                on = false;
                if (registered) {
                    SpaceAtmosphere.unregisterAtmosphere(this);
                    registered = false;
                    blob = null;
                    airFill = 0;
                    wasteFill = 0;
                }
            }
            if (server.getGameTime() % 5 == 0) atmosphere = SpaceAtmosphere.getAtmosphere(server, worldPosition);
            networkPackNT(100);
        }

        @Override
        public void tickClient() {
            if (!on || level == null || level.getGameTime() % 2 != 0) return;
            RandomSource rand = level.getRandom();
            NTMFluidProperty prop = NTMFluidProperties.get(getFluidType());
            level.addParticle(new CoolingTowerParticleOptions.Builder()
                            .setLift(0.1F)
                            .setBaseScale(0.3F)
                            .setMaxScale(1F)
                            .setLife(20 + rand.nextInt(20))
                            .setColor(prop != null ? prop.color() & 0xFFFFFF : CoolingTowerParticleOptions.NO_TINT)
                            .build(),
                    worldPosition.getX() + 0.5 + rand.nextDouble() - 0.5,
                    worldPosition.getY() + 1,
                    worldPosition.getZ() + 0.5 + rand.nextDouble() - 0.5,
                    0, 0, 0);
        }

        private void consumeFromTank(int amount) {
            int bonus = Mth.clamp(amount, 0, bonusFill);
            bonusFill -= bonus;
            amount -= bonus;
            tank.setFill(tank.getFill() - amount);
        }

        @Override
        public void setRemoved() {
            if (registered && level instanceof ServerLevel) {
                SpaceAtmosphere.unregisterAtmosphere(this);
                registered = false;
            }
            super.setRemoved();
        }

        /** A scrubber claims the vent's waste gas; one at a time, the one that can run. */
        public boolean registerScrubber(SpaceScrubber.ScrubberBE candidate) {
            if (isRemoved() || tank.getTankType() != NTMFluids.byName("OXYGEN")) return false;
            if (scrubber == candidate) return true;
            if (scrubber != null && !scrubber.isRemoved() && scrubber.canOperate()) return false;
            scrubber = candidate;
            return true;
        }

        public boolean hasSeal() {
            return airFill > 1;
        }

        // IAtmosphereProvider

        @Override
        public ServerLevel getAtmosphereLevel() {
            return (ServerLevel) level;
        }

        @Override
        public int getMaxBlobRadius() {
            return 256;
        }

        @Override
        public BlockPos getRootPosition() {
            return worldPosition;
        }

        @Override
        public Fluid getFluidType() {
            Fluid type = tank.getTankType();
            return type != null ? type : NTMFluids.byName("OXYGEN");
        }

        @Override
        public double getFluidPressure() {
            if (blob == null || blob.getBlobSize() == 0) return 0;
            return (double) airFill / (double) blob.getBlobSize() * 0.2D;
        }

        @Override
        public void onBlobCreated(AtmosphereBlob created) {
            blob = created;
        }

        @Override
        public void consume(int amount) {
            airFill -= amount;
            wasteFill += amount;
            if (airFill < 1) airFill = 1;
        }

        @Override
        public void produce(int amount) {
            int produced = Mth.clamp(amount, 0, wasteFill);
            bonusFill += produced;
            wasteFill -= produced;
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
            output.writeBoolean(on);
            output.writeInt(airFill);
            output.writeInt(bonusFill);
            tank.packetSerialize(output);
            output.writeBoolean(atmosphere != null);
            if (atmosphere != null) {
                output.writeInt(atmosphere.fluids.size());
                for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
                    output.writeInt(net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(entry.fluid));
                    output.writeDouble(entry.pressure);
                }
            }
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            on = input.readBoolean();
            airFill = input.readInt();
            bonusFill = input.readInt();
            tank.packetDeserialize(input);
            if (input.readBoolean()) {
                CBT_Atmosphere read = new CBT_Atmosphere();
                int count = input.readInt();
                for (int i = 0; i < count; i++) {
                    Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(input.readInt());
                    read.fluids.add(new CBT_Atmosphere.FluidEntry(fluid, input.readDouble()));
                }
                atmosphere = read;
            } else {
                atmosphere = null;
            }
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            input.child("tank").ifPresent(tank::deserialize);
            // vents from before the gas swap may have no type saved: they were oxygen
            if (tank.getTankType() == null) tank.setTankType(NTMFluids.byName("OXYGEN"));
            airFill = input.getIntOr("fill", 0);
            wasteFill = input.getIntOr("waste", 0);
            bonusFill = input.getIntOr("bonus", 0);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            tank.serialize(output.child("tank"));
            output.putInt("fill", airFill);
            output.putInt("waste", wasteFill);
            output.putInt("bonus", bonusFill);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.atmosphereVent");
        }
    }
}
