#version 150
uniform sampler2D Sampler0;
uniform int Pass;
in vec2 texCoord;
out vec4 fragColor;

vec3 sampleAt(vec2 at) { return texture(Sampler0, clamp(at, vec2(0.0), vec2(1.0))).rgb; }
float lightness(vec3 c) { return (min(c.r,min(c.g,c.b)) + max(c.r,max(c.g,c.b))) * 0.5; }
void main() {
    vec3 color = sampleAt(texCoord);
    if (Pass == 0 || Pass == 1) {
        vec2 stepSize = (Pass == 0 ? vec2(1.0,0.0) : vec2(0.0,1.0)) / vec2(textureSize(Sampler0,0));
        vec3 sum = vec3(0.0);
        for (int i=-10; i<=10; ++i) sum += sampleAt(texCoord + float(i)*stepSize);
        color = sum / 21.0;
    } else if (Pass == 2) {
        // Scaling HSL saturation is exactly mixing RGB toward its HSL lightness.
        // This avoids the old hue calculation's division by zero on grayscale.
        float lum = lightness(color);
        color = mix(vec3(lum),color,0.5 + 0.5*min(0.2,lum)*5.0);
        vec3 average = vec3(0.0);
        for (int y=1; y<=9; ++y) for (int x=1; x<=9; ++x)
            average += sampleAt(vec2(x,y)*0.1);
        float exposure = clamp(0.3/max(lightness(average/81.0),0.000001),0.7,4.0);
        color *= 0.6 + 0.4*exposure;
    } else if (Pass == 3) {
        // Original bloom2 has one iteration (-0.1, then +1), four taps and this gain.
        float i=-0.1, width=0.002;
        vec3 sum=sampleAt(texCoord+vec2(i,i)*width*0.7)
                +sampleAt(texCoord+vec2(i,-i)*width*0.7)
                +sampleAt(texCoord+vec2(0.0,i)*width)
                +sampleAt(texCoord+vec2(i,0.0)*width);
        sum *= 10.0/4.8;
        float brightness=(color.r+color.g+color.b)/3.0;
        color=(color+sum*sum*(0.015-brightness*0.01)*5.0)*0.9;
    }
    fragColor=vec4(clamp(color,vec3(0.0),vec3(1.0)),1.0);
}
