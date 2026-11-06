// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

/**
 * backport: OpenComputers: Rebooted support. Main 1.7.10 made each machine an OC SimpleComponent;
 * NeoForge has no optional interfaces, so here an OC Adapter touching a machine (or one of its
 * ports) gets the same peripheral CC gets, under the same component name and with the same
 * methods. Like main's {@code @Callback(direct = true)} getters, snapshot reads run straight on the
 * computer's thread; the methods CC runs on the main thread are OC synced calls, run on the server
 * thread in the next tick. Only linked with OpenComputers loaded.
 *
 * <p>The methods are the peripheral's @LuaFunction ones, read from its class file (so CC need not
 * be installed). OC finds callbacks per class, so each peripheral class gets one small generated
 * environment class whose {@code @Callback} methods forward to {@link Env#call}.
 */
final class OcBridge implements DriverBlock {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LUA_FUNCTION = "Ldan200/computercraft/api/lua/LuaFunction;";
    private static final String LUA_EXCEPTION = "dan200.computercraft.api.lua.LuaException";
    private static final String I_ARGUMENTS = "Ldan200/computercraft/api/lua/IArguments;";
    private static final String OBJECTS = "[Ljava/lang/Object;";
    private static final Pattern OPTIONAL = Pattern.compile("Ljava/util/Optional<L([^;<>]+);>;");
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodType SPREAD =
            MethodType.methodType(Object[].class, Object.class, Object[].class);

    private static final ClassValue<Factory> FACTORIES =
            new ClassValue<>() {
                @Override
                protected Factory computeValue(Class<?> type) {
                    return factory(type);
                }
            };

    private OcBridge() {}

    static void register() {
        Driver.add(new OcBridge());
    }

    // OC hands in the way from the Adapter to the block; CC's side is the block's face it touches.
    @Override
    public boolean worksWith(Level level, BlockPos pos, Direction side) {
        return Peripherals.find(level, pos, side.getOpposite()) != null;
    }

    @Override
    public @Nullable ManagedEnvironment createEnvironment(
            Level level, BlockPos pos, Direction side) {
        SnapshotPeripheral<?> snapshot = Peripherals.find(level, pos, side.getOpposite());
        return snapshot == null ? null : FACTORIES.get(snapshot.getClass()).make(snapshot);
    }

    /** One machine as an OC component; its generated subclass carries the callbacks. */
    abstract static class Env extends AbstractManagedEnvironment {

        private final SnapshotPeripheral<?> snapshot;
        private final Call[] calls;

        protected Env(SnapshotPeripheral<?> snapshot, Object[] calls) {
            this.snapshot = snapshot;
            this.calls = (Call[]) calls;
            setNode(
                    Network.newNode(this, Visibility.Network)
                            .withComponent(snapshot.getType(), Visibility.Network)
                            .create());
        }

        protected final Object[] call(int index, Context context, Arguments args) throws Exception {
            Call call = calls[index];
            Object[] values = new Object[call.params.length];
            for (int i = 0; i < values.length; i++) values[i] = call.params[i].read(args, i);
            try {
                return (Object[]) call.target.invokeExact((Object) snapshot, values);
            } catch (Exception e) {
                // CC's errors (thrown by ComputerError with CC loaded) read as plain OC errors
                if (e.getClass().getName().equals(LUA_EXCEPTION)) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
                throw e;
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }
    }

    @FunctionalInterface
    private interface Param {
        Object read(Arguments args, int index);
    }

    private record Call(String name, boolean direct, MethodHandle target, Param[] params) {}

    private record Factory(MethodHandle make, Call[] calls) {

        ManagedEnvironment make(SnapshotPeripheral<?> snapshot) {
            try {
                return (ManagedEnvironment) make.invoke(snapshot, (Object[]) calls);
            } catch (Throwable t) {
                throw new IllegalStateException("cannot make the OC component of " + snapshot, t);
            }
        }
    }

    private record Found(
            String method, String desc, @Nullable String signature, List<String> names,
            boolean mainThread) {}

    private static Factory factory(Class<?> type) {
        List<Call> calls = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Class<?> c = type; c != null && c != SnapshotPeripheral.class; c = c.getSuperclass()) {
            for (Found found : read(c)) {
                if (!seen.add(found.method + found.desc)) continue; // overridden lower down
                Call bound = bind(c, found);
                if (bound == null) {
                    LOGGER.warn("OpenComputers: {}.{} has no OC form", c.getName(), found.method);
                    continue;
                }
                for (String name : found.names) {
                    if (names.add(name)) {
                        calls.add(new Call(name, bound.direct, bound.target, bound.params));
                    }
                }
            }
        }
        try {
            Class<?> made = LOOKUP.defineClass(generate(type, calls));
            MethodHandle make =
                    LOOKUP.findConstructor(
                            made,
                            MethodType.methodType(
                                    void.class, SnapshotPeripheral.class, Object[].class));
            return new Factory(make, calls.toArray(new Call[0]));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot make the OC component of " + type, e);
        }
    }

    /** The @LuaFunction methods a class declares, straight from its bytes. */
    private static List<Found> read(Class<?> type) {
        byte[] bytes;
        String file = type.getName().substring(type.getName().lastIndexOf('.') + 1) + ".class";
        try (InputStream in = type.getResourceAsStream(file)) {
            if (in == null) return List.of();
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + type, e);
        }
        List<Found> found = new ArrayList<>();
        new ClassReader(bytes)
                .accept(
                        new ClassVisitor(Opcodes.ASM9) {
                            @Override
                            public @Nullable MethodVisitor visitMethod(
                                    int access,
                                    String name,
                                    String desc,
                                    @Nullable String signature,
                                    String @Nullable [] exceptions) {
                                if ((access & Opcodes.ACC_PUBLIC) == 0
                                        || (access & Opcodes.ACC_STATIC) != 0) return null;
                                return new LuaFunctionVisitor(name, desc, signature, found);
                            }
                        },
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return found;
    }

    private static final class LuaFunctionVisitor extends MethodVisitor {

        private final String method;
        private final String desc;
        private final @Nullable String signature;
        private final List<Found> found;
        private final List<String> names = new ArrayList<>();
        private boolean marked;
        private boolean mainThread;

        LuaFunctionVisitor(
                String method, String desc, @Nullable String signature, List<Found> found) {
            super(Opcodes.ASM9);
            this.method = method;
            this.desc = desc;
            this.signature = signature;
            this.found = found;
        }

        @Override
        public @Nullable AnnotationVisitor visitAnnotation(String type, boolean visible) {
            if (!LUA_FUNCTION.equals(type)) return null;
            marked = true;
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override
                public void visit(String key, Object value) {
                    if ("mainThread".equals(key)) mainThread = (Boolean) value;
                }

                @Override
                public @Nullable AnnotationVisitor visitArray(String key) {
                    if (!"value".equals(key)) return null;
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(@Nullable String ignored, Object value) {
                            names.add((String) value);
                        }
                    };
                }
            };
        }

        @Override
        public void visitEnd() {
            if (marked) {
                found.add(
                        new Found(
                                method,
                                desc,
                                signature,
                                names.isEmpty() ? List.of(method) : List.copyOf(names),
                                mainThread));
            }
        }
    }

    private static @Nullable Call bind(Class<?> owner, Found found) {
        if (!Type.getReturnType(found.desc).getDescriptor().equals(OBJECTS)) return null;
        Type[] types = Type.getArgumentTypes(found.desc);
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, LOOKUP);
            if (types.length == 1 && types[0].getDescriptor().equals(I_ARGUMENTS)) {
                // the free-form setters: their ComputerArgs twin reads OC's Arguments directly
                MethodHandle target =
                        lookup.findVirtual(
                                owner,
                                found.method,
                                MethodType.methodType(Object[].class, ComputerArgs.class));
                return new Call(
                        found.method,
                        !found.mainThread,
                        spread(target, 1),
                        new Param[] {(args, i) -> new Args(args)});
            }
            List<String> optionals = new ArrayList<>();
            if (found.signature != null) {
                Matcher m = OPTIONAL.matcher(found.signature);
                while (m.find()) optionals.add(m.group(1));
            }
            Param[] params = new Param[types.length];
            int optional = 0;
            for (int i = 0; i < types.length; i++) {
                String desc = types[i].getDescriptor();
                String inner = null;
                if (desc.equals("Ljava/util/Optional;")) {
                    if (optional >= optionals.size()) return null;
                    inner = optionals.get(optional++);
                }
                Param param = param(desc, inner);
                if (param == null) return null;
                params[i] = param;
            }
            MethodHandle target =
                    lookup.findVirtual(
                            owner,
                            found.method,
                            MethodType.fromMethodDescriptorString(
                                    found.desc, owner.getClassLoader()));
            return new Call(found.method, !found.mainThread, spread(target, types.length), params);
        } catch (ReflectiveOperationException | TypeNotPresentException | LinkageError e) {
            return null;
        }
    }

    private static MethodHandle spread(MethodHandle target, int count) {
        return target.asSpreader(Object[].class, count).asType(SPREAD);
    }

    /** Reads one argument the way CC's typed parameters do, with OC's own checks and errors. */
    private static @Nullable Param param(String desc, @Nullable String inner) {
        return switch (desc) {
            case "I" -> (args, i) -> args.checkInteger(i);
            case "J" -> (args, i) -> args.checkLong(i);
            case "D" -> (args, i) -> args.checkDouble(i);
            case "F" -> (args, i) -> (float) args.checkDouble(i);
            case "Z" -> (args, i) -> args.checkBoolean(i);
            case "Ljava/lang/String;" -> (args, i) -> args.checkString(i);
            case "Ljava/util/Map;" -> (args, i) -> lua(args.checkTable(i));
            case "Ljava/lang/Object;" -> (args, i) -> lua(args.checkAny(i));
            case "Ljava/util/Optional;" -> {
                Param of = inner == null ? null : param(unboxed(inner), null);
                yield of == null
                        ? null
                        : (args, i) ->
                                i >= args.count() || args.checkAny(i) == null
                                        ? Optional.empty()
                                        : Optional.of(of.read(args, i));
            }
            default -> null;
        };
    }

    private static String unboxed(String type) {
        return switch (type) {
            case "java/lang/Integer" -> "I";
            case "java/lang/Long" -> "J";
            case "java/lang/Double" -> "D";
            case "java/lang/Float" -> "F";
            case "java/lang/Boolean" -> "Z";
            default -> "L" + type + ";";
        };
    }

    /** OC hands Lua strings over as bytes; CC's methods expect Java strings. */
    private static @Nullable Object lua(@Nullable Object value) {
        if (value instanceof byte[] bytes) return new String(bytes, StandardCharsets.UTF_8);
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> out = new LinkedHashMap<>();
            map.forEach((k, v) -> out.put(lua(k), lua(v)));
            return out;
        }
        return value;
    }

    /** OC's Arguments seen through the free-form setters' view. */
    private record Args(Arguments args) implements ComputerArgs {

        @Override
        public int count() {
            return args.count();
        }

        @Override
        public @Nullable Object get(int index) {
            return lua(args.checkAny(index));
        }

        @Override
        public int getInt(int index) {
            return args.checkInteger(index);
        }

        @Override
        public String getString(int index) {
            return args.checkString(index);
        }
    }

    /** {@code public Object[] call<i>(Context, Arguments)} per method, each a @Callback forwarding. */
    private static byte[] generate(Class<?> type, List<Call> calls) {
        String env = Type.getInternalName(Env.class);
        String self = env + "$" + type.getName().replace('.', '_');
        String init =
                MethodType.methodType(void.class, SnapshotPeripheral.class, Object[].class)
                        .toMethodDescriptorString();
        String callback =
                MethodType.methodType(Object[].class, Context.class, Arguments.class)
                        .toMethodDescriptorString();
        String forward =
                MethodType.methodType(Object[].class, int.class, Context.class, Arguments.class)
                        .toMethodDescriptorString();
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC,
                self,
                null,
                env,
                null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", init, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, env, "<init>", init, false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        for (int i = 0; i < calls.size(); i++) {
            Call call = calls.get(i);
            mv =
                    cw.visitMethod(
                            Opcodes.ACC_PUBLIC,
                            "call" + i,
                            callback,
                            null,
                            new String[] {"java/lang/Exception"});
            AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(Callback.class), true);
            av.visit("value", call.name);
            av.visit("direct", call.direct);
            av.visitEnd();
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitLdcInsn(i);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, env, "call", forward, false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }
}
