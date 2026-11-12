// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.backport.InteractionResult;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.CelestialBody;
import com.hbm.space.atmosphere.SpaceAtmosphere;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.util.BobMathUtil;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The atmosphere machines: the compressor that draws a planet's air, the emitter that vents gas into it, and the algae film. */
public final class SpaceAtmoMachines {

    private SpaceAtmoMachines() {}

    public static final int[] COMPRESSOR_DIMS = {3, 0, 1, 0, 0, 1};
    public static final int[] EMITTER_DIMS = {2, 0, 1, 0, 1, 0};
    public static final int[] ALGAE_DIMS = {0, 0, 0, 0, 0, 0};
    public static final long COMPRESSOR_POWER = 1_000_000L;
    public static final long EMITTER_POWER = 250_000L;

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion();
    }

    public static final RegistryHandle<Compressor> COMPRESSOR = Reg.blockItem("atmospheric_compressor", Compressor::new, SpaceAtmoMachines::props);
    public static final Reg.BeHandle<CompressorBE> COMPRESSOR_BE = Reg.be("atmospheric_compressor",
                    () -> new BlockEntityType<>(CompressorBE::new, Set.of(COMPRESSOR.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static final RegistryHandle<Emitter> EMITTER = Reg.blockItem("atmospheric_emitter", Emitter::new, SpaceAtmoMachines::props);
    public static final Reg.BeHandle<EmitterBE> EMITTER_BE = Reg.be("atmospheric_emitter",
                    () -> new BlockEntityType<>(EmitterBE::new, Set.of(EMITTER.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static final RegistryHandle<Algae> ALGAE = Reg.blockItem("algae_film", Algae::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<AlgaeBE> ALGAE_BE = Reg.be("algae_film",
                    () -> new BlockEntityType<>(AlgaeBE::new, Set.of(ALGAE.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static void init() {
        var after = Library.id("air_scrubber");
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, after, () -> new ItemStack(COMPRESSOR.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, after, () -> new ItemStack(EMITTER.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, after, () -> new ItemStack(ALGAE.get()));
    }

    /** Sets a gas on a machine's tank from a fluid identifier held by the player; true if it took. */
    private static boolean identify(ItemStack held, Consumer<Fluid> apply) {
        FluidIdentifierData data = held.get(ModDataComponents.FLUID_IDENTIFIER.get());
        if (data == null || data.primary() == Fluids.EMPTY) return false;
        if (!NTMFluidProperties.hasTrait(data.primary(), FT_Gaseous.class)) return false;
        apply.accept(data.primary());
        return true;
    }

    private static boolean isScrewdriver(ItemStack held) {
        return held.getItem() instanceof IToolable.Tool tool && tool.toolType() == IToolable.ToolType.SCREWDRIVER;
    }

    // compressor

    public static class Compressor extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock, ILookOverlay {
        public Compressor(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CompressorBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(COMPRESSOR_BE).powerIn().fluidOut().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, COMPRESSOR_DIMS);
            l.port(0, 0, 0).port(1, 0, 0).port(0, 0, -1).port(1, 0, -1);
            return l.build(this, declaredCaps, COMPRESSOR_DIMS, 0, 0);
        }

        @Override
        protected InteractionResult useItemOnAtCore(ItemStack held, BlockState coreState, Level level, BlockPos core, Player player,
                InteractionHand hand, BlockHitResult hit) {
            if (!(level.getBlockEntity(core) instanceof CompressorBE be)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (isScrewdriver(held)) {
                if (!level.isClientSide()) be.cycleGas();
                return InteractionResult.SUCCESS;
            }
            boolean took = identify(held, f -> {
                if (!level.isClientSide()) be.switchGas(f);
            });
            return took ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            BlockPos core = overlayCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof CompressorBE be)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            if (!CelestialBody.hasTrait(level, CBT_Atmosphere.class)) {
                info.line(Component.literal("CANNOT COLLECT IN VACUUM"), 0xFF5555);
                return;
            }
            info.line(Component.literal("Power: " + BobMathUtil.getShortNumber(be.power) + "HE"), be.hasPower() ? 0x55FF55 : 0xFF5555);
            Fluid type = be.tank.getTankType();
            info.flow(false, Component.empty().append(type == null ? Component.empty() : type.getFluidType().getDescription())
                    .append(": " + be.tank.getFill() + "/" + be.tank.getMaxFill() + "mB").getString());
        }
    }

    public static class CompressorBE extends BlockEntityMachineBase
            implements IEnergyHandlerMK2, FluidFlushSender, FluidTankAccess, SyncUnitSchema {
        public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.byName("EARTHAIR"), 50_000);
        private final FluidTankNTM[] sending = {tank};
        private final FluidFlushOutputs flush = new FluidFlushOutputs();
        public long power;
        public float rot, prevRot;
        private float rotSpeed;

        public CompressorBE(BlockPos pos, BlockState state) {
            super(COMPRESSOR_BE.get(), pos, state, 0);
        }

        public boolean hasPower() {
            return power >= COMPRESSOR_POWER / 50;
        }

        private @Nullable CBT_Atmosphere outdoorAtmosphere(ServerLevel server) {
            if (CelestialBody.inOrbit(server) || SpaceAtmosphere.hasAtmosphere(server, worldPosition)) return null;
            return CelestialBody.getTrait(server, CBT_Atmosphere.class);
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            CBT_Atmosphere atmosphere = outdoorAtmosphere(server);
            if (atmosphere != null && atmosphere.getPressure() > 0.0D) {
                // if the air doesn't hold the gas we're drawing, pick a new one
                if (tank.getTankType() == null || !atmosphere.hasFluid(tank.getTankType())) tank.setTankType(atmosphere.getMainFluid());
            } else {
                tank.setTankType(null);
            }
            Fluid type = tank.getTankType();
            if (hasPower() && type != null && tank.getFill() + 200 <= tank.getMaxFill()) {
                tank.setFill(tank.getFill() + 200);
                power -= COMPRESSOR_POWER / 50;
                CelestialBody.consumeGas(server, type, 200);
                setChanged();
            }
            flush.provide(server, this);
            networkPackNT(50);
        }

        @Override
        public void tickClient() {
            if (hasPower()) rotSpeed = Math.min(30F, rotSpeed + 0.2F);
            else rotSpeed = Math.max(0F, rotSpeed - 0.1F);
            prevRot = rot;
            rot += rotSpeed;
            if (rot >= 360) {
                rot -= 360;
                prevRot -= 360;
            }
        }

        public void cycleGas() {
            if (!(level instanceof ServerLevel server)) return;
            CBT_Atmosphere atmosphere = outdoorAtmosphere(server);
            if (atmosphere == null) return;
            atmosphere.sortDescending();
            Fluid current = tank.getTankType();
            for (int i = 0; i < atmosphere.fluids.size(); i++) {
                if (atmosphere.fluids.get(i).fluid != current) continue;
                int next = i + 1 >= atmosphere.fluids.size() ? 0 : i + 1;
                if (atmosphere.fluids.get(next).pressure <= 0.0001) next = 0;
                tank.setTankType(atmosphere.fluids.get(next).fluid);
                break;
            }
        }

        public boolean switchGas(Fluid target) {
            if (!(level instanceof ServerLevel server) || target == tank.getTankType()) return false;
            CBT_Atmosphere atmosphere = outdoorAtmosphere(server);
            if (atmosphere == null || !atmosphere.hasFluid(target)) return false;
            tank.setTankType(target);
            return true;
        }

        @Override
        public long getPower() {
            return power;
        }

        @Override
        public void setPower(long p) {
            power = Math.max(0L, Math.min(p, COMPRESSOR_POWER));
        }

        @Override
        public long getMaxPower() {
            return COMPRESSOR_POWER;
        }

        @Override
        public FluidTankNTM[] getSendingTanks() {
            return sending;
        }

        @Override
        public void declareFlush(FlushLanes out) {
            out.add(tank, FlushFaces.own());
        }

        @Override
        public void forEachTank(Consumer<FluidTankNTM> visitor) {
            visitor.accept(tank);
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
        public long getFluidAvailable(Fluid type, int pressure) {
            return tank.provides(type) && pressure == tank.getPressure() ? tank.getFill() : 0;
        }

        @Override
        public void useUpFluid(Fluid type, int pressure, long amount) {
            if (!tank.provides(type) || pressure != tank.getPressure()) return;
            if (tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
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
            return Component.translatable("container.air");
        }
    }

    // emitter

    public static class Emitter extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock, ILookOverlay {
        public Emitter(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new EmitterBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(EMITTER_BE).powerIn().fluidIn().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, EMITTER_DIMS);
            l.port(0, 0, 0).port(-1, 0, 0).port(0, 0, -1).port(-1, 0, -1);
            return l.build(this, declaredCaps, EMITTER_DIMS, 0, 0);
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            BlockPos core = overlayCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof EmitterBE be)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            info.line(Component.literal("Power: " + BobMathUtil.getShortNumber(be.power) + "HE"), be.power > 0 ? 0x55FF55 : 0xFF5555);
            Fluid type = be.tank.getTankType();
            info.flow(true, Component.empty().append(type == null ? Component.empty() : type.getFluidType().getDescription())
                    .append(": " + be.tank.getFill() + "/" + be.tank.getMaxFill() + "mB").getString());
        }
    }

    public static class EmitterBE extends BlockEntityMachineBase
            implements IEnergyHandlerMK2, IFluidHandlerMK2, FluidTankAccess, SyncUnitSchema {
        public final FluidTankNTM tank = new FluidTankNTM(null, 50_000);
        public long power;
        public float fan, prevFan;

        public EmitterBE(BlockPos pos, BlockState state) {
            super(EMITTER_BE.get(), pos, state, 0);
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            Fluid type = tank.getTankType();
            if (tank.getFill() > 0 && type != null) {
                int toSpill = Math.max(tank.getFill() / 2, 1);
                if (power >= toSpill * 10L) {
                    tank.setFill(tank.getFill() - toSpill);
                    if (NTMFluidProperties.hasTrait(type, FT_Gaseous.class)) CelestialBody.emitGas(server, type, toSpill);
                    power -= toSpill * 10L;
                    if (tank.getFill() <= 0) tank.setTankType(null);
                    setChanged();
                }
            }
            networkPackNT(50);
        }

        @Override
        public void tickClient() {
            prevFan = fan;
            if (power >= Math.max(tank.getFill() / 2, 1) * 10L) {
                fan += 45;
                if (fan >= 360) {
                    fan -= 360;
                    prevFan -= 360;
                }
            }
        }

        @Override
        public long getPower() {
            return power;
        }

        @Override
        public void setPower(long p) {
            power = Math.max(0L, Math.min(p, EMITTER_POWER));
        }

        @Override
        public long getMaxPower() {
            return EMITTER_POWER;
        }

        @Override
        public boolean canReceiveFluid(Fluid type) {
            return tank.joins(type);
        }

        @Override
        public long getDemand(Fluid type, int pressure) {
            return pressure == tank.getPressure() && tank.accepts(type) ? tank.getMaxFill() - tank.getFill() : 0;
        }

        @Override
        public long transferFluid(Fluid type, int pressure, long amount) {
            if (pressure != tank.getPressure() || !tank.accepts(type)) return amount;
            long left = amount - tank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
            if (left != amount) setChanged();
            return left;
        }

        @Override
        public void forEachTank(Consumer<FluidTankNTM> visitor) {
            visitor.accept(tank);
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
            return Component.translatable("container.atmosphericEmitter");
        }
    }

    // algae film

    public static class Algae extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {
        public Algae(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new AlgaeBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(ALGAE_BE).fluidIn().fluidOut();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, ALGAE_DIMS);
            return l.build(this, declaredCaps, ALGAE_DIMS, 0, 0);
        }
    }

    /** Turns carbon dioxide into oxygen, but only where there's no air to do it for it, in orbit. */
    public static class AlgaeBE extends BlockEntityMachineBase implements IFluidHandlerMK2, FluidFlushSender, FluidTankAccess, SyncUnitSchema {
        public final FluidTankNTM[] tanks = {
            new FluidTankNTM(NTMFluids.byName("CARBONDIOXIDE"), 8_000), new FluidTankNTM(NTMFluids.byName("OXYGEN"), 8_000)
        };
        private final FluidTankNTM[] sending = {tanks[1]};
        private final FluidFlushOutputs flush = new FluidFlushOutputs();
        public boolean canOperate;

        public AlgaeBE(BlockPos pos, BlockState state) {
            super(ALGAE_BE.get(), pos, state, 0);
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            canOperate = CelestialBody.inOrbit(server);
            if (canOperate && server.random.nextBoolean() && tanks[0].getFill() > 0 && tanks[1].getFill() < tanks[1].getMaxFill()) {
                tanks[0].setFill(tanks[0].getFill() - 1);
                tanks[1].setFill(tanks[1].getFill() + 1);
                setChanged();
            }
            flush.provide(server, this);
            networkPackNT(20);
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
            return 2;
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
            output.writeBoolean(canOperate);
            for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            canOperate = input.readBoolean();
            for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            for (int i = 0; i < tanks.length; i++) {
                final FluidTankNTM tank = tanks[i];
                input.child("t" + i).ifPresent(tank::deserialize);
            }
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("t" + i));
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.algaeFilm");
        }
    }
}
