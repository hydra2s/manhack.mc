//#version 460 compatibility

#ifndef I16VEC2
#define I16VEC2 ivec2
#endif

//
#ifdef POSITION_COLOR_TEXTURE_LIGHT_NORMAL
layout (location=0) in vec3 Position;
layout (location=1) in vec4 Color;
layout (location=2) in vec2 UV0;
const I16VEC2 UV1 = I16VEC2(0);
layout (location=3) in I16VEC2 UV2;
layout (location=4) in vec3 Normal;
#endif

#ifdef POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL
layout (location=0) in vec3 Position;
layout (location=1) in vec4 Color;
layout (location=2) in vec2 UV0;
layout (location=3) in I16VEC2 UV1;
layout (location=4) in I16VEC2 UV2;
layout (location=5) in vec3 Normal;
#endif

#ifdef POSITION_TEXTURE_COLOR_LIGHT
layout (location=0) in vec3 Position;
layout (location=2) in vec4 Color;
layout (location=1) in vec2 UV0;
const I16VEC2 UV1 = I16VEC2(0);
layout (location=3) in I16VEC2 UV2;
const vec3 Normal = vec3(0.f.xxx);
#endif

#ifdef POSITION
layout (location=0) in vec3 Position;
const vec4 Color = vec4(1.f.xxxx);
const vec2 UV0 = vec2(0.f.xx);
const I16VEC2 UV1 = I16VEC2(0);
const I16VEC2 UV2 = I16VEC2(0);
const vec3 Normal = vec3(0.f.xxx);
#endif

#ifdef POSITION_COLOR
layout (location=0) in vec3 Position;
layout (location=1) in vec4 Color;
const vec2 UV0 = vec2(0.f.xx);
const I16VEC2 UV1 = I16VEC2(0);
const I16VEC2 UV2 = I16VEC2(0);
const vec3 Normal = vec3(0.f.xxx);
#endif

#ifdef POSITION_COLOR_LIGHT
layout (location=0) in vec3 Position;
layout (location=1) in vec4 Color;
const vec2 UV0 = vec2(0.f.xx);
const I16VEC2 UV1 = I16VEC2(0);
layout (location=2) in I16VEC2 UV2;
const vec3 Normal = vec3(0.f.xxx);
#endif

#ifdef POSITION_TEXTURE
layout (location=0) in vec3 Position;
const vec4 Color = vec4(1.f.xxxx);
layout (location=1) in vec2 UV0;
const I16VEC2 UV1 = I16VEC2(0);
const I16VEC2 UV2 = I16VEC2(0);
const vec3 Normal = vec3(0.f.xxx);
#endif

#ifdef POSITION_COLOR_TEXTURE
layout (location=0) in vec3 Position;
layout (location=1) in vec4 Color;
layout (location=2) in vec2 UV0;
const I16VEC2 UV1 = I16VEC2(0);
const I16VEC2 UV2 = I16VEC2(0);
const vec3 Normal = vec3(0.f.xxx);
#endif

#ifdef POSITION_TEXTURE_COLOR
layout (location=0) in vec3 Position;
layout (location=2) in vec4 Color;
layout (location=1) in vec2 UV0;
const I16VEC2 UV1 = I16VEC2(0);
const I16VEC2 UV2 = I16VEC2(0);
const vec3 Normal = vec3(0.f.xxx);
#endif

#ifdef POSITION_COLOR_TEXTURE_LIGHT
layout (location=0) in vec3 Position;
layout (location=1) in vec4 Color;
layout (location=2) in vec2 UV0;
const I16VEC2 UV1 = I16VEC2(0);
layout (location=3) in vec2 UV2;
const vec3 Normal = vec3(0.f.xxx);
#endif

#ifdef POSITION_TEXTURE_LIGHT_COLOR
layout (location=0) in vec3 Position;
layout (location=3) in vec4 Color;
layout (location=1) in vec2 UV0;
const I16VEC2 UV1 = I16VEC2(0);
layout (location=2) in I16VEC2 UV2;
const vec3 Normal = vec3(0.f.xxx);
#endif

#ifdef POSITION_TEXTURE_COLOR_NORMAL
layout (location=0) in vec3 Position;
layout (location=2) in vec4 Color;
layout (location=1) in vec2 UV0;
const I16VEC2 UV1 = I16VEC2(0);
const I16VEC2 UV2 = I16VEC2(0);
layout (location=3) in vec3 Normal;
#endif

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform int FogShape;

//
layout(xfb_buffer = 0, xfb_stride = 48) out Data
{
    layout(xfb_offset = 0) out vec3 position;
    layout(xfb_offset = 12) out vec4 normal;
    layout(xfb_offset = 28) out vec2 texCoord0;
    layout(xfb_offset = 36) out uint vertexColor;
    layout(xfb_offset = 40) out uint texCoord1;
    layout(xfb_offset = 44) out uint texCoord2;
};

//
void main() {
    position = Position;
    vertexColor = packUnorm4x8(Color);
    texCoord0 = UV0;
    texCoord1 = packHalf2x16(UV1);
    texCoord2 = packHalf2x16(UV2);
    normal = vec4(Normal, 0.0);
    gl_Position = vec4(0.f.xxx, 1.f);
}
