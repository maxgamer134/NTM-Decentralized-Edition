// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.client.render.MissilePronter;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.space.rocket.RocketStruct;
import com.hbm.space.rocket.RocketStruct.RocketStage;
import com.hbm.space.rocket.SpaceRocketParts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

/**
 * Draws a rocket as NTM:Space did: each stage's fuselages stacked over its thrusters, clustered
 * around the axis when it has several, legs out on the bottom stage when deployed, the capsule on
 * top. Spent stages fall away while decoupling. NTM:Space also clipped an interstage shroud over
 * upper thrusters; that one isn't drawn.
 */
public final class RocketPronter {

    private RocketPronter() {}

    private static final ResourceLocation LEGS_DEPLOYED = Library.id("models/missile_parts/rp_s_20_leggy_deployed.obj");
    private static final Map<ResourceLocation, HFRWavefrontObject> MESHES = new HashMap<>();

    private static HFRWavefrontObject mesh(ResourceLocation id) {
        return MESHES.computeIfAbsent(id, MissilePronter::partMesh);
    }

    private static void draw(ItemCustomMissilePart part, boolean deployed, PoseStack pose, SubmitNodeCollector collector, int light) {
        if (part.mesh == null || part.skin == null) return;
        ResourceLocation id = deployed && part == SpaceRocketParts.RP_LEGS_20.get() ? LEGS_DEPLOYED : part.mesh;
        HFRWavefrontObject mesh = mesh(id);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutoutCull(part.skin), (p, buffer) -> mesh.render(p, buffer, light, -1));
    }

    /** A part size's radius, for spacing a cluster: SIZE_20 is 2m. */
    private static double radius(ItemCustomMissilePart.@Nullable PartSize size) {
        if (size == null) return 0;
        String name = size.name();
        return name.startsWith("SIZE_") ? Integer.parseInt(name.substring(5)) / 10D : 0;
    }

    public static void pront(RocketStruct rocket, PoseStack pose, SubmitNodeCollector collector, int light, boolean deployed) {
        pront(rocket, pose, collector, light, deployed, 0, 0);
    }

    public static void pront(RocketStruct rocket, PoseStack pose, SubmitNodeCollector collector, int light, boolean deployed,
            int decoupleTimer, float partialTicks) {
        pront(rocket, pose, collector, light, deployed, decoupleTimer, partialTicks, null);
    }

    /** As above; a drop pod payload with a pose from its flying rocket is drawn animated. */
    public static void pront(RocketStruct rocket, PoseStack pose, SubmitNodeCollector collector, int light, boolean deployed,
            int decoupleTimer, float partialTicks, RenderDropPod.@Nullable Pose pod) {
        pose.pushPose();
        for (RocketStage stage : rocket.stages) {
            int stack = stage.getStack();
            int cluster = stage.getCluster();
            if (deployed && stage.thruster != null && stage.fins != null && stage.fins.height > stage.thruster.height)
                pose.translate(0, stage.fins.height - stage.thruster.height, 0);

            for (int c = 0; c < cluster; c++) {
                pose.pushPose();
                if (decoupleTimer > 0) {
                    float lerp = decoupleTimer + partialTicks;
                    pose.translate(0, -lerp, 0);
                    pose.mulPose(Axis.XP.rotationDegrees(lerp));
                }
                if (c > 0) {
                    pose.mulPose(Axis.YP.rotationDegrees(360F * c / (cluster - 1)));
                    pose.translate(stage.fuselage != null ? radius(stage.fuselage.bottom) : stage.thruster != null ? radius(stage.thruster.top) : 0, 0, 0);
                }
                if (stage.thruster != null) {
                    draw(stage.thruster, deployed, pose, collector, light);
                    pose.translate(0, stage.thruster.height, 0);
                }
                if (stage.fuselage != null) {
                    if (stage.fins != null) draw(stage.fins, deployed, pose, collector, light);
                    for (int s = 0; s < stack; s++) {
                        draw(stage.fuselage, deployed, pose, collector, light);
                        pose.translate(0, stage.fuselage.height, 0);
                    }
                }
                pose.popPose();
            }
            if (stage.thruster != null) pose.translate(0, stage.thruster.height, 0);
            if (stage.fuselage != null) pose.translate(0, stage.fuselage.height * stack, 0);
            // only the bottom stage deploys or decouples
            deployed = false;
            decoupleTimer = 0;
        }
        if (pod != null && rocket.capsule == SpaceRocketParts.RP_POD_20.get()) RenderDropPod.render(pod, pose, collector, light);
        else if (rocket.capsule != null) draw(rocket.capsule, false, pose, collector, light);
        pose.popPose();
    }
}
