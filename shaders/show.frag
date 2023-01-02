#version 450 core

//
uniform sampler2D swapchainImage;

//
layout(location = 0) in vec2 uv;
layout(location = 0) out vec4 fcolor;

//
void main() {
    fcolor = vec4(textureLod(swapchainImage, uv, 0));
}
