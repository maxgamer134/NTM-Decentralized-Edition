// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.rocket.SpaceLayouts;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The solar panel: about 100 HE/t in Kerbin's sun, more or less with each body's distance. */
public final class SpaceSolar {

    private SpaceSolar() {}

    public static final int[] DIMS = {0, 0, 2, 2, 0, 0};

    public static final RegistryHandle<Panel> BLOCK = Reg.blockItem("machine_solar", Panel::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<PanelBE> BE = Reg.be("machine_solar",
                    () -> new BlockEntityType<>(PanelBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_solar_boiler"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Panel extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {
        public Panel(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new PanelBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerOut().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, DIMS);
            for (int z = -2; z <= 2; z++) layout.port(0, 0, z);
            return layout.build(this, declaredCaps, DIMS, 0, 0);
        }
    }

    public static class PanelBE extends BlockEntityMachineBase implements IEnergyHandlerMK2 {
        public static final long MAX_POWER = 1_000;
        private long power;

        public PanelBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 0);
        }

        @Override
        public void tickServer() {
            if (level == null) return;
            // sun ranges 1-4
            int sun = level.getBrightness(LightLayer.SKY, worldPosition) - level.getSkyDarken() - 11;
            if (sun <= 0 || !level.canSeeSky(worldPosition.above())) return;
            // eclipses dim the panel too
            float sunPower = com.hbm.space.SpaceEclipse.sunPower(level, worldPosition);
            power = Math.min(MAX_POWER, power + Mth.ceil(sun * 25 * sunPower));
        }

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
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            power = input.getLongOr("power", 0L);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("power", power);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.solarPanel");
        }
    }
}
