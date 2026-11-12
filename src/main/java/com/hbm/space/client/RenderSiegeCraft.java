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
import com.hbm.render.util.BeamPronter;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.space.SpaceInvaders;
import com.hbm.space.SpaceSiege;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Random;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The siege craft: the spinning saucer skinned by tier, its coils glowing from green to red as it is hurt, sparks
 * arcing off the rim and the lock-on beam. Space's balefire glint over the hull while firing is left out.
 */
public class RenderSiegeCraft extends EntityRenderer<SpaceSiege.SiegeCraft, RenderSiegeCraft.State> implements ConcurrentRenderStateExtraction {

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[SpaceInvaders.SiegeTier.getLength()];
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private HFRWavefrontObject mesh;

    static {
        for (int i = 0; i < TEXTURES.length; i++)
            TEXTURES[i] = Library.id("textures/entity/siege_craft_" + SpaceInvaders.SiegeTier.tiers[i].name + ".png");
    }

    public RenderSiegeCraft(EntityRendererProvider.Context context) {
        super(context);
        this.shadowStrength = 0F;
    }

    public static final class State extends EntityRenderState {
        float spin, tilt, health;
        boolean alive, beam;
        int tier, age;
        Vec3 delta = Vec3.ZERO;
    }

    @Override
    protected boolean affectedByCulling(SpaceSiege.SiegeCraft entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpaceSiege.SiegeCraft entity, State s, float pt) {
        super.extractRenderState(entity, s, pt);
        s.spin = (float) ((entity.tickCount + pt) * 5 % 360D);
        s.alive = entity.isAlive();
        s.tilt = entity.deathTime + pt;
        s.health = entity.getHealth() / entity.getMaxHealth();
        s.tier = entity.getTier().id;
        s.age = entity.tickCount;
        s.beam = entity.getBeam();
        s.delta = s.beam ? entity.getLockon().subtract(entity.getX(), entity.getY(), entity.getZ()) : Vec3.ZERO;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (mesh == null) mesh = Meshes.load(Library.id("models/mobs/siege_ufo.obj"));
        HFRWavefrontObject m = mesh;
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0, 0.5, 0);
        ps.pushPose();
        RenderSpaceFish.rot(ps, s.spin, 0, 1, 0);
        if (!s.alive) RenderSpaceFish.rot(ps, s.tilt * 5, 1, 0, 1);

        int ufo = m.partId("UFO"), coils = m.partId("Coils");
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(TEXTURES[Math.min(s.tier, TEXTURES.length - 1)]),
                (pose, buf) -> m.renderPart(pose, buf, light, -1, ufo));
        float h = Math.max(0F, Math.min(1F, s.health));
        int coil = 0xFF000000 | ((int) ((1F - h) * 255F) << 16) | ((int) (h * 255F) << 8);
        col.submitCustomGeometry(ps, RenderTypes.entityCutoutNoCull(WHITE),
                (pose, buf) -> m.renderPart(pose, buf, LightTexture.FULL_BRIGHT, coil, coils));

        Random rand = new Random(s.age / 4);
        int seed = (int) (System.currentTimeMillis() % 1000) / 50;
        for (int i = 0; i < 8; i++) {
            RenderSpaceFish.rot(ps, 45, 0, 1, 0);
            if (rand.nextInt(5) == 0 || s.beam) {
                ps.pushPose();
                ps.translate(4, 0, 0);
                BeamPronter.prontBeam(ps, col, new Vec3(-1.125, 0, 2.875), EnumWaveType.RANDOM, EnumBeamType.LINE, 0x80d0ff, 0xffffff,
                        seed, 15, 0.125F, 1, 0);
                ps.popPose();
            }
        }
        ps.popPose();

        if (s.beam) {
            double length = s.delta.length();
            BeamPronter.prontBeam(ps, col, s.delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x101020, 0x101020, s.age / 6,
                    (int) (length / 2 + 1), 0.1F, 4, 0.25F);
            BeamPronter.prontBeam(ps, col, s.delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, s.age / 2,
                    (int) (length / 2 + 1), 0.7F, 2, 0.0625F);
            BeamPronter.prontBeam(ps, col, s.delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, s.age / 4,
                    (int) (length / 2 + 1), 0.7F, 2, 0.0625F);
        }
        ps.popPose();
        super.submit(s, ps, col, camera);
    }
}
