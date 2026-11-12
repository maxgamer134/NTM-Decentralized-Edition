// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blockentity.machine.BlockEntityCondenser;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The steam radiator: a condenser that shrugs off the vacuum, since it sheds its heat by radiation. */
public final class SpaceRadiator {

    private SpaceRadiator() {}

    public static final int[] DIMS = {1, 1, 0, 0, 1, 1};
    public static final int TANK = 500;

    public static final RegistryHandle<Radiator> BLOCK = Reg.blockItem("machine_radiator", Radiator::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<RadiatorBE> BE = Reg.be("machine_radiator",
                    () -> new BlockEntityType<>(RadiatorBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_condenser_powered"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Radiator extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {
        public Radiator(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new RadiatorBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).fluidIn().fluidOut();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts layout = new SpaceLayouts().box(0, 0, 0, DIMS);
            layout.port(1, 1, 0);
            layout.port(1, -1, 0);
            layout.port(-1, 1, 0);
            layout.port(-1, -1, 0);
            return layout.build(this, declaredCaps, DIMS, 0, 0);
        }
    }

    public static class RadiatorBE extends BlockEntityCondenser {
        public RadiatorBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, TANK, TANK);
        }

        @Override
        protected boolean vacuumProof() {
            return true;
        }
    }
}
