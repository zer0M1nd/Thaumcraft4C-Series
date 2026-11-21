#version 150
uniform sampler2D Sampler0;
uniform float Enabled;
in vec2 texturePosition;
in float pulse;
out vec4 fragColor;
void main() {
    if(Enabled < 0.5){fragColor=texture(Sampler0,texturePosition);return;}

    vec4 sampled = texture(Sampler0, texturePosition);
    // Original shader selects the rune mask using an exactly zero blue channel.
    fragColor = sampled.b == 0.0
        ? mix(vec4(0.1, 0.1, 0.1, 1.0), vec4(0.5, 0.0, 0.5, 1.0), pulse) / 1.2
        : sampled;
}
