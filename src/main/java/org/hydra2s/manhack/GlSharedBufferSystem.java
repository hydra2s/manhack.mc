package org.hydra2s.manhack;

import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.RendererCInfo;
import org.hydra2s.noire.objects.MemoryAllocationObj;
import org.hydra2s.noire.objects.MinecraftRendererObj;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL45;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.EXTMemoryObject.glCreateMemoryObjectsEXT;
import static org.lwjgl.opengl.EXTMemoryObject.glNamedBufferStorageMemEXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.util.vma.Vma.vmaCreateVirtualBlock;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;

//
public class GlSharedBufferSystem {

    //
    public static void initialize() throws IOException {
        resourceTargetMap = new HashMap<Integer, VkSharedBuffer>(){{
            put(0, vkCreateBuffer(1024L * 1024L * 256L));
        }};
        //initialize();
    };

    //
    static public class VkSharedBuffer {
        public int glMemory = 0;
        public int glStorageBuffer = 0;
        public MemoryAllocationObj.BufferObj obj;
        public MemoryAllocationCInfo.BufferCInfo bufferCreateInfo;
        public PointerBuffer vb;

        // also, is this full size
        VmaVirtualBlockCreateInfo vbInfo = VmaVirtualBlockCreateInfo.create().flags(VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT);
    };

    // TODO: support for typed (entity, indexed, blocks, etc.)
    public static Map<Integer, VkSharedBuffer> resourceTargetMap = new HashMap<Integer, VkSharedBuffer>();

    // TODO: needs fully replace OpenGL buffer memory stack
    // TODO: needs immutable storage and ranges support
    public static VkSharedBuffer vkCreateBuffer(long defaultSize) {
        VkSharedBuffer resource = new VkSharedBuffer();
        var _pipelineLayout = GlContext.rendererObj.pipelineLayout;
        var _memoryAllocator = GlContext.rendererObj.memoryAllocator;

        //
        resource.obj = new MemoryAllocationObj.BufferObj(GlContext.rendererObj.logicalDevice.getHandle(), resource.bufferCreateInfo = new MemoryAllocationCInfo.BufferCInfo() {{
            isHost = true;
            isDevice = true;
            size = defaultSize;
            usage = VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            memoryAllocator = _memoryAllocator.getHandle().get();
        }});

        //
        vmaCreateVirtualBlock(resource.vbInfo.size(resource.bufferCreateInfo.size), resource.vb = memAllocPointer(1));
        if (resource.glMemory == 0) {
            glImportMemoryWin32HandleEXT(resource.glMemory = glCreateMemoryObjectsEXT(), resource.bufferCreateInfo.size + resource.obj.memoryOffset, GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, resource.obj.getWin32Handle().get(0));
        }

        //
        int glBuffer = GL45.glCreateBuffers();
        //int glBuffer = GL20.glGenBuffers();
        glNamedBufferStorageMemEXT(glBuffer, resource.bufferCreateInfo.size, resource.glMemory, resource.obj.memoryOffset);
        resource.glStorageBuffer = glBuffer;

        // TODO: bind with GL object!
        //GlContext.virtualBufferMap.put(glBuffer[0], resource);
        return resource;
    };


}
