package org.hydra2s.manhack.shared.vulkan;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.texture.NativeImage;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.shared.interfaces.GlBaseSharedTexture;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.MemoryAllocationObj;
import org.lwjgl.vulkan.VkExtent3D;

import java.nio.IntBuffer;
import java.util.Map;

import static org.lwjgl.opengl.EXTMemoryObject.glCreateMemoryObjectsEXT;
import static org.lwjgl.opengl.EXTMemoryObject.glTexStorageMem2DEXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.GL_R8;
import static org.lwjgl.opengl.GL30.GL_RG8;
import static org.lwjgl.vulkan.VK10.*;

//
public class GlVulkanSharedTexture implements GlBaseSharedTexture {

    //
    static public class VkSharedImage {
        public int glMemory = 0;
        public VkExtent3D extent;
        public MemoryAllocationObj.ImageObj obj;
        public MemoryAllocationCInfo.ImageCInfo imageCreateInfo;
    };

    //
    public static Map<Integer, VkSharedImage> sharedImageMap = null;

    //
    public static void prepareImage(NativeImage.InternalFormat internalFormat, int id, int maxLevel, int width, int height) {
        if (internalFormat == NativeImage.InternalFormat.RGB) {
            // TODO: Vulkan Support for RGB!
            for(int i = 0; i <= maxLevel; ++i) {
                GlStateManager._texImage2D(3553, i, internalFormat.getValue(), width >> i, height >> i, 0, 6408, 5121, (IntBuffer)null);
            }
        } else {
            VkSharedImage sharedImage = sharedImageMap.get(id);
            if (sharedImage == null) {
                sharedImage = new VkSharedImage();
                sharedImage.extent = VkExtent3D.calloc();
                sharedImage.extent.set(width, height, 1);

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
                var _pipelineLayout = GlContext.rendererObj.pipelineLayout;
                var _memoryAllocator = GlContext.rendererObj.memoryAllocator;
                VkSharedImage finalResource = sharedImage;
                int finalVkFormat = vkFormat;
                sharedImage.obj = new MemoryAllocationObj.ImageObj(GlContext.rendererObj.logicalDevice.getHandle(), sharedImage.imageCreateInfo = new MemoryAllocationCInfo.ImageCInfo(){{
                    extent3D = finalResource.extent;
                    format = finalVkFormat;
                    mipLevels = 1;
                    arrayLayers = 1;
                    isHost = false;
                    isDevice = true;
                    memoryAllocator = _memoryAllocator.getHandle().get();
                }});

                // TODO: use DSA! Needs avoid using binding...
                glBindTexture(GL_TEXTURE_2D, id);
                if (sharedImage.glMemory == 0) {
                    glImportMemoryWin32HandleEXT(sharedImage.glMemory = glCreateMemoryObjectsEXT(), sharedImage.obj.memoryRequirements2.memoryRequirements().size(), GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, sharedImage.obj.getWin32Handle().get(0));
                }
                glTexStorageMem2DEXT(GL_TEXTURE_2D, maxLevel + 1, glFormat, width, height, sharedImage.glMemory, sharedImage.obj.memoryOffset);

                //
                sharedImageMap.put(id, sharedImage);
            }
        };
    };

}
