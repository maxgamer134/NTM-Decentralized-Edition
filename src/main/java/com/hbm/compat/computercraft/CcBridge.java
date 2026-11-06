// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dan200.computercraft.core.methods.ObjectSource;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * backport: the CC side of the peripherals, only linked with CC loaded. Each snapshot gets one
 * stable wrapper; CC finds the machine's @LuaFunction methods on the snapshot through
 * {@link ObjectSource}, so CC sees exactly the methods, names and threads it saw before the
 * snapshots stopped being IPeripherals themselves (they had to, to load without CC for OpenComputers).
 */
public final class CcBridge implements IPeripheral, ObjectSource {

    private final SnapshotPeripheral<?> snapshot;

    private CcBridge(SnapshotPeripheral<?> snapshot) {
        this.snapshot = snapshot;
    }

    static void declare() {
        Peripherals.declare(PeripheralCapability.get(), CcBridge::of);
    }

    private static IPeripheral of(SnapshotPeripheral<?> snapshot) {
        Object known = snapshot.ccAdapter;
        if (known == null) snapshot.ccAdapter = known = new CcBridge(snapshot);
        return (IPeripheral) known;
    }

    @Override
    public String getType() {
        return snapshot.getType();
    }

    @Override
    public Object getTarget() {
        return snapshot.getTarget();
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof CcBridge bridge && bridge.snapshot.sameAs(snapshot);
    }

    @Override
    public Iterable<Object> getExtra() {
        return List.of(snapshot);
    }

    public static ComputerArgs args(IArguments args) {
        return new Args(args);
    }

    /** A LuaException, thrown unchecked so CC-free callers need not declare it. */
    static RuntimeException error(String message) {
        throw sneaky(new LuaException(message));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable thrown) throws T {
        throw (T) thrown;
    }

    private record Args(IArguments args) implements ComputerArgs {

        @Override
        public int count() {
            return args.count();
        }

        @Override
        public @Nullable Object get(int index) {
            try {
                return args.get(index);
            } catch (LuaException e) {
                throw sneaky(e);
            }
        }

        @Override
        public int getInt(int index) {
            try {
                return args.getInt(index);
            } catch (LuaException e) {
                throw sneaky(e);
            }
        }

        @Override
        public String getString(int index) {
            try {
                return args.getString(index);
            } catch (LuaException e) {
                throw sneaky(e);
            }
        }
    }
}
