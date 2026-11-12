#version 150

// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors): shaders/crescent.frag in
// 1.21.1 core form. The body alpha mask is left out (NTM:Space never turns it on for the sky);
// ColorModulator's alpha fades the shadow with the body it covers.

uniform vec4 ColorModulator;
uniform float Phase;
uniform float Offset;

in vec2 texCoord0;

out vec4 fragColor;

#define PI 3.1415926538

vec2 quantize(vec2 inp, vec2 period) {
    return floor(inp / period) * period;
}

void main() {
    vec2 movingUV = texCoord0 + vec2(Offset, 0.0);

    vec2 fragCoord = quantize(movingUV, vec2(0.0625, 0.0625)) - vec2(Offset, 0.0);
    vec2 uv = (2.25 * fragCoord - 1.1);
    vec2 suv = (2.0 * fragCoord - 1.0);

    vec3 light = vec3(sin(Phase * PI), 0.0, cos(Phase * PI));

    vec3 n = vec3(uv, sqrt(1.0 - clamp(dot(uv, uv), 0.0, 1.0)));
    float brightness = dot(n, light);

    // when nearly new moon, ring glow
    brightness = max(brightness, (abs(Phase) - 0.7) * clamp(dot(suv, suv), 0.0, 1.0));

    // become full square when nearing full illumination
    if (abs(Phase) < 0.5) {
        if (Phase < 0.0) {
            brightness = Phase * 4.0 + 2.0 - uv.x;
        } else {
            brightness = -Phase * 4.0 + 2.0 + uv.x;
        }
    }

    brightness = max(brightness, 0.05);

    fragColor = vec4(0.0, 0.0, 0.0, (1.0 - brightness) * ColorModulator.a);
}
