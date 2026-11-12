// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.backport.InteractionResult;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.AudioLoop;
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
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Rocket;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
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
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

/**
 * The HTR3 thermal rocket: a station engine that burns superheated hydrogen (or another heated
 * propellant set with a fluid identifier) and brings a station up to speed in four seconds.
 */
public final class SpaceHTR3 {

    private SpaceHTR3() {}

    public static final int[] DIMS = {3, 3, 3, 3, 5, 5};

    public static final RegistryHandle<SoundEvent> START = NeoForgeRegistrar.registerSound("misc.htrstart");
    public static final RegistryHandle<SoundEvent> STOP = NeoForgeRegistrar.registerSound("misc.htrstop");
    public static final RegistryHandle<SoundEvent> LOOP = NeoForgeRegistrar.registerSound("misc.htrloop");

    public static final RegistryHandle<HTR3> BLOCK = Reg.blockItem("machine_htr3", HTR3::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<HTR3BE> BE = Reg.be("machine_htr3",
                    () -> new BlockEntityType<>(HTR3BE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_xenon_thruster"), () -> new ItemStack(BLOCK.get()));
    }

    /** The cell the PWR or a heat exchanger pipes propellant into, behind the engine to the left. */
    static BlockPos portOf(BlockPos core, Direction facing) {
        Direction rot = facing.getClockWise();
        return core.relative(rot, -5);
    }

    public static class HTR3 extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock, ILookOverlay {
        public HTR3(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new HTR3BE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).fluidIn();
        }

        /** NTM placed it turned a quarter to the left of where the player looks. */
        @Override
        public Direction getDirModified(Direction dir) {
            return dir.getCounterClockWise();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, DIMS);
            layout.port(5, 0, 0);
            return layout.build(this, declaredCaps, DIMS, 5, 3);
        }

        /** A fluid identifier sets the propellant to what its fluid heats up into, if that can fly. */
        @Override
        protected InteractionResult useItemOnAtCore(ItemStack stack, BlockState coreState, Level level, BlockPos core,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (player.isShiftKeyDown() || !(stack.getItem() instanceof FluidIdentifierItem)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(level.getBlockEntity(core) instanceof HTR3BE htr)) return InteractionResult.PASS;
            Fluid type = stack.getOrDefault(ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY).primary();
            if (type == null || type == Fluids.EMPTY) return InteractionResult.SUCCESS;
            FT_Heatable heatable = NTMFluidProperties.getTrait(type, FT_Heatable.class);
            if (heatable != null && heatable.getFirstStep() != null) {
                Fluid heated = heatable.getFirstStep().typeProduced();
                if (NTMFluidProperties.hasTrait(heated, FT_Rocket.class)) {
                    htr.tank.setTankType(heated);
                    htr.setChanged();
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            if (!CelestialBody.inOrbit(level)) return;
            BlockPos core = findClientCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof HTR3BE htr)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            if (!htr.isFacingPrograde()) {
                info.line(Component.literal("! ! ! ").append(Component.translatable("atmosphere.engineFacing")).append(" ! ! !"),
                        BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
                return;
            }
            FluidTankNTM tank = htr.tank;
            info.input(Component.empty().append(tank.getFluid() == null ? Component.empty() : tank.getFluid().getFluidType().getDescription())
                    .append(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB"));
            Direction facing = level.getBlockState(core).getValue(FACING);
            if (portOf(core, facing).equals(info.getHitPos())) info.line("Connect to PWR from here");
        }
    }

    public static class HTR3BE extends BlockEntityMachineBase
            implements IPropulsion, IFluidHandlerMK2, FluidTankAccess, SyncUnitSchema, AudioLoop {
        public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.byName("SUPERHEATED_HYDROGEN"), 1_280_000);
        private boolean isOn;
        private float speed;
        public double lastTime;
        public double time;
        private float soundtime;
        private boolean registered;
        private int fuelCost;
        private @Nullable AudioWrapper audio;

        public HTR3BE(BlockPos pos, BlockState state) {
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
                    Direction dir = facing().getClockWise();
                    if (CONTRAIL != null) CONTRAIL.accept(this, dir);
                }
            } else {
                speed = Math.max(0F, speed - 0.05F);
                audioLoop(false, 0F);
            }
            lastTime = time;
            time += speed;
        }

        /** Set by the client: the rocket flame thrown out of the bell. */
        public static java.util.function.@Nullable BiConsumer<HTR3BE, Direction> CONTRAIL;

        public @Nullable Fluid propellant() {
            return tank.getFluid();
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
                if (isFacingPrograde()) unregisterPropulsion();
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
            FT_Rocket trait = tank.getFluid() == null ? null : NTMFluidProperties.getTrait(tank.getFluid(), FT_Rocket.class);
            int isp = trait != null ? trait.getISP() : 300;
            fuelCost = SolarSystem.getFuelCost(deltaV, shipMass, isp);
            return tank.getFill() >= fuelCost;
        }

        @Override
        public void addErrors(List<Component> errors) {
            if (tank.getFill() < fuelCost) {
                errors.add(Component.literal(" - Insufficient fuel: needs " + fuelCost + "mB").withStyle(ChatFormatting.RED));
            }
        }

        @Override
        public float getThrust() {
            return 800_000_000.0F;
        }

        @Override
        public int startBurn() {
            isOn = true;
            tank.setFill(tank.getFill() - fuelCost);
            setChanged();
            return 80;
        }

        @Override
        public int endBurn() {
            isOn = false;
            return 80; // cooldown
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
            output.writeFloat(soundtime);
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
            soundtime = input.readFloat();
            fuelCost = input.readInt();
            tank.packetDeserialize(input);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            isOn = input.getBooleanOr("on", false);
            input.child("tank").ifPresent(tank::deserialize);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putBoolean("on", isOn);
            tank.serialize(output.child("tank"));
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.htr");
        }
    }
}
