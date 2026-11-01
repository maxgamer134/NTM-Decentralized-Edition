// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.network;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.backport.Nbt;

/**
 * Main's flow control pump: a buffer of {@link #rate} mB that the input network fills and the
 * output network drains, so at most that much passes per tick.
 */
public class BlockEntityFlowControlPump extends BlockEntityMachineBase
        implements FluidTankAccess, FluidFlushSender, IControlReceiver, SyncUnitSchema {

    public static final int MAX_RATE = 10_000;
    public static final float ROTOR_ACCELERATION = 1F;

    @SyncField(units = 1L << 0)
    public final FluidTankNTM tank;

    @SyncField(units = 1L << 1)
    public int rate = 100;

    @SyncField(units = 1L << 2)
    public ConnectionPriority priority = ConnectionPriority.NORMAL;

    @SyncField(units = 1L << 3)
    public boolean redstone;

    @SyncField(units = 1L << 3)
    public boolean isWorking;

    public boolean topRail;
    public boolean bottomRail = true;
    public float rotor;
    public float prevRotor;
    public float rotorSpeed;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public BlockEntityFlowControlPump(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOW_CONTROL_PUMP.get(), pos, state, 0);
        this.tank = new FluidTankNTM(rate);
        this.sending = new FluidTankNTM[] {tank};
    }

    /** Output end, main's {@code getRotation(DOWN)} of the machine's dir; the input is opposite. */
    public Direction outputSide() {
        return BlockMultiblockCore.coreFacing(getBlockState()).getCounterClockWise();
    }

    @Override
    public void tickServer() {
        if (!redstone) flush.provide((ServerLevel) level, this);

        // if the capacity were changed directly, any excess buffered fluid would be destroyed
        // when running a closed loop or handling hard to get fluids, that's quite bad
        if (bufferMismatch()) {
            tank.changeTankSize(Math.max(tank.getFill(), rate));
            setChanged();
        }

        Direction out = outputSide();
        redstone =
                level.hasNeighborSignal(worldPosition)
                        || level.hasNeighborSignal(worldPosition.relative(out))
                        || level.hasNeighborSignal(worldPosition.relative(out.getOpposite()));

        networkPackNT(50);
        isWorking = false;
    }

    @Override
    public void tickClient() {
        if (level.getGameTime() % 20 == 0) {
            topRail = !level.getBlockState(worldPosition.above()).isAir();
            bottomRail = !level.getBlockState(worldPosition.below()).isAir();
        }

        if (isWorking) rotorSpeed += ROTOR_ACCELERATION;
        else rotorSpeed -= ROTOR_ACCELERATION;
        rotorSpeed = Mth.clamp(rotorSpeed, 0F, 30F);

        prevRotor = rotor;
        rotor += rotorSpeed;
        if (rotor >= 360F) {
            rotor -= 360F;
            prevRotor -= 360F;
        }
    }

    public boolean bufferMismatch() {
        return rate != tank.getMaxFill();
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return tank.joins(type);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (redstone || bufferMismatch()) return 0L;
        if (pressure != tank.getPressure() || !tank.accepts(type)) return 0L;
        return (long) tank.getMaxFill() - tank.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (redstone || pressure != tank.getPressure() || !tank.accepts(type)) return amount;
        int accepted = tank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public int receivingPressureMask(Fluid type) {
        return 1 << tank.getPressure();
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (redstone || !tank.provides(type) || pressure != tank.getPressure()) return 0L;
        return tank.getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (amount <= 0 || !tank.provides(type) || pressure != tank.getPressure()) return;
        if (tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) {
            isWorking = true;
            setChanged();
        }
    }

    @Override
    public int providingPressureMask(Fluid type) {
        return 1 << tank.getPressure();
    }

    @Override
    public ConnectionPriority getFluidPriority() {
        return priority;
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
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index != 0) throw new IndexOutOfBoundsException(index);
        return tank;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D)
                <= 128D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        // "rate" from the shared pump screen, "capacity" as main's GUIPump names it
        if (data.contains("rate")) rate = Math.clamp(Nbt.getIntOr(data, "rate", rate), 0, MAX_RATE);
        if (data.contains("capacity"))
            rate = Math.clamp(Nbt.getIntOr(data, "capacity", rate), 0, MAX_RATE);
        if (data.contains("pressure"))
            tank.withPressure(
                    Math.clamp(Nbt.getByteOr(data, "pressure", (byte) tank.getPressure()), 0, 5));
        if (data.contains("priority")) {
            priority =
                    ConnectionPriority.VALUES[
                            Math.clamp(
                                    Nbt.getByteOr(data, "priority", (byte) priority.ordinal()),
                                    0,
                                    ConnectionPriority.VALUES.length - 1)];
        }
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("t").ifPresent(tank::deserialize);
        priority =
                ConnectionPriority.VALUES[
                        Math.clamp(
                                in.getByteOr("p", (byte) priority.ordinal()),
                                0,
                                ConnectionPriority.VALUES.length - 1)];
        rate = Math.clamp(in.getIntOr("buffer", rate), 0, MAX_RATE);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        tank.serialize(out.child("t"));
        out.putByte("p", (byte) priority.ordinal());
        out.putInt("buffer", rate);
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> tank.packetSerialize(output);
            case 1 -> output.writeInt(rate);
            case 2 -> output.writeByte(priority.ordinal());
            case 3 -> {
                output.writeBoolean(redstone);
                output.writeBoolean(isWorking);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> tank.packetDeserialize(input);
            case 1 -> rate = Math.clamp(input.readInt(), 0, MAX_RATE);
            case 2 ->
                    priority =
                            ConnectionPriority.VALUES[
                                    Math.clamp(
                                            input.readByte(),
                                            0,
                                            ConnectionPriority.VALUES.length - 1)];
            case 3 -> {
                redstone = input.readBoolean();
                isWorking = input.readBoolean();
            }
            default -> throw new IllegalArgumentException();
        }
    }
}
