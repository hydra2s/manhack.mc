#version 450 core

//
const vec3 triangles[6] = {
    //
    vec3(-1.f, -1.f, 0.f),
    vec3( 1.f, -1.f, 0.f),
    vec3(-1.f,  1.f, 0.f),

    //
    vec3( 1.f, -1.f, 0.f),
    vec3(-1.f,  1.f, 0.f),
    vec3( 1.f,  1.f, 0.f)
};

//
layout(location = 0) out vec2 uv;

//
void main() {
    gl_Position = vec4(triangles[gl_VertexID], 1.f);
    uv = triangles[gl_VertexID].xy * 0.5f + 0.5f;
}
