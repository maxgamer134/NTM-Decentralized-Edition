// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.backport.InteractionResult;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.AstronomyUtil;
import com.hbm.space.CelestialBody;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.space.trait.CBT_Atmosphere;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The atmosphere editor: a creative block that puts gas into a body's atmosphere or takes it out, with no power or tank.
 * A screwdriver switches it on and off, a hand drill raises or lowers the throughput by a power of ten, a defuser swaps
 * emitting and capturing, and a fluid identifier picks the gas.
 */
public final class SpaceAtmoEditor {

    private SpaceAtmoEditor() {}

    public static final int[] DIMS = {0, 0, 0, 0, 0, 0};

    public static final RegistryHandle<Editor> BLOCK = Reg.blockItem("atmosphere_editor", Editor::new,
            () -> BlockBehaviour.Properties.of().strength(3F, 3F).requiresCorrectToolForDrops());
    public static final Reg.BeHandle<EditorBE> BE = Reg.be("atmosphere_editor",
                    () -> new BlockEntityType<>(EditorBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("atmospheric_emitter"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Editor extends BlockMultiblockCore implements ITickingBlock, ILookOverlay {
        public Editor(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new EditorBE(pos, state);
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            return new SpaceLayouts().box(0, 0, 0, DIMS).build(this, declaredCaps, DIMS, 0, 0);
        }

        @Override
        protected InteractionResult useItemOnAtCore(ItemStack held, BlockState coreState, Level level, BlockPos core, Player player,
                InteractionHand hand, BlockHitResult hit) {
            if (!(level.getBlockEntity(core) instanceof EditorBE be)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (held.getItem() instanceof IToolable.Tool tool) {
                IToolable.ToolType type = tool.toolType();
                if (type == IToolable.ToolType.SCREWDRIVER) {
                    if (!level.isClientSide()) be.toggle();
                    return InteractionResult.SUCCESS;
                }
                if (type == IToolable.ToolType.HAND_DRILL) {
                    if (!level.isClientSide()) be.changeThroughput(player.isShiftKeyDown() ? -1 : 1);
                    return InteractionResult.SUCCESS;
                }
                if (type == IToolable.ToolType.DEFUSER) {
                    if (!level.isClientSide()) be.swapMode();
                    return InteractionResult.SUCCESS;
                }
            }
            FluidIdentifierData data = held.get(ModDataComponents.FLUID_IDENTIFIER.get());
            if (data != null && data.primary() != null) {
                if (!level.isClientSide()) {
                    be.setGas(data.primary());
                    player.displayClientMessage(Component.literal("Changed type to ").append(data.primary().getFluidType().getDescription()).append("!"), true);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            BlockPos core = overlayCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof EditorBE be)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);
            double pressure = atmosphere != null ? atmosphere.getPressure(be.gas) : 0;
            if (pressure < 0.0001) pressure = 0;
            pressure = Math.round(pressure * 1_000.0) / 1_000.0;
            info.line(Component.literal("State: " + (be.isOn ? "RUNNING" : "OFF")), be.isOn ? 0x55FF55 : 0xFF5555);
            info.line(Component.literal("Current gas: ").append(be.gas.getFluidType().getDescription()).append(" - " + pressure), 0xFFFFFF);
            info.line(Component.literal("Current mode: " + (be.isEmitting ? "EMITTING" : "CAPTURING")), 0xFFFFFF);
            info.line(Component.literal("Current throughput: " + Math.pow(10, be.throughputFactor) / AstronomyUtil.MB_PER_ATM), 0xFFFFFF);
        }
    }

    public static class EditorBE extends BlockEntityMachineBase implements SyncUnitSchema {
        public boolean isOn;
        public int throughputFactor = 10;
        public Fluid gas = NTMFluids.byName("EARTHAIR");
        public boolean isEmitting = true;

        public EditorBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 0);
        }

        void toggle() {
            isOn = !isOn;
            setChanged();
        }

        void changeThroughput(int by) {
            throughputFactor += by;
            setChanged();
        }

        void swapMode() {
            isEmitting = !isEmitting;
            setChanged();
        }

        void setGas(Fluid fluid) {
            gas = fluid;
            setChanged();
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            if (isOn && server.getGameTime() % 5 == 0) {
                double amount = Math.pow(10, throughputFactor);
                if (isEmitting) CelestialBody.emitGas(server, gas, amount);
                else CelestialBody.consumeGas(server, gas, amount);
            }
            networkPackNT(25);
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
            output.writeBoolean(isOn);
            output.writeInt(throughputFactor);
            String name = String.valueOf(NTMFluids.legacyName(gas));
            output.writeInt(name.length());
            output.writeCharSequence(name, java.nio.charset.StandardCharsets.UTF_8);
            output.writeBoolean(isEmitting);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            isOn = input.readBoolean();
            throughputFactor = input.readInt();
            String name = input.readCharSequence(input.readInt(), java.nio.charset.StandardCharsets.UTF_8).toString();
            try {
                gas = NTMFluids.byName(name);
            } catch (IllegalStateException ignored) {
                // keep the gas we had
            }
            isEmitting = input.readBoolean();
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            isOn = input.getBooleanOr("isOn", false);
            throughputFactor = input.getIntOr("throughput", 10);
            isEmitting = input.getBooleanOr("emit", true);
            String name = input.getStringOr("fluid", "EARTHAIR");
            try {
                gas = NTMFluids.byName(name);
            } catch (IllegalStateException ignored) {
                gas = NTMFluids.byName("EARTHAIR");
            }
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putBoolean("isOn", isOn);
            output.putInt("throughput", throughputFactor);
            output.putBoolean("emit", isEmitting);
            String name = NTMFluids.legacyName(gas);
            output.putString("fluid", name == null ? "EARTHAIR" : name);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.hbm.atmosphere_editor");
        }
    }
}
