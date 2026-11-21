#version 150
uniform sampler2D Sampler0;
uniform mat4 InvProjection;
uniform vec3 MistColor;
uniform float Density;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    float depth = texture(Sampler0, texCoord).r;
    vec4 view = InvProjection * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    float distanceToCamera = length(view.xyz) / max(abs(view.w), 0.00001);
    fragColor = vec4(MistColor, 1.0 - exp(-Density * distanceToCamera));
}
