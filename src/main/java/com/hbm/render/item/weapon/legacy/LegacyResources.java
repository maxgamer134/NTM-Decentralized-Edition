// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.legacy;

import com.hbm.client.model.Meshes;
import com.hbm.lib.Library;
import com.hbm.render.anim.AnimationLoader;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.HashMap;
import net.minecraft.resources.ResourceLocation;

/** backport-fix: BF-161 main 1.0.27_X5000's ResourceManager entries for the pre-rework guns. */
public final class LegacyResources {

    private LegacyResources() {}

    private static HFRWavefrontObject load(String path) {
        return Meshes.load(Library.id(path));
    }

    private static ResourceLocation tex(String path) {
        return Library.id("textures/models/weapons/" + path + ".png");
    }

    // main loads ff_python with plain HFRWavefrontObject (flat), the rest with smoothing
    public static final HFRWavefrontObject ff_python = Meshes.faceNormals(Library.id("models/weapons/python.obj"));
    public static final HFRWavefrontObject ff_nightmare = load("models/weapons/nightmare.obj");
    public static final HFRWavefrontObject nightmare_dark = load("models/weapons/nightmare_dark.obj");
    public static final HFRWavefrontObject cursed_revolver = load("models/weapons/cursed.obj");

    public static final ResourceLocation ff_gold = tex("ff/gold");
    public static final ResourceLocation ff_gun_bright = tex("ff/gun_bright");
    public static final ResourceLocation ff_gun_dark = tex("ff/gun_dark");
    public static final ResourceLocation ff_iron = tex("ff/iron");
    public static final ResourceLocation ff_saturnite = tex("ff/saturnite");
    public static final ResourceLocation ff_schrabidium = tex("ff/schrabidium");
    public static final ResourceLocation ff_wood = tex("ff/wood");
    public static final ResourceLocation ff_cursed = tex("ff/cursed");
    public static final ResourceLocation ff_nightmare_orig_tex = tex("ff/nightmare_orig");
    public static final ResourceLocation ff_nightmare_tex = tex("ff/nightmare");

    public static final HashMap<String, BusAnimation> python_anim =
            AnimationLoader.load(Library.id("models/weapons/animations/python.json"));
    public static final HashMap<String, BusAnimation> cursed_anim =
            AnimationLoader.load(Library.id("models/weapons/animations/cursed.json"));
}
