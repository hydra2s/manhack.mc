#version 460 core
#extension GL_ARB_separate_shader_objects : enable
#extension GL_GOOGLE_include_directive : enable
#extension GL_ARB_separate_shader_objects : enable
#extension GL_EXT_shader_explicit_arithmetic_types_int64 : enable
#extension GL_EXT_shader_explicit_arithmetic_types_int32 : enable
#extension GL_EXT_shader_explicit_arithmetic_types_int16 : enable
#extension GL_EXT_shader_explicit_arithmetic_types_int8 : enable
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
#include "layout.glsl"

//
void main() {
	uint index = gl_VertexIndex;
	if (geomData != 0) {
		// TODO: correct draw call stride
		DrawCall drawCall = DrawCall(geomData + gl_DrawID*768);
		if (drawCall.vertexBinding.address > 0 && drawCall.indexAddress > 0) {
			vec4 vPosition = vec4(Vec3(drawCall.vertexBinding.address + drawCall.vertexBinding.stride * index).vertex, 1.f);

			//
			//vPosition.xyz += drawCall.chunkOffset.xyz;//drawCall.chunkOffset.xyz;
			vPosition *= drawCall.transform;
			
			//
			gl_Position = (vPosition) * projection;
			//gl_Position = vec4(0.f.xxx, 1.f);
		} else {
			gl_Position = vec4(0.f.xxx, 1.f);
		}
	} else {
		gl_Position = vec4(0.f.xxx, 1.f);
	}
	// DEBUG!
	//gl_Position = vec4(triangles[gl_VertexIndex%6], 1.f);
}
