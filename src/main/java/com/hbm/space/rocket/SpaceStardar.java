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
import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystem;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * The StarDar: a receiver dish that programs an empty hard drive with a body to fly to, and maps
 * a stretch of that body's surface (16 by 16 chunks around the drive's rough destination) so a
 * flat landing zone can be picked. The surface is scanned a few chunks a tick, not all at once.
 */
public final class SpaceStardar {

    private SpaceStardar() {}

    public static final int[] DIMS = {0, 3, 2, 2, 2, 2};
    public static final int MAP = 256;

    public static final RegistryHandle<Stardar> BLOCK = Reg.blockItem("machine_stardar", Stardar::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 6F).requiresCorrectToolForDrops().noOcclusion());

    public static final Reg.BeHandle<StardarBE> BE = Reg.be("machine_stardar",
                    () -> new BlockEntityType<>(StardarBE::new, Set.of(BLOCK.get()), null))
            .ticksClient(StardarBE::tickClient)
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static final RegistryHandle<MenuType<Menu>> MENU = NeoForgeRegistrar.registerBlockEntityMenu("machine_stardar", StardarBE.class);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("machine_drive_processor"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Stardar extends BlockMultiblockCore implements ITickingBlock {
        public Stardar(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new StardarBE(pos, state);
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, 0, 0, 2, 2, 2, 2);
            for (int ox = -2; ox <= 2; ox += 2)
                for (int oz = -2; oz <= 2; oz += 2)
                    if (ox != 0 || oz != 0) layout.box(ox, 0, oz, 0, 3, 0, 0, 0, 0);
            // the rack
            layout.box(1, -2, 2, 0, 1, 0, 0, 0, 0);
            return layout.build(this, declaredCaps, DIMS, 2, 3);
        }
    }

    /** Where a drive's surface map is centred: picked at random within probe range the first time. */
    public static ItemVOTVdrive.Destination approximateDestination(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("ax") || !tag.contains("az")) {
            int range = Math.max(1, NeoForgeHbmConfig.runtime().spaceMaxProbeDistance());
            java.util.Random rand = new java.util.Random();
            int ax = rand.nextInt(range * 2) - range, az = rand.nextInt(range * 2) - range;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
                t.putInt("ax", ax);
                t.putInt("az", az);
            });
            tag.putInt("ax", ax);
            tag.putInt("az", az);
        }
        return new ItemVOTVdrive.Destination(ItemVOTVdrive.getBody(stack), tag.getInt("ax"), tag.getInt("az"));
    }

    public static class StardarBE extends BlockEntityMachineBase implements IControlReceiver, IGUIProvider, SyncUnitSchema {
        public float dishYaw, dishPitch, prevDishYaw, prevDishPitch;
        public float targetYaw, targetPitch;
        private long pointAt;

        /** Surface heights, MAP x MAP, row-major by z; null without a drive to map. */
        public int @Nullable [] heightmap;
        /** Bumped whenever the map changes, so the screen knows to redraw it. */
        public int heightmapVersion;
        private int scanned = -1;
        private ItemStack previous = ItemStack.EMPTY;
        private int sentVersion = -1;

        public StardarBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 1);
        }

        private void repoint() {
            pointAt = level.getGameTime() + level.random.nextInt(300) + 300;
            targetYaw = Mth.wrapDegrees(level.random.nextFloat() * 360);
            targetPitch = level.random.nextFloat() * 80;
        }

        @Override
        public void tickServer() {
            if (level.getGameTime() >= pointAt) repoint();
            ItemStack drive = getItem(0);
            CelestialBody body = drive.getItem() instanceof ItemVOTVdrive ? ItemVOTVdrive.getBody(drive).getBody() : null;
            if (body != null && body.dimension != null) {
                if (heightmap == null || !ItemStack.isSameItemSameComponents(drive, previous)) {
                    approximateDestination(drive);
                    previous = drive.copy();
                    heightmap = new int[MAP * MAP];
                    scanned = 0;
                    heightmapVersion++;
                }
                if (scanned >= 0 && scanned < 256) scan(body, drive);
            } else if (heightmap != null) {
                heightmap = null;
                scanned = -1;
                previous = ItemStack.EMPTY;
                heightmapVersion++;
            }
            if (level.getGameTime() % 10 == 0 || sentVersion != heightmapVersion && (scanned < 0 || scanned >= 256)) {
                networkPackNT(250);
                sentVersion = heightmapVersion;
            }
        }

        private void scan(CelestialBody body, ItemStack drive) {
            ServerLevel target = level.getServer() != null ? level.getServer().getLevel(body.dimension) : null;
            if (target == null) {
                scanned = -1;
                return;
            }
            ItemVOTVdrive.Destination destination = approximateDestination(drive);
            int baseX = (destination.x() >> 4) - 8, baseZ = (destination.z() >> 4) - 8;
            for (int n = 0; n < 4 && scanned < 256; n++, scanned++) {
                int cx = scanned % 16, cz = scanned / 16;
                ChunkAccess chunk = target.getChunk(baseX + cx, baseZ + cz, ChunkStatus.SURFACE, true);
                for (int x = 0; x < 16; x++)
                    for (int z = 0; z < 16; z++)
                        heightmap[(cz * 16 + z) * MAP + cx * 16 + x] = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 1;
            }
            if (scanned >= 256) heightmapVersion++;
        }

        public void tickClient() {
            float yawOffset = Mth.wrapDegrees(targetYaw - dishYaw);
            float moveYaw = Mth.clamp(yawOffset, -0.5F, 0.5F);
            float pitchOffset = targetPitch - dishPitch;
            float pitchSpeed = yawOffset == 0 ? Math.abs(pitchOffset) : moveYaw / yawOffset * Math.abs(pitchOffset);
            float movePitch = Mth.clamp(pitchOffset, -pitchSpeed, pitchSpeed);
            prevDishYaw = dishYaw;
            prevDishPitch = dishPitch;
            dishYaw += moveYaw;
            dishPitch += movePitch;
        }

        /** Programs the empty drive with a body, at given coordinates if any, or a new station spot. */
        private void processDrive(String bodyName, boolean orbit, boolean hasCoords, int ix, int iz) {
            if (!getItem(0).is(SpaceRockets.HARD_DRIVE.get())) return;
            ItemStack result;
            if (orbit) {
                // a new station drive reserves a free cell of the orbit grid
                if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return;
                result = new ItemStack(SpaceRockets.DRIVES.get(SolarSystem.Body.ORBIT).get());
                long cell = com.hbm.space.station.OrbitalStation.findFreeSpace(com.hbm.space.SolarSystemSavedData.get(server), new java.util.Random());
                ItemVOTVdrive.setCoordinates(result, (int) (cell >> 32), (int) cell);
                ItemVOTVdrive.setProcessed(result, true);
            } else {
                CelestialBody body = CelestialBody.getBodyOrNull(bodyName);
                if (body == null || body.type == null || !body.canLand) return;
                result = new ItemStack(SpaceRockets.DRIVES.get(body.type).get());
                if (hasCoords && !CelestialBody.inOrbit(level)) {
                    CustomData.update(DataComponents.CUSTOM_DATA, result, t -> {
                        t.putInt("ax", ix);
                        t.putInt("az", iz);
                    });
                    ItemVOTVdrive.setCoordinates(result, ix, iz);
                    ItemVOTVdrive.setProcessed(result, true);
                }
            }
            setItem(0, result);
            repoint();
            setChanged();
        }

        /** Moves a drive's landing point to a spot on its surface map. */
        private void updateDriveCoords(int x, int z) {
            ItemStack drive = getItem(0);
            if (!(drive.getItem() instanceof ItemVOTVdrive) || x < 0 || x >= MAP || z < 0 || z >= MAP) return;
            ItemVOTVdrive.Destination destination = approximateDestination(drive);
            ItemVOTVdrive.setCoordinates(drive, ((destination.x() >> 4) - 8) * 16 + x, ((destination.z() >> 4) - 8) * 16 + z);
            // the map stays: the drive's change is its own doing
            previous = drive.copy();
            setChanged();
        }

        @Override
        public boolean hasPermission(Player player) {
            // a remote access console's screen works from any distance
            return stillValid(player) || player.containerMenu instanceof Menu menu && menu.remote && menu.blockEntity() == this && !isRemoved();
        }

        @Override
        public void receiveControl(CompoundTag data) {
            if (data.contains("pid"))
                processDrive(data.getString("pid"), data.getBoolean("orbit"), data.contains("ix"), data.getInt("ix"), data.getInt("iz"));
            if (data.contains("px") && data.contains("pz")) updateDriveCoords(data.getInt("px"), data.getInt("pz"));
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public long syncUnitMask() {
            return super.syncUnitMask() | 3L;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            switch (unit) {
                case 0 -> {
                    output.writeFloat(targetYaw);
                    output.writeFloat(targetPitch);
                }
                case 1 -> {
                    // only finished maps go out, as heights over the map's lowest point
                    boolean ready = heightmap != null && scanned >= 256;
                    output.writeInt(heightmapVersion);
                    output.writeBoolean(ready);
                    if (ready) {
                        int min = Integer.MAX_VALUE;
                        for (int h : heightmap) min = Math.min(min, h);
                        output.writeInt(min);
                        for (int h : heightmap) output.writeByte(Math.min(h - min, 255));
                    }
                }
                default -> super.writeSyncUnit(unit, output);
            }
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            switch (unit) {
                case 0 -> {
                    targetYaw = input.readFloat();
                    targetPitch = input.readFloat();
                }
                case 1 -> {
                    heightmapVersion = input.readInt();
                    if (input.readBoolean()) {
                        int min = input.readInt();
                        int[] map = new int[MAP * MAP];
                        for (int i = 0; i < map.length; i++) map[i] = min + input.readUnsignedByte();
                        heightmap = map;
                    } else {
                        heightmap = null;
                    }
                }
                default -> super.readSyncUnit(unit, input);
            }
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            targetYaw = input.getFloatOr("yaw", 0F);
            targetPitch = input.getFloatOr("pitch", 0F);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putFloat("yaw", targetYaw);
            output.putFloat("pitch", targetPitch);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.machineStardar");
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

    public static class Menu extends BlockEntityMenu<StardarBE> {
        /** Opened from a remote access console, so the player need not stand near the dish. */
        public boolean remote;

        public Menu(int containerId, Inventory playerInv, StardarBE be) {
            super(MENU.get(), containerId, be);
            checkContainerSize(be, 1);
            addSlot(new Slot(be, 0, 150, 124));
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++)
                    addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 174 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInv, col, 8 + col * 18, 232));
        }

        @Override
        public boolean stillValid(Player player) {
            if (!remote) return super.stillValid(player);
            StardarBE be = blockEntity();
            return !be.isRemoved() && be.getLevel() != null && be.getLevel().getBlockEntity(be.getBlockPos()) == be;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return quickMove(player, index, false, stack -> index < 1
                    ? moveItemStackTo(stack, 1, slots.size(), true)
                    : moveItemStackTo(stack, 0, 1, false));
        }
    }
}
