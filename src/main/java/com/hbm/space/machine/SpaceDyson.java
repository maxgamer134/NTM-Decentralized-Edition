// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.backport.InteractionResult;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.IToolable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm.items.CreativeContents;
import com.hbm.items.ISatChip;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.ItemSatelliteChip;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.space.CelestialBody;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.space.trait.CBT_Atmosphere;
import com.hbm.space.trait.CBT_Dyson;
import com.hbm.util.BobMathUtil;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jspecify.annotations.Nullable;

/**
 * The Dyson swarm's ground machines: the spin launcher that flings swarm members at the star, the
 * receiver that turns the swarm's light into a beam, and the converters that stand in the beam and
 * make it into power, heat or an atmosphere.
 */
public final class SpaceDyson {

    private SpaceDyson() {}

    public static final RegistryHandle<SoundEvent> SPIN_CHARGE = NeoForgeRegistrar.registerSound("misc.spincharge");
    public static final RegistryHandle<SoundEvent> SPIN_SHOT = NeoForgeRegistrar.registerSound("misc.spinshot");
    public static final RegistryHandle<SoundEvent> BEAM = NeoForgeRegistrar.registerSound("block.dyson_beam");

    public static final RegistryHandle<ItemSatelliteChip> RELAY_ITEM = Reg.item("sat_dyson_relay",
            props -> new ItemSatelliteChip(props, null), () -> new Item.Properties().stacksTo(1));

    public static SatelliteType relayType;

    // launcher
    public static final int[] LAUNCHER_DIMS = {16, 0, 8, 0, 0, 0};
    public static final long LAUNCHER_POWER = 20_000_000L;
    private static final int MEMBERS_PER_LAUNCH = 4;
    // receiver
    public static final int[] RECEIVER_DIMS = {2, 0, 4, 2, 2, 2};
    // converters
    public static final int[] HE_DIMS = {2, 0, 4, 4, 1, 1};
    public static final int[] TU_DIMS = {2, 0, 1, 6, 1, 1};
    public static final int[] ANATMO_DIMS = {2, 0, 5, 5, 1, 1};
    /** 100 THE/s makes 0.1 atm in eight hours. */
    private static final long HE_TO_MB = 28_800_000;

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().strength(10F, 12F).requiresCorrectToolForDrops().noOcclusion();
    }

    public static final RegistryHandle<Launcher> LAUNCHER = Reg.blockItem("dyson_launcher", Launcher::new, SpaceDyson::props,
            (block, props) -> new com.hbm.items.DescBlockItem(block, props, 2, com.hbm.items.DescBlockItem.Desc.PLAIN));
    public static final Reg.BeHandle<LauncherBE> LAUNCHER_BE = Reg.be("dyson_launcher",
                    () -> new BlockEntityType<>(LauncherBE::new, Set.of(LAUNCHER.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static final RegistryHandle<Receiver> RECEIVER = Reg.blockItem("dyson_receiver", Receiver::new, SpaceDyson::props,
            (block, props) -> new com.hbm.items.DescBlockItem(block, props, 2, com.hbm.items.DescBlockItem.Desc.PLAIN));
    public static final Reg.BeHandle<ReceiverBE> RECEIVER_BE = Reg.be("dyson_receiver",
                    () -> new BlockEntityType<>(ReceiverBE::new, Set.of(RECEIVER.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static final RegistryHandle<ConverterHE> CONVERTER_HE = Reg.blockItem("dyson_converter_he", ConverterHE::new, SpaceDyson::props);
    public static final Reg.BeHandle<ConverterHEBE> CONVERTER_HE_BE = Reg.be("dyson_converter_he",
                    () -> new BlockEntityType<>(ConverterHEBE::new, Set.of(CONVERTER_HE.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static final RegistryHandle<ConverterTU> CONVERTER_TU = Reg.blockItem("dyson_converter_tu", ConverterTU::new, SpaceDyson::props);
    public static final Reg.BeHandle<ConverterTUBE> CONVERTER_TU_BE = Reg.be("dyson_converter_tu",
            () -> new BlockEntityType<>(ConverterTUBE::new, Set.of(CONVERTER_TU.get()), null));

    public static final RegistryHandle<ConverterAnatmo> CONVERTER_ANATMO = Reg.blockItem("dyson_converter_anatmogenesis", ConverterAnatmo::new, SpaceDyson::props);
    public static final Reg.BeHandle<ConverterAnatmoBE> CONVERTER_ANATMO_BE = Reg.be("dyson_converter_anatmogenesis",
                    () -> new BlockEntityType<>(ConverterAnatmoBE::new, Set.of(CONVERTER_ANATMO.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static void init() {
        relayType = SatelliteType.register("dyson_relay", Library.id("sat_dyson_relay"), "DYSON_RELAY",
                DysonRelay::new, DysonRelay.CODEC, null);
        ResourceLocation after = Library.id("machine_radiator");
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("sat_designator"), () -> new ItemStack(RELAY_ITEM.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, after, () -> new ItemStack(LAUNCHER.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, after, () -> new ItemStack(RECEIVER.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, after, () -> new ItemStack(CONVERTER_HE.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, after, () -> new ItemStack(CONVERTER_TU.get()));
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, after, () -> new ItemStack(CONVERTER_ANATMO.get()));
    }

    /** The satellite that lets a receiver collect the swarm at night. */
    public static class DysonRelay extends Satellite {
        public static final MapCodec<DysonRelay> CODEC = MapCodec.unit(DysonRelay::new);

        public DysonRelay() {}

        @Override
        public SatelliteType type() {
            return relayType;
        }
    }

    /** Something that can stand in a receiver's beam and take its energy. */
    public interface Converter {
        /** Takes the beam's energy at the given cell; false if power can't be received there. */
        boolean provideEnergy(BlockPos at, long energy);
    }

    private static Item swarmMember() {
        return BuiltInRegistries.ITEM.get(Library.id("swarm_member"));
    }

    private static InteractionResult insertChip(ItemStack held, Level level, BlockPos core, Player player, int slot) {
        if (!held.is(ModItems.SAT_CHIP.get()) || !(level.getBlockEntity(core) instanceof BlockEntityMachineBase be)
                || !be.getItem(slot).isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            be.setItem(slot, held.copyWithCount(1));
            held.consume(1, player);
            be.markChanged();
            level.playSound(null, core, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1F, 1F);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult takeChip(Level level, BlockPos core, Player player, int slot) {
        if (!(level.getBlockEntity(core) instanceof BlockEntityMachineBase be) || be.getItem(slot).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            player.getInventory().placeItemBackInInventory(be.getItem(slot));
            be.setItem(slot, ItemStack.EMPTY);
            be.markChanged();
            level.playSound(null, core, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1F, 1F);
        }
        return InteractionResult.SUCCESS;
    }

    // launcher

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
            return MachineCaps.of(LAUNCHER_BE).powerIn().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts();
            // the legs, the back dish, the dish and the arm, as NTM:Space's checks list them
            l.box(0, 14, 6, 0, 14, 1, 0, 6, -5).box(0, 14, 6, 0, 14, 1, 0, -5, 6);
            l.box(0, 7, 2, 2, 0, 0, 3, 2, 2).box(0, 6, 1, 2, 0, 0, 3, 3, 3).box(0, 5, 0, 2, 0, 0, 3, 3, 3)
                    .box(0, 4, -1, 2, 0, 0, 3, 3, 3).box(0, 3, -2, 2, 0, 0, 3, 2, 2);
            l.box(0, 14, 6, 2, 0, 0, 0, 3, 3).box(0, 14, 6, 0, 0, 0, 2, 3, 3);
            int[][] dish = {{5, 13, 5}, {4, 12, 7}, {3, 11, 8}, {2, 10, 9}, {1, 9, 10}, {0, 8, 10}, {-1, 7, 10}, {-2, 6, 10}, {-3, 5, 10}, {-4, 4, 9}};
            for (int[] d : dish) l.box(0, d[1], d[0], 2, 0, 0, 2, 10, d[2]);
            l.box(0, 3, -3, 2, 0, 2, 0, 8, 8).box(0, 2, -4, 2, 0, 2, 0, 7, 7).box(0, 2, -7, 1, 0, 0, 0, 6, 6);
            l.box(0, 2, 0, 0, 0, 8, -8, 3, 3).box(0, 1, 0, 1, 1, 8, -8, 1, 1);
            l.box(0, 0, -7, 1, 0, 0, 0, 4, 4).box(0, 0, -6, 1, 0, 0, 0, 5, 5).box(0, 0, -5, 1, 0, 0, 0, 6, 6);
            l.box(0, 0, 0, 3, 0, 7, 0, 2, 2);
            l.box(0, 0, -8, 0, 0, 0, 0, 0, 0);
            for (int z = 0; z >= -4; z--) {
                l.port(2, 0, z);
                l.port(-2, 0, z);
            }
            return l.build(this, declaredCaps, LAUNCHER_DIMS, 0, 0);
        }

        @Override
        protected InteractionResult useItemOnAtCore(ItemStack held, BlockState coreState, Level level, BlockPos core, Player player,
                InteractionHand hand, BlockHitResult hit) {
            // upstream MachineDysonLauncher: feeding it a FAT TETO plushie engages sunset overdrive
            if (com.hbm.blockentity.BlockEntityPlushie.typeOf(held) == com.hbm.space.SpacePlushies.SpacePlushie.FATO
                    && level.getBlockEntity(core) instanceof LauncherBE launcher) {
                if (launcher.sunsetOverdrive) return InteractionResult.TRY_WITH_EMPTY_HAND;
                if (!level.isClientSide()) {
                    launcher.sunsetOverdrive = true;
                    held.consume(1, player);
                    launcher.markChanged();
                }
                return InteractionResult.SUCCESS;
            }
            return insertChip(held, level, core, player, LauncherBE.SLOT_CHIP);
        }

        @Override
        protected InteractionResult useAtCore(BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
            return takeChip(level, core, player, LauncherBE.SLOT_CHIP);
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            BlockPos core = overlayCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof LauncherBE launcher)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            if (launcher.swarmId > 0) {
                info.line(Component.literal("ID: " + launcher.swarmId), 0xFFFFFF);
                info.line(Component.literal("Swarm: " + launcher.swarmCount + " members"), 0xFFFFFF);
                info.line(Component.literal("Power: " + BobMathUtil.getShortNumber(launcher.power) + "HE"),
                        launcher.power < LAUNCHER_POWER ? 0xFF5555 : 0x55FF55);
            } else {
                info.line(Component.literal("No Satellite ID-Chip installed!"), 0xFF5555);
            }
        }
    }

    public static class LauncherBE extends BlockEntityMachineBase implements IEnergyHandlerMK2, SyncUnitSchema {
        public static final int SLOT_MEMBERS = 0;
        public static final int SLOT_CHIP = 1;

        public int swarmId;
        public int swarmCount;
        public long power;
        public boolean isOperating;
        public boolean isSpinningDown;
        public int operatingTime;
        public int satCount;
        public float rotation, lastRotation, speed;
        public int payloadTicks;
        public boolean sunsetOverdrive;

        public LauncherBE(BlockPos pos, BlockState state) {
            super(LAUNCHER_BE.get(), pos, state, 2);
        }

        private int spinUpTime() {
            return sunsetOverdrive ? 38 : 132;
        }

        private int spinDownTime() {
            return sunsetOverdrive ? 12 : 68;
        }

        private long powerPerTick() {
            return LAUNCHER_POWER / spinUpTime();
        }

        @Override
        public int getMaxStackSize() {
            return MEMBERS_PER_LAUNCH;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == SLOT_MEMBERS ? stack.is(swarmMember()) : stack.is(ModItems.SAT_CHIP.get());
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            tryLoad(server);
            swarmId = ISatChip.getFreqS(getItem(SLOT_CHIP));
            swarmCount = CBT_Dyson.count(server, swarmId);
            ItemStack members = getItem(SLOT_MEMBERS);
            satCount = members.getCount();

            isOperating = !isSpinningDown && power >= powerPerTick() && members.is(swarmMember()) && swarmId > 0;

            if (isSpinningDown) {
                if (++operatingTime > spinDownTime()) {
                    isSpinningDown = false;
                    operatingTime = 0;
                }
            } else if (isOperating) {
                if (operatingTime == 0)
                    server.playSound(null, worldPosition.getX(), worldPosition.getY() + 8, worldPosition.getZ(), SPIN_CHARGE.get(),
                            SoundSource.BLOCKS, 1.5F, sunsetOverdrive ? 1.0F : 0.25F);
                operatingTime++;
                power -= powerPerTick();
                if (operatingTime > spinUpTime()) launch(server, members);
            } else {
                operatingTime = 0;
            }
            networkPackNT(250);
        }

        private void launch(ServerLevel server, ItemStack members) {
            int toLaunch = Math.min(members.getCount(), MEMBERS_PER_LAUNCH);
            CBT_Dyson.launch(server, swarmId, toLaunch);

            CBT_Atmosphere atmosphere = CelestialBody.getTrait(server, CBT_Atmosphere.class);
            double pressure = atmosphere != null ? atmosphere.getPressure() : 0;
            double scaled = 1.0 - Math.pow(1.0 - pressure, 3);
            float volume = Math.min((float) scaled * 16F, 4F);

            Direction facing = getBlockState().getValue(BlockMultiblockCore.FACING);
            Direction rot = facing.getClockWise();
            double sx = worldPosition.getX() + 0.5 + rot.getStepX() * 6;
            double sy = worldPosition.getY() + 8;
            double sz = worldPosition.getZ() + 0.5 + rot.getStepZ() * 6;
            server.playSound(null, sx, sy, sz, SPIN_SHOT.get(), SoundSource.BLOCKS, volume, 0.9F + server.random.nextFloat() * 0.3F);
            server.playSound(null, sx, sy, sz, SPIN_SHOT.get(), SoundSource.BLOCKS, volume, 1F + server.random.nextFloat() * 0.3F);
            int count = Math.min(20, (int) (pressure * 80));
            if (count > 0) {
                double px = worldPosition.getX() + rot.getStepX() * 9;
                double py = worldPosition.getY() + 12;
                double pz = worldPosition.getZ() + rot.getStepZ() * 9;
                com.hbm.packet.PacketWire.sendToAllAround(
                        com.hbm.packet.toclient.RocketFlameBurstPayload.spinlaunch(px, py, pz,
                                facing.getStepX() * 10, 10, facing.getStepZ() * 10, 3F,
                                10 + count / 2 + server.random.nextInt(5), count),
                        new com.hbm.handler.threading.TargetPoint(server, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 150));
            }

            members.shrink(toLaunch);
            operatingTime = 0;
            isSpinningDown = true;
            setChanged();
        }

        /** Pulls one swarm member at a time from whatever inventory sits at the foot of the machine. */
        private void tryLoad(ServerLevel server) {
            ItemStack held = getItem(SLOT_MEMBERS);
            if (held.getCount() >= MEMBERS_PER_LAUNCH) return;
            Direction facing = getBlockState().getValue(BlockMultiblockCore.FACING);
            BlockPos source = worldPosition.relative(facing.getOpposite(), 9);
            IItemHandler handler = server.getCapability(Capabilities.ItemHandler.BLOCK, source, facing);
            if (handler == null) return;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack probe = handler.extractItem(i, 1, true);
                if (probe.isEmpty() || !probe.is(swarmMember())) continue;
                if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, probe)) continue;
                ItemStack taken = handler.extractItem(i, 1, false);
                if (held.isEmpty()) setItem(SLOT_MEMBERS, taken);
                else held.grow(taken.getCount());
                setChanged();
                return;
            }
        }

        @Override
        public void tickClient() {
            float acceleration = sunsetOverdrive ? 2.5F : 0.75F;
            float deceleration = sunsetOverdrive ? 15.0F : 3.0F;
            float resetSpeed = sunsetOverdrive ? 30.0F : 8.0F;
            if (isOperating) {
                speed = Math.min(90F, speed + acceleration);
            } else if (speed > 0.1F) {
                speed = Math.max(resetSpeed, speed - deceleration);
            }
            lastRotation = rotation;
            if (!isOperating && speed <= resetSpeed && rotation > 360 - resetSpeed * 1.5) {
                lastRotation -= 360;
                rotation = 0;
                speed = 0;
            } else {
                rotation += speed;
            }
            if (rotation >= 360) {
                rotation -= 360;
                lastRotation -= 360;
            }
            payloadTicks = isSpinningDown ? payloadTicks + 1 : 0;
        }

        @Override
        public long getPower() {
            return power;
        }

        @Override
        public void setPower(long p) {
            power = Math.max(0L, Math.min(p, LAUNCHER_POWER));
        }

        @Override
        public long getMaxPower() {
            return LAUNCHER_POWER;
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
            output.writeInt(swarmId);
            output.writeLong(power);
            output.writeBoolean(isOperating);
            output.writeBoolean(isSpinningDown);
            output.writeInt(swarmCount);
            output.writeInt(satCount);
            output.writeBoolean(sunsetOverdrive);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            swarmId = input.readInt();
            power = input.readLong();
            isOperating = input.readBoolean();
            isSpinningDown = input.readBoolean();
            swarmCount = input.readInt();
            satCount = input.readInt();
            sunsetOverdrive = input.readBoolean();
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            power = input.getLongOr("power", 0L);
            isSpinningDown = input.getBooleanOr("spinDown", false);
            operatingTime = input.getIntOr("time", 0);
            sunsetOverdrive = input.getBooleanOr("overdrive", false);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("power", power);
            output.putBoolean("spinDown", isSpinningDown);
            output.putInt("time", operatingTime);
            output.putBoolean("overdrive", sunsetOverdrive);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.machineDysonLauncher");
        }

        public AABB renderBox() {
            return new AABB(worldPosition).inflate(100, 0, 100).expandTowards(0, 100, 0);
        }
    }

    // receiver

    /** Sun luminosity doesn't fit a long, so the output follows a Gompertz curve: single satellites are poor and swarms are enormous. */
    public static long getEnergyOutput(int swarmCount) {
        double density = swarmCount / 1024.0D;
        long maxOutput = Long.MAX_VALUE / 10;
        double gompertz = Math.exp(-32.0D * Math.exp(-1.3D * density));
        return (long) (maxOutput * gompertz) / 20;
    }

    public static class Receiver extends BlockMultiblockCore implements ITickingBlock, ILookOverlay {
        public Receiver(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ReceiverBE(pos, state);
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, RECEIVER_DIMS);
            l.box(0, 0, 0, 1, 0, 8, -4, 1, 1).box(0, 0, 0, 4, 0, 4, 0, 1, 1);
            l.box(0, 10, 0, 1, 0, 6, 6, 6, 6);
            l.box(0, 0, 0, 17, 0, 1, 1, 1, 1);
            return l.build(this, declaredCaps, RECEIVER_DIMS, 2, 0);
        }

        @Override
        protected InteractionResult useItemOnAtCore(ItemStack held, BlockState coreState, Level level, BlockPos core, Player player,
                InteractionHand hand, BlockHitResult hit) {
            return insertChip(held, level, core, player, 0);
        }

        @Override
        protected InteractionResult useAtCore(BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
            return takeChip(level, core, player, 0);
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            BlockPos core = overlayCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof ReceiverBE receiver)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            if (receiver.swarmId > 0) {
                long out = receiver.isReceiving && receiver.swarmConsumers > 0
                        ? getEnergyOutput(receiver.swarmCount) / receiver.swarmConsumers * 20 : 0;
                info.line(Component.literal("ID: " + receiver.swarmId), 0xFFFFFF);
                info.line(Component.literal("Swarm: " + receiver.swarmCount + " members"), 0xFFFFFF);
                info.line(Component.literal("Consumers: " + receiver.swarmConsumers + " consumers"), 0xFFFFFF);
                info.line(Component.literal("Power: " + BobMathUtil.getShortNumber(out) + "HE/s"), 0xFFFFFF);
            } else {
                info.line(Component.literal("No Satellite ID-Chip installed!"), 0xFF5555);
            }
        }
    }

    public static class ReceiverBE extends BlockEntityMachineBase implements SyncUnitSchema {
        public boolean isReceiving;
        public int swarmId, swarmCount, swarmConsumers, beamLength;

        public ReceiverBE(BlockPos pos, BlockState state) {
            super(RECEIVER_BE.get(), pos, state, 1);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.is(ModItems.SAT_CHIP.get());
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            Direction dir = getBlockState().getValue(BlockMultiblockCore.FACING).getOpposite();
            swarmId = ISatChip.getFreqS(getItem(0));

            Satellite sat = swarmId > 0 ? SatelliteSavedData.at(server, worldPosition.getX(), worldPosition.getZ()).getSatFromFreq(swarmId) : null;
            int sun = server.getBrightness(LightLayer.SKY, worldPosition) - server.getSkyDarken() - 11;

            boolean occluded = false;
            for (int x = -3; x <= 3 && !occluded; x++)
                for (int z = -3; z <= 3; z++)
                    if (server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldPosition.getX() + x, worldPosition.getZ() + z)
                            > worldPosition.getY() + 10) {
                        occluded = true;
                        break;
                    }

            swarmCount = CBT_Dyson.count(server, swarmId);
            swarmConsumers = CBT_Dyson.consumers(server, swarmId);
            boolean was = isReceiving;
            isReceiving = (sat instanceof DysonRelay || sun > 0) && swarmId > 0 && !occluded && swarmCount > 0 && swarmConsumers > 0;

            if (isReceiving) {
                long energy = getEnergyOutput(swarmCount) / swarmConsumers;
                int maxLength = 24;
                beamLength = maxLength;
                for (int i = 9; i < maxLength; i++) {
                    BlockPos at = worldPosition.relative(dir, i).above();
                    BlockState state = server.getBlockState(at);
                    boolean detonate = true;
                    BlockEntity be = null;
                    if (i > 10 && !state.isAir()) {
                        BlockPos core = MultiblockSurface.coreOfAny(server, at, state);
                        be = server.getBlockEntity(core != null ? core : at);
                        if (be instanceof Converter converter) detonate = !converter.provideEnergy(at, energy);
                        // NTM:Space's tungsten crate takes the beam as a laser, a 5000th of it
                        else if (be instanceof com.hbm.blockentity.machine.storage.BlockEntityCrateTungsten crate) {
                            crate.addEnergy(server, at, energy / 5000, Direction.NORTH);
                            detonate = false;
                        }
                    }
                    if (state.isSolidRender(server, at) || be != null) {
                        if (detonate) {
                            server.removeBlock(at, false);
                            server.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 3F, true, Level.ExplosionInteraction.BLOCK);
                        }
                        beamLength = i;
                        break;
                    }
                }
                Vec3 a = Vec3.atCenterOf(worldPosition).add(0, 1, 0);
                Vec3 b = a.add(dir.getStepX() * beamLength, 0, dir.getStepZ() * beamLength);
                AABB beam = new AABB(a, b).inflate(0.4, 0.5, 0.4);
                for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, beam)) {
                    entity.hurt(server.damageSources().explosion(null), (float) energy / 1_000F);
                    entity.igniteForSeconds(10);
                }
            }
            if (was != isReceiving) setChanged();
            networkPackNT(250);
        }

        @Override
        public void tickClient() {
            if (isReceiving && level != null && level.getGameTime() % 80 == 0) {
                Direction dir = getBlockState().getValue(BlockMultiblockCore.FACING).getOpposite();
                level.playLocalSound(worldPosition.getX() + 0.5 + dir.getStepX() * 8, worldPosition.getY(), worldPosition.getZ() + 0.5 + dir.getStepZ() * 8,
                        BEAM.get(), SoundSource.BLOCKS, 0.75F, 0.85F, false);
            }
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
            output.writeBoolean(isReceiving);
            output.writeInt(swarmId);
            output.writeInt(swarmCount);
            output.writeInt(swarmConsumers);
            output.writeInt(beamLength);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            isReceiving = input.readBoolean();
            swarmId = input.readInt();
            swarmCount = input.readInt();
            swarmConsumers = input.readInt();
            beamLength = input.readInt();
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.machineDysonReceiver");
        }
    }

    // converters

    /** Lets a converter's block reach its core and the beam's target cell. */
    private static boolean beamCell(BlockEntity be, BlockPos at, int distance) {
        Direction facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        return at.equals(be.getBlockPos().relative(facing, distance).above());
    }

    public static class ConverterHE extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {
        public ConverterHE(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ConverterHEBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(CONVERTER_HE_BE).powerOut().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, HE_DIMS);
            l.port(0, 0, -4);
            return l.build(this, declaredCaps, HE_DIMS, 4, 0);
        }
    }

    public static class ConverterHEBE extends BlockEntityMachineBase implements Converter, IEnergyHandlerMK2, SyncUnitSchema {
        public long power;
        public boolean isConverting;
        private int cooldown;

        public ConverterHEBE(BlockPos pos, BlockState state) {
            super(CONVERTER_HE_BE.get(), pos, state, 0);
        }

        @Override
        public void tickServer() {
            boolean was = isConverting;
            isConverting = power > 0;
            if (isConverting && level != null && level.getGameTime() % 2 == 0 && level instanceof ServerLevel server) {
                Direction facing = getBlockState().getValue(BlockMultiblockCore.FACING);
                Direction rot = facing.getClockWise();
                double x = worldPosition.getX() + 0.5 + facing.getStepX() * 3.5;
                double z = worldPosition.getZ() + 0.5 + facing.getStepZ() * 3.5;
                server.sendParticles(ParticleTypes.END_ROD, x + rot.getStepX(), worldPosition.getY() + 2.5, z + rot.getStepZ(), 1, 0.1, 0.1, 0.1, 0.02);
                server.sendParticles(ParticleTypes.END_ROD, x - rot.getStepX(), worldPosition.getY() + 2.5, z - rot.getStepZ(), 1, 0.1, 0.1, 0.1, 0.02);
                // to stop this acting as an endgame battery it empties itself right after the power has been taken
                if (++cooldown > 10) {
                    power = 0;
                    cooldown = 0;
                }
            }
            if (was != isConverting) setChanged();
            networkPackNT(250);
        }

        @Override
        public boolean provideEnergy(BlockPos at, long energy) {
            if (!beamCell(this, at, 4)) return false;
            power = energy;
            cooldown = 0;
            return true;
        }

        @Override
        public long getPower() {
            return power;
        }

        @Override
        public void setPower(long p) {
            power = Math.max(0L, p);
        }

        @Override
        public long getMaxPower() {
            return power;
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
            output.writeBoolean(isConverting);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            isConverting = input.readBoolean();
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.machineDysonConverterHE");
        }
    }

    public static class ConverterTU extends BlockMultiblockCore implements ITickingBlock {
        public ConverterTU(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ConverterTUBE(pos, state);
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, TU_DIMS);
            l.heat(0, 1, 0).heat(0, 2, 0);
            return l.build(this, declaredCaps, TU_DIMS, 6, 0);
        }
    }

    public static class ConverterTUBE extends BlockEntityMachineBase implements Converter, IHeatSource {
        public int heatEnergy;

        public ConverterTUBE(BlockPos pos, BlockState state) {
            super(CONVERTER_TU_BE.get(), pos, state, 0);
        }

        @Override
        public boolean provideEnergy(BlockPos at, long energy) {
            if (!beamCell(this, at, 6)) return false;
            if (energy > Integer.MAX_VALUE) {
                heatEnergy = Integer.MAX_VALUE;
                return true;
            }
            heatEnergy += (int) energy;
            if (heatEnergy < 0) heatEnergy = Integer.MAX_VALUE;
            return true;
        }

        @Override
        public int getHeatStored(Level level, BlockPos pos) {
            return BlockMultiblockCore.vendsHeatAt(this, pos) ? heatEnergy : 0;
        }

        @Override
        public void useUpHeat(Level level, BlockPos pos, int heat) {
            if (!BlockMultiblockCore.vendsHeatAt(this, pos)) return;
            heatEnergy = Math.max(0, heatEnergy - heat);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            heatEnergy = input.getIntOr("heatEnergy", 0);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putInt("heatEnergy", heatEnergy);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.machineDysonConverterTU");
        }
    }

    public static class ConverterAnatmo extends BlockMultiblockCore implements ITickingBlock, ILookOverlay {
        public ConverterAnatmo(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ConverterAnatmoBE(pos, state);
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, ANATMO_DIMS);
            return l.build(this, declaredCaps, ANATMO_DIMS, 5, 0);
        }

        @Override
        protected InteractionResult useItemOnAtCore(ItemStack held, BlockState coreState, Level level, BlockPos core, Player player,
                InteractionHand hand, BlockHitResult hit) {
            if (!(level.getBlockEntity(core) instanceof ConverterAnatmoBE converter)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (held.getItem() instanceof IToolable.Tool tool && tool.toolType() == IToolable.ToolType.SCREWDRIVER) {
                if (!level.isClientSide()) {
                    converter.isEmitting = !converter.isEmitting;
                    converter.markChanged();
                }
                return InteractionResult.SUCCESS;
            }
            FluidIdentifierData data = held.get(ModDataComponents.FLUID_IDENTIFIER.get());
            if (data == null || data.primary() == Fluids.EMPTY) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                Fluid type = data.primary();
                if (NTMFluidProperties.hasTrait(type, FT_Gaseous.class)) {
                    converter.fluid = type;
                    converter.markChanged();
                    player.sendSystemMessage(Component.literal("Changed type to ").append(type.getFluidType().getDescription()).append("!")
                            .withStyle(ChatFormatting.YELLOW));
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            BlockPos core = overlayCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof ConverterAnatmoBE converter)) return;
            CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);
            double pressure = atmosphere != null ? atmosphere.getPressure(converter.fluid) : 0;
            if (pressure < 0.0001) pressure = 0;
            pressure = Math.round(pressure * 1_000.0) / 1_000.0;
            info.title(getName(), 0xFFFF00, 0x404000);
            info.line(Component.literal("Current rate: " + ((double) converter.gasProduced * 20 * 60 * 60 / com.hbm.space.AstronomyUtil.MB_PER_ATM) + "atm per hour"), 0xFFFFFF);
            info.line(Component.literal("Current gas: ").append(converter.fluid.getFluidType().getDescription()).append(" - " + pressure), 0xFFFFFF);
            info.line(Component.literal("Current mode: " + (converter.isEmitting ? "EMITTING" : "CAPTURING")), 0xFFFFFF);
        }
    }

    public static class ConverterAnatmoBE extends BlockEntityMachineBase implements Converter, SyncUnitSchema {
        public Fluid fluid = com.hbm.inventory.fluid.NTMFluids.byName("OXYGEN");
        public boolean isEmitting = true;
        public long gasProduced;
        public boolean isConverting;

        public ConverterAnatmoBE(BlockPos pos, BlockState state) {
            super(CONVERTER_ANATMO_BE.get(), pos, state, 0);
        }

        @Override
        public void tickServer() {
            boolean was = isConverting;
            isConverting = gasProduced > 0;
            if (was != isConverting) setChanged();
            networkPackNT(250);
            gasProduced = 0;
        }

        @Override
        public boolean provideEnergy(BlockPos at, long energy) {
            if (!beamCell(this, at, 5) || level == null) return false;
            long volume = energy / HE_TO_MB;
            gasProduced += volume;
            if (isEmitting) CelestialBody.emitGas(level, fluid, volume);
            else CelestialBody.consumeGas(level, fluid, volume);
            return true;
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
            output.writeBoolean(isConverting);
            output.writeLong(gasProduced);
            output.writeBoolean(isEmitting);
            byte[] name = BuiltInRegistries.FLUID.getKey(fluid).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            output.writeInt(name.length);
            output.writeBytes(name);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            isConverting = input.readBoolean();
            gasProduced = input.readLong();
            isEmitting = input.readBoolean();
            byte[] name = new byte[input.readInt()];
            input.readBytes(name);
            fluid = fluidOf(new String(name, java.nio.charset.StandardCharsets.UTF_8));
        }

        private static Fluid fluidOf(String id) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            return rl == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(rl);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            Fluid saved = fluidOf(input.getStringOr("fluid", ""));
            if (saved != Fluids.EMPTY) fluid = saved;
            isEmitting = input.getBooleanOr("emit", true);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
            output.putBoolean("emit", isEmitting);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.dysonConverterAnatmogenesis");
        }
    }
}
