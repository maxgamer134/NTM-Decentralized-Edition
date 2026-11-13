// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.machine.BlockEntityCrucible;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.CelestialBody;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.util.BobMathUtil;
import com.hbm.util.CrucibleUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The magma drill: on Moho, over a 3x3 of lava four blocks down, it draws slag and mineral-rich
 * magma for 10,000 HE a tick and pours them out of its spout like a crucible.
 */
public final class SpaceMagma {

    private SpaceMagma() {}

    public static final int[] DIMS = {3, 3, 3, 3, 3, 3};
    public static final long MAX_POWER = 1_000_000;
    public static final long CONSUMPTION = 10_000;
    public static final int MAX_LIQUID = MaterialShapes.BLOCK.q(16);

    /** Mineral-rich magma: Space only, so it exists only with Space on; its ingot is ingot_magma (c:ingots/magma). */
    public static final NTMMaterial MAT_RICH_MAGMA =
            Mats.makeSmeltable(24_002, Mats.df("magma"), 0x7F7F7F, 0x353555, 0xFF6212);

    public static final RegistryHandle<Magma> BLOCK = Reg.blockItem("machine_magma", Magma::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion(),
            (block, props) -> new com.hbm.items.DescBlockItem(block, props, 1, com.hbm.items.DescBlockItem.Desc.PLAIN));
    public static final Reg.BeHandle<MagmaBE> BE = Reg.be("machine_magma",
                    () -> new BlockEntityType<>(MagmaBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer).ticksClient(BlockEntityMachineBase::tickClient);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_alkylation"), () -> new ItemStack(BLOCK.get()));
    }

    static boolean onMoho(Level level) {
        return "moho".equals(CelestialBody.getBody(level).name);
    }

    public static class Magma extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock, ILookOverlay {
        public Magma(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new MagmaBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, DIMS);
            // six power ports on the back face, NTM's core - dir * 3 (+- rot), one and two below the core
            for (int x = -1; x <= 1; x++) {
                l.port(x, -1, -3);
                l.port(x, -2, -3);
            }
            return l.build(this, declaredCaps, DIMS, 3, 3);
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            BlockPos core = overlayCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof MagmaBE drill)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            int blink = BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00;
            if (!onMoho(level)) {
                info.line(Component.literal("! ! ! MUST BE ON MOHO ! ! !"), blink);
            } else if (!drill.validPosition) {
                info.line(Component.literal("! ! ! INSUFFICIENT LAVA FOUND ! ! !"), blink);
            } else {
                info.line(Component.literal("Power: " + BobMathUtil.getShortNumber(drill.power) + "HE"),
                        drill.power < CONSUMPTION ? 0xFF5555 : 0x55FF55);
                for (MaterialStack sta : drill.liquids) {
                    info.line(Component.empty().append(sta.material.getLocalizedName())
                            .append(": ").append(Mats.formatAmount(sta.amount, false)), 0xFFFF55);
                }
            }
        }
    }

    public static class MagmaBE extends BlockEntityMachineBase implements IEnergyHandlerMK2, SyncUnitSchema {
        public long power;
        public boolean operating;
        public boolean validPosition = true;
        public final List<MaterialStack> liquids = new ArrayList<>();
        public final List<BlockEntityCrucible.PourStream> streams = new ArrayList<>();
        private int pourColor = -1;
        private float pourLen;

        public float drillSpeed, drillRotation, prevDrillRotation;
        public float lavaHeight, prevLavaHeight;

        public MagmaBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 0);
        }

        public Direction facing() {
            return getBlockState().getValue(BlockMultiblockCore.FACING);
        }

        @Override
        public void tickServer() {
            pourColor = -1;
            operating = canOperate();
            if (operating) {
                power -= CONSUMPTION;
                if (level.getGameTime() % 10 == 0) {
                    MaterialStack[] outputs = {
                        new MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.q(1)),
                        new MaterialStack(MAT_RICH_MAGMA, MaterialShapes.QUANTUM.q(4)),
                    };
                    for (MaterialStack mat : outputs) {
                        int total = 0;
                        for (MaterialStack m : liquids) total += m.amount;
                        if (total + mat.amount > MAX_LIQUID) break;
                        addToStack(mat);
                    }
                }
                setChanged();
            }

            if (!liquids.isEmpty()) {
                Direction dir = facing();
                int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
                CrucibleUtil.ImpactPos impact = new CrucibleUtil.ImpactPos();
                MaterialStack didPour = CrucibleUtil.pourFullStack(level,
                        x + 0.5D + dir.getStepX() * 3.875D, y + 1.25D, z + 0.5D + dir.getStepZ() * 3.875D,
                        6, true, liquids, MaterialShapes.INGOT.q(1), impact);
                if (didPour != null) {
                    pourColor = didPour.material.moltenColor;
                    pourLen = Math.max(1F, y + 1 - (float) (Math.ceil(impact.y) - 0.875));
                    markSyncEvent();
                    setChanged();
                }
            }
            liquids.removeIf(o -> o.amount <= 0);
            networkPackNT(250);
        }

        @Override
        public void tickClient() {
            long now = level.getGameTime();
            streams.removeIf(s -> now - s.birth() >= 20);
            prevLavaHeight = lavaHeight;
            prevDrillRotation = drillRotation;
            if (operating) {
                drillSpeed = Math.min(15F, drillSpeed + 0.15F);
                lavaHeight += (level.random.nextFloat() - 0.5F) * 0.01F;
                lavaHeight = net.minecraft.util.Mth.lerp(0.02F, lavaHeight, 0.9F);
            } else {
                drillSpeed = Math.max(0F, drillSpeed - 0.3F);
                lavaHeight = net.minecraft.util.Mth.lerp(0.02F, lavaHeight, 0F);
            }
            drillRotation += drillSpeed;
            if (drillRotation > 360F) {
                drillRotation -= 360F;
                prevDrillRotation -= 360F;
            }
        }

        private boolean canOperate() {
            if (!onMoho(level)) return false;
            validPosition = isValidPosition();
            return validPosition && power >= CONSUMPTION;
        }

        private boolean isValidPosition() {
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    if (!level.getBlockState(worldPosition.offset(x, -4, z)).is(Blocks.LAVA)) return false;
            return true;
        }

        private void addToStack(MaterialStack stack) {
            for (MaterialStack mat : liquids) {
                if (mat.material == stack.material) {
                    mat.amount += stack.amount;
                    return;
                }
            }
            liquids.add(stack.copy());
        }

        @Override
        public long getPower() {
            return power;
        }

        @Override
        public void setPower(long p) {
            power = Math.max(0L, Math.min(p, MAX_POWER));
        }

        @Override
        public long getMaxPower() {
            return MAX_POWER;
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.machineMagma");
        }

        // sync: 0 state, 1 liquids, 2 the pour (an event, sent whenever it pours)

        @Override
        public long syncUnitMask() {
            return super.syncUnitMask() | 7L;
        }

        @Override
        public long syncEventUnits() {
            return 1L << 2;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            switch (unit) {
                case 0 -> {
                    output.writeBoolean(operating);
                    output.writeLong(power);
                    output.writeBoolean(validPosition);
                }
                case 1 -> {
                    output.writeShort(liquids.size());
                    for (MaterialStack sta : liquids) {
                        output.writeInt(sta.material.id);
                        output.writeInt(sta.amount);
                    }
                }
                case 2 -> {
                    output.writeInt(pourColor);
                    output.writeFloat(pourLen);
                }
                default -> super.writeSyncUnit(unit, output);
            }
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            switch (unit) {
                case 0 -> {
                    operating = input.readBoolean();
                    power = input.readLong();
                    validPosition = input.readBoolean();
                }
                case 1 -> {
                    liquids.clear();
                    int count = input.readShort();
                    for (int i = 0; i < count; i++) {
                        NTMMaterial mat = Mats.matById.get(input.readInt());
                        int amount = input.readInt();
                        if (mat != null) liquids.add(new MaterialStack(mat, amount));
                    }
                }
                case 2 -> {
                    pourColor = input.readInt();
                    pourLen = input.readFloat();
                }
                default -> super.readSyncUnit(unit, input);
            }
        }

        @Override
        public void afterSyncUnits(long units) {
            if ((units & 1L << 2) != 0 && level != null && level.isClientSide() && pourColor != -1)
                streams.add(new BlockEntityCrucible.PourStream(pourColor, false, pourLen, level.getGameTime()));
        }

        @Override
        public void afterInitialSyncUnits() {}

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            power = input.getLongOr("power", 0L);
            liquids.clear();
            int[] data = input.getIntArray("liquids").orElse(new int[0]);
            for (int i = 0; i + 1 < data.length; i += 2) {
                NTMMaterial mat = Mats.matById.get(data[i]);
                if (mat != null) liquids.add(new MaterialStack(mat, data[i + 1]));
            }
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("power", power);
            int[] data = new int[liquids.size() * 2];
            for (int i = 0; i < liquids.size(); i++) {
                data[i * 2] = liquids.get(i).material.id;
                data[i * 2 + 1] = liquids.get(i).amount;
            }
            output.putIntArray("liquids", data);
        }
    }
}
