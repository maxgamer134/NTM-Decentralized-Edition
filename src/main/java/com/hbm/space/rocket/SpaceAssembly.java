// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.rocket;

import com.hbm.api.control.IControlReceiver;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.items.CreativeContents;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.CelestialBody;
import com.hbm.space.rocket.RocketStruct.RocketStage;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.jspecify.annotations.Nullable;

/**
 * The rocket assembly building (NTM:Space's VAB): a capsule and up to five stages of fuselage,
 * fins and thrusters go in, a finished rocket comes out (and goes back apart the same way). Two
 * drive slots per stage say where that stage flies from and to, to check it carries the fuel.
 * As the rocket grows the building raises work platforms between its stages; they are their own
 * invisible block here, where NTM:Space made them part of the multiblock.
 */
public final class SpaceAssembly {

    private SpaceAssembly() {}

    public static final int[] DIMS = {0, 2, 4, 4, 4, 4};

    public static final RegistryHandle<Assembly> BLOCK = Reg.blockItem("machine_rocket_assembly", Assembly::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 6F).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryHandle<Platform> PLATFORM = Reg.block("rocket_assembly_platform", Platform::new,
            () -> BlockBehaviour.Properties.of().strength(-1F, 3_600_000F).noLootTable().noOcclusion()
                    .pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, e) -> false));

    public static final Reg.BeHandle<AssemblyBE> BE = Reg.be("machine_rocket_assembly",
                    () -> new BlockEntityType<>(AssemblyBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static final RegistryHandle<MenuType<Menu>> MENU = NeoForgeRegistrar.registerBlockEntityMenu("machine_rocket_assembly", AssemblyBE.class);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("launch_pad_large"), () -> new ItemStack(BLOCK.get()));
    }

    /** A work platform or spire; drawn by the building's renderer, so invisible itself. */
    public static class Platform extends Block {
        public Platform(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }
    }

    public static class Assembly extends BlockMultiblockCore implements ITickingBlock {
        public Assembly(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new AssemblyBE(pos, state);
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, 0, 0, 4, 4, 4, 4);
            // the corner legs, down to the ground
            for (int sx = -1; sx <= 1; sx += 2)
                for (int sz = -1; sz <= 1; sz += 2) {
                    layout.box(4 * sx, 0, 4 * sz, 0, 2, 0, 0, 0, 0);
                    layout.box(3 * sx, 0, 4 * sz, 0, 2, 0, 0, 0, 0);
                    layout.box(4 * sx, 0, 3 * sz, 0, 2, 0, 0, 0, 0);
                }
            return layout.build(this, declaredCaps, DIMS, 4, 2);
        }
    }

    public static class AssemblyBE extends BlockEntityMachineBase implements IControlReceiver, IGUIProvider, SyncUnitSchema {
        public static final int CAPSULE = 0;
        public static final int RESULT = 1 + RocketStruct.MAX_STAGES * 3;
        public static final int DRIVES = RESULT + 1;
        public static final int SLOTS = DRIVES + RocketStruct.MAX_STAGES * 2;

        public RocketStruct rocket = new RocketStruct();
        private int previousHeight = -1;
        private List<Integer> platforms = new ArrayList<>();
        private int spire;
        private boolean platformFailed;
        /** The stage the screen shows; shared by everyone looking, as in NTM:Space. */
        public int currentStage;

        public AssemblyBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, SLOTS);
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
            rocket = new RocketStruct(getItem(CAPSULE));
            for (int i = 1; i < RocketStruct.MAX_STAGES * 3; i += 3) {
                if (getItem(i).isEmpty() && getItem(i + 1).isEmpty() && getItem(i + 2).isEmpty()) {
                    // pull later stages up into the gap
                    if (i + 3 < RocketStruct.MAX_STAGES * 3 && !(getItem(i + 3).isEmpty() && getItem(i + 4).isEmpty() && getItem(i + 5).isEmpty())) {
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

            int height = (int) rocket.getHeight();
            if (height != previousHeight) {
                rebuildPlatforms();
                previousHeight = height;
            }
            if (platformFailed) rocket.addIssue(Component.literal("VAB ceiling too low").withStyle(ChatFormatting.RED));
            networkPackNT(250);
        }

        private void rebuildPlatforms() {
            clearPlatforms();
            int maxHeight = Integer.MAX_VALUE;
            for (int h = 1; worldPosition.getY() + h < level.getMaxBuildHeight(); h++) {
                BlockState state = level.getBlockState(worldPosition.above(h));
                if (!state.canBeReplaced() && !state.is(PLATFORM.get())) {
                    maxHeight = h;
                    break;
                }
            }
            double check = rocket.getHeight();
            if (rocket.capsule != null) check -= rocket.capsule.height;
            if (!rocket.stages.isEmpty() && rocket.stages.get(0).thruster != null) check -= rocket.stages.get(0).thruster.height;
            platformFailed = check >= maxHeight;
            if (platformFailed) return;

            double target = 0;
            for (int i = 0; i < rocket.stages.size(); i++) {
                RocketStage stage = rocket.stages.get(i);
                RocketStage next = i < rocket.stages.size() - 1 ? rocket.stages.get(i + 1) : null;
                if (stage.fuselage != null) target += stage.fuselage.height * stage.getStack();
                if (next != null && next.thruster != null) target += next.thruster.height;
                int platform = (int) Math.round(target);
                if (platform > 0) {
                    placePlatform(platform);
                    platforms.add(platform);
                }
            }
            spire = (int) Math.round(target);
            for (int y = 1; y <= spire; y++) for (int[] c : SPIRES) setPlatform(c[0], y, c[1]);
        }

        private static final int[][] SPIRES = {{0, 0}, {-4, -4}, {4, -4}, {-4, 4}, {4, 4}};

        private void setPlatform(int x, int y, int z) {
            BlockPos at = worldPosition.offset(x, y, z);
            if (level.isInWorldBounds(at) && level.getBlockState(at).canBeReplaced())
                level.setBlock(at, PLATFORM.get().defaultBlockState(), 3);
        }

        private void clearPlatform(int x, int y, int z) {
            BlockPos at = worldPosition.offset(x, y, z);
            if (level.getBlockState(at).is(PLATFORM.get())) level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        }

        private void placePlatform(int y) {
            for (int x = -4; x <= 4; x++)
                for (int z = -4; z <= 4; z++)
                    if ((Math.abs(x) != 4 || Math.abs(z) != 4) && (x != 0 || z != 0)) setPlatform(x, y, z);
        }

        private void clearPlatforms() {
            for (int y : platforms)
                for (int x = -4; x <= 4; x++)
                    for (int z = -4; z <= 4; z++) clearPlatform(x, y, z);
            platforms = new ArrayList<>();
            for (int y = 1; y <= spire; y++) for (int[] c : SPIRES) clearPlatform(c[0], y, c[1]);
            spire = 0;
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if (level != null && !level.isClientSide()) clearPlatforms();
            super.preRemoveSideEffects(pos, state);
        }

        public List<Component> issues(Level level) {
            int stage = Math.max(rocket.stages.size() - 1 - currentStage, 0);
            ItemVOTVdrive.Target from = ItemVOTVdrive.getTarget(fromDrive(currentStage), level);
            ItemVOTVdrive.Target to = ItemVOTVdrive.getTarget(toDrive(currentStage), level);
            return rocket.findIssues(stage, from.body(), to.body(), from.inOrbit(), to.inOrbit());
        }

        public void construct() {
            if (!rocket.validate()) return;
            setItem(RESULT, ItemCustomRocket.build(rocket));
            for (int i = 0; i < RESULT; i++) setItem(i, ItemStack.EMPTY);
        }

        public boolean canDeconstruct() {
            if (ItemCustomRocket.get(getItem(RESULT)) == null) return false;
            for (int i = 0; i < RESULT; i++) if (!getItem(i).isEmpty()) return false;
            return true;
        }

        public void deconstruct() {
            if (!canDeconstruct()) return;
            RocketStruct built = ItemCustomRocket.get(getItem(RESULT));
            if (built == null || built.capsule == null) return;
            setItem(CAPSULE, built.capsuleStack());
            for (int i = 0; i < built.stages.size() && i < RocketStruct.MAX_STAGES; i++) {
                int o = i * 3;
                RocketStage stage = built.stages.get(built.stages.size() - 1 - i);
                if (stage.fuselage != null) setItem(o + 1, new ItemStack(stage.fuselage, stage.fuselageCount));
                if (stage.fins != null) setItem(o + 2, new ItemStack(stage.fins));
                if (stage.thruster != null) setItem(o + 3, new ItemStack(stage.thruster, stage.thrusterCount));
            }
            setItem(RESULT, ItemStack.EMPTY);
        }

        @Override
        public boolean hasPermission(Player player) {
            return stillValid(player);
        }

        @Override
        public void receiveControl(CompoundTag data) {
            if (data.getBoolean("construct")) construct();
            if (data.getBoolean("deconstruct")) deconstruct();
            if (data.contains("stage")) currentStage = Math.clamp(data.getInt("stage"), 0, RocketStruct.MAX_STAGES - 1);
            setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return Menu.accepts(slot, stack);
        }

        @Override
        public long syncUnitMask() {
            return super.syncUnitMask() | 3L;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            switch (unit) {
                case 0 -> rocket.write(new FriendlyByteBuf(output));
                case 1 -> output.writeByte(currentStage);
                default -> super.writeSyncUnit(unit, output);
            }
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            switch (unit) {
                case 0 -> rocket = RocketStruct.read(new FriendlyByteBuf(input));
                case 1 -> currentStage = input.readByte();
                default -> super.readSyncUnit(unit, input);
            }
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            platforms = new ArrayList<>();
            for (int y : input.getIntArray("platforms").orElse(new int[0])) platforms.add(y);
            spire = input.getIntOr("spire", 0);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putIntArray("platforms", platforms.stream().mapToInt(Integer::intValue).toArray());
            output.putInt("spire", spire);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.machineRocketAssembly");
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

    public static class Menu extends BlockEntityMenu<AssemblyBE> {
        public Menu(int containerId, Inventory playerInv, AssemblyBE be) {
            super(MENU.get(), containerId, be);
            checkContainerSize(be, AssemblyBE.SLOTS);
            addSlot(new Part(be, AssemblyBE.CAPSULE, 18, 13, -1));
            for (int i = 0; i < RocketStruct.MAX_STAGES; i++) {
                addSlot(new Part(be, 1 + i * 3, 18, 44, i));
                addSlot(new Part(be, 2 + i * 3, 18, 62, i));
                addSlot(new Part(be, 3 + i * 3, 18, 80, i));
            }
            addSlot(new Part(be, AssemblyBE.RESULT, 42, 91, -1));
            for (int i = 0; i < RocketStruct.MAX_STAGES; i++) {
                addSlot(new Part(be, AssemblyBE.DRIVES + i * 2, 161, 54, i));
                addSlot(new Part(be, AssemblyBE.DRIVES + i * 2 + 1, 170, 87, i));
            }
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++)
                    addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 142 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInv, col, 8 + col * 18, 200));
        }

        static boolean accepts(int slot, ItemStack stack) {
            if (slot == AssemblyBE.RESULT) return stack.getItem() instanceof ItemCustomRocket;
            if (AssemblyBE.isDriveSlot(slot)) return stack.getItem() instanceof ItemVOTVdrive;
            if (slot == AssemblyBE.CAPSULE && com.hbm.saveddata.satellites.SatelliteType.fromStack(stack) != null) return true;
            ItemCustomMissilePart part = ItemCustomMissilePart.part(stack);
            if (part == null || part.type == null) return false;
            if (slot == AssemblyBE.CAPSULE)
                return part.type == PartType.WARHEAD && part != SpaceRocketParts.RP_POD_20.get()
                        && (part.warheadType() == SpaceRocketParts.APOLLO || part.warheadType() == SpaceRocketParts.SATELLITE);
            return part.type == switch ((slot - 1) % 3) {
                case 0 -> PartType.FUSELAGE;
                case 1 -> PartType.FINS;
                default -> PartType.THRUSTER;
            };
        }

        /** A slot of one stage, shown only while the screen is on that stage. */
        private class Part extends Slot {
            private final int stage;

            Part(AssemblyBE be, int index, int x, int y, int stage) {
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
                return !AssemblyBE.isDriveSlot(getContainerSlot());
            }
        }

        @Override
        public void clicked(int slotId, int button, ClickType input, Player player) {
            // the drive slots only take a copy of the drive, to read it
            if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof Part part
                    && AssemblyBE.isDriveSlot(part.getContainerSlot())) {
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
            return quickMove(player, index, false, stack -> index < AssemblyBE.SLOTS
                    ? moveItemStackTo(stack, AssemblyBE.SLOTS, slots.size(), true)
                    : moveItemStackTo(stack, 0, AssemblyBE.DRIVES, false));
        }
    }

    /** For the screen: the body a drive leaves from or flies to, if it names one. */
    public static @Nullable CelestialBody body(ItemStack drive, Level level) {
        return ItemVOTVdrive.getTarget(drive, level).body();
    }
}
