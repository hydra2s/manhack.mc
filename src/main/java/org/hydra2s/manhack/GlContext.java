package org.hydra2s.manhack;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.texture.NativeImage;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.RendererCInfo;
import org.hydra2s.noire.objects.MemoryAllocationObj;
import org.hydra2s.noire.objects.MinecraftRendererObj;
import org.hydra2s.noire.objects.PipelineLayoutObj;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL45;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;
import org.lwjgl.vulkan.VkExtent3D;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.glCopyBufferSubData;
import static org.lwjgl.opengl.GL43.glBindVertexBuffer;
import static org.lwjgl.opengl.GL44.GL_CLIENT_STORAGE_BIT;
import static org.lwjgl.opengl.GL44.GL_DYNAMIC_STORAGE_BIT;
import static org.lwjgl.opengl.GL45.glNamedBufferSubData;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;

public class GlContext {

    public static boolean worldRendering = false;

    //
    static public class ResourceImage {
        public int glMemory = 0;
        public VkExtent3D extent;
        public MemoryAllocationObj.ImageObj obj;
        public MemoryAllocationCInfo.ImageCInfo imageCreateInfo;
    };

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
        public int glVirtualBuffer = 0;
        public int glStorageBuffer = 0;
        public int stride = 0;
        public int bindingIndex = 0;
        public int target = 0;
        public ByteBuffer allocatedMemory;
        //public ArrayList<Runnable> defer = null;

        //
        public ByteBuffer map(long vkWholeSize, long i) {
            return this.mapped.obj.map(vkWholeSize, i + offset.get(0));
        }

        public void unmap() {
            this.mapped.obj.unmap();
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

    // TODO: support for typed (entity, indexed, blocks, etc.)
    public static Map<Integer, ResourceBuffer> resourceTargetMap = new HashMap<Integer, ResourceBuffer>();

    //
    public static Map<Integer, ResourceImage> resourceImageMap = new HashMap<Integer, ResourceImage>();

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
    public static MinecraftRendererObj rendererObj;

    //
    public static void initialize() throws IOException {
        rendererObj = new MinecraftRendererObj(null, new RendererCInfo(){

        });
        resourceTargetMap = new HashMap<Integer, ResourceBuffer>(){{
            put(0, vkCreateBuffer(1024L * 1024L * 256L));
        }};
    };


    // TODO: fill geometryLevel acceleration structure
    public static void inclusion() {
        // colored texture (albedo)
        ShaderProgram shader = RenderSystem.getShader();

        //
        List<ResourceImage> resources = new ArrayList<ResourceImage>();
        for(int j = 0; j < 8; ++j) {
            resources.add(GlContext.resourceImageMap.get(RenderSystem.getShaderTexture(j)));
        };

        //
        ResourceImage Sampler0 = resources.get(0);

        // also, us needs VertexFormat
        // TODO: support normal map and PBR (i.e. IRIS)
    };

    //
    public static void glPrepareImage(NativeImage.InternalFormat internalFormat, int id, int maxLevel, int width, int height) {
        if (internalFormat == NativeImage.InternalFormat.RGB) {
            for(int i = 0; i <= maxLevel; ++i) {
                GlStateManager._texImage2D(3553, i, internalFormat.getValue(), width >> i, height >> i, 0, 6408, 5121, (IntBuffer)null);
            }
        } else {
            ResourceImage resource = GlContext.resourceImageMap.get(id);
            MemoryAllocationObj.ImageObj resourceObj = null;

            //
            if (resource == null) {
                resource = new ResourceImage();
                resource.extent = VkExtent3D.create();
                resource.extent.set(width, height, 1);

                //
                int vkFormat = VK_FORMAT_R8G8B8A8_UNORM, glFormat = GL_RGBA8;

                //
                if (internalFormat == NativeImage.InternalFormat.RG) {
                    vkFormat = VK_FORMAT_R8G8_UNORM;
                    glFormat = GL_RG8;
                }

                //
                if (internalFormat == NativeImage.InternalFormat.RED) {
                    vkFormat = VK_FORMAT_R8_UNORM;
                    glFormat = GL_R8;
                }

                //
                var _pipelineLayout = rendererObj.pipelineLayout;
                var _memoryAllocator = rendererObj.memoryAllocator;
                ResourceImage finalResource = resource;
                int finalVkFormat = vkFormat;
                resourceObj = resource.obj = new MemoryAllocationObj.ImageObj(rendererObj.logicalDevice.getHandle(), resource.imageCreateInfo = new MemoryAllocationCInfo.ImageCInfo(){{
                    extent3D = finalResource.extent;
                    format = finalVkFormat;
                    mipLevels = 1;
                    arrayLayers = 1;
                    isHost = false;
                    isDevice = true;
                    memoryAllocator = _memoryAllocator.getHandle().get();
                }});

                //
                glBindTexture(GL_TEXTURE_2D, id);
                if (resource.glMemory == 0) {
                    glImportMemoryWin32HandleEXT(resource.glMemory = glCreateMemoryObjectsEXT(), width * height * 4, GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, resourceObj.getWin32Handle().get(0));
                }
                glTexStorageMem2DEXT(GL_TEXTURE_2D, maxLevel + 1, glFormat, width, height, resource.glMemory, resource.obj.memoryOffset);

                //
                GlContext.resourceImageMap.put(id, resource);
            } else {
                resourceObj = resource.obj;
            };
        };
    };

    // TODO: needs fully replace OpenGL buffer memory stack
    // TODO: needs immutable storage and ranges support
    public static ResourceBuffer vkCreateBuffer(long defaultSize) {
        var _pipelineLayout = rendererObj.pipelineLayout;
        var _memoryAllocator = rendererObj.memoryAllocator;
        ResourceBuffer resource = new ResourceBuffer();
        resource.obj = new MemoryAllocationObj.BufferObj(rendererObj.logicalDevice.getHandle(), resource.bufferCreateInfo = new MemoryAllocationCInfo.BufferCInfo(){{
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
    };

    //
    public static int glCreateBuffer() throws Exception {
        // TODO: support for typed (entity, indexed, blocks, etc.)
        var mapped = resourceTargetMap.get(0);
        var cache = new ResourceCache();
        cache.glStorageBuffer = mapped.glStorageBuffer;
        //cache.glStorageBuffer = GL45.glCreateBuffers();
        cache.mapped = mapped;
        return cache.glVirtualBuffer;
    };

    //
    public static ResourceCache glAllocateMemory(ResourceCache cache, long defaultSize, int usage) throws Exception {
        // TODO: support for typed (entity, indexed, blocks, etc.)
        var mapped = resourceTargetMap.get(0);

        if (cache == null) {
            System.out.println("Allocation Failed: " + "Allocation Not Found!");
            throw new Exception("Allocation Failed: " + "Allocation Not Found!");
        }

        defaultSize = Math.max(defaultSize, 1024L*64L*3L);
        if (cache.size != defaultSize)
        //if (cache.size < defaultSize)
        {
            var oldGlBuffer = cache.glStorageBuffer;
            var oldGlOffset = cache.offset.get(0);
            var oldGlSize = cache.size;
            var oldGlAlloc = cache.allocId.get(0);

            //
            cache.size = 0L; cache.offset.put(0, 0L);
            int res = vmaVirtualAllocate(mapped.vb.get(0), cache.allocCreateInfo.size(cache.size = defaultSize), cache.allocId.put(0, 0L), cache.offset);
            if (res != VK_SUCCESS) {
                System.out.println("Allocation Failed: " + res);
                throw new Exception("Allocation Failed: " + res);
            }

            //
            //cache.allocCreateInfo.size(cache.size = defaultSize);
            //GL45.glNamedBufferStorage(cache.glStorageBuffer, cache.allocCreateInfo.size(), GL_CLIENT_STORAGE_BIT | GL_DYNAMIC_STORAGE_BIT | GL_MAP_READ_BIT | GL_MAP_WRITE_BIT);
            //GL45.glNamedBufferData(cache.glStorageBuffer, cache.allocCreateInfo.size(), usage);

            //
            if (oldGlSize != 0) {
                if (oldGlBuffer != 0) { GL45.glCopyNamedBufferSubData(oldGlBuffer, cache.glStorageBuffer, oldGlOffset, cache.offset.get(0), Math.min(oldGlSize, cache.size)); };
                if (oldGlAlloc != 0) { vmaVirtualFree(mapped.vb.get(0), oldGlAlloc); };
                //if (oldGlBuffer != 0) { GL45.glDeleteBuffers(oldGlBuffer); };
                oldGlSize = 0L; oldGlOffset = 0L; oldGlAlloc = 0L;
            }

            //
            if (cache.target == GL_ARRAY_BUFFER && cache.stride > 0 && cache.size > 0) {
                glBindVertexBuffer(cache.bindingIndex, cache.glStorageBuffer, cache.offset.get(0), cache.stride);
            }
        }
        return cache;
    }

    //
    public static ResourceCache glAllocateMemory(int target, long defaultSize, int usage) throws Exception {
        return glAllocateMemory(boundBuffers.get(target), defaultSize, usage);
    }

    //
    public static void glBindBuffer(int target, int glVirtual) {
        GL20.glBindBuffer(target, 0);
        boundBuffers.remove(target);
        if (glVirtual != 0) {
            var cache = resourceCacheMap.get(glVirtual);
            if (cache != null && cache.glStorageBuffer != 0) {
                boundBuffers.put(cache.target = target, cache); // TODO: unbound memory
                GL20.glBindBuffer(target, cache.glStorageBuffer);

                //
                if (cache.target == GL_ARRAY_BUFFER && cache.stride > 0 && cache.size > 0) {
                    glBindVertexBuffer(cache.bindingIndex, cache.glStorageBuffer, cache.offset.get(0), cache.stride);
                }
                //cache.defer.clear();
            }
        }
    }

    // TODO: full replace by Vulkan
    public static ResourceCache glBufferData(int target, long data, int usage) throws Exception {
        return glAllocateMemory(target, data, usage);
    }

    // TODO: full replace by Vulkan
    public static ResourceCache glBufferData(int target, ByteBuffer data, int usage) throws Exception {
        var cache = glAllocateMemory(target, data.capacity(), usage);
        //glBufferSubData(target, cache.offset.get(0), data);
        glNamedBufferSubData(cache.glStorageBuffer, cache.offset.get(0), data);
        return cache;
    }

};
