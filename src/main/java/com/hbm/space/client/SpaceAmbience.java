// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.space.CelestialBody;
import com.hbm.space.SpaceExtras;
import com.hbm.space.atmosphere.SpaceEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * NTM:Space's soundscape: its own music off Earth, and the hum of a station's machinery for anyone
 * standing in a station's artificial gravity.
 */
public final class SpaceAmbience {

    /** NTM:Space's ship hum volume (its client config default). */
    private static final float HUM_VOLUME = 0.1F;

    private static @Nullable Music music;
    private static @Nullable AudioWrapper hum;

    private SpaceAmbience() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((SelectMusicEvent event) -> {
            Level level = Minecraft.getInstance().level;
            Music original = event.getOriginalMusic();
            if (level == null || (original != Musics.GAME && original != Musics.CREATIVE)) return;
            if (level.dimension() == Level.OVERWORLD) return;
            if (!CelestialBody.inOrbit(level) && CelestialBody.getBodyOrNull(level.dimension()) == null) return;
            if (music == null) {
                Holder<net.minecraft.sounds.SoundEvent> sound =
                        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SpaceExtras.MUSIC_SPACE.get());
                music = new Music(sound, original.getMinDelay(), original.getMaxDelay(), false);
            }
            event.setMusic(music);
        });

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            LocalPlayer player = Minecraft.getInstance().player;
            boolean inStation = player != null && CelestialBody.inOrbit(player.level())
                    && SpaceEnvironment.gravityOf(player) > 0;
            if (inStation) {
                if (hum == null || !hum.isPlaying()) {
                    hum = AudioSystem.getLoopedSound(SpaceExtras.STATION_HUM.get(), SoundSource.AMBIENT,
                            (float) player.getX(), (float) player.getY(), (float) player.getZ(), HUM_VOLUME, 5F, 1F, 10);
                    if (hum != null) {
                        hum.attachTo(player);
                        hum.startSound();
                    }
                }
                if (hum != null) hum.keepAlive();
            } else if (hum != null) {
                hum.stopSound();
                hum = null;
            }
        });
    }
}
