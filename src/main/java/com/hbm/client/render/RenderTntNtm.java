// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.client.render;

import com.hbm.entity.item.EntityTntNtm;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.PrimedTnt;

/** The vanilla primed-TNT look; with NTM:Space's changes on, NTM's charges tumble about their middle. */
public class RenderTntNtm extends TntRenderer {

    public RenderTntNtm(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PrimedTnt entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light) {
        if (!(entity instanceof EntityTntNtm tnt) || !com.hbm.space.SpaceToggle.changes()) {
            super.render(entity, yaw, partialTicks, pose, buffer, light);
            return;
        }
        pose.pushPose();
        pose.translate(0F, 0.5F, 0F);
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, tnt.prevSpinYaw, tnt.spinYaw)));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, tnt.prevSpinPitch, tnt.spinPitch)));
        pose.translate(0F, -0.5F, 0F);
        super.render(entity, yaw, partialTicks, pose, buffer, light);
        pose.popPose();
    }
}
