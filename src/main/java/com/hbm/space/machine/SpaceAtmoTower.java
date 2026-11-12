// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.machine.BlockEntityDeuteriumExtractor;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.util.BobMathUtil;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The atmosphere tower: the deuterium tower's process on air, 50 mB of air for 1 mB of nitrogen. */
public final class SpaceAtmoTower {

    private SpaceAtmoTower() {}

    public static final int[] DIMS = {6, 0, 1, 0, 0, 1};
    public static final long MAX_POWER = 1_000_000L;

    public static final RegistryHandle<Tower> BLOCK = Reg.blockItem("machine_atmo_tower", Tower::new,
            () -> BlockBehaviour.Properties.of().strength(10F, 12F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<TowerBE> BE = Reg.be("machine_atmo_tower",
                    () -> new BlockEntityType<>(TowerBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_deuterium_tower"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Tower extends BlockMultiblockCore implements ITickingBlock, ILookOverlay, ICapabilityBlock {
        public Tower(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TowerBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().fluidIn().fluidOut().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, DIMS);
            l.port(0, 0, 0);
            l.port(1, 0, 0);
            l.port(0, 0, -1);
            l.port(1, 0, -1);
            return l.build(this, declaredCaps, DIMS, 0, 0);
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            if (!(level.getBlockEntity(pos) instanceof TowerBE be)) return;
            info.title(Component.empty().append(Component.translatable(getDescriptionId())), 0xFFFF00, 0x404000);
            info.line("Power: " + BobMathUtil.getShortNumber(be.power) + "HE", be.hasPower() ? 0x55FF55 : 0xFF5555);
            for (int i = 0; i < be.tanks.length; i++) {
                FluidTankNTM tank = be.tanks[i];
                info.flow(i < 1, NTMFluidProperties.clientName(tank.getFluid()) + ": " + tank.getFill() + "/" + tank.getMaxFill() + "mB");
            }
        }
    }

    public static class TowerBE extends BlockEntityDeuteriumExtractor {
        public TowerBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 50_000, 5_000);
            tanks[0].setTankType(NTMFluids.byName("EARTHAIR"));
            tanks[1].setTankType(NTMFluids.byName("NITROGEN"));
        }

        @Override
        public long getMaxPower() {
            return MAX_POWER;
        }
    }
}
