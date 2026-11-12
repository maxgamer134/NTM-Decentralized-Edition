// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.space.SpaceSiege;
import com.hbm.space.trait.CBT_Invasion;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The invasion's client side: the warning ping and chat line five ticks before it starts, and the
 * meteors streaking down while it lasts (WorldProviderCelestial.Meteor), which {@link SpaceSky}
 * draws.
 */
public final class SpaceSiegeClient {

    /** The meteors in flight around the player, client-side only as in NTM:Space. */
    static final List<Meteor> METEORS = new ArrayList<>();

    private SpaceSiegeClient() {}

    public static void register() {
        CBT_Invasion.clientTick = (invasion, body) -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null) return;
            if (!invasion.isInvading && !invasion.warningPlayed) {
                invasion.warningPlayed = true;
                mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SpaceSiege.ALARM_PING.get(),
                        SoundSource.PLAYERS, 10F, 1F, false);
                player.displayClientMessage(Component.literal("Incoming Invasion!").withStyle(ChatFormatting.RED), false);
            }
            if (invasion.isInvading && invasion.rand.nextInt(Math.max(1, 5 - invasion.wave)) == 0) {
                Meteor.addMeteor();
            }
        };
        NeoForge.EVENT_BUS.addListener(SpaceSiegeClient::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            METEORS.clear();
            return;
        }
        if (mc.isPaused()) return;
        RandomSource rand = mc.level.random;
        ListIterator<Meteor> iterator = METEORS.listIterator();
        while (iterator.hasNext()) {
            Meteor meteor = iterator.next();
            Meteor fragment = meteor.update(rand);
            if (meteor.isDead) iterator.remove();
            if (fragment != null) iterator.add(fragment);
        }
    }

    /** WorldProviderCelestial.MeteorType. */
    enum MeteorType {
        STANDARD,
        FRAGMENT,
        SMOKE
    }

    /** WorldProviderCelestial.Meteor: a glowing streak falling from 2017 blocks up, leaving smoke behind. */
    static final class Meteor {
        double posX, posY, posZ;
        double prevPosX, prevPosY, prevPosZ;
        final double motionX, motionY, motionZ;
        boolean isDead;
        long age;
        final MeteorType type;

        Meteor(double posX, double posY, double posZ) {
            this(posX, posY, posZ, MeteorType.STANDARD, -31.2, -20.8, 20);
        }

        Meteor(double posX, double posY, double posZ, MeteorType type, double motionX, double motionY, double motionZ) {
            this.posX = prevPosX = posX;
            this.posY = prevPosY = posY;
            this.posZ = prevPosZ = posZ;
            this.type = type;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
        }

        static void addMeteor() {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;
            RandomSource rand = player.level().random;
            METEORS.add(new Meteor(player.getX() + rand.nextInt(16000) - 8000, 2017, player.getZ() + rand.nextInt(16000) - 8000));
        }

        private Meteor update(RandomSource rand) {
            if (posY <= 500 && type != MeteorType.SMOKE) isDead = true;
            if (type == MeteorType.SMOKE) {
                age++;
                if (age >= 60) isDead = true;
            }
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            if (type != MeteorType.SMOKE && type != MeteorType.FRAGMENT) {
                return new Meteor(posX + rand.nextInt(16) - 8, posY + rand.nextInt(16), posZ + rand.nextInt(16) - 8,
                        MeteorType.SMOKE, 0, 0, 0);
            }
            return null;
        }
    }
}
