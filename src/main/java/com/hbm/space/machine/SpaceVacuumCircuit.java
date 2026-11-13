// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.IUpgradeInfoProvider;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.inventory.slot.SlotRecipeOutput;
import com.hbm.inventory.slot.SlotUpgrade;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import com.hbm.space.atmosphere.SpaceAtmosphere;
import com.hbm.space.rocket.SpaceDriveProcessor;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.util.BobMathUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space's vacuum solderer: solders the processor circuits from wafers and boards, and only
 * works where there is next to no air (0.001 atm or less), so it has to be set up in space.
 */
public final class SpaceVacuumCircuit {

    private SpaceVacuumCircuit() {}

    public static final int[] DIMS = {0, 0, 1, 1, 1, 1};

    public static final RegistryHandle<Item> CIRCUIT_GAAS = Reg.item("circuit_gaas", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> CIRCUIT_GASCHIP = Reg.item("circuit_gaschip", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> CIRCUIT_HFCHIP = Reg.item("circuit_hfchip", Item::new, Item.Properties::new);
    public static final RegistryHandle<Item> CIRCUIT_CAPACITOR_LANTHANIUM =
            Reg.item("circuit_capacitor_lanthanium", Item::new, Item.Properties::new);

    public static final RegistryHandle<Solderer> BLOCK = Reg.blockItem("machine_vacuum_circuit", Solderer::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion(),
            (block, props) -> new com.hbm.items.DescBlockItem(block, props, 1, com.hbm.items.DescBlockItem.Desc.PLAIN));

    public static final Reg.BeHandle<CircuitBE> BE = Reg.be("machine_vacuum_circuit",
                    () -> new BlockEntityType<>(CircuitBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static final RegistryHandle<MenuType<Menu>> MENU =
            NeoForgeRegistrar.registerBlockEntityMenu("machine_vacuum_circuit", CircuitBE.class);

    /** A recipe: the wafers and the boards (each matched by any one of its two slots), and what it makes. */
    public record Recipe(Supplier<ItemStack> output, int duration, long consumption, CountIngredient[] wafer, CountIngredient[] pcb) {}

    private static @Nullable List<Recipe> recipes;

    /** NTM:Space's defaults; built on first use, once the items exist. */
    public static List<Recipe> recipes() {
        List<Recipe> built = recipes;
        if (built != null) return built;
        built = new ArrayList<>();
        // t0 you can go to the mun when you got all the shit. :)
        built.add(new Recipe(() -> new ItemStack(SpaceDriveProcessor.CIRCUITS.get(0).get()), 200, 250,
                new CountIngredient[] {CountIngredient.of(ModItems.CIRCUIT_CHIP.get(), 8)},
                new CountIngredient[] {CountIngredient.of(ModItems.CIRCUIT_PCB.get(), 2), CountIngredient.of(ModItems.CIRCUIT_CAPACITOR.get(), 4)}));
        built.add(new Recipe(() -> new ItemStack(SpaceDriveProcessor.CIRCUITS.get(1).get()), 400, 1_000,
                new CountIngredient[] {CountIngredient.of(CIRCUIT_GASCHIP.get(), 16)},
                new CountIngredient[] {CountIngredient.of(ModItems.CIRCUIT_BASIC.get(), 4), CountIngredient.of(ModItems.CIRCUIT_CAPACITOR.get(), 32)}));
        built.add(new Recipe(() -> new ItemStack(SpaceDriveProcessor.CIRCUITS.get(2).get()), 800, 25_000,
                new CountIngredient[] {CountIngredient.of(CIRCUIT_GASCHIP.get(), 48), CountIngredient.of(CIRCUIT_HFCHIP.get(), 16)},
                new CountIngredient[] {CountIngredient.of(ModItems.CIRCUIT_ADVANCED.get(), 8), CountIngredient.of(CIRCUIT_CAPACITOR_LANTHANIUM.get(), 64)}));
        recipes = built;
        return built;
    }

    private static boolean matches(ItemStack a, ItemStack b, CountIngredient[] wanted) {
        for (CountIngredient ing : wanted) {
            if (!(ing.test(a) || ing.test(b))) return false;
        }
        return true;
    }

    public static @Nullable Recipe getRecipe(ItemStack w0, ItemStack w1, ItemStack p0, ItemStack p1) {
        for (Recipe recipe : recipes()) {
            if (matches(w0, w1, recipe.wafer()) && matches(p0, p1, recipe.pcb())) return recipe;
        }
        return null;
    }

    private static boolean anyIngredient(ItemStack stack, boolean wafer) {
        for (Recipe recipe : recipes()) {
            for (CountIngredient ing : wafer ? recipe.wafer() : recipe.pcb()) if (ing.matchesItem(stack)) return true;
        }
        return false;
    }

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_soldering_station"), () -> new ItemStack(BLOCK.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.PARTS, Library.id("circuit_capacitor"),
                () -> new ItemStack(CIRCUIT_CAPACITOR_LANTHANIUM.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.PARTS, Library.id("circuit_chip"), () -> new ItemStack(CIRCUIT_HFCHIP.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.PARTS, Library.id("circuit_chip"), () -> new ItemStack(CIRCUIT_GASCHIP.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.PARTS, Library.id("circuit_silicon"), () -> new ItemStack(CIRCUIT_GAAS.get()));
    }

    public static class Solderer extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock, ILookOverlay {
        public Solderer(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CircuitBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().items().itemsAtCells().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, DIMS);
            layout.port(1, 0, 1);
            layout.port(1, 0, -1);
            layout.port(-1, 0, 1);
            layout.port(-1, 0, -1);
            return layout.build(this, declaredCaps, DIMS, 0, 0);
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            BlockPos core = overlayCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof CircuitBE be) || be.canOperate) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            info.line(Component.literal("! ! ! ").append(Component.translatable("atmosphere.noAir")).append(" ! ! !"),
                    BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
        }
    }

    public static class CircuitBE extends BlockEntityMachineBase
            implements IGUIProvider, IEnergyHandlerMK2, IUpgradeInfoProvider, SyncUnitSchema {

        public static final int SLOT_OUTPUT = 4, SLOT_BATTERY = 5, SLOT_UPGRADE_START = 6, SLOT_UPGRADE_END = 7;
        private static final int[] ACCESSIBLE = {0, 1, 2, 3, SLOT_OUTPUT};
        private static final int[] VALID_UPGRADES = IUpgradeInfoProvider.upgradeCaps(UpgradeType.SPEED, 3, UpgradeType.POWER, 3);

        private final UpgradeManager upgradeManager = new UpgradeManager(this);
        public long power;
        public long maxPower = 2_000;
        public long consumption;
        public int progress;
        public int processTime = 1;
        public boolean canOperate = true;
        private @Nullable Recipe recipe;
        public ItemStack display = ItemStack.EMPTY;

        public CircuitBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 8);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            super.setItem(slot, stack);
            if (level == null || level.isClientSide()) return;
            if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END && ItemMachineUpgrade.isUpgrade(stack))
                level.playSound(null, worldPosition, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            CBT_Atmosphere atmosphere = SpaceAtmosphere.getAtmosphere(level, worldPosition);
            canOperate = atmosphere == null || atmosphere.getPressure() <= 0.001;
            power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);
            recipe = getRecipe(inventory.get(0), inventory.get(1), inventory.get(2), inventory.get(3));
            upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
            int red = upgradeManager.getLevel(UpgradeType.SPEED);
            int blue = upgradeManager.getLevel(UpgradeType.POWER);
            long intendedMaxPower;
            if (recipe != null) {
                processTime = recipe.duration() - recipe.duration() * red / 6 + recipe.duration() * blue / 3;
                consumption = recipe.consumption() + recipe.consumption() * red - recipe.consumption() * blue / 6;
                intendedMaxPower = recipe.consumption() * 20;
                if (canProcess(recipe)) {
                    progress++;
                    power -= consumption;
                    if (progress >= processTime) {
                        progress = 0;
                        consume(recipe);
                        ItemStack out = inventory.get(SLOT_OUTPUT);
                        ItemStack made = recipe.output().get();
                        if (out.isEmpty()) inventory.set(SLOT_OUTPUT, made);
                        else out.grow(made.getCount());
                        setChanged();
                    }
                    if (server.getGameTime() % 20 == 0) {
                        Direction dir = BlockMultiblockCore.coreFacing(getBlockState());
                        Direction rot = dir.getClockWise();
                        ParticleCreators.sparks(server,
                                worldPosition.getX() + 0.5 + dir.getStepX() * 0.625 + rot.getStepX() * 0.5,
                                worldPosition.getY() + 1.25,
                                worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.625 + rot.getStepZ() * 0.5,
                                3, false, 25);
                    }
                } else {
                    progress = 0;
                }
            } else {
                progress = 0;
                consumption = 100;
                intendedMaxPower = 2_000;
            }
            maxPower = Math.max(intendedMaxPower, power);
            networkPackNT(25);
        }

        private boolean canProcess(Recipe recipe) {
            if (!canOperate || power < consumption) return false;
            ItemStack out = inventory.get(SLOT_OUTPUT);
            if (out.isEmpty()) return true;
            ItemStack made = recipe.output().get();
            return ItemStack.isSameItemSameComponents(out, made) && out.getCount() + made.getCount() <= out.getMaxStackSize();
        }

        private void consume(Recipe recipe) {
            consumeGroup(0, recipe.wafer());
            consumeGroup(2, recipe.pcb());
        }

        private void consumeGroup(int first, CountIngredient[] group) {
            for (CountIngredient ing : group) {
                for (int i = first; i < first + 2; i++) {
                    ItemStack stack = inventory.get(i);
                    if (ing.test(stack)) {
                        stack.shrink(ing.count());
                        break;
                    }
                }
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == SLOT_BATTERY) return IBatteryItem.isBattery(stack);
            if (slot == SLOT_UPGRADE_START || slot == SLOT_UPGRADE_END) return ItemMachineUpgrade.isUpgrade(stack);
            if (slot >= 4) return false;
            int first = slot < 2 ? 0 : 2;
            for (int i = first; i < first + 2; i++)
                if (i != slot && !inventory.get(i).isEmpty() && ItemStack.isSameItem(inventory.get(i), stack)) return false;
            return anyIngredient(stack, slot < 2);
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return ACCESSIBLE;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return slot < 4 && canPlaceItem(slot, stack);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return slot == SLOT_OUTPUT;
        }

        @Override
        public long getPower() {
            return Math.max(0L, Math.min(power, maxPower));
        }

        @Override
        public void setPower(long p) {
            power = p;
        }

        @Override
        public long getMaxPower() {
            return maxPower;
        }

        @Override
        public int[] getValidUpgrades() {
            return VALID_UPGRADES;
        }

        @Override
        public Component getDisplayName() {
            return getName();
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.machineVacuumCircuit");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new Menu(containerId, playerInventory, this);
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
            output.writeLong(maxPower);
            output.writeLong(consumption);
            output.writeInt(progress);
            output.writeInt(processTime);
            output.writeBoolean(canOperate);
            output.writeInt(recipe == null ? -1 : BuiltInRegistries.ITEM.getId(recipe.output().get().getItem()));
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            power = input.readLong();
            maxPower = input.readLong();
            consumption = input.readLong();
            progress = input.readInt();
            processTime = input.readInt();
            canOperate = input.readBoolean();
            int id = input.readInt();
            display = id < 0 ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.byId(id));
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            power = input.getLongOr("power", 0L);
            maxPower = input.getLongOr("maxPower", 2_000L);
            progress = input.getIntOr("progress", 0);
            processTime = input.getIntOr("processTime", 1);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("power", power);
            output.putLong("maxPower", maxPower);
            output.putInt("progress", progress);
            output.putInt("processTime", processTime);
        }
    }

    public static class Menu extends BlockEntityMenu<CircuitBE> {
        public Menu(int containerId, Inventory playerInv, CircuitBE be) {
            super(MENU.get(), containerId, be);
            checkContainerSize(be, 8);
            for (int i = 0; i < 2; i++)
                for (int j = 0; j < 2; j++) addSlot(SlotFiltered.gated(be, i * 2 + j, 26 + j * 18, 18 + i * 18));
            addSlot(new SlotRecipeOutput(playerInv.player, be, CircuitBE.SLOT_OUTPUT, 98, 27));
            addSlot(new SlotFiltered(be, CircuitBE.SLOT_BATTERY, 143, 72, IBatteryItem::isBattery));
            addSlot(new SlotUpgrade(be, CircuitBE.SLOT_UPGRADE_START, 89, 72));
            addSlot(new SlotUpgrade(be, CircuitBE.SLOT_UPGRADE_END, 107, 72));
            addStandardInventorySlots(playerInv, 8, 122);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return quickMove(player, index, false, stack -> index < 8
                    ? moveItemStackTo(stack, 8, slots.size(), true)
                    : moveItemStackTo(stack, 0, 4, false) || moveItemStackTo(stack, 5, 8, false));
        }
    }
}
