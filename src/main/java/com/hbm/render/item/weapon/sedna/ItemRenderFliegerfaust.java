// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.HbmAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.hbm.backport.client.core.SubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import com.hbm.backport.client.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderFliegerfaust extends ItemRenderWeaponBase {
    private final RenderType body = RenderTypes.entityCutout(ResourceManager.fliegerfaust_tex);

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack,
                pose,
                -2.75F * offset,
                -2F * offset,
                -1.5F * offset,
                -0.9375,
                -7.75 / 8D,
                -2.5);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        float aimingProgress =
                Mth.lerp(interp, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);
        double scale = 1.25D;
        pose.scale((float) scale, (float) scale, (float) scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
        double[] drum = HbmAnimations.getRelevantTransformation("DRUM");
        double[] drumrot = HbmAnimations.getRelevantTransformation("DRUMROT");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] unloading = HbmAnimations.getRelevantTransformation("UNLOADING");

        // the drum stays full until the unloading flag of a reload drops it to what is left
        int rocketNum =
                unloading[0] > 0
                        ? gun.getConfig(stack, 0)
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .getAmount(stack, null)
                        : 9;

        pose.mulPose(Axis.ZP.rotationDegrees(aimingProgress * -45));

        pose.translate(0, 0, 1);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 0, -1);

        pose.translate(0, -6, -1);
        pose.mulPose(Axis.XP.rotationDegrees((float) reload[0]));
        pose.translate(0, 6, 1);

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body, ResourceManager.fliegerfaust, Parts.LAUNCHER, light);

        pose.pushPose();
        pose.translate(0, 0, 2);
        pose.translate(drum[0], drum[1], drum[2]);
        pose.translate(0, 0, -8.5);
        pose.mulPose(Axis.XP.rotationDegrees((float) drumrot[0]));
        pose.translate(0, 0, 8.5);
        submitPart(collector, pose, body, ResourceManager.fliegerfaust, Parts.DRUM, light);
        for (int i = 0; i < Math.min(rocketNum, Parts.ROCKETS.length); i++)
            submitPart(collector, pose, body, ResourceManager.fliegerfaust, Parts.ROCKETS[i], light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.75F, 0.75F, 0.75F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -2.75F * offset, -2F * offset, -1.5F * offset).scale(1.25F);
        body.part(m, ResourceManager.fliegerfaust, Parts.LAUNCHER, ResourceManager.fliegerfaust_tex);
        Matrix4f drum = new Matrix4f(m).translate(0F, 0F, 2F);
        body.part(drum, ResourceManager.fliegerfaust, Parts.DRUM, ResourceManager.fliegerfaust_tex);
        for (int rocket : Parts.ROCKETS)
            body.part(drum, ResourceManager.fliegerfaust, rocket, ResourceManager.fliegerfaust_tex);
    }

    private static final class Parts {
        static final int LAUNCHER = ResourceManager.fliegerfaust.partId("Launcher");
        static final int DRUM = ResourceManager.fliegerfaust.partId("Drum");
        static final int[] ROCKETS = rockets();

        private static int[] rockets() {
            int[] ids = new int[9];
            for (int i = 0; i < ids.length; i++)
                ids[i] = ResourceManager.fliegerfaust.partId("Rocket" + (i + 1));
            return ids;
        }
    }
}
