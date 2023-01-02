package org.hydra2s.manhack.shared.vulkan;

//
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.GlRendererObj;
import org.hydra2s.manhack.shared.interfaces.GlBaseSharedBuffer;
import org.hydra2s.manhack.virtual.buffer.GlVulkanVirtualBuffer;
import org.hydra2s.noire.descriptors.AccelerationStructureCInfo;
import org.hydra2s.noire.descriptors.DataCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.AccelerationStructureObj;
import org.hydra2s.noire.objects.MemoryAllocationObj;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL45;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;
import org.lwjgl.vulkan.VkAccelerationStructureBuildRangeInfoKHR;
import org.lwjgl.vulkan.VkAccelerationStructureInstanceKHR;
import org.lwjgl.vulkan.VkTransformMatrixKHR;

//
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//
import static org.lwjgl.opengl.EXTMemoryObject.glCreateMemoryObjectsEXT;
import static org.lwjgl.opengl.EXTMemoryObject.glNamedBufferStorageMemEXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.util.vma.Vma.vmaCreateVirtualBlock;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;

//
public class GlVulkanSharedBuffer implements GlBaseSharedBuffer {

    //
    public static final long uniformStride = 768L;
    public static final long maxDrawCalls = 1024L;

    // TODO: unify with GlRendererObj
    public static AccelerationStructureObj.BottomAccelerationStructureObj bottomLvl = null;
    public static AccelerationStructureObj.TopAccelerationStructureObj topLvl = null;
    public static MemoryAllocationObj.BufferObj instanceBuffer = null;
    public static VkAccelerationStructureInstanceKHR instanceInfo = null;
    public static VkAccelerationStructureBuildRangeInfoKHR.Buffer drawRanges = null;

    // TODO: unify with GlRendererObj
    public static void initialize() throws Exception {

        //
        sharedBufferMap = new HashMap<Integer, VkSharedBuffer>(){{
            put(0, createBuffer(1024L * 1024L * maxDrawCalls * 3L, true));  // for GL shared memory!!!
            put(1, createBuffer(1024L * 1024L * maxDrawCalls * 3L, false)); // for temp memory
            put(2, createBuffer(uniformStride * maxDrawCalls, false));
        }};

        //
        uniformDataBufferHost = new GlVulkanVirtualBuffer.VirtualBufferObj(0);
        uniformDataBuffer = new GlVulkanVirtualBuffer.VirtualBufferObj(2);

        // TODO: correct sizeof of uniform
        uniformDataBufferHost.data(GL_UNIFORM_BUFFER, uniformStride, GL_DYNAMIC_DRAW);
        uniformDataBuffer.data(GL_UNIFORM_BUFFER, uniformStride * 1024L, GL_DYNAMIC_DRAW);


        // create the largest acceleration structure allocation (up to 2 million)
        var _memoryAllocator = GlContext.rendererObj.memoryAllocator;


        bottomLvl = new AccelerationStructureObj.BottomAccelerationStructureObj(GlContext.rendererObj.logicalDevice.getHandle(), new AccelerationStructureCInfo.BottomAccelerationStructureCInfo(){{
            memoryAllocator = _memoryAllocator.getHandle().get();
            geometries = new ArrayList<>() {{
                for (int I = 0; I < maxDrawCalls; I++) {
                    add(new DataCInfo.TriangleGeometryCInfo() {{
                        vertexBinding = new DataCInfo.VertexBindingCInfo() {{
                            stride = 12;
                            vertexCount = 2048 * 3;
                            format = VK_FORMAT_R32G32B32_SFLOAT;
                        }};
                        indexBinding = new DataCInfo.IndexBindingCInfo() {{
                            vertexCount = 2048 * 3;
                            type = VK_INDEX_TYPE_UINT32;
                        }};
                    }});
                }
            }};
        }});

        // should to be reformed!
        //bottomLvl.geometryData.clear();

        //
        instanceBuffer = new MemoryAllocationObj.BufferObj(GlContext.rendererObj.logicalDevice.getHandle(), new MemoryAllocationCInfo.BufferCInfo(){{
            isHost = true;
            isDevice = true;
            size = VkAccelerationStructureInstanceKHR.SIZEOF;
            usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            memoryAllocator = _memoryAllocator.getHandle().get();
        }});

        //
        instanceInfo = VkAccelerationStructureInstanceKHR.create(memAddress(instanceBuffer.map(VkAccelerationStructureInstanceKHR.SIZEOF, 0)));
        instanceInfo.mask(0xFF);
        instanceInfo.accelerationStructureReference(bottomLvl.getDeviceAddress());
        instanceInfo.flags(0);

        // will be changed into camera position shifting
        instanceInfo.transform(VkTransformMatrixKHR.calloc().matrix(memAllocFloat(12).put(0, new float[]{
            1.0F, 0.0F, 0.0F, 0.0F,
            0.0F, 1.0F, 0.0F, 0.0F,
            0.0F, 0.0F, 1.0F, 0.0F
        })));

        //
        //instanceBuffer.unmap();

        //
        topLvl = new AccelerationStructureObj.TopAccelerationStructureObj(GlContext.rendererObj.logicalDevice.getHandle(), new AccelerationStructureCInfo.TopAccelerationStructureCInfo(){{
            memoryAllocator = _memoryAllocator.getHandle().get();
            instances = new DataCInfo.InstanceGeometryCInfo(){{
                instanceBinding = new DataCInfo.InstanceBindingCInfo(){{
                    address = instanceBuffer.getDeviceAddress();
                    vertexCount = 1;
                }};
            }};
        }});

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
