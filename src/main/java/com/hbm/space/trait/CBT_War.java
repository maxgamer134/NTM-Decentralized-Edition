// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.trait;

import com.hbm.space.CelestialBody;
import com.hbm.space.SolarSystemSavedData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * A body at war: its health and shield, and the projectiles flying at it. Everything it does is
 * drawn by the sky (the projectiles' shockwaves and the flash they light the air with); the
 * server counts the projectiles down, damages the body they were aimed at when they land and
 * destroys it when its health runs out. NTM:Space resent the projectile timers every tick; here
 * the client counts them down itself between syncs, which only carry new or landed projectiles.
 */
public class CBT_War extends CelestialBodyTrait {

    /** The client side of {@link #update} (the impact sound); set by the client. */
    public static BiConsumer<CBT_War, CelestialBody> clientTick = (war, body) -> {};

    public int health;
    public int shield;
    public List<Projectile> projectiles;

    public CBT_War() {
        this(100, 0);
    }

    public CBT_War(int health, int shield) {
        this.health = health;
        this.shield = shield;
        this.projectiles = new ArrayList<>();
    }

    public void launchProjectile(Projectile proj) {
        projectiles.add(proj);
    }

    public void launchProjectile(float traveltime, int size, int damage, double x, double y, double z, ProjectileType type, String target) {
        projectiles.add(new Projectile(traveltime, size, damage, x, y, z, type, target));
    }

    /** A split shot bursting into {@code amount} smaller ones once it arrives. */
    public void split(int amount, Projectile projectile, ProjectileType type) {
        if (projectile.getTravel() <= 0) {
            Random rand = new Random();
            for (int j = 0; j < amount; j++) {
                float randX = rand.nextFloat() * 160 - 80;
                float randY = rand.nextFloat() * 90 - 80;
                launchProjectile(Math.abs(20 + j * 10), projectile.getSize(), projectile.getDamage(), projectile.getTranslateX(),
                        projectile.getTranslateY() - randY * j, projectile.getTranslateZ() + randX * j, type, projectile.getTarget());
            }
            destroyProjectile(projectile);
        }
    }

    @Override
    public void update(boolean isRemote, CelestialBody body) {
        if (isRemote) {
            // the timers are deterministic, so the client runs them between syncs
            for (int i = 0; i < projectiles.size(); i++) {
                Projectile projectile = projectiles.get(i);
                projectile.update();
                if (projectile.getTravel() <= 0) projectile.impact();
                if (projectile.getAnimtime() >= 100) projectiles.remove(i--);
            }
            clientTick.accept(this, body);
            return;
        }
        boolean sync = false;
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile projectile = projectiles.get(i);
            projectile.update();
            if (projectile.getTravel() <= 0) projectile.impact();

            if (projectile.getAnimtime() >= 100) {
                destroyProjectile(projectile);
                i--;
                sync = true;
                CelestialBody target = CelestialBody.getBodyOrNull(projectile.getTarget());
                if (target != null) {
                    if (health > 0) damage(projectile.getDamage(), target, body);
                    else {
                        target.modifyTraits(new CBT_Destroyed());
                        health = 0;
                    }
                }
                continue;
            }

            if (projectile.getType() == ProjectileType.SPLITSHOT && projectile.getTravel() <= 0) {
                split(4, projectile, ProjectileType.SMALL);
                i--;
                sync = true;
            }
        }
        if (sync) SolarSystemSavedData.get().changed();
        else if (!projectiles.isEmpty()) SolarSystemSavedData.get().setDirty();
    }

    /** CelestialBody.damage: the shield takes the hit first, then the body's health. */
    private void damage(int dmg, CelestialBody target, CelestialBody self) {
        CBT_War war;
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = null;
        if (target == self) {
            war = this;
        } else {
            traits = target.getTraits();
            war = (CBT_War) traits.get(CBT_War.class);
            if (war == null) {
                war = new CBT_War();
                traits.put(CBT_War.class, war);
            }
        }
        if (war.shield > 0) war.shield -= dmg;
        else war.health -= dmg;
        if (traits != null) target.setTraits(traits);
    }

    public void destroyProjectile(Projectile proj) {
        projectiles.remove(proj);
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putInt("health", health);
        nbt.putInt("shield", shield);
        CompoundTag projectilesTag = new CompoundTag();
        for (int i = 0; i < projectiles.size(); i++) {
            CompoundTag projTag = new CompoundTag();
            projectiles.get(i).writeToNBT(projTag);
            projectilesTag.put("projectile" + i, projTag);
        }
        nbt.put("projectiles", projectilesTag);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        shield = nbt.getInt("shield");
        health = nbt.getInt("health");
        CompoundTag projectilesTag = nbt.getCompound("projectiles");
        projectiles = new ArrayList<>();
        for (int i = 0; projectilesTag.contains("projectile" + i); i++) {
            Projectile projectile = new Projectile();
            projectile.readFromNBT(projectilesTag.getCompound("projectile" + i));
            projectiles.add(projectile);
        }
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeInt(health);
        buf.writeInt(shield);
        buf.writeInt(projectiles.size());
        for (Projectile projectile : projectiles) projectile.writeToBytes(buf);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        // NTM:Space read the shield first and so swapped health and shield on every client
        health = buf.readInt();
        shield = buf.readInt();
        int count = buf.readInt();
        projectiles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Projectile projectile = new Projectile();
            projectile.readFromBytes(buf);
            projectiles.add(projectile);
        }
    }

    public static class Projectile {

        private float traveltime;
        private int size;
        private int damage;
        private int animtime;
        private float flashtime;
        private double translateX;
        private double translateY;
        private double translateZ;
        private ProjectileType type;
        public int GUIangle;
        /** The body it was fired at (NTM:Space kept its dimension id). */
        private String target = "";

        public Projectile() {
            this.type = ProjectileType.MEDIUM;
        }

        public Projectile(float traveltime, int size, int damage, double posX, double posY, double posZ, ProjectileType type, @Nullable String target) {
            this.traveltime = traveltime;
            this.size = size;
            this.damage = damage;
            this.translateX = posX;
            this.translateY = posY;
            this.translateZ = posZ;
            this.type = type;
            this.target = target == null ? "" : target;
        }

        public void writeToNBT(CompoundTag nbt) {
            nbt.putInt("damage", damage);
            nbt.putFloat("traveltime", traveltime);
            nbt.putInt("size", size);
            nbt.putDouble("translateX", translateX);
            nbt.putDouble("translateY", translateY);
            nbt.putDouble("translateZ", translateZ);
            nbt.putInt("animtime", animtime);
            nbt.putString("type", type.name());
            nbt.putString("target", target);
        }

        public void readFromNBT(CompoundTag nbt) {
            damage = nbt.getInt("damage");
            traveltime = nbt.getFloat("traveltime");
            size = nbt.getInt("size");
            translateX = nbt.getDouble("translateX");
            translateY = nbt.getDouble("translateY");
            translateZ = nbt.getDouble("translateZ");
            animtime = nbt.getInt("animtime");
            try {
                type = ProjectileType.valueOf(nbt.getString("type"));
            } catch (IllegalArgumentException e) {
                type = ProjectileType.MEDIUM;
            }
            target = nbt.getString("target");
        }

        public void writeToBytes(FriendlyByteBuf buf) {
            buf.writeInt(damage);
            buf.writeFloat(traveltime);
            buf.writeInt(size);
            buf.writeDouble(translateX);
            buf.writeDouble(translateY);
            buf.writeDouble(translateZ);
            buf.writeInt(animtime);
            buf.writeFloat(flashtime);
            buf.writeByte(type.ordinal());
            buf.writeInt(GUIangle);
            buf.writeUtf(target);
        }

        public void readFromBytes(FriendlyByteBuf buf) {
            damage = buf.readInt();
            traveltime = buf.readFloat();
            size = buf.readInt();
            translateX = buf.readDouble();
            translateY = buf.readDouble();
            translateZ = buf.readDouble();
            animtime = buf.readInt();
            flashtime = buf.readFloat();
            type = ProjectileType.values()[Mth.clamp(buf.readByte(), 0, ProjectileType.values().length - 1)];
            GUIangle = buf.readInt();
            target = buf.readUtf();
        }

        public ProjectileType getType() {
            return type;
        }

        public void setType(ProjectileType type) {
            this.type = type;
        }

        public float getFlashtime() {
            return flashtime;
        }

        public void setFlashtime(float flashtime) {
            this.flashtime = flashtime;
        }

        public void update() {
            if (traveltime > 0) {
                traveltime--;
            } else {
                traveltime = 0;
                if (type != ProjectileType.SPLITSHOT) animtime = Math.min(100, animtime + 1);
            }
        }

        public void impact() {
            flashtime += 0.6F;
            flashtime = Math.min(100F, flashtime + 0.1F * (100F - flashtime) * 0.15F);
            if (flashtime >= 100) flashtime = 100;
            if (animtime == 0) flashtime = 0;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getDamage() {
            return damage;
        }

        public void setDamage(int damage) {
            this.damage = damage;
        }

        public float getTravel() {
            return traveltime;
        }

        public void setTravel(float travel) {
            this.traveltime = travel;
        }

        public int getAnimtime() {
            return animtime;
        }

        public void setAnimtime(int animtime) {
            this.animtime = animtime;
        }

        public double getTranslateX() {
            return translateX;
        }

        public double getTranslateY() {
            return translateY;
        }

        public double getTranslateZ() {
            return translateZ;
        }

        public String getTarget() {
            return target;
        }
    }

    public enum ProjectileType {
        SMALL,
        MEDIUM,
        HUGE,
        INCENDIARY,
        NUCLEAR,
        SPLITSHOT
    }
}
