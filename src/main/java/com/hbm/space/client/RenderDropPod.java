// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.client.render.MissilePronter;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.space.CelestialBody;
import com.hbm.space.rocket.EntityRideableRocket;
import com.hbm.space.rocket.EntityRideableRocket.RocketState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * The drop pod payload of a flying rocket, animated as NTM:Space did: its door swings open once
 * landed and shut again before launch, four airbrakes fan out while it falls fast and fold away
 * as it slows, and its legs drop half a block out of the hull except on the way up. In orbit the
 * whole mesh, heat shield included, is drawn still.
 */
public final class RenderDropPod {

    private RenderDropPod() {}

    private static final ResourceLocation MESH = Library.id("models/missile_parts/rp_drop_pod.obj");
    private static final ResourceLocation SKIN = Library.id("textures/models/missile_parts/warheads/rp_drop_pod.png");

    private static @Nullable HFRWavefrontObject mesh;
    private static int body, door, airbrake, legs;

    /** Per rocket: whether the brakes are out, and when they began folding away. */
    private static final Map<EntityRideableRocket, long[]> BRAKES = Collections.synchronizedMap(new WeakHashMap<>());

    /** How a pod is posed this frame, taken from its rocket at extraction. */
    public record Pose(boolean inOrbit, float door, float brakes, float legs) {}

    private static HFRWavefrontObject mesh() {
        if (mesh == null) {
            HFRWavefrontObject m = MissilePronter.partMesh(MESH);
            body = m.partId("DropPod");
            door = m.partId("Door");
            airbrake = m.partId("Airbrake0");
            legs = m.partId("Legs");
            mesh = m;
        }
        return mesh;
    }

    public static Pose extract(EntityRideableRocket rocket, float partialTicks) {
        if (CelestialBody.inOrbit(rocket.level())) return new Pose(true, 0, 0, 0);

        RocketState state = rocket.getState();
        float lerpTimer = rocket.getStateTimer() + partialTicks;

        float doorRotation = 0;
        if (state == RocketState.LANDED) doorRotation = Mth.clamp(lerpTimer * 2, 0, 90);
        else if (state == RocketState.AWAITING) doorRotation = Mth.clamp(90 - lerpTimer * 2, 0, 90);

        float brakeRotation = 0;
        if (state == RocketState.LANDING) {
            // NTM:Space read the synced motion; the client sees the fall as its position change
            double motionY = rocket.getY() - rocket.yOld;
            long[] brakes = BRAKES.computeIfAbsent(rocket, r -> new long[] {0, 0});
            if (motionY > -0.4) {
                if (brakes[0] != 0) {
                    brakes[0] = 0;
                    brakes[1] = System.currentTimeMillis();
                }
            } else {
                brakes[0] = 1;
            }
            if (brakes[0] == 0) {
                float t = Mth.clamp((System.currentTimeMillis() - brakes[1]) / 1000F, 0, 1);
                brakeRotation = (1 - t) * 65;
            } else {
                brakeRotation = 65;
            }
        }

        float legExtension = state == RocketState.LAUNCHING ? 0 : 1;
        return new Pose(false, doorRotation, brakeRotation, legExtension);
    }

    public static void render(Pose state, PoseStack pose, SubmitNodeCollector collector, int light) {
        HFRWavefrontObject m = mesh();
        if (state.inOrbit()) {
            collector.submitCustomGeometry(pose, RenderTypes.entityCutoutCull(SKIN), (p, buffer) -> m.render(p, buffer, light, -1));
            return;
        }

        part(m, body, pose, collector, light);

        pose.pushPose();
        pose.translate(0.69291F, 2.8333F, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(state.door()));
        pose.translate(-0.69291F, -2.8333F, 0);
        part(m, door, pose, collector, light);
        pose.popPose();

        for (int i = 0; i < 4; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 90 - 45));
            pose.translate(0.46194, 3.5, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(state.brakes()));
            pose.translate(-0.46194, -3.5, 0);
            pose.mulPose(Axis.YP.rotationDegrees(45));
            part(m, airbrake, pose, collector, light);
            pose.popPose();
        }

        pose.pushPose();
        pose.translate(0, -state.legs() * 0.5, 0);
        part(m, legs, pose, collector, light);
        pose.popPose();
    }

    private static void part(HFRWavefrontObject m, int id, PoseStack pose, SubmitNodeCollector collector, int light) {
        collector.submitCustomGeometry(pose, RenderTypes.entityCutoutCull(SKIN), (p, buffer) -> m.renderPart(p, buffer, light, -1, id));
    }
}
