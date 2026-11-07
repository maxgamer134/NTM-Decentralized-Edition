// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.render.anim.BusAnimation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's pre-rework gun config (handler/GunConfiguration), field
 * for field.
 */
public class GunConfiguration implements Cloneable {

    public static final int MODE_NORMAL = 0;
    public static final int MODE_RELEASE = 1;

    public static final int FIRE_MANUAL = 0;
    public static final int FIRE_AUTO = 1;
    public static final int FIRE_BURST = 2;

    public static final int RELOAD_NONE = 0;
    public static final int RELOAD_FULL = 1;
    public static final int RELOAD_SINGLE = 2;

    public static final String RSOUND_REVOLVER = "hbm:weapon.revolverReload";
    public static final String RSOUND_MAG = "hbm:weapon.magReload";
    public static final String RSOUND_MAG_BOLT = "hbm:weapon.magReloadBolt";
    public static final String RSOUND_SHOTGUN = "hbm:weapon.shotgunReload";
    public static final String RSOUND_LAUNCHER = "hbm:weapon.rpgReload";
    public static final String RSOUND_GRENADE = "hbm:weapon.hkReload";
    public static final String RSOUND_GRENADE_NEW = "hbm:weapon.glReload";
    public static final String RSOUND_FATMAN = "hbm:weapon.fatmanReload";

    /** amount of ticks between each bullet */
    public int rateOfFire;
    /** amount of bullets fired per delay passed */
    public int roundsPerCycle;
    /** amount of bullets fired per burst */
    public int roundsPerBurst;
    /** 0 = normal, 1 = release, 2 = charge */
    public int gunMode;
    /** 0 = manual, 1 = automatic, 2 = burst */
    public int firingMode;
    /** weapon won't fire after weapon breaks */
    public int durability;

    /** animations, filled on the client by {@link #loadAnimations} the first time one is asked for */
    public final EnumMap<AnimType, BusAnimation> animations = new EnumMap<>(AnimType.class);
    public Runnable loadAnimations;
    public boolean animationsLoaded = false;

    /** whether ot not to disable crosshair when sneaking */
    public boolean hasSights;
    /** does the weapon fire from the centre of the screen */
    public boolean isCentered;
    /** texture overlay when sneaking */
    public ResourceLocation scopeTexture;
    /** whether the FOV multiplier should be absolute or multiplicative to other modifiers */
    public boolean absoluteFOV = true;
    /** the multiplier for the FOV, 0 means no change */
    public float zoomFOV = 0.0F;

    /** duration of every animation cycle, used also for how quickly a burst fire rifle can fire */
    public int firingDuration;
    public String firingSound = "";
    public String firingSoundEmpty = null;
    public float firingVolume = 1.0F;
    public float firingPitch = 1.0F;

    /** how long the reload animation will play */
    public int reloadDuration;
    public int emptyReloadAdditionalDuration;
    public String reloadSound = "";
    public String reloadSoundEmpty = null;
    /** whether the reload sound should be played at the beginning or at the end of the reload */
    public boolean reloadSoundEnd = true;
    public String equipSound = "";

    /** how much ammo the clip can hold, 0 if drawn from inventory */
    public int ammoCap;
    /** 0 does not allow direct reload, 1 is full clip, 2 is single bullet */
    public int reloadType;
    /** whether or not the infinity enchantment should work */
    public boolean allowsInfinity;
    /** whether the reload animations play in sequence (single-bullet loading) */
    public boolean reloadAnimationsSequential = false;
    /** whether the ammo count should be displayed */
    public boolean showAmmo = true;

    /** for electrically powered weapons */
    public long maxCharge;
    public long chargeRate;
    public long dischargePerShot;

    public String name = "";
    public EnumGunManufacturer manufacturer = EnumGunManufacturer.NONE;
    public List<String> comment = new ArrayList<>();

    /** bullet configs for main and alternative fire */
    public List<Integer> config = new ArrayList<>();

    /** crosshair */
    public Crosshair crosshair;

    /** casing eject behavior */
    public Ejector ejector = null;

    public GunConfiguration silenced() {
        this.firingSound = "hbm:weapon.silencerShoot";
        return this;
    }

    /** main's CasingEjector: where casings leave the gun, how fast, how many and when. */
    public static final class Ejector {
        public double offsetX, offsetY, offsetZ;
        public double motionX, motionY, motionZ;
        public float yawFactor, pitchFactor;
        public boolean afterReload;
        public int amount = 1;
        public int delay;

        public Ejector setOffset(double x, double y, double z) {
            offsetX = x;
            offsetY = y;
            offsetZ = z;
            return this;
        }

        public Ejector setMotion(double x, double y, double z) {
            motionX = x;
            motionY = y;
            motionZ = z;
            return this;
        }

        public Ejector setAngleRange(float yaw, float pitch) {
            yawFactor = yaw;
            pitchFactor = pitch;
            return this;
        }

        public Ejector setAfterReload() {
            afterReload = true;
            return this;
        }

        public Ejector setAmount(int amount) {
            this.amount = amount;
            return this;
        }

        public Ejector setDelay(int delay) {
            this.delay = delay;
            return this;
        }

        /** main's CasingEjector.clone() */
        public Ejector copy() {
            Ejector e = new Ejector().setOffset(offsetX, offsetY, offsetZ).setMotion(motionX, motionY, motionZ);
            e.yawFactor = yawFactor;
            e.pitchFactor = pitchFactor;
            e.afterReload = afterReload;
            e.amount = amount;
            e.delay = delay;
            return e;
        }
    }
}
