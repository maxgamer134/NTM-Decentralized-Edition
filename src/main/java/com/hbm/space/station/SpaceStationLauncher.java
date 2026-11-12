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
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import com.hbm.space.rocket.EntityRideableRocket;
import com.hbm.space.rocket.EntityRideableRocket.RocketState;
import com.hbm.space.rocket.ItemCustomRocket;
import com.hbm.space.rocket.ItemVOTVdrive;
import com.hbm.space.rocket.RocketStruct;
import com.hbm.space.rocket.SpaceLaunchPad;
import com.hbm.space.rocket.SpaceLaunchPad.PadBE;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.space.rocket.SpaceRocketParts;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The orbital station launching port: a docking port that is also a rocket assembly and a pad.
 * A capsule and stages go in, the port fits tanks to what they burn, and once full and given a
 * drive it builds the rocket hanging under it, ready to board and fly. With a station core as the
 * capsule it sends a new station up around a body that has none yet.
 */
public final class SpaceStationLauncher {

    private SpaceStationLauncher() {}

    public static final RegistryHandle<Launcher> BLOCK = Reg.blockItem("orbital_station_launcher", Launcher::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 6F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<LauncherBE> BE = Reg.be("orbital_station_launcher",
                    () -> new BlockEntityType<>(LauncherBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);
    public static final RegistryHandle<MenuType<Menu>> MENU = NeoForgeRegistrar.registerBlockEntityMenu("orbital_station_launcher", LauncherBE.class);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("machine_stardar"), () -> new ItemStack(BLOCK.get()));
    }

    private static boolean isNone(@Nullable Fluid fluid) {
        return fluid == null || fluid == NTMFluids.NONE;
    }

    public static class Launcher extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock, ILookOverlay {
        public Launcher(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LauncherBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).fluidIn().items();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, SpaceStations.PORT_DIMS);
            for (int i = -1; i <= 1; i++) {
                layout.port(2, 1, i).port(-2, 1, i).port(i, 1, 2).port(i, 1, -2);
            }
            return layout.build(this, declaredCaps, SpaceStations.PORT_DIMS, 2, 0);
        }

        @Override
        protected InteractionResult useAtCore(BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
            if (!CelestialBody.inOrbit(level) || !(level.getBlockEntity(core) instanceof LauncherBE launcher)) return InteractionResult.PASS;
            // the outer ring is left alone, to build against
            BlockPos at = hit.getBlockPos();
            if (Math.abs(at.getX() - core.getX()) >= 2 || Math.abs(at.getZ() - core.getZ()) >= 2) return InteractionResult.PASS;
            if (launcher.hasDocked) {
                if (!level.isClientSide() && !launcher.hasRider) launcher.enterCapsule(player);
                return InteractionResult.SUCCESS;
            }
            return super.useAtCore(coreState, level, core, player, hit);
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            if (!CelestialBody.inOrbit(level)) {
                info.title(getName(), 0xffff00, 0x404000)
                        .line(Component.literal("! ! ! ").append(Component.translatable("atmosphere.noOrbit")).append(" ! ! !"), 0xff0000);
                return;
            }
            BlockPos core = overlayCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof LauncherBE pad)) return;
            if (pad.hasDocked) {
                if (pad.hasRider) info.line(Component.translatable("station.occupiedRocket").withStyle(ChatFormatting.YELLOW));
                else info.line(Component.translatable("station.enterRocket"));
                info.title(getName(), 0xffff00, 0x404000);
                return;
            }
            if (pad.rocket == null || !pad.rocket.validate()) return;
            boolean any = pad.solidFuel.max > 0;
            for (FluidTankNTM tank : pad.tanks) any |= !isNone(tank.getTankType());
            if (!any) return;
            info.line(Component.literal("Required fuels:"));
            for (FluidTankNTM tank : pad.tanks) {
                if (isNone(tank.getTankType())) continue;
                info.input(Component.empty().append(tank.getTankType().getFluidType().getDescription())
                        .append(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB"));
            }
            if (pad.solidFuel.max > 0)
                info.input(Component.empty().append(ModItems.ROCKET_FUEL.get().getDescription())
                        .append(": " + pad.solidFuel.level + "/" + pad.solidFuel.max + "kg"));
            info.title(getName(), 0xffff00, 0x404000);
        }
    }

    public static class LauncherBE extends BlockEntityMachineBase
            implements IControlReceiver, IGUIProvider, SyncUnitSchema, IFluidHandlerMK2, FluidTankAccess {
        public static final int DRIVE = 0, FUEL_IN = 1, FUEL_OUT = 2, CAPSULE = 3, STAGES = 4;
        public static final int DRIVES = STAGES + RocketStruct.MAX_STAGES * 3;
        public static final int SLOTS = DRIVES + RocketStruct.MAX_STAGES * 2;

        public RocketStruct rocket = new RocketStruct();
        private @Nullable EntityRideableRocket docked;
        public final FluidTankNTM[] tanks = new FluidTankNTM[RocketStruct.MAX_STAGES * 2];
        public final SpaceLaunchPad.SolidFuelTank solidFuel = new SpaceLaunchPad.SolidFuelTank();
        /** The stage the screen shows; shared by everyone looking, as in NTM:Space. */
        public int currentStage;
        public boolean hasDocked, hasRider;
        public float rot, prevRot;

        public LauncherBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, SLOTS);
            for (int i = 0; i < tanks.length; i++) tanks[i] = new FluidTankNTM(NTMFluids.NONE, PadBE.TANK_SIZE);
        }

        public static boolean isDriveSlot(int slot) {
            return slot >= DRIVES;
        }

        public ItemStack fromDrive(int stage) {
            return getItem(DRIVES + stage * 2);
        }

        public ItemStack toDrive(int stage) {
            return getItem(DRIVES + stage * 2 + 1);
        }

        @Override
        public boolean dropsSlot(int slot) {
            return !isDriveSlot(slot);
        }

        @Override
        public int getMaxStackSize() {
            return 8;
        }

        @Override
        public void tickServer() {
            if (level == null || !CelestialBody.inOrbit(level)) return;

            // construction, as the assembly building does it
            rocket = new RocketStruct(getItem(CAPSULE));
            for (int i = STAGES; i < DRIVES; i += 3) {
                if (getItem(i).isEmpty() && getItem(i + 1).isEmpty() && getItem(i + 2).isEmpty()) {
                    if (i + 3 < DRIVES && !(getItem(i + 3).isEmpty() && getItem(i + 4).isEmpty() && getItem(i + 5).isEmpty())) {
                        for (int k = 0; k < 3; k++) {
                            setItem(i + k, getItem(i + 3 + k));
                            setItem(i + 3 + k, ItemStack.EMPTY);
                        }
                    } else {
                        break;
                    }
                }
                rocket.addStage(getItem(i), getItem(i + 1), getItem(i + 2));
            }

            // launching, as the pad does it
            if (rocket.validate() && PadBE.updateStorageTanks(rocket, tanks, solidFuel, false)) setChanged();
            for (FluidTankNTM tank : tanks) if (!isNone(tank.getTankType())) tank.loadTank(FUEL_IN, FUEL_OUT, inventory);
            ItemStack solid = getItem(FUEL_IN);
            if (!solid.isEmpty() && solid.is(ModItems.ROCKET_FUEL.get()) && solidFuel.level < solidFuel.max) {
                solid.shrink(1);
                solidFuel.level = Math.min(solidFuel.level + 250, solidFuel.max);
                setChanged();
            }

            if (docked != null && (docked.isRemoved() || docked.getState() == RocketState.UNDOCKING)) docked = null;
            hasDocked = docked != null;
            hasRider = hasDocked && docked.isVehicle();
            networkPackNT(250);
        }

        @Override
        public void tickClient() {
            if (level == null || !CelestialBody.inOrbit(level)) return;
            prevRot = rot;
            rot = hasDocked ? Math.min(rot + 2.25F, 90F) : Math.max(rot - 2.25F, 0F);
        }

        public void enterCapsule(Player player) {
            if (docked != null && !docked.isVehicle()) player.startRiding(docked);
        }

        /** Takes a rocket hanging under the port, as one loaded with the world does. */
        public void dockRocket(EntityRideableRocket rocket) {
            docked = rocket;
        }

        // launch checks

        private boolean hasDrive() {
            return getItem(DRIVE).getItem() instanceof ItemVOTVdrive;
        }

        private boolean areTanksFull() {
            for (FluidTankNTM tank : tanks) if (!isNone(tank.getTankType()) && tank.getFill() < tank.getMaxFill()) return false;
            return solidFuel.level >= solidFuel.max;
        }

        private static boolean isStation(RocketStruct rocket) {
            return rocket.capsule == SpaceRocketParts.RP_STATION_CORE_20.get();
        }

        /** Where the port is: in orbit of the body its station circles. */
        private ItemVOTVdrive.Target from() {
            OrbitalStation station = OrbitalStation.getStationFromPosition(level, worldPosition.getX(), worldPosition.getZ());
            return new ItemVOTVdrive.Target(station.orbiting, true, station.hasStation);
        }

        private boolean canReachDestination() {
            ItemStack drive = getItem(DRIVE);
            if (!ItemVOTVdrive.getProcessed(drive)) return false;
            if (ItemVOTVdrive.getBody(drive) == SolarSystem.Body.ORBIT
                    && rocket.capsule != SpaceRocketParts.RP_CAPSULE_20.get() && !isStation(rocket)) return false;
            ItemVOTVdrive.Target from = from(), to = ItemVOTVdrive.getTarget(drive, level);
            if (to.isValid() == isStation(rocket)) return false;
            if (from.body() == null || to.body() == null) return false;
            return rocket.hasSufficientFuel(from.body(), to.body(), from.inOrbit(), to.inOrbit());
        }

        public boolean canLaunch() {
            return rocket.validate() && hasDrive() && areTanksFull() && canReachDestination();
        }

        public void launch(@Nullable Player player) {
            if (level == null || !canLaunch()) return;
            EntityRideableRocket entity = EntityRideableRocket.of(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5,
                    worldPosition.getZ() + 0.5, ItemCustomRocket.build(rocket)).withProgram(getItem(DRIVE)).launchedBy(player);
            entity.setPos(entity.getX(), entity.getY() - entity.getBbHeight(), entity.getZ());
            level.addFreshEntity(entity);
            for (FluidTankNTM tank : tanks) {
                tank.setTankType(NTMFluids.NONE);
                tank.changeTankSize(PadBE.TANK_SIZE);
                tank.setFill(0);
            }
            solidFuel.level = solidFuel.max = 0;
            setItem(DRIVE, ItemStack.EMPTY);
            for (int i = CAPSULE; i < SLOTS; i++) setItem(i, ItemStack.EMPTY);
            dockRocket(entity);
            setChanged();
        }

        /** The stage on show's own issues, by the drives it was given. */
        public List<Component> stageIssues(Level level) {
            int stage = Math.max(rocket.stages.size() - 1 - currentStage, 0);
            ItemVOTVdrive.Target from = ItemVOTVdrive.getTarget(fromDrive(currentStage), level);
            ItemVOTVdrive.Target to = ItemVOTVdrive.getTarget(toDrive(currentStage), level);
            return rocket.findIssues(stage, from.body(), to.body(), from.inOrbit(), to.inOrbit());
        }

        /** What stands between the rocket and launch; empty without a whole rocket. Works on either side. */
        public List<Component> findIssues() {
            List<Component> issues = new ArrayList<>();
            if (!rocket.validate()) return issues;
            PadBE.findTankIssues(issues, tanks, solidFuel);
            if (PadBE.findDriveIssues(issues, rocket, getItem(DRIVE))) return issues;
            PadBE.findTravelIssues(issues, rocket, from(), ItemVOTVdrive.getTarget(getItem(DRIVE), level));
            return issues;
        }

        // fluids

        @Override
        public boolean canReceiveFluid(Fluid type) {
            for (FluidTankNTM tank : tanks) if (!isNone(tank.getTankType()) && tank.joins(type)) return true;
            return false;
        }

        @Override
        public void forEachTank(Consumer<FluidTankNTM> visitor) {
            for (FluidTankNTM tank : tanks) visitor.accept(tank);
        }

        @Override
        public long getDemand(Fluid type, int pressure) {
            if (pressure != 0) return 0;
            long headroom = 0;
            for (FluidTankNTM tank : tanks)
                if (!isNone(tank.getTankType()) && tank.accepts(type)) headroom += tank.getMaxFill() - tank.getFill();
            return headroom;
        }

        @Override
        public long transferFluid(Fluid type, int pressure, long amount) {
            if (pressure != 0) return amount;
            long remaining = amount;
            for (FluidTankNTM tank : tanks) {
                if (remaining <= 0) break;
                if (!isNone(tank.getTankType()) && tank.accepts(type))
                    remaining -= tank.fill(type, (int) Math.min(remaining, Integer.MAX_VALUE), true);
            }
            if (remaining != amount) markChanged();
            return remaining;
        }

        @Override
        public int tankCount() {
            return tanks.length;
        }

        @Override
        public FluidTankNTM tank(int index) {
            return tanks[index];
        }

        // control, sync and saving

        @Override
        public boolean hasPermission(Player player) {
            return stillValid(player);
        }

        @Override
        public void receiveControl(CompoundTag data) {
            receiveControl(null, data);
        }

        @Override
        public void receiveControl(@Nullable Player player, CompoundTag data) {
            if (data.getBoolean("launch")) launch(player);
            if (data.contains("stage")) currentStage = Math.clamp(data.getInt("stage"), 0, RocketStruct.MAX_STAGES - 1);
            setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return Menu.accepts(slot, stack);
        }

        @Override
        public long syncUnitMask() {
            return super.syncUnitMask() | 0xFL;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            switch (unit) {
                case 0 -> rocket.write(new FriendlyByteBuf(output));
                case 1 -> {
                    output.writeByte(currentStage);
                    output.writeBoolean(hasDocked);
                    output.writeBoolean(hasRider);
                }
                case 2 -> {
                    output.writeInt(solidFuel.level);
                    output.writeInt(solidFuel.max);
                }
                case 3 -> {
                    for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
                }
                default -> super.writeSyncUnit(unit, output);
            }
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            switch (unit) {
                case 0 -> rocket = RocketStruct.read(new FriendlyByteBuf(input));
                case 1 -> {
                    currentStage = input.readByte();
                    hasDocked = input.readBoolean();
                    hasRider = input.readBoolean();
                }
                case 2 -> {
                    solidFuel.level = input.readInt();
                    solidFuel.max = input.readInt();
                }
                case 3 -> {
                    for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
                }
                default -> super.readSyncUnit(unit, input);
            }
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            solidFuel.level = input.getIntOr("solid", 0);
            solidFuel.max = input.getIntOr("maxSolid", 0);
            for (int i = 0; i < tanks.length; i++) input.child("t" + i).ifPresent(tanks[i]::deserialize);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putInt("solid", solidFuel.level);
            output.putInt("maxSolid", solidFuel.max);
            for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("t" + i));
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.orbitalStationLauncher");
        }

        @Override
        public Component getDisplayName() {
            return getName();
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new Menu(containerId, playerInventory, this);
        }
    }

    public static class Menu extends BlockEntityMenu<LauncherBE> {
        public Menu(int containerId, Inventory playerInv, LauncherBE be) {
            super(MENU.get(), containerId, be);
            checkContainerSize(be, LauncherBE.SLOTS);
            addSlot(new Part(be, LauncherBE.DRIVE, 42, 55, -1));
            addSlot(new Slot(be, LauncherBE.FUEL_IN, 42, 73));
            addSlot(new Slot(be, LauncherBE.FUEL_OUT, 42, 91));
            addSlot(new Part(be, LauncherBE.CAPSULE, 18, 13, -1));
            for (int i = 0; i < RocketStruct.MAX_STAGES; i++) {
                addSlot(new Part(be, LauncherBE.STAGES + i * 3, 18, 44, i));
                addSlot(new Part(be, LauncherBE.STAGES + i * 3 + 1, 18, 62, i));
                addSlot(new Part(be, LauncherBE.STAGES + i * 3 + 2, 18, 80, i));
            }
            for (int i = 0; i < RocketStruct.MAX_STAGES; i++) {
                addSlot(new Part(be, LauncherBE.DRIVES + i * 2, 161, 54, i));
                addSlot(new Part(be, LauncherBE.DRIVES + i * 2 + 1, 170, 87, i));
            }
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++)
                    addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 142 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInv, col, 8 + col * 18, 200));
        }

        static boolean accepts(int slot, ItemStack stack) {
            if (slot == LauncherBE.DRIVE || LauncherBE.isDriveSlot(slot)) return stack.getItem() instanceof ItemVOTVdrive;
            if (slot == LauncherBE.FUEL_IN || slot == LauncherBE.FUEL_OUT) return true;
            if (slot == LauncherBE.CAPSULE && com.hbm.saveddata.satellites.SatelliteType.fromStack(stack) != null) return true;
            ItemCustomMissilePart part = ItemCustomMissilePart.part(stack);
            if (part == null || part.type == null) return false;
            if (slot == LauncherBE.CAPSULE)
                return part.type == PartType.WARHEAD && part != SpaceRocketParts.RP_POD_20.get()
                        && (part.warheadType() == SpaceRocketParts.APOLLO || part.warheadType() == SpaceRocketParts.SATELLITE);
            return part.type == switch ((slot - LauncherBE.STAGES) % 3) {
                case 0 -> PartType.FUSELAGE;
                case 1 -> PartType.FINS;
                default -> PartType.THRUSTER;
            };
        }

        /** A slot of one stage, shown only while the screen is on that stage. */
        private class Part extends Slot {
            private final int stage;

            Part(LauncherBE be, int index, int x, int y, int stage) {
                super(be, index, x, y);
                this.stage = stage;
            }

            @Override
            public boolean isActive() {
                return stage < 0 || blockEntity().currentStage == stage;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return isActive() && accepts(getContainerSlot(), stack);
            }

            @Override
            public boolean mayPickup(Player player) {
                return !LauncherBE.isDriveSlot(getContainerSlot());
            }

            @Override
            public int getMaxStackSize() {
                return getContainerSlot() == LauncherBE.DRIVE ? 1 : super.getMaxStackSize();
            }
        }

        @Override
        public void clicked(int slotId, int button, ClickType input, Player player) {
            // the stage drive slots only take a copy of the drive, to read it
            if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof Part part
                    && LauncherBE.isDriveSlot(part.getContainerSlot())) {
                if (input != ClickType.PICKUP || !part.isActive()) return;
                ItemStack held = getCarried();
                if (!held.isEmpty() && !accepts(part.getContainerSlot(), held)) return;
                part.set(held.isEmpty() ? ItemStack.EMPTY : held.copyWithCount(1));
                return;
            }
            super.clicked(slotId, button, input, player);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return quickMove(player, index, false, stack -> index < LauncherBE.SLOTS
                    ? moveItemStackTo(stack, LauncherBE.SLOTS, slots.size(), true)
                    : moveItemStackTo(stack, 0, LauncherBE.DRIVES, false));
        }
    }
}
