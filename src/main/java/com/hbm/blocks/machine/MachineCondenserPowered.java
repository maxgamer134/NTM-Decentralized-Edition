// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityCondenserPowered;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.BobMathUtil;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineCondenserPowered extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock, com.hbm.interfaces.IToolable {
    public MachineCondenserPowered(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(DamageableMachine.DAMAGED, false));
    }

    @Override
    protected void createBlockStateDefinition(
            net.minecraft.world.level.block.state.StateDefinition.Builder<
                            net.minecraft.world.level.block.Block, BlockState>
                    builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DamageableMachine.DAMAGED);
    }

    // NTM:Space: the first explosion wrecks the machine, the second removes it
    @Override
    protected void onExplosionHit(
            BlockState state,
            net.minecraft.server.level.ServerLevel level,
            BlockPos core,
            net.minecraft.world.level.Explosion explosion,
            java.util.function.BiConsumer<net.minecraft.world.item.ItemStack, BlockPos> onHit) {
        if (DamageableMachine.onExplosion(level, core, explosion)) return;
        super.onExplosionHit(state, level, core, explosion, onHit);
    }

    @Override
    public boolean dropFromExplosion(net.minecraft.world.level.Explosion explosion) {
        return !com.hbm.space.SpaceToggle.changes() && super.dropFromExplosion(explosion);
    }

    @Override
    public boolean onScrew(
            Level level,
            net.minecraft.world.entity.player.Player player,
            BlockPos core,
            net.minecraft.core.@Nullable Direction side,
            net.minecraft.world.phys.Vec3 hit,
            com.hbm.interfaces.IToolable.ToolType tool) {
        if (tool != com.hbm.interfaces.IToolable.ToolType.TORCH) return false;
        return com.hbm.interfaces.IRepairable.tryRepairMultiblock(level, core, player);
    }

    // backport-fix: BF-137 main colours only the arrow; the fluid or item name after it stays white
    private static void line(ILookOverlay.LookInfo info, boolean input, FluidTankNTM tank) {
        info.flow(
                input, NTMFluidProperties.clientName(tank.getFluid())
                        + ": "
                        + String.format(Locale.US, "%,d", tank.getFill())
                        + "/"
                        + String.format(Locale.US, "%,d", tank.getMaxFill())
                        + "mB");
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCondenserPowered(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        var owner = BlockMultiblockCore.ownerOf(level, pos);
        BlockPos core = owner != null ? owner.pos() : pos;
        if (!(level.getBlockEntity(core) instanceof BlockEntityCondenserPowered be)) return;
        if (DamageableMachine.isDamaged(level.getBlockState(core))) {
            com.hbm.client.RepairLookOverlay.build(level, core, info);
            return;
        }
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        info.line(
                BobMathUtil.getShortNumber(be.power)
                        + "HE / "
                        + BobMathUtil.getShortNumber(be.getMaxPower())
                        + "HE"); // backport-fix: BF-137 white like main's
        line(info, true, be.input);
        line(info, false, be.output);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().fluidIn().fluidOut().fe();
    }
}
