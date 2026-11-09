// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.EntityRenderState;
import com.hbm.backport.client.core.EntityRenderer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.entity.missile.EntityRocketLambda;
import com.hbm.items.weapon.sedna.factory.LegoClient;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class RenderLambdaRocket extends EntityRenderer<EntityRocketLambda, RenderLambdaRocket.State>
        implements ConcurrentRenderStateExtraction {

    public RenderLambdaRocket(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntityRocketLambda entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            EntityRocketLambda entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityRocketLambda entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.flicker = 0.8F + entity.getRandom().nextFloat() * 0.4F;
        state.dir = entity.flightDirection(partialTicks);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        // backport-fix: BF-113 nose along the flight direction (main: always upright)
        boolean tilted = !com.hbm.backport.LaunchAxis.upright(state.dir);
        if (tilted) {
            poseStack.pushPose();
            poseStack.mulPose(com.hbm.backport.LaunchAxis.rotation(state.dir));
        }
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutoutCull(ResourceManager.lambda_rocket_tex),
                (pose, buffer) -> ResourceManager.lambda_rocket.render(pose, buffer, light, -1));
        if (tilted) poseStack.popPose();

        if (state.age >= 2) {
            // move the flare one block towards the player so it doesn't clip into the model
            Vec3 toCamera = camera.pos.subtract(state.x, state.y, state.z).normalize();
            poseStack.pushPose();
            poseStack.translate(toCamera.x, toCamera.y, toCamera.z);
            poseStack.scale(2F, 2F, 2F);
            float scale = (float) Math.min(5, (state.age - 2) * 0.5) * state.flicker;
            LegoClient.submitFlareSprite(
                    poseStack, collector, camera.orientation, scale, 1F, 0.75F, 0.5F, 0.5F, 0.75F);
            poseStack.popPose();
        }

        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        float age;
        float flicker;
        Vec3 dir = com.hbm.backport.LaunchAxis.UP;
    }
}
