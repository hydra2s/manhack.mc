package org.hydra2s.manhack;

//
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.MemoryAllocationObj;
import org.hydra2s.noire.objects.PipelineLayoutObj;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL45;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;

//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

//
import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL30.glGetInteger;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL45.glNamedBufferSubData;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;

//
public class GlVirtualBufferSystem {

    public static final boolean VGL_VERSION_A = true;

    //
    static public class ResourceBuffer {
        public int glMemory = 0;
        public int glStorageBuffer = 0;
        public MemoryAllocationObj.BufferObj obj;
        public MemoryAllocationCInfo.BufferCInfo bufferCreateInfo;
        public PointerBuffer vb;

        // also, is this full size
        //public long lastOffset = 0L;
        VmaVirtualBlockCreateInfo vbInfo = VmaVirtualBlockCreateInfo.create()
            .flags(VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT);



    };

    //
    static public class ResourceCache {
        public long size = 0;
        public LongBuffer offset = memAllocLong(1).put(0, 0L);

        //
        //public VmaVirtualAllocationInfo allocInfo;
        public VmaVirtualAllocationCreateInfo allocCreateInfo = null;
        public ResourceBuffer mapped = null;
        public PointerBuffer allocId = null;

        // TODO: Virtual OpenGL Memory!
        public int glVirtualBuffer = -1;
        public int glStorageBuffer = -1;
        public int stride = 0;
        public int bindingIndex = 0;
        public int target = 0;
        public ByteBuffer allocatedMemory;
        public int vao = 0;
        //public ArrayList<Runnable> defer = null;

        //
        public ByteBuffer map(int target, int access, long vkWholeSize, long i) {
            if (VGL_VERSION_A) {
                return (this.allocatedMemory = this.mapped.obj.map(vkWholeSize, i + offset.get(0)));
            } else {
                return (this.allocatedMemory = glMapBuffer(target, access));
            }
        }

        public void unmap(int target) {
            if (VGL_VERSION_A) {
                this.mapped.obj.unmap();
            } else {
                glUnmapBuffer(target);
            }
            this.allocatedMemory = null;
        }

        //
        public ResourceCache() {
            //this.defer = new ArrayList<>();
            this.glVirtualBuffer = resourceCacheMap.push(this);
            this.offset = memAllocLong(1).put(0, 0L);
            this.allocId = memAllocPointer(1).put(0, 0L);
            this.allocCreateInfo = VmaVirtualAllocationCreateInfo.create().alignment(4L);
        }
    };


    public static ResourceCache assertVirtualBuffer(ResourceCache cache) throws Exception {
        if (cache == null || cache.glVirtualBuffer <= 0 || !resourceCacheMap.contains(cache)) {
            System.out.println("Wrong Virtual Buffer Id! " + (cache != null ? cache.glVirtualBuffer : -1));
            throw new Exception("Wrong Virtual Buffer Id! " + (cache != null ? cache.glVirtualBuffer : -1));
        }
        return cache;
    }


    // TODO: support for typed (entity, indexed, blocks, etc.)
    public static Map<Integer, ResourceBuffer> resourceTargetMap = new HashMap<Integer, ResourceBuffer>();

    // TODO: Virtual OpenGL Buffers support!
    // TODO: Outstanding Array instead of Map!
    public static PipelineLayoutObj.OutstandingArray<ResourceCache> resourceCacheMap = new PipelineLayoutObj.OutstandingArray<ResourceCache>();
    public static ResourceCache dummyCache = new ResourceCache();

    //
    public static Map<Integer, ResourceCache> boundBuffers = new HashMap<Integer, ResourceCache>() {{

    }};

    //
    public static boolean hasIndexBuffer = false;

    //
    public static void initialize() throws IOException {
        resourceTargetMap = new HashMap<Integer, ResourceBuffer>(){{
            put(0, vkCreateBuffer(1024L * 1024L * 256L));
        }};
    };

    // TODO: needs fully replace OpenGL buffer memory stack
    // TODO: needs immutable storage and ranges support
    public static ResourceBuffer vkCreateBuffer(long defaultSize) {
        if (VGL_VERSION_A) {
            ResourceBuffer resource = new ResourceBuffer();
            var _pipelineLayout = GlContext.rendererObj.pipelineLayout;
            var _memoryAllocator = GlContext.rendererObj.memoryAllocator;
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
            //GlContext.resourceCacheMap.put(glBuffer[0], resource);
            return resource;
        }
        return null;
    };

    //
    public static int glCreateBuffer() throws Exception {
        // TODO: support for typed (entity, indexed, blocks, etc.)
        var mapped = resourceTargetMap.get(0);
        var cache = new ResourceCache();

        assertVirtualBuffer(cache);

        if (VGL_VERSION_A) {
            cache.glStorageBuffer = mapped.glStorageBuffer;
            cache.mapped = mapped;
        } else {
            cache.glStorageBuffer = GL45.glGenBuffers();
        }

        //
        System.out.println("Generated New Virtual Buffer! Id: " + cache.glVirtualBuffer);
        return cache.glVirtualBuffer;
    };

    //
    public static ResourceCache glBindVertexBuffer(ResourceCache cache) {
        if (cache.target == GL_ARRAY_BUFFER && cache.stride > 0 && cache.size > 0 && cache.vao > 0) {
            GL45.glVertexArrayVertexBuffer(cache.vao, cache.bindingIndex, cache.glStorageBuffer, cache.offset.get(0), cache.stride);
            // TODO: fix calling spam by VAO objects

            /*
            System.out.println("Vertex Buffer Bound!");
            System.out.println("Arg0 (VAO): " + cache.vao);
            System.out.println("Arg1 (BindingIndex): " + cache.bindingIndex);
            System.out.println("Arg2 (Buffer, SYSTEM): " + cache.glStorageBuffer);
            System.out.println("Arg2 (Buffer, VIRTUAL): " + cache.glVirtualBuffer);
            System.out.println("Arg3 (Offset): " + cache.offset.get(0));
            System.out.println("Arg4 (Stride): " + cache.stride);
            */
        }
        return cache;
    }

    //
    public static ResourceCache glAllocateMemory(ResourceCache cache, long defaultSize, int usage) throws Exception {
        // TODO: support for typed (entity, indexed, blocks, etc.)
        var mapped = resourceTargetMap.get(0);

        //
        assertVirtualBuffer(cache);
        if (cache.size < defaultSize)
        {
            System.out.println("WARNING! Size of virtual buffer was changed! " + cache.size + " != " + defaultSize);
            System.out.println("Virtual GL buffer ID: " + cache.glVirtualBuffer);

            //
            glDeallocateBuffer(cache);

            //
            int res = vmaVirtualAllocate(mapped.vb.get(0), cache.allocCreateInfo.size(cache.size = defaultSize), cache.allocId.put(0, 0L), cache.offset.put(0, 0L));
            if (res != VK_SUCCESS) {
                System.out.println("Allocation Failed: " + res);
                throw new Exception("Allocation Failed: " + res);
            }

            //
            System.out.println("Virtual buffer Size Changed: " + cache.size);
        }
        return cache;
    }

    //
    public static ResourceCache glAllocateMemory(int target, long defaultSize, int usage) throws Exception {
        return glAllocateMemory(boundBuffers.get(target), defaultSize, usage);
    }

    //
    public static void glBindBuffer(int target, int glVirtual) throws Exception {
        GL20.glBindBuffer(target, 0);
        boundBuffers.remove(target);

        // TODO: unbound memory
        var cache = assertVirtualBuffer(resourceCacheMap.get(glVirtual));
        if (target == GL_ARRAY_BUFFER) {
            cache.vao = cache.vao > 0 ? cache.vao : glGetInteger(GL_VERTEX_ARRAY_BINDING);
        }

        //
        //if (target == GL_ELEMENT_ARRAY_BUFFER) {
            //System.out.println("Bound Virtual Index Buffer: " + cache.glVirtualBuffer + ", With offset: " + cache.offset.get(0));
        //}

        // TODO: unbound memory
        boundBuffers.put(cache.target = target, cache);
        GL20.glBindBuffer(target, cache.glStorageBuffer);
        glBindVertexBuffer(cache);
    }

    public static ResourceCache glDeallocateBuffer(ResourceCache resource) throws Exception {
        if (VGL_VERSION_A) {
            assertVirtualBuffer(resource);
            vmaVirtualFree(resource.mapped.vb.get(0), resource.allocId.get(0));
            System.out.println("Deallocated Virtual Buffer! Id: " + resource.glVirtualBuffer);
            resource.size = 0L;
            resource.offset.put(0, 0L);
        }
        return resource;
    }

    public static ResourceCache glDeallocateBuffer(int glVirtualBuffer) throws Exception {
        return glDeallocateBuffer(assertVirtualBuffer(GlVirtualBufferSystem.resourceCacheMap.get(glVirtualBuffer)));
    }

    public static void glDeleteBuffer(int glVirtualBuffer) throws Exception {
        GlVirtualBufferSystem.ResourceCache resource = assertVirtualBuffer(GlVirtualBufferSystem.resourceCacheMap.get(glVirtualBuffer));
        GlVirtualBufferSystem.resourceCacheMap.removeMem(glDeallocateBuffer(resource));
        GlVirtualBufferSystem.boundBuffers.remove(resource.target);
        System.out.println("Deleted Virtual Buffer! Id: " + resource.glVirtualBuffer);
        resource.glVirtualBuffer = -1;

        if (!VGL_VERSION_A) {
            glDeleteBuffers(resource.glStorageBuffer);
        }
    }

    // TODO: full replace by Vulkan
    public static ResourceCache glBufferData(int target, long data, int usage) throws Exception {
        var cache = assertVirtualBuffer(boundBuffers.get(target));
        if (VGL_VERSION_A) {
            glAllocateMemory(cache, data, usage);
        } else {
            GL45.glNamedBufferData(cache.glStorageBuffer, cache.size = data, usage);
        }
        glBindVertexBuffer(cache);
        return cache;
    }

    // TODO: full replace by Vulkan
    public static ResourceCache glBufferData(int target, ByteBuffer data, int usage) throws Exception {
        var cache = assertVirtualBuffer(boundBuffers.get(target));
        if (VGL_VERSION_A) {
            glAllocateMemory(cache, data.remaining(), usage);
            glNamedBufferSubData(cache.glStorageBuffer, cache.offset.get(0), data);
        } else {
            GL45.glNamedBufferData(cache.glStorageBuffer, data, usage);
            cache.size = data.remaining();
        }
        glBindVertexBuffer(cache);
        return cache;
    }
};
