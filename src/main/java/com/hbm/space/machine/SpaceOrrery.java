// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.rocket.SpaceLayouts;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The orrery: a block that shows the star system around it, the bodies where they are and their orbits drawn out. */
public final class SpaceOrrery {

    private SpaceOrrery() {}

    public static final int[] DIMS = {0, 0, 0, 0, 0, 0};

    public static final RegistryHandle<Orrery> BLOCK = Reg.blockItem("orrery", Orrery::new,
            () -> BlockBehaviour.Properties.of().strength(10F, 12F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<OrreryBE> BE = Reg.be("orrery",
            () -> new BlockEntityType<>(OrreryBE::new, Set.of(BLOCK.get()), null));

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("atmosphere_editor"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Orrery extends BlockMultiblockCore implements ITickingBlock {
        public Orrery(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new OrreryBE(pos, state);
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            return new SpaceLayouts().box(0, 0, 0, DIMS).build(this, declaredCaps, DIMS, 0, 0);
        }
    }

    public static class OrreryBE extends BlockEntityMachineBase {
        public OrreryBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 0);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.hbm.orrery");
        }
    }
}
