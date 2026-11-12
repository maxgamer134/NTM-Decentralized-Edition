// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.backport.TooltipDisplay;
import com.hbm.backport.compat.ItemCompat;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.capability.NtmCapabilities;
import com.hbm.capability.port.FluidPort;
import com.hbm.capability.port.IPortHost;
import com.hbm.capability.port.PortView;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Rocket;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.space.station.OrbitalStation;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jspecify.annotations.Nullable;

/**
 * The rocket transporter pad: two linked pads on different bodies trade a small rocket back and
 * forth, carrying the items and fluids put into one pad to the other pad's outputs. Every trip
 * burns propellant from the two fuel tanks, as much as the delta-v between the bodies costs for
 * the cargo's mass. The pads keep their chunks loaded. Linked with the transporter pad linker.
 */
public final class SpaceTransporter {

    private SpaceTransporter() {}

    /** NTM's {up, down, north, south, west, east} placement box. */
    public static final int[] DIMS = {1, 0, 1, 1, 1, 2};

    public static final RegistryHandle<TransporterBlock> BLOCK = Reg.blockItem("transporter_rocket", TransporterBlock::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<TransporterBE> BE = Reg.be("transporter_rocket",
                    () -> new BlockEntityType<>(TransporterBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);
    public static final RegistryHandle<MenuType<Menu>> MENU = NeoForgeRegistrar.registerBlockEntityMenu("transporter_rocket", TransporterBE.class);
    public static final RegistryHandle<Item> LINKER =
            Reg.item("transporter_linker", Linker::new, () -> new Item.Properties().stacksTo(1));

    /** Opens the linker screen: the pad clicked and the pads saved on the linker. Set by the client. */
    public static BiConsumer<TransporterInfo, List<TransporterInfo>> OPEN_LINKER = (from, saved) -> {};

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_htr3"), () -> new ItemStack(BLOCK.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.CONTROL, Library.id("screwdriver_desh"), () -> new ItemStack(LINKER.get()));
    }

    static ResourceKey<Level> dim(String id) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(id));
    }

    static @Nullable TransporterBE coreAt(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TransporterBE be) return be;
        BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(level, pos);
        return owner != null && level.getBlockEntity(owner.pos()) instanceof TransporterBE be ? be : null;
    }

    /** A pad, comparable by dimension and position, as the linker and the link sync carry it. */
    public static final class TransporterInfo {
        public final String name;
        public final ResourceKey<Level> dimension;
        public final BlockPos pos;
        public @Nullable TransporterInfo linkedTo;

        public TransporterInfo(String name, ResourceKey<Level> dimension, BlockPos pos) {
            this.name = name;
            this.dimension = dimension;
            this.pos = pos;
        }

        public static TransporterInfo from(TransporterBE be) {
            TransporterInfo info = new TransporterInfo(be.getTransporterName(), Objects.requireNonNull(be.getLevel()).dimension(), be.getBlockPos());
            info.linkedTo = be.getLinkedTransporter();
            return info;
        }

        /** The body the pad stands on, for the screen's planet icon. */
        public ResourceLocation planet() {
            return CelestialBody.getBody(dimension).texture;
        }

        public CompoundTag save() {
            return save(true);
        }

        private CompoundTag save(boolean recurse) {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", name);
            tag.putString("dim", dimension.location().toString());
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            if (recurse && linkedTo != null) tag.put("linked", linkedTo.save(false));
            return tag;
        }

        public static TransporterInfo load(CompoundTag tag) {
            TransporterInfo info = new TransporterInfo(tag.getString("name"), dim(tag.getString("dim")),
                    new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")));
            if (tag.contains("linked")) info.linkedTo = load(tag.getCompound("linked"));
            return info;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, pos);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof TransporterInfo info && dimension == info.dimension && pos.equals(info.pos);
        }
    }

    public static class TransporterBlock extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {
        public TransporterBlock(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TransporterBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).fluidIn().fluidOut();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts()
                    .box(0, 0, 0, 0, 0, 1, 1, 1, 2)
                    .box(0, 0, 0, 1, 0, 1, 1, 0, 0)
                    .box(0, 0, 0, 1, 0, 1, 1, -2, 2);
            int noDown = MASK_ALL & ~MASK_DOWN;
            // the four cargo fluid ports on the corners, and the two propellant ports on the side
            for (int x : new int[] {-1, 1}) for (int z : new int[] {-1, 1}) l.port(x, 0, z, noDown);
            l.port(2, 0, -1, noDown);
            l.port(2, 0, 1, noDown);
            return l.build(this, declaredCaps, DIMS, 1, 0);
        }
    }

    public static class TransporterBE extends BlockEntityMachineBase
            implements IGUIProvider, IControlReceiver, IFluidHandlerMK2, FluidFlushSender, FluidTankAccess, SyncUnitSchema, IPortHost {

        public static final int INPUT_SLOTS = 8, OUTPUT_SLOTS = 16, SLOT_COUNT = 22;
        public static final int INPUT_TANKS = 4, OUTPUT_TANKS = 8;
        private static final int MASS_MULT = 100;

        public final FluidTankNTM[] tanks = new FluidTankNTM[10];
        private final FluidTankNTM[] sending = new FluidTankNTM[OUTPUT_TANKS - INPUT_TANKS];
        private final FluidFlushOutputs flush = new FluidFlushOutputs();
        /** The four corner ports: cargo fluid in and out. */
        private final FluidPort cargoPort;
        /** The two side ports: propellant in only, ahead of other receivers, as in NTM:Space. */
        private final FuelPort fuelPort = new FuelPort();

        private String name = "Transporter";
        private @Nullable ResourceKey<Level> linkedDim;
        private @Nullable BlockPos linkedPos;
        private @Nullable TransporterBE linked;
        private boolean chunkForced;

        public boolean hasRocket = true;
        public int launchTicks;
        public int threshold;

        public TransporterBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, SLOT_COUNT);
            for (int i = 0; i < OUTPUT_TANKS; i++) tanks[i] = new FluidTankNTM(null, 128_000);
            tanks[8] = new FluidTankNTM(NTMFluids.byName("HYDROGEN"), 64_000);
            tanks[9] = new FluidTankNTM(NTMFluids.byName("OXYGEN"), 64_000);
            System.arraycopy(tanks, INPUT_TANKS, sending, 0, sending.length);
            cargoPort = FluidPort.of(tanks, new int[] {0, 1, 2, 3}, new int[] {4, 5, 6, 7}, this::setChanged);
        }

        private boolean isFuelCell(BlockPos cell) {
            return cell.getY() == worldPosition.getY() && (cell.equals(local(2, -1)) || cell.equals(local(2, 1)));
        }

        @Override
        public @Nullable FluidPort fluidAccess(BlockPos cell, Direction side) {
            return isFuelCell(cell) ? null : cargoPort;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <D> @Nullable D portAccess(com.hbm.capability.port.PortDomain<D> domain, BlockPos cell, Direction side) {
            if (domain == com.hbm.capability.port.PortDomain.FLUID && isFuelCell(cell)) return (D) fuelPort;
            return IPortHost.super.portAccess(domain, cell, side);
        }

        /** Propellant intake: fills only the two fuel tanks, never gives anything back. */
        private final class FuelPort implements PortView, IFluidHandlerMK2 {
            @Override
            public <T> @Nullable T as(Class<T> type, NtmCapabilities.CapRole role) {
                return role == NtmCapabilities.CapRole.FLUID_IN && type.isInstance(this) ? type.cast(this) : null;
            }

            @Override
            public ConnectionPriority getFluidPriority() {
                return ConnectionPriority.HIGH;
            }

            @Override
            public boolean canReceiveFluid(Fluid type) {
                return tanks[8].joins(type) || tanks[9].joins(type);
            }

            @Override
            public long getDemand(Fluid type, int pressure) {
                if (pressure != 0) return 0;
                long demand = 0;
                for (int i = OUTPUT_TANKS; i < tanks.length; i++)
                    if (tanks[i].accepts(type)) demand += tanks[i].getMaxFill() - tanks[i].getFill();
                return demand;
            }

            @Override
            public long transferFluid(Fluid type, int pressure, long amount) {
                if (pressure != 0) return amount;
                long left = amount;
                for (int i = OUTPUT_TANKS; i < tanks.length && left > 0; i++) {
                    if (!tanks[i].accepts(type)) continue;
                    left -= tanks[i].fill(type, (int) Math.min(left, Integer.MAX_VALUE), true);
                }
                if (left != amount) setChanged();
                return left;
            }
        }

        public Direction facing() {
            return getBlockState().getValue(BlockMultiblockCore.FACING);
        }

        private BlockPos local(int x, int z) {
            Direction f = facing();
            return worldPosition.offset(MultiblockMaskTable.worldX(x, 0, z, f), 0, MultiblockMaskTable.worldZ(x, 0, z, f));
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            if (!chunkForced) {
                server.setChunkForced(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, true);
                chunkForced = true;
            }

            // tank types from the identifiers, and fills split evenly between matching tanks
            for (int i = 0; i < INPUT_TANKS; i++) {
                tanks[i].setType(OUTPUT_SLOTS + i, OUTPUT_SLOTS + i, inventory);
                for (int o = i + 1; o < INPUT_TANKS; o++) splitFill(tanks[i], tanks[o]);
            }
            for (int i = INPUT_TANKS; i < OUTPUT_TANKS; i++)
                for (int o = i + 1; o < OUTPUT_TANKS; o++) splitFill(tanks[i], tanks[o]);
            tanks[8].setType(20, 20, inventory);
            tanks[9].setType(21, 21, inventory);

            Direction f = facing();
            tryLoad(server, local(0, -2), f);
            tryUnload(server, local(0, 2), f.getOpposite());

            fetchLinkedTransporter();

            if (linked != null && canSend(linked)) {
                int sentItems = 0;
                int sentFluid = 0;
                for (int i = 0; i < INPUT_SLOTS; i++) {
                    ItemStack stack = inventory.get(i);
                    if (stack.isEmpty()) continue;
                    int before = stack.getCount();
                    ItemStack left = linked.addToOutputs(stack);
                    inventory.set(i, left);
                    sentItems += before - left.getCount();
                }
                for (int i = 0; i < INPUT_TANKS; i++) {
                    FluidTankNTM from = tanks[i];
                    FluidTankNTM to = linked.tanks[i + INPUT_TANKS];
                    if (to.getTankType() != from.getTankType()) to.setTankType(from.getTankType());
                    Fluid fluid = from.getFluid();
                    int amount = Math.min(from.getFill(), to.getMaxFill() - to.getFill());
                    if (fluid == null || amount <= 0) continue;
                    int moved = to.fill(fluid, amount, true);
                    from.drain(moved, true);
                    sentFluid += moved;
                }
                hasSent(linked, sentItems + sentFluid / 1000);
                setChanged();
                linked.setChanged();
            }

            // if the two pads disagree about who has the rocket, whichever ticks first fixes it
            if (linked != null && hasRocket == linked.hasRocket) hasRocket = !hasRocket;
            stepLaunch();
            flush.provide(server, this);
            networkPackNT(250);
        }

        @Override
        public void tickClient() {
            stepLaunch();
            if (level == null || launchTicks <= 0 || launchTicks >= 100) return;
            double x = worldPosition.getX() + 0.5, y = worldPosition.getY(), z = worldPosition.getZ() + 0.5;
            com.hbm.client.ClientEffects.spawnGasFlame(level, x, y + 0.5 + launchTicks, z, 0, -1, 0, 1F);
            if (launchTicks < 10)
                com.hbm.client.ClientEffects.spawnSmokeShock(level, x, y + 0.5, z, 1 + level.random.nextInt(3), 1 + level.random.nextGaussian());
        }

        private void stepLaunch() {
            launchTicks = Mth.clamp(launchTicks + (hasRocket ? -1 : 1), hasRocket ? -20 : 0, 100);
        }

        /** Splitting is commutative: the order of the two tanks doesn't matter. */
        private static void splitFill(FluidTankNTM in, FluidTankNTM out) {
            if (in.getTankType() != out.getTankType() || in.getTankType() == null) return;
            Fluid fluid = in.getFill() > 0 ? in.getFluid() : out.getFluid();
            if (fluid == null) return;
            if (in.getFill() > 0 && out.getFill() > 0 && in.getFluid() != out.getFluid()) return;
            int fill = in.getFill() + out.getFill();
            float total = in.getMaxFill() + out.getMaxFill();
            int inFill = Mth.ceil(in.getMaxFill() / total * fill);
            int outFill = Mth.floor(out.getMaxFill() / total * fill);
            // cap filling (this will make 1mB of fluid in rare cases), as in NTM:Space
            if (outFill == out.getMaxFill() - 1) outFill = out.getMaxFill();
            setAmount(in, fluid, inFill);
            setAmount(out, fluid, outFill);
        }

        private static void setAmount(FluidTankNTM tank, Fluid fluid, int amount) {
            if (tank.getFill() == amount) return;
            if (tank.getFill() > 0) {
                tank.setFill(amount);
                return;
            }
            tank.fill(fluid, amount, true);
        }

        /** Puts a stack into this pad's output slots; returns what didn't fit. */
        ItemStack addToOutputs(ItemStack stack) {
            ItemStack left = stack.copy();
            for (int i = INPUT_SLOTS; i < OUTPUT_SLOTS && !left.isEmpty(); i++) {
                ItemStack there = inventory.get(i);
                if (there.isEmpty() || !ItemStack.isSameItemSameComponents(there, left)) continue;
                int move = Math.min(left.getCount(), there.getMaxStackSize() - there.getCount());
                if (move <= 0) continue;
                there.grow(move);
                left.shrink(move);
            }
            for (int i = INPUT_SLOTS; i < OUTPUT_SLOTS && !left.isEmpty(); i++) {
                if (!inventory.get(i).isEmpty()) continue;
                inventory.set(i, left.copy());
                left = ItemStack.EMPTY;
            }
            return left;
        }

        /** Takes one item from the inventory behind the pad into the input slots. */
        private void tryLoad(ServerLevel server, BlockPos pos, Direction side) {
            IItemHandler handler = server.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            if (handler == null) return;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack offered = handler.extractItem(slot, 1, true);
                if (offered.isEmpty()) continue;
                int target = -1;
                for (int j = 0; j < INPUT_SLOTS && target < 0; j++) {
                    ItemStack there = inventory.get(j);
                    if (!there.isEmpty() && there.getCount() < there.getMaxStackSize() && ItemStack.isSameItemSameComponents(there, offered)) target = j;
                }
                for (int j = 0; j < INPUT_SLOTS && target < 0; j++) if (inventory.get(j).isEmpty()) target = j;
                if (target < 0) continue;
                ItemStack taken = handler.extractItem(slot, 1, false);
                if (taken.isEmpty()) continue;
                if (inventory.get(target).isEmpty()) inventory.set(target, taken);
                else inventory.get(target).grow(1);
                setChanged();
                return;
            }
        }

        /** Gives one item from the output slots to the inventory in front of the pad. */
        private void tryUnload(ServerLevel server, BlockPos pos, Direction side) {
            IItemHandler handler = server.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            if (handler == null) return;
            for (int i = INPUT_SLOTS; i < OUTPUT_SLOTS; i++) {
                ItemStack out = inventory.get(i);
                if (out.isEmpty()) continue;
                ItemStack one = out.copyWithCount(1);
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    if (!handler.insertItem(slot, one, true).isEmpty()) continue;
                    handler.insertItem(slot, one, false);
                    out.shrink(1);
                    setChanged();
                    return;
                }
            }
        }

        public int getThreshold() {
            return threshold == 0 ? 0 : 1 << (threshold - 1);
        }

        /** Items and fluids (per bucket) waiting to be sent, as a "mass" of sorts. */
        protected int itemCount() {
            int count = 0;
            for (int i = 0; i < INPUT_SLOTS; i++) count += inventory.get(i).getCount();
            for (int i = 0; i < INPUT_TANKS; i++) count += tanks[i].getFill() / 1000;
            return count;
        }

        private @Nullable FT_Rocket fuelStats() {
            for (int i = 8; i <= 9; i++) {
                Fluid fluid = tanks[i].getFluid();
                FT_Rocket stats = fluid == null ? null : NTMFluidProperties.getTrait(fluid, FT_Rocket.class);
                if (stats != null) return stats;
            }
            return null;
        }

        private record Target(CelestialBody body, boolean inOrbit) {}

        private static Target target(Level level, BlockPos pos) {
            if (CelestialBody.inOrbit(level)) {
                OrbitalStation station = OrbitalStation.getStationFromPosition(level, pos.getX(), pos.getZ());
                return new Target(station.orbiting != null ? station.orbiting : CelestialBody.getBody(Level.OVERWORLD), true);
            }
            return new Target(CelestialBody.getBody(level), false);
        }

        private int sendCost(TransporterBE to, int mass, FT_Rocket stats) {
            Target from = target(level, worldPosition);
            Target dest = target(to.level, to.worldPosition);
            return Math.min(64_000, SolarSystem.getCostBetween(from.body, dest.body, mass * MASS_MULT, (int) stats.getThrust(),
                    stats.getISP(), from.inOrbit, dest.inOrbit));
        }

        /** The rocket is here and has landed, the far pad has seen it off, and there's fuel for the trip. */
        private boolean canSend(TransporterBE to) {
            if (launchTicks > -20) return false;
            if (to.launchTicks < 100) return false;
            if (!hasRocket) return false;
            int mass = itemCount();
            if (mass < getThreshold()) return false;
            FT_Rocket stats = fuelStats();
            if (stats == null) return false;
            int cost = sendCost(to, mass, stats);
            return tanks[8].getFill() >= cost && tanks[9].getFill() >= cost;
        }

        /** Burns fuel for what was actually sent, and hands the rocket over. */
        private void hasSent(TransporterBE to, int quantitySent) {
            FT_Rocket stats = fuelStats();
            if (stats != null) {
                int cost = sendCost(to, quantitySent, stats);
                tanks[8].setFill(tanks[8].getFill() - cost);
                tanks[9].setFill(tanks[9].getFill() - cost);
            }
            hasRocket = false;
            to.hasRocket = true;
        }

        private void fetchLinkedTransporter() {
            if (linked != null && linked.isRemoved()) linked = null;
            if (linked != null || linkedDim == null || linkedPos == null || !(level instanceof ServerLevel server)) return;
            ServerLevel there = server.getServer().getLevel(linkedDim);
            if (there == null || !there.isLoaded(linkedPos)) return;
            if (there.getBlockEntity(linkedPos) instanceof TransporterBE be && be != this) linked = be;
        }

        public String getTransporterName() {
            return name;
        }

        public @Nullable TransporterInfo getLinkedTransporter() {
            if (linkedDim == null || linkedPos == null) return null;
            return new TransporterInfo("Linked Transporter", linkedDim, linkedPos);
        }

        private void clearLink() {
            linked = null;
            linkedDim = null;
            linkedPos = null;
            setChanged();
        }

        public void unlinkTransporter() {
            fetchLinkedTransporter();
            if (linked != null) linked.clearLink();
            clearLink();
        }

        /** Commutative: links and unlinks its pair too. */
        @Override
        public void receiveControl(CompoundTag data) {
            if (data.contains("name")) name = data.getString("name");
            if (data.contains("threshold")) threshold = Mth.clamp(data.getInt("threshold"), 0, 10);
            if (data.contains("unlink")) unlinkTransporter();
            if (data.contains("linkedTo") && level != null) {
                fetchLinkedTransporter();
                if (linked != null) linked.clearLink();
                linked = null;
                int[] coords = data.getIntArray("linkedTo");
                if (coords.length == 3) {
                    linkedDim = dim(data.getString("dimension"));
                    linkedPos = new BlockPos(coords[0], coords[1], coords[2]);
                    fetchLinkedTransporter();
                    if (linked != null) {
                        // the far pad drops whatever it was linked to before
                        TransporterBE old = linked;
                        old.fetchLinkedTransporter();
                        if (old.linked != null && old.linked != this) old.linked.clearLink();
                        old.linked = this;
                        old.linkedDim = level.dimension();
                        old.linkedPos = worldPosition;
                        old.setChanged();
                        hasRocket = true;
                        old.hasRocket = false;
                    }
                }
            }
            setChanged();
        }

        @Override
        public boolean hasPermission(Player player) {
            return stillValid(player);
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            if (level instanceof ServerLevel server) {
                unlinkTransporter();
                server.setChunkForced(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, false);
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS || slot >= OUTPUT_SLOTS;
        }

        // fluids: cargo into the four input tanks and propellant into the fuel tanks, cargo out of the output tanks

        private boolean receives(int i) {
            return i < INPUT_TANKS || i >= OUTPUT_TANKS;
        }

        @Override
        public boolean canReceiveFluid(Fluid type) {
            for (int i = 0; i < tanks.length; i++) if (receives(i) && tanks[i].joins(type)) return true;
            return false;
        }

        @Override
        public long getDemand(Fluid type, int pressure) {
            if (pressure != 0) return 0;
            long demand = 0;
            for (int i = 0; i < tanks.length; i++)
                if (receives(i) && tanks[i].accepts(type)) demand += tanks[i].getMaxFill() - tanks[i].getFill();
            return demand;
        }

        @Override
        public long transferFluid(Fluid type, int pressure, long amount) {
            if (pressure != 0) return amount;
            long left = amount;
            for (int i = 0; i < tanks.length && left > 0; i++) {
                if (!receives(i) || !tanks[i].accepts(type)) continue;
                left -= tanks[i].fill(type, (int) Math.min(left, Integer.MAX_VALUE), true);
            }
            if (left != amount) setChanged();
            return left;
        }

        @Override
        public FluidTankNTM[] getSendingTanks() {
            return sending;
        }

        @Override
        public void declareFlush(FlushLanes out) {
            for (FluidTankNTM tank : sending) out.add(cargoPort, tank, FlushFaces.activePlane((cell, side) -> !isFuelCell(cell)));
        }

        @Override
        public long getFluidAvailable(Fluid type, int pressure) {
            long total = 0;
            for (FluidTankNTM tank : sending) if (tank.provides(type) && pressure == tank.getPressure()) total += tank.getFill();
            return total;
        }

        @Override
        public void useUpFluid(Fluid type, int pressure, long amount) {
            for (FluidTankNTM tank : sending) {
                if (amount <= 0) break;
                if (!tank.provides(type) || pressure != tank.getPressure()) continue;
                int drained = tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
                amount -= drained;
                if (drained > 0) setChanged();
            }
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
        public Component getDisplayName() {
            return getName();
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new Menu(containerId, playerInventory, this);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.transporter");
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
            output.writeInt(threshold);
            output.writeBoolean(linkedDim != null && linkedPos != null);
            if (linkedDim != null && linkedPos != null) {
                ByteBufCodecs.STRING_UTF8.encode(output, linkedDim.location().toString());
                output.writeLong(linkedPos.asLong());
            }
            for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
            ByteBufCodecs.STRING_UTF8.encode(output, name);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            hasRocket = input.readBoolean();
            threshold = input.readInt();
            if (input.readBoolean()) {
                linkedDim = dim(ByteBufCodecs.STRING_UTF8.decode(input));
                linkedPos = BlockPos.of(input.readLong());
            } else {
                linkedDim = null;
                linkedPos = null;
            }
            for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
            name = ByteBufCodecs.STRING_UTF8.decode(input);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            name = input.getStringOr("name", "Transporter");
            String d = input.getStringOr("linkedDim", "");
            int[] coords = input.getIntArray("linkedTo").orElse(new int[0]);
            linked = null;
            if (!d.isEmpty() && coords.length == 3) {
                linkedDim = dim(d);
                linkedPos = new BlockPos(coords[0], coords[1], coords[2]);
            } else {
                linkedDim = null;
                linkedPos = null;
            }
            for (int i = 0; i < tanks.length; i++) {
                final FluidTankNTM tank = tanks[i];
                input.child("t" + i).ifPresent(tank::deserialize);
            }
            hasRocket = input.getBooleanOr("rocket", true);
            threshold = input.getIntOr("threshold", 0);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putString("name", name);
            if (linkedDim != null && linkedPos != null) {
                output.putString("linkedDim", linkedDim.location().toString());
                output.putIntArray("linkedTo", new int[] {linkedPos.getX(), linkedPos.getY(), linkedPos.getZ()});
            }
            for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("t" + i));
            output.putBoolean("rocket", hasRocket);
            output.putInt("threshold", threshold);
        }
    }

    public static class Menu extends BlockEntityMenu<TransporterBE> {
        public Menu(int containerId, Inventory playerInv, TransporterBE be) {
            super(MENU.get(), containerId, be);
            checkContainerSize(be, TransporterBE.SLOT_COUNT);
            addSlots(be, 0, 8, 34, 2, 4); // input slots
            addSlots(be, 8, 98, 34, 2, 4); // output slots
            addSlots(be, 16, 8, 106, 1, 4); // input fluid identifiers
            addSlots(be, 20, 188, 90, 1, 2); // fuel fluid identifiers
            addSlots(playerInv, 9, 8, 154, 3, 9);
            addSlots(playerInv, 0, 8, 212, 1, 9);
        }

        private void addSlots(net.minecraft.world.Container inv, int from, int x, int y, int rows, int cols) {
            for (int row = 0; row < rows; row++)
                for (int col = 0; col < cols; col++) addSlot(new Slot(inv, col + row * cols + from, x + col * 18, y + row * 18));
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            int size = TransporterBE.SLOT_COUNT;
            return quickMove(player, index, false, stack -> index < size
                    ? moveItemStackTo(stack, size, slots.size(), true)
                    : moveItemStackTo(stack, 0, size, false));
        }
    }

    /** Sneak-click a pad to save it; click a pad to link it to one of the saved pads. */
    public static class Linker extends ItemCompat {
        public Linker(Properties props) {
            super(props);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                Consumer<Component> adder, TooltipFlag flag) {
            adder.accept(Component.literal("Sneak-click to save transporter"));
            adder.accept(Component.literal("Use on transporter to link to a saved transporter"));
        }

        @Override
        public com.hbm.backport.InteractionResult backport$onItemUseFirst(ItemStack stack, UseOnContext context) {
            Level level = context.getLevel();
            Player player = context.getPlayer();
            TransporterBE pad = coreAt(level, context.getClickedPos());
            if (pad == null || player == null) return com.hbm.backport.InteractionResult.PASS;
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    List<TransporterInfo> saved = saved(stack);
                    TransporterInfo info = TransporterInfo.from(pad);
                    saved.remove(info);
                    saved.add(info);
                    store(stack, saved);
                    player.sendSystemMessage(Component.literal("Added transporter to linker"));
                }
            } else if (level.isClientSide()) {
                OPEN_LINKER.accept(TransporterInfo.from(pad), saved(stack));
            }
            return com.hbm.backport.InteractionResult.SUCCESS;
        }

        /** While held, keeps the saved pads' names and links current, for the client's linker screen. */
        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
            if (!(owner instanceof ServerPlayer player) || player.getMainHandItem() != stack || level.getGameTime() % 10 != 0) return;
            List<TransporterInfo> saved = saved(stack);
            List<TransporterInfo> fresh = new ArrayList<>(saved.size());
            for (TransporterInfo info : saved) {
                ServerLevel there = level.getServer().getLevel(info.dimension);
                if (there == null || !there.isLoaded(info.pos)) {
                    fresh.add(info);
                    continue;
                }
                if (there.getBlockEntity(info.pos) instanceof TransporterBE be) fresh.add(TransporterInfo.from(be));
            }
            CompoundTag before = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            ListTag list = new ListTag();
            for (TransporterInfo info : fresh) list.add(info.save());
            if (!list.equals(before.getList("transporters", Tag.TAG_COMPOUND))) store(stack, fresh);
        }

        public static List<TransporterInfo> saved(ItemStack stack) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            ListTag list = tag.getList("transporters", Tag.TAG_COMPOUND);
            List<TransporterInfo> out = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) out.add(TransporterInfo.load(list.getCompound(i)));
            return out;
        }

        private static void store(ItemStack stack, List<TransporterInfo> transporters) {
            ListTag list = new ListTag();
            for (TransporterInfo info : transporters) list.add(info.save());
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put("transporters", list));
        }
    }
}
