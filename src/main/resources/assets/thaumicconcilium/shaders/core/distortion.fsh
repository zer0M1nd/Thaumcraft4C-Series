#version 150
uniform sampler2D Sampler0;
uniform float Enabled;
uniform float Time;
in vec2 texturePosition;
out vec4 fragColor;
void main() {
    if(Enabled < 0.5){fragColor=texture(Sampler0,texturePosition);return;}

    vec2 mapCoordinate = texturePosition;
    mapCoordinate.t -= Time * 0.02;
    vec2 offset = (texture(Sampler0, fract(mapCoordinate)).xy - vec2(0.5)) * (1.0 - texturePosition.t);
    fragColor = vec4(0.9) * texture(Sampler0, fract(texturePosition + offset));
}
