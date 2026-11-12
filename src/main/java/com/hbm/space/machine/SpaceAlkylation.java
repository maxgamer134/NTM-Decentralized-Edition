// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.backport.InteractionResult;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.CreativeContents;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.lib.Library;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.space.rocket.SpaceLayouts;
import com.hbm.util.BobMathUtil;
import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The alkylation unit: 100 mB of the fluid set with an identifier, plus a reagent, becomes two
 * products every other tick for 4,000 HE.
 */
public final class SpaceAlkylation {

    private SpaceAlkylation() {}

    public static final int[] DIMS = {3, 0, 2, 2, 1, 1};
    public static final long MAX_POWER = 1_000_000;

    /** Input to (reagent, product, byproduct); a null reagent means none. */
    public record Recipe(String input, @Nullable String reagent, int reagentAmount, String product, int productAmount,
            String byproduct, int byproductAmount) {}

    private static final Map<String, Recipe> RECIPES = new LinkedHashMap<>();

    private static void add(String in, @Nullable String reagent, int ra, String product, int pa, String by, int ba) {
        RECIPES.put(in, new Recipe(in, reagent, ra, product, pa, by, ba));
    }

    static {
        // halogenated methane alkylates
        add("CHLOROMETHANE", null, 0, "UNSATURATEDS", 75, "CHLORINE", 25);
        // the unit's iron oxide catalyst and heat make cyclic chains
        add("UNSATURATEDS", null, 0, "AROMATICS", 70, "PETROLEUM", 30);
        // dichloromethane alkylates further, into high-octane aromatics
        add("AROMATICS", "RADIOSOLVENT", 40, "XYLENE", 100, "CHLORINE", 10);
        // as real alkylation units turn isobutane into gasoline, with plenty of sulfuric acid
        add("PETROLEUM", "SULFURIC_ACID", 50, "LIGHTOIL", 50, "PEROXIDE", 20);
        add("XYLENE", "CCL", 60, "REFORMGAS", 100, "CHLOROMETHANE", 50);
        add("CHLOROETHANE", "UNSATURATEDS", 60, "CBENZ", 70, "CHLOROMETHANE", 10);
    }

    public static Iterable<Recipe> recipes() {
        return RECIPES.values();
    }

    public static @Nullable Recipe recipeFor(@Nullable Fluid input) {
        if (input == null) return null;
        for (Recipe r : RECIPES.values()) if (NTMFluids.byName(r.input()) == input) return r;
        return null;
    }

    public static final RegistryHandle<Alkylation> BLOCK = Reg.blockItem("machine_alkylation", Alkylation::new,
            () -> BlockBehaviour.Properties.of().strength(5F, 10F).requiresCorrectToolForDrops().noOcclusion());
    public static final Reg.BeHandle<AlkylationBE> BE = Reg.be("machine_alkylation",
                    () -> new BlockEntityType<>(AlkylationBE::new, Set.of(BLOCK.get()), null))
            .ticksServer(BlockEntityMachineBase::tickServer);

    public static void init() {
        CreativeContents.insertAfter(CreativeContents.Tab.MACHINES, Library.id("machine_catalytic_reformer"), () -> new ItemStack(BLOCK.get()));
    }

    public static class Alkylation extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock, ILookOverlay {
        public Alkylation(Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new AlkylationBE(pos, state);
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(BE).powerIn().fluidIn().fluidOut().fe();
        }

        @Override
        protected MultiblockLayout runtimeLayout(int declaredCaps) {
            SpaceLayouts l = new SpaceLayouts().box(0, 0, 0, DIMS);
            int noDown = MASK_ALL & ~MASK_DOWN;
            for (int x : new int[] {-1, 1}) for (int z : new int[] {-2, 0, 2}) l.port(x, 0, z, noDown);
            return l.build(this, declaredCaps, DIMS, 2, 0);
        }

        @Override
        protected InteractionResult useItemOnAtCore(ItemStack stack, BlockState coreState, Level level, BlockPos core,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (player.isShiftKeyDown() || !(stack.getItem() instanceof FluidIdentifierItem)) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(level.getBlockEntity(core) instanceof AlkylationBE be)) return InteractionResult.PASS;
            Fluid type = stack.getOrDefault(ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY).primary();
            if (type == null || type == Fluids.EMPTY) return InteractionResult.SUCCESS;
            be.tanks[0].setTankType(type);
            be.setChanged();
            player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                    .append(NTMFluidProperties.getDisplayName(type)).append("!"));
            return InteractionResult.SUCCESS;
        }

        @Override
        public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
            BlockPos core = overlayCore(level, pos);
            if (core == null || !(level.getBlockEntity(core) instanceof AlkylationBE be)) return;
            info.title(getName(), 0xFFFF00, 0x404000);
            info.line(Component.literal("Power: " + BobMathUtil.getShortNumber(be.power) + "HE"),
                    be.power < MAX_POWER / 20 ? 0xFF5555 : 0x55FF55);
            for (int i = 0; i < be.tanks.length; i++) {
                FluidTankNTM tank = be.tanks[i];
                if (tank.getFluid() == null || tank.getFluid() == NTMFluids.NONE) continue;
                Component line = Component.empty().append(tank.getFluid().getFluidType().getDescription())
                        .append(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB");
                if (i < 2) info.input(line);
                else info.output(line);
            }
        }
    }

    public static class AlkylationBE extends BlockEntityMachineBase
            implements IEnergyHandlerMK2, IFluidHandlerMK2, FluidFlushSender, FluidTankAccess, SyncUnitSchema {
        public final FluidTankNTM[] tanks = {
            new FluidTankNTM(NTMFluids.byName("CHLOROMETHANE"), 8_000), new FluidTankNTM(NTMFluids.NONE, 4_000),
            new FluidTankNTM(NTMFluids.byName("UNSATURATEDS"), 8_000), new FluidTankNTM(NTMFluids.byName("CHLORINE"), 8_000)
        };
        private final FluidTankNTM[] receiving = {tanks[0], tanks[1]};
        private final FluidTankNTM[] sending = {tanks[2], tanks[3]};
        private final FluidFlushOutputs flush = new FluidFlushOutputs();
        public long power;

        public AlkylationBE(BlockPos pos, BlockState state) {
            super(BE.get(), pos, state, 11);
        }

        @Override
        public void tickServer() {
            if (!(level instanceof ServerLevel server)) return;
            if (server.getGameTime() % 2 == 0 && alkylate()) setChanged();
            flush.provide(server, this);
            networkPackNT(25);
        }

        private boolean alkylate() {
            Recipe out = recipeFor(tanks[0].getFluid());
            if (out == null) {
                tanks[2].setTankType(NTMFluids.NONE);
                tanks[3].setTankType(NTMFluids.NONE);
                return false;
            }
            tanks[1].setTankType(out.reagent() == null ? NTMFluids.NONE : NTMFluids.byName(out.reagent()));
            tanks[2].setTankType(NTMFluids.byName(out.product()));
            tanks[3].setTankType(NTMFluids.byName(out.byproduct()));
            if (power < 4_000) return false; // 40 kHE/s
            if (tanks[0].getFill() < 100) return false;
            if (tanks[1].getFill() < out.reagentAmount()) return false;
            if (tanks[2].getFill() + out.productAmount() > tanks[2].getMaxFill()) return false;
            if (tanks[3].getFill() + out.byproductAmount() > tanks[3].getMaxFill()) return false;
            tanks[0].setFill(tanks[0].getFill() - 100);
            tanks[1].setFill(tanks[1].getFill() - out.reagentAmount());
            tanks[2].setFill(tanks[2].getFill() + out.productAmount());
            tanks[3].setFill(tanks[3].getFill() + out.byproductAmount());
            power -= 4_000;
            return true;
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
        public boolean canReceiveFluid(Fluid type) {
            for (FluidTankNTM tank : receiving) if (tank.joins(type)) return true;
            return false;
        }

        @Override
        public long getDemand(Fluid type, int pressure) {
            if (pressure != 0) return 0;
            for (FluidTankNTM tank : receiving) if (tank.accepts(type)) return tank.getMaxFill() - tank.getFill();
            return 0;
        }

        @Override
        public long transferFluid(Fluid type, int pressure, long amount) {
            if (pressure != 0) return amount;
            for (FluidTankNTM tank : receiving) {
                if (!tank.accepts(type)) continue;
                long left = amount - tank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
                if (left != amount) setChanged();
                return left;
            }
            return amount;
        }

        @Override
        public FluidTankNTM[] getSendingTanks() {
            return sending;
        }

        @Override
        public void declareFlush(FlushLanes out) {
            for (FluidTankNTM tank : sending) out.add(tank, FlushFaces.own());
        }

        @Override
        public long getFluidAvailable(Fluid type, int pressure) {
            long total = 0;
            for (FluidTankNTM tank : sending) if (tank.provides(type) && pressure == tank.getPressure()) total += tank.getFill();
            return total;
        }

        @Override
        public void useUpFluid(Fluid type, int pressure, long amount) {
            for (FluidTankNTM tank : sending) {
                if (amount <= 0) break;
                if (!tank.provides(type) || pressure != tank.getPressure()) continue;
                int drained = tank.drain((int) Math.min(amount, Integer.MAX_VALUE), true);
                amount -= drained;
                if (drained > 0) setChanged();
            }
        }

        @Override
        public void forEachTank(Consumer<FluidTankNTM> visitor) {
            for (FluidTankNTM tank : tanks) visitor.accept(tank);
        }

        @Override
        public int tankCount() {
            return tanks.length;
        }

        @Override
        public FluidTankNTM tank(int index) {
            return tanks[index];
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("container.alkylation");
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
            output.writeLong(power);
            for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) {
                super.readSyncUnit(unit, input);
                return;
            }
            power = input.readLong();
            for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            power = input.getLongOr("power", 0L);
            for (int i = 0; i < tanks.length; i++) {
                final FluidTankNTM tank = tanks[i];
                input.child("t" + i).ifPresent(tank::deserialize);
            }
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("power", power);
            for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child("t" + i));
        }
    }
}
