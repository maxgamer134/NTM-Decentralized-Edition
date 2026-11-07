// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.hbm.entity.projectile.EntityBulletBaseNT.IBulletHitBehaviorNT;
import com.hbm.entity.projectile.EntityBulletBaseNT.IBulletHurtBehaviorNT;
import com.hbm.entity.projectile.EntityBulletBaseNT.IBulletImpactBehaviorNT;
import com.hbm.entity.projectile.EntityBulletBaseNT.IBulletRicochetBehaviorNT;
import com.hbm.entity.projectile.EntityBulletBaseNT.IBulletUpdateBehaviorNT;
import com.hbm.config.BalanceConfig;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.lib.ModDamageTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import com.hbm.particle.SpentCasing;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's pre-rework bullet config (handler/BulletConfiguration).
 * The ammo is an item handle instead of a ComparableStack, the damage type a registry key.
 */
public class BulletConfiguration implements Cloneable {

    /** what item this specific configuration consumes */
    public Supplier<? extends Item> ammo;
    /** how many ammo units one item restores */
    public int ammoCount = 1;
    /** how fast the bullet is (in sanics per second, or sps) */
    public float velocity;
    /** spread of bullets in gaussian range */
    public float spread;
    /** weapon durability reduced (centered around 10) */
    public int wear;
    /** greatest amount of pellets created each shot */
    public int bulletsMin;
    /** least amount of pellets created each shot */
    public int bulletsMax;

    /** damage bounds */
    public float dmgMin;
    public float dmgMax;
    public float headshotMult = 1.0F;

    /** acceleration torwards neg Y */
    public double gravity;
    /** max age in ticks before despawning */
    public int maxAge;

    /** whether the projectile should be able to bounce off of blocks */
    public boolean doesRicochet;
    /** the maximum angle at which the projectile should bounce */
    public double ricochetAngle;
    /** lower bound ricochet chance (below R angle) */
    public int LBRC;
    /** higher bound ricochet chance (above R angle) */
    public int HBRC;
    /** how much of the initial velocity is kept after bouncing */
    public double bounceMod;

    /** how many ticks until the projectile can hurt the shooter */
    public int selfDamageDelay = 5;

    /** whether or not the bullet should penetrate mobs */
    public boolean doesPenetrate;
    /** disables collisions with blocks entirely */
    public boolean isSpectral;
    /** whether or not the bullet should break glass */
    public boolean doesBreakGlass;
    /** whether the bullet should stay alive after colliding with a block */
    public boolean liveAfterImpact;
    /** creates a "muzzle flash" and a ton of smoke with every projectile spawned */
    public boolean blackPowder = false;

    // bullet effects
    public List<MobEffectInstance> effects;
    public int incendiary;
    public int emp;
    public boolean blockDamage = true;
    public float explosive;
    public double jolt;
    public int rainbow;
    public int nuke;
    public int shrapnel;
    public int chlorine;
    public int leadChance;
    public int caustic;
    public boolean destroysBlocks;
    public boolean instakill;
    public IBulletHurtBehaviorNT bntHurt;
    public IBulletHitBehaviorNT bntHit;
    public IBulletRicochetBehaviorNT bntRicochet;
    public IBulletImpactBehaviorNT bntImpact;
    public IBulletUpdateBehaviorNT bntUpdate;

    // appearance
    public int style;
    // additional appearance data, i.e. particle effects
    public int trail;
    // ricochet sound type
    public int plink;
    // vanilla particle FX
    public String vPFX = "";

    // energy projectiles
    // power consumed per shot
    public int dischargePerShot;
    // unlocalised firing mode name
    public String modeName;
    // firing mode text colour
    public ChatFormatting chatColour = ChatFormatting.WHITE;
    // firing rate
    public int firingRate;

    public ResourceKey<DamageType> damageType = ModDamageTypes.REVOLVER_BULLET;
    public boolean dmgProj = true;
    public boolean dmgFire = false;
    public boolean dmgExplosion = false;
    public boolean dmgBypass = false;

    public static final int STYLE_NONE = -1;
    public static final int STYLE_NORMAL = 0;
    public static final int STYLE_PISTOL = 1;
    public static final int STYLE_FLECHETTE = 2;
    public static final int STYLE_PELLET = 3;
    public static final int STYLE_BOLT = 4;
    public static final int STYLE_FOLLY = 5;
    public static final int STYLE_ROCKET = 6;
    public static final int STYLE_STINGER = 7;
    public static final int STYLE_NUKE = 8;
    public static final int STYLE_MIRV = 9;
    public static final int STYLE_GRENADE = 10;
    public static final int STYLE_BF = 11;
    public static final int STYLE_ORB = 12;
    public static final int STYLE_METEOR = 13;
    public static final int STYLE_APDS = 14;
    public static final int STYLE_BLADE = 15;
    public static final int STYLE_BARREL = 16;
    public static final int STYLE_TAU = 17;
    public static final int STYLE_LEADBURSTER = 18;

    public static final int PLINK_NONE = 0;
    public static final int PLINK_BULLET = 1;
    public static final int PLINK_GRENADE = 2;
    public static final int PLINK_ENERGY = 3;
    public static final int PLINK_SING = 4;

    public static final int BOLT_LACUNAE = 0;
    public static final int BOLT_NIGHTMARE = 1;
    public static final int BOLT_LASER = 2;
    public static final int BOLT_ZOMG = 3;
    public static final int BOLT_WORM = 4;
    public static final int BOLT_GLASS_CYAN = 5;
    public static final int BOLT_GLASS_BLUE = 6;

    public SpentCasing spentCasing;

    public boolean isAmmo(ItemStack stack) {
        if (ammo == null || stack.isEmpty()) return false;
        if (stack.is(ammo.get())) return true;
        EnumAmmo type = ModItems.AMMO_STANDARD.typeOf(stack);
        if (type == null) return false;
        if (alsoAccepts != null) {
            for (EnumAmmo t : alsoAccepts) if (t == type) return true;
            return false;
        }
        // pre_rework_guns_use_new_ammo: any post-rework round of the same calibre
        return BalanceConfig.preReworkGunsUseNewAmmo
                && LegacyAmmo.sameCalibre(BuiltInRegistries.ITEM.getKey(ammo.get()).getPath(), type);
    }

    /**
     * The post-rework rounds a config whose own round is already post-rework loads (the old .44 shots), in place of
     * the calibre rule.
     */
    public EnumAmmo[] alsoAccepts;

    /** the round shown for this config: the first twin once the old round is converted */
    public ItemStack ammoStack() {
        if (ammo == null) return ItemStack.EMPTY;
        if (alsoAccepts == null && BalanceConfig.preReworkGunsUseNewAmmo) {
            EnumAmmo[] twins = LegacyAmmo.twins(BuiltInRegistries.ITEM.getKey(ammo.get()).getPath());
            if (twins.length > 0) return ModItems.AMMO_STANDARD.stack(twins[0]);
        }
        return new ItemStack(ammo.get());
    }

    public BulletConfiguration setToBolt(int trail) {
        this.style = STYLE_BOLT;
        this.trail = trail;
        return this;
    }

    public BulletConfiguration setToFire(int duration) {
        this.incendiary = duration;
        return this;
    }

    public BulletConfiguration setHeadshot(float mult) {
        this.headshotMult = mult;
        return this;
    }

    public BulletConfiguration accuracyMod(float mod) {
        this.spread *= mod;
        return this;
    }

    /** main's laser-guided rockets: they steer to where the shooter looks */
    public BulletConfiguration setToGuided() {
        this.bntUpdate = com.hbm.items.weapon.legacy.factory.BulletConfigFactory.getLaserSteering();
        this.doesRicochet = false;
        return this;
    }

    public BulletConfiguration setToHoming(Supplier<? extends net.minecraft.world.item.Item> ammo) {
        this.ammo = ammo;
        return getChlorophyte();
    }

    public BulletConfiguration getChlorophyte() {
        this.bntUpdate = com.hbm.items.weapon.legacy.factory.BulletConfigFactory.getHomingBehavior(30, 180);
        this.bntHurt = com.hbm.items.weapon.legacy.factory.BulletConfigFactory.getPenHomingBehavior();
        this.dmgMin *= 2F;
        this.dmgMax *= 2F;
        this.wear *= 0.5;
        this.velocity *= 0.3;
        this.doesRicochet = false;
        this.doesPenetrate = true;
        this.vPFX = "greendust";
        if (this.spentCasing != null) {
            int[] colors = this.spentCasing.getColors();
            this.spentCasing = this.spentCasing.clone();
            if (colors != null && colors.length > 0) {
                int[] colorClone = colors.clone();
                colorClone[colorClone.length - 1] = 0x659750; // standard chlorophyte colouring in last place
                this.spentCasing.setColor(colorClone).register(this.spentCasing.getName() + "Cl");
            }
        }
        return this;
    }

    public DamageSource getDamage(EntityBulletBaseNT bullet, LivingEntity shooter) {
        var type =
                bullet.level()
                        .registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(damageType);
        return shooter != null ? new DamageSource(type, bullet, shooter) : new DamageSource(type);
    }

    @Override
    public BulletConfiguration clone() {
        try {
            return (BulletConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            return new BulletConfiguration();
        }
    }
}
