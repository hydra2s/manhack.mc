package org.hydra2s.manhack;

//
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.RendererCInfo;
import org.hydra2s.noire.objects.MemoryAllocationObj;
import org.hydra2s.noire.objects.MinecraftRendererObj;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL45;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;

//
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.EXTMemoryObject.glCreateMemoryObjectsEXT;
import static org.lwjgl.opengl.EXTMemoryObject.glNamedBufferStorageMemEXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.util.vma.Vma.VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;

//
public class GlContext {
    public static boolean worldRendering = false;
    public static MinecraftRendererObj rendererObj;

    //
    public static void initialize() throws IOException {
        rendererObj = new MinecraftRendererObj(null, new RendererCInfo(){

        });
        GlSharedBufferSystem.initialize();
    };

    //
    public static void inclusion() {
        ShaderProgram shader = RenderSystem.getShader();

        //
        List<GlSharedTextureSystem.VkSharedImage> resources = new ArrayList<GlSharedTextureSystem.VkSharedImage>();
        for(int j = 0; j < 8; ++j) {
            resources.add(GlSharedTextureSystem.resourceImageMap.get(RenderSystem.getShaderTexture(j)));
        };

        //
        //GlSharedTextureSystem.VkSharedImage Sampler0 = resources.get(0);
    };

};
