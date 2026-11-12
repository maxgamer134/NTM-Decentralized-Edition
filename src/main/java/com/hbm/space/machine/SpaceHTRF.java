// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.machine.albion.BlockEntityCooledBase;
import com.hbm.blockentity.machine.fusion.FusionPorts;
import com.hbm.blockentity.machine.fusion.IFusionPowerReceiver;
import com.hbm.blocks.machine.fusion.BlockFusionMachine;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.CreativeContents;
import com.hbm.items.DescBlockItem;
import com.hbm.lib.Library;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.atmosphere.IBlockSealable;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.space.station.IPropulsion;
import com.hbm.util.BobMathUtil;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The HTR-F4 NEO: a station engine that throws a fusion reactor's plasma out of its bell. It needs
 * a full charge, a running coolant loop and enough plasma energy from a torus for the trip.
 */
public final class SpaceHTRF {

    private SpaceHTRF() {}

    public static final int[] DIMS = {2, 2, 2, 2, 11, 9};

    public static final RegistryHandle<SoundEvent> START = NeoForgeRegistrar.registerSound("misc.htrfstart");

    public static final RegistryHandle<HTRF> BLOCK = Reg.blockItem("machine_htrf4neo", HTRF::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion(),
            (block, props) -> new DescBlockItem(block, props, 1));
    public static final Reg.BeHandle<HTRFBE> BE = Reg.be("machine_htrf4neo",
                    () -> new BlockEntityType<>(HTRFBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_htr3"), () -> new ItemStack(BLOCK.get()));
    }

    /**
     * The cell a fusion reactor torus feeds plasma into: the end of the engine opposite the bell.
     * As in NTM:Space the plasma node sits one block past it (local 10), so the torus stands one
     * block clear of the engine: the port reaches two blocks.
     */
    static FusionPorts.Port plasmaPort(BlockPos core, Direction facing) {
        Direction out = facing.getCounterClockWise();
        return new FusionPorts.Port(FusionPorts.Kind.PLASMA, core.relative(out, 9), out, 2);
    }

    public static class HTRF extends BlockFusionMachine implements ICapabilityBlock, ILookOverlay, IBlockSealable {
        public HTRF(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new HTRFBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().fluidIn().fluidOut().fe();
        }

        @Override
        public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
            return List.of(plasmaPort(core, facing));
        }

        /** NTM placed it turned a quarter to the left of where the player looks. */
        @Override
        public Direction getDirModified(Direction dir) {
            return dir.getCounterClockWise();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, DIMS);
            layout.port(9, 0, 0);
            layout.port(5, -2, -2);
            layout.port(-1, -2, -2);
            layout.port(5, -2, 2);
            layout.port(-1, -2, 2);
            return layout.build(this, declaredCaps, DIMS, 11, 2);
        }

        @Override
        public boolean isSealed(Level level, BlockPos pos) {
            return true;
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            BlockPos core = findClientCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof HTRFBE htrf)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            // the overlay shows anywhere; the engines only fire on an orbital station
            info.line(Component.literal("Thrust: " + BobMathUtil.getShortNumber((long) htrf.getThrust()) + "N"), 0x55FFFF);
            if (!CelestialBody.inOrbit(level)) info.line(Component.literal("! ! ! ").append(Component.translatable("atmosphere.noOrbit")).append(" ! ! !"), BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
            if (CelestialBody.inOrbit(level) && !htrf.isFacingPrograde()) {
                info.line(Component.literal("! ! ! ").append(Component.translatable("atmosphere.engineFacing")).append(" ! ! !"),
                        BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
                return;
            }
            info.line(Component.literal("Plasma Energy: ").append(Component.literal(BobMathUtil.getShortNumber(htrf.plasmaEnergy) + "TU")
                    .withStyle(htrf.plasmaEnergy == 0 ? ChatFormatting.RED : ChatFormatting.GREEN)));
            info.line(Component.literal("Power: ").append(Component.literal(BobMathUtil.getShortNumber(htrf.power) + "HE")
                    .withStyle(htrf.power < htrf.getMaxPower() ? ChatFormatting.RED : ChatFormatting.GREEN)));
            int heat = (int) Math.ceil(htrf.temperature);
            info.line(Component.literal("Temperature: ").append(Component.literal(heat + "K")
                    .withStyle(heat > 123 ? ChatFormatting.RED : ChatFormatting.AQUA)));
            for (int i = 0; i < 2; i++) {
                var tank = htrf.coolantTanks[i];
                info.flow(i == 0, NTMFluidProperties.clientName(tank.getFluid()) + ": " + tank.getFill() + "/" + tank.getMaxFill() + "mB");
            }
            if (!htrf.isCool()) {
                info.line("! ! ! INSUFFICIENT COOLING ! ! !", BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
            }
            Direction facing = level.getBlockState(core).getValue(FACING);
            BlockPos hit = info.getHitPos();
            if (hit != null && hit.equals(plasmaPort(core, facing).cell())) info.line("Connect to Fusion Reactor from here");
        }
    }

    public static class HTRFBE extends BlockEntityCooledBase implements IPropulsion, IFusionPowerReceiver, AudioLoop {
        public static final long MAX_POWER = 200_000_000L;
        private static final long UNIT_ENGINE = 1L << 3;

        public long plasmaEnergy;
        public long plasmaEnergySync;
        public float rotor;
        public float prevRotor;
        public float rotorSpeed;
        private float soundtime;
        private boolean registered;
        public boolean isOn;
        public int fuelCost;
        public float thrustAmount;
        public float plasmaR;
        public float plasmaG;
        public float plasmaB;
        private @Nullable AudioWrapper audio;

        public HTRFBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 0);
        }

        public Direction facing() {
            return getBlockState().getValue(BlockMultiblockCore.FACING);
        }

        /** The engine has to point along the station's direction of travel. */
        public boolean isFacingPrograde() {
            return facing() == Direction.SOUTH;
        }

        @Override
        public void tickServer() {
            if (level == null) return;
            flush.provide((ServerLevel) level, this);

            if (!registered) {
                if (isFacingPrograde()) registerPropulsion();
                registered = true;
                isOn = false;
            }

            plasmaEnergySync = plasmaEnergy;
            tickCooling();

            if (isOn) {
                soundtime++;
                if (soundtime == 1) level.playSound(null, worldPosition, START.get(), SoundSource.BLOCKS, 1.5F, 1F);
                else if (soundtime > 30) soundtime = 30;
            } else {
                soundtime--;
                if (soundtime == 29) level.playSound(null, worldPosition, SpaceHTR3.STOP.get(), SoundSource.BLOCKS, 2.0F, 1F);
                else if (soundtime <= 0) soundtime = 0;
            }

            networkPackNT(200);
            plasmaEnergy = 0;
        }

        @Override
        public void tickClient() {
            if (power >= MAX_POWER || isOn) rotorSpeed += 0.125F;
            else rotorSpeed -= 0.125F;
            rotorSpeed = Mth.clamp(rotorSpeed, 0F, 15F);
            prevRotor = rotor;
            rotor += rotorSpeed;
            if (rotor >= 360F) {
                rotor -= 360F;
                prevRotor -= 360F;
            }

            if (isOn) {
                if (soundtime > 28) audioLoop(true, 1F);
                thrustAmount = Math.min(1F, thrustAmount + 0.01F);
            } else {
                audioLoop(false, 0F);
                thrustAmount = Math.max(0F, thrustAmount - 0.01F);
            }
        }

        @Override
        public AudioWrapper createAudioLoop() {
            return AudioSystem.getLoopedSound(SpaceHTR3.LOOP.get(), SoundSource.BLOCKS,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 0.25F, 27.5F, 1.0F, 20);
        }

        @Override
        public void audioLoop(boolean running, float volume) {
            audio = AudioLoop.loop(this, audio, running, volume, 0F, false);
        }

        @Override
        public void audioLoop(boolean running, float volume, float pitch) {
            audio = AudioLoop.loop(this, audio, running, volume, pitch, true);
        }

        @Override
        public void setRemoved() {
            if (registered && level != null) {
                unregisterPropulsion();
                registered = false;
            }
            audioLoop(false, 0F);
            super.setRemoved();
        }

        // fusion

        @Override
        public boolean receivesFusionPower() {
            return true;
        }

        @Override
        public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
            plasmaEnergy = fusionPower;
            plasmaR = r;
            plasmaG = g;
            plasmaB = b;
        }

        // IPropulsion

        @Override
        public BlockEntity getBlockEntity() {
            return this;
        }

        @Override
        public boolean canPerformBurn(int shipMass, double deltaV) {
            fuelCost = SolarSystem.getFuelCost(deltaV, shipMass, 250);
            if (plasmaEnergySync < fuelCost) return false;
            if (power < MAX_POWER) return false;
            return isCool();
        }

        @Override
        public void addErrors(List<Component> errors) {
            if (plasmaEnergySync < fuelCost) {
                errors.add(Component.literal("Insufficient plasma energy: needs " + BobMathUtil.getShortNumber(fuelCost) + " TU")
                        .withStyle(ChatFormatting.RED));
            }
            if (power < MAX_POWER) errors.add(Component.literal("Insufficient power").withStyle(ChatFormatting.RED));
            if (!isCool()) errors.add(Component.literal("Coolant loop not operational!").withStyle(ChatFormatting.RED));
        }

        @Override
        public float getThrust() {
            return 1_600_000_000.0F;
        }

        @Override
        public int startBurn() {
            isOn = true;
            power = 0;
            setChanged();
            return 180;
        }

        @Override
        public int endBurn() {
            isOn = false;
            return 180;
        }

        @Override
        public long getMaxPower() {
            return MAX_POWER;
        }

        // sync and saving

        @Override
        public long syncUnitMask() {
            return super.syncUnitMask() | UNIT_ENGINE;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            if (unit != 3) {
                super.writeSyncUnit(unit, output);
                return;
            }
            output.writeLong(plasmaEnergySync);
            output.writeBoolean(isOn);
            output.writeInt(fuelCost);
            output.writeFloat(soundtime);
            output.writeFloat(plasmaR);
            output.writeFloat(plasmaG);
            output.writeFloat(plasmaB);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 3) {
                super.readSyncUnit(unit, input);
                return;
            }
            plasmaEnergy = input.readLong();
            isOn = input.readBoolean();
            fuelCost = input.readInt();
            soundtime = input.readFloat();
            plasmaR = input.readFloat();
            plasmaG = input.readFloat();
            plasmaB = input.readFloat();
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            plasmaEnergy = input.getLongOr("plasma", 0L);
            isOn = input.getBooleanOr("on", false);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("plasma", plasmaEnergy);
            output.putBoolean("on", isOn);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.htrfneo");
        }
    }
}
