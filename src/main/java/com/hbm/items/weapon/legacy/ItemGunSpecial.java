// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only
package com.hbm.items.weapon.legacy;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityBullet;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.lib.ModDamageTypes;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * Main 1.0.27_X5000's bow-held one-offs (GunSuicide, GunJack, GunEuthanasia, GunDefabricator): right click
 * draws like a bow, the shot goes on release or every few ticks while held. Lore comes from the lang .desc keys.
 */
public abstract class ItemGunSpecial extends ItemCustomLore {

    protected final Predicate<ItemStack> ammo;

    protected ItemGunSpecial(Properties properties, double attack, Predicate<ItemStack> ammo) {
        super(properties.stacksTo(1).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attack, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build()));
        this.ammo = ammo;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public com.hbm.backport.InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
        if (canStart(player)) player.startUsingItem(hand);
        return com.hbm.backport.InteractionResult.CONSUME;
    }

    protected boolean canStart(Player player) { return true; }

    protected boolean hasAmmo(Player player) {
        if (player.getAbilities().instabuild) return true;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (ammo.test(inv.getItem(i))) return true;
        return false;
    }

    protected void consumeAmmo(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (ammo.test(s)) { s.shrink(1); return; }
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player) usingTick(level, player, stack, count);
    }

    protected void usingTick(Level level, Player player, ItemStack stack, int count) {}

    /** The charge main's onPlayerStoppedUsing worked with: ticks held, nothing below 10. */
    protected static int charge(ItemGunSpecial gun, ItemStack stack, LivingEntity e, int timeLeft) {
        return gun.getUseDuration(stack, e) - timeLeft;
    }

    protected static Predicate<ItemStack> is(java.util.function.Supplier<? extends Item> item) {
        return s -> !s.isEmpty() && s.is(item.get());
    }

    /** gun_revolver_inverted: one lead bullet, then the shooter dies. */
    public static class Suicide extends ItemGunSpecial {
        public Suicide(Properties p) {
            // main took any ModItems.ammo_357 meta
            super(p.durability(500), 2.5D, s -> s.getItem() instanceof ItemAmmoLegacy && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().startsWith("ammo_357"));
        }

        @Override
        public boolean backport$releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
            if (!(entity instanceof Player player) || !hasAmmo(player) || charge(this, stack, entity, timeLeft) < 10) return false;
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            LegacySounds.play(player, "weapon.revolverShoot", 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) consumeAmmo(player);
            if (level instanceof ServerLevel) {
                player.hurt(level.damageSources().source(ModDamageTypes.SUICIDE), 10000);
                if (!player.getAbilities().instabuild) player.setHealth(0.0F);
            }
            return true;
        }
    }

    /** gun_jack: four shells, 24 to 48 pellets. */
    public static class Jack extends ItemGunSpecial {
        public int dmgMin = 12;
        public int dmgMax = 24;

        public Jack(Properties p) { super(p, 4.5D, is(LegacyGuns.GUN_JACK_AMMO)); }

        @Override
        protected boolean canStart(Player player) { return hasAmmo(player); }

        @Override
        public boolean backport$releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
            if (!(entity instanceof Player player) || !hasAmmo(player) || charge(this, stack, entity, timeLeft) < 10) return false;
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            LegacySounds.play(player, "weapon.shotgunShoot", 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) consumeAmmo(player);
            int k = level.random.nextInt(25) + 24;
            for (int i = 0; i < k; i++) {
                EntityBullet bullet = new EntityBullet(ModEntities.BULLET.get(), level, player, 3.0F);
                bullet.damage = dmgMin + level.random.nextInt(dmgMax - dmgMin);
                if (!level.isClientSide) level.addFreshEntity(bullet);
            }
            return true;
        }
    }

    /** gun_euthanasia and gun_twigun: a syringe every 8 ticks, one in five kills outright. */
    public static class Euthanasia extends ItemGunSpecial {
        public Euthanasia(Properties p) { super(p, 3.0D, is(LegacyGuns.GUN_EUTHANASIA_AMMO)); }

        @Override
        protected void usingTick(Level level, Player player, ItemStack stack, int count) {
            if (!hasAmmo(player) || count % 8 != 0) return;
            EntityBullet bullet = new EntityBullet(ModEntities.BULLET.get(), level, player, 3.0F, 2, 8, level.random.nextInt(5) == 0, false);
            bullet.damage = 1 + level.random.nextInt(3);
            bullet.antidote = true;
            LegacySounds.play(player, "weapon.rifleShoot", 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            if (player.getAbilities().instabuild) bullet.canBePickedUp = 2;
            else consumeAmmo(player);
            if (!level.isClientSide) level.addFreshEntity(bullet);
        }
    }

    /** gun_defabricator: a tau bolt every other tick, a cell every second. */
    public static class Defabricator extends ItemGunSpecial {
        public Defabricator(Properties p) { super(p, 6.5D, is(LegacyGuns.GUN_DEFABRICATOR_AMMO)); }

        @Override
        protected void usingTick(Level level, Player player, ItemStack stack, int count) {
            if (!hasAmmo(player) || count % 2 != 0) return;
            EntityBullet bullet = new EntityBullet(ModEntities.BULLET.get(), level, player, 3.0F, 40, 120, false, "tauDay");
            bullet.damage = 40 + level.random.nextInt(120 - 40);
            LegacySounds.play(player, "weapon.defabShoot", 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
            if (count == getUseDuration(stack, player)) LegacySounds.play(player, "weapon.defabSpinup", 1.0F, 1.0F);
            if (count % 20 == 0 && !player.getAbilities().instabuild) consumeAmmo(player);
            if (!level.isClientSide) level.addFreshEntity(bullet);
        }
    }

    /** gun_lever_action_sonata: the mare's leg, flipped; one 20 gauge shell, a kick backwards and the shooter dies. */
    public static class Sonata extends ItemGunSpecial {
        public int dmgMin = 8;
        public int dmgMax = 16;

        public Sonata(Properties p) {
            // main took any ModItems.ammo_20gauge meta
            super(p.durability(500), 3.5D, s -> s.getItem() instanceof ItemAmmoLegacy && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().startsWith("ammo_20gauge"));
        }

        @Override
        public com.hbm.backport.InteractionResult backport$use(Level level, Player player, InteractionHand hand) {
            if (getAnim(player.getItemInHand(hand)) == 0) player.startUsingItem(hand);
            return com.hbm.backport.InteractionResult.CONSUME;
        }

        @Override
        public boolean backport$releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
            if (!(entity instanceof Player player) || !hasAmmo(player) || charge(this, stack, entity, timeLeft) < 10) return false;
            net.minecraft.world.phys.Vec3 vec = player.getLookAngle().scale(-1).normalize();
            player.push(vec.x * 0.75, vec.y * 0.75, vec.z * 0.75);
            consumeAmmo(player);
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            if (level instanceof ServerLevel) {
                player.hurt(level.damageSources().source(ModDamageTypes.SUICIDE), 10000);
                if (!player.getAbilities().instabuild) player.setHealth(0.0F);
            }
            LegacySounds.play(player, "weapon.revolverShootAlt", 5.0F, 0.75F);
            setAnim(stack, 1);
            return true;
        }

        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, net.minecraft.world.entity.Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
            int j = getAnim(stack);
            if (j > 0) {
                setAnim(stack, j < 30 ? j + 1 : 0);
                if (j == 15) LegacySounds.play(entity, "weapon.leverActionReload", 2F, 0.85F);
            }
        }

        @Override
        public net.minecraft.network.chat.Component getName(ItemStack stack) {
            return net.minecraft.network.chat.Component.translatable(getDescriptionId() + (com.hbm.main.Polaroid.isBalefireDay() ? "_2" : ""));
        }

        public static int getAnim(ItemStack stack) {
            return com.hbm.backport.Nbt.getIntOr(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag(), "animation", 0);
        }

        private static void setAnim(ItemStack stack, int i) {
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, t -> t.putInt("animation", i));
        }

        public static float getRotationFromAnim(ItemStack stack) {
            float rad = 0.0174533F * 7.5F;
            int i = getAnim(stack);
            if (i < 10) return 0;
            i -= 10;
            return i < 10 ? rad * i : (rad * 10) - (rad * (i - 10));
        }

        public static float getOffsetFromAnim(ItemStack stack) {
            float i = getAnim(stack);
            if (i < 10) return 0;
            i -= 10;
            return i < 10 ? i / 10 : 2 - (i / 10);
        }
    }

    /**
     * gun_dash: while held, a laser every other tick. main spawned an EntityLaser that lived two ticks and
     * hurt everything within a block of where the shooter looked; the gun does that itself here.
     */
    public static class Dash extends ItemGunSpecial {
        public Dash(Properties p) { super(p, 3.0D, is(LegacyGuns.GUN_DASH_AMMO)); }

        @Override
        protected void usingTick(Level level, Player player, ItemStack stack, int count) {
            if (!hasAmmo(player)) return;
            if (count % 2 == 0) {
                LegacySounds.play(player, "weapon.rifleShoot", 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
                if (!player.getAbilities().instabuild) consumeAmmo(player);
            }
            if (!(level instanceof ServerLevel server)) return;
            net.minecraft.world.phys.Vec3 hit = player.pick(100, 1.0F, false).getLocation();
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, hit.x, hit.y, hit.z, 1, 0, 0, 0, 0);
            level.playSound(null, hit.x, hit.y, hit.z, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
            for (net.minecraft.world.entity.Entity e : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(hit.x - 1, hit.y - 1, hit.z - 1, hit.x + 1, hit.y + 1, hit.z + 1)))
                e.hurt(level.damageSources().source(ModDamageTypes.RADIATION), 5);
        }
    }

    /**
     * gun_folly_legacy: main's GunFolly. right click opens it, loads a silver bullet, closes it and starts
     * a 100 tick lock while held; once locked a click fires main's test bullet and throws the shooter back.
     * main's EntitySSmokeFX puffs are the repo's large smoke particles here.
     */
    public static class Folly extends ItemCustomLore {
        public Folly(Properties p) { super(p.stacksTo(1)); }

        @Override
        public com.hbm.backport.InteractionResult backport$use(Level world, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            int state = get(stack, "state");
            if (state == 0) {
                LegacySounds.play(player, "weapon.follyOpen", 1.0F, 1.0F);
                set(stack, "state", 1);
            } else if (state == 1) {
                Predicate<ItemStack> ammo = is(LegacyGuns.AMMO_FOLLY);
                ItemStack found = ItemStack.EMPTY;
                Inventory inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize() && found.isEmpty(); i++) if (ammo.test(inv.getItem(i))) found = inv.getItem(i);
                if (!found.isEmpty()) {
                    LegacySounds.play(player, "weapon.follyReload", 1.0F, 1.0F);
                    found.shrink(1);
                    set(stack, "state", 2);
                } else {
                    LegacySounds.play(player, "weapon.follyClose", 1.0F, 1.0F);
                    set(stack, "state", 0);
                }
            } else if (state == 2) {
                LegacySounds.play(player, "weapon.follyClose", 1.0F, 1.0F);
                set(stack, "state", 3);
                set(stack, "timer", 100);
            } else if (state == 3 && get(stack, "timer") == 0) {
                set(stack, "state", 0);
                LegacySounds.play(player, "weapon.follyFire", 1.0F, 1.0F);
                net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                player.push(-look.x * 1.75D, -look.y * 1.75D, -look.z * 1.75D);
                if (world instanceof ServerLevel server) {
                    world.addFreshEntity(new com.hbm.entity.projectile.EntityBulletBaseNT(world, BulletConfigSyncingUtil.TEST_CONFIG, player));
                    for (int i = 0; i < 25; i++)
                        server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                player.getX() + look.x + world.random.nextGaussian() * 0.35, player.getEyeY() + look.y + world.random.nextGaussian() * 0.35,
                                player.getZ() + look.z + world.random.nextGaussian() * 0.35, 0, look.x, look.y, look.z, 1);
                }
            }
            return com.hbm.backport.InteractionResult.SUCCESS;
        }

        @Override
        public void inventoryTick(ItemStack stack, ServerLevel world, net.minecraft.world.entity.Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
            if (get(stack, "state") != 3) return;
            if (slot == EquipmentSlot.MAINHAND) {
                int timer = get(stack, "timer");
                if (timer > 0) {
                    timer--;
                    if (timer % 20 == 0 && timer != 0) LegacySounds.play(entity, "weapon.follyBuzzer", 1.0F, 1.0F);
                    if (timer == 0) LegacySounds.play(entity, "weapon.follyAquired", 1.0F, 1.0F);
                    set(stack, "timer", timer);
                }
            } else {
                set(stack, "timer", 100);
            }
        }

        public static int get(ItemStack stack, String key) {
            return com.hbm.backport.Nbt.getIntOr(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag(), key, 0);
        }

        private static void set(ItemStack stack, String key, int v) {
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, t -> t.putInt(key, v));
        }
    }

    private static void spray(Level level, Player player, net.minecraft.world.level.material.Fluid fluid) {
        if (level.isClientSide) return;
        com.hbm.entity.projectile.EntityChemical chem = new com.hbm.entity.projectile.EntityChemical(ModEntities.CHEMICAL.get(), level, player, 0, 0, 0);
        chem.setFluid(fluid);
        level.addFreshEntity(chem);
    }

    /** main's EntityPlasmaBeam (25 - 45) is the repo's legacy EntityBullet here. */
    private static EntityBullet plasma(Level level, Player player) {
        EntityBullet b = new EntityBullet(ModEntities.BULLET.get(), level, player, 1F, 25, 45, false, false);
        if (player.getAbilities().instabuild) b.canBePickedUp = 2;
        return b;
    }

    /** gun_immolator: fire while held, plasma while sneaking. main's EntityFire is the repo's EntityChemical spraying diesel. */
    public static class Immolator extends ItemGunSpecial {
        public Immolator(Properties p) { super(p, 4.0D, is(LegacyGuns.GUN_IMMOLATOR_AMMO)); }

        @Override
        protected void usingTick(Level level, Player player, ItemStack stack, int count) {
            if (!hasAmmo(player)) return;
            boolean creative = player.getAbilities().instabuild;
            if (!player.isShiftKeyDown()) {
                if (!creative && count % 10 == 0) consumeAmmo(player);
                if (count == getUseDuration(stack, player)) LegacySounds.play(player, "weapon.flamethrowerIgnite", 1.0F, 1F);
                if (count % 5 == 0) LegacySounds.play(player, "weapon.flamethrowerShoot", 1.0F, 1F);
                spray(level, player, com.hbm.inventory.fluid.NTMFluids.DIESEL);
            } else {
                EntityBullet b = plasma(level, player);
                if (!creative && count % 4 == 0) consumeAmmo(player);
                if (count == getUseDuration(stack, player)) LegacySounds.play(player, "weapon.immolatorIgnite", 1.0F, 1F);
                if (count % 10 == 0) LegacySounds.play(player, "weapon.immolatorShoot", 1.0F, 1F);
                if (!level.isClientSide) level.addFreshEntity(b);
            }
        }
    }

    /** gun_cryolator: main's EntityLN2 is the repo's EntityChemical spraying oxygen, like the cryo cannon. */
    public static class Cryolator extends ItemGunSpecial {
        public Cryolator(Properties p) { super(p, 4.0D, is(LegacyGuns.GUN_CRYOLATOR_AMMO)); }

        @Override
        protected void usingTick(Level level, Player player, ItemStack stack, int count) {
            if (!hasAmmo(player)) return;
            if (!player.getAbilities().instabuild && count % 10 == 0) consumeAmmo(player);
            if (count % 5 == 0) LegacySounds.play(player, "weapon.cryolatorShoot", 1.0F, 1F);
            spray(level, player, com.hbm.inventory.fluid.NTMFluids.OXYGEN);
        }
    }

    /** gun_hp: five plasma bolts a tick, four of them scattered. */
    public static class HP extends ItemGunSpecial {
        public HP(Properties p) { super(p, 4.0D, is(LegacyGuns.GUN_HP_AMMO)); }

        @Override
        protected void usingTick(Level level, Player player, ItemStack stack, int count) {
            if (!hasAmmo(player)) return;
            if (!player.getAbilities().instabuild && count % 20 == 0) consumeAmmo(player);
            if (count == getUseDuration(stack, player)) LegacySounds.play(player, "weapon.immolatorIgnite", 1.0F, 1F);
            if (count % 10 == 0) LegacySounds.play(player, "weapon.immolatorShoot", 1.0F, 1F);
            if (level.isClientSide) return;
            for (int i = 0; i < 5; i++) {
                EntityBullet b = plasma(level, player);
                if (i > 0) {
                    net.minecraft.world.phys.Vec3 m = b.getDeltaMovement();
                    b.setDeltaMovement(m.x * (0.75 + level.random.nextDouble() * 0.5), m.y * (0.75 + level.random.nextDouble() * 0.5), m.z * (0.75 + level.random.nextDouble() * 0.5));
                }
                level.addFreshEntity(b);
            }
        }
    }

    /** gun_spark: one beam on release that explodes where it lands; main's EntitySparkBeam is the b92's beam here. */
    public static class Spark extends ItemGunSpecial {
        public Spark(Properties p) { super(p, 4.5D, is(LegacyGuns.GUN_SPARK_AMMO)); }

        @Override
        public boolean backport$releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
            if (!(entity instanceof Player player) || !hasAmmo(player) || charge(this, stack, entity, timeLeft) < 10) return false;
            LegacySounds.play(player, "weapon.sparkShoot", 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) consumeAmmo(player);
            if (!level.isClientSide) level.addFreshEntity(new com.hbm.entity.projectile.EntityB92Beam(ModEntities.BEAM_BOMB.get(), player, 3F));
            return true;
        }
    }
}
