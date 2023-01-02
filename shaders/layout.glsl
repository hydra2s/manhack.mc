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
	layout (offset = 0) mat4x4 transform;
	layout (offset = 64) uint64_t indexAddress;
	layout (offset = 72) uint indexType;
	layout (offset = 76) uint indexStride;
    layout (offset = 80) vec4 chunkOffset;
	layout (offset = 96) BufferBinding vertexBinding;
	layout (offset = 128) BufferBinding normalBinding;
	layout (offset = 160) BufferBinding uvBinding;
	layout (offset = 192) BufferBinding colorBinding;
	layout (offset = 224) int colorTex;
};

//
layout (set = 2, binding = 0, scalar) uniform UBO {
	layout (offset = 0) uint64_t accelStruct;
	layout (offset = 8) uint64_t geomData;
	layout (offset = 16) mat4x4 projection;
	layout (offset = 80) mat4x4 view;
	layout (offset = 144) vec4 cameraPos;
};

//
layout (buffer_reference, scalar, buffer_reference_align = 1) buffer Uint8 {
    uint8_t index;
};

//
layout (buffer_reference, scalar, buffer_reference_align = 1) buffer Uint16 {
    uint16_t index;
};

//
layout (buffer_reference, scalar, buffer_reference_align = 1) buffer Uint32 {
    uint32_t index;
};

//
layout (buffer_reference, scalar, buffer_reference_align = 1) buffer Vec3 {
    vec3 vertex;
};
