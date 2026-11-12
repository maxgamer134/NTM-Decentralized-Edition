// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import com.hbm.space.CelestialBody;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

/**
 * A star's Dyson swarms, each under the id its launchers and receivers share. Swarm members fail
 * at a rate that grows with the star's whole swarm, so launchers have to keep launching.
 */
public class CBT_Dyson extends CelestialBodyTrait {

    private Map<Integer, Swarm> swarms = new HashMap<>();

    private static final class Swarm {
        int members;
        int consumers;
        // counted up as receivers ask during a tick, copied to consumers at the next attenuation
        int addedConsumers;

        Swarm(int members) {
            this.members = members;
        }
    }

    public static void launch(Level level, int id) {
        launch(level, id, 1);
    }

    public static void launch(Level level, int id, int amount) {
        CelestialBody star = CelestialBody.getStar(level);
        CBT_Dyson current = star.getTrait(CBT_Dyson.class);
        CBT_Dyson dyson = current != null ? (CBT_Dyson) current.copy() : new CBT_Dyson();
        dyson.swarms.computeIfAbsent(id, k -> new Swarm(0)).members += amount;
        star.modifyTraits(dyson);
    }

    public static int count(Level level, int id) {
        CBT_Dyson dyson = CelestialBody.getStar(level).getTrait(CBT_Dyson.class);
        if (dyson == null) return 0;
        Swarm swarm = dyson.swarms.get(id);
        return swarm == null ? 0 : swarm.members;
    }

    /** Registers one more receiver on the swarm and returns how many shared it last tick. */
    public static int consumers(Level level, int id) {
        CBT_Dyson dyson = CelestialBody.getStar(level).getTrait(CBT_Dyson.class);
        if (dyson == null) return 0;
        Swarm swarm = dyson.swarms.get(id);
        if (swarm == null) return 0;
        swarm.addedConsumers++;
        return swarm.consumers;
    }

    public int size() {
        int size = 0;
        for (Swarm swarm : swarms.values()) size += swarm.members;
        return size;
    }

    /** Once a tick: members fail with a chance that grows with the star's whole swarm. */
    public void attenuate(net.minecraft.util.RandomSource random) {
        int size = size();
        for (Swarm swarm : swarms.values()) {
            swarm.consumers = swarm.addedConsumers;
            swarm.addedConsumers = 0;
            if (swarm.members <= 0) continue;
            double decayChance = (double) size / (1024 * 5 * 20);
            if (random.nextDouble() < decayChance) swarm.members--;
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        int[] data = new int[swarms.size() * 2];
        int i = 0;
        for (Map.Entry<Integer, Swarm> entry : swarms.entrySet()) {
            data[i++] = entry.getKey();
            data[i++] = entry.getValue().members;
        }
        nbt.putIntArray("swarm", data);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        int[] data = nbt.getIntArray("swarm");
        swarms = new HashMap<>();
        for (int i = 0; i + 1 < data.length; i += 2) swarms.put(data[i], new Swarm(data[i + 1]));
    }

    // NTM:Space sent the swarm id as a short, so ids past 32767 arrived wrong on clients
    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(swarms.size());
        for (Map.Entry<Integer, Swarm> entry : swarms.entrySet()) {
            buf.writeVarInt(entry.getKey());
            buf.writeVarInt(entry.getValue().members);
        }
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        swarms = new HashMap<>();
        for (int i = 0; i < count; i++) swarms.put(buf.readVarInt(), new Swarm(buf.readVarInt()));
    }
}
