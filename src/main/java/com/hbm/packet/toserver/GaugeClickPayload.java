// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A click on a machine's tank gauge with a fluid container on the cursor (a Mekanism gauge
 * dropper, a bucket, an NTM canister): left takes fluid out of the tank into the container, right
 * pours the container into the tank. It goes through the tank's own slot logic, so a gauge takes
 * exactly what that machine's fluid slots would.
 */
public final class GaugeClickPayload extends ThreadedPayload {

    public static final Type<GaugeClickPayload> TYPE = new Type<>(Library.id("gauge_click"));
    public static final StreamCodec<ByteBuf, GaugeClickPayload> STREAM_CODEC = streamCodec(GaugeClickPayload::decode);

    private final int containerId;
    private final int tank;
    private final boolean fillTank;

    public GaugeClickPayload(int containerId, int tank, boolean fillTank) {
        this.containerId = containerId;
        this.tank = tank;
        this.fillTank = fillTank;
    }

    private static GaugeClickPayload decode(ByteBuf buf) {
        return new GaugeClickPayload(buf.readInt(), buf.readShort(), buf.readBoolean());
    }

    public static void handleServer(GaugeClickPayload payload, IPayloadHandlerContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        if (!(sp.containerMenu instanceof BlockEntityMenu<?> menu) || menu.containerId != payload.containerId) return;
        if (!menu.stillValid(sp)) return;
        FluidTankAccess tanks = FluidTankAccess.of(menu.blockEntity());
        if (payload.tank < 0 || payload.tank >= tanks.tankCount()) return;
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty() || !FluidTankNTM.isFluidContainer(carried)) return;
        FluidTankNTM tank = tanks.tank(payload.tank);

        // one container at a time, like a machine slot pair: in, then whatever comes out
        NonNullList<ItemStack> slots = NonNullList.withSize(2, ItemStack.EMPTY);
        slots.set(0, carried.copyWithCount(1));
        boolean moved = payload.fillTank ? tank.loadTank(0, 1, slots) : tank.unloadTank(0, 1, slots);
        if (!moved) return;
        ItemStack result = slots.get(1).isEmpty() ? slots.get(0) : slots.get(1);
        ItemStack rest = carried.copyWithCount(carried.getCount() - 1);
        if (rest.isEmpty()) {
            menu.setCarried(result);
        } else {
            menu.setCarried(rest);
            if (!result.isEmpty() && !sp.getInventory().add(result)) sp.drop(result, false);
        }
        menu.blockEntity().setChanged();
        menu.broadcastChanges();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(containerId);
        buf.writeShort(tank);
        buf.writeBoolean(fillTank);
    }

    @Override
    public @NotNull Type<GaugeClickPayload> type() {
        return TYPE;
    }
}
