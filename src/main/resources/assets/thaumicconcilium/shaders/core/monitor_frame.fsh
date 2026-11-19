#version 150
uniform sampler2D Sampler0;
uniform float Enabled;
uniform float PreviousColors[6];
uniform float NextColors[6];
uniform float Progress;
in vec2 texturePosition;
out vec4 fragColor;
const vec4 colors[6] = vec4[6](vec4(1.,.988,1.,1.),vec4(.42,.42,.42,1.),vec4(1.,0.,0.,1.),vec4(.188,.98,.973,1.),vec4(.188,1.,0.,1.),vec4(.965,1.,0.,1.));
void main() {
    if(Enabled < 0.5){fragColor=texture(Sampler0,texturePosition);return;}

    vec4 sampled=texture(Sampler0,texturePosition);
    int segment=255-int(round(sampled.b*255.0));
    if(segment>=0 && segment<6) {
        fragColor=mix(colors[int(PreviousColors[segment])],colors[int(NextColors[segment])],Progress)/1.2;
    } else fragColor=sampled;
}
