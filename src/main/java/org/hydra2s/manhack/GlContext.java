package org.hydra2s.manhack;

//
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import org.hydra2s.manhack.shared.vulkan.GlVulkanSharedBuffer;
import org.hydra2s.manhack.shared.vulkan.GlVulkanSharedTexture;
import org.hydra2s.manhack.virtual.buffer.GlBaseVirtualBuffer;
import org.hydra2s.noire.descriptors.RendererCInfo;

//
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: unify with GlRendererObj
public class GlContext {
    public static boolean worldRendering = false;
    public static GlRendererObj rendererObj;

    //
    public static VertexBuffer boundVertexBuffer;
    public static ShaderProgram boundShaderProgram; // only for download a uniform data

    //
    public static Map<Integer, GlBaseVirtualBuffer.VirtualBufferObj> boundBuffers = new HashMap<Integer, GlBaseVirtualBuffer.VirtualBufferObj>() {{

    }};

    //
    public static UnifiedMap<GlBaseVirtualBuffer.VirtualBufferObj> virtualBufferMap = new UnifiedMap<GlBaseVirtualBuffer.VirtualBufferObj>();
    public static GlBaseVirtualBuffer.VirtualBufferObj dummyCache = new GlBaseVirtualBuffer.VirtualBufferObj();

    //
    public static void initialize() throws Exception {
        rendererObj = new GlRendererObj(null, new RendererCInfo(){

        });

        //
        GlVulkanSharedBuffer.initialize(); // most stable!
        //GlDirectSharedBuffer.initialize();
    };

};
