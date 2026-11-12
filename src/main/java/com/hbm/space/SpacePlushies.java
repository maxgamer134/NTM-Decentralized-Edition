// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space;

import com.hbm.backport.InteractionResult;
import com.hbm.blockentity.BlockEntityPlushie;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockPlushie;
import com.hbm.blocks.generic.BlockPlushie.Plushie;
import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import com.hbm.items.CreativeContents;
import com.hbm.items.block.ItemBlockPlushie;
import com.hbm.lib.Library;
import com.hbm.registration.ItemFamily;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * NTM:Space's vocaloid plushies: Kasane Teto, Hatsune Miku and Akita Neru, plus the fat Teto a
 * Teto plushie turns into when fed butter. They join main's plushies in {@link BlockPlushie#TYPES}
 * in NTM:Space's order (the three after Hundun, the fat Teto last), so they exist only with Space on.
 */
public final class SpacePlushies {

    private SpacePlushies() {}

    private static final ResourceLocation BUTTER = Library.id("butter");

    public static final RegistryHandle<SoundEvent> SOUND_TETO = NeoForgeRegistrar.registerSound("block.teto");
    public static final RegistryHandle<SoundEvent> SOUND_MIKU = NeoForgeRegistrar.registerSound("block.miku");
    public static final RegistryHandle<SoundEvent> SOUND_AKITA = NeoForgeRegistrar.registerSound("block.akita");

    public enum SpacePlushie implements Plushie {
        TETO("Kasane Teto", "please help I've been trapped in her basement for da-", () -> SOUND_TETO.get()),
        MIKU("Hatsune Miku", "In your wifi, and your heart.", () -> SOUND_MIKU.get()),
        NERU("Akita Neru", "Careful, she might electrocute you.", () -> SOUND_AKITA.get()),
        FATO("FAT TETO", "pls don't bully me", () -> SOUND_TETO.get());

        public final String label;
        public final String inscription;
        private final Supplier<SoundEvent> sound;

        SpacePlushie(String label, String inscription, Supplier<SoundEvent> sound) {
            this.label = label;
            this.inscription = inscription;
            this.sound = sound;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public @Nullable String inscription() {
            return inscription;
        }

        @Override
        public ItemStack stack() {
            return ITEMS.stack(this);
        }

        @Override
        public void squeak(Level level, BlockPos pos) {
            level.playSound(null, pos, sound.get(), SoundSource.BLOCKS, 0.25F, this == FATO ? 0.5F : 1F);
        }

        @Override
        public InteractionResult useItemOn(ItemStack held, BlockEntityPlushie plushie, Player player) {
            // a Teto fed butter becomes FAT TETO
            if (this != TETO || held.isEmpty() || !BuiltInRegistries.ITEM.getKey(held.getItem()).equals(BUTTER))
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            Level level = plushie.getLevel();
            if (level == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
            plushie.squish();
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            plushie.type = FATO;
            plushie.setChanged();
            plushie.networkPackNTTracking();
            held.shrink(1);
            BlockPos pos = plushie.getBlockPos();
            level.playSound(null, pos, ModSounds.PLAYER_GROAN.get(), SoundSource.BLOCKS, 1F, 2F);
            return InteractionResult.SUCCESS;
        }
    }

    public static final ItemFamily<SpacePlushie, ItemBlockPlushie> ITEMS = Reg.family(
            SpacePlushie.values(),
            type -> "plushie_" + type.id(),
            (props, type) -> new ItemBlockPlushie(ModBlocks.PLUSHIE.get(), props, type),
            Item.Properties::new);

    static {
        BlockPlushie.TYPES.addAfter(PlushieType.HUNDUN, SpacePlushie.TETO);
        BlockPlushie.TYPES.addAfter(SpacePlushie.TETO, SpacePlushie.MIKU);
        BlockPlushie.TYPES.addAfter(SpacePlushie.MIKU, SpacePlushie.NERU);
        BlockPlushie.TYPES.add(SpacePlushie.FATO);
    }

    public static void init() {
        // NTM:Space lists the vocaloids between Hundun and the dragon; FAT TETO stays out of the tab
        CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, Library.id("plushie_hundun"),
                () -> ITEMS.stack(SpacePlushie.TETO));
        CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, Library.id("plushie_teto"),
                () -> ITEMS.stack(SpacePlushie.MIKU));
        CreativeContents.insertAfter(CreativeContents.Tab.BLOCKS, Library.id("plushie_miku"),
                () -> ITEMS.stack(SpacePlushie.NERU));
    }
}
