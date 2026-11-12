// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.logic;

import com.hbm.NuclearTech;
import com.hbm.backport.SubLevelPush;
import com.hbm.backport.SubLevelShapes;
import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.explosion.ExplosionBalefire;
import com.hbm.explosion.ExplosionNukeGeneric;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;

public class EntityBalefire extends EntityExplosionChunkloading {

    public int age = 0;
    public int destructionRange = 0;
    public ExplosionBalefire exp;
    public int speed = 1;
    public boolean did = false;
    /** NTM:Space's antimatter bomb: its own crater, set only while Space is on. */
    public boolean antimatter = false;

    public EntityBalefire antimatter() {
        this.antimatter = true;
        return this;
    }

    private ExplosionBalefire makeExplosion() {
        return antimatter
                ? new com.hbm.space.explosion.ExplosionAntimatter((int) getX(), (int) getY(), (int) getZ(), level(), destructionRange)
                : new ExplosionBalefire((int) getX(), (int) getY(), (int) getZ(), level(), destructionRange);
    }

    public EntityBalefire(EntityType<? extends EntityBalefire> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.did) {
            if (!level().isClientSide() && NeoForgeHbmConfig.runtime().extendedLogging())
                NuclearTech.LOGGER.info(
                        "[NUKE] Initialized BF explosion at {} / {} / {} with strength {}!",
                        getX(),
                        getY(),
                        getZ(),
                        destructionRange);

            exp = makeExplosion();
            if (level() instanceof ServerLevel server) burnBuilds(server);

            this.did = true;
        }

        speed += 1;

        boolean flag = false;
        for (int i = 0; i < this.speed; i++) {
            flag = exp.update();

            if (flag) {
                this.discard();
            }
        }

        // NTM:Space's antimatter bomb also floods everything in twice its range with radiation
        if (antimatter && this.tickCount < 10)
            com.hbm.util.ContaminationUtil.radiate(level(), getX(), getY(), getZ(), destructionRange * 2D,
                    40000D * destructionRange, com.hbm.util.ContaminationUtil.ContaminationType.CREATIVE);

        if (!flag) {
            ExplosionNukeGeneric.dealDamage(
                    level(), getX(), getY(), getZ(), this.destructionRange * 2);
        }

        age++;
    }

    /**
     * Sable compat (not in ntm-next): ExplosionBalefire eats world columns from the surface down, so a
     * physics build over the crater was left hanging in the air. Its blocks inside the crater's
     * cylinder go at once - from the crater floor (ExplosionBalefire's deepest cut below the blast)
     * up to the blast radius above it - and the blast throws what is left.
     */
    private void burnBuilds(ServerLevel server) {
        int r = destructionRange;
        double x = (int) getX() + 0.5, y = (int) getY(), z = (int) getZ() + 0.5;
        double floor = y - (10 + r * 0.25) - 2.0103;
        double top = y + r;
        Vec3 center = new Vec3(x, y, z);
        SubLevelShapes.clear(
                server, center, r * 1.5,
                (wx, wy, wz) -> wy >= floor && wy <= top && (wx - x) * (wx - x) + (wz - z) * (wz - z) < (double) r * r,
                null);
        SubLevelPush.blast(server, center, r * 2, r * 0x1p-1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        age = input.getIntOr("age", 0);
        destructionRange = input.getIntOr("destructionRange", 0);
        speed = input.getIntOr("speed", 1);
        did = input.getBooleanOr("did", false);
        antimatter = input.getBooleanOr("antimatter", false);

        exp = makeExplosion();
        exp.readFromNbt(input, "exp_");

        this.did = true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("age", age);
        output.putInt("destructionRange", destructionRange);
        output.putInt("speed", speed);
        output.putBoolean("did", did);
        if (antimatter) output.putBoolean("antimatter", true);

        if (exp != null) exp.saveToNbt(output, "exp_");
    }
}
