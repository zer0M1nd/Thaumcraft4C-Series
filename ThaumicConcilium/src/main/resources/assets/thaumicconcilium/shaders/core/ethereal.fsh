#version 150
uniform sampler2D Sampler0;
uniform float Enabled;
in vec2 texturePosition;
in float progress;
out vec4 fragColor;
void main(){
    if(Enabled < 0.5){fragColor=texture(Sampler0,texturePosition);return;}
fragColor=mix(texture(Sampler0,texturePosition),vec4(0.0,191.0/255.0,1.0,0.5),progress);}
