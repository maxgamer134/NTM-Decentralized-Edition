// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.EntityRenderState;
import com.hbm.backport.client.core.EntityRenderer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.space.rocket.EntityRideableRocket;
import com.hbm.space.rocket.EntityRideableRocket.RocketState;
import com.hbm.space.rocket.RocketStruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jspecify.annotations.Nullable;

/** Draws a rideable rocket from its parts, pitched along its ascent, legs out once landing. */
public class RenderRideableRocket extends EntityRenderer<EntityRideableRocket, RenderRideableRocket.State>
        implements ConcurrentRenderStateExtraction {

    public RenderRideableRocket(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public boolean shouldRender(EntityRideableRocket entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityRideableRocket entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.rocket = entity.getRocket();
        state.pitch = entity.getViewXRot(partialTicks);
        state.nose = entity.nose(state.pitch);
        RocketState s = entity.getState();
        state.deployed = s == RocketState.LANDING || s == RocketState.LANDED || s == RocketState.AWAITING
                || s == RocketState.TIPPING || s == RocketState.NEEDSFUEL;
        state.decoupleTimer = entity.decoupleTimer;
        state.partialTicks = partialTicks;
        state.pod = entity.isReusable() ? RenderDropPod.extract(entity, partialTicks) : null;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.rocket != null) {
            pose.pushPose();
            // the rocket faces east and leans over that way
            pose.mulPose(com.hbm.backport.LaunchAxis.rotation(state.nose));
            RocketPronter.pront(state.rocket, pose, collector, state.lightCoords, state.deployed, state.decoupleTimer, state.partialTicks, state.pod);
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        @Nullable RocketStruct rocket;
        float pitch;
        net.minecraft.world.phys.Vec3 nose = com.hbm.backport.LaunchAxis.UP;
        boolean deployed;
        int decoupleTimer;
        float partialTicks;
        RenderDropPod.@Nullable Pose pod;
    }
}
