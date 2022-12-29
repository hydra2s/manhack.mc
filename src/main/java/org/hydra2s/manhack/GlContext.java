package org.hydra2s.manhack;

//
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import org.hydra2s.noire.descriptors.RendererCInfo;
import org.hydra2s.noire.objects.MinecraftRendererObj;

//
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//
public class GlContext {
    public static boolean worldRendering = false;
    public static MinecraftRendererObj rendererObj;

    //
    public static void initialize() throws IOException {
        rendererObj = new MinecraftRendererObj(null, new RendererCInfo(){

        });
        //GlVirtualBufferSystem.initialize();
    };

    //
    public static void inclusion() {
        ShaderProgram shader = RenderSystem.getShader();

        //
        List<GlSharedTextureSystem.ResourceImage> resources = new ArrayList<GlSharedTextureSystem.ResourceImage>();
        for(int j = 0; j < 8; ++j) {
            resources.add(GlSharedTextureSystem.resourceImageMap.get(RenderSystem.getShaderTexture(j)));
        };

        //
        GlSharedTextureSystem.ResourceImage Sampler0 = resources.get(0);
    };

};
