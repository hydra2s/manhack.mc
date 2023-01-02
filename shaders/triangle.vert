#version 460 core
#extension GL_ARB_separate_shader_objects : enable
#extension GL_GOOGLE_include_directive : enable
#extension GL_ARB_separate_shader_objects : enable
#extension GL_EXT_shader_explicit_arithmetic_types_int64 : enable
#extension GL_EXT_shader_explicit_arithmetic_types_int32 : enable
#extension GL_EXT_shader_explicit_arithmetic_types_int16 : enable
#extension GL_EXT_nonuniform_qualifier : enable
#extension GL_EXT_scalar_block_layout : enable
#extension GL_EXT_buffer_reference : enable
#extension GL_EXT_buffer_reference2 : enable
#extension GL_EXT_samplerless_texture_functions : enable
#extension GL_EXT_fragment_shader_barycentric : enable
#extension GL_EXT_shader_explicit_arithmetic_types_float16 : enable
#extension GL_EXT_shader_atomic_float : enable

//
out gl_PerVertex { vec4 gl_Position; };

//
layout (location = 0) pervertexEXT out Inputs {
	vec4 test;
};

//
void main() {
	gl_Position = vec4(0.f, 0.f, 0.f, 1.f);
}
