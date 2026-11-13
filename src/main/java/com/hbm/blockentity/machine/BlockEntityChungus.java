// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine;

import com.hbm.blockentity.AudioLoop;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.packet.SyncField;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityChungus extends BlockEntityTurbineBase
        implements AudioLoop, com.hbm.interfaces.IRepairable, com.hbm.blocks.machine.DamageableMachine.Holder {

    public static final String[] ROR = new String[] {PREFIX_VALUE + "output"};
    public static @Nullable Consumer<BlockEntityChungus> CLIENT_SOUND;
    public float rotor;
    public float lastRotor;
    public float fanAcceleration = 0F;

    @SyncField(units = 1L << 3)
    protected int turnTimer;

    public BlockEntityChungus(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.CHUNGUS.get(),
                pos,
                state,
                MachineData.CHUNGUS_INPUT_TANK_SIZE.get(),
                MachineData.CHUNGUS_OUTPUT_TANK_SIZE.get());
    }

    @Override
    public double consumptionPercent() {
        return 1D;
    }

    @Override
    public double getEfficiency() {
        return MachineData.CHUNGUS_EFFICIENCY.get();
    }

    @Override
    protected void onServerTick() {
        turnTimer--;
        if (operational) turnTimer = 25;
    }

    public boolean isTurning() {
        return turnTimer > 0;
    }

    @Override
    protected void onClientTick() {
        if (isDamaged()) return;
        this.lastRotor = this.rotor;
        this.rotor += this.fanAcceleration;
        if (this.rotor >= 360) {
            this.rotor -= 360;
            this.lastRotor -= 360;
        }

        if (turnTimer > 0) {
            this.fanAcceleration =
                    Mth.clamp(this.fanAcceleration + 0.075F + audioDesync(), 0F, 25F);

            Direction dir = facing();
            Direction side = dir.getClockWise();
            var rand = level.getRandom();
            for (int i = 0; i < 10; i++) {
                level.addParticle(
                        ParticleTypes.CLOUD,
                        worldPosition.getX()
                                + 0.5
                                + dir.getStepX() * (rand.nextDouble() + 1.25)
                                + rand.nextGaussian() * side.getStepX() * 0.65,
                        worldPosition.getY() + 2.5 + rand.nextGaussian() * 0.65,
                        worldPosition.getZ()
                                + 0.5
                                + dir.getStepZ() * (rand.nextDouble() + 1.25)
                                + rand.nextGaussian() * side.getStepZ() * 0.65,
                        -dir.getStepX() * 0.2,
                        0,
                        -dir.getStepZ() * 0.2);
            }
        } else {
            this.fanAcceleration = Mth.clamp(this.fanAcceleration - 0.1F, 0F, 25F);
        }

        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    private float audioDesync() {
        return Mth.positiveModulo(worldPosition.hashCode(), 50) / 1000F;
    }

    @Override
    public AudioWrapper createAudioLoop() {
        float speed = fanAcceleration / 25F;
        return AudioSystem.getLoopedSound(
                ModSounds.CHUNGUS_TURBINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                getVolume(0.5F * speed),
                20F,
                0.25F + 0.75F * speed,
                20);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "output").equals(name)) return "" + (int) this.powerBuffer;
        return null;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 3 -> output.writeInt(this.turnTimer);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 3 -> this.turnTimer = input.readInt();
            default -> super.readSyncUnit(unit, input);
        }
    }


    // backport: woven trait AudioLoop
    private AudioWrapper hbm$audio;

    public void audioLoop(boolean running, float volume) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, 0F, false);
    }

    public void audioLoop(boolean running, float volume, float pitch) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, pitch, true);
    }

    private void hbm$audioRemove() {
        audioLoop(false, 0F);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$audioRemove();
    }

    // NTM:Space: wrecked by an explosion (see DamageableMachine), repaired with a blowtorch
    private net.minecraft.world.level.@org.jspecify.annotations.Nullable Explosion lastExplosion;

    public boolean isDamaged() {
        return com.hbm.blocks.machine.DamageableMachine.isDamaged(getBlockState());
    }

    @Override
    public net.minecraft.world.level.@org.jspecify.annotations.Nullable Explosion lastExplosion() {
        return lastExplosion;
    }

    @Override
    public void lastExplosion(
            net.minecraft.world.level.@org.jspecify.annotations.Nullable Explosion explosion) {
        lastExplosion = explosion;
    }

    private static final java.util.function.Supplier<
                    java.util.List<com.hbm.inventory.recipes.ingredient.CountIngredient>>
            REPAIR_MATERIALS =
                    com.google.common.base.Suppliers.memoize(
                            () -> java.util.List.of(
                                    com.hbm.inventory.recipes.ingredient.CountIngredient.of(com.hbm.inventory.OreDictManager.STEEL.plateWelded(), 6),
                                    com.hbm.inventory.recipes.ingredient.CountIngredient.of(com.hbm.inventory.OreDictManager.STEEL.pipe(), 8),
                                    com.hbm.inventory.recipes.ingredient.CountIngredient.of(com.hbm.inventory.OreDictManager.ANY_RESISTANTALLOY.ingot(), 8),
                                    com.hbm.inventory.recipes.ingredient.CountIngredient.of(com.hbm.inventory.OreDictManager.GOLD.wireDense(), 24),
                                    com.hbm.inventory.recipes.ingredient.CountIngredient.of(com.hbm.items.ModItems.FLYWHEEL_BERYLLIUM.get(), 1)));

    @Override
    public java.util.List<com.hbm.inventory.recipes.ingredient.CountIngredient>
            getRepairMaterials() {
        return REPAIR_MATERIALS.get();
    }

    @Override
    public void repair(net.minecraft.world.entity.player.Player player) {
        com.hbm.blocks.machine.DamageableMachine.setDamaged(level, worldPosition, false);
    }

    @Override
    public void tryExtinguish(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            com.hbm.interfaces.IRepairable.EnumExtinguishType type) {}

    @Override
    public void tickServer() {
        if (isDamaged()) return;
        super.tickServer();
    }

    @Override
    protected void collectImplicitComponents(
            net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (isDamaged()) components.set(com.hbm.items.ModDataComponents.MACHINE_DAMAGED.get(), true);
    }

    @Override
    protected void applyImplicitComponents(com.hbm.backport.DataComponentGetter components) {
        super.applyImplicitComponents(components);
        if (level != null
                && Boolean.TRUE.equals(
                        components.get(com.hbm.items.ModDataComponents.MACHINE_DAMAGED.get())))
            com.hbm.blocks.machine.DamageableMachine.setDamaged(level, worldPosition, true);
    }
}
