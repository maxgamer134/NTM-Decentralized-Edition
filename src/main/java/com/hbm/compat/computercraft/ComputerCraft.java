// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

// The computer gate: this class has no CC or OC imports. Peripherals and the peripheral classes are
// CC-free at runtime (their @LuaFunction marks are only read, by CC or by OcBridge's scan), so they
// run for either mod; CcBridge (CC) and OcBridge (OpenComputers) are only linked when theirs is here.
public final class ComputerCraft {

    public static final String MOD_ID = "computercraft";
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    /** Either computer mod is here, so the peripheral roster and its snapshots are needed. */
    static final boolean ANY = LOADED || OpenComputers.LOADED;

    private ComputerCraft() {}

    public static void init() {
        if (ANY) Peripherals.init();
    }

    public static void declare(List<RegistryHandle<? extends Block>> roster) {
        if (LOADED) CcBridge.declare();
    }

    @SafeVarargs
    public static <BE extends BlockEntity> void addon(
            Class<BE> type,
            Function<? super BE, ? extends SnapshotPeripheral<?>> factory,
            boolean proxied,
            RegistryHandle<? extends Block>... blocks) {
        NeoForgeRegistrar.assertOpen("ComputerCraft.addon");
        if (ANY) Peripherals.addon(type, factory, proxied, List.of(blocks));
    }

    @SafeVarargs
    public static void extend(
            Class<? extends BlockEntity> type, RegistryHandle<? extends Block>... blocks) {
        NeoForgeRegistrar.assertOpen("ComputerCraft.extend");
        if (ANY) Peripherals.extend(type, List.of(blocks));
    }
}
