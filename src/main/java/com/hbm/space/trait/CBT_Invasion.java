// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystemSavedData;
import com.hbm.space.SpaceInvaders;
import com.hbm.space.SpaceInvaders.SiegeTier;
import com.hbm.space.SpaceSiege;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * An alien invasion of the body, started by killing the Radioactive Angel there. After a short countdown it runs
 * three waves of glyphid drop pods, mini UFOs and siege craft, each ended by enough kills, then the big UFO; killing
 * that ends the invasion. The wave's kills show as a boss bar ({@link SpaceSiege}).
 */
public class CBT_Invasion extends CelestialBodyTrait {

    // while i could polymorphize to the heavens, this event is more-or-less "scripted"
    // in the sense that you would be fighting the ufo types we HAVE sequentially to the boss
    // oh dont worry you still get to have fun killing! :)

    /** The client side of {@link #update}: the warning ping and chat line and the meteors; set by the client. */
    public static BiConsumer<CBT_Invasion, CelestialBody> clientTick = (invasion, body) -> {};

    public int wave;
    public int kills;
    public int killreq;
    public double waveTime;
    public boolean isInvading;
    public int lastSpawns; // prevent over-lagging the server
    public int spawndelay;
    public int podBurstCounter = 0;
    public int podCooldown = 0;
    public boolean bossSpawned = false;
    public boolean warningPlayed;

    public final Random rand = new Random();

    public CBT_Invasion() {}

    public CBT_Invasion(int wave, double waveTime, boolean isInvading) {
        this.wave = wave;
        this.waveTime = waveTime;
        this.isInvading = isInvading;
        // Space set this on the first server tick, which the client could miss and ping at once instead of at five ticks
        this.warningPlayed = !isInvading && waveTime > 5;
    }

    /** Counts down to the invasion; true when the client has to hear about it (the warning, the start). */
    public boolean prepare() {
        boolean sync = false;
        if (!isInvading && waveTime >= 0) {
            waveTime--;
            boolean wasPlayed = warningPlayed;
            warningPlayed = true;
            if (waveTime <= 5) warningPlayed = false;
            if (wasPlayed && !warningPlayed) sync = true;
            if (waveTime <= 0) {
                isInvading = true;
                sync = true;
            }
        }
        return sync;
    }

    @Override
    public void update(boolean isRemote, CelestialBody body) {
        if (isRemote) {
            clientTick.accept(this, body);
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        boolean counting = !isInvading && waveTime >= 0;
        boolean sync = prepare();

        if (isInvading && body.dimension != null) {
            ServerLevel level = server.getLevel(body.dimension);
            if (level != null && !level.players().isEmpty()) {
                if (logicTick(level)) sync = true;
                handleBurstSpawning(level);
                spawnAttempt(level);
                counting = true;
            }
        }

        // NTM:Space resent every trait each tick; here the client hears only of the changes it shows
        if (sync) SolarSystemSavedData.get().changed();
        else if (counting) SolarSystemSavedData.get().setDirty();
    }

    private static boolean tooDeep(ServerLevel level, Player player) {
        return player.getY() < 50 && !level.isFlat();
    }

    public void spawnCattle(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        ServerPlayer player = players.get(level.random.nextInt(players.size()));
        if (tooDeep(level, player)) return;

        var rand = level.random;
        SpaceInvaders.CombatDropPod pod = SpaceInvaders.COMBAT_POD.get().create(level);
        if (pod == null) return;
        pod.setPos(player.getX() + rand.nextGaussian() * 15, 250, player.getZ() + rand.nextGaussian() * 15);

        EntityType<?> glyph;
        int amount = 1;
        if (wave == 1) {
            amount = 2;
            glyph = ModEntities.GLYPHID.get();
        } else if (wave == 2) {
            int roll = rand.nextInt(3);
            amount = Math.max(1, rand.nextInt(4));
            glyph = switch (roll) {
                case 0 -> ModEntities.GLYPHID.get();
                case 1 -> ModEntities.GLYPHID_BRAWLER.get();
                default -> ModEntities.GLYPHID_DIGGER.get();
            };
        } else if (wave >= 3) {
            int roll = rand.nextInt(5);
            amount = Math.max(1, rand.nextInt(6));
            switch (roll) {
                case 0 -> glyph = ModEntities.GLYPHID.get();
                case 1 -> glyph = ModEntities.GLYPHID_BRAWLER.get();
                case 2 -> glyph = ModEntities.GLYPHID_DIGGER.get();
                case 3 -> glyph = ModEntities.GLYPHID_BLASTER.get();
                default -> {
                    glyph = ModEntities.GLYPHID_BEHEMOTH.get();
                    amount = 1;
                }
            }
        } else {
            glyph = ModEntities.GLYPHID.get();
        }

        Entity cargo = glyph.create(level);
        if (cargo == null) return;
        pod.setEntity(cargo, amount, 2);
        level.addFreshEntity(pod);
    }

    private void handleBurstSpawning(ServerLevel level) {
        if (wave > 3) return;

        if (podCooldown > 0) {
            podCooldown--;
            return;
        }

        if (level.getGameTime() % 10 + level.random.nextInt(3) == 0) {
            spawnCattle(level);
            podBurstCounter++;
            if (podBurstCounter >= 3 + (wave - 1)) {
                podBurstCounter = 0;
                podCooldown = 500;
            }
        }
    }

    /** True when the wave changed. */
    private boolean logicTick(ServerLevel level) {
        if (!isInvading) return false;

        switch (wave) {
            case 0:
                advanceWave(level);
                return true;
            case 1:
                killreq = 20;
                break;
            case 2:
                killreq = 100;
                break;
            case 3:
                killreq = 150;
                break;
            case 4:
                killreq = 1;
                if (!bossSpawned) {
                    spawnBoss(level);
                    bossSpawned = true;
                }
                return false;
            default:
                return false;
        }
        if (kills >= killreq) {
            advanceWave(level);
            return true;
        }
        return false;
    }

    private void advanceWave(ServerLevel level) {
        wave++;
        kills = 0;
        broadcast(level, "Wave " + (wave == 4 ? "FINAL" : wave) + " is starting!", ChatFormatting.GOLD);
    }

    public void spawnAttempt(ServerLevel level) {
        if (wave > 3) return;
        int timer = 200;
        if (wave == 2) timer = 100;
        if (wave == 3) timer = 80;

        if (level.getGameTime() % timer != 0) return;

        List<ServerPlayer> players = level.players();
        ServerPlayer player = players.get(level.random.nextInt(players.size()));
        if (tooDeep(level, player)) return;

        var rand = level.random;
        double spawnX = player.getX() + rand.nextGaussian() * 30;
        double spawnZ = player.getZ() + rand.nextGaussian() * 30;
        double spawnY = player.getY() + 30 + rand.nextInt(20);

        float bigUfoChance = 0.0F;
        if (wave == 2) bigUfoChance = 0.1F;
        else if (wave == 3) bigUfoChance = 0.2F;

        float waveFactor = wave * 0.05F;
        if (waveFactor > 0.2F) waveFactor = 0.2F;

        if (rand.nextFloat() < bigUfoChance) {
            SpaceSiege.SiegeCraft bigUfo = SpaceSiege.SIEGE_CRAFT.get().create(level);
            if (bigUfo == null) return;
            bigUfo.moveTo(spawnX, spawnY, spawnZ, rand.nextFloat() * 360.0F, 0.0F);

            float bigRoll = rand.nextFloat();
            SiegeTier chosen = (bigRoll < 0.5F - waveFactor) ? SiegeTier.STONE : SiegeTier.IRON;

            bigUfo.setTier(chosen);
            level.addFreshEntity(bigUfo);
        } else {
            SpaceInvaders.SiegeUFO smallUfo = SpaceInvaders.SIEGE_UFO.get().create(level);
            if (smallUfo == null) return;
            smallUfo.moveTo(spawnX, spawnY, spawnZ, rand.nextFloat() * 360.0F, 0.0F);

            float smallRoll = rand.nextFloat();
            SiegeTier chosen;

            if (smallRoll < 0.15F - (waveFactor * 1.5F)) chosen = SiegeTier.CLAY;
            else if (smallRoll < 0.35F - (waveFactor * 1.2F)) chosen = SiegeTier.STONE;
            else if (smallRoll < 0.50F - waveFactor) chosen = SiegeTier.IRON;
            else if (smallRoll < 0.65F - (waveFactor * 0.8F)) chosen = SiegeTier.SILVER;
            else if (smallRoll < 0.80F - (waveFactor * 0.5F)) chosen = SiegeTier.GOLD;
            else if (smallRoll < 0.90F - (waveFactor * 0.2F)) chosen = SiegeTier.DESH;
            else if (smallRoll < 0.97F) chosen = SiegeTier.SCHRAB;
            else chosen = SiegeTier.DNT;

            smallUfo.setTier(chosen);
            level.addFreshEntity(smallUfo);
        }

        lastSpawns++;
    }

    /** Called when anything dies on the body: siege mobs count towards the wave, the big UFO ends the invasion. */
    public void onKill(LivingEntity entity, CelestialBody body) {
        if (entity instanceof SpaceInvaders.SiegeUFO) {
            // GOLD and above are worth 2 kills
            int value = entity.getMaxHealth() >= 100 * 0.25 ? 2 : 1;
            kills += value;
            body.modifyTraits(this);
        } else if (entity instanceof SpaceSiege.SiegeCraft) {
            kills += 10;
            body.modifyTraits(this);
        }

        if (wave >= 4 && entity instanceof EntityUFO) {
            Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = body.getTraits();
            currentTraits.remove(CBT_Invasion.class);

            for (Player player : entity.level().players())
                player.sendSystemMessage(Component.literal("The Invasion Is Over!").withStyle(ChatFormatting.YELLOW));

            body.setTraits(currentTraits);
        }
    }

    private void spawnBoss(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        ServerPlayer player = players.get(level.random.nextInt(players.size()));
        EntityUFO entity = ModEntities.UFO.get().create(level);
        if (entity == null) return;
        entity.scanCooldown = 100;
        entity.moveTo(player.getX(), player.getY() + 50, player.getZ(), Mth.wrapDegrees(level.random.nextFloat() * 360.0F), 0.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yBodyRot = entity.getYRot();
        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.EVENT, null);
        level.addFreshEntity(entity);
    }

    private void broadcast(ServerLevel level, String text, ChatFormatting color) {
        for (ServerPlayer p : level.players())
            p.sendSystemMessage(Component.literal(text).withStyle(color, ChatFormatting.BOLD));
    }

    /** The boss bar's fill: the kills still needed this wave. */
    public float getMaxHealth() {
        return killreq;
    }

    public float getHealth() {
        return killreq - kills;
    }

    public Component getBarName() {
        return Component.literal("Wave " + wave);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putInt("wave", wave);
        nbt.putInt("kills", kills);
        nbt.putInt("killreq", killreq);
        nbt.putDouble("waveTime", waveTime);
        nbt.putBoolean("isInvading", isInvading);
        nbt.putBoolean("warningPlayed", warningPlayed);
        nbt.putInt("podBurst", podBurstCounter);
        nbt.putInt("podCooldown", podCooldown);
        nbt.putBoolean("bossSpawned", bossSpawned);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        wave = nbt.getInt("wave");
        kills = nbt.getInt("kills");
        killreq = nbt.getInt("killreq");
        waveTime = nbt.getDouble("waveTime");
        isInvading = nbt.getBoolean("isInvading");
        warningPlayed = nbt.getBoolean("warningPlayed");
        podBurstCounter = nbt.getInt("podBurst");
        podCooldown = nbt.getInt("podCooldown");
        bossSpawned = nbt.getBoolean("bossSpawned");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeInt(wave);
        buf.writeInt(kills);
        buf.writeInt(killreq);
        buf.writeDouble(waveTime);
        buf.writeBoolean(isInvading);
        buf.writeBoolean(warningPlayed);
        buf.writeInt(podBurstCounter);
        buf.writeInt(podCooldown);
        buf.writeBoolean(bossSpawned);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        wave = buf.readInt();
        kills = buf.readInt();
        killreq = buf.readInt();
        waveTime = buf.readDouble();
        isInvading = buf.readBoolean();
        warningPlayed = buf.readBoolean();
        podBurstCounter = buf.readInt();
        podCooldown = buf.readInt();
        bossSpawned = buf.readBoolean();
    }
}
