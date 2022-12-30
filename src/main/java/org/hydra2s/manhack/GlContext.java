package org.hydra2s.manhack;

//
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import org.hydra2s.manhack.shared.vulkan.GlVulkanSharedBuffer;
import org.hydra2s.manhack.shared.vulkan.GlVulkanSharedTexture;
import org.hydra2s.noire.descriptors.RendererCInfo;

//
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//
public class GlContext {
    public static boolean worldRendering = false;
    public static GlRendererObj rendererObj;

    //
    public static void initialize() throws IOException {
        rendererObj = new GlRendererObj(null, new RendererCInfo(){

        });

        //
        GlVulkanSharedBuffer.initialize(); // most stable!
        //GlDirectSharedBuffer.initialize();
    };

    //
    public static void inclusion() {
        ShaderProgram shader = RenderSystem.getShader();

        //
        List<GlVulkanSharedTexture.VkSharedImage> resources = new ArrayList<GlVulkanSharedTexture.VkSharedImage>();
        for(int j = 0; j < 8; ++j) {
            resources.add(GlVulkanSharedTexture.sharedImageMap.get(RenderSystem.getShaderTexture(j)));
        };

        //
        //GlSharedTextureSystem.VkSharedImage Sampler0 = resources.get(0);
    };

};
