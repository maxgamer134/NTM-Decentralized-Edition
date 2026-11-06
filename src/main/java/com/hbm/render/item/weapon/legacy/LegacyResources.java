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

    public static final HashMap<String, BusAnimation> benelli_anim = AnimationLoader.load(Library.id("models/weapons/animations/benelli.json"));
    public static final HashMap<String, BusAnimation> ks23_anim = AnimationLoader.load(Library.id("models/weapons/animations/ks23.json"));
    public static final HashMap<String, BusAnimation> spas_12_anim = AnimationLoader.load(Library.id("models/weapons/animations/legacy/spas12.json"));
    public static final HashMap<String, BusAnimation> supershotty_anim = AnimationLoader.load(Library.id("models/weapons/animations/supershotty.json"));
    public static final HFRWavefrontObject remington = Meshes.load(Library.id("models/weapons/remington.obj"));
    public static final ResourceLocation remington_tex = Library.id("textures/models/weapons/remington.png");
    public static final ResourceLocation casings_tex = Library.id("textures/particle/casings.png");
    public static final HFRWavefrontObject spas_12 = Meshes.load(Library.id("models/weapons/legacy/spas-12.obj"));
    public static final ResourceLocation spas_12_tex = Library.id("textures/models/weapons/spas-12.png");
    public static final HFRWavefrontObject benelli = Meshes.load(Library.id("models/weapons/benelli_new.obj"));
    public static final ResourceLocation benelli_tex = Library.id("textures/models/weapons/benelli_tex.png");
    public static final HFRWavefrontObject shotty = Meshes.load(Library.id("models/weapons/supershotty.obj"));
    public static final ResourceLocation shotty_tex = Library.id("textures/models/weapons/shotty.png");
    public static final HFRWavefrontObject ks23 = Meshes.load(Library.id("models/weapons/ks23.obj"));
    public static final ResourceLocation ks23_tex = Library.id("textures/models/weapons/ks23.png");
    public static final HFRWavefrontObject sauergun = Meshes.load(Library.id("models/weapons/sauergun.obj"));
    public static final ResourceLocation sauergun_tex = Library.id("textures/models/weapons/sauergun.png");
    public static final HFRWavefrontObject ff_maresleg = Meshes.faceNormals(Library.id("models/weapons/legacy/maresleg.obj"));
    public static final ResourceLocation ff_gun_normal = Library.id("textures/models/weapons/ff/gun_normal.png");
    public static final ResourceLocation ff_wood_red = Library.id("textures/models/weapons/ff/wood_red.png");
    public static final HFRWavefrontObject uboinik = Meshes.faceNormals(Library.id("models/weapons/legacy/uboinik.obj"));
    public static final ResourceLocation uboinik_tex = Library.id("textures/models/ModelUboinik.png");

    public static final HFRWavefrontObject model_uzi = Meshes.faceNormals(Library.id("models/weapons/legacy/uzi.obj"));
    public static final HFRWavefrontObject model_uzibarrel = Meshes.faceNormals(Library.id("models/weapons/legacy/uzibarrel.obj"));
    public static final HFRWavefrontObject model_uzisilencer = Meshes.faceNormals(Library.id("models/weapons/legacy/uzisilencer.obj"));

    public static final HashMap<String, BusAnimation> novac_anim = AnimationLoader.load(Library.id("models/weapons/animations/novac.json"));
    public static final HFRWavefrontObject lilmac = Meshes.load(Library.id("models/weapons/legacy/lilmac.obj"));
    public static final ResourceLocation lilmac_scope_tex = Library.id("textures/models/weapons/lilmac_scope.png");
    public static final ResourceLocation lilmac_tex = Library.id("textures/models/weapons/lilmac.png");
    public static final ResourceLocation blackjack_tex = Library.id("textures/models/weapons/blackjack.png");
    public static final ResourceLocation lent_gun_tex = Library.id("textures/models/weapons/lent_gun.png");
    public static final ResourceLocation lil_pip_tex = Library.id("textures/models/weapons/lil_pip.png");
    public static final HFRWavefrontObject novac = Meshes.load(Library.id("models/weapons/novac.obj"));
    public static final ResourceLocation novac_scope_tex = Library.id("textures/models/weapons/novac_scope.png");
    public static final ResourceLocation novac_tex = Library.id("textures/models/weapons/novac.png");
    public static final ResourceLocation red_key_tex = Library.id("textures/models/weapons/red_key.png");
}
