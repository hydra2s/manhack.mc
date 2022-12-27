package org.hydra2s.manhack;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.texture.NativeImage;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.RendererCInfo;
import org.hydra2s.noire.objects.MemoryAllocationObj;
import org.hydra2s.noire.objects.MinecraftRendererObj;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL45;
import org.lwjgl.vulkan.VkExtent3D;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL30.GL_R8;
import static org.lwjgl.opengl.GL30.GL_RG8;
import static org.lwjgl.opengl.GL31.GL_COPY_WRITE_BUFFER;
import static org.lwjgl.opengl.GL31.glCopyBufferSubData;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TYPE_2D;

public class GlContext {

    public static boolean worldRendering = false;

    static public class ResourceImage {
        public int glMemory;
        public VkExtent3D extent;
        public MemoryAllocationObj.ImageObj obj;
        public MemoryAllocationCInfo.ImageCInfo imageCreateInfo;
    };

    static public class ResourceBuffer {
        public int glMemory;
        public MemoryAllocationObj.BufferObj obj;
        public MemoryAllocationCInfo.BufferCInfo bufferCreateInfo;
    };

    //
    static public class ResourceCache {
        public static MemoryAllocationObj.BufferObj cache;
        public static int glCache = 0;
        public static long offset = 0;
        public static long size = 0;

        //
        public ResourceCache(int target, long defaultSize) {
            //
            this.glCache = GlContext.glCreateBuffer(target, defaultSize);
            this.cache = GlContext.resourceBufferMap.get(this.glCache).obj;

            //
            this.offset = 0;
            this.size = 0;
        }
    };

    //
    public static Map<Integer, ResourceImage> resourceImageMap = new HashMap<Integer, ResourceImage>();
    public static Map<Integer, ResourceBuffer> resourceBufferMap = new HashMap<Integer, ResourceBuffer>();

    //
    public static boolean hasIndexBuffer = false;
    public static VertexFormat currentVertexFormat = null;

    //
    public static MinecraftRendererObj rendererObj;

    //
    public static void initialize() throws IOException {
        rendererObj = new MinecraftRendererObj(null, new RendererCInfo(){

        });

        // in finale...
        // TODO: create geometry level for entities (blank)
        // TODO: create instance level for everything (blank)
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
                    isHost = true;
                    isDevice = true;
                    memoryAllocator = _memoryAllocator.getHandle().get();
                }});

                //
                glBindTexture(GL_TEXTURE_2D, id);
                glImportMemoryWin32HandleEXT(resource.glMemory = glCreateMemoryObjectsEXT(), width * height * 4, GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, resourceObj.Win32Handle.get(0));
                glTexStorageMem2DEXT(GL_TEXTURE_2D, maxLevel + 1, glFormat, width, height, resource.glMemory, resource.obj.memoryOffset);

                //
                GlContext.resourceImageMap.put(id, resource);
            } else {
                resourceObj = resource.obj;
            };
        };
    };

    //
    public static int glCreateBuffer(int target, long defaultSize) {
        int glBuffer[] = {0};
        ResourceBuffer resource = new ResourceBuffer();

        //
        var _pipelineLayout = rendererObj.pipelineLayout;
        var _memoryAllocator = rendererObj.memoryAllocator;
        resource.obj = new MemoryAllocationObj.BufferObj(rendererObj.logicalDevice.getHandle(), new MemoryAllocationCInfo.BufferCInfo(){{
            isHost = true;
            isDevice = true;
            size = defaultSize;
            usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            memoryAllocator = _memoryAllocator.getHandle().get();
        }});

        //
        GL45.glCreateBuffers(glBuffer);
        glImportMemoryWin32HandleEXT(resource.glMemory = glCreateMemoryObjectsEXT(), defaultSize, GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, resource.obj.Win32Handle.get(0));
        glNamedBufferStorageMemEXT(glBuffer[0], defaultSize, resource.glMemory, resource.obj.memoryOffset);

        //
        GlContext.resourceBufferMap.put(glBuffer[0], resource);
        return glBuffer[0];
    };

    //
    public static void glBufferData(long defaultSize, int target, ByteBuffer data, int usage) {
        //
        if (target == GL_ARRAY_BUFFER || target == GL_ELEMENT_ARRAY_BUFFER) {
            int id = glGetInteger(target == GL_ARRAY_BUFFER ? GL_ARRAY_BUFFER_BINDING : GL_ELEMENT_ARRAY_BUFFER_BINDING);
            ResourceBuffer resource = GlContext.resourceBufferMap.get(id);

            //
            if (resource == null) {
                var _pipelineLayout = rendererObj.pipelineLayout;
                var _memoryAllocator = rendererObj.memoryAllocator;
                (resource = new ResourceBuffer()).obj = new MemoryAllocationObj.BufferObj(rendererObj.logicalDevice.getHandle(), new MemoryAllocationCInfo.BufferCInfo(){{
                    isHost = true;
                    isDevice = true;
                    size = Math.max(defaultSize, data.capacity());
                    usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
                    memoryAllocator = _memoryAllocator.getHandle().get();
                }});

                //
                glImportMemoryWin32HandleEXT(resource.glMemory = glCreateMemoryObjectsEXT(), defaultSize, GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, resource.obj.Win32Handle.get(0));
                glBufferStorageMemEXT(target, defaultSize, resource.glMemory, resource.obj.memoryOffset);

                //
                GlContext.resourceBufferMap.put(id, resource);
            }

            // BUT needs also Math.min(defaultSize, data.limit())
            glBufferSubData(target, 0, data);
        } else {
            GL20.glBufferData(target, data, usage);
        };
    }

};
