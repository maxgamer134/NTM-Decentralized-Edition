#version 150

// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors): WeatherProviderCelestial's
// texture environment (COMBINE: RGB replaced by the vertex colour, alpha = texture alpha times vertex
// alpha) on top of 1.21.1's particle fragment shader, so rain and snow take the body's liquid colour
// exactly instead of being multiplied into the grey rain texture.

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = vec4(vertexColor.rgb, texture(Sampler0, texCoord0).a * vertexColor.a) * ColorModulator;
    if (color.a <= 0.1) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
