//
struct BufferBinding {
	uint64_t address;
	uint64_t size;
	uint relativeOffset;
	uint stride;
	uint format;
	uint unknown;
};

//
layout (buffer_reference, scalar, buffer_reference_align = 1) buffer DrawCall {
	mat4x4 transform;
	uint64_t indexAddress;
	uint indexType;
	uint unknown;
	BufferBinding vertexBinding;
	BufferBinding normalBinding;
	BufferBinding uvBinding;
	BufferBinding colorBinding;
	int colorTex;
};

//
layout (set = 0, binding = 2) uniform UBO {
	uint64_t accelStruct;
	uint64_t geomData;
	mat4x4 projection;
	mat4x4 view;
	vec4 cameraPos;
};
