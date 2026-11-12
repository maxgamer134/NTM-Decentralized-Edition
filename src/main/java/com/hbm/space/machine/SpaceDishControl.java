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
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.IGUIProvider;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemReactorSensor;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.space.rocket.SpaceStardar;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The StarDar remote access console: once linked to a StarDar with a reactor sensor (record the
 * StarDar, then use the sensor on the console), right-clicking it opens that StarDar's screen.
 */
public final class SpaceDishControl {

    private SpaceDishControl() {}

    public static final int[] DIMS = {0, 0, 0, 0, 0, 1};

    public static final RegistryHandle<DishControl> BLOCK = Reg.blockItem("dish_control", DishControl::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());

    public static final Reg.BeHandle<DishControlBE> BE = Reg.be("dish_control",
                    () -> new BlockEntityType<>(DishControlBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MISSILES, Library.id("machine_stardar"), () -> new ItemStack(BLOCK.get()));
        ItemReactorSensor.addonTarget(SpaceDishControl::sensorUse);
    }

    /** The reactor sensor records a StarDar, and hands what it recorded to a console. */
    private static InteractionResult sensorUse(UseOnContext context, @Nullable BlockPos core) {
        if (core == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockState state = level.getBlockState(core);
        if (state.is(SpaceStardar.BLOCK.get()))
            return ItemReactorSensor.recordPosition(context, Component.translatable("chat.reactorSensor.positionSet"));
        if (!state.is(BLOCK.get())) return InteractionResult.PASS;
        Long packed = context.getItemInHand().get(ModDataComponents.REACTOR_POS.get());
        if (packed == null) return InteractionResult.PASS;
        Player player = context.getPlayer();
        if (!level.isClientSide() && level.getBlockEntity(core) instanceof DishControlBE console) {
            boolean linked = console.tryLink(BlockPos.of(packed));
            if (player != null)
                ItemReactorSensor.announce(player, Component.translatable(linked ? "chat.dishControl.linked" : "chat.dishControl.missing"));
        }
        ItemReactorSensor.confirm(context);
        return InteractionResult.SUCCESS;
    }

    public static class DishControl extends BlockMultiblockCore implements ITickingBlock {
        public DishControl(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new DishControlBE(pos, state);
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            return new SpaceLayouts().box(0, 0, 0, DIMS).build(this, declaredCaps, DIMS, 0, 0);
        }

        @Override
        protected InteractionResult useAtCore(BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
            if (player.isSecondaryUseActive()) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(level.getBlockEntity(core) instanceof DishControlBE console)) return InteractionResult.PASS;
            if (!console.isLinked) {
                tell(player, "chat.dishControl.notLinked");
                return InteractionResult.SUCCESS;
            }
            BlockPos link = console.linkPosition;
            // the StarDar's screen reads it on the client, so its chunk has to be loaded there too
            if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server) || !level.isLoaded(link)
                    || !server.getChunkSource().chunkMap.getPlayers(new ChunkPos(link), false).contains(sp)) {
                tell(player, "chat.dishControl.notLoaded");
                return InteractionResult.SUCCESS;
            }
            if (!(level.getBlockEntity(link) instanceof SpaceStardar.StardarBE stardar)) {
                tell(player, "chat.dishControl.missing");
                return InteractionResult.SUCCESS;
            }
            MenuProvider provider = new SimpleMenuProvider((id, inv, p) -> {
                SpaceStardar.Menu menu = new SpaceStardar.Menu(id, inv, stardar);
                menu.remote = true;
                return menu;
            }, stardar.getDisplayName());
            IGUIProvider.openBlockMenu(player, provider, link);
            return InteractionResult.SUCCESS;
        }

        private void tell(Player player, String key) {
            player.sendSystemMessage(Component.literal("[")
                    .append(Component.translatable(getDescriptionId()))
                    .append("] ")
                    .withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.translatable(key).withStyle(ChatFormatting.RED)));
        }
    }

    public static class DishControlBE extends BlockEntityMachineBase implements SyncUnitSchema {
        public BlockPos linkPosition = BlockPos.ZERO;
        public boolean isLinked;
        /** Whether the linked StarDar holds a drive, for the drive on the console's model. */
        public boolean hasDrive;
        private boolean foundLink;

        public DishControlBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 0);
        }

        @Override
        public void tickServer() {
            if (isLinked && level.isLoaded(linkPosition)) {
                BlockEntity be = level.getBlockEntity(linkPosition);
                // the link is checked once after loading, as in NTM
                if (!foundLink) {
                    isLinked = be instanceof SpaceStardar.StardarBE;
                    foundLink = true;
                    setChanged();
                }
                hasDrive = be instanceof SpaceStardar.StardarBE stardar && !stardar.getItem(0).isEmpty();
            } else if (!isLinked) {
                hasDrive = false;
            }
            networkPackNT(15);
        }

        /** Links to the StarDar any of whose cells sits at {@code pos}. */
        public boolean tryLink(BlockPos pos) {
            BlockPos core = MultiblockSurface.coreOfAny(level, pos, level.getBlockState(pos));
            isLinked = core != null && level.getBlockEntity(core) instanceof SpaceStardar.StardarBE;
            if (isLinked) linkPosition = core.immutable();
            foundLink = true;
            setChanged();
            return isLinked;
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
            output.writeBoolean(isLinked);
            output.writeLong(linkPosition.asLong());
            output.writeBoolean(hasDrive);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            isLinked = input.readBoolean();
            linkPosition = BlockPos.of(input.readLong());
            hasDrive = input.readBoolean();
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            isLinked = input.getBooleanOr("isLinked", false);
            linkPosition = BlockPos.of(input.getLongOr("linkPos", 0L));
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putBoolean("isLinked", isLinked);
            output.putLong("linkPos", linkPosition.asLong());
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.dishControl");
        }
    }
}
