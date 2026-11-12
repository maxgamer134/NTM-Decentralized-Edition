#version 150

// SPDX-FileCopyrightText: 2026 maxgamer134
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from NTM:Space (JameH2, MellowArpeggiation and contributors): shaders/default.vert, which
// hands the fragment shader the untransformed vertex position.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 vPosition;

void main() {
    vPosition = Position;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
