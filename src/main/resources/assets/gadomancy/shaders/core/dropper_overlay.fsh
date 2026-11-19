#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec4 vertexColor;
in vec4 lightColor;
in vec2 texCoord0;
out vec4 fragColor;
void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    vec4 primary = vertexColor * ColorModulator;
    // Fixed-function GL_ADD adds RGB but multiplies alpha. Lightmap is the next texture unit.
    vec4 color = vec4(min(texel.rgb + primary.rgb, vec3(1.0)), texel.a * primary.a) * lightColor;
    if (color.a <= 1.0 / 255.0) discard;
    fragColor = color;
}
