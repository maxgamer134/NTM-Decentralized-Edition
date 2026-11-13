// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.rocket;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.backport.InteractionResult;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ClimbBox;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The rocket launch pad: a finished rocket and a destination drive go in, the pad fits one tank
 * per propellant the rocket burns (plus solid fuel), and once full and powered it launches. A
 * service tower rises beside tall rockets, with an arm at capsule height; the tower, its arm and
 * the ladders are their own invisible block, as NTM:Space's pad grew them out of the multiblock.
 */
public final class SpaceLaunchPad {

    private SpaceLaunchPad() {}

    public static final int[] DIMS = {2, 0, 7, 6, 6, 6};

    public static final RegistryHandle<Pad> BLOCK = Reg.blockItem("launch_pad_rocket", Pad::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 6F).requiresCorrectToolForDrops().noOcclusion().forceSolidOn(),
            (block, props) -> new com.hbm.items.DescBlockItem(block, props, 1, com.hbm.items.DescBlockItem.Desc.PLAIN));

    public static final RegistryHandle<Tower> TOWER = Reg.block("launch_pad_rocket_tower", Tower::new,
            () -> BlockBehaviour.Properties.of().strength(-1F, 3_600_000F).noLootTable().noOcclusion()
                    .pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, e) -> false));

    public static final Reg.BeHandle<PadBE> BE = Reg.be("launch_pad_rocket",
                    () -> new BlockEntityType<>(PadBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static final RegistryHandle<MenuType<Menu>> MENU = NeoForgeRegistrar.registerBlockEntityMenu("launch_pad_rocket", PadBE.class);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("machine_rocket_assembly"), () -> new ItemStack(BLOCK.get()));
    }

    /** A tower column, arm cell or ladder; the pad's renderer draws them. Ladders climb on their FACING face. */
    public static class Tower extends Block implements ClimbBox {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        public static final BooleanProperty LADDER = BooleanProperty.create("ladder");
        // a quarter block either side of the face, as NTM:Space's ladder boxes: the tower is solid,
        // so a box inside the block only ever touches the player and never starts the climb
        private static final AABB[] FACES = {
            new AABB(0.25, 0, 0.75, 0.75, 1, 1.25), // south
            new AABB(-0.25, 0, 0.25, 0.25, 1, 0.75), // west
            new AABB(0.25, 0, -0.25, 0.75, 1, 0.25), // north
            new AABB(0.75, 0, 0.25, 1.25, 1, 0.75), // east
        };

        public Tower(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LADDER, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, LADDER);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public @Nullable AABB climbBox(BlockState state) {
            return state.getValue(LADDER) ? FACES[state.getValue(FACING).get2DDataValue()] : null;
        }
    }

    public static class Pad extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock, com.hbm.interfaces.ILookOverlay {
        public Pad(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new PadBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().fluidIn().items().fe();
        }

        @Override
        protected InteractionResult useAtCore(BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
            if (CelestialBody.inOrbit(level)) return InteractionResult.PASS;
            return super.useAtCore(coreState, level, core, player, hit);
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts()
                    .box(0, 0, 0, 2, 0, 6, 6, 4, 4)
                    .box(0, 0, -2, 2, 0, 4, 0, 6, 6)
                    .box(0, 0, 2, 2, 0, 0, 4, 6, 6);
            for (int or = 1; or < 5; or++) for (int oy = 0; oy < 3; oy++) layout.port(or, oy, -7);
            layout.port(-3, 0, -7);
            // the side ladder is its own climbable block
            for (int oy = 0; oy < 3; oy++) layout.remove(SIDE_LADDER_X, oy, LADDER_Z);
            return layout.build(this, declaredCaps, DIMS, 6, 0);
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, com.hbm.interfaces.ILookOverlay.LookInfo info) {
            if (!CelestialBody.inOrbit(level)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            info.line(Component.literal("! ! ! ").append(Component.translatable("atmosphere.yesOrbit")).append(" ! ! !"),
                    com.hbm.util.BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
        }
    }

    static final int SIDE_LADDER_X = 6, MAIN_LADDER_X = 2, LADDER_Z = -5;
    private static final int[][] TOWER_CELLS = {{2, -4}, {2, -5}, {2, -6}, {3, -4}, {4, -4}, {3, -6}, {4, -6}};
    private static final int[][] ARM_CELLS;

    static {
        List<int[]> arm = new ArrayList<>();
        arm.add(new int[] {2, -3});
        for (int x = -1; x <= 1; x++) for (int z = -1; z >= -6; z--) arm.add(new int[] {x, z});
        ARM_CELLS = arm.toArray(new int[0][]);
    }

    /** A rocket's solid fuel, in kg. */
    public static class SolidFuelTank {
        public int level;
        public int max;
    }

    public static class PadBE extends BlockEntityMachineBase
            implements IControlReceiver, IGUIProvider, SyncUnitSchema, IEnergyHandlerMK2, IFluidHandlerMK2, FluidTankAccess {
        public static final int ROCKET = 0, DRIVE = 1, BATTERY = 2, FUEL_IN = 3, FUEL_OUT = 4, SLOTS = 5;
        public static final long MAX_POWER = 100_000;
        public static final int TANK_SIZE = 64_000;

        public long power;
        public final SolidFuelTank solidFuel = new SolidFuelTank();
        public final FluidTankNTM[] tanks = new FluidTankNTM[RocketStruct.MAX_STAGES * 2];
        public boolean canSeeSky = true;
        public @Nullable RocketStruct rocket;
        /** How tall the tower stands, 0 for none. */
        public int height;
        private boolean sideLadder;

        public PadBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, SLOTS);
            for (int i = 0; i < tanks.length; i++) tanks[i] = new FluidTankNTM(NTMFluids.NONE, TANK_SIZE);
        }

        private Direction facing() {
            return getBlockState().getValue(BlockMultiblockCore.FACING);
        }

        private BlockPos local(int x, int y, int z) {
            Direction f = facing();
            return worldPosition.offset(MultiblockMaskTable.worldX(x, y, z, f), y, MultiblockMaskTable.worldZ(x, y, z, f));
        }

        /** The world side the pad's local +x faces, where both ladders are climbed from. */
        private Direction ladderFace() {
            BlockPos a = local(0, 0, 0), b = local(1, 0, 0);
            return Direction.fromDelta(b.getX() - a.getX(), 0, b.getZ() - a.getZ());
        }

        @Override
        public void tickServer() {
            if (!sideLadder) {
                for (int oy = 0; oy < 3; oy++) setTower(local(SIDE_LADDER_X, oy, LADDER_Z), true);
                sideLadder = true;
            }

            power += ItemEnergyTransfer.extract(this, BATTERY, MAX_POWER - power, false);
            updateTanks();
            for (FluidTankNTM tank : tanks) if (!isNone(tank.getTankType())) tank.loadTank(FUEL_IN, FUEL_OUT, inventory);
            ItemStack solid = getItem(FUEL_IN);
            if (!solid.isEmpty() && solid.is(ModItems.ROCKET_FUEL.get()) && solidFuel.level < solidFuel.max) {
                solid.shrink(1);
                solidFuel.level = Math.min(solidFuel.level + 250, solidFuel.max);
                setChanged();
            }

            rocket = ItemCustomRocket.get(getItem(ROCKET));
            if (rocket != null) {
                double capsule = rocket.capsule != null ? rocket.capsule.height : 0;
                int newHeight = (int) Math.floor(rocket.getHeight() - capsule + 1);
                if (newHeight <= 8) newHeight = 0;
                if (newHeight != height) {
                    canSeeSky = level.canSeeSky(worldPosition.above(3));
                    if (canSeeSky) {
                        clearTower();
                        height = newHeight;
                        buildTower();
                        setChanged();
                    }
                }
            }
            networkPackNT(250);
        }

        private boolean setTower(BlockPos at, boolean ladder) {
            if (!level.isInWorldBounds(at)) return false;
            BlockState state = level.getBlockState(at);
            if (!state.canBeReplaced() && !state.is(TOWER.get())) return false;
            level.setBlock(at, TOWER.get().defaultBlockState().setValue(Tower.FACING, ladderFace()).setValue(Tower.LADDER, ladder), 3);
            return true;
        }

        private void clearTowerAt(BlockPos at) {
            if (level.getBlockState(at).is(TOWER.get())) level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        }

        private void buildTower() {
            if (height <= 0) return;
            for (int oy = 3; oy < height + 3; oy++)
                for (int[] c : TOWER_CELLS) setTower(local(c[0], oy, c[1]), c[0] == MAIN_LADDER_X && c[1] == LADDER_Z);
            for (int[] c : ARM_CELLS) setTower(local(c[0], height + 2, c[1]), false);
        }

        private void clearTower() {
            if (height <= 0) return;
            for (int oy = 3; oy < height + 3; oy++) for (int[] c : TOWER_CELLS) clearTowerAt(local(c[0], oy, c[1]));
            for (int[] c : ARM_CELLS) clearTowerAt(local(c[0], height + 2, c[1]));
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if (level != null && !level.isClientSide()) {
                clearTower();
                for (int oy = 0; oy < 3; oy++) clearTowerAt(local(SIDE_LADDER_X, oy, LADDER_Z));
            }
            super.preRemoveSideEffects(pos, state);
        }

        static boolean isNone(@Nullable Fluid fluid) {
            return fluid == null || fluid == NTMFluids.NONE;
        }

        private void updateTanks() {
            ItemStack stack = getItem(ROCKET);
            RocketStruct built = ItemCustomRocket.get(stack);
            if (built == null) return;
            // a rocket that comes fueled fills the tanks it brings
            boolean hasFuel = ItemCustomRocket.hasFuel(stack);
            if (hasFuel) ItemCustomRocket.setFuel(stack, false);
            if (updateStorageTanks(built, tanks, solidFuel, hasFuel)) setChanged();
        }

        /** Fits the tanks to what the rocket burns; existing fills are kept. Returns whether anything changed. */
        public static boolean updateStorageTanks(RocketStruct rocket, FluidTankNTM[] tanks, SolidFuelTank solidFuel, boolean hasFuel) {
            Map<Fluid, Integer> fuels = new LinkedHashMap<>(rocket.getFillRequirement());
            Integer solid = fuels.remove(NTMFluids.NONE);
            solidFuel.max = solid != null ? solid : 0;
            if (hasFuel) solidFuel.level = solidFuel.max;
            solidFuel.level = Math.min(solidFuel.level, solidFuel.max);

            List<int[]> wanted = new ArrayList<>(); // fluid id, size, fill
            for (FluidTankNTM tank : tanks) {
                Fluid type = tank.getTankType();
                if (isNone(type) || !fuels.containsKey(type)) continue;
                int size = fuels.remove(type);
                wanted.add(new int[] {BuiltInRegistries.FLUID.getId(type), size, Math.min(tank.getFill(), size)});
            }
            for (Map.Entry<Fluid, Integer> entry : fuels.entrySet())
                wanted.add(new int[] {BuiltInRegistries.FLUID.getId(entry.getKey()), entry.getValue(), 0});
            if (hasFuel) for (int[] w : wanted) w[2] = w[1];
            wanted.sort((a, b) -> b[0] - a[0]);

            boolean changed = false;
            for (int i = 0; i < tanks.length; i++) {
                FluidTankNTM tank = tanks[i];
                Fluid type = i < wanted.size() ? BuiltInRegistries.FLUID.byId(wanted.get(i)[0]) : NTMFluids.NONE;
                int size = i < wanted.size() ? wanted.get(i)[1] : TANK_SIZE;
                int fill = i < wanted.size() ? wanted.get(i)[2] : 0;
                if (tank.getTankType() == type && tank.getMaxFill() == size && tank.getFill() == fill) continue;
                if (tank.getTankType() != type) tank.setTankType(type);
                tank.changeTankSize(size);
                tank.setFill(fill);
                changed = true;
            }
            return changed;
        }

        // launch checks

        public boolean hasRocket() {
            return ItemCustomRocket.get(getItem(ROCKET)) != null;
        }

        public boolean hasDrive() {
            return getItem(DRIVE).getItem() instanceof ItemVOTVdrive;
        }

        private boolean areTanksFull() {
            for (FluidTankNTM tank : tanks) if (!isNone(tank.getTankType()) && tank.getFill() < tank.getMaxFill()) return false;
            return solidFuel.level >= solidFuel.max;
        }

        static ItemVOTVdrive.Target from(Level level) {
            return new ItemVOTVdrive.Target(CelestialBody.getBody(level), CelestialBody.inOrbit(level), true);
        }

        static boolean isStation(RocketStruct rocket) {
            return rocket.capsule == SpaceRocketParts.RP_STATION_CORE_20.get();
        }

        static boolean canCarryToOrbit(RocketStruct rocket) {
            return rocket.capsule == SpaceRocketParts.RP_CAPSULE_20.get() || isStation(rocket);
        }

        private boolean canReachDestination(RocketStruct built) {
            ItemStack drive = getItem(DRIVE);
            if (!ItemVOTVdrive.getProcessed(drive)) return false;
            if (ItemVOTVdrive.getBody(drive) == SolarSystem.Body.ORBIT && !canCarryToOrbit(built)) return false;
            ItemVOTVdrive.Target from = from(level), to = ItemVOTVdrive.getTarget(drive, level);
            if (to.isValid() == isStation(built)) return false;
            if (from.body() == null || to.body() == null) return false;
            return built.hasSufficientFuel(from.body(), to.body(), from.inOrbit(), to.inOrbit());
        }

        public boolean canLaunch() {
            RocketStruct built = ItemCustomRocket.get(getItem(ROCKET));
            return built != null && hasDrive() && power >= MAX_POWER * 3 / 4 && areTanksFull() && canSeeSky && canReachDestination(built);
        }

        public void launch() {
            if (!canLaunch()) return;
            // a pad on a Sable build starts the rocket in world space, along the pad's axis (as the Soyuz)
            double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 3, z = worldPosition.getZ() + 0.5;
            net.minecraft.world.phys.Vec3 at = com.hbm.backport.SubLevelSpace.toWorld(level, x, y, z);
            EntityRideableRocket entity = EntityRideableRocket.of(level, at.x, at.y, at.z, getItem(ROCKET)).withProgram(getItem(DRIVE));
            entity.setLaunchAxis(com.hbm.backport.LaunchAxis.padUp(level, x, y, z));
            level.addFreshEntity(entity);
            for (FluidTankNTM tank : tanks) {
                tank.setTankType(NTMFluids.NONE);
                tank.changeTankSize(TANK_SIZE);
                tank.setFill(0);
            }
            solidFuel.level = solidFuel.max = 0;
            power -= MAX_POWER * 3 / 4;
            setItem(ROCKET, ItemStack.EMPTY);
            setItem(DRIVE, ItemStack.EMPTY);
            setChanged();
        }

        private static Component line(ChatFormatting color, String text) {
            return Component.literal(text).withStyle(color);
        }

        public static void findTankIssues(List<Component> issues, FluidTankNTM[] tanks, SolidFuelTank solidFuel) {
            for (FluidTankNTM tank : tanks) {
                Fluid type = tank.getTankType();
                if (isNone(type)) continue;
                String name = type.getFluidType().getDescription().getString();
                if (name.contains(" ")) name = name.substring(name.lastIndexOf(' ') + 1);
                issues.add(line(tank.getFill() < tank.getMaxFill() ? ChatFormatting.YELLOW : ChatFormatting.GREEN,
                        tank.getFill() + "/" + tank.getMaxFill() + "mB " + name));
            }
            if (solidFuel.max > 0)
                issues.add(line(solidFuel.level < solidFuel.max ? ChatFormatting.YELLOW : ChatFormatting.GREEN,
                        solidFuel.level + "/" + solidFuel.max + "kg Solid Fuel"));
        }

        public static boolean findDriveIssues(List<Component> issues, RocketStruct rocket, ItemStack drive) {
            if (!(drive.getItem() instanceof ItemVOTVdrive)) {
                issues.add(line(ChatFormatting.YELLOW, "No destination drive installed"));
                return true;
            }
            if (!ItemVOTVdrive.getProcessed(drive)) {
                issues.add(line(ChatFormatting.RED, "Destination drive needs processing"));
                return true;
            }
            if (ItemVOTVdrive.getBody(drive) == SolarSystem.Body.ORBIT && !canCarryToOrbit(rocket)) {
                issues.add(line(ChatFormatting.RED, "Satellite target must be a planet"));
                return true;
            }
            return false;
        }

        public static void findTravelIssues(List<Component> issues, RocketStruct rocket, ItemVOTVdrive.Target from, ItemVOTVdrive.Target to) {
            if (to.inOrbit() && !to.isValid() && !isStation(rocket)) issues.add(line(ChatFormatting.RED, "Station not yet launched"));
            if (to.inOrbit() && to.isValid() && isStation(rocket)) issues.add(line(ChatFormatting.RED, "Station already launched"));
            if (from.body() == null || to.body() == null
                    || !rocket.hasSufficientFuel(from.body(), to.body(), from.inOrbit(), to.inOrbit()))
                issues.add(line(ChatFormatting.RED, "Rocket can't reach destination"));
        }

        /** What stands between the rocket and launch; empty without a rocket. Works on either side. */
        public List<Component> findIssues() {
            List<Component> issues = new ArrayList<>();
            RocketStruct built = ItemCustomRocket.get(getItem(ROCKET));
            if (built == null) return issues;
            if (!canSeeSky) issues.add(line(ChatFormatting.RED, "Pad is obstructed"));
            if (power < MAX_POWER * 3 / 4) issues.add(line(ChatFormatting.RED, "Insufficient power"));
            findTankIssues(issues, tanks, solidFuel);
            if (findDriveIssues(issues, built, getItem(DRIVE))) return issues;
            findTravelIssues(issues, built, from(level), ItemVOTVdrive.getTarget(getItem(DRIVE), level));
            return issues;
        }

        // power and fluid

        @Override
        public long getPower() {
            return power;
        }

        @Override
        public void setPower(long p) {
            power = Math.clamp(p, 0L, MAX_POWER);
        }

        @Override
        public long getMaxPower() {
            return MAX_POWER;
        }

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
            if (data.getBoolean("launch")) launch();
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
                case 0 -> output.writeLong(power);
                case 1 -> {
                    output.writeInt(solidFuel.level);
                    output.writeInt(solidFuel.max);
                    output.writeInt(height);
                    output.writeBoolean(canSeeSky);
                }
                case 2 -> {
                    output.writeBoolean(rocket != null);
                    if (rocket != null) rocket.write(new FriendlyByteBuf(output));
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
                case 0 -> power = input.readLong();
                case 1 -> {
                    solidFuel.level = input.readInt();
                    solidFuel.max = input.readInt();
                    height = input.readInt();
                    canSeeSky = input.readBoolean();
                }
                case 2 -> rocket = input.readBoolean() ? RocketStruct.read(new FriendlyByteBuf(input)) : null;
                case 3 -> {
                    for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
                }
                default -> super.readSyncUnit(unit, input);
            }
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            power = input.getLongOr("power", 0L);
            solidFuel.level = input.getIntOr("solid", 0);
            solidFuel.max = input.getIntOr("maxSolid", 0);
            height = input.getIntOr("height", 0);
            canSeeSky = input.getBooleanOr("sky", true);
            for (int i = 0; i < tanks.length; i++) {
                FluidTankNTM tank = tanks[i];
                input.child("t" + i).ifPresent(tank::deserialize);
            }
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("power", power);
            output.putInt("solid", solidFuel.level);
            output.putInt("maxSolid", solidFuel.max);
            output.putInt("height", height);
            output.putBoolean("sky", canSeeSky);
            for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("t" + i));
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.launchPadRocket");
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

    public static class Menu extends BlockEntityMenu<PadBE> {
        public Menu(int containerId, Inventory playerInv, PadBE be) {
            super(MENU.get(), containerId, be);
            checkContainerSize(be, PadBE.SLOTS);
            addSlot(new Filtered(be, PadBE.ROCKET, 37, 21));
            addSlot(new Filtered(be, PadBE.DRIVE, 37, 39));
            addSlot(new Slot(be, PadBE.BATTERY, 167, 90));
            addSlot(new Slot(be, PadBE.FUEL_IN, 77, 21));
            addSlot(new Slot(be, PadBE.FUEL_OUT, 77, 39));
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++)
                    addSlot(new Slot(playerInv, col + row * 9 + 9, 14 + col * 18, 154 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInv, col, 14 + col * 18, 212));
        }

        static boolean accepts(int slot, ItemStack stack) {
            return switch (slot) {
                case PadBE.ROCKET -> stack.getItem() instanceof ItemCustomRocket;
                case PadBE.DRIVE -> stack.getItem() instanceof ItemVOTVdrive;
                default -> true;
            };
        }

        private static class Filtered extends Slot {
            Filtered(PadBE be, int index, int x, int y) {
                super(be, index, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return accepts(getContainerSlot(), stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return quickMove(player, index, false, stack -> index < PadBE.SLOTS
                    ? moveItemStackTo(stack, PadBE.SLOTS, slots.size(), true)
                    : moveItemStackTo(stack, 0, PadBE.SLOTS, false));
        }
    }
}
