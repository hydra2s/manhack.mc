package org.hydra2s.manhack.shared.vulkan;

//
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.shared.interfaces.GlBaseSharedBuffer;
import org.hydra2s.manhack.virtual.buffer.GlVulkanVirtualBuffer;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.MemoryAllocationObj;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL45;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;

//
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//
import static org.lwjgl.opengl.EXTMemoryObject.glCreateMemoryObjectsEXT;
import static org.lwjgl.opengl.EXTMemoryObject.glNamedBufferStorageMemEXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.util.vma.Vma.vmaCreateVirtualBlock;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;

//
public class GlVulkanSharedBuffer implements GlBaseSharedBuffer {

    public static final long uniformStride = 768L;

    //
    public static void initialize() throws Exception {

        //
        sharedBufferMap = new HashMap<Integer, VkSharedBuffer>(){{
            put(0, createBuffer(1024L * 1024L * 1024L * 3L, true));  // for GL shared memory!!!
            put(1, createBuffer(1024L * 1024L * 1024L, false)); // for temp memory
            put(2, createBuffer(uniformStride * 1024L, false));
        }};

        //
        uniformDataBufferHost = new GlVulkanVirtualBuffer.VirtualBufferObj(0);
        uniformDataBuffer = new GlVulkanVirtualBuffer.VirtualBufferObj(2);

        // TODO: correct sizeof of uniform
        uniformDataBufferHost.data(GL_UNIFORM_BUFFER, uniformStride, GL_DYNAMIC_DRAW);
        uniformDataBuffer.data(GL_UNIFORM_BUFFER, uniformStride * 1024L, GL_DYNAMIC_DRAW);
    };

    //
    // TODO: use polymorphism in this case
    static public class VkSharedBuffer {
        public int glMemory = 0;
        public int glStorageBuffer = 0;
        public MemoryAllocationObj.BufferObj obj;
        public MemoryAllocationCInfo.BufferCInfo bufferCreateInfo;
        public PointerBuffer vb;

        // also, is this full size
        public VmaVirtualBlockCreateInfo vbInfo = VmaVirtualBlockCreateInfo.calloc().flags(VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT);
    };

    // TODO: support for typed (entity, indexed, blocks, etc.)
    public static Map<Integer, VkSharedBuffer> sharedBufferMap = new HashMap<Integer, VkSharedBuffer>();
    public static GlVulkanVirtualBuffer.VirtualBufferObj uniformDataBufferHost;
    public static GlVulkanVirtualBuffer.VirtualBufferObj uniformDataBuffer;

    // TODO: add host-based memory support (as version)
    public static VkSharedBuffer createBuffer(long defaultSize, boolean isHost) {
        VkSharedBuffer sharedBuffer = new VkSharedBuffer();
        var _pipelineLayout = GlContext.rendererObj.pipelineLayout;
        var _memoryAllocator = GlContext.rendererObj.memoryAllocator;

        //
        sharedBuffer.obj = new MemoryAllocationObj.BufferObj(GlContext.rendererObj.logicalDevice.getHandle(), sharedBuffer.bufferCreateInfo = new MemoryAllocationCInfo.BufferCInfo() {{
            isHost = isHost; // false if !isHost and there is no resizableBAR support
            isDevice = !isHost;
            size = defaultSize;
            usage = VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            memoryAllocator = _memoryAllocator.getHandle().get();
        }});

        //
        vmaCreateVirtualBlock(sharedBuffer.vbInfo.size(sharedBuffer.bufferCreateInfo.size), sharedBuffer.vb = memAllocPointer(1));
        if (sharedBuffer.glMemory == 0) {
            glImportMemoryWin32HandleEXT(sharedBuffer.glMemory = glCreateMemoryObjectsEXT(), sharedBuffer.obj.memoryRequirements2.memoryRequirements().size(), GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, sharedBuffer.obj.getWin32Handle().get(0));
        }

        //
        glNamedBufferStorageMemEXT(sharedBuffer.glStorageBuffer = GL45.glCreateBuffers(), sharedBuffer.bufferCreateInfo.size, sharedBuffer.glMemory, sharedBuffer.obj.memoryOffset);

        // TODO: bind with GL object!
        //GlContext.virtualBufferMap.put(glBuffer[0], sharedBuffer);
        return sharedBuffer;
    };

}
