// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.legacy;

import com.hbm.backport.TooltipDisplay;
import com.hbm.backport.compat.ItemCompat;
import com.hbm.data.ItemData;
import com.hbm.entity.projectile.EntityBulletBaseNT;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.IKeybindReceiver;
import com.hbm.items.IEquipReceiver;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.weapon.GunContent;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.HbmAnimationPayload;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.render.anim.BusAnimation;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * backport-fix: BF-161 main 1.0.27_X5000's pre-rework gun (items/weapon/ItemGunBase). Mouse and
 * reload buttons come in through the gun keybinds (main's GunButtonPacket); the state lives in the
 * gun state component under main's NBT keys.
 */
public class ItemGunBase extends ItemCompat implements IKeybindReceiver, IEquipReceiver {

    public final GunConfiguration mainConfig;
    public final @Nullable GunConfiguration altConfig;

    public ItemGunBase(GunConfiguration config, Properties properties) {
        this(config, null, properties);
    }

    public ItemGunBase(
            GunConfiguration config, @Nullable GunConfiguration alt, Properties properties) {
        super(properties);
        mainConfig = config;
        altConfig = alt;
    }

    // ---- input (main's GunButtonPacket) ----

    @Override
    public boolean canHandleKeybind(Player player, ItemStack stack, EnumKeybind keybind) {
        return keybind == EnumKeybind.GUN_PRIMARY
                || keybind == EnumKeybind.GUN_SECONDARY
                || keybind == EnumKeybind.GUN_TERTIARY
                || keybind == EnumKeybind.RELOAD;
    }

    /** aiming, toggled by the post-rework aim key (main aimed these by sneaking) */
    public static boolean isAiming(net.minecraft.world.entity.@Nullable LivingEntity entity) {
        if (entity == null) return false;
        ItemStack held = entity.getMainHandItem();
        return held.getItem() instanceof ItemGunBase && com.hbm.items.weapon.sedna.ItemGunBaseNT.getIsAiming(held);
    }

    @Override
    public void handleKeybind(Player player, ItemStack stack, EnumKeybind keybind, boolean state) {
        Level world = player.level();
        if (world.isClientSide() || player.getMainHandItem() != stack) return;
        if (!ItemData.ENABLE_GUNS.get() || GunContent.isDisabled(this)) return;
        switch (keybind) {
            case GUN_PRIMARY -> {
                if (state && getIsAltDown(stack)) return;
                setIsMouseDown(stack, state);
                if (state) startAction(stack, world, player, true);
                else endAction(stack, world, player, true);
            }
            case GUN_TERTIARY -> {
                if (state)
                    com.hbm.items.weapon.sedna.ItemGunBaseNT.setIsAiming(
                            stack, !com.hbm.items.weapon.sedna.ItemGunBaseNT.getIsAiming(stack));
            }
            case GUN_SECONDARY -> {
                if (state && getIsMouseDown(stack)) return;
                setIsAltDown(stack, state);
                if (state) startAction(stack, world, player, false);
                else endAction(stack, world, player, false);
            }
            case RELOAD -> {
                if (!state) return;
                boolean reloads =
                        mainConfig.reloadType != GunConfiguration.RELOAD_NONE
                                || (altConfig != null && altConfig.reloadType != 0);
                if (reloads
                        && (getMag(stack) < mainConfig.ammoCap || hasInfinity(stack, mainConfig))
                        && canReload(stack, world, player)) startReloadAction(stack, world, player);
            }
            default -> {}
        }
    }

    // ---- ticking ----

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;
        boolean isCurrentItem = slot == EquipmentSlot.MAINHAND && player.getMainHandItem() == stack;
        boolean wasHeld = readNBT(stack, "isEquipped") == 1;
        if (isCurrentItem != wasHeld) {
            writeNBT(stack, "isEquipped", isCurrentItem ? 1 : 0);
            if (isCurrentItem) onEquip(player, stack);
        }
        updateServer(stack, level, player, isCurrentItem);
    }

    protected void updateServer(
            ItemStack stack, Level world, Player player, boolean isCurrentItem) {

        if (getDelay(stack) > 0 && isCurrentItem) setDelay(stack, getDelay(stack) - 1);

        if (getIsMouseDown(stack) && !isCurrentItem) setIsMouseDown(stack, false);

        int burstDuration = getBurstDuration(stack);
        if (burstDuration > 0) {
            if (altConfig == null) {
                if (burstDuration % mainConfig.firingDuration == 0
                        && tryShoot(stack, world, player, true)) fire(stack, world, player);
            } else {
                boolean canFire =
                        altConfig.firingDuration == 1
                                || burstDuration % altConfig.firingDuration == 0;
                if (canFire && tryShoot(stack, world, player, false))
                    altFire(stack, world, player);
            }

            setBurstDuration(stack, getBurstDuration(stack) - 1);
            if (getBurstDuration(stack) == 0) setDelay(stack, mainConfig.rateOfFire);
        }

        if (getIsAltDown(stack) && !isCurrentItem) setIsAltDown(stack, false);

        if (ItemData.ENABLE_GUNS.get()
                && !GunContent.isDisabled(this)
                && mainConfig.firingMode == 1
                && getIsMouseDown(stack)
                && tryShoot(stack, world, player, isCurrentItem)) {
            fire(stack, world, player);
            setDelay(stack, mainConfig.rateOfFire);
        }

        if (getIsReloading(stack) && isCurrentItem) reload2(stack, world, player);

        BulletConfiguration queued = getCasing(stack);
        int timer = getCasingTimer(stack);

        if (queued != null && timer > 0) {
            timer--;
            if (timer <= 0) trySpawnCasing(player, mainConfig.ejector, queued, stack);
            setCasingTimer(stack, timer);
        }
    }

    // ---- firing ----

    /** whether or not the gun can shoot in its current state */
    protected boolean tryShoot(ItemStack stack, Level world, Player player, boolean main) {

        // cancel reload when trying to shoot if it's a single reload weapon and at least one round
        // is loaded
        if (getIsReloading(stack)
                && mainConfig.reloadType == GunConfiguration.RELOAD_SINGLE
                && getMag(stack) > 0) {
            setReloadCycle(stack, 0);
            setIsReloading(stack, false);
        }

        if (main
                && getDelay(stack) == 0
                && !getIsReloading(stack)
                && getItemWear(stack) < mainConfig.durability) return hasAmmo(stack, player, main);

        if (!main
                && altConfig != null
                && getDelay(stack) == 0
                && !getIsReloading(stack)
                && getItemWear(stack) < mainConfig.durability) return hasAmmo(stack, player, false);

        return false;
    }

    public boolean hasAmmo(ItemStack stack, Player player, boolean main) {
        GunConfiguration config = main ? mainConfig : altConfig;
        if (config.reloadType == GunConfiguration.RELOAD_NONE) {
            return getBeltSize(player, getBeltType(player, stack, main)) > 0;
        } else {
            return getMag(stack) > 0;
        }
    }

    /** called on the server when the gun shoots its main fire */
    protected void fire(ItemStack stack, Level world, Player player) {

        BulletConfiguration config;

        if (mainConfig.reloadType == GunConfiguration.RELOAD_NONE) {
            config = getBeltCfg(player, stack, true);
        } else {
            config = BulletConfigSyncingUtil.pullConfig(mainConfig.config.get(getMagType(stack)));
        }
        if (config == null) return;

        int bullets = config.bulletsMin;

        for (int k = 0; k < mainConfig.roundsPerCycle; k++) {

            if (!hasAmmo(stack, player, true)) break;

            if (config.bulletsMax > config.bulletsMin)
                bullets += world.getRandom().nextInt(config.bulletsMax - config.bulletsMin);

            for (int i = 0; i < bullets; i++) {
                spawnProjectile(world, player, stack, BulletConfigSyncingUtil.getKey(config));
            }

            useUpAmmo(player, stack, true);
            player.inventoryMenu.broadcastChanges();

            int wear =
                    (int)
                            Math.ceil(
                                    config.wear
                                            / (1F + enchantment(world, stack, Enchantments.UNBREAKING)));
            setItemWear(stack, getItemWear(stack) + wear);
        }

        AnimType animType = getMag(stack) == 0 ? AnimType.CYCLE_EMPTY : AnimType.CYCLE;
        playAnimation(player, stack, animType);

        String firingSound = mainConfig.firingSound;
        if (getMag(stack) == 0 && mainConfig.firingSoundEmpty != null)
            firingSound = mainConfig.firingSoundEmpty;
        LegacySounds.play(player, firingSound, mainConfig.firingVolume, mainConfig.firingPitch);

        if (mainConfig.ejector != null && !mainConfig.ejector.afterReload)
            queueCasing(player, mainConfig.ejector, config, stack);
    }

    /** called on the server when the gun shoots its alt fire */
    protected void altFire(ItemStack stack, Level world, Player player) {

        if (altConfig == null) return;

        BulletConfiguration config =
                altConfig.reloadType == GunConfiguration.RELOAD_NONE
                        ? getBeltCfg(player, stack, false)
                        : BulletConfigSyncingUtil.pullConfig(
                                altConfig.config.get(getMagType(stack)));
        if (config == null) return;

        int bullets = config.bulletsMin;

        for (int k = 0; k < altConfig.roundsPerCycle; k++) {

            if (altConfig.reloadType != GunConfiguration.RELOAD_NONE
                    && !hasAmmo(stack, player, true)) break;

            if (config.bulletsMax > config.bulletsMin)
                bullets += world.getRandom().nextInt(config.bulletsMax - config.bulletsMin);

            for (int i = 0; i < bullets; i++) {
                spawnProjectile(world, player, stack, BulletConfigSyncingUtil.getKey(config));
            }

            playAnimation(player, stack, AnimType.ALT_CYCLE);

            useUpAmmo(player, stack, false);
            player.inventoryMenu.broadcastChanges();

            setItemWear(stack, getItemWear(stack) + config.wear);
        }

        LegacySounds.play(player, altConfig.firingSound, mainConfig.firingVolume, altConfig.firingPitch);

        if (altConfig.ejector != null) queueCasing(player, altConfig.ejector, config, stack);
    }

    /** spawns the actual projectile, can be overridden to change projectile entity */
    protected void spawnProjectile(Level world, Player player, ItemStack stack, int config) {
        EntityBulletBaseNT bullet = new EntityBulletBaseNT(world, config, player);
        world.addFreshEntity(bullet);
    }

    /** called on the server when the mouse button is pressed */
    public void startAction(ItemStack stack, Level world, Player player, boolean main) {

        boolean validConfig =
                mainConfig.firingMode == GunConfiguration.FIRE_MANUAL
                        || mainConfig.firingMode == GunConfiguration.FIRE_BURST;

        if (validConfig && main && tryShoot(stack, world, player, main)) {

            if (mainConfig.firingMode == GunConfiguration.FIRE_BURST) {
                if (getBurstDuration(stack) <= 0)
                    setBurstDuration(stack, mainConfig.firingDuration * mainConfig.roundsPerBurst);
            } else {
                fire(stack, world, player);
                setDelay(stack, mainConfig.rateOfFire);
            }
        }

        if (!main && altConfig != null && tryShoot(stack, world, player, main)) {

            if (altConfig.firingMode == GunConfiguration.FIRE_BURST
                    && getBurstDuration(stack) <= 0) {
                setBurstDuration(stack, altConfig.firingDuration * altConfig.roundsPerBurst);
            } else {
                altFire(stack, world, player);
                setDelay(stack, altConfig.rateOfFire);
            }
        }
    }

    /** called on the server when the mouse button is released */
    public void endAction(ItemStack stack, Level world, Player player, boolean main) {}

    // ---- reloading ----

    /** reload action, if existent */
    protected void reload2(ItemStack stack, Level world, Player player) {

        if (getMag(stack) >= mainConfig.ammoCap) {
            setIsReloading(stack, false);
            return;
        }

        if (getReloadCycle(stack) <= 0) {

            BulletConfiguration prevCfg =
                    BulletConfigSyncingUtil.pullConfig(mainConfig.config.get(getMagType(stack)));
            BulletConfiguration cfg =
                    BulletConfigSyncingUtil.pullConfig(mainConfig.config.get(getMagType(stack)));
            if (cfg == null) {
                setIsReloading(stack, false);
                return;
            }

            final int countNeeded =
                    (mainConfig.reloadType == GunConfiguration.RELOAD_FULL)
                            ? mainConfig.ammoCap - getMag(stack)
                            : 1;
            final int availableStacks = countAmmo(player, cfg);
            final int availableFills = availableStacks * cfg.ammoCount;
            final boolean hasLoaded = availableFills > 0;
            final int toAdd = Math.min(availableFills * cfg.ammoCount, countNeeded);
            final int toConsume = (int) Math.ceil((double) toAdd / cfg.ammoCount);

            // Skip logic if cannot reload
            if (availableFills == 0) {
                setIsReloading(stack, false);
                return;
            }

            String reloadSound =
                    mainConfig.reloadSoundEmpty != null && getMag(stack) == 0
                            ? mainConfig.reloadSoundEmpty
                            : mainConfig.reloadSound;

            setMag(stack, getMag(stack) + toAdd);

            if (getMag(stack) >= mainConfig.ammoCap) {
                setIsReloading(stack, false);
                playAnimation(player, stack, AnimType.RELOAD_END);
            } else {
                resetReloadCycle(player, stack);
                AnimType animType =
                        availableFills <= 1 ? AnimType.RELOAD_END : AnimType.RELOAD_CYCLE;
                playAnimation(player, stack, animType);
                if (availableFills > 1 && !mainConfig.reloadSoundEnd)
                    LegacySounds.play(player, reloadSound, 1.0F, 1.0F);
            }

            if (hasLoaded && mainConfig.reloadSoundEnd)
                LegacySounds.play(player, reloadSound, 1.0F, 1.0F);

            if (mainConfig.ejector != null && mainConfig.ejector.afterReload)
                queueCasing(player, mainConfig.ejector, prevCfg, stack);

            consumeAmmo(player, cfg, toConsume);

        } else {
            setReloadCycle(stack, getReloadCycle(stack) - 1);
        }

        if (stack != player.getMainHandItem()) {
            setReloadCycle(stack, 0);
            setIsReloading(stack, false);
        }
    }

    /** initiates a reload */
    public void startReloadAction(ItemStack stack, Level world, Player player) {

        if (getMag(stack) == 0) resetAmmoType(stack, world, player);

        if (player.isShiftKeyDown() && hasInfinity(stack, mainConfig)) {

            if (getMag(stack) == mainConfig.ammoCap) {
                setMag(stack, 0);
                this.resetAmmoType(stack, world, player);
                LegacySounds.play(player, "tile.piston.out", 1.0F, 1.0F);
            }

            return;
        }

        if (getMag(stack) == mainConfig.ammoCap) return;

        if (getIsReloading(stack)) return;

        if (!mainConfig.reloadSoundEnd) {
            String reloadSound =
                    mainConfig.reloadSoundEmpty != null && getMag(stack) == 0
                            ? mainConfig.reloadSoundEmpty
                            : mainConfig.reloadSound;
            LegacySounds.play(player, reloadSound, 1.0F, 1.0F);
        }

        AnimType reloadType = getMag(stack) == 0 ? AnimType.RELOAD_EMPTY : AnimType.RELOAD;
        playAnimation(player, stack, reloadType);

        setIsReloading(stack, true);
        resetReloadCycle(player, stack);
    }

    public boolean canReload(ItemStack stack, Level world, Player player) {

        if (getMag(stack) == mainConfig.ammoCap && hasInfinity(stack, mainConfig)) return true;

        if (getMag(stack) == 0) {
            for (int config : mainConfig.config) {
                BulletConfiguration cfg = BulletConfigSyncingUtil.pullConfig(config);
                if (cfg != null && hasAmmoItem(player, cfg)) return true;
            }
        } else {
            BulletConfiguration cfg =
                    BulletConfigSyncingUtil.pullConfig(mainConfig.config.get(getMagType(stack)));
            return cfg != null && hasAmmoItem(player, cfg);
        }

        return false;
    }

    /** searches the player's inv for the next fitting ammo type and changes the gun's mag */
    protected void resetAmmoType(ItemStack stack, Level world, Player player) {
        for (int i = 0; i < mainConfig.config.size(); i++) {
            BulletConfiguration cfg = BulletConfigSyncingUtil.pullConfig(mainConfig.config.get(i));
            if (cfg != null && hasAmmoItem(player, cfg)) {
                setMagType(stack, i);
                break;
            }
        }
    }

    // ---- tooltip ----

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> list,
            TooltipFlag flag) {

        BulletConfiguration cfg = magConfig(stack);
        if (cfg == null) return;

        list.accept(
                Component.translatable(
                        "desc.item.gun.ammo",
                        mainConfig.ammoCap > 0
                                ? Component.translatable(
                                        "desc.item.gun.ammoMag", getMag(stack), mainConfig.ammoCap)
                                : Component.translatable("desc.item.gun.ammoBelt")));

        list.accept(Component.translatable("desc.item.gun.ammoType", cfg.ammoStack().getHoverName()));
        if (altConfig != null && altConfig.ammoCap == 0) {
            BulletConfiguration alt = BulletConfigSyncingUtil.pullConfig(altConfig.config.get(0));
            if (alt != null && alt.ammo != null && cfg.ammo != null && alt.ammo.get() != cfg.ammo.get())
                list.accept(
                        Component.translatable("desc.item.gun.ammoTypeAlt", alt.ammoStack().getHoverName()));
        }

        addAdditionalInformation(stack, list);
    }

    protected void addAdditionalInformation(ItemStack stack, Consumer<Component> list) {
        BulletConfiguration bulletConfig = magConfig(stack);
        list.accept(
                Component.translatable("desc.item.gun.damage", bulletConfig.dmgMin, bulletConfig.dmgMax));
        if (bulletConfig.bulletsMax != 1)
            list.accept(
                    Component.translatable(
                            "desc.item.gun.pellets", bulletConfig.bulletsMin, bulletConfig.bulletsMax));
        int dura = Math.max(mainConfig.durability - getItemWear(stack), 0);
        list.accept(Component.translatable("desc.item.durability", dura + " / " + mainConfig.durability));

        list.accept(Component.empty());
        list.accept(
                Component.translatable(
                        "desc.item.gun.name",
                        Component.translatableWithFallback("gun.name." + mainConfig.name, mainConfig.name)));
        list.accept(
                Component.translatable(
                        "desc.item.gun.manufacturer",
                        Component.translatable(mainConfig.manufacturer.getKey())));

        if (!mainConfig.comment.isEmpty()) {
            list.accept(Component.empty());
            for (String s : mainConfig.comment)
                list.accept(Component.literal(s).withStyle(ChatFormatting.ITALIC));
        }
    }

    public @Nullable BulletConfiguration magConfig(ItemStack stack) {
        if (mainConfig.config.isEmpty()) return null;
        int mag = getMagType(stack);
        return BulletConfigSyncingUtil.pullConfig(
                mainConfig.config.get(mag < mainConfig.config.size() ? mag : 0));
    }

    // ---- belt / inventory ammo ----

    private static boolean hasAmmoItem(Player player, BulletConfiguration cfg) {
        for (ItemStack s : player.getInventory().items) if (cfg.isAmmo(s)) return true;
        return false;
    }

    private static int countAmmo(Player player, BulletConfiguration cfg) {
        int count = 0;
        for (ItemStack s : player.getInventory().items) if (cfg.isAmmo(s)) count += s.getCount();
        return count;
    }

    private static void consumeAmmo(Player player, BulletConfiguration cfg, int amount) {
        for (ItemStack s : player.getInventory().items) {
            if (amount <= 0) return;
            if (!cfg.isAmmo(s)) continue;
            int take = Math.min(amount, s.getCount());
            s.shrink(take);
            amount -= take;
        }
    }

    /** the ammo a belt-fed gun draws next: the first config whose ammo the player carries */
    public static @Nullable BulletConfiguration getBeltCfg(Player player, ItemStack stack, boolean main) {
        ItemGunBase gun = (ItemGunBase) stack.getItem();
        GunConfiguration guncfg = main ? gun.mainConfig : (gun.altConfig != null ? gun.altConfig : gun.mainConfig);
        for (int config : guncfg.config) {
            BulletConfiguration cfg = BulletConfigSyncingUtil.pullConfig(config);
            if (cfg != null && hasAmmoItem(player, cfg)) return cfg;
        }
        return guncfg.config.isEmpty() ? null : BulletConfigSyncingUtil.pullConfig(guncfg.config.get(0));
    }

    public static @Nullable BulletConfiguration getBeltType(Player player, ItemStack stack, boolean main) {
        return getBeltCfg(player, stack, main);
    }

    public static int getBeltSize(Player player, @Nullable BulletConfiguration ammo) {
        return ammo == null ? 0 : countAmmo(player, ammo);
    }

    public void useUpAmmo(Player player, ItemStack stack, boolean main) {

        if (!main && altConfig == null) return;

        GunConfiguration config = main ? mainConfig : altConfig;

        if (hasInfinity(stack, config)) return;
        if (isTrenchMaster(player) && player.getRandom().nextInt(3) == 0) return;
        if (hasAoS(player) && player.getRandom().nextInt(3) == 0) return;

        if (config.reloadType != GunConfiguration.RELOAD_NONE) {
            setMag(stack, getMag(stack) - 1);
        } else {
            BulletConfiguration belt = getBeltType(player, stack, main);
            if (belt != null) consumeAmmo(player, belt, 1);
        }
    }

    public boolean hasInfinity(ItemStack stack, GunConfiguration config) {
        return config.allowsInfinity
                && clientOrServerEnchantment(stack, Enchantments.INFINITY) > 0;
    }

    private static int clientOrServerEnchantment(ItemStack stack, ResourceKey<Enchantment> key) {
        var enchantments = stack.getEnchantments();
        for (var entry : enchantments.entrySet())
            if (entry.getKey().is(key)) return entry.getIntValue();
        return 0;
    }

    private static int enchantment(Level world, ItemStack stack, ResourceKey<Enchantment> key) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), stack);
    }

    // ---- casings ----

    protected static void queueCasing(
            Entity entity, GunConfiguration.Ejector ejector, BulletConfiguration bullet, ItemStack stack) {

        if (ejector == null || bullet == null || bullet.spentCasing == null) return;

        if (ejector.delay <= 0) {
            trySpawnCasing(entity, ejector, bullet, stack);
        } else {
            setCasing(stack, bullet);
            setCasingTimer(stack, ejector.delay);
        }
    }

    protected static void trySpawnCasing(
            Entity entity, GunConfiguration.Ejector ejector, BulletConfiguration bullet, ItemStack stack) {

        if (ejector == null) return; // abort if the gun can't eject bullets at all
        if (bullet == null) return; // abort if there's no valid bullet cfg
        if (bullet.spentCasing == null) return; // abort if the bullet is caseless
        if (!(entity instanceof Player player)) return;

        for (int i = 0; i < ejector.amount; i++)
            CasingCreator.composeEffect(
                    player.level(),
                    player,
                    ejector.offsetZ,
                    ejector.offsetY,
                    ejector.offsetX,
                    ejector.motionZ,
                    ejector.motionY,
                    ejector.motionX,
                    Math.max(ejector.yawFactor, ejector.pitchFactor),
                    bullet.spentCasing.getName());
    }

    // ---- equip & animations ----

    /** 1.7.10's ItemRenderer kept the hold when only the NBT changed (same item and damage) */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        com.hbm.items.weapon.sedna.ItemGunBaseNT.setIsAiming(stack, false);
        if (!mainConfig.equipSound.isEmpty()) LegacySounds.play(player, mainConfig.equipSound, 1, 1);
        playAnimation(player, stack, AnimType.EQUIP);
    }

    protected static void playAnimation(Player player, ItemStack stack, AnimType type) {
        if (player instanceof ServerPlayer serverPlayer)
            PacketWire.sendTo(new HbmAnimationPayload(serverPlayer, stack, type.ordinal(), 0, 0), serverPlayer);
    }

    /** client: the animation main's GunAnimationPacket would start, null for none */
    public @Nullable BusAnimation getAnimation(ItemStack stack, AnimType type) {
        GunConfiguration config = ((ItemGunBase) stack.getItem()).mainConfig;
        if (!config.animationsLoaded && config.loadAnimations != null) {
            config.loadAnimations.run();
            config.animationsLoaded = true;
        }
        return config.animations.get(type);
    }

    public Crosshair getCrosshair() {
        return mainConfig.crosshair;
    }

    // ---- state, main's NBT keys ----

    public static void resetReloadCycle(Player player, ItemStack stack) {
        writeNBT(stack, "reload", getReloadDuration(player, stack));
    }

    public static void setIsReloading(ItemStack stack, boolean b) {
        writeNBT(stack, "isReloading", b ? 1 : 0);
    }

    public static boolean getIsReloading(ItemStack stack) {
        return readNBT(stack, "isReloading") == 1;
    }

    public static void setIsMouseDown(ItemStack stack, boolean b) {
        writeNBT(stack, "isMouseDown", b ? 1 : 0);
    }

    public static boolean getIsMouseDown(ItemStack stack) {
        return readNBT(stack, "isMouseDown") == 1;
    }

    public static void setIsAltDown(ItemStack stack, boolean b) {
        writeNBT(stack, "isAltDown", b ? 1 : 0);
    }

    public static boolean getIsAltDown(ItemStack stack) {
        return readNBT(stack, "isAltDown") == 1;
    }

    public static void setDelay(ItemStack stack, int i) {
        writeNBT(stack, "dlay", i);
    }

    public static int getDelay(ItemStack stack) {
        return readNBT(stack, "dlay");
    }

    public static void setItemWear(ItemStack stack, int i) {
        writeNBT(stack, "wear", i);
    }

    public static int getItemWear(ItemStack stack) {
        return readNBT(stack, "wear");
    }

    public static void setReloadCycle(ItemStack stack, int i) {
        writeNBT(stack, "reload", i);
    }

    public static int getReloadCycle(ItemStack stack) {
        return readNBT(stack, "reload");
    }

    public static void setMag(ItemStack stack, int i) {
        writeNBT(stack, "magazine", i);
    }

    public static int getMag(ItemStack stack) {
        return readNBT(stack, "magazine");
    }

    public static void setMagType(ItemStack stack, int i) {
        writeNBT(stack, "magazineType", i);
    }

    public static int getMagType(ItemStack stack) {
        return readNBT(stack, "magazineType");
    }

    public static void setBurstDuration(ItemStack stack, int i) {
        writeNBT(stack, "bduration", i);
    }

    public static int getBurstDuration(ItemStack stack) {
        return readNBT(stack, "bduration");
    }

    public static void setCasing(ItemStack stack, BulletConfiguration bullet) {
        writeNBT(stack, "casing", BulletConfigSyncingUtil.getKey(bullet));
    }

    public static @Nullable BulletConfiguration getCasing(ItemStack stack) {
        return BulletConfigSyncingUtil.pullConfig(readNBT(stack, "casing"));
    }

    public static void setCasingTimer(ItemStack stack, int i) {
        writeNBT(stack, "casingTimer", i);
    }

    public static int getCasingTimer(ItemStack stack) {
        return readNBT(stack, "casingTimer");
    }

    /** writes only on change, so an idle gun doesn't resync its stack every tick */
    public static void writeNBT(ItemStack stack, String key, int value) {
        if (readNBT(stack, key) != value) ItemGunBaseNT.setValueInt(stack, key, value);
    }

    public static int readNBT(ItemStack stack, String key) {
        return ItemGunBaseNT.getValueInt(stack, key);
    }

    public static int getReloadDuration(Player player, ItemStack stack) {
        GunConfiguration config = ((ItemGunBase) stack.getItem()).mainConfig;
        int cycle = config.reloadDuration;
        if (getMag(stack) == 0) cycle += config.emptyReloadAdditionalDuration;
        if (isTrenchMaster(player)) return Math.max(1, cycle / 2);
        return cycle;
    }

    public static boolean isTrenchMaster(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.TRENCHMASTER_PLATE.get())
                && ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.TRENCHMASTER);
    }

    public static boolean hasAoS(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return false;
        ItemStack mod = ArmorModHandler.pryMods(helmet)[ArmorModHandler.HELMET_ONLY];
        return mod != null && mod.is(ModItems.CARD_AOS.get());
    }
}
