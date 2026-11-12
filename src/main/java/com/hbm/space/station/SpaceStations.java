// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.station;

import com.hbm.api.control.IControlReceiver;
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
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Rocket;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.SolarSystemSavedData;
import com.hbm.space.rocket.EntityRideableRocket;
import com.hbm.space.rocket.EntityRideableRocket.RocketState;
import com.hbm.space.rocket.ItemCustomRocket;
import com.hbm.space.rocket.ItemVOTVdrive;
import com.hbm.space.rocket.RocketStruct;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.space.rocket.SpaceRocketParts;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The orbital station's blocks: the core every station is built around (placed by the first
 * crewed rocket to arrive, and unbreakable), the docking ports players add to it, the computer
 * that flies it to another body, and a creative engine.
 */
public final class SpaceStations {

    private SpaceStations() {}

    public static final int[] PORT_DIMS = {1, 0, 2, 2, 2, 2};

    public static final RegistryHandle<Port> CORE = Reg.block("orbital_station", Port::new,
            () -> BlockBehaviour.Properties.of().strength(100F, 480F).requiresCorrectToolForDrops().noOcclusion());
    public static final RegistryHandle<Port> PORT = Reg.blockItem("orbital_station_port", Port::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 6F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<PortBE> PORT_BE = Reg.be("orbital_station",
                    () -> new BlockEntityType<>(PortBE::new, Set.of(CORE.get(), PORT.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static final RegistryHandle<Computer> COMPUTER = Reg.blockItem("orbital_station_computer", Computer::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 6F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<ComputerBE> COMPUTER_BE = Reg.be("orbital_station_computer",
                    () -> new BlockEntityType<>(ComputerBE::new, Set.of(COMPUTER.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static final RegistryHandle<CreativePropulsion> PROPULSION_CREATIVE = Reg.blockItem("propulsion_creative", CreativePropulsion::new,
            () -> BlockBehaviour.Properties.of().strength(1F, 6F));
    public static final Reg.BeHandle<CreativePropulsionBE> PROPULSION_CREATIVE_BE = Reg.be("propulsion_creative",
                    () -> new BlockEntityType<>(CreativePropulsionBE::new, Set.of(PROPULSION_CREATIVE.get()), null))
            .ticksServer(CreativePropulsionBE::tick);

    /** Opens the computer's screen; the client sets it. */
    public static Consumer<BlockPos> openComputer = pos -> {};

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("machine_stardar"), () -> new ItemStack(PROPULSION_CREATIVE.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("machine_stardar"), () -> new ItemStack(COMPUTER.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("machine_stardar"), () -> new ItemStack(PORT.get()));
        StationEvents.register();
    }

    private static void noOrbit(ILookOverlay.LookInfo info, Block block) {
        info.title(block.getName(), 0xffff00, 0x404000)
                .line(Component.literal("! ! ! ").append(Component.translatable("atmosphere.noOrbit")).append(" ! ! !"), 0xff0000);
    }

    // the docking ports

    public static class Port extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock, ILookOverlay {
        public Port(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new PortBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(PORT_BE).fluidIn().items();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, PORT_DIMS);
            for (int i = -1; i <= 1; i++) {
                layout.port(2, 1, i).port(-2, 1, i).port(i, 1, 2).port(i, 1, -2);
            }
            return layout.build(this, declaredCaps, PORT_DIMS, 2, 0);
        }

        @Override
        protected InteractionResult useItemOnAtCore(ItemStack held, BlockState coreState, Level level, BlockPos core, Player player,
                InteractionHand hand, BlockHitResult hit) {
            if (!CelestialBody.inOrbit(level) || !(level.getBlockEntity(core) instanceof PortBE port)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (port.hasStoredItems() || port.hasDocked || held.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
            ItemStack rocket = null;
            if (held.getItem() instanceof ItemCustomRocket && ItemCustomRocket.hasFuel(held)) rocket = held.copyWithCount(1);
            else if (held.is(SpaceRocketParts.RP_CAPSULE_20.get()) || held.is(SpaceRocketParts.RP_POD_20.get()))
                rocket = ItemCustomRocket.build(new RocketStruct(held));
            if (rocket == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                port.spawnRocket(rocket);
                held.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useAtCore(BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
            if (!CelestialBody.inOrbit(level) || !(level.getBlockEntity(core) instanceof PortBE port)) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (port.hasStoredItems()) {
                port.giveStoredItems(player);
            } else if (port.hasDocked && !port.hasRider) {
                if (player.isSecondaryUseActive()) {
                    port.despawnRocket();
                    port.giveStoredItems(player);
                } else {
                    port.enterCapsule(player);
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            if (!CelestialBody.inOrbit(level)) {
                noOrbit(info, this);
                return;
            }
            BlockPos core = findClientCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof PortBE port)) return;
            for (ItemStack stack : port.inventory) if (!stack.isEmpty()) info.output(stack.getHoverName());
            if (info.size() > 0) {
                info.line(Component.translatable("station.retrieveRocket"));
            } else if (port.hasDocked) {
                if (port.hasRider) info.line(Component.translatable("station.occupiedRocket").withStyle(ChatFormatting.YELLOW));
                else info.line(Component.translatable(net.minecraft.client.Minecraft.getInstance().player != null
                        && net.minecraft.client.Minecraft.getInstance().player.isShiftKeyDown() ? "station.removeRocket" : "station.enterRocket"));
                if (port.needsFuel) {
                    FluidTankNTM tank = port.tank;
                    info.input(Component.empty().append(tank.getTankType().getFluidType().getDescription()).append(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB"));
                    if (!port.hasFuel) info.line(Component.translatable("station.emptyRocket").withStyle(ChatFormatting.RED));
                }
            } else {
                info.line(Component.translatable("station.placeRocket"));
            }
            info.title(getName(), 0xffff00, 0x404000);
        }
    }

    public static class PortBE extends BlockEntityMachineBase implements SyncUnitSchema, IFluidHandlerMK2, FluidTankAccess {
        private @Nullable OrbitalStation station;
        private @Nullable EntityRideableRocket docked;
        public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.byName("HYDRAZINE"), 16_000);
        public boolean isReserved;
        public boolean hasDocked, hasRider, needsFuel, hasFuel;
        public float rot, prevRot;

        public PortBE(BlockPos pos, BlockState state) {
            super(PORT_BE.get(), pos, state, 16);
        }

        public boolean isCore() {
            return getBlockState().is(CORE.get());
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server) || !CelestialBody.inOrbit(level)) return;
            // the ports keep the station's state ticking for whoever is aboard
            station = OrbitalStation.getStationFromPosition(SolarSystemSavedData.get(server), worldPosition.getX(), worldPosition.getZ());
            if (isCore()) station.update(level);
            station.addPort(this);

            if (docked != null && docked.isReusable()) {
                // size the tank to the trip, keeping what it holds
                tank.changeTankSize(Math.max(getFillRequirement(false), tank.getFill()));
            }
            if (docked != null && (docked.isRemoved() || docked.getState() == RocketState.UNDOCKING)) {
                // the fuel goes only on a real undocking, in case the pod is stashed before launch
                if (!docked.isRemoved() && docked.isReusable()) {
                    boolean toOrbit = docked.getTarget().inOrbit();
                    tank.setFill(Math.max(0, tank.getFill() - getFillRequirement(toOrbit)));
                }
                docked = null;
            }
            if (docked != null && docked.isReusable()) {
                boolean fuelled = hasSufficientFuel(docked.getTarget().inOrbit());
                if (fuelled && docked.getState() == RocketState.NEEDSFUEL)
                    docked.setState(!docked.navDrive.isEmpty() ? RocketState.AWAITING : RocketState.LANDED);
                else if (!fuelled && docked.getState() != RocketState.NEEDSFUEL) docked.setState(RocketState.NEEDSFUEL);
            }
            hasDocked = docked != null;
            hasRider = hasDocked && docked.isVehicle();
            needsFuel = hasDocked && docked.isReusable();
            hasFuel = needsFuel && hasSufficientFuel(docked.getTarget().inOrbit());
            if (hasDocked) isReserved = false;
            networkPackNT(OrbitalStation.STATION_SIZE / 2);
        }

        @Override
        public void tickClient() {
            prevRot = rot;
            rot = hasDocked ? Math.min(rot + 2.25F, 90F) : Math.max(rot - 2.25F, 0F);
        }

        @Override
        public void setRemoved() {
            if (station != null) station.removePort(this);
            super.setRemoved();
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if (docked != null && !docked.isRemoved()) docked.dropNDie();
            super.preRemoveSideEffects(pos, state);
        }

        public void enterCapsule(Player player) {
            if (docked != null && !docked.isVehicle()) player.startRiding(docked);
        }

        public void dockRocket(EntityRideableRocket rocket) {
            if (docked != rocket) despawnRocket();
            docked = rocket;
        }

        /** Stows the docked rocket and its drive as items, ready to hand back. */
        public void despawnRocket() {
            if (docked == null) return;
            RocketStruct rocket = docked.getRocket();
            List<ItemStack> stow = new ArrayList<>();
            if (!docked.navDrive.isEmpty()) stow.add(docked.navDrive.copy());
            if (!rocket.stages.isEmpty()) stow.add(ItemCustomRocket.build(rocket, true));
            else if (rocket.capsule != null) stow.add(rocket.capsuleStack());
            for (int i = 0; i < inventory.size() && !stow.isEmpty(); i++)
                if (inventory.get(i).isEmpty()) setItem(i, stow.removeLast());
            docked.discard();
            docked = null;
            setChanged();
        }

        public void reservePort() {
            isReserved = true;
        }

        public void spawnRocket(ItemStack stack) {
            if (level == null) return;
            EntityRideableRocket rocket = EntityRideableRocket.of(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, stack);
            rocket.setPos(rocket.getX(), rocket.getY() - rocket.getBbHeight(), rocket.getZ());
            rocket.setState(rocket.isReusable() ? RocketState.NEEDSFUEL : RocketState.LANDED);
            level.addFreshEntity(rocket);
            dockRocket(rocket);
        }

        public @Nullable EntityRideableRocket getDocked() {
            return docked;
        }

        public boolean hasStoredItems() {
            for (ItemStack stack : inventory) if (!stack.isEmpty()) return true;
            return false;
        }

        public void giveStoredItems(Player player) {
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.get(i);
                if (stack.isEmpty()) continue;
                if (!player.getInventory().add(stack.copy())) player.drop(stack.copy(), false);
                setItem(i, ItemStack.EMPTY);
            }
            player.containerMenu.broadcastChanges();
            setChanged();
        }

        public boolean hasSufficientFuel(boolean toOrbit) {
            return tank.getFill() >= getFillRequirement(toOrbit);
        }

        public int getFillRequirement(boolean toOrbit) {
            if (toOrbit) return 500; // hopping between stations is much cheaper
            CelestialBody body = station != null ? station.orbiting : CelestialBody.getBody(Level.OVERWORLD);
            int mass = docked != null ? docked.getRocket().getLaunchMass() : 4_000;
            FT_Rocket fuel = com.hbm.inventory.fluid.NTMFluidProperties.getTrait(tank.getTankType(), FT_Rocket.class);
            int thrust = fuel != null ? (int) fuel.getThrust() : 600_000;
            int isp = fuel != null ? fuel.getISP() : 300;
            return SolarSystem.getCostBetween(body, body, mass, thrust, isp, false, true);
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
            if (left != amount) markChanged();
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
            output.writeBoolean(hasDocked);
            output.writeBoolean(hasRider);
            output.writeBoolean(needsFuel);
            output.writeBoolean(hasFuel);
            tank.packetSerialize(output);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            hasDocked = input.readBoolean();
            hasRider = input.readBoolean();
            needsFuel = input.readBoolean();
            hasFuel = input.readBoolean();
            tank.packetDeserialize(input);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            input.child("t0").ifPresent(tank::deserialize);
            tank.setTankType(NTMFluids.byName("HYDRAZINE"));
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            tank.serialize(output.child("t0"));
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.orbitalStation");
        }
    }

    // the computer

    public static class Computer extends BlockMultiblockCore implements ITickingBlock, ILookOverlay {
        public Computer(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ComputerBE(pos, state);
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            return new SpaceLayouts().build(this, declaredCaps, new int[] {0, 0, 0, 0, 0, 0}, 0, 0);
        }

        @Override
        protected InteractionResult useItemOnAtCore(ItemStack held, BlockState coreState, Level level, BlockPos core, Player player,
                InteractionHand hand, BlockHitResult hit) {
            if (!CelestialBody.inOrbit(level) || !(level.getBlockEntity(core) instanceof ComputerBE computer)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!(held.getItem() instanceof ItemVOTVdrive)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (computer.isTravelling() || computer.hasDrive()) return InteractionResult.FAIL;
            ItemVOTVdrive.Destination destination = ItemVOTVdrive.getDestination(held);
            if (destination == null || destination.body() == SolarSystem.Body.ORBIT) return InteractionResult.FAIL;
            if (!computer.travelTo(destination.body().getBody(), held.copyWithCount(1))) return InteractionResult.FAIL;
            held.consume(1, player);
            level.playSound(null, core, com.hbm.sound.ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1F, 1F);
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useAtCore(BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
            if (!CelestialBody.inOrbit(level) || !(level.getBlockEntity(core) instanceof ComputerBE computer)) return InteractionResult.PASS;
            if (computer.isTravelling()) return InteractionResult.PASS;
            if (computer.hasDrive()) {
                if (!level.isClientSide()) {
                    ItemStack drive = computer.getItem(0).copy();
                    if (!player.getInventory().add(drive)) player.drop(drive, false);
                    computer.setItem(0, ItemStack.EMPTY);
                    computer.setChanged();
                }
            } else if (level.isClientSide()) {
                openComputer.accept(core);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            if (!CelestialBody.inOrbit(level)) {
                noOrbit(info, this);
                return;
            }
            BlockPos core = findClientCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof ComputerBE computer)) return;
            OrbitalStation station = OrbitalStation.clientStation;
            double progress = station.getUnscaledProgress(0);
            if (!station.hasEngines) {
                info.line(Component.literal("No engines available").withStyle(ChatFormatting.RED));
            } else if (!station.errorsAt.isEmpty()) {
                for (BlockPos at : station.errorsAt) {
                    if (!(level.getBlockEntity(at) instanceof IPropulsion engine)) continue;
                    List<Component> errors = new ArrayList<>();
                    engine.addErrors(errors);
                    if (errors.isEmpty()) continue;
                    info.line(level.getBlockState(at).getBlock().getName().withStyle(ChatFormatting.RED));
                    errors.forEach(info::line);
                }
            } else if (progress > 0) {
                Component target = Component.translatable("body." + station.target.name);
                switch (station.state) {
                    case LEAVING -> info.line(Component.translatable("station.engage").append(": ").withStyle(ChatFormatting.AQUA).append(target));
                    case ARRIVING -> info.line(Component.translatable("station.disengage").withStyle(ChatFormatting.AQUA));
                    default -> info.line(Component.translatable("station.travelling").append(": ").withStyle(ChatFormatting.AQUA).append(target));
                }
                info.line(Component.translatable("station.progress").append(": ").withStyle(ChatFormatting.AQUA).append(Math.round(progress * 100) + "%"));
            } else if (computer.hasDrive) {
                info.line(Component.translatable("station.interactDrive"));
            } else {
                info.line(Component.translatable("station.insertDrive"));
            }
            info.title(getName(), 0xffff00, 0x404000);
        }
    }

    public static class ComputerBE extends BlockEntityMachineBase implements IControlReceiver, SyncUnitSchema {
        public boolean hasDrive;

        public ComputerBE(BlockPos pos, BlockState state) {
            super(COMPUTER_BE.get(), pos, state, 1);
        }

        private OrbitalStation station() {
            return OrbitalStation.getStationFromPosition(level, worldPosition.getX(), worldPosition.getZ());
        }

        public boolean hasDrive() {
            return !getItem(0).isEmpty();
        }

        public boolean travelTo(CelestialBody body, ItemStack drive) {
            OrbitalStation station = station();
            if (station.orbiting == body) return false;
            if (level instanceof net.minecraft.server.level.ServerLevel server) station.loadEngines(server);
            station.travelTo(body);
            if (station.state == OrbitalStation.StationState.ORBIT) return false; // the engines refused
            setItem(0, drive);
            setChanged();
            return true;
        }

        public boolean isTravelling() {
            return station().state != OrbitalStation.StationState.ORBIT;
        }

        @Override
        public void tickServer() {
            hasDrive = hasDrive();
            networkPackNT(50);
        }

        @Override
        public boolean hasPermission(Player player) {
            return player.distanceToSqr(worldPosition.getCenter()) < 64 * 64;
        }

        @Override
        public void receiveControl(CompoundTag data) {
            if (!(level instanceof ServerLevel)) return;
            OrbitalStation station = station();
            if (data.contains("name")) station.name = data.getString("name");
            if (data.contains("gravity")) station.gravityMultiplier = data.getBoolean("gravity") ? 1 : 0;
            SolarSystemSavedData.get((ServerLevel) level).setDirty();
        }

        @Override
        public long syncUnitMask() {
            return super.syncUnitMask() | 1L;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            if (unit != 0) super.writeSyncUnit(unit, output);
            else output.writeBoolean(hasDrive);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) super.readSyncUnit(unit, input);
            else hasDrive = input.readBoolean();
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.orbitalStationComputer");
        }
    }

    // the creative engine

    public static class CreativePropulsion extends Block implements EntityBlock, ITickingBlock {
        public CreativePropulsion(Properties props) {
            super(props);
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CreativePropulsionBE(pos, state);
        }
    }

    public static class CreativePropulsionBE extends BlockEntity implements IPropulsion {
        private boolean registered;

        public CreativePropulsionBE(BlockPos pos, BlockState state) {
            super(PROPULSION_CREATIVE_BE.get(), pos, state);
        }

        static void tick(CreativePropulsionBE be) {
            if (!be.registered && be.level != null && CelestialBody.inOrbit(be.level)) {
                be.registerPropulsion();
                be.registered = true;
            }
        }

        @Override
        public void setRemoved() {
            if (registered) {
                unregisterPropulsion();
                registered = false;
            }
            super.setRemoved();
        }

        @Override
        public BlockEntity getBlockEntity() {
            return this;
        }

        @Override
        public boolean canPerformBurn(int shipMass, double deltaV) {
            return true;
        }

        @Override
        public void addErrors(List<Component> errors) {}

        @Override
        public float getThrust() {
            return 10_000_000;
        }

        @Override
        public int startBurn() {
            return 20;
        }

        @Override
        public int endBurn() {
            return 20;
        }
    }

    /** Sends the player's station to them; called each second for players in orbit. */
    static void syncTo(ServerPlayer player, OrbitalStation station) {
        StationSyncPayload.send(player, station);
    }
}
