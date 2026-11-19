#version 150
uniform sampler2D Sampler0;
uniform float Enabled;
in vec2 texturePosition;
in float pulse;
out vec4 fragColor;
void main() {
    if(Enabled < 0.5){fragColor=texture(Sampler0,texturePosition);return;}

    vec4 color=texture(Sampler0,texturePosition);
    color.rgb=mix(color.rgb,vec3(0.7),pulse);
    fragColor=color;
}
