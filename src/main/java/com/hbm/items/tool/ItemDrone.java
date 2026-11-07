// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.client.ModifierKeys;
import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.hbm.backport.TooltipDisplay;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import com.hbm.backport.compat.ItemCompat;

public class ItemDrone extends ItemCompat {

    public final EnumDroneType type;

    public ItemDrone(Properties props, EnumDroneType type) {
        super(props);
        this.type = type;
    }

    @Override
    public InteractionResult backport$useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;

        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (type != EnumDroneType.REQUEST) {
            EntityDeliveryDrone drone = new EntityDeliveryDrone(level);
            drone.setExpress(
                    type == EnumDroneType.PATROL_EXPRESS
                            || type == EnumDroneType.PATROL_EXPRESS_CHUNKLOADING);
            drone.setChunkLoading(
                    type == EnumDroneType.PATROL_CHUNKLOADING
                            || type == EnumDroneType.PATROL_EXPRESS_CHUNKLOADING);
            BlockPos pos = context.getClickedPos();
            drone.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            level.addFreshEntity(drone);
        }
        context.getItemInHand().shrink(1);

        return InteractionResult.SUCCESS;
    }

    // backport-fix: BF-136 main's drones show their yellow .desc lines while shift is held
    // and the "Hold <LSHIFT>" hint otherwise; ours had no tooltip at all
    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (!ModifierKeys.detailsHeld()) {
            adder.accept(TooltipStyle.holdDetails());
            return;
        }
        for (Component line :
                TooltipStyle.lines(
                        Component.translatableWithFallback(
                                "item.hbm.drone." + type.id + ".desc", ""))) {
            adder.accept(line.copy().withStyle(ChatFormatting.YELLOW));
        }
    }

    public enum EnumDroneType {
        PATROL,
        PATROL_CHUNKLOADING,
        PATROL_EXPRESS,
        PATROL_EXPRESS_CHUNKLOADING,
        REQUEST;

        public static final EnumDroneType[] VALUES = values();
        public final String id = name().toLowerCase(Locale.ROOT);
    }
}
