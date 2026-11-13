// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.client.ClientEffects;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.PlasmaBlastPayload;
import com.hbm.particle.RocketFlameColor;
import com.hbm.registration.Reg.BeHandle;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;

/**
 * NTM:Space's geysers. The volcanic one only smokes; the chloric one spouts a column of acid
 * vapor that burns whatever stands in it; the electric one charges up for 5 to 25 seconds and
 * then strikes everything in the 60 block column above it. NTM:Space spawned the chloric vapor
 * from the server side, so on a dedicated server nobody saw it; here the client draws it.
 */
public final class SpaceGeysers {

    private SpaceGeysers() {}

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2F).lightLevel(s -> 15)
                .sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    public static final RegistryHandle<Volcanic> GEYSIR_VOLCANIC = Reg.blockItem("geysir_volcanic", Volcanic::new, SpaceGeysers::props);
    public static final RegistryHandle<Chloric> GEYSIR_CHLORIC = Reg.blockItem("geysir_chloric", Chloric::new, SpaceGeysers::props);
    public static final RegistryHandle<Electric> GEYSIR_ELECTRIC = Reg.blockItem("geysir_electric", Electric::new, SpaceGeysers::props);

    public static final BeHandle<ChloricBE> CHLORIC_BE = Reg.be("geysir_chloric",
            () -> new BlockEntityType<>(ChloricBE::new, Set.of(GEYSIR_CHLORIC.get()), null));
    public static final BeHandle<ElectricBE> ELECTRIC_BE = Reg.be("geysir_electric",
            () -> new BlockEntityType<>(ElectricBE::new, Set.of(GEYSIR_ELECTRIC.get()), null));

    public static void init() {
        var anchor = Library.id("moon_turf");
        for (var b : new RegistryHandle<?>[] {GEYSIR_CHLORIC, GEYSIR_ELECTRIC, GEYSIR_VOLCANIC})
            CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, anchor, () -> new ItemStack((Block) b.get()));
    }

    /** Hurts the living things in the column above a geyser. */
    static void zap(Level level, BlockPos pos, ResourceKey<DamageType> type, float fraction, SoundEvent sound) {
        AABB box = new AABB(pos.getX() - 0.5, pos.getY() + 0.5, pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 60, pos.getZ() + 1.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e.hurt(level.damageSources().source(type), Mth.clamp(e.getMaxHealth() * fraction, 3F, 20F)))
                level.playSound(null, e.getX(), e.getY(), e.getZ(), sound, SoundSource.BLOCKS, 1F, 1F);
        }
    }

    public static class Volcanic extends Block {
        public Volcanic(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
            ClientEffects.spawnGasFlame(level, pos.getX() + rand.nextDouble(), pos.getY() + 0.5 + rand.nextDouble(), pos.getZ() + rand.nextDouble(),
                    rand.nextGaussian() * 0.2, 0.1, rand.nextGaussian() * 0.2, 1F);
        }
    }

    abstract static class GeyserBlock extends BaseEntityBlock {
        GeyserBlock(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }
    }

    public static class Chloric extends GeyserBlock {
        public Chloric(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(Chloric::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ChloricBE(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return type == CHLORIC_BE.get() ? (l, p, s, be) -> ChloricBE.tick(l, p) : null;
        }
    }

    public static class ChloricBE extends BlockEntity {
        private static final RocketFlameColor VAPOR = new RocketFlameColor(0xA4 / 255F, 0xD7 / 255F, 0xDD / 255F, false, false, false, 240);

        public ChloricBE(BlockPos pos, BlockState state) {
            super(CHLORIC_BE.get(), pos, state);
        }

        static void tick(Level level, BlockPos pos) {
            if (level.isClientSide) {
                ClientEffects.spawnRocketFlame(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.5F, 0, 4, 0,
                        100 + level.random.nextInt(20), VAPOR);
            } else {
                zap(level, pos, ModDamageTypes.ACID, 0.1F, SoundEvents.FIRE_EXTINGUISH);
            }
        }
    }

    public static class Electric extends GeyserBlock {
        public Electric(BlockBehaviour.Properties props) {
            super(props);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return simpleCodec(Electric::new);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ElectricBE(pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return type == ELECTRIC_BE.get() ? (l, p, s, be) -> ((ElectricBE) be).tick() : null;
        }
    }

    /** TileEntityLightningVolcano: counts down, then strikes and draws its spike until the flash fades. */
    public static class ElectricBE extends BlockEntity {
        public int chargetime;
        public float flashd;

        public ElectricBE(BlockPos pos, BlockState state) {
            super(ELECTRIC_BE.get(), pos, state);
        }

        void tick() {
            if (chargetime > 0) {
                flashd = 0;
            } else {
                flashd += 0.3F;
                flashd = Math.min(100F, flashd + 0.3F * (100F - flashd) * 0.15F);
            }
            // the client counts down on its own and hears from the server whenever the charge restarts
            if (!(level instanceof ServerLevel server)) {
                if (chargetime > 0) chargetime--;
                return;
            }
            BlockPos pos = worldPosition;
            if (chargetime == 1) {
                double x = pos.getX() + 0.5, y = pos.getY() + 20.5, z = pos.getZ() + 0.5;
                PacketWire.sendToAllAround(new PlasmaBlastPayload(x, y, z, 1F, 1F, 1F, 0F, 90F, 40F), new TargetPoint(server, x, y, z, 256));
                server.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 200F, 0.8F + server.random.nextFloat() * 0.2F);
                zap(server, pos, ModDamageTypes.ELECTRICITY, 1F, ModSounds.TESLA_ZAP.get());
            }
            if (chargetime > 0) chargetime--;
            if (flashd >= 100) {
                chargetime = server.random.nextInt(400) + 100;
                setChanged();
                server.sendBlockUpdated(pos, getBlockState(), getBlockState(), 2);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("charge", chargetime);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            chargetime = tag.getInt("charge");
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            CompoundTag tag = super.getUpdateTag(registries);
            tag.putInt("charge", chargetime);
            return tag;
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
