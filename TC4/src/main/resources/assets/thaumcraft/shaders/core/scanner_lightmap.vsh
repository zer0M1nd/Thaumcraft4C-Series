#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec4 vertexColor;
out vec2 texCoord0;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // The lens is emissive. Do not amplify its tiny pulse through the world's
    // nonlinear lightmap, gamma or torch flicker. Preserve texture alpha exactly.
    float pulse = clamp((float(UV2.x) - 200.0) / 10.0, -1.0, 1.0);
    vertexColor = vec4(Color.rgb * (0.995 + 0.005 * pulse), Color.a);
    texCoord0 = UV0;
}
