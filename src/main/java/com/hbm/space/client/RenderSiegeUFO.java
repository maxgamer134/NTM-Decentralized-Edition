// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors)

package com.hbm.space.client;

import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.EntityRenderState;
import com.hbm.backport.client.core.EntityRenderer;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.client.rendertype.RenderTypes;
import com.hbm.client.model.Meshes;
import com.hbm.lib.Library;
import com.hbm.lib.crankshaft.ConcurrentRenderStateExtraction;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.space.SpaceInvaders;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The mini UFO, spinning, skinned by its siege tier; it wobbles when hit and tumbles when shot down. */
public class RenderSiegeUFO extends EntityRenderer<SpaceInvaders.SiegeUFO, RenderSiegeUFO.State> implements ConcurrentRenderStateExtraction {

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[SpaceInvaders.SiegeTier.getLength()];
    private HFRWavefrontObject mesh;

    static {
        for (int i = 0; i < TEXTURES.length; i++)
            TEXTURES[i] = Library.id("textures/entity/ufo_siege_" + SpaceInvaders.SiegeTier.tiers[i].name + ".png");
    }

    public RenderSiegeUFO(EntityRendererProvider.Context context) {
        super(context);
    }

    public static final class State extends EntityRenderState {
        float spin, tilt, wobble;
        boolean alive;
        int tier;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceInvaders.SiegeUFO entity, State s, float pt) {
        super.extractRenderState(entity, s, pt);
        s.spin = (float) ((entity.tickCount + pt) * 5 % 360D);
        s.alive = entity.isAlive();
        s.tilt = entity.deathTime + pt;
        s.wobble = entity.invulnerableTime > 0 ? (float) (Math.sin(System.currentTimeMillis() * 0.01D) * (entity.invulnerableTime - pt)) : 0F;
        s.tier = entity.getTier().id;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (mesh == null) mesh = Meshes.load(Library.id("models/mobs/mini_ufo.obj"));
        HFRWavefrontObject m = mesh;
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0, 0.25, 0);
        RenderSpaceFish.rot(ps, s.spin, 0, 1, 0);
        if (!s.alive) RenderSpaceFish.rot(ps, s.tilt * 5, 1, 0, 1);
        else if (s.wobble != 0) RenderSpaceFish.rot(ps, s.wobble, 1, 0, 0);
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(TEXTURES[Math.min(s.tier, TEXTURES.length - 1)]),
                (pose, buf) -> m.render(pose, buf, light, -1));
        ps.popPose();
        super.submit(s, ps, col, camera);
    }
}
