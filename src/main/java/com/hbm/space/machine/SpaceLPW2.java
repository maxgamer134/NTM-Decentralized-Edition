// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.machine.BlockEntityMachineLPW2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.MachineLPW2;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Rocket;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.space.station.IPropulsion;
import com.hbm.util.BobMathUtil;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space turns main's decorative LPW-2 into a station engine burning reformed kerosene and
 * oxygen. {@link MachineLPW2} and {@link BlockEntityMachineLPW2} hand over to this class only with
 * Space on; with it off the LPW-2 stays main's ornament.
 */
public final class SpaceLPW2 {

    private SpaceLPW2() {}

    public static final int[] DIMS = {6, 0, 3, 3, 9, 10};

    public static final RegistryHandle<SoundEvent> START = NeoForgeRegistrar.registerSound("misc.lpwstart");
    public static final RegistryHandle<SoundEvent> STOP = NeoForgeRegistrar.registerSound("misc.lpwstop");
    public static final RegistryHandle<SoundEvent> LOOP = NeoForgeRegistrar.registerSound("misc.lpwloop");

    public static void init() {}

    public static MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.LPW2).fluidIn();
    }

    /** Space's LPW-2 stands nine blocks out and turned a quarter left, with two fuel ports on top. */
    public static MultiblockLayout layout(BlockMultiblockCore block, int declaredCaps) {
        SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, DIMS);
        layout.port(1, 3, 3);
        layout.port(1, 3, -3);
        return layout.build(block, declaredCaps, DIMS, 9, 0);
    }

    public static void buildLookOverlay(BlockMultiblockCore block, Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        if (!CelestialBody.inOrbit(level)) return;
        BlockPos core = block.findClientCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof LPW2BE lpw)) return;
        info.title(block.getName(), 0xFFFF00, 0x404000);
        if (!lpw.isFacingPrograde()) {
            info.line(Component.literal("! ! ! ").append(Component.translatable("atmosphere.engineFacing")).append(" ! ! !"),
                    BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
            return;
        }
        for (FluidTankNTM tank : lpw.tanks) {
            info.input(NTMFluidProperties.clientName(tank.getFluid()) + ": " + tank.getFill() + "/" + tank.getMaxFill() + "mB");
        }
    }

    public static class LPW2BE extends BlockEntityMachineLPW2
            implements IPropulsion, IFluidHandlerMK2, FluidTankAccess, SyncUnitSchema, AudioLoop {
        public final FluidTankNTM[] tanks = {
            new FluidTankNTM(NTMFluids.KEROSENE_REFORM, 256_000), new FluidTankNTM(NTMFluids.OXYGEN, 256_000)
        };
        private boolean isOn;
        private float speed;
        public double lastTime;
        public double time;
        private float soundtime;
        private boolean registered;
        private int fuelCost;
        private @Nullable AudioWrapper audio;

        /** Set by the client: the rocket flame thrown out of the bell. */
        public static @Nullable BiConsumer<LPW2BE, Direction> CONTRAIL;

        public LPW2BE(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        public Direction facing() {
            return getBlockState().getValue(BlockMultiblockCore.FACING);
        }

        public boolean isFacingPrograde() {
            return facing() == Direction.SOUTH;
        }

        /** The animation runs only while the engine burns, spinning up and down with it. */
        @Override
        public double animationTime(double worldTime, float partialTicks) {
            return lastTime + (time - lastTime) * partialTicks;
        }

        @Override
        public void tickServer() {
            if (level == null || !CelestialBody.inOrbit(level)) return;
            if (!registered) {
                if (isFacingPrograde()) registerPropulsion();
                registered = true;
                isOn = false;
            }
            if (isOn) {
                soundtime++;
                if (soundtime == 1) level.playSound(null, worldPosition, START.get(), SoundSource.BLOCKS, 1.5F, 1F);
                else if (soundtime > 20) soundtime = 20;
            } else {
                soundtime--;
                if (soundtime == 19) level.playSound(null, worldPosition, STOP.get(), SoundSource.BLOCKS, 2.0F, 1F);
                else if (soundtime <= 0) soundtime = 0;
            }
            networkPackNT(250);
            lastTime = time;
            time += speed;
        }

        @Override
        public void tickClient() {
            if (level == null || !CelestialBody.inOrbit(level)) return;
            if (isOn) {
                speed = Math.min(1F, speed + 0.05F);
                if (soundtime > 18) {
                    audioLoop(true, 1F);
                    if (CONTRAIL != null) CONTRAIL.accept(this, facing().getClockWise());
                }
            } else {
                speed = Math.max(0F, speed - 0.05F);
                audioLoop(false, 0F);
            }
            lastTime = time;
            time += speed;
        }

        @Override
        public AudioWrapper createAudioLoop() {
            return AudioSystem.getLoopedSound(LOOP.get(), SoundSource.BLOCKS,
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

        // IPropulsion

        @Override
        public BlockEntity getBlockEntity() {
            return this;
        }

        @Override
        public boolean canPerformBurn(int shipMass, double deltaV) {
            FT_Rocket trait = tanks[0].getFluid() == null ? null : NTMFluidProperties.getTrait(tanks[0].getFluid(), FT_Rocket.class);
            int isp = trait != null ? trait.getISP() : 300;
            fuelCost = SolarSystem.getFuelCost(deltaV, shipMass, isp);
            for (FluidTankNTM tank : tanks) if (tank.getFill() < fuelCost) return false;
            return true;
        }

        @Override
        public void addErrors(List<Component> errors) {
            for (FluidTankNTM tank : tanks) {
                if (tank.getFill() < fuelCost) {
                    errors.add(Component.literal(" - Insufficient fuel: needs " + fuelCost + "mB").withStyle(ChatFormatting.RED));
                }
            }
        }

        @Override
        public float getThrust() {
            return 2_000_000_000.0F; // F1 thrust
        }

        @Override
        public int startBurn() {
            isOn = true;
            for (FluidTankNTM tank : tanks) tank.setFill(tank.getFill() - fuelCost);
            setChanged();
            return 120;
        }

        @Override
        public int endBurn() {
            isOn = false;
            return 120; // cooldown
        }

        // fluids

        @Override
        public boolean canReceiveFluid(Fluid type) {
            for (FluidTankNTM tank : tanks) if (tank.joins(type)) return true;
            return false;
        }

        @Override
        public void forEachTank(Consumer<FluidTankNTM> visitor) {
            for (FluidTankNTM tank : tanks) visitor.accept(tank);
        }

        @Override
        public long getDemand(Fluid type, int pressure) {
            if (pressure != 0) return 0;
            long demand = 0;
            for (FluidTankNTM tank : tanks) if (tank.accepts(type)) demand += tank.getMaxFill() - tank.getFill();
            return demand;
        }

        @Override
        public long transferFluid(Fluid type, int pressure, long amount) {
            if (pressure != 0) return amount;
            long left = amount;
            for (FluidTankNTM tank : tanks) {
                if (left <= 0 || !tank.accepts(type)) continue;
                left -= tank.fill(type, (int) Math.min(left, Integer.MAX_VALUE), true);
            }
            if (left != amount) setChanged();
            return left;
        }

        @Override
        public int tankCount() {
            return tanks.length;
        }

        @Override
        public FluidTankNTM tank(int index) {
            return tanks[index];
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
            output.writeFloat(soundtime);
            output.writeInt(fuelCost);
            for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            isOn = input.readBoolean();
            soundtime = input.readFloat();
            fuelCost = input.readInt();
            for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            isOn = input.getBooleanOr("on", false);
            for (int i = 0; i < tanks.length; i++) input.child("t" + i).ifPresent(tanks[i]::deserialize);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putBoolean("on", isOn);
            for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("t" + i));
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.lpw");
        }
    }
}
