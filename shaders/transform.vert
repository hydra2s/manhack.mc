#version 460 compatibility

//
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV1;
in vec2 UV2;
in vec3 Normal;

//
layout(xfb_buffer = 0, xfb_stride = 48) out;
layout(xfb_buffer = 0) out Data
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
}
