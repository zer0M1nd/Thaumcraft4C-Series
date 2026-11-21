#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec3 position;
out vec2 texturePosition;
out vec4 vertexColor;
void main(){vec4 camera=ModelViewMat*vec4(Position,1.0);position=camera.xyz;texturePosition=UV0;vertexColor=Color;gl_Position=ProjMat*camera;}
